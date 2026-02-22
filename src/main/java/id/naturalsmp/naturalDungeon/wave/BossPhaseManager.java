package id.naturalsmp.naturaldungeon.wave;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * Configurable multi-phase boss system.
 * Reads phases from dungeon config (stages.X.boss.phases) and triggers
 * skill actions when boss HP drops below thresholds.
 *
 * Per vision_naturaldungeon.md §5: Multi-Phase Boss System
 */
public class BossPhaseManager {

    private final NaturalDungeon plugin;
    private final List<BossPhase> phases;
    private int currentPhaseIndex = 0;
    private boolean phaseTransitioning = false;

    public BossPhaseManager(NaturalDungeon plugin, String dungeonId, int stageIndex) {
        this.plugin = plugin;
        this.phases = loadPhases(dungeonId, stageIndex);
    }

    private List<BossPhase> loadPhases(String dungeonId, int stageIndex) {
        List<BossPhase> result = new ArrayList<>();
        YamlConfiguration config = plugin.getDungeonManager().loadDungeonConfig(dungeonId);
        List<Map<?, ?>> phaseList = config.getMapList("stages." + (stageIndex + 1) + ".boss.phases");

        for (Map<?, ?> map : phaseList) {
            int hpThreshold = map.get("hp-threshold") != null ? ((Number) map.get("hp-threshold")).intValue() : 100;
            List<String> skills = new ArrayList<>();
            if (map.get("skills") instanceof List<?> sl) {
                for (Object o : sl)
                    skills.add(o.toString());
            }
            result.add(new BossPhase(hpThreshold, skills));
        }

        // Sort by threshold descending (Phase 1 = highest HP)
        result.sort((a, b) -> b.hpThreshold - a.hpThreshold);
        return result;
    }

    /**
     * Call this every tick while boss is alive.
     * Returns true if a phase transition happened.
     */
    public boolean tick(LivingEntity boss, Collection<UUID> participants) {
        if (phases.isEmpty() || phaseTransitioning)
            return false;

        double maxHp = boss.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
        double hpPercent = (boss.getHealth() / maxHp) * 100;

        // Check if we should advance to the next phase
        if (currentPhaseIndex < phases.size() - 1) {
            BossPhase nextPhase = phases.get(currentPhaseIndex + 1);
            if (hpPercent <= nextPhase.hpThreshold) {
                advancePhase(boss, participants);
                return true;
            }
        }

        return false;
    }

    private void advancePhase(LivingEntity boss, Collection<UUID> participants) {
        currentPhaseIndex++;
        BossPhase phase = phases.get(currentPhaseIndex);
        phaseTransitioning = true;

        // Announce phase transition
        String title = "&c&lPhase " + (currentPhaseIndex + 1);
        String subtitle = "&7Boss enters phase " + (currentPhaseIndex + 1) + "!";
        for (UUID pid : participants) {
            Player p = Bukkit.getPlayer(pid);
            if (p != null) {
                p.sendTitle(ChatUtils.colorize(title), ChatUtils.colorize(subtitle), 10, 40, 10);
                p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.8f);
            }
        }

