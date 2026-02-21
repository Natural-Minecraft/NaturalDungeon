package id.naturalsmp.naturaldungeon.leaderboard;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.dungeon.Dungeon;
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

import java.util.*;

public class LeaderboardGUI implements Listener {

    private final NaturalDungeon plugin;

    public LeaderboardGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String dungeonId) {
        Dungeon dungeon = plugin.getDungeonManager().getDungeon(dungeonId);
        if (dungeon == null)
            return;

        Inventory inv = GUIUtils.createGUI(new LeaderboardHolder(dungeonId), 54,
                "&#FFD700🏆 " + dungeon.getDisplayName() + " ʟᴇᴀᴅᴇʀʙᴏᴀʀᴅ");

        GUIUtils.fillAll(inv, Material.BLACK_STAINED_GLASS_PANE);

        // ─── Leaderboard entries (top 10) ───
        List<Map<?, ?>> entries = plugin.getLeaderboardManager().getTopEntries(dungeonId);
        int[] slots = { 10, 11, 12, 13, 14, 15, 16, 19, 20, 21 };

        for (int i = 0; i < Math.min(entries.size(), slots.length); i++) {
            Map<?, ?> entry = entries.get(i);
            @SuppressWarnings("unchecked")
            List<String> players = (List<String>) entry.get("players");
            long timeMs = (long) entry.get("time");
            String rank = (String) entry.get("rank");

            String posColor = switch (i + 1) {
                case 1 -> "&#FFD700"; // Gold
                case 2 -> "&#C0C0C0"; // Silver
                case 3 -> "&#CD7F32"; // Bronze
                default -> "&#AAAAAA";
            };

            String posEmoji = switch (i + 1) {
                case 1 -> "🥇";
                case 2 -> "🥈";
                case 3 -> "🥉";
                default -> "•";
            };

            List<String> lore = new ArrayList<>();
            lore.add(GUIUtils.separator());
            lore.add("&7Waktu: &#55FF55" + ChatUtils.formatTime(timeMs / 1000));
            lore.add("&7Rank: &#FFAA00" + rank);
            if (entry.containsKey("date")) {
                lore.add("");
                lore.add("&8" + new Date((long) entry.get("date")));
            }

            inv.setItem(slots[i], GUIUtils.createItem(getRankMaterial(i + 1),
                    posColor + "&l" + posEmoji + " #" + (i + 1) + " &f" + String.join(", ", players),
                    lore));
        }

        // ─── Info (slot 31) ───
        inv.setItem(31, GUIUtils.createItem(Material.CLOCK,
                "&#55CCFF&lℹ ɪɴꜰᴏ",
                GUIUtils.separator(),
                "&7Leaderboard menampilkan",
                "&7waktu tercepat &f10 terbaik&7.",
                "",
                "&7Selesaikan dungeon lebih cepat",
                "&7untuk naik peringkat!"));

        // ─── Back button (slot 49) ───
        inv.setItem(49, GUIUtils.createItem(Material.ARROW,
                "&#FF5555&l← ᴋᴇᴍʙᴀʟɪ",
                "&7Kembali ke menu dungeon."));

        player.openInventory(inv);
        GUIUtils.playOpenSound(player);
    }

    private Material getRankMaterial(int rank) {
        return switch (rank) {
            case 1 -> Material.GOLD_BLOCK;
            case 2 -> Material.IRON_BLOCK;
            case 3 -> Material.COPPER_BLOCK;
            default -> Material.COAL_BLOCK;
        };
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof LeaderboardHolder))
            return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getView().getTopInventory())
            return;

        if (e.getCurrentItem() != null && e.getCurrentItem().getType() == Material.ARROW) {
            if (e.getWhoClicked() instanceof Player p) {
                GUIUtils.playClickSound(p);
                plugin.getDungeonManager().openDungeonGUI(p);
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof LeaderboardHolder)
            e.setCancelled(true);
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
