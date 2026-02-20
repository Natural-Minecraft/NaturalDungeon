package id.naturalsmp.naturaldungeon.stats;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Manages player stats, persisting to playerdata/<uuid>.yml
 */
public class PlayerStatsManager {

    private final NaturalDungeon plugin;
    private final Map<UUID, PlayerStats> cache = new HashMap<>();
    private final File dataFolder;

    public PlayerStatsManager(NaturalDungeon plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists())
            dataFolder.mkdirs();
    }

    public PlayerStats getStats(UUID uuid) {
        return cache.computeIfAbsent(uuid, this::loadStats);
    }

    private PlayerStats loadStats(UUID uuid) {
        File file = new File(dataFolder, uuid + ".yml");
        PlayerStats stats = new PlayerStats(uuid);
        if (!file.exists())
            return stats;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        stats.setTotalClears(config.getInt("total-clears", 0));
        stats.setTotalDeaths(config.getInt("total-deaths", 0));
        stats.setFlawlessClears(config.getInt("flawless-clears", 0));
        stats.setFastestTime(config.getLong("fastest-time", Long.MAX_VALUE));
        stats.setTotalDamageDealt(config.getDouble("damage-dealt", 0));
        stats.setTotalDamageTaken(config.getDouble("damage-taken", 0));

        stats.setDailyClears(config.getInt("daily-clears", 0));
        stats.setLastDailyReset(config.getLong("last-daily-reset", System.currentTimeMillis()));
        stats.setWeeklyClears(config.getInt("weekly-clears", 0));
        stats.setLastWeeklyReset(config.getLong("last-weekly-reset", System.currentTimeMillis()));

        // Load dungeon-specific clears
        if (config.isConfigurationSection("dungeon-clears")) {
            Map<String, Integer> clears = new HashMap<>();
            for (String key : config.getConfigurationSection("dungeon-clears").getKeys(false)) {
                clears.put(key, config.getInt("dungeon-clears." + key, 0));
            }
            stats.setDungeonClears(clears);
        }
        return stats;
    }

    public void saveStats(UUID uuid) {
        PlayerStats stats = cache.get(uuid);
        if (stats == null)
            return;

        File file = new File(dataFolder, uuid + ".yml");
        YamlConfiguration config = new YamlConfiguration();
        config.set("total-clears", stats.getTotalClears());
        config.set("total-deaths", stats.getTotalDeaths());
        config.set("flawless-clears", stats.getFlawlessClears());
        config.set("fastest-time", stats.getFastestTime());
        config.set("damage-dealt", stats.getTotalDamageDealt());
        config.set("damage-taken", stats.getTotalDamageTaken());

        config.set("daily-clears", stats.getDailyClears());
        config.set("last-daily-reset", stats.getLastDailyReset());
        config.set("weekly-clears", stats.getWeeklyClears());
        config.set("last-weekly-reset", stats.getLastWeeklyReset());

        for (Map.Entry<String, Integer> entry : stats.getDungeonClears().entrySet()) {
            config.set("dungeon-clears." + entry.getKey(), entry.getValue());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save stats for " + uuid + ": " + e.getMessage());
        }
    }

    public void saveAll() {
        for (UUID uuid : cache.keySet()) {
            saveStats(uuid);
        }
    }

    /**
     * Record a dungeon completion for a player.
     */
    public void recordClear(UUID uuid, String dungeonId, long timeMs, int deaths) {
        PlayerStats stats = getStats(uuid);
        stats.addClear(dungeonId, timeMs, deaths);
        saveStats(uuid);
    }

    /**
     * Check if player has cleared a specific dungeon+difficulty.
     */
    public boolean hasCleared(UUID uuid, String dungeonId) {
        return getStats(uuid).getDungeonClears().containsKey(dungeonId);
    }
}
