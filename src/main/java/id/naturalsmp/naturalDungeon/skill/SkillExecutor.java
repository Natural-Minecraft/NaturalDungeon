package id.naturalsmp.naturaldungeon.skill;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Runtime executor for mob skills. Runs on a repeating tick to evaluate
 * and execute skills for dungeon mobs.
 */
public class SkillExecutor {

    private final NaturalDungeon plugin;
    private final SkillRegistry registry;
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>(); // entity UUID -> (skillId -> expiry tick)
    private long currentTick = 0;

    public SkillExecutor(NaturalDungeon plugin, SkillRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
        startTickLoop();
    }

    private void startTickLoop() {
        new BukkitRunnable() {
            @Override
            public void run() {
                currentTick++;
                // Process every 10 ticks (0.5s) for performance
                if (currentTick % 10 != 0)
                    return;

                // Iterate over all tracked mobs in dungeon instances
                for (org.bukkit.World world : Bukkit.getWorlds()) {
                    for (LivingEntity entity : world.getLivingEntities()) {
                        if (!entity.hasMetadata("dungeon_mob"))
                            continue;
                        if (!entity.hasMetadata("mob_skills"))
                            continue;

                        String skillStr = entity.getMetadata("mob_skills").get(0).asString();
                        if (skillStr.isEmpty())
                            continue;

                        String[] skillIds = skillStr.split(",");
                        Player nearestPlayer = findNearestPlayer(entity, 25);
                        if (nearestPlayer == null)
                            continue;

                        tryExecuteSkills(entity, nearestPlayer, skillIds);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 1L); // Start after 1s, run every tick
    }

    private void tryExecuteSkills(LivingEntity entity, Player target, String[] skillIds) {
        UUID entityId = entity.getUniqueId();
        Map<String, Long> entityCooldowns = cooldowns.computeIfAbsent(entityId, k -> new HashMap<>());

        for (String skillId : skillIds) {
            MobSkill skill = registry.getSkill(skillId.trim());
            if (skill == null)
                continue;

            // Check cooldown
            Long expiry = entityCooldowns.get(skillId);
            if (expiry != null && currentTick < expiry)
                continue;

            // Check if can execute
            if (!skill.canExecute(entity, target))
                continue;

            // Execute!
            skill.execute(entity, target);

            // Set cooldown
            entityCooldowns.put(skillId, currentTick + skill.getCooldownTicks());

            // Only execute one skill per tick cycle per entity
            break;
        }
    }

    private Player findNearestPlayer(LivingEntity entity, double maxDist) {
        Player nearest = null;
        double nearestDist = maxDist * maxDist;

        for (Player player : entity.getWorld().getPlayers()) {
            if (player.isDead() || player.getGameMode() == org.bukkit.GameMode.SPECTATOR)
                continue;
            double dist = player.getLocation().distanceSquared(entity.getLocation());
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = player;
            }
        }
        return nearest;
    }

    /**
     * Clean up cooldown data for a dead/removed entity.
     */
    public void clearCooldowns(UUID entityId) {
        cooldowns.remove(entityId);
    }
}
