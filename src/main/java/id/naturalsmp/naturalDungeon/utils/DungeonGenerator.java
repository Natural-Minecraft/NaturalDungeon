package id.naturalsmp.naturaldungeon.utils;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.dungeon.Dungeon;
import id.naturalsmp.naturaldungeon.dungeon.DungeonType;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DungeonGenerator {

    private final NaturalDungeon plugin;

    public DungeonGenerator(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public boolean generate(String id, DungeonType type, Player creator) {
        File file = new File(plugin.getDataFolder(), "dungeons/" + id + ".yml");
        if (file.exists()) {
            return false;
        }

        try {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            file.createNewFile();

            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            populateConfig(config, id, type, creator);
            config.save(file);

            // Reload to register
            plugin.getDungeonManager().loadDungeons();
            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void populateConfig(YamlConfiguration config, String id, DungeonType type, Player p) {
        Location loc = p.getLocation();

        // Basic Info
        config.set("display-name", "&a" + formatId(id));
        config.set("description", Arrays.asList("&7A generated dungeon.", "&7Type: " + type.name()));
        config.set("world", loc.getWorld().getName());
        config.set("max-players", 4);
        config.set("material", type == DungeonType.BOSS_RUSH ? "WITHER_SKELETON_SKULL" : "SPAWNER");
        config.set("cooldown", 3600);

        // Difficulties (Standard Normal)
        config.set("difficulties.normal.min-tier", 1);
        config.set("difficulties.normal.key-req", "none");

        // Stages & Waves based on Type
        String path = "difficulties.normal.stages.1";

        // Locations (Instance 1)
        config.set(path + ".locations.1.safe-zone", serializeLoc(loc));
        config.set(path + ".locations.1.boss-spawn", serializeLoc(loc.clone().add(10, 0, 10))); // Offset boss

        // Mob Spawns (Instance 1)
        List<String> mobSpawns = new ArrayList<>();
        mobSpawns.add(serializeLoc(loc.clone().add(5, 0, 5)));
        mobSpawns.add(serializeLoc(loc.clone().add(-5, 0, -5)));
        mobSpawns.add(serializeLoc(loc.clone().add(5, 0, -5)));
        mobSpawns.add(serializeLoc(loc.clone().add(-5, 0, 5)));
        config.set(path + ".locations.1.mob-spawns", mobSpawns);

        // Wave Logic
        if (type == DungeonType.BASIC) {
            createWave(config, path, 0, 5, 5, "ZOMBIE", "SKELETON");
            createWave(config, path, 1, 8, 10, "ZOMBIE", "SPIDER");
            createWave(config, path, 2, 10, 10, "ZOMBIE", "SKELETON", "CREEPER");
        } else if (type == DungeonType.WAVE_DEFENSE) {
            for (int i = 0; i < 10; i++) {
                int count = 5 + (i * 2);
                createWave(config, path, i, count, 5, "ZOMBIE", "SKELETON");
            }
        } else if (type == DungeonType.BOSS_RUSH) {
            config.set(path + ".boss.id", "DungeonBoss"); // Placeholder boss
            createWave(config, path, 0, 1, 5, "WITHER_SKELETON"); // Minion
        }
    }

    private void createWave(YamlConfiguration config, String root, int index, int count, int delay, String... mobs) {
        String path = root + ".waves." + index;
        config.set(path + ".mobs", Arrays.asList(mobs));
        config.set(path + ".count", count);
        config.set(path + ".delay", delay);
    }

    private String serializeLoc(Location loc) {
        return loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ() + "," + loc.getYaw()
                + "," + loc.getPitch();
    }

    private String formatId(String id) {
        String[] parts = id.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0)
                sb.append(" ");
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase());
        }
        return sb.toString();
    }
}
