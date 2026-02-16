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
}
