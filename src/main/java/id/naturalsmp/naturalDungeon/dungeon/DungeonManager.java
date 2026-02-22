package id.naturalsmp.naturaldungeon.dungeon;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.party.Party;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import id.naturalsmp.naturaldungeon.utils.ConfigUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
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

    public boolean cloneDungeon(String sourceId, String newId) {
        Dungeon source = dungeons.get(sourceId);
        if (source == null)
            return false;

        File dungeonsFolder = new File(plugin.getDataFolder(), "dungeons");
        File sourceFile = new File(dungeonsFolder, sourceId + ".yml");
        File newFile = new File(dungeonsFolder, newId + ".yml");

        if (!sourceFile.exists() || newFile.exists())
            return false;

        try {
            // Copy file content
            java.nio.file.Files.copy(sourceFile.toPath(), newFile.toPath());

            // Load new config and change display name & id
            YamlConfiguration newConfig = YamlConfiguration.loadConfiguration(newFile);
            newConfig.set("display-name", source.getDisplayName() + " (Copy)");
            newConfig.save(newFile);

            // Load into memory
            dungeons.put(newId, new Dungeon(newId, newConfig));
            plugin.getLogger().info("Successfully cloned dungeon " + sourceId + " to " + newId);
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to clone dungeon " + sourceId + " to " + newId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
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
        if (player.hasPermission("naturaldungeon.bypass.cooldown"))
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
            // Also remove ALL other players sharing the same taskId (party members)
            pendingStarts.values().removeIf(id -> id.equals(taskId));
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

        if (plugin.isMaintenanceMode()) {
            if (participants.size() > 0) {
                Player p = Bukkit.getPlayer(participants.get(0));
                if (p != null)
                    p.sendMessage(ChatUtils.colorize(
                            "&c&l[!] &cDungeon System sedang dalam masa perbaikan (Maintenance). Coba lagi nanti!"));
            }
            return;
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

    public void startTestDungeon(Player player, Dungeon dungeon) {
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
            player.sendMessage(ChatUtils.colorize("&cSemua arena sedang digunakan!"));
            return;
        }

        DungeonDifficulty difficulty = dungeon.getDifficulty("normal");
        if (difficulty == null && !dungeon.getDifficulties().isEmpty()) {
            difficulty = dungeon.getDifficulties().iterator().next(); // Grab first available
        }

        if (difficulty == null) {
            player.sendMessage(ChatUtils.colorize("&cDungeon ini belum memiliki level difficulty untuk di test!"));
            return;
        }

        List<UUID> participants = Collections.singletonList(player.getUniqueId());
        DungeonInstance instance = new DungeonInstance(plugin, dungeon, difficulty, instanceId, participants);
        instance.setTestMode(true);

        activeInstances.put(player.getUniqueId(), instance);
        player.sendMessage(ChatUtils.colorize("&6&l[TEST MODE] &7Memulai dungeon tanpa cooldown..."));
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

        if (type.equalsIgnoreCase("VANILLA")) {
            // Format: VANILLA:MATERIAL:AMOUNT
            int amount = Integer.parseInt(parts[2]);
            Material mat = Material.matchMaterial(parts[1]);
            return mat != null && player.getInventory().containsAtLeast(new ItemStack(mat), amount);
        } else if (type.equalsIgnoreCase("MMOITEMS") && plugin.hasMMOItems()) {
            // Format: MMOITEMS:TYPE:ID:AMOUNT
            if (parts.length < 4)
                return true;
            int amount = Integer.parseInt(parts[3]);
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
        if (type.equalsIgnoreCase("VANILLA")) {
            // Format: VANILLA:MATERIAL:AMOUNT
            int amount = Integer.parseInt(parts[2]);
            Material mat = Material.matchMaterial(parts[1]);
            if (mat != null)
                player.getInventory().removeItem(new ItemStack(mat, amount));
        } else if (type.equalsIgnoreCase("MMOITEMS") && plugin.hasMMOItems()) {
            // Format: MMOITEMS:TYPE:ID:AMOUNT
            if (parts.length < 4)
                return;
            int amount = Integer.parseInt(parts[3]);
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
        plugin.getDungeonGUI().open(player);
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

    // ============ EDITOR UTILITY METHODS ============

    private File getDungeonFile(String dungeonId) {
        return new File(plugin.getDataFolder(), "dungeons/" + dungeonId + ".yml");
    }

    public YamlConfiguration loadDungeonConfig(String dungeonId) {
        File file = getDungeonFile(dungeonId);
        if (!file.exists())
            return new YamlConfiguration();
        return YamlConfiguration.loadConfiguration(file);
    }

    private void saveDungeonConfig(String dungeonId, YamlConfiguration config) {
        try {
            config.save(getDungeonFile(dungeonId));
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save dungeon config: " + dungeonId + " - " + e.getMessage());
        }
    }

    public void setDungeonConfig(String dungeonId, String path, Object value) {
        YamlConfiguration config = loadDungeonConfig(dungeonId);
        config.set(path, value);
        saveDungeonConfig(dungeonId, config);
        reloadDungeon(dungeonId);
    }

    public void createEmptyDungeon(String dungeonId) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("display-name", dungeonId);
        config.set("description", "A new dungeon");
        config.set("world", "world");
        config.set("max-players", 4);
        config.set("cooldown", 300);
        config.set("material", "DIAMOND_SWORD");
        saveDungeonConfig(dungeonId, config);
        reloadDungeon(dungeonId);
    }

    public void createDifficulty(String dungeonId, String diffId) {
        YamlConfiguration config = loadDungeonConfig(dungeonId);
        config.set("difficulties." + diffId + ".display", diffId);
        config.set("difficulties." + diffId + ".min-tier", 0);
        config.set("difficulties." + diffId + ".reward-multiplier", 1.0);
        config.set("difficulties." + diffId + ".max-deaths", 5);
        saveDungeonConfig(dungeonId, config);
        reloadDungeon(dungeonId);
    }

    public void deleteDifficulty(String dungeonId, String diffId) {
        YamlConfiguration config = loadDungeonConfig(dungeonId);
        config.set("difficulties." + diffId, null);
        saveDungeonConfig(dungeonId, config);
        reloadDungeon(dungeonId);
    }

    public int getStageCount(String dungeonId) {
        YamlConfiguration config = loadDungeonConfig(dungeonId);
        ConfigurationSection sec = config.getConfigurationSection("stages");
        return sec == null ? 0 : sec.getKeys(false).size();
    }

    public void addStage(String dungeonId) {
        int nextStage = getStageCount(dungeonId) + 1;
        YamlConfiguration config = loadDungeonConfig(dungeonId);
        config.set("stages." + nextStage + ".waves.1.mobs", new ArrayList<>());
        saveDungeonConfig(dungeonId, config);
        reloadDungeon(dungeonId);
    }

    public void deleteStage(String dungeonId, int stageNum) {
        YamlConfiguration config = loadDungeonConfig(dungeonId);
        config.set("stages." + stageNum, null);
        saveDungeonConfig(dungeonId, config);
        reloadDungeon(dungeonId);
    }

    public int getWaveCount(String dungeonId, int stageIndex) {
        YamlConfiguration config = loadDungeonConfig(dungeonId);
        String path = "stages." + (stageIndex + 1) + ".waves";
        if (config.isList(path)) {
            List<?> list = config.getList(path);
            return list == null ? 0 : list.size();
        }
        ConfigurationSection sec = config.getConfigurationSection(path);
        return sec == null ? 0 : sec.getKeys(false).size();
    }

    public void addWave(String dungeonId, int stageIndex) {
        int nextWave = getWaveCount(dungeonId, stageIndex) + 1;
        YamlConfiguration config = loadDungeonConfig(dungeonId);
        config.set("stages." + (stageIndex + 1) + ".waves." + nextWave + ".mobs", new ArrayList<>());
        config.set("stages." + (stageIndex + 1) + ".waves." + nextWave + ".delay", 0);
        config.set("stages." + (stageIndex + 1) + ".waves." + nextWave + ".count", 1);
        saveDungeonConfig(dungeonId, config);
        reloadDungeon(dungeonId);
    }

    public void deleteWave(String dungeonId, int stageIndex, int waveIndex) {
        YamlConfiguration config = loadDungeonConfig(dungeonId);
        config.set("stages." + (stageIndex + 1) + ".waves." + (waveIndex + 1), null);
        saveDungeonConfig(dungeonId, config);
        reloadDungeon(dungeonId);
    }

    public void addWaveMob(String dungeonId, int stageIndex, int waveIndex, String mobId) {
        YamlConfiguration config = loadDungeonConfig(dungeonId);
        String path = "stages." + (stageIndex + 1) + ".waves." + (waveIndex + 1) + ".mobs";
        List<String> mobs = config.getStringList(path);
        mobs.add(mobId);
        config.set(path, mobs);
        saveDungeonConfig(dungeonId, config);
        reloadDungeon(dungeonId);
    }

    public List<String> getLootEntries(String dungeonId) {
        YamlConfiguration config = loadDungeonConfig(dungeonId);
        ConfigurationSection sec = config.getConfigurationSection("loot");
        if (sec == null)
            return new ArrayList<>();
        return new ArrayList<>(sec.getKeys(false));
    }

    public void addLootEntry(String dungeonId) {
        int nextEntry = getLootEntries(dungeonId).size() + 1;
        YamlConfiguration config = loadDungeonConfig(dungeonId);
        config.set("loot." + nextEntry + ".type", "VANILLA");
        config.set("loot." + nextEntry + ".material", "DIAMOND");
        config.set("loot." + nextEntry + ".amount", 1);
        config.set("loot." + nextEntry + ".chance", 100.0);
        saveDungeonConfig(dungeonId, config);
    }

    public void deleteLootEntry(String dungeonId, int entryIndex) {
        YamlConfiguration config = loadDungeonConfig(dungeonId);
        config.set("loot." + entryIndex, null);
        saveDungeonConfig(dungeonId, config);
    }

    private void reloadDungeon(String dungeonId) {
        File file = getDungeonFile(dungeonId);
        if (file.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            dungeons.put(dungeonId, new Dungeon(dungeonId, config));
        }
    }
}
