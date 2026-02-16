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

    public void setDungeonClears(Map<String, Integer> map) {
        dungeonClears.clear();
        dungeonClears.putAll(map);
    }
}
