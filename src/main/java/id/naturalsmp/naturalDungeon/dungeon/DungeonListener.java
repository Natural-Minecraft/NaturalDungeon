package id.naturalsmp.naturaldungeon.dungeon;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Bukkit;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;

import java.util.*;

import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import id.naturalsmp.naturaldungeon.dungeon.MutatorType;

public class DungeonListener implements Listener {

    private final NaturalDungeon plugin;
    private final Map<UUID, ItemStack[]> savedInventories = new HashMap<>();

    public DungeonListener(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player player = e.getEntity();
        DungeonInstance instance = plugin.getDungeonManager().getActiveInstance(player);
        if (instance != null) {
            e.setCancelled(true); // Prevent vanilla death screen and respawn logic
            instance.handleFakeDeath(player);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        Player player = e.getPlayer();
        ItemStack[] saved = savedInventories.remove(player.getUniqueId());
        if (saved != null)
            player.getInventory().setContents(saved);
    }

    // Invulnerability check — cancel damage if player just respawned
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player player))
            return;

        DungeonInstance instance = plugin.getDungeonManager().getActiveInstance(player);
        if (instance != null && instance.isInvulnerable(player.getUniqueId())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onMobDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof LivingEntity living))
            return;
        if (living instanceof Player)
            return;

        // Mutator: VAMPIRIC
        if (e instanceof EntityDamageByEntityEvent damageEvent) {
            if (damageEvent.getDamager() instanceof LivingEntity damager && damager != player) {
                // Mob hit player
                DungeonInstance inst = plugin.getDungeonManager().getActiveInstance(player);
                if (inst != null && inst.hasMutator(MutatorType.VAMPIRIC)) {
                    double healAmount = damageEvent.getFinalDamage() * 0.5; // heal 50% of damage dealt
                    double newHp = Math.min(damager.getHealth() + healAmount,
                            damager.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
                    damager.setHealth(newHp);

                    // VFX
                    damager.getWorld().spawnParticle(org.bukkit.Particle.HEART, damager.getLocation().add(0, 1, 0), 3,
                            0.3, 0.3, 0.3, 0);
                }
            }
        }

        // Find if this mob belongs to any dungeon
        for (DungeonInstance instance : plugin.getDungeonManager().getActiveInstances()) {
            if (instance.getWaveManager() != null && instance.getWaveManager().isDungeonMob(living.getUniqueId())) {
                // Update name tag immediately after damage
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (living.isDead())
                        return;
                    String name = ChatUtils.colorize("&7[Lv." + instance.getDungeon().getMinTier() + "] &f"
                            + living.getType().name() + " &c❤ " + (int) living.getHealth() + "/"
                            + (int) living.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getBaseValue());
                    living.setCustomName(name);

                    // Update boss bar with boss HP if this is the boss
                    if (instance.getWaveManager().isBossMob(living.getUniqueId())) {
                        double maxHp = living.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getBaseValue();
                        double currentHp = living.getHealth();
                        double progress = currentHp / maxHp;
                        instance.updateBossBar("&d&lBOSS HP &f" + (int) currentHp + "/" + (int) maxHp,
                                progress, org.bukkit.boss.BarColor.PURPLE);
                    }
                }, 1L);
                break;
            }
        }
    }

    @EventHandler
    public void onMobDeath(EntityDeathEvent e) {
        LivingEntity living = e.getEntity();
        if (living instanceof Player)
            return;

        for (DungeonInstance instance : plugin.getDungeonManager().getActiveInstances()) {
            if (instance.getWaveManager() != null && instance.getWaveManager().isDungeonMob(living.getUniqueId())) {
                if (instance.hasMutator(MutatorType.EXPLOSIVE)) {
                    // Drop primed TNT basically
                    org.bukkit.Location loc = living.getLocation();
                    loc.getWorld().spawnParticle(org.bukkit.Particle.FLAME, loc.clone().add(0, 1, 0), 15, 0.5, 0.5, 0.5,
                            0.1);
                    loc.getWorld().playSound(loc, org.bukkit.Sound.ENTITY_CREEPER_PRIMED, 1f, 1f);

                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (!instance.isActive())
                            return;
                        loc.getWorld().spawnParticle(org.bukkit.Particle.EXPLOSION_LARGE, loc, 1, 0, 0, 0, 0);
                        loc.getWorld().playSound(loc, org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);

                        // Damage nearby players
                        for (UUID uuid : instance.getParticipants()) {
                            Player p = Bukkit.getPlayer(uuid);
                            if (p != null && !instance.isInvulnerable(uuid)
                                    && p.getLocation().distanceSquared(loc) <= 3 * 3) {
                                p.damage(6.0); // 3 hearts
                            }
                        }
                    }, 30L); // 1.5s delay
                }
                break;
            }
        }
    }
}
