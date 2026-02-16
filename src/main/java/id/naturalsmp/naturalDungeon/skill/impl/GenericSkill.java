package id.naturalsmp.naturaldungeon.skill.impl;

import id.naturalsmp.naturaldungeon.skill.MobSkill;
import id.naturalsmp.naturaldungeon.skill.SkillCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * Generic skill — deals direct damage or applies a simple effect.
 * Used for melee, movement, defense, special, and boss skills.
 */
public class GenericSkill implements MobSkill {

    private final String id;
    private final String displayName;
    private final SkillCategory category;
    private final int cooldownTicks;
    private final double range;
    private final double damage;

    public GenericSkill(String id, String displayName, SkillCategory category, int cooldownTicks, double range,
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

        switch (category) {
            case MELEE -> {
                target.damage(damage, caster);
            }
            case MOVEMENT -> {
                // Teleport/dash to near player
                org.bukkit.Location loc = target.getLocation();
                loc.add(loc.getDirection().multiply(-2));
                caster.teleport(loc);
            }
            case DEFENSE -> {
                // Self-heal
                double maxHp = caster.getMaxHealth();
                double healAmt = maxHp * 0.1;
                caster.setHealth(Math.min(maxHp, caster.getHealth() + healAmt));
            }
            case SPECIAL, BOSS -> {
                // Damage + knockback
                target.damage(damage, caster);
                org.bukkit.util.Vector kb = target.getLocation().toVector()
                        .subtract(caster.getLocation().toVector()).normalize().multiply(1.5);
                kb.setY(0.5);
                target.setVelocity(kb);
            }
            default -> target.damage(damage, caster);
        }
    }
}
