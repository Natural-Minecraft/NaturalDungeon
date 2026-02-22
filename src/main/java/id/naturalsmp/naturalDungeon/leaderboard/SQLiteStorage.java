package id.naturalsmp.naturaldungeon.leaderboard;

import id.naturalsmp.naturaldungeon.NaturalDungeon;

import java.io.File;
import java.sql.*;
import java.util.*;

/**
 * SQLite-backed leaderboard storage with weekly/monthly reset support.
 */
public class SQLiteStorage {

    private final NaturalDungeon plugin;
    private Connection connection;

    public SQLiteStorage(NaturalDungeon plugin) {
        this.plugin = plugin;
        initDatabase();
    }

    private void initDatabase() {
        try {
            File dbFile = new File(plugin.getDataFolder(), "leaderboard.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

            try (Statement stmt = connection.createStatement()) {
                // All-time leaderboard
                stmt.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS leaderboard (" +
                                "  uuid TEXT NOT NULL," +
                                "  dungeon_id TEXT NOT NULL," +
                                "  difficulty TEXT NOT NULL," +
                                "  time_ms BIGINT NOT NULL," +
                                "  deaths INTEGER NOT NULL DEFAULT 0," +
                                "  completed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                                "  PRIMARY KEY (uuid, dungeon_id, difficulty)" +
                                ")");

                // Weekly entries
                stmt.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS leaderboard_weekly (" +
                                "  uuid TEXT NOT NULL," +
                                "  dungeon_id TEXT NOT NULL," +
                                "  difficulty TEXT NOT NULL," +
                                "  time_ms BIGINT NOT NULL," +
                                "  deaths INTEGER NOT NULL DEFAULT 0," +
                                "  week_number INTEGER NOT NULL," +
                                "  completed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                                "  PRIMARY KEY (uuid, dungeon_id, difficulty, week_number)" +
                                ")");

                // Analytics
                stmt.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS dungeon_analytics (" +
                                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "  dungeon_id TEXT NOT NULL," +
                                "  event_type TEXT NOT NULL," +
                                "  duration_ms BIGINT," +
                                "  deaths INTEGER," +
                                "  stage INTEGER," +
                                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                                ")");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize SQLite leaderboard: " + e.getMessage());
        }
    }

    /**
     * Record a dungeon completion asynchronously to avoid blocking the main thread.
     */
    public void recordCompletion(UUID uuid, String dungeonId, String difficulty, long timeMs, int deaths) {
        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(
                org.bukkit.Bukkit.getPluginManager().getPlugin("NaturalDungeon"), () -> {
                    try (PreparedStatement stmt = connection.prepareStatement(
                            "INSERT INTO leaderboard (uuid, dungeon_id, difficulty, time_ms, deaths) VALUES (?, ?, ?, ?, ?) "
                                    +
                                    "ON CONFLICT(uuid, dungeon_id, difficulty) DO UPDATE SET time_ms = MIN(time_ms, ?), deaths = MIN(deaths, ?)")) {
                        stmt.setString(1, uuid.toString());
                        stmt.setString(2, dungeonId);
                        stmt.setString(3, difficulty);
                        stmt.setLong(4, timeMs);
                        stmt.setInt(5, deaths);
                        stmt.setLong(6, timeMs);
                        stmt.setInt(7, deaths);
                        stmt.executeUpdate();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // Also insert weekly
                    try (PreparedStatement stmt = connection.prepareStatement(
                            "INSERT INTO leaderboard_weekly (uuid, dungeon_id, difficulty, time_ms, deaths, week_number) VALUES (?, ?, ?, ?, ?, ?) "
                                    +
                                    "ON CONFLICT(uuid, dungeon_id, difficulty, week_number) DO UPDATE SET time_ms = MIN(time_ms, ?), deaths = MIN(deaths, ?)")) {
                        stmt.setString(1, uuid.toString());
                        stmt.setString(2, dungeonId);
                        stmt.setString(3, difficulty);
                        stmt.setLong(4, timeMs);
                        stmt.setInt(5, deaths);
                        stmt.setInt(6, getWeekNumber());
                        stmt.setLong(7, timeMs);
                        stmt.setInt(8, deaths);
                        stmt.executeUpdate();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    /**
     * Record an analytics event (START, CLEAR, WIPE, DEATH).
     */
    public void recordAnalytics(String dungeonId, String eventType, long durationMs, int deaths, int stage) {
        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(
                org.bukkit.Bukkit.getPluginManager().getPlugin("NaturalDungeon"), () -> {
                    try (PreparedStatement stmt = connection.prepareStatement(
                            "INSERT INTO dungeon_analytics (dungeon_id, event_type, duration_ms, deaths, stage) VALUES (?, ?, ?, ?, ?)")) {
                        stmt.setString(1, dungeonId);
                        stmt.setString(2, eventType);
                        stmt.setLong(3, durationMs);
                        stmt.setInt(4, deaths);
                        stmt.setInt(5, stage);
                        stmt.executeUpdate();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    /**
     * Get top entries for a dungeon+difficulty (all-time).
     */
    public List<LeaderboardEntry> getTopEntries(String dungeonId, String difficulty, int limit) {
        return queryEntries(
                "SELECT uuid, time_ms, deaths FROM leaderboard WHERE dungeon_id = ? AND difficulty = ? ORDER BY time_ms ASC LIMIT ?",
                dungeonId, difficulty, limit);
    }

    /**
     * Get personal best time for a specific player and dungeon.
     * Returns -1 if no record exists.
     */
    public long getPersonalBest(UUID uuid, String dungeonId) {
        long bestTime = -1;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT MIN(time_ms) as pb FROM leaderboard WHERE uuid = ? AND dungeon_id = ?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, dungeonId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long pb = rs.getLong("pb");
                    if (!rs.wasNull()) {
                        bestTime = pb;
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to query personal best: " + e.getMessage());
        }
        return bestTime;
    }

    /**
     * Get top entries for current week.
     */
    public List<LeaderboardEntry> getWeeklyTopEntries(String dungeonId, String difficulty, int limit) {
        List<LeaderboardEntry> entries = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT uuid, time_ms, deaths FROM leaderboard_weekly WHERE dungeon_id = ? AND difficulty = ? AND week_number = ? ORDER BY time_ms ASC LIMIT ?")) {
            ps.setString(1, dungeonId);
            ps.setString(2, difficulty);
            ps.setInt(3, getWeekNumber());
            ps.setInt(4, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    entries.add(new LeaderboardEntry(
                            UUID.fromString(rs.getString("uuid")),
                            rs.getLong("time_ms"),
                            rs.getInt("deaths")));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to query weekly leaderboard: " + e.getMessage());
        }
        return entries;
    }

    private List<LeaderboardEntry> queryEntries(String sql, String dungeonId, String difficulty, int limit) {
        List<LeaderboardEntry> entries = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, dungeonId);
            ps.setString(2, difficulty);
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    entries.add(new LeaderboardEntry(
                            UUID.fromString(rs.getString("uuid")),
                            rs.getLong("time_ms"),
                            rs.getInt("deaths")));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to query leaderboard: " + e.getMessage());
        }
        return entries;
    }

    /**
     * Check if a player has cleared a specific dungeon+difficulty.
     */
    public boolean hasCleared(UUID uuid, String dungeonId, String difficulty) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT 1 FROM leaderboard WHERE uuid = ? AND dungeon_id = ? AND difficulty = ?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, dungeonId);
            ps.setString(3, difficulty);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private int getWeekNumber() {
        Calendar cal = Calendar.getInstance();
        return cal.get(Calendar.YEAR) * 100 + cal.get(Calendar.WEEK_OF_YEAR);
    }

    public int getAnalyticsCount(String dungeonId, String eventType) {
        int count = 0;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) AS total FROM dungeon_analytics WHERE dungeon_id = ? AND event_type = ?")) {
            ps.setString(1, dungeonId);
            ps.setString(2, eventType);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    count = rs.getInt("total");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to query analytics count: " + e.getMessage());
        }
        return count;
    }

    public long getAnalyticsAverageClearTime(String dungeonId) {
        long avg = 0;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT AVG(duration_ms) AS avg_time FROM dungeon_analytics WHERE dungeon_id = ? AND event_type = 'CLEAR'")) {
            ps.setString(1, dungeonId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    avg = rs.getLong("avg_time");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to query analytics average time: " + e.getMessage());
        }
        return avg;
    }

    public Map<Integer, Integer> getDeathHeatmap(String dungeonId) {
        Map<Integer, Integer> heatmap = new HashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT stage, SUM(deaths) AS total_deaths FROM dungeon_analytics WHERE dungeon_id = ? AND deaths > 0 GROUP BY stage")) {
            ps.setString(1, dungeonId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    heatmap.put(rs.getInt("stage"), rs.getInt("total_deaths"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to query death heatmap: " + e.getMessage());
        }
        return heatmap;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to close SQLite: " + e.getMessage());
        }
    }

    public static class LeaderboardEntry {
        private final UUID uuid;
        private final long timeMs;
        private final int deaths;

        public LeaderboardEntry(UUID uuid, long timeMs, int deaths) {
            this.uuid = uuid;
            this.timeMs = timeMs;
            this.deaths = deaths;
        }

        public UUID getUuid() {
            return uuid;
        }

        public long getTimeMs() {
            return timeMs;
        }

        public int getDeaths() {
            return deaths;
        }
    }
}
