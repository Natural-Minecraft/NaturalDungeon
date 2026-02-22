package id.naturalsmp.naturaldungeon.dungeon;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles visual effects and mechanical modifiers per difficulty level.
 * Called when a dungeon starts or during the run.
 */
public class DifficultyEffectsManager {

    private final NaturalDungeon plugin;
    private final Map<Integer, BukkitTask> activeTasks = new HashMap<>();

    public DifficultyEffectsManager(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public void applyDifficultyEffects(DungeonInstance instance) {
        String diffId = instance.getDifficulty().getId().toLowerCase();

        // Initial setup
        switch (diffId) {
            case "easy":
                //
                break;
            case "normal":
                break;
            case "hard":
                // Rain in the world if possible, or send resource pack event?
                break;
            case "nightmare":
                for (UUID pid : instance.getParticipants()) {
                    Player p = Bukkit.getPlayer(pid);
                    if (p != null) {
                        p.addPotionEffect(
                                new PotionEffect(PotionEffectType.DARKNESS, Integer.MAX_VALUE, 0, false, false));
                    }
                }
                break;
            case "hell":
                for (UUID pid : instance.getParticipants()) {
                    Player p = Bukkit.getPlayer(pid);
                    if (p != null) {
                        p.addPotionEffect(
                                new PotionEffect(PotionEffectType.DARKNESS, Integer.MAX_VALUE, 0, false, false));
                        // Additional hardcore modifiers
                    }
                }
                break;
        }

        // Start ambient loop
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!instance.isActive()) {
                stopEffects(instance.getInstanceId());
                return;
            }

            for (UUID pid : instance.getParticipants()) {
                Player p = Bukkit.getPlayer(pid);
                if (p != null && !p.isDead()) {
                    playAmbient(p, diffId);
                }
            }
        }, 60L, 100L); // Every 5 seconds

        activeTasks.put(instance.getInstanceId(), task);
    }

    private void playAmbient(Player p, String diffId) {
        Location loc = p.getLocation();
        switch (diffId) {
            case "easy":
                // Ambient birds/peaceful
                if (Math.random() < 0.2) {
                    p.playSound(loc, Sound.ENTITY_PARROT_AMBIENT, 0.5f, 1f);
                }
                break;
            case "nightmare":
                // Lava particles, eerie sounds
                if (Math.random() < 0.3) {
                    p.playSound(loc, Sound.AMBIENT_CAVE, 0.5f, 0.5f);
                }
                loc.getWorld().spawnParticle(Particle.ASH, loc.add(0, 2, 0), 20, 5, 5, 5, 0.01);
                loc.getWorld().spawnParticle(Particle.LAVA, loc.add(0, 5, 0), 2, 5, 1, 5, 0.1);
                break;
            case "hell":
                // Heartbeat / screen shake / wither ambiance
                p.playSound(loc, Sound.ENTITY_WARDEN_HEARTBEAT, 1f, 1f);
                if (Math.random() < 0.5) {
                    p.spawnParticle(org.bukkit.Particle.LARGE_SMOKE, p.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5,
                            0.05);
                }
                break;
        }
    }

    public void stopEffects(int instanceId) {
        BukkitTask task = activeTasks.remove(instanceId);
        if (task != null) {
            task.cancel();
        }
    }

    public void shutdown() {
        for (BukkitTask task : activeTasks.values()) {
            task.cancel();
        }
        activeTasks.clear();
    }
}
