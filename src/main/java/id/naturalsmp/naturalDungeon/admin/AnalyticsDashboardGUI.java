package id.naturalsmp.naturaldungeon.admin;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.dungeon.Dungeon;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import id.naturalsmp.naturaldungeon.utils.GUIUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AnalyticsDashboardGUI implements Listener {

    private final NaturalDungeon plugin;
    private final Component TITLE = Component.text("\uF808\uE212"); // Assuming E212 is analytics icon

    public AnalyticsDashboardGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = GUIUtils.createGUI(54, TITLE);
        GUIUtils.fillBorder(inv, GUIUtils.createFiller(Material.BLACK_STAINED_GLASS_PANE, "§8"));

        player.sendMessage(ChatUtils.colorize("&eLoading analytics data..."));

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Dungeon> dungeons = new ArrayList<>(plugin.getDungeonManager().getDungeons().values());

            // Add overall server stats in the center top (Slot 4)
            // But we will just list dungeons with their analytics

            int[] slots = { 10, 11, 12, 13, 14, 15, 16,
                    19, 20, 21, 22, 23, 24, 25,
                    28, 29, 30, 31, 32, 33, 34,
                    37, 38, 39, 40, 41, 42, 43 };

            int index = 0;
            for (Dungeon dungeon : dungeons) {
                if (index >= slots.length)
                    break;

                int starts = plugin.getSqliteStorage().getAnalyticsCount(dungeon.getId(), "START");
                int clears = plugin.getSqliteStorage().getAnalyticsCount(dungeon.getId(), "CLEAR");
                int wipes = plugin.getSqliteStorage().getAnalyticsCount(dungeon.getId(), "WIPE");
                long avgDuration = plugin.getSqliteStorage().getAnalyticsAverageClearTime(dungeon.getId());
                Map<Integer, Integer> deathsMap = plugin.getSqliteStorage().getDeathHeatmap(dungeon.getId());

                int totalDeaths = deathsMap.values().stream().mapToInt(Integer::intValue).sum();

                // Find deadliest stage
                int deadliestStage = -1;
                int maxStageDeaths = -1;
                for (Map.Entry<Integer, Integer> entry : deathsMap.entrySet()) {
                    if (entry.getValue() > maxStageDeaths) {
                        maxStageDeaths = entry.getValue();
                        deadliestStage = entry.getKey();
                    }
                }

                double winRate = starts > 0 ? (double) clears / starts * 100 : 0.0;

                ItemStack icon = new ItemStack(dungeon.getIcon());
                ItemMeta meta = icon.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatUtils.colorize("&6&l" + dungeon.getName()));
                    List<String> lore = new ArrayList<>();
                    lore.add(ChatUtils.colorize("&8Analytics ID: " + dungeon.getId()));
                    lore.add("");
                    lore.add(ChatUtils.colorize("&7Total Starts: &e" + starts));
                    lore.add(ChatUtils.colorize("&7Total Clears: &a" + clears));
                    lore.add(ChatUtils.colorize("&7Total Wipes: &c" + wipes));
                    lore.add(ChatUtils.colorize("&7Win Rate: &b" + String.format("%.1f", winRate) + "%"));
                    lore.add("");
                    lore.add(ChatUtils.colorize("&7Avg Clear Time: &f" + ChatUtils.formatTime(avgDuration / 1000)));
                    lore.add(ChatUtils.colorize("&7Total Deaths: &c" + totalDeaths));
                    if (deadliestStage != -1) {
                        lore.add(ChatUtils.colorize(
                                "&7Deadliest Stage: &4Stage " + deadliestStage + " &8(" + maxStageDeaths + " deaths)"));
                    }
                    lore.add("");
                    lore.add(ChatUtils.colorize("&eClick for detailed heatmap &8(WIP)"));
                    meta.setLore(lore);
                    icon.setItemMeta(meta);
                }

                inv.setItem(slots[index], icon);
                index++;
            }

            // Return button
            ItemStack back = new ItemStack(Material.DARK_OAK_DOOR);
            ItemMeta bm = back.getItemMeta();
            if (bm != null) {
                bm.setDisplayName(ChatUtils.colorize("&cReturn to Admin Menu"));
                back.setItemMeta(bm);
            }
            inv.setItem(49, back);

            Bukkit.getScheduler().runTask(plugin, () -> {
                player.openInventory(inv);
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1f);
            });
        });
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getView().title().equals(TITLE))
            return;
        e.setCancelled(true);
        if (e.getClickedInventory() == null || e.getClickedInventory() != e.getView().getTopInventory())
            return;

        Player player = (Player) e.getWhoClicked();
        ItemStack item = e.getCurrentItem();
        if (item == null || item.getType() == Material.AIR)
            return;

        player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1f);

        if (e.getSlot() == 49) {
            plugin.getAdminDashboardGUI().open(player);
        }
    }
}
