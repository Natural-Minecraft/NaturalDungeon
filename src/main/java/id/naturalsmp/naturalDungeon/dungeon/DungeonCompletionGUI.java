package id.naturalsmp.naturaldungeon.dungeon;

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

import java.util.*;

public class DungeonCompletionGUI implements Listener {

    private final NaturalDungeon plugin;

    public DungeonCompletionGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, DungeonInstance instance, boolean victory) {
        String title = victory
                ? "&#55FF55✦ ᴅᴜɴɢᴇᴏɴ ᴄᴏᴍᴘʟᴇᴛᴇ ✦"
                : "&#FF5555✖ ᴅᴜɴɢᴇᴏɴ ꜰᴀɪʟᴇᴅ ✖";

        Inventory inv = GUIUtils.createGUI(new CompletionHolder(), 45, title);
        GUIUtils.fillAll(inv, Material.BLACK_STAINED_GLASS_PANE);

        if (victory) {
            buildVictoryGUI(inv, player, instance);
        } else {
            buildDefeatGUI(inv, player, instance);
        }

        // Close button (slot 40)
        inv.setItem(40, GUIUtils.createItem(Material.BARRIER,
                "&#FF5555&l✖ ᴛᴜᴛᴜᴘ",
                "&7Klik untuk menutup."));

        player.openInventory(inv);
        GUIUtils.playOpenSound(player);
    }

    private void buildVictoryGUI(Inventory inv, Player player, DungeonInstance instance) {
        long duration = instance.getDuration() / 1000;
        String timeStr = ChatUtils.formatTime(duration);
        String rank = instance.getPerformanceRank();
        UUID mvpUUID = instance.getMVP();

        // ─── Trophy (slot 4) ───
        inv.setItem(4, GUIUtils.createItem(Material.NETHER_STAR,
                "&#FFD700&l✦ ᴠɪᴄᴛᴏʀʏ ✦",
                GUIUtils.separator(),
                "&#FFD700⚔ " + instance.getDungeon().getDisplayName(),
                "&7Difficulty: &#FFAA00" + instance.getDifficulty().getDisplayName(),
                "",
                "&7Waktu: &#55FF55" + timeStr,
                "&7Rank: " + getRankColor(rank) + "&l" + rank,
                GUIUtils.separator()));

        // ─── MVP (slot 20) ───
        String mvpName = mvpUUID != null ? org.bukkit.Bukkit.getOfflinePlayer(mvpUUID).getName() : "N/A";
        inv.setItem(20, GUIUtils.createItem(Material.DIAMOND_SWORD,
                "&#FFD700&l🏆 ᴍᴠᴘ",
                GUIUtils.separator(),
                "&7Player: &#FFAA00" + mvpName,
                "&7Damage Dealt: &f" + ChatUtils.formatLarge(instance.getDamageDealt(mvpUUID)),
                "&7Mobs Killed: &f" + instance.getMobsKilled(mvpUUID)));

        // ─── Personal Stats (slot 22) ───
        inv.setItem(22, GUIUtils.createItem(Material.BOOK,
                "&#AA44FF&l📊 ꜱᴛᴀᴛɪꜱᴛɪᴋ ᴋᴀᴍᴜ",
                GUIUtils.separator(),
                "&7Damage: &f" + ChatUtils.formatLarge(instance.getDamageDealt(player.getUniqueId())),
                "&7Taken: &f" + ChatUtils.formatLarge(instance.getDamageTaken(player.getUniqueId())),
                "&7Kills: &f" + instance.getMobsKilled(player.getUniqueId()),
                "&7Deaths: &c" + instance.getTotalDeaths()));

        // ─── Loot Preview (slot 24) ───
        List<ItemStack> loot = instance.getCollectedLoot();
        List<String> lootLore = new ArrayList<>();
        lootLore.add(GUIUtils.separator());
        if (loot != null && !loot.isEmpty()) {
            for (int i = 0; i < Math.min(5, loot.size()); i++) {
                ItemStack item = loot.get(i);
                String name = item.getType().name().replace("_", " ");
                lootLore.add("&#FFAA00 • &f" + name + " &7x" + item.getAmount());
            }
            if (loot.size() > 5) {
                lootLore.add("&8  ...dan " + (loot.size() - 5) + " lainnya");
            }
        } else {
            lootLore.add("&7Tidak ada loot.");
        }
        inv.setItem(24, GUIUtils.createItem(Material.CHEST,
                "&#55FF55&l🎁 ʟᴏᴏᴛ",
                lootLore));
    }

    private void buildDefeatGUI(Inventory inv, Player player, DungeonInstance instance) {
        // ─── Defeat Info (slot 4) ───
        inv.setItem(4, GUIUtils.createItem(Material.SKELETON_SKULL,
                "&#FF5555&l✖ ᴅᴇꜰᴇᴀᴛ ✖",
                GUIUtils.separator(),
                "&#FF5555⚔ " + instance.getDungeon().getDisplayName(),
                "&7Difficulty: &#FFAA00" + instance.getDifficulty().getDisplayName(),
                "",
                "&7Terakhir: Stage &f" + instance.getCurrentStage(),
                "&7Total Deaths: &#FF5555" + instance.getTotalDeaths(),
                GUIUtils.separator(),
                "&7Coba lagi lain kali!"));

        // ─── Stats (slot 22) ───
        inv.setItem(22, GUIUtils.createItem(Material.BOOK,
                "&#AA44FF&l📊 ꜱᴛᴀᴛɪꜱᴛɪᴋ",
                GUIUtils.separator(),
                "&7Damage: &f" + ChatUtils.formatLarge(instance.getDamageDealt(player.getUniqueId())),
                "&7Kills: &f" + instance.getMobsKilled(player.getUniqueId())));
    }

    private String getRankColor(String rank) {
        return switch (rank) {
            case "S" -> "&#FFD700";
            case "A" -> "&#55FF55";
            case "B" -> "&#55CCFF";
            case "C" -> "&#FFAA00";
            default -> "&#FF5555";
        };
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof CompletionHolder))
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
        if (e.getInventory().getHolder() instanceof CompletionHolder)
            e.setCancelled(true);
    }

    public static class CompletionHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
