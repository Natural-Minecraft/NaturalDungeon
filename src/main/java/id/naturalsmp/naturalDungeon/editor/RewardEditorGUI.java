package id.naturalsmp.naturaldungeon.editor;

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

import java.util.*;

/**
 * Loot table editor: add/remove/edit loot entries.
 */
public class RewardEditorGUI implements Listener {

    private final NaturalDungeon plugin;

    public RewardEditorGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String dungeonId) {
        Inventory inv = GUIUtils.createGUI(new RewardHolder(dungeonId), 36,
                "&#55FF55🎁 ʀᴇᴡᴀʀᴅꜱ: &f" + dungeonId);

        GUIUtils.fillAll(inv, Material.BLACK_STAINED_GLASS_PANE);

        // List existing loot entries
        List<String> entries = plugin.getDungeonManager().getLootEntries(dungeonId);
        int slot = 9;
        for (int i = 0; i < entries.size() && slot <= 26; i++) {
            inv.setItem(slot, GUIUtils.createItem(Material.GOLD_INGOT,
                    "&#FFD700&l" + entries.get(i),
                    GUIUtils.separator(),
                    "&7Loot entry #" + (i + 1),
                    "",
                    "&#FFAA00&l⚔ Klik &7→ Edit",
                    "&#FF5555&l✖ Shift+Klik &7→ Hapus"));
            slot++;
        }

        inv.setItem(31, GUIUtils.createItem(Material.EMERALD,
                "&#55FF55&l✚ ᴛᴀᴍʙᴀʜ ʟᴏᴏᴛ",
                GUIUtils.separator(),
                "&7Tambah loot entry baru.",
                "",
                "&#FFAA00&l➥ KLIK"));

        inv.setItem(27, GUIUtils.createItem(Material.ARROW,
                "&#FF5555&l← ᴋᴇᴍʙᴀʟɪ"));

        inv.setItem(35, GUIUtils.createItem(Material.HOPPER,
                "&#FFD700&l🔄 ʟᴏᴏᴛ ꜱɪᴍᴜʟᴀᴛᴏʀ",
                GUIUtils.separator(),
                "&7Jalankan uji coba Monte Carlo",
                "&7dan evaluasi probabilitas drop.",
                "",
                "&#FFAA00&l➥ KLIK"));

        player.openInventory(inv);
        GUIUtils.playOpenSound(player);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof RewardHolder holder))
            return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getView().getTopInventory())
            return;
        if (e.getCurrentItem() == null)
            return;

        Player player = (Player) e.getWhoClicked();
        GUIUtils.playClickSound(player);

        if (e.getSlot() == 27) {
            new DungeonMainEditorGUI(plugin).open(player, holder.dungeonId);
            return;
        }
        if (e.getSlot() == 35) {
            new LootSimulatorGUI(plugin).open(player, holder.dungeonId);
            return;
        }
        if (e.getSlot() == 31) {
            plugin.getDungeonManager().addLootEntry(holder.dungeonId);
            player.sendMessage(ChatUtils.colorize("&#55FF55✔ Loot entry ditambahkan!"));
            GUIUtils.playSuccessSound(player);
            open(player, holder.dungeonId);
            return;
        }
        if (e.getSlot() >= 9 && e.getSlot() <= 26) {
            int idx = e.getSlot() - 9;
            if (e.isShiftClick()) {
                plugin.getDungeonManager().deleteLootEntry(holder.dungeonId, idx);
                player.sendMessage(ChatUtils.colorize("&#FF5555✖ Loot entry dihapus!"));
                open(player, holder.dungeonId);
            } else {
                new LootEntryEditorGUI(plugin).open(player, holder.dungeonId, idx);
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof RewardHolder)
            e.setCancelled(true);
    }

    public static class RewardHolder implements InventoryHolder {
        public final String dungeonId;

        public RewardHolder(String id) {
            this.dungeonId = id;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
