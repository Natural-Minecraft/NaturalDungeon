package id.naturalsmp.naturaldungeon.progression;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Dungeon Mastery Progression — per-player XP and level tracking per dungeon.
 *
 * Mastery levels unlock:
 * - Level 1-5: Basic progression (more XP per clear)
 * - Level 5: Access to Hard difficulty
 * - Level 10: Access to Nightmare difficulty
 * - Level 15: Mastery title
 * - Level 20: Legendary skin unlock
 *
 * Data stored in: plugins/NaturalDungeon/mastery/{uuid}.yml
 */
public class MasteryManager {

    private final NaturalDungeon plugin;
    private final File masteryDir;
    private final Map<UUID, YamlConfiguration> cache = new HashMap<>();

    // XP curve: xpToLevel = BASE * (level ^ EXPONENT)
    private static final int BASE_XP = 100;
    private static final double EXPONENT = 1.5;

    public MasteryManager(NaturalDungeon plugin) {
        this.plugin = plugin;
        this.masteryDir = new File(plugin.getDataFolder(), "mastery");
        if (!masteryDir.exists())
            masteryDir.mkdirs();
    }

    /**
     * Get the mastery level for a player+dungeon pair.
     */
    public int getLevel(UUID player, String dungeonId) {
        return getConfig(player).getInt("dungeons." + dungeonId + ".level", 0);
    }

    /**
     * Get current XP for a player+dungeon pair.
     */
    public int getXP(UUID player, String dungeonId) {
        return getConfig(player).getInt("dungeons." + dungeonId + ".xp", 0);
    }

    /**
     * Get total clears for a player+dungeon pair.
     */
    public int getClears(UUID player, String dungeonId) {
        return getConfig(player).getInt("dungeons." + dungeonId + ".clears", 0);
    }

    /**
     * Get best time for a player+dungeon pair (in seconds).
     */
    public long getBestTime(UUID player, String dungeonId) {
        return getConfig(player).getLong("dungeons." + dungeonId + ".best-time", -1);
    }

    /**
     * XP required for the given level.
     */
    public int xpForLevel(int level) {
        if (level <= 0)
            return 0;
        return (int) (BASE_XP * Math.pow(level, EXPONENT));
    }

    /**
     * Award XP to a player for a dungeon. Handles leveling up.
     */
    public void awardXP(UUID playerId, String dungeonId, int amount) {
        YamlConfiguration config = getConfig(playerId);
        String path = "dungeons." + dungeonId;

        int currentXP = config.getInt(path + ".xp", 0);
        int currentLevel = config.getInt(path + ".level", 0);
        currentXP += amount;

        // Level up loop
        while (currentXP >= xpForLevel(currentLevel + 1)) {
            currentXP -= xpForLevel(currentLevel + 1);
            currentLevel++;
            onLevelUp(playerId, dungeonId, currentLevel);
        }

        config.set(path + ".xp", currentXP);
        config.set(path + ".level", currentLevel);
        saveConfig(playerId, config);

        // Notify player
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            int nextLevelXP = xpForLevel(currentLevel + 1);
            player.sendMessage(ChatUtils.colorize("&#FFD700&l⭐ &7+" + amount + " Mastery XP &8(" +
                    currentXP + "/" + nextLevelXP + ")"));
        }
    }

    /**
     * Record a dungeon clear with time.
     */
    public void recordClear(UUID playerId, String dungeonId, long timeSeconds) {
        YamlConfiguration config = getConfig(playerId);
        String path = "dungeons." + dungeonId;

        int clears = config.getInt(path + ".clears", 0) + 1;
        config.set(path + ".clears", clears);

        long bestTime = config.getLong(path + ".best-time", -1);
        if (bestTime < 0 || timeSeconds < bestTime) {
            config.set(path + ".best-time", timeSeconds);
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.sendMessage(ChatUtils.colorize("&#55FF55&l🏆 &7New best time! &f" +
                        formatTime(timeSeconds)));
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.2f);
            }
        }

        saveConfig(playerId, config);
    }

    private void onLevelUp(UUID playerId, String dungeonId, int newLevel) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null)
            return;

        player.sendTitle(ChatUtils.colorize("&#FFD700&l⭐ LEVEL UP!"),
                ChatUtils.colorize("&#FFFFFF" + dungeonId + " &7— Mastery &f" + newLevel), 10, 60, 20);
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);

        // Unlock messages
        switch (newLevel) {
            case 5 -> player.sendMessage(ChatUtils.colorize(
                    "&#55FF55&l🔓 &7Unlocked &cHard &7difficulty untuk " + dungeonId + "!"));
            case 10 -> player.sendMessage(ChatUtils.colorize(
                    "&#55FF55&l🔓 &7Unlocked &4Nightmare &7difficulty untuk " + dungeonId + "!"));
            case 15 -> player.sendMessage(ChatUtils.colorize(
                    "&#FFD700&l🏅 &7Earned &6Mastery Title &7untuk " + dungeonId + "!"));
            case 20 -> player.sendMessage(ChatUtils.colorize(
                    "&#AA44FF&l✨ &7Max Mastery! Legendary rewards unlocked!"));
        }
    }

    /**
     * Get player mastery profile summary for all dungeons.
     */
    public Map<String, int[]> getProfile(UUID playerId) {
        YamlConfiguration config = getConfig(playerId);
        Map<String, int[]> profile = new LinkedHashMap<>();

        if (config.getConfigurationSection("dungeons") != null) {
            for (String id : config.getConfigurationSection("dungeons").getKeys(false)) {
                int level = config.getInt("dungeons." + id + ".level", 0);
                int xp = config.getInt("dungeons." + id + ".xp", 0);
                int clears = config.getInt("dungeons." + id + ".clears", 0);
                profile.put(id, new int[] { level, xp, clears });
            }
        }

        return profile;
    }

    private YamlConfiguration getConfig(UUID playerId) {
        if (cache.containsKey(playerId))
            return cache.get(playerId);
        File file = new File(masteryDir, playerId + ".yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        cache.put(playerId, config);
        return config;
    }

    private void saveConfig(UUID playerId, YamlConfiguration config) {
        try {
            config.save(new File(masteryDir, playerId + ".yml"));
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save mastery for " + playerId + ": " + e.getMessage());
        }
    }

    private String formatTime(long seconds) {
        long min = seconds / 60;
        long sec = seconds % 60;
        return String.format("%d:%02d", min, sec);
    }
}
