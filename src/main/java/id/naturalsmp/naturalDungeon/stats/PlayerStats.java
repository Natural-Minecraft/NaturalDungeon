package id.naturalsmp.naturaldungeon.stats;

import java.util.*;

/**
 * Tracks per-player dungeon statistics.
 */
public class PlayerStats {

    private final UUID playerId;
    private int totalClears = 0;
    private int totalDeaths = 0;
    private int flawlessClears = 0;
    private long fastestTime = Long.MAX_VALUE; // milliseconds
    private double totalDamageDealt = 0;
    private double totalDamageTaken = 0;

    // Daily/Weekly Trackers
    private int dailyClears = 0;
    private long lastDailyReset = 0;
    private int weeklyClears = 0;
    private long lastWeeklyReset = 0;

    private final Map<String, Integer> dungeonClears = new HashMap<>(); // dungeonId -> count

    public PlayerStats(UUID playerId) {
        this.playerId = playerId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public int getTotalClears() {
        return totalClears;
    }

    public int getTotalDeaths() {
        return totalDeaths;
    }

    public int getFlawlessClears() {
        return flawlessClears;
    }

    public long getFastestTime() {
        return fastestTime;
    }

    public double getTotalDamageDealt() {
        return totalDamageDealt;
    }

    public double getTotalDamageTaken() {
        return totalDamageTaken;
    }

    public Map<String, Integer> getDungeonClears() {
        return new HashMap<>(dungeonClears);
    }

    public void addClear(String dungeonId, long timeMs, int deaths) {
        totalClears++;
        totalDeaths += deaths;
        if (deaths == 0)
            flawlessClears++;
        if (timeMs < fastestTime)
            fastestTime = timeMs;
        dungeonClears.merge(dungeonId, 1, Integer::sum);

        checkResets();
        dailyClears++;
        weeklyClears++;
    }

    public void checkResets() {
        Calendar cal = Calendar.getInstance();

        // Daily reset (midnight)
        Calendar dailyReset = Calendar.getInstance();
        dailyReset.setTimeInMillis(lastDailyReset);
        if (cal.get(Calendar.DAY_OF_YEAR) != dailyReset.get(Calendar.DAY_OF_YEAR)
                || cal.get(Calendar.YEAR) != dailyReset.get(Calendar.YEAR)) {
            dailyClears = 0;
            lastDailyReset = System.currentTimeMillis();
        }

        // Weekly reset (Monday)
        Calendar weeklyReset = Calendar.getInstance();
        weeklyReset.setTimeInMillis(lastWeeklyReset);
        if (cal.get(Calendar.WEEK_OF_YEAR) != weeklyReset.get(Calendar.WEEK_OF_YEAR)
                || cal.get(Calendar.YEAR) != weeklyReset.get(Calendar.YEAR)) {
            weeklyClears = 0;
            lastWeeklyReset = System.currentTimeMillis();
        }
    }

    public void addDamageDealt(double damage) {
        totalDamageDealt += damage;
    }

    public void addDamageTaken(double damage) {
        totalDamageTaken += damage;
    }

    // For loading from file
    public void setTotalClears(int v) {
        this.totalClears = v;
    }

    public void setTotalDeaths(int v) {
        this.totalDeaths = v;
    }

    public void setFlawlessClears(int v) {
        this.flawlessClears = v;
    }

    public void setFastestTime(long v) {
        this.fastestTime = v;
    }

    public void setTotalDamageDealt(double v) {
        this.totalDamageDealt = v;
    }

    public void setTotalDamageTaken(double v) {
        this.totalDamageTaken = v;
    }

    public int getDailyClears() {
        checkResets();
        return dailyClears;
    }

    public void setDailyClears(int dailyClears) {
        this.dailyClears = dailyClears;
    }

    public long getLastDailyReset() {
        return lastDailyReset;
    }

    public void setLastDailyReset(long lastDailyReset) {
        this.lastDailyReset = lastDailyReset;
    }

    public int getWeeklyClears() {
        checkResets();
        return weeklyClears;
    }

    public void setWeeklyClears(int weeklyClears) {
        this.weeklyClears = weeklyClears;
    }

    public long getLastWeeklyReset() {
        return lastWeeklyReset;
    }

    public void setLastWeeklyReset(long lastWeeklyReset) {
        this.lastWeeklyReset = lastWeeklyReset;
    }

    public void setDungeonClears(Map<String, Integer> map) {
        dungeonClears.clear();
        dungeonClears.putAll(map);
    }
}
