package id.naturalsmp.naturaldungeon.progression;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Seasonal Ranking System — monthly competitive leaderboards.
 *
 * Each season lasts ~30 days. Players earn Season Points (SP) from:
 * - Dungeon clears (base SP)
 * - Difficulty multiplier
 * - Speed bonus (faster = more SP)
 * - Flawless bonus (no deaths)
 *
 * Ranks: Bronze → Silver → Gold → Platinum → Diamond → Master → Legend
 *
 * Data stored in: plugins/NaturalDungeon/seasons/
 */
public class SeasonManager {

    private final NaturalDungeon plugin;
    private final File seasonDir;
    private YamlConfiguration seasonConfig;
    private File seasonFile;

    // Season metadata
    private int currentSeason;
    private long seasonStartMs;
    private long seasonEndMs;

    // Rank thresholds (cumulative SP)
    public enum SeasonRank {
        BRONZE(0, "&7🥉", "Bronze"),
        SILVER(500, "&f🥈", "Silver"),
        GOLD(1500, "&6🥇", "Gold"),
        PLATINUM(3500, "&b💎", "Platinum"),
        DIAMOND(6000, "&b✦", "Diamond"),
        MASTER(10000, "&d⚜", "Master"),
        LEGEND(20000, "&6&l★", "Legend");

        public final int threshold;
        public final String icon;
        public final String name;

        SeasonRank(int threshold, String icon, String name) {
            this.threshold = threshold;
            this.icon = icon;
            this.name = name;
        }
    }

    public SeasonManager(NaturalDungeon plugin) {
        this.plugin = plugin;
        this.seasonDir = new File(plugin.getDataFolder(), "seasons");
        if (!seasonDir.exists())
            seasonDir.mkdirs();
        loadOrCreateSeason();
    }

    private void loadOrCreateSeason() {
        seasonFile = new File(seasonDir, "current.yml");
        seasonConfig = YamlConfiguration.loadConfiguration(seasonFile);

        if (!seasonConfig.contains("season")) {
            // Create new season
            currentSeason = 1;
            seasonStartMs = System.currentTimeMillis();
            seasonEndMs = seasonStartMs + (30L * 24 * 60 * 60 * 1000);

            seasonConfig.set("season", currentSeason);
            seasonConfig.set("start", seasonStartMs);
            seasonConfig.set("end", seasonEndMs);
            saveSeason();
            plugin.getLogger().info("Created Season " + currentSeason);
        } else {
            currentSeason = seasonConfig.getInt("season");
            seasonStartMs = seasonConfig.getLong("start");
            seasonEndMs = seasonConfig.getLong("end");

            // Check if season expired
            if (System.currentTimeMillis() >= seasonEndMs) {
                endSeason();
            }
        }
    }

