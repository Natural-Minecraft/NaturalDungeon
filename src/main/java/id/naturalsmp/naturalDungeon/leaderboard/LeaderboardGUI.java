package id.naturalsmp.naturaldungeon.leaderboard;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.dungeon.Dungeon;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import id.naturalsmp.naturaldungeon.utils.ConfigUtils;
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

import java.util.*;

public class LeaderboardGUI implements Listener {

    private final NaturalDungeon plugin;

    public LeaderboardGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
        // Listener registered as singleton in NaturalDungeon.registerListeners()
    }

    public void open(Player player, String dungeonId) {
        Dungeon dungeon = plugin.getDungeonManager().getDungeon(dungeonId);
        if (dungeon == null)
            return;

        Inventory inv = Bukkit.createInventory(new LeaderboardHolder(dungeonId), 54,
                ChatUtils.colorize("&#FFBB00❂ &#FFFFFF" + dungeon.getDisplayName() + " &#FFBB00ʟᴇᴀᴅᴇʀʙᴏᴀʀᴅ"));

        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);
        for (int i = 0; i < 54; i++)
            inv.setItem(i, filler);

        List<Map<?, ?>> entries = plugin.getLeaderboardManager().getTopEntries(dungeonId);
        int[] slots = { 10, 11, 12, 13, 14, 15, 16, 19, 20, 21 }; // Top 10

        for (int i = 0; i < Math.min(entries.size(), slots.length); i++) {
            Map<?, ?> entry = entries.get(i);
            List<String> players = (List<String>) entry.get("players");
            long timeMs = (long) entry.get("time");
            String rank = (String) entry.get("rank");

            ItemStack item = new ItemStack(getRankMaterial(i + 1));
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatUtils.colorize("&6#" + (i + 1) + " &f" + String.join(", ", players)));

            List<String> lore = new ArrayList<>();
            lore.add(ChatUtils
                    .colorize("&7Waktu: &e" + id.naturalsmp.naturaldungeon.utils.ChatUtils.formatTime(timeMs / 1000)));
            lore.add(ChatUtils.colorize("&7Rank: &#FFBB00" + rank));
            lore.add("");
            lore.add(ChatUtils.colorize("&8Tanggal: " + new Date((long) entry.get("date")).toString()));
            meta.setLore(lore);
            item.setItemMeta(meta);

            inv.setItem(slots[i], item);
        }

        inv.setItem(49, createItem(Material.ARROW, "&cKembali"));
        player.openInventory(inv);
    }

    private Material getRankMaterial(int rank) {
        return switch (rank) {
            case 1 -> Material.GOLD_BLOCK;
            case 2 -> Material.IRON_BLOCK;
            case 3 -> Material.COPPER_BLOCK;
            default -> Material.COAL_BLOCK;
        };
    }

    private ItemStack createItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.colorize(name));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof LeaderboardHolder))
            return;
        e.setCancelled(true);
        if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.ARROW) {
            if (e.getWhoClicked() instanceof Player p) {
                plugin.getDungeonManager().openDungeonGUI(p);
            }
        }
    }

    public static class LeaderboardHolder implements InventoryHolder {
        private final String dungeonId;

        public LeaderboardHolder(String dungeonId) {
            this.dungeonId = dungeonId;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
