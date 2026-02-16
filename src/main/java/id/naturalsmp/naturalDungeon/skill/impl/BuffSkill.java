package id.naturalsmp.naturaldungeon.skill.impl;

import id.naturalsmp.naturaldungeon.skill.MobSkill;
import id.naturalsmp.naturaldungeon.skill.SkillCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Buff skill — applies a positive potion effect to the mob (self-buff).
 */
public class BuffSkill implements MobSkill {

    private final String id;
    private final String displayName;
    private final int cooldownTicks;
    private final int potionTypeId;
    private final int durationTicks;

    public BuffSkill(String id, String displayName, int cooldownTicks, int potionTypeId, int durationTicks) {
        this.id = id;
        this.displayName = displayName;
        this.cooldownTicks = cooldownTicks;
        this.potionTypeId = potionTypeId;
        this.durationTicks = durationTicks;
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
        return SkillCategory.BUFF;
    }

    @Override
    public int getCooldownTicks() {
        return cooldownTicks;
    }

    @Override
    public double getRange() {
        return 0;
    } // Self-targeting

    @Override
    public boolean canExecute(LivingEntity caster, Player target) {
        return true; // Can always self-buff
    }

    @Override
    public void execute(LivingEntity caster, Player target) {
        PotionEffectType type = mapPotionType();
        if (type != null) {
            caster.addPotionEffect(new PotionEffect(type, durationTicks, 1, false, true));
        }
    }

    private PotionEffectType mapPotionType() {
        return switch (id) {
            case "speed_boost" -> PotionEffectType.SPEED;
            case "strength_boost", "rage" -> PotionEffectType.STRENGTH;
            case "resistance_boost", "iron_skin" -> PotionEffectType.RESISTANCE;
            case "regeneration" -> PotionEffectType.REGENERATION;
            case "fire_resistance_buff" -> PotionEffectType.FIRE_RESISTANCE;
            case "absorption" -> PotionEffectType.ABSORPTION;
            case "invisibility" -> PotionEffectType.INVISIBILITY;
            case "jump_boost" -> PotionEffectType.JUMP_BOOST;
            default -> PotionEffectType.SPEED;
        };
    }
}
