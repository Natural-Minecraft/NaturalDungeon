package id.naturalsmp.naturaldungeon.stats;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

/**
 * GUI to display player's dungeon statistics.
 */
public class StatsGUI implements Listener {

    private final NaturalDungeon plugin;

    public StatsGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        open(player, player);
    }

    public void open(Player viewer, Player target) {
        PlayerStats stats = plugin.getPlayerStatsManager().getStats(target.getUniqueId());
        String title = ChatUtils.colorize("&6&lSTATISTIK &7" + target.getName());
        Inventory inv = Bukkit.createInventory(new StatsHolder(), 27, title);

        // Fill border
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++)
            inv.setItem(i, filler);

        // Player head
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        skullMeta.setOwningPlayer(target);
        skullMeta.setDisplayName(ChatUtils.colorize("&b&l" + target.getName()));
        skullMeta.setLore(Arrays.asList(
                ChatUtils.colorize("&7Dungeon Statistics"),
                ""));
        head.setItemMeta(skullMeta);
        inv.setItem(4, head);

        // Total Clears
        inv.setItem(10, createStatItem(Material.DIAMOND_SWORD, "&a&lTotal Clears",
                "&f" + stats.getTotalClears()));

        // Total Deaths
        inv.setItem(11, createStatItem(Material.SKELETON_SKULL, "&c&lTotal Deaths",
                "&f" + stats.getTotalDeaths()));

        // Flawless Clears
        inv.setItem(12, createStatItem(Material.NETHER_STAR, "&d&lFlawless Clears",
                "&f" + stats.getFlawlessClears()));

        // Daily Clears
        inv.setItem(13, createStatItem(Material.GOLD_NUGGET, "&e&lDaily Clears",
                "&f" + stats.getDailyClears()));

        // Fastest Time
        String timeStr = stats.getFastestTime() == Long.MAX_VALUE ? "N/A"
                : formatTime(stats.getFastestTime());
        inv.setItem(14, createStatItem(Material.CLOCK, "&e&lFastest Time",
                "&f" + timeStr));

        // Damage Dealt
        inv.setItem(15, createStatItem(Material.IRON_SWORD, "&6&lDamage Dealt",
                "&f" + String.format("%.0f", stats.getTotalDamageDealt())));

        // Damage Taken
        inv.setItem(16, createStatItem(Material.SHIELD, "&4&lDamage Taken",
                "&f" + String.format("%.0f", stats.getTotalDamageTaken())));

        // Weekly Clears
        inv.setItem(21, createStatItem(Material.GOLD_INGOT, "&6&lWeekly Clears",
                "&f" + stats.getWeeklyClears()));

        // Close
        inv.setItem(22, createItem(Material.BARRIER, "&cTutup"));

        viewer.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof StatsHolder))
            return;
        e.setCancelled(true);
        if (e.getCurrentItem() != null && e.getCurrentItem().getType() == Material.BARRIER) {
            e.getWhoClicked().closeInventory();
        }
    }

    private ItemStack createStatItem(Material mat, String name, String value) {
        return createItem(mat, name, "", ChatUtils.colorize("  &7» " + value));
    }

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.colorize(name));
        if (lore.length > 0) {
            List<String> list = new ArrayList<>();
            for (String l : lore)
                list.add(ChatUtils.colorize(l));
            meta.setLore(list);
        }
        item.setItemMeta(meta);
        return item;
    }

    private String formatTime(long ms) {
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        seconds %= 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public static class StatsHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
