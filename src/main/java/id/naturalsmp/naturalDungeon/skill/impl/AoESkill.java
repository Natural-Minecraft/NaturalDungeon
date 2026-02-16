package id.naturalsmp.naturaldungeon.skill.impl;

import id.naturalsmp.naturaldungeon.skill.MobSkill;
import id.naturalsmp.naturaldungeon.skill.SkillCategory;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Collection;

/**
 * Area-of-Effect skill — damages all players within radius.
 */
public class AoESkill implements MobSkill {

    private final String id;
    private final String displayName;
    private final int cooldownTicks;
    private final double radius;
    private final double damage;

    public AoESkill(String id, String displayName, int cooldownTicks, double radius, double damage) {
        this.id = id;
        this.displayName = displayName;
        this.cooldownTicks = cooldownTicks;
        this.radius = radius;
        this.damage = damage;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public SkillCategory getCategory() {
        return SkillCategory.AOE;
    }

    @Override
    public int getCooldownTicks() {
        return cooldownTicks;
    }

    @Override
    public double getRange() {
        return radius;
    }

    @Override
    public void execute(LivingEntity caster, Player target) {
        Location center = caster.getLocation();

        // Particle effect
        caster.getWorld().spawnParticle(Particle.EXPLOSION, center, 10, radius / 2, 1, radius / 2);
        caster.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.8f);

        // Damage nearby players
        Collection<org.bukkit.entity.Entity> nearby = caster.getWorld().getNearbyEntities(center, radius, radius,
                radius);
        for (org.bukkit.entity.Entity e : nearby) {
            if (e instanceof Player p && !p.isDead()) {
                double dist = p.getLocation().distance(center);
                if (dist <= radius) {
                    // Damage falloff by distance
                    double mult = 1.0 - (dist / radius) * 0.5;
                    p.damage(damage * mult, caster);
                }
            }
        }
    }
}
