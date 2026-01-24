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
            savedInventories.put(player.getUniqueId(), player.getInventory().getContents().clone());
            e.getDrops().clear();
            e.setDroppedExp(0);
            e.setKeepInventory(true);
            e.setKeepLevel(true);
            instance.handlePlayerDeath(player);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        Player player = e.getPlayer();
        ItemStack[] saved = savedInventories.remove(player.getUniqueId());
        if (saved != null)
            player.getInventory().setContents(saved);
    }

    @EventHandler
    public void onMobDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof LivingEntity living))
            return;
        if (living instanceof Player)
            return;

        // Find if this mob belongs to any dungeon
        for (DungeonInstance instance : plugin.getDungeonManager().getActiveInstances()) {
            if (instance.getWaveManager().isDungeonMob(living.getUniqueId())) {
                // Update name tag immediately after damage
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (living.isDead())
                        return;
                    String name = ChatUtils.colorize("&7[Lv." + instance.getDungeon().getMinTier() + "] &f"
                            + living.getType().name() + " &c❤ " + (int) living.getHealth() + "/"
                            + (int) living.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getBaseValue());
                    living.setCustomName(name);
                }, 1L);
                break;
            }
        }
    }
}
