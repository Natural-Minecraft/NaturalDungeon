package id.naturalsmp.naturaldungeon.skill.impl;

import id.naturalsmp.naturaldungeon.skill.MobSkill;
import id.naturalsmp.naturaldungeon.skill.SkillCategory;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Entity manipulation skills — summoning and tethering.
 */
public class EntityManipulationSkill implements MobSkill {

    private final String id;
    private final String displayName;
    private final int cooldownTicks;
    private final double range;

    public EntityManipulationSkill(String id, String displayName, int cooldownTicks, double range) {
        this.id = id;
        this.displayName = displayName;
        this.cooldownTicks = cooldownTicks;
        this.range = range;
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
        return SkillCategory.SPECIAL;
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

        switch (id) {
            case "summon_minions" -> executeSummonMinions(caster, target);
            case "soul_tether" -> executeSoulTether(caster, target);
        }
    }

    private void executeSummonMinions(LivingEntity caster, Player target) {
        Location loc = caster.getLocation();
        caster.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1.2f, 0.5f);
        caster.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0, 1, 0), 50, 1, 1, 1, 0.1);

        for (int i = 0; i < 3; i++) {
            Zombie minion = caster.getWorld().spawn(loc, Zombie.class);
            minion.setBaby();
            minion.setMetadata("dungeon_mob", new org.bukkit.metadata.FixedMetadataValue(
                    org.bukkit.Bukkit.getPluginManager().getPlugin("NaturalDungeon"), true));
            minion.setTarget(target);

            // Cleanup after 30s
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (minion.isValid())
                        minion.remove();
                }
            }.runTaskLater(org.bukkit.Bukkit.getPluginManager().getPlugin("NaturalDungeon"), 600);
        }
    }

    private void executeSoulTether(LivingEntity caster, Player target) {
        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_CONDUIT_ACTIVATE, 1.5f, 0.5f);

        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (t >= 100 || !caster.isValid() || !target.isValid()) {
                    this.cancel();
                    return;
                }

                Location cLoc = caster.getLocation().add(0, 1, 0);
                Location tLoc = target.getLocation().add(0, 1, 0);
                Vector dir = tLoc.toVector().subtract(cLoc.toVector());
                double dist = dir.length();
                dir.normalize();

                // Particle line
                for (double d = 0; d < dist; d += 0.5) {
                    Location pLoc = cLoc.clone().add(dir.clone().multiply(d));
                    pLoc.getWorld().spawnParticle(Particle.SOUL, pLoc, 1, 0, 0, 0, 0);
                }

                if (dist > 8) {
                    target.setVelocity(cLoc.toVector().subtract(tLoc.toVector()).normalize().multiply(0.3));
                    target.damage(1.0, caster);
                }

                t += 5;
            }
        }.runTaskTimer(org.bukkit.Bukkit.getPluginManager().getPlugin("NaturalDungeon"), 0, 5);
    }
}