    /**
     * Award Season Points after a dungeon clear.
     */
    public void awardSP(UUID playerId, String dungeonId, String difficultyId,
            long durationSeconds, int deaths, int partySize) {
        if (!isSeasonActive())
            return;

        // Base SP
        int sp = 50;

        // Difficulty multiplier
        sp += switch (difficultyId.toLowerCase()) {
            case "easy" -> 0;
            case "normal" -> 25;
            case "hard" -> 75;
            case "nightmare" -> 150;
            default -> 25;
        };

        // Speed bonus (under 5 min = +50, under 10 min = +25)
        if (durationSeconds < 300)
            sp += 50;
        else if (durationSeconds < 600)
            sp += 25;

        // Flawless bonus
        if (deaths == 0)
            sp += 40;

        // Party size bonus
        if (partySize >= 3)
            sp += 20;

        // Record
        String path = "players." + playerId;
        int current = seasonConfig.getInt(path + ".sp", 0);
        int newSP = current + sp;
        seasonConfig.set(path + ".sp", newSP);
        seasonConfig.set(path + ".clears",
                seasonConfig.getInt(path + ".clears", 0) + 1);
        seasonConfig.set(path + ".last-active", System.currentTimeMillis());
        saveSeason();

        // Notify player
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            SeasonRank oldRank = getRankForSP(current);
            SeasonRank newRank = getRankForSP(newSP);

            player.sendMessage(ChatUtils.colorize(
                    "&#FFD700&l🏆 &7+" + sp + " Season Points &8(Total: &f" + newSP + "&8)"));

            if (newRank != oldRank) {
                player.sendTitle(
                        ChatUtils.colorize("&#FFD700&lRANK UP!"),
                        ChatUtils.colorize(newRank.icon + " " + newRank.name), 10, 60, 20);
                player.sendMessage(ChatUtils.colorize(
                        "&#55FF55&l🔓 &7Rank naik ke " + newRank.icon + " &f" + newRank.name + "!"));
                player.playSound(player.getLocation(),
                        org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.2f);
            }
        }
    }

    public int getSP(UUID playerId) {
        return seasonConfig.getInt("players." + playerId + ".sp", 0);
    }

    public SeasonRank getRank(UUID playerId) {
        return getRankForSP(getSP(playerId));
    }

    public SeasonRank getRankForSP(int sp) {
        SeasonRank result = SeasonRank.BRONZE;
        for (SeasonRank rank : SeasonRank.values()) {
            if (sp >= rank.threshold)
                result = rank;
        }
        return result;
    }

    /**
     * Get top N players for this season.
     */
    public List<Map.Entry<UUID, Integer>> getLeaderboard(int limit) {
        Map<UUID, Integer> spMap = new HashMap<>();
        if (seasonConfig.getConfigurationSection("players") != null) {
            for (String key : seasonConfig.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    int sp = seasonConfig.getInt("players." + key + ".sp", 0);
                    spMap.put(uuid, sp);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        return spMap.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public boolean isSeasonActive() {
        return System.currentTimeMillis() < seasonEndMs;
    }

    public int getCurrentSeason() {
        return currentSeason;
    }

    public long getSeasonEndMs() {
        return seasonEndMs;
    }

    public long getRemainingMs() {
        return Math.max(0, seasonEndMs - System.currentTimeMillis());
    }

    public String getRemainingTimeFormatted() {
        long remaining = getRemainingMs();
        long days = remaining / (24 * 60 * 60 * 1000);
        long hours = (remaining % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000);
        return days + "d " + hours + "h";
    }

    private void endSeason() {
        // Archive current season
        File archive = new File(seasonDir, "season_" + currentSeason + ".yml");
        try {
            seasonConfig.save(archive);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to archive season: " + e.getMessage());
        }

        // Announce top 3
        List<Map.Entry<UUID, Integer>> top = getLeaderboard(3);
        Bukkit.broadcastMessage(ChatUtils.colorize(
                "&#FFD700&l🏆 Season " + currentSeason + " telah berakhir!"));
        for (int i = 0; i < top.size(); i++) {
            String name = Bukkit.getOfflinePlayer(top.get(i).getKey()).getName();
            Bukkit.broadcastMessage(ChatUtils.colorize(
                    "  &#FFD700#" + (i + 1) + " &f" + name + " &7— &f" +
                            top.get(i).getValue() + " SP"));
        }

        // Start new season
        currentSeason++;
        seasonStartMs = System.currentTimeMillis();
        seasonEndMs = seasonStartMs + (30L * 24 * 60 * 60 * 1000);
        seasonConfig = new YamlConfiguration();
        seasonConfig.set("season", currentSeason);
        seasonConfig.set("start", seasonStartMs);
        seasonConfig.set("end", seasonEndMs);
        saveSeason();

        plugin.getLogger().info("Season " + currentSeason + " started!");
    }

    private void saveSeason() {
        try {
            seasonConfig.save(seasonFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save season: " + e.getMessage());
        }
    }
}
