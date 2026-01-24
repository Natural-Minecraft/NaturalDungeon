package id.naturalsmp.naturaldungeon.loot;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import id.naturalsmp.naturaldungeon.utils.ConfigUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class LootManager {

    private final NaturalDungeon plugin;
    private final Random random = new Random();

    public LootManager(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public List<ItemStack> distributeLoot(List<UUID> players, ConfigurationSection lootSection, boolean isBoss) {
        List<ItemStack> allLoot = new ArrayList<>();
        if (lootSection == null)
            return allLoot;

        String lootPath = isBoss ? "boss" : "completion";
        List<?> lootList = lootSection.getList(lootPath);
        if (lootList == null)
            return allLoot;

        for (int i = 0; i < lootList.size(); i++) {
            String path = lootPath + "." + i;
            String type = lootSection.getString(path + ".type", "VANILLA");
            int chance = lootSection.getInt(path + ".chance", 100);
            if (random.nextInt(100) >= chance)
                continue;

            String amountStr = lootSection.getString(path + ".amount", "1");
            int amount = parseAmount(amountStr);
            ItemStack item = null;

            switch (type.toUpperCase()) {
                case "MMOITEMS" -> {
                    if (plugin.hasMMOItems()) {
                        String id = lootSection.getString(path + ".id", "");
                        String[] parts = id.split(":");
                        if (parts.length == 2) {
                            item = plugin.getMmoItemsHook().getItem(parts[0], parts[1]);
                            if (item != null)
                                item.setAmount(amount);
                        }
                    }
                }
                case "VANILLA" -> {
                    String materialStr = lootSection.getString(path + ".material", "DIAMOND");
                    try {
                        Material material = Material.valueOf(materialStr.toUpperCase());
                        item = new ItemStack(material, amount);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                case "COMMAND" -> {
                    String command = lootSection.getString(path + ".command", "");
                    for (UUID uuid : players) {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null) {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                                    command.replace("%player%", player.getName()));
                        }
                    }
                }
            }
            if (item != null)
                allLoot.add(item);
        }

        /*
         * Removed: Auto-add to inventory. Items will now drop from completion chest.
         * for (UUID uuid : players) {
         * Player player = Bukkit.getPlayer(uuid);
         * if (player != null) {
         * for (ItemStack item : allLoot) {
         * player.getInventory().addItem(item.clone());
         * }
         * }
         * }
         */
        return allLoot;
    }

    public void spawnLootChest(Location location, List<ItemStack> loot) {
        location.getBlock().setType(Material.ENDER_CHEST);

        // Final effect
        location.getWorld().playSound(location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        location.getWorld().spawnParticle(org.bukkit.Particle.EXPLOSION_EMITTER, location.clone().add(0.5, 0.5, 0.5),
                5);

        // Spawn holographic items and animate them
        for (int i = 0; i < loot.size(); i++) {
            ItemStack item = loot.get(i);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Item itemEntity = location.getWorld().dropItem(location.clone().add(0.5, 1.2, 0.5), item);
                itemEntity.setVelocity(new Vector(
                        (Math.random() - 0.5) * 0.2,
                        0.4,
                        (Math.random() - 0.5) * 0.2));
                itemEntity.setPickupDelay(20);

                // Hologram effect using invisible ArmorStand
                ArmorStand as = (ArmorStand) location.getWorld().spawn(itemEntity.getLocation(), ArmorStand.class,
                        armorStand -> {
                            armorStand.setVisible(false);
                            armorStand.setGravity(false);
                            armorStand.setCustomName(ChatUtils
                                    .colorize(item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName()
                                            : item.getType().name()));
                            armorStand.setCustomNameVisible(true);
                            armorStand.setMarker(true);
                        });

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (as.isValid())
                        as.remove();
                }, 100L);
            }, i * 5L);
        }
    }

    private int parseAmount(String amountStr) {
        if (amountStr.contains("-")) {
            String[] parts = amountStr.split("-");
            int min = Integer.parseInt(parts[0].trim());
            int max = Integer.parseInt(parts[1].trim());
            return random.nextInt(max - min + 1) + min;
        }
        return Integer.parseInt(amountStr.trim());
    }

    public void giveXpReward(List<UUID> players, int dungeonStages) {
        if (!plugin.hasAuraSkills())
            return;
        int baseXp = ConfigUtils.getInt("rewards.base-xp");
        String skill = ConfigUtils.getString("rewards.xp-skill");
        int totalXp = baseXp * dungeonStages;
        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                plugin.getAuraSkillsHook().addXp(player, skill, totalXp);
                player.sendMessage(ChatUtils.colorize("&a+" + totalXp + " " + skill + " XP!"));
            }
        }
    }
}
