package id.naturalsmp.naturaldungeon.dungeon;

import org.bukkit.configuration.ConfigurationSection;
import java.util.*;

public class DungeonDifficulty {

    private final String id;
    private final String displayName;
    private final int minTier;
    private final String keyReq; // Format: TYPE:ID:AMOUNT (e.g., MMOITEMS:MATERIAL:KEY:1)
    private final int maxDeaths;
    private final double rewardMultiplier;
    private final List<Dungeon.Stage> stages;
    private final ConfigurationSection lootSection;

    public DungeonDifficulty(String id, ConfigurationSection config) {
        this.id = id;
        this.displayName = config.getString("display", id);
        this.minTier = config.getInt("min-tier", 1);
        this.keyReq = config.getString("key-req", null);
        this.maxDeaths = config.getInt("max-deaths", 3);
        this.rewardMultiplier = config.getDouble("reward-multiplier", 1.0);
        this.lootSection = config.getConfigurationSection("loot");
        this.stages = new ArrayList<>();

        ConfigurationSection stagesSection = config.getConfigurationSection("stages");
        if (stagesSection != null) {
            for (String stageKey : stagesSection.getKeys(false)) {
                stages.add(
                        new Dungeon.Stage(Integer.parseInt(stageKey), stagesSection.getConfigurationSection(stageKey)));
            }
        }
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMinTier() {
        return minTier;
    }

    public String getKeyReq() {
        return keyReq;
    }

    public int getMaxDeaths() {
        return maxDeaths;
    }

    public List<Dungeon.Stage> getStages() {
        return stages;
    }

    public ConfigurationSection getLootSection() {
        return lootSection;
    }

    public double getRewardMultiplier() {
        return rewardMultiplier;
    }

    public int getTotalStages() {
        return stages.size();
    }
}
