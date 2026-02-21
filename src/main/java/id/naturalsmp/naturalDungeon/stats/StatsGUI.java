package id.naturalsmp.naturaldungeon.stats;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import id.naturalsmp.naturaldungeon.utils.GUIUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
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

                Inventory inv = GUIUtils.createGUI(new StatsHolder(), 45,
                                "&#AA44FF📊 ꜱᴛᴀᴛɪꜱᴛɪᴋ &7— &f" + target.getName());

                GUIUtils.fillBorder(inv, Material.BLACK_STAINED_GLASS_PANE);
                GUIUtils.fillEmpty(inv, Material.GRAY_STAINED_GLASS_PANE);

                // ─── Player Head (slot 4) ───
                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
                skullMeta.setOwningPlayer(target);
                skullMeta.displayName(GUIUtils.toComponent("&#FFD700&l" + target.getName()));
                skullMeta.lore(List.of(
                                GUIUtils.toComponent(GUIUtils.separator()),
                                GUIUtils.toComponent("&7Dungeon Adventurer"),
                                GUIUtils.toComponent("&7Total Clears: &#55FF55" + stats.getTotalClears())));
                head.setItemMeta(skullMeta);
                inv.setItem(4, head);

                // ─── Combat Stats Row (slots 19-21) ───
                inv.setItem(19, GUIUtils.createItem(Material.DIAMOND_SWORD,
                                "&#55FF55&l⚔ ᴛᴏᴛᴀʟ ᴄʟᴇᴀʀꜱ",
                                GUIUtils.separator(),
                                "  &7» &#55FF55" + stats.getTotalClears()));

                inv.setItem(20, GUIUtils.createItem(Material.SKELETON_SKULL,
                                "&#FF5555&l💀 ᴛᴏᴛᴀʟ ᴅᴇᴀᴛʜꜱ",
                                GUIUtils.separator(),
                                "  &7» &#FF5555" + stats.getTotalDeaths()));

                inv.setItem(21, GUIUtils.createItem(Material.NETHER_STAR,
                                "&#AA44FF&l✦ ꜰʟᴀᴡʟᴇꜱꜱ ᴄʟᴇᴀʀꜱ",
                                GUIUtils.separator(),
                                "  &7» &#AA44FF" + stats.getFlawlessClears()));

                // ─── Performance Stats Row (slots 23-25) ───
                inv.setItem(23, GUIUtils.createItem(Material.GOLD_NUGGET,
                                "&#FFAA00&l📅 ᴅᴀɪʟʏ ᴄʟᴇᴀʀꜱ",
                                GUIUtils.separator(),
                                "  &7» &#FFAA00" + stats.getDailyClears()));

                inv.setItem(24, GUIUtils.createItem(Material.GOLD_INGOT,
                                "&#FFD700&l📆 ᴡᴇᴇᴋʟʏ ᴄʟᴇᴀʀꜱ",
                                GUIUtils.separator(),
                                "  &7» &#FFD700" + stats.getWeeklyClears()));

                String timeStr = stats.getFastestTime() == Long.MAX_VALUE ? "N/A"
                                : formatTime(stats.getFastestTime());
                inv.setItem(25, GUIUtils.createItem(Material.CLOCK,
                                "&#55CCFF&l⏱ ꜰᴀꜱᴛᴇꜱᴛ ᴛɪᴍᴇ",
                                GUIUtils.separator(),
                                "  &7» &#55CCFF" + timeStr));

                // ─── Damage Stats (slots 29-30) ───
                inv.setItem(29, GUIUtils.createItem(Material.IRON_SWORD,
                                "&#FFAA00&l💥 ᴅᴀᴍᴀɢᴇ ᴅᴇᴀʟᴛ",
                                GUIUtils.separator(),
                                "  &7» &f" + String.format("%.0f", stats.getTotalDamageDealt())));

                inv.setItem(30, GUIUtils.createItem(Material.SHIELD,
                                "&#FF5555&l🛡 ᴅᴀᴍᴀɢᴇ ᴛᴀᴋᴇɴ",
                                GUIUtils.separator(),
                                "  &7» &f" + String.format("%.0f", stats.getTotalDamageTaken())));

                // ─── Close Button (slot 40) ───
                inv.setItem(40, GUIUtils.createItem(Material.BARRIER,
                                "&#FF5555&l✖ ᴛᴜᴛᴜᴘ",
                                "&7Klik untuk menutup."));

                viewer.openInventory(inv);
                GUIUtils.playOpenSound(viewer);
        }

        @EventHandler
        public void onClick(InventoryClickEvent e) {
                if (!(e.getInventory().getHolder() instanceof StatsHolder))
                        return;
                e.setCancelled(true);
                if (e.getClickedInventory() != e.getView().getTopInventory())
                        return;
                if (e.getCurrentItem() != null && e.getCurrentItem().getType() == Material.BARRIER) {
                        GUIUtils.playClickSound((Player) e.getWhoClicked());
                        e.getWhoClicked().closeInventory();
                }
        }

        @EventHandler
        public void onDrag(InventoryDragEvent e) {
                if (e.getInventory().getHolder() instanceof StatsHolder)
                        e.setCancelled(true);
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