        // Visual effects
        Location loc = boss.getLocation();
        boss.getWorld().spawnParticle(Particle.EXPLOSION, loc, 5, 1, 1, 1, 0);
        boss.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.add(0, 1, 0), 50, 1, 1, 1, 0.05);

        // Execute skills for this phase
        for (String skill : phase.skills) {
            executeSkill(skill, boss, participants);
        }

        // Brief invulnerability during transition (1.5 seconds)
        boss.setInvulnerable(true);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!boss.isDead()) {
                boss.setInvulnerable(false);
                phaseTransitioning = false;
            }
        }, 30L);
    }

    /**
     * Execute a boss skill by ID.
     */
    public void executeSkill(String skillId, LivingEntity boss, Collection<UUID> participants) {
        switch (skillId.toUpperCase()) {
            case "MELEE_BOOST" -> {
                boss.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 600, 2, false, false));
                boss.getWorld().spawnParticle(Particle.CRIT, boss.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
                broadcastSkill(participants, "&c🗡 &7Boss mendapat &cMelee Boost!");
            }
            case "SUMMON_ADDS" -> {
                // Spawn 3-5 zombies around boss
                Location loc = boss.getLocation();
                int count = 3 + new Random().nextInt(3);
                for (int i = 0; i < count; i++) {
                    double angle = i * (2 * Math.PI / count);
                    Location spawnLoc = loc.clone().add(Math.cos(angle) * 4, 0, Math.sin(angle) * 4);
                    boss.getWorld().spawn(spawnLoc, org.bukkit.entity.Zombie.class);
                }
                boss.getWorld().spawnParticle(Particle.SOUL, loc, 30, 2, 1, 2, 0.05);
                broadcastSkill(participants, "&5🧟 &7Boss memanggil &dminions!");
            }
            case "SHOCKWAVE" -> {
                Location loc = boss.getLocation();
                double radius = 8.0;
                for (UUID pid : participants) {
                    Player p = Bukkit.getPlayer(pid);
                    if (p != null && p.getLocation().distanceSquared(loc) <= radius * radius) {
                        org.bukkit.util.Vector knockback = p.getLocation().toVector()
                                .subtract(loc.toVector()).normalize().multiply(2).setY(0.8);
                        p.setVelocity(knockback);
                        p.damage(6.0, boss);
                    }
                }
                // Ring particles
                for (double angle = 0; angle < Math.PI * 2; angle += 0.2) {
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    loc.getWorld().spawnParticle(Particle.EXPLOSION, loc.clone().add(x, 0.5, z), 1);
                }
                broadcastSkill(participants, "&e💥 &7Boss meluncurkan &eShockwave!");
            }
            case "SHIELD_PHASE" -> {
                boss.setInvulnerable(true);
                boss.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 200, 0, false, false));
                // Auto-remove after 10 seconds
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!boss.isDead())
                        boss.setInvulnerable(false);
                }, 200L);
                broadcastSkill(participants, "&b🛡 &7Boss mengaktifkan &bShield!");
            }
            case "FIRE_AURA" -> {
                boss.addPotionEffect(
                        new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
                // Start fire aura task
                Bukkit.getScheduler().runTaskTimer(plugin, task -> {
                    if (boss.isDead()) {
                        task.cancel();
                        return;
                    }
                    Location loc = boss.getLocation();
                    for (UUID pid : participants) {
                        Player p = Bukkit.getPlayer(pid);
                        if (p != null && p.getLocation().distanceSquared(loc) <= 25) { // 5 block radius
                            p.setFireTicks(40);
                        }
                    }
                    loc.getWorld().spawnParticle(Particle.FLAME, loc.add(0, 1, 0), 15, 1.5, 0.5, 1.5, 0.02);
                }, 20L, 20L);
                broadcastSkill(participants, "&6🔥 &7Boss menyalakan &6Fire Aura!");
            }
            case "LIGHTNING" -> {
                // Strike random player
                List<UUID> online = new ArrayList<>();
                for (UUID pid : participants) {
                    Player p = Bukkit.getPlayer(pid);
                    if (p != null && !p.isDead())
                        online.add(pid);
                }
                if (!online.isEmpty()) {
                    Player target = Bukkit.getPlayer(online.get(new Random().nextInt(online.size())));
                    if (target != null) {
                        target.getWorld().strikeLightning(target.getLocation());
                    }
                }
                broadcastSkill(participants, "&b⚡ &7Boss menyerang dengan &bPetir!");
            }
            case "TELEPORT" -> {
                // Teleport to random player
                List<UUID> online = new ArrayList<>();
                for (UUID pid : participants) {
                    Player p = Bukkit.getPlayer(pid);
                    if (p != null && !p.isDead())
                        online.add(pid);
                }
                if (!online.isEmpty()) {
                    Player target = Bukkit.getPlayer(online.get(new Random().nextInt(online.size())));
                    if (target != null) {
                        boss.getWorld().spawnParticle(Particle.PORTAL, boss.getLocation(), 40, 0.5, 1, 0.5, 0.5);
                        boss.teleport(target.getLocation().add(2, 0, 0));
                        boss.getWorld().spawnParticle(Particle.PORTAL, boss.getLocation(), 40, 0.5, 1, 0.5, 0.5);
                        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.5f);
                    }
                }
                broadcastSkill(participants, "&5🌀 &7Boss ber-&dTeleport!");
            }
            case "REGEN" -> {
                boss.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 2, false, true));
                boss.getWorld().spawnParticle(Particle.HEART, boss.getLocation().add(0, 2, 0), 10, 0.5, 0.5, 0.5, 0);
                broadcastSkill(participants, "&a💚 &7Boss mendapat &aRegenerasi!");
            }
            case "ARENA_SHRINK" -> {
                broadcastSkill(participants, "&4💀 &7Arena akan &cmenyusut!");
                // Visual warning — no actual boundary shrink (needs arena system)
                boss.getWorld().spawnParticle(Particle.DUST_COLOR_TRANSITION, boss.getLocation(), 100,
                        10, 5, 10, 0, new Particle.DustTransition(
                                org.bukkit.Color.RED, org.bukkit.Color.BLACK, 2f));
            }
            case "LASER_BEAM" -> {
                broadcastSkill(participants, "&c🔴 &7Boss menembakkan &cLaser!");
                // 2-second telegraph then damage
                Location loc = boss.getLocation().add(0, 1.5, 0);
                org.bukkit.util.Vector dir = boss.getLocation().getDirection().normalize();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    for (double d = 0; d < 20; d += 0.5) {
                        Location point = loc.clone().add(dir.clone().multiply(d));
                        boss.getWorld().spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0,
                                new Particle.DustOptions(org.bukkit.Color.RED, 1.5f));
                        // Damage nearby players
                        for (UUID pid : participants) {
                            Player p = Bukkit.getPlayer(pid);
                            if (p != null && p.getLocation().distanceSquared(point) <= 4) {
                                p.damage(8.0, boss);
                            }
                        }
                    }
                }, 40L);
            }
            default -> plugin.getLogger().info("Unknown boss skill: " + skillId);
        }
    }

    private void broadcastSkill(Collection<UUID> participants, String message) {
        for (UUID pid : participants) {
            Player p = Bukkit.getPlayer(pid);
            if (p != null) {
                p.sendMessage(ChatUtils.colorize(message));
            }
        }
    }

    public int getCurrentPhaseIndex() {
        return currentPhaseIndex;
    }

    public int getTotalPhases() {
        return phases.size();
    }

    public boolean hasPhases() {
        return !phases.isEmpty();
    }

    // ─── Inner Classes ──────────────────────────────────────────────

    private static class BossPhase {
        final int hpThreshold;
        final List<String> skills;

        BossPhase(int hpThreshold, List<String> skills) {
            this.hpThreshold = hpThreshold;
            this.skills = skills;
        }
    }
}
