package id.naturalsmp.naturaldungeon.skill;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * Interface for all mob skills in the dungeon system.
 */
public interface MobSkill {

    /**
     * Unique skill ID (lowercase, underscores).
     */
    String getId();

    /**
     * Display name for GUI/editing.
     */
    String getDisplayName();

    /**
     * Skill category.
     */
    SkillCategory getCategory();

    /**
     * Cooldown in ticks between uses.
     */
    int getCooldownTicks();

    /**
     * Activation range in blocks.
     */
    double getRange();

    /**
     * Execute the skill. Called by SkillExecutor.
     * 
     * @param caster The mob casting the skill.
     * @param target Nearest player target (may be null).
     */
    void execute(LivingEntity caster, Player target);

    /**
     * Whether the skill can currently execute.
     */
    default boolean canExecute(LivingEntity caster, Player target) {
        if (target == null)
            return false;
        return caster.getLocation().distanceSquared(target.getLocation()) <= getRange() * getRange();
    }
}
