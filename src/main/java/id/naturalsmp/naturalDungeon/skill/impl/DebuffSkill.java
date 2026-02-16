package id.naturalsmp.naturaldungeon.skill.impl;

import id.naturalsmp.naturaldungeon.skill.MobSkill;
import id.naturalsmp.naturaldungeon.skill.SkillCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Debuff skill — applies a negative potion effect to the player.
 */
public class DebuffSkill implements MobSkill {

    private final String id;
    private final String displayName;
    private final int cooldownTicks;
    private final double range;
    private final int potionTypeId; // Using raw IDs for flexible potion mapping
    private final int durationTicks;

    public DebuffSkill(String id, String displayName, int cooldownTicks, double range, int potionTypeId,
            int durationTicks) {
        this.id = id;
        this.displayName = displayName;
        this.cooldownTicks = cooldownTicks;
        this.range = range;
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
        return SkillCategory.DEBUFF;
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

        PotionEffectType type = mapPotionType();
        if (type != null) {
            target.addPotionEffect(new PotionEffect(type, durationTicks, 1, false, true));
        }
    }

    private PotionEffectType mapPotionType() {
        return switch (id) {
            case "slow" -> PotionEffectType.SLOWNESS;
            case "weakness" -> PotionEffectType.WEAKNESS;
            case "blindness" -> PotionEffectType.BLINDNESS;
            case "wither", "curse_of_decay" -> PotionEffectType.WITHER;
            case "poison" -> PotionEffectType.POISON;
            case "hunger" -> PotionEffectType.HUNGER;
            case "mining_fatigue" -> PotionEffectType.MINING_FATIGUE;
            case "nausea" -> PotionEffectType.NAUSEA;
            case "levitation" -> PotionEffectType.LEVITATION;
            case "darkness" -> PotionEffectType.DARKNESS;
            case "silence" -> PotionEffectType.SLOWNESS; // Custom behavior mapped
            default -> PotionEffectType.SLOWNESS;
        };
    }
}
