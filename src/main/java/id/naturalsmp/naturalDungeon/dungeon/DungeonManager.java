package id.naturalsmp.naturaldungeon.dungeon;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.party.Party;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import id.naturalsmp.naturaldungeon.utils.ConfigUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import java.util.*;

public class DungeonManager implements Listener {

    private final NaturalDungeon plugin;
    private final Map<String, Dungeon> dungeons = new HashMap<>();
    private final Map<UUID, DungeonInstance> activeInstances = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public DungeonManager(NaturalDungeon plugin) {
        this.plugin = plugin;
        loadDungeons();
    }

    public void loadDungeons() {
        dungeons.clear();
        File dungeonsFolder = new File(plugin.getDataFolder(), "dungeons");
        if (!dungeonsFolder.exists())
            dungeonsFolder.mkdirs();

        File[] files = dungeonsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null)
            return;

        for (File file : files) {
            String id = file.getName().replace(".yml", "");
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            dungeons.put(id, new Dungeon(id, config));
            plugin.getLogger().info("Loaded dungeon: " + id);
        }
    }

    public Dungeon getDungeon(String id) {
        return dungeons.get(id);
    }

    public Collection<Dungeon> getDungeons() {
        return dungeons.values();
    }

    public Set<String> getDungeonIds() {
        return dungeons.keySet();
    }

    public DungeonInstance getActiveInstance(Player player) {
        return activeInstances.get(player.getUniqueId());
    }

    public Collection<DungeonInstance> getActiveInstances() {
        return new HashSet<>(activeInstances.values());
    }

    public boolean isInDungeon(Player player) {
        return activeInstances.containsKey(player.getUniqueId());
    }

    public int getActiveInstanceCount() {
        return (int) activeInstances.values().stream().distinct().count();
    }

    public boolean isOnCooldown(Player player, Dungeon dungeon) {
        if (player.hasPermission("natural.dungeon.cooldown.bypass"))
            return false;

        Long lastRun = cooldowns.get(player.getUniqueId());
        if (lastRun == null)
            return false;
        return System.currentTimeMillis() - lastRun < dungeon.getCooldownSeconds() * 1000;
    }

    public long getRemainingCooldown(Player player, Dungeon dungeon) {
        Long lastRun = cooldowns.get(player.getUniqueId());
        if (lastRun == null)
            return 0;
        long elapsed = System.currentTimeMillis() - lastRun;
        long remaining = dungeon.getCooldownSeconds() * 1000 - elapsed;
        return remaining > 0 ? remaining / 1000 : 0;
    }

    public void startDungeon(Player player, Dungeon dungeon) {
        List<UUID> participants = new ArrayList<>();
        participants.add(player.getUniqueId());

        Party party = plugin.getPartyManager().getParty(player.getUniqueId());
        if (party != null) {
            for (UUID memberId : party.getMembers()) {
                if (memberId.equals(player.getUniqueId()))
                    continue;
                Player member = Bukkit.getPlayer(memberId);
                if (member != null && member.isOnline()) {
                    participants.add(memberId);
                }
            }
        }

        startDungeon(participants, dungeon, dungeon.getDifficulty("normal"));
    }

    private final Map<UUID, Integer> pendingStarts = new HashMap<>();

    @EventHandler
    public void onPlayerChat(org.bukkit.event.player.AsyncPlayerChatEvent e) {
        if (pendingStarts.containsKey(e.getPlayer().getUniqueId())) {
            if (e.getMessage().equalsIgnoreCase("cancel")) {
                e.setCancelled(true); // Don't show "cancel" in chat
                cancelPendingStart(e.getPlayer(), true);
            }
        }
    }

    public void cancelPendingStart(Player player, boolean msg) {
        Integer taskId = pendingStarts.remove(player.getUniqueId());
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
            if (msg)
                player.sendMessage(ChatUtils.colorize("&c&lDungeon Start Cancelled!"));
        }
    }

    public void startDungeon(List<UUID> participants, Dungeon dungeon, DungeonDifficulty difficulty) {
        if (difficulty == null) {
            difficulty = dungeon.getDifficulty("normal");
            if (difficulty == null) {
                plugin.getLogger()
                        .warning("Failed to start dungeon " + dungeon.getId() + ": No 'normal' difficulty found.");
                return;
            }
        }

        // Validation moved to *before* delay
        for (UUID uuid : participants) {
            if (activeInstances.containsKey(uuid) || pendingStarts.containsKey(uuid)) {
                // Already busy
                return;
            }
        }

        // SMART ROUTER (Check instance availability early)
        int instanceId = -1;
        Set<Integer> occupiedIds = new HashSet<>();
        for (DungeonInstance active : activeInstances.values()) {
            if (active.getDungeon().getId().equals(dungeon.getId())) {
                occupiedIds.add(active.getInstanceId());
            }
        }
        for (int i = 1; i <= 3; i++) {
            if (!occupiedIds.contains(i)) {
                instanceId = i;
                break;
            }
        }
        if (instanceId == -1) {
            String msg = ChatUtils
                    .colorize("&6&lNaturalDungeon &8» &cMaaf, semua arena (1, 2, 3) sedang digunakan. Silakan antri!");
            for (UUID uuid : participants) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null)
                    p.sendMessage(msg);
            }
            return;
        }

        final int finalInstanceId = instanceId;
        final DungeonDifficulty finalDifficulty = difficulty;

        // Broadcast Warmup
        for (UUID uuid : participants) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.sendMessage(ChatUtils
                        .colorize("&6&lNaturalDungeon &8» &eTeleporting in 5 seconds... &7(Type 'cancel' to abort)"));
                p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
            }
        }

        int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Remove from pending map first
            for (UUID uuid : participants)
                pendingStarts.remove(uuid);

            // Re-validate Players & Key
            List<UUID> validParticipants = new ArrayList<>();
            for (UUID uuid : participants) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline())
                    validParticipants.add(uuid);
            }

            if (validParticipants.isEmpty())
                return;

            Player leader = Bukkit.getPlayer(validParticipants.get(0));
            if (leader == null)
                return;

            // Consume Key just before start
            if (!checkKey(leader, finalDifficulty)) {
                leader.sendMessage(
                        ConfigUtils.getMessage("dungeon.key-required", "%key%", finalDifficulty.getKeyReq()));
                return; // Abort
            }
            consumeKey(leader, finalDifficulty);

            // START
            plugin.getLogger().info("Starting dungeon " + dungeon.getId() + " (Instance " + finalInstanceId + ") for "
                    + validParticipants.size() + " players.");
            DungeonInstance instance = new DungeonInstance(plugin, dungeon, finalDifficulty, finalInstanceId,
                    validParticipants);
            for (UUID uuid : validParticipants) {
                activeInstances.put(uuid, instance);
            }
            instance.start();

        }, 100L).getTaskId();

        for (UUID uuid : participants) {
            pendingStarts.put(uuid, taskId);
        }
    }

    private boolean checkKey(Player player, DungeonDifficulty diff) {
        String req = diff.getKeyReq();
        if (req == null || req.equalsIgnoreCase("none"))
            return true;

        String[] parts = req.split(":");
        if (parts.length < 3)
            return true;

        String type = parts[0];
        int amount = Integer.parseInt(parts[2]);

        if (type.equalsIgnoreCase("VANILLA")) {
            Material mat = Material.matchMaterial(parts[1]);
            return mat != null && player.getInventory().containsAtLeast(new ItemStack(mat), amount);
        } else if (type.equalsIgnoreCase("MMOITEMS") && plugin.hasMMOItems()) {
            return plugin.getMmoItemsHook().hasItem(player, parts[1], parts[2], amount);
        }
        return true;
    }

    private void consumeKey(Player player, DungeonDifficulty diff) {
        String req = diff.getKeyReq();
        if (req == null || req.equalsIgnoreCase("none"))
            return;

        String[] parts = req.split(":");
        if (parts.length < 3)
            return;

        String type = parts[0];
        int amount = Integer.parseInt(parts[2]);

        if (type.equalsIgnoreCase("VANILLA")) {
            Material mat = Material.matchMaterial(parts[1]);
            if (mat != null)
                player.getInventory().removeItem(new ItemStack(mat, amount));
        } else if (type.equalsIgnoreCase("MMOITEMS") && plugin.hasMMOItems()) {
            plugin.getMmoItemsHook().consumeItem(player, parts[1], parts[2], amount);
        }
    }

    public void endDungeon(DungeonInstance instance, boolean victory) {
        for (UUID uuid : instance.getParticipants()) {
            activeInstances.remove(uuid);
            if (victory)
                cooldowns.put(uuid, System.currentTimeMillis());
        }
        instance.end(victory);
    }

    public void shutdown() {
        for (DungeonInstance instance : new HashSet<>(activeInstances.values())) {
            instance.forceEnd();
        }
        activeInstances.clear();
        // Clear pending
        for (Integer taskId : pendingStarts.values())
            Bukkit.getScheduler().cancelTask(taskId);
        pendingStarts.clear();
    }

    public void openDungeonGUI(Player player) {
        new DungeonGUI(plugin).open(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        cancelPendingStart(player, false); // Cancel if they were waiting
        DungeonInstance instance = activeInstances.get(player.getUniqueId());
        if (instance != null) {
            instance.handlePlayerLeave(player);
        }
    }
}
