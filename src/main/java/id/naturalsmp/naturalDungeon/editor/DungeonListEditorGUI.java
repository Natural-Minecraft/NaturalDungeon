package id.naturalsmp.naturaldungeon.editor;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.dungeon.Dungeon;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import id.naturalsmp.naturaldungeon.utils.DungeonValidator;
import id.naturalsmp.naturaldungeon.utils.GUIUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * Lists all dungeons with validation status, create/edit options.
 * Premium admin editor following NaturalCore aesthetic standards.
 */
public class DungeonListEditorGUI implements Listener {

    private final NaturalDungeon plugin;
    private final NamespacedKey dungeonKey;

    public DungeonListEditorGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
        this.dungeonKey = new NamespacedKey(plugin, "dungeon_id");
    }

    public void open(Player player) {
        Inventory inv = GUIUtils.createGUI(new ListHolder(), 54,
                "&#FFD700⚙ ᴅᴜɴɢᴇᴏɴ ᴇᴅɪᴛᴏʀ ⚙");

        GUIUtils.fillBorder(inv, Material.BLACK_STAINED_GLASS_PANE);
        GUIUtils.fillEmpty(inv, Material.GRAY_STAINED_GLASS_PANE);

        // ─── Dungeon List ───
        int[] slots = { 10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34 };
        int idx = 0;

        for (String id : plugin.getDungeonManager().getDungeonIds()) {
            if (idx >= slots.length)
                break;
            Dungeon dungeon = plugin.getDungeonManager().getDungeon(id);
            if (dungeon == null)
                continue;

            List<String> errors = DungeonValidator.validate(plugin, dungeon);
            boolean isValid = errors.isEmpty();

            List<String> lore = new ArrayList<>();
            lore.add(GUIUtils.separator());
            lore.add("&7ID: &f" + id);
            lore.add("&7Difficulties: &f" + dungeon.getDifficulties().size());
            lore.add("&7Stages: &f" + dungeon.getTotalStages());
            lore.add("");

            if (isValid) {
                lore.add("&#55FF55✔ Siap dimainkan!");
            } else {
                lore.add("&#FF5555⚠ Setup Belum Selesai:");
                for (int j = 0; j < Math.min(4, errors.size()); j++) {
                    lore.add("  &#FF5555• " + errors.get(j));
                }
                if (errors.size() > 4) {
                    lore.add("  &8...dan " + (errors.size() - 4) + " error lainnya");
                }
            }
            lore.add("");
            lore.add("&#FFAA00&l➥ KLIK UNTUK EDIT");

            Material mat = isValid ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK;
            String color = isValid ? "&#55FF55" : "&#FF5555";

            ItemStack item = GUIUtils.createItem(mat, color + "&l" + dungeon.getDisplayName(), lore);
            ItemMeta meta = item.getItemMeta();
            meta.getPersistentDataContainer().set(dungeonKey, PersistentDataType.STRING, id);
            item.setItemMeta(meta);
            inv.setItem(slots[idx++], item);
        }

        // ─── Create New Button (slot 49) ───
        inv.setItem(49, GUIUtils.createItem(Material.EMERALD,
                "&#55FF55&l✚ ʙᴜᴀᴛ ᴅᴜɴɢᴇᴏɴ ʙᴀʀᴜ",
                GUIUtils.separator(),
                "&7Klik untuk membuat dungeon baru.",
                "",
                "&#FFAA00&l➥ KLIK"));

        player.openInventory(inv);
        GUIUtils.playOpenSound(player);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof ListHolder))
            return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getView().getTopInventory())
            return;
        if (e.getCurrentItem() == null)
            return;

        Player player = (Player) e.getWhoClicked();

        // Create new dungeon
        if (e.getCurrentItem().getType() == Material.EMERALD) {
            GUIUtils.playClickSound(player);
            player.closeInventory();
            plugin.getEditorChatInput().requestInput(player,
                    ChatUtils.colorize("&#FFD700&l⚙ &7Masukkan ID dungeon baru &f(huruf kecil, tanpa spasi)&7:"),
                    input -> {
                        String id = input.toLowerCase().replace(" ", "_");
                        plugin.getDungeonManager().createEmptyDungeon(id);
                        player.sendMessage(
                                ChatUtils.colorize("&#55FF55✔ Dungeon &f" + id + " &#55FF55berhasil dibuat!"));
                        GUIUtils.playSuccessSound(player);
                        new DungeonMainEditorGUI(plugin).open(player, id);
                    });
            return;
        }

        // Open dungeon editor
        ItemMeta meta = e.getCurrentItem().getItemMeta();
        if (meta == null)
            return;
        String dungeonId = meta.getPersistentDataContainer().get(dungeonKey, PersistentDataType.STRING);
        if (dungeonId != null) {
            GUIUtils.playClickSound(player);
            new DungeonMainEditorGUI(plugin).open(player, dungeonId);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof ListHolder)
            e.setCancelled(true);
    }

    public static class ListHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
