package id.naturalsmp.naturaldungeon.skill.impl;

import id.naturalsmp.naturaldungeon.skill.MobSkill;
import id.naturalsmp.naturaldungeon.skill.SkillCategory;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Collection;

/**
 * Crowd Control skills for manipulating player movement and vision.
 */
public class CrowdControlSkill implements MobSkill {

    private final String id;
    private final String displayName;
    private final int cooldownTicks;
    private final double range;
    private final double power;

    public CrowdControlSkill(String id, String displayName, int cooldownTicks, double range, double power) {
        this.id = id;
        this.displayName = displayName;
        this.cooldownTicks = cooldownTicks;
        this.range = range;
        this.power = power;
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
            case "gravity_vortex" -> executeGravityVortex(caster, target);
            case "time_dilation" -> executeTimeDilation(caster, target);
            case "blind_fog" -> executeBlindFog(caster, target);
            case "phantom_possession" -> executePhantomPossession(caster, target);
        }
    }

    private void executeGravityVortex(LivingEntity caster, Player target) {
        final Location center = target.getLocation();
        caster.getWorld().playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 0.5f);

        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (t >= 40) {
                    this.cancel();
                    return;
                }

                // Visual vortex
                for (int i = 0; i < 5; i++) {
                    double angle = Math.toRadians((t * 20 + i * 72) % 360);
                    double r = 4.0 * (1.0 - (double) t / 40);
                    double x = Math.cos(angle) * r;
                    double z = Math.sin(angle) * r;
                    center.getWorld().spawnParticle(Particle.PORTAL, center.clone().add(x, 1, z), 5, 0, 0, 0, 0);
                }

                // Pull players
                Collection<Entity> nearby = center.getWorld().getNearbyEntities(center, 5, 5, 5);
                for (Entity e : nearby) {
                    if (e instanceof Player p) {
                        Vector pull = center.toVector().subtract(p.getLocation().toVector()).normalize().multiply(0.2);
                        p.setVelocity(p.getVelocity().add(pull));
                    }
                }
                t++;
            }
        }.runTaskTimer(org.bukkit.Bukkit.getPluginManager().getPlugin("NaturalDungeon"), 0, 1);
    }

    private void executeTimeDilation(LivingEntity caster, Player target) {
        final Location loc = caster.getLocation();
        caster.getWorld().playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 0.5f);
        caster.getWorld().spawnParticle(Particle.ENCHANTED_HIT, loc, 100, 5, 2, 5, 0.1);

        Collection<Entity> targets = caster.getWorld().getNearbyEntities(loc, range, range, range);
        for (Entity e : targets) {
            if (e instanceof Player p) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 4));
                p.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 100, 4));
                p.sendMessage("§b§l§k!§r §3§lTime slows down around you... §b§l§k!");
            }
        }
    }

    private void executeBlindFog(LivingEntity caster, Player target) {
        final Location loc = target.getLocation();
        caster.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 0.5f);

        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (t >= 60) {
                    this.cancel();
                    return;
                }

                loc.getWorld().spawnParticle(Particle.LARGE_SMOKE, loc, 20, 4, 2, 4, 0.02);
                loc.getWorld().spawnParticle(Particle.SQUID_INK, loc, 10, 3, 1, 3, 0.05);

                if (t % 20 == 0) {
                    Collection<Entity> targets = loc.getWorld().getNearbyEntities(loc, 5, 5, 5);
                    for (Entity e : targets) {
                        if (e instanceof Player p) {
                            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
                            p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 60, 0));
                        }
                    }
                }
                t += 2;
            }
        }.runTaskTimer(org.bukkit.Bukkit.getPluginManager().getPlugin("NaturalDungeon"), 0, 2);
    }

    private void executePhantomPossession(LivingEntity caster, Player target) {
        caster.getWorld().playSound(target.getLocation(), Sound.ENTITY_PHANTOM_SWOOP, 1f, 0.5f);
        caster.getWorld().spawnParticle(Particle.SOUL, target.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);

        target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 1));
        target.setVelocity(new Vector(0, 0, 0)); // Freeze briefly

        new BukkitRunnable() {
            @Override
            public void run() {
                target.setHealth(Math.max(0, target.getHealth() - power));
                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1f, 2f);
            }
        }.runTaskLater(org.bukkit.Bukkit.getPluginManager().getPlugin("NaturalDungeon"), 20);
    }
}
