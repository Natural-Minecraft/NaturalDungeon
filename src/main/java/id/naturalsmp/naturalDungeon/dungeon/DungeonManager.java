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

    public void startDungeon(List<UUID> participants, Dungeon dungeon, DungeonDifficulty difficulty) {
        if (difficulty == null) {
            // Fallback if difficulty optional or missing
            difficulty = dungeon.getDifficulty("normal");
            if (difficulty == null) {
                plugin.getLogger()
                        .warning("Failed to start dungeon " + dungeon.getId() + ": No 'normal' difficulty found.");
                return;
            }
        }

        // SMART ROUTER: Find the first available instance (1, 2, or 3)
        int instanceId = -1;
        Set<Integer> occupiedIds = new HashSet<>();
        for (DungeonInstance active : activeInstances.values()) {
            if (active.getDungeon().getId().equals(dungeon.getId())) {
                occupiedIds.add(active.getInstanceId());
            }
        }

        // Try instances 1 to 3
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

        // [ATOMIC] Consume key now that instance is confirmed
        Player leader = Bukkit.getPlayer(participants.get(0));
        if (leader != null) {
            if (!checkKey(leader, difficulty)) {
                leader.sendMessage(ConfigUtils.getMessage("dungeon.key-required", "%key%", difficulty.getKeyReq()));
                return;
            }
            consumeKey(leader, difficulty);
        }

        plugin.getLogger().info("Starting dungeon " + dungeon.getId() + " (Instance " + instanceId + ") for "
                + participants.size() + " players.");
        DungeonInstance instance = new DungeonInstance(plugin, dungeon, difficulty, instanceId, participants);
        for (UUID uuid : participants) {
            activeInstances.put(uuid, instance);
        }
        instance.start();
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
    }

    public void openDungeonGUI(Player player) {
        new DungeonGUI(plugin).open(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        DungeonInstance instance = activeInstances.get(player.getUniqueId());
        if (instance != null) {
            instance.handlePlayerLeave(player);
        }
    }
}
