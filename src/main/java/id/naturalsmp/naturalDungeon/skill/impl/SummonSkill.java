package id.naturalsmp.naturaldungeon.skill.impl;

import id.naturalsmp.naturaldungeon.skill.MobSkill;
import id.naturalsmp.naturaldungeon.skill.SkillCategory;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

/**
 * Summon skill — spawns additional mobs around the caster.
 */
public class SummonSkill implements MobSkill {

    private final String id;
    private final String displayName;
    private final int cooldownTicks;
    private final double range;
    private final String entityTypeName;
    private final int count;

    public SummonSkill(String id, String displayName, int cooldownTicks, double range, String entityTypeName,
            int count) {
        this.id = id;
        this.displayName = displayName;
        this.cooldownTicks = cooldownTicks;
        this.range = range;
        this.entityTypeName = entityTypeName;
        this.count = count;
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
        return SkillCategory.SUMMON;
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
    public boolean canExecute(LivingEntity caster, Player target) {
        return true; // Can always summon
    }

    @Override
    public void execute(LivingEntity caster, Player target) {
        EntityType type;
        try {
            type = EntityType.valueOf(entityTypeName);
        } catch (IllegalArgumentException e) {
            type = EntityType.ZOMBIE;
        }

        Location base = caster.getLocation();
        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI / count) * i;
            double x = Math.cos(angle) * 3;
            double z = Math.sin(angle) * 3;
            Location spawnLoc = base.clone().add(x, 0, z);

            LivingEntity summoned = (LivingEntity) caster.getWorld().spawnEntity(spawnLoc, type);
            summoned.setCustomName(
                    ChatUtils.colorize("&7Summoned " + entityTypeName));
            summoned.setCustomNameVisible(true);
            // Tag as dungeon mob so they get cleaned up
            summoned.setMetadata("dungeon_mob",
                    new FixedMetadataValue(Bukkit.getPluginManager().getPlugin("NaturalDungeon"), true));
        }
    }
}
