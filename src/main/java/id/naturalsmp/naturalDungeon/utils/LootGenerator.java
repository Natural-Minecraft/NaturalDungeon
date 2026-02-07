package id.naturalsmp.naturaldungeon.utils;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LootGenerator {

    private final NaturalDungeon plugin;

    public LootGenerator(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public boolean generate(String id, int tier, Player requester) {
        File file = new File(plugin.getDataFolder(), "dungeons/" + id + ".yml");
        if (!file.exists()) {
            return false;
        }

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

            // Clear existing loot
            config.set("loot", null);

            // Generate new loot
            generateLootTable(config, "loot.completion", tier, false); // Chest reward
            generateLootTable(config, "loot.boss", tier, true); // Boss drops

            config.save(file);
            plugin.getDungeonManager().loadDungeons(); // Reload
            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void generateLootTable(YamlConfiguration config, String path, int tier, boolean isBoss) {
        int itemsCount = isBoss ? 3 : 5; // Boss drops fewer but better items

        for (int i = 0; i < itemsCount; i++) {
            Map<String, Object> itemData = rollItem(tier, isBoss);
            String itemPath = path + "." + i;

            config.set(itemPath + ".type", itemData.get("type"));
            config.set(itemPath + ".chance", itemData.get("chance"));
            config.set(itemPath + ".amount", itemData.get("amount"));

            if (itemData.containsKey("material")) {
                config.set(itemPath + ".material", itemData.get("material"));
            }
            if (itemData.containsKey("id")) {
                config.set(itemPath + ".id", itemData.get("id"));
            }
        }
    }

    private Map<String, Object> rollItem(int tier, boolean isBoss) {
        Map<String, Object> map = new HashMap<>();
        double roll = Math.random();

        // Rarity Distribution based on Tier
        // Tier 1: 80% Common, 19% Rare, 1% Legendary
        // Tier 10: 40% Common, 40% Rare, 20% Legendary
        double legendaryChance = 0.01 + (tier * 0.02);
        double rareChance = 0.20 + (tier * 0.02);

        if (isBoss) {
            legendaryChance *= 2; // Boss doubles legendary chance
        }

        if (roll < legendaryChance) {
            // LEGENDARY
            map.put("chance", isBoss ? 100 : 10);
            if (tier >= 7) {
                map.put("type", "VANILLA");
                map.put("material", "NETHERITE_SCRAP");
                map.put("amount", "1");
            } else {
                map.put("type", "VANILLA");
                map.put("material", "DIAMOND");
                map.put("amount", "1-3");
            }
        } else if (roll < legendaryChance + rareChance) {
            // RARE
            map.put("chance", isBoss ? 100 : 30);
            map.put("type", "VANILLA");
            if (tier >= 5) {
                map.put("material", "GOLD_INGOT");
                map.put("amount", "5-10");
            } else {
                map.put("material", "IRON_INGOT");
                map.put("amount", "5-10");
            }
        } else {
            // COMMON
            map.put("chance", 50);
            map.put("type", "VANILLA");
            if (tier >= 5) {
                map.put("material", "IRON_NUGGET");
                map.put("amount", "10-20");
            } else {
                map.put("material", "COAL");
                map.put("amount", "5-10");
            }
        }

        // MMOItems Placeholder (If we had a list of IDs)
        // if (roll < 0.05) { ... }

        return map;
    }
}
