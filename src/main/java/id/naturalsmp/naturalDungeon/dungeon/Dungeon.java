package id.naturalsmp.naturaldungeon.dungeon;

import org.bukkit.configuration.ConfigurationSection;
import java.util.*;

public class Dungeon {

    private final String id;
    private final String displayName;
    private final String world;
    private final int maxPlayers;
    private final String material;
    private final List<String> description;
    private final long cooldownSeconds;
    private final Map<String, DungeonDifficulty> difficulties;

    public Dungeon(String id, ConfigurationSection config) {
        this.id = id;
        this.displayName = config.getString("display-name", id);
        this.world = config.getString("world", "dungeon");
        this.maxPlayers = config.getInt("max-players", 4);
        this.material = config.getString("material", "SPAWNER");
        this.description = config.getStringList("description");
        this.cooldownSeconds = config.getLong("cooldown", 3600);
        this.difficulties = new LinkedHashMap<>();

        ConfigurationSection diffSection = config.getConfigurationSection("difficulties");
        if (diffSection != null) {
            for (String key : diffSection.getKeys(false)) {
                difficulties.put(key, new DungeonDifficulty(key, diffSection.getConfigurationSection(key)));
            }
        }

        // Backwards compatibility
        if (difficulties.isEmpty() && config.contains("stages")) {
            difficulties.put("normal", new DungeonDifficulty("normal", config));
        }
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getWorld() {
        return world;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public String getMaterial() {
        return material;
    }

    public List<String> getDescription() {
        return description;
    }

    public long getCooldownSeconds() {
        return cooldownSeconds;
    }

    public DungeonDifficulty getDifficulty(String id) {
        return difficulties.get(id);
    }

    public Collection<DungeonDifficulty> getDifficulties() {
        return difficulties.values();
    }

    public boolean hasDifficulty(String id) {
        return difficulties.containsKey(id);
    }

    public int getMinTier() {
        if (difficulties.containsKey("normal"))
            return difficulties.get("normal").getMinTier();
        return difficulties.isEmpty() ? 1 : difficulties.values().iterator().next().getMinTier();
    }

    public int getTotalStages() {
        if (difficulties.containsKey("normal"))
            return difficulties.get("normal").getTotalStages();
        return difficulties.isEmpty() ? 0 : difficulties.values().iterator().next().getTotalStages();
    }

    public List<Stage> getStages() {
        // Fallback for older code mostly
        if (difficulties.containsKey("normal"))
            return difficulties.get("normal").getStages();
        return difficulties.isEmpty() ? Collections.emptyList() : difficulties.values().iterator().next().getStages();
    }

    public static class Stage {
        private final int number;
        private final List<Wave> waves;
        private final String bossId;
        private final Map<Integer, StageLocation> locations = new HashMap<>();

        public Stage(int number, ConfigurationSection config) {
            this.number = number;
            this.bossId = config.getString("boss.id", null);
            this.waves = new ArrayList<>();

            List<?> wavesList = config.getList("waves");
            if (wavesList != null) {
                for (int i = 0; i < wavesList.size(); i++) {
                    ConfigurationSection waveSection = config.getConfigurationSection("waves." + i);
                    if (waveSection != null)
                        waves.add(new Wave(waveSection));
                }
            }

            // Parse locations
            ConfigurationSection locs = config.getConfigurationSection("locations");
            if (locs != null) {
                for (String key : locs.getKeys(false)) {
                    try {
                        int id = Integer.parseInt(key);
                        locations.put(id, new StageLocation(locs.getConfigurationSection(key)));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            // Fallback for single instance (Legacy/Default)
            if (locations.isEmpty() || !locations.containsKey(1)) {
                if (config.contains("safe-zone")) {
                    locations.put(1, new StageLocation(config, true));
                }
            }
        }

        public Stage(int number, DungeonDifficulty diff, ConfigurationSection config) {
            this(number, config);
        }

        public int getNumber() {
            return number;
        }

        public List<Wave> getWaves() {
            return waves;
        }

        public boolean hasBoss() {
            return bossId != null && !bossId.isEmpty();
        }

        public String getBossId() {
            return bossId;
        }

        public StageLocation getLocation(int instanceId) {
            // Default to instance 1 if specific instance not found (fallback)
            return locations.getOrDefault(instanceId, locations.get(1));
        }

        public Set<Integer> getConfiguredInstances() {
            return locations.keySet();
        }
    }

    public static class StageLocation {
        private final String safeZone;
        private final String arenaRegion; // [NEW]
        private final List<Double> bossSpawnLocation;

        public StageLocation(ConfigurationSection config) {
            this(config, false);
        }

        public StageLocation(ConfigurationSection config, boolean legacy) {
            this.safeZone = config.getString("safe-zone", "");
            this.arenaRegion = config.getString("arena-region", ""); // [NEW]
            if (legacy) {
                this.bossSpawnLocation = config.getDoubleList("boss.spawn-location");
            } else {
                this.bossSpawnLocation = config.getDoubleList("boss-spawn");
            }
        }

        public String getSafeZone() {
            return safeZone;
        }

        public String getArenaRegion() {
            return arenaRegion;
        }

        public List<Double> getBossSpawnLocation() {
            return bossSpawnLocation;
        }
    }

    public static class Wave {
        private final List<String> mobs;
        private final int count;
        private final int delay;

        public Wave(ConfigurationSection config) {
            this.mobs = config.getStringList("mobs");
            this.count = config.getInt("count", 5);
            this.delay = config.getInt("delay", 5);
        }

        public Wave(List<String> mobs, int count, int delay) {
            this.mobs = mobs;
            this.count = count;
            this.delay = delay;
        }

        public List<String> getMobs() {
            return mobs;
        }

        public int getCount() {
            return count;
        }

        public int getDelay() {
            return delay;
        }
    }
}
