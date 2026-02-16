package id.naturalsmp.naturaldungeon.mob;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.metadata.FixedMetadataValue;

/**
 * Spawns custom mobs with configured stats.
 */
public class CustomMobSpawner {

    private final NaturalDungeon plugin;

    public CustomMobSpawner(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    /**
     * Spawn a custom mob at the given location.
     * 
     * @return The spawned entity, or null if mob not found.
     */
    public LivingEntity spawnMob(String mobId, Location location) {
        CustomMob customMob = plugin.getCustomMobManager().getMob(mobId);
        if (customMob == null)
            return null;

        Entity entity = location.getWorld().spawnEntity(location, customMob.getEntityType());
        if (!(entity instanceof LivingEntity living)) {
            entity.remove();
            return null;
        }

        // Set display name
        living.setCustomName(id.naturalsmp.naturaldungeon.utils.ChatUtils.colorize(
                (customMob.isBoss() ? "&c&l" : "&e") + customMob.getName()));
        living.setCustomNameVisible(true);

        // Set health
        if (living.getAttribute(Attribute.MAX_HEALTH) != null) {
            living.getAttribute(Attribute.MAX_HEALTH).setBaseValue(customMob.getHealth());
            living.setHealth(customMob.getHealth());
        }

        // Set attack damage
        if (living.getAttribute(Attribute.ATTACK_DAMAGE) != null) {
            living.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(customMob.getDamage());
        }

        // Set speed
        if (living.getAttribute(Attribute.MOVEMENT_SPEED) != null) {
            living.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(customMob.getSpeed());
        }

        // Tag for dungeon system
        living.setMetadata("dungeon_mob", new FixedMetadataValue(plugin, true));
        living.setMetadata("custom_mob_id", new FixedMetadataValue(plugin, mobId));
        if (customMob.isBoss()) {
            living.setMetadata("dungeon_boss", new FixedMetadataValue(plugin, true));
        }

        // Assign skills metadata
        if (!customMob.getSkillIds().isEmpty()) {
            living.setMetadata("mob_skills", new FixedMetadataValue(plugin,
                    String.join(",", customMob.getSkillIds())));
        }

        return living;
    }
}
