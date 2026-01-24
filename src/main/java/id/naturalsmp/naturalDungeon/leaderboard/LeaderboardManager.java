package id.naturalsmp.naturaldungeon.leaderboard;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class LeaderboardManager {

    private final NaturalDungeon plugin;
    private final File file;
    private YamlConfiguration config;

    public LeaderboardManager(NaturalDungeon plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "leaderboards.yml");
        load();
    }

    public void load() {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addEntry(String dungeonId, List<String> playerNames, long timeMs, String rank) {
        String path = dungeonId + ".entries";
        List<Map<?, ?>> entries = config.getMapList(path);

        Map<String, Object> entry = new HashMap<>();
        entry.put("players", playerNames);
        entry.put("time", timeMs);
        entry.put("rank", rank);
        entry.put("date", System.currentTimeMillis());

        entries.add(entry);

        // Sort by time (lowest first) and keep top 10
        List<Map<?, ?>> sorted = entries.stream()
                .sorted(Comparator.comparingLong(m -> (long) m.get("time")))
                .limit(10)
                .collect(Collectors.toList());

        config.set(path, sorted);
        save();
    }

    public List<Map<?, ?>> getTopEntries(String dungeonId) {
        return config.getMapList(dungeonId + ".entries");
    }
}
