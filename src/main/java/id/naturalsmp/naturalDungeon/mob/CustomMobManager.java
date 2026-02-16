package id.naturalsmp.naturaldungeon.mob;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Manages custom mob definitions (YAML persistence).
 */
public class CustomMobManager {

    private final NaturalDungeon plugin;
    private final Map<String, CustomMob> mobs = new LinkedHashMap<>();
    private final File dataFile;

    public CustomMobManager(NaturalDungeon plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "custom_mobs.yml");
        loadAll();
    }

    public void loadAll() {
        mobs.clear();
        if (!dataFile.exists())
            return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);

        for (String key : config.getKeys(false)) {
            ConfigurationSection sec = config.getConfigurationSection(key);
            if (sec == null)
                continue;

            CustomMob mob = new CustomMob(key);
            mob.setName(sec.getString("name", key));
            try {
                mob.setEntityType(EntityType.valueOf(sec.getString("entity-type", "ZOMBIE")));
            } catch (IllegalArgumentException e) {
                mob.setEntityType(EntityType.ZOMBIE);
            }
            mob.setHealth(sec.getDouble("health", 20.0));
            mob.setDamage(sec.getDouble("damage", 5.0));
            mob.setSpeed(sec.getDouble("speed", 0.23));
            mob.setBoss(sec.getBoolean("boss", false));
            mob.setSkillIds(sec.getStringList("skills"));
            mobs.put(key, mob);
        }
    }

    public void saveAll() {
        YamlConfiguration config = new YamlConfiguration();
        for (CustomMob mob : mobs.values()) {
            String key = mob.getId();
            config.set(key + ".name", mob.getName());
            config.set(key + ".entity-type", mob.getEntityType().name());
            config.set(key + ".health", mob.getHealth());
            config.set(key + ".damage", mob.getDamage());
            config.set(key + ".speed", mob.getSpeed());
            config.set(key + ".boss", mob.isBoss());
            config.set(key + ".skills", mob.getSkillIds());
        }
        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save custom_mobs.yml: " + e.getMessage());
        }
    }

    public CustomMob getMob(String id) {
        return mobs.get(id);
    }

    public Collection<CustomMob> getAllMobs() {
        return mobs.values();
    }

    public void createMob(String id) {
        if (!mobs.containsKey(id)) {
            mobs.put(id, new CustomMob(id));
            saveAll();
        }
    }

    public void deleteMob(String id) {
        mobs.remove(id);
        saveAll();
    }

    public void updateMob(CustomMob mob) {
        mobs.put(mob.getId(), mob);
        saveAll();
    }
}
