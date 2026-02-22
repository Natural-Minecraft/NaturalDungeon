package id.naturalsmp.naturaldungeon.dungeon;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import id.naturalsmp.naturaldungeon.utils.ConfigUtils;

import java.util.Random;
import java.util.UUID;

/**
 * Handles spawning random events during a dungeon stage.
 * E.g. 10% chance to spawn a Treasure Goblin, Elite Mob, or Hidden Chest.
 */
public class RandomEventsManager {

    private final NaturalDungeon plugin;
    private final Random random = new Random();

    public RandomEventsManager(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    /**
     * Try to trigger a random event. Typically called when a combat stage starts.
     * 
     * @param instance The dungeon instance
     * @param spawnLoc A location to spawn the event near
     */
    public void tryTriggerEvent(DungeonInstance instance, Location spawnLoc) {
        // 15% random chance
        if (random.nextDouble() > 0.15) {
            return;
        }

        int eventType = random.nextInt(3); // 0, 1, 2

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!instance.isActive())
                return;

            switch (eventType) {
                case 0:
                    spawnTreasureGoblin(instance, spawnLoc.clone().add(randomOffset(), 0, randomOffset()));
                    break;
                case 1:
                    spawnEliteMob(instance, spawnLoc.clone().add(randomOffset(), 0, randomOffset()));
                    break;
                case 2:
                    spawnHiddenChest(instance, spawnLoc.clone().add(randomOffset(), 0, randomOffset()));
                    break;
            }
        }, 100L); // Delay 5 seconds into the stage
    }

    private double randomOffset() {
        return (random.nextDouble() - 0.5) * 8.0; // +/- 4 blocks
    }

    private void broadcastEvent(DungeonInstance instance, String message) {
        for (UUID pid : instance.getParticipants()) {
            Player p = Bukkit.getPlayer(pid);
            if (p != null) {
                p.sendMessage(ChatUtils.colorize(message));
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
            }
        }
    }

    private void spawnTreasureGoblin(DungeonInstance instance, Location loc) {
        loc = findGround(loc);
        Zombie goblin = (Zombie) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
        goblin.setBaby(true);
        goblin.setCustomName(ChatUtils.colorize("&#FFD700Treasure Goblin"));
        goblin.setCustomNameVisible(true);
        goblin.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 2, false, false));
        goblin.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));

        goblin.getEquipment().setHelmet(new ItemStack(Material.GOLD_BLOCK));
        loc.getWorld().spawnParticle(Particle.TOTEM, loc, 50, 1, 1, 1, 0.1);

        // We mark it so it drops extra coins/loot on death using
        // PersistentDataContainer if needed,
        // or just let WaveManager handle it if we add a meta tag.
        goblin.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "treasure_goblin"),
                org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);

        broadcastEvent(instance, "&e&lA Treasure Goblin has appeared!");
    }

    private void spawnEliteMob(DungeonInstance instance, Location loc) {
        loc = findGround(loc);
        LivingEntity elite = (LivingEntity) loc.getWorld().spawnEntity(loc, EntityType.WITHER_SKELETON);
        elite.setCustomName(ChatUtils.colorize("&#FF4444Elite Guardian"));
        elite.setCustomNameVisible(true);
        elite.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, Integer.MAX_VALUE, 1, false, false));
        elite.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, Integer.MAX_VALUE, 4, false, false));
        elite.setHealth(elite.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue());

        loc.getWorld().spawnParticle(Particle.FLAME, loc, 50, 1, 1, 1, 0.05);

        elite.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "elite_mob"),
                org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);

        broadcastEvent(instance, "&c&lAn Elite Guardian has spawned!");
    }

    private void spawnHiddenChest(DungeonInstance instance, Location loc) {
        loc = findGround(loc);
        Block block = loc.getBlock();
        block.setType(Material.CHEST);

        if (block.getState() instanceof Chest chest) {
            chest.setCustomName(ChatUtils.colorize("&#FFD700Hidden Chest"));
            chest.update();
            // In a real implementation we might populate it directly or use LootManager
        }

        loc.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, loc, 30, 0.5, 0.5, 0.5, 0);

        // Note: For actual functionality, players opening it should get loot.
        // Needs an EventListener catching InventoryOpenEvent or PlayerInteractEvent.

        broadcastEvent(instance, "&b&lA Hidden Chest spawned somewhere nearby!");
    }

    private Location findGround(Location loc) {
        for (int y = loc.getBlockY() + 5; y > loc.getBlockY() - 5; y--) {
            Location check = new Location(loc.getWorld(), loc.getX(), y, loc.getZ());
            if (check.getBlock().getType().isSolid()) {
                if (!check.clone().add(0, 1, 0).getBlock().getType().isSolid()) {
                    return check.add(0, 1, 0);
                }
            }
        }
        return loc;
    }
}
