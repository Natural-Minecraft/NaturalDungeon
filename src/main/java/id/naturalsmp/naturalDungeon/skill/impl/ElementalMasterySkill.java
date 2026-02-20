package id.naturalsmp.naturaldungeon.skill.impl;

import id.naturalsmp.naturaldungeon.skill.MobSkill;
import id.naturalsmp.naturaldungeon.skill.SkillCategory;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;

/**
 * High-quality elemental mastery skills with complex VFX and mechanics.
 */
public class ElementalMasterySkill implements MobSkill {

    private final String id;
    private final String displayName;
    private final int cooldownTicks;
    private final double range;
    private final double damage;

    public ElementalMasterySkill(String id, String displayName, int cooldownTicks, double range, double damage) {
        this.id = id;
        this.displayName = displayName;
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
            case "meteor" -> executeMeteor(caster, target);
            case "tsunami" -> executeTsunami(caster, target);
            case "earth_spikes" -> executeEarthSpikes(caster, target);
            case "thunder_strike" -> executeThunderStrike(caster, target);
        }
    }

    private void executeMeteor(LivingEntity caster, Player target) {
        Location targetLoc = target.getLocation();
        Location skyLoc = targetLoc.clone().add(0, 20, 0);
        World world = targetLoc.getWorld();

        // Warning circle
        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (t >= 20) {
                    this.cancel();
                    return;
                }
                for (int i = 0; i < 360; i += 20) {
                    double angle = Math.toRadians(i);
                    double x = Math.cos(angle) * 3;
                    double z = Math.sin(angle) * 3;
                    world.spawnParticle(Particle.FLAME, targetLoc.clone().add(x, 0.1, z), 1, 0, 0, 0, 0);
                }
                t++;
            }
        }.runTaskTimer(org.bukkit.Bukkit.getPluginManager().getPlugin("NaturalDungeon"), 0, 1);

        // Falling Meteor
        new BukkitRunnable() {
            Location current = skyLoc.clone();
            Vector dir = targetLoc.toVector().subtract(skyLoc.toVector()).normalize().multiply(1.5);

            @Override
            public void run() {
                if (current.getY() <= targetLoc.getY() || current.getBlock().getType().isSolid()) {
                    this.cancel();
                    world.spawnParticle(Particle.EXPLOSION_EMITTER, current, 1, 0, 0, 0, 0);
                    world.playSound(current, Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.5f);

                    Collection<Entity> targets = world.getNearbyEntities(current, 4, 4, 4);
                    for (Entity e : targets) {
                        if (e instanceof Player p) {
                            p.damage(damage, caster);
                            p.setFireTicks(100);
                        }
                    }
                    return;
                }

                world.spawnParticle(Particle.LARGE_SMOKE, current, 5, 0.2, 0.2, 0.2, 0.05);
                world.spawnParticle(Particle.FLAME, current, 10, 0.3, 0.3, 0.3, 0.1);
                current.add(dir);
            }
        }.runTaskTimer(org.bukkit.Bukkit.getPluginManager().getPlugin("NaturalDungeon"), 20, 1);
    }

    private void executeTsunami(LivingEntity caster, Player target) {
        Location center = caster.getLocation();
        Vector direction = target.getLocation().toVector().subtract(center.toVector()).normalize().setY(0);

        new BukkitRunnable() {
            int t = 0;
            Location current = center.clone();

            @Override
            public void run() {
                if (t >= 30) {
                    this.cancel();
                    return;
                }

                current.add(direction.clone().multiply(0.8));

                // Visual wave
                for (int i = -3; i <= 3; i++) {
                    Vector side = new Vector(-direction.getZ(), 0, direction.getX()).normalize().multiply(i);
                    Location particleLoc = current.clone().add(side);
                    caster.getWorld().spawnParticle(Particle.SPLASH, particleLoc, 10, 0.5, 1, 0.5, 0.1);
                    caster.getWorld().spawnParticle(Particle.BUBBLE, particleLoc, 5, 0.5, 0.5, 0.5, 0.05);
                }

                if (t % 5 == 0) {
                    caster.getWorld().playSound(current, Sound.ITEM_BUCKET_EMPTY, 1f, 0.5f);
                }

                // Effect
                Collection<Entity> targets = caster.getWorld().getNearbyEntities(current, 3, 3, 3);
                for (Entity e : targets) {
                    if (e instanceof Player p && !p.equals(caster)) {
                        p.setVelocity(direction.clone().multiply(1.2).setY(0.4));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 2));
                        p.damage(damage / 5.0, caster);
                    }
                }
                t++;
            }
        }.runTaskTimer(org.bukkit.Bukkit.getPluginManager().getPlugin("NaturalDungeon"), 0, 2);
    }

    private void executeEarthSpikes(LivingEntity caster, Player target) {
        Location center = caster.getLocation();
        Vector dir = target.getLocation().toVector().subtract(center.toVector()).normalize().setY(0);

        new BukkitRunnable() {
            int t = 0;
            Location current = center.clone();

            @Override
            public void run() {
                if (t >= 10) {
                    this.cancel();
                    return;
                }

                current.add(dir.clone().multiply(1.5));
                Location spikeLoc = current.clone();

                // Jump up effect
                spikeLoc.getWorld().spawnParticle(Particle.BLOCK, spikeLoc, 50, 0.5, 0.5, 0.5,
                        Material.STONE.createBlockData());
                spikeLoc.getWorld().playSound(spikeLoc, Sound.BLOCK_STONE_BREAK, 1f, 0.5f);

                Collection<Entity> targets = spikeLoc.getWorld().getNearbyEntities(spikeLoc, 1.5, 2, 1.5);
                for (Entity e : targets) {
                    if (e instanceof Player p) {
                        p.setVelocity(new Vector(0, 0.8, 0));
                        p.damage(damage, caster);
                    }
                }
                t++;
            }
        }.runTaskTimer(org.bukkit.Bukkit.getPluginManager().getPlugin("NaturalDungeon"), 0, 3);
    }

    private void executeThunderStrike(LivingEntity caster, Player target) {
        Location loc = target.getLocation();

        // Telegraph
        loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc.clone().add(0, 5, 0), 50, 0.5, 3, 0.5, 0.1);
        loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_AMBIENT, 1f, 2f);

        new BukkitRunnable() {
            @Override
            public void run() {
                loc.getWorld().strikeLightningEffect(loc);
                loc.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2f, 1f);

                Collection<Entity> targets = loc.getWorld().getNearbyEntities(loc, 4, 4, 4);
                for (Entity e : targets) {
                    if (e instanceof Player p) {
                        p.damage(damage, caster);
                        p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1));
                    }
                }
            }
        }.runTaskLater(org.bukkit.Bukkit.getPluginManager().getPlugin("NaturalDungeon"), 20);
    }
}
