package id.naturalsmp.naturaldungeon.skill.impl;

import id.naturalsmp.naturaldungeon.skill.MobSkill;
import id.naturalsmp.naturaldungeon.skill.SkillCategory;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;

/**
 * Ranged projectile skill — launches a projectile toward the target.
 */
public class ProjectileSkill implements MobSkill {

    private final String id;
    private final String displayName;
    private final SkillCategory category;
    private final int cooldownTicks;
    private final double range;
    private final double damage;

    public ProjectileSkill(String id, String displayName, SkillCategory category, int cooldownTicks, double range,
            double damage) {
        this.id = id;
        this.displayName = displayName;
        this.category = category;
        this.cooldownTicks = cooldownTicks;
        this.range = range;
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
        return category;
    }

    @Override
    public int getCooldownTicks() {
        return cooldownTicks;
    }

    @Override
    public double getRange() {
        return range;
    }

    @Override
    public void execute(LivingEntity caster, Player target) {
        if (target == null)
            return;

        Location origin = caster.getEyeLocation();
        Vector direction = target.getEyeLocation().toVector().subtract(origin.toVector()).normalize();

        // Choose projectile by skill type
        if (id.contains("fireball") || id.contains("fire")) {
            SmallFireball fb = caster.getWorld().spawn(origin, SmallFireball.class);
            fb.setDirection(direction);
            fb.setShooter(caster);
        } else if (id.contains("arrow") || id.contains("soul_arrow") || id.contains("spectral")) {
            Arrow arrow = caster.getWorld().spawn(origin, Arrow.class);
            arrow.setVelocity(direction.multiply(2.0));
            arrow.setShooter(caster);
            arrow.setDamage(damage);
        } else {
            // Default: snowball-like projectile
            Snowball sb = caster.getWorld().spawn(origin, Snowball.class);
            sb.setVelocity(direction.multiply(1.5));
            sb.setShooter(caster);
            // Damage is applied on hit via listener
        }
    }
}
