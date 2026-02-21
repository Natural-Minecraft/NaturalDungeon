package id.naturalsmp.naturaldungeon.editor;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.dungeon.Dungeon;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import id.naturalsmp.naturaldungeon.utils.DungeonValidator;
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
 * List and manage stages for a dungeon.
 */
public class StageEditorGUI implements Listener {

    private final NaturalDungeon plugin;

    public StageEditorGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String dungeonId) {
        Inventory inv = GUIUtils.createGUI(new StageEditorHolder(dungeonId), 36,
                "&#FFAA00🏗 ꜱᴛᴀɢᴇꜱ: &f" + dungeonId);

        GUIUtils.fillAll(inv, Material.BLACK_STAINED_GLASS_PANE);

        Dungeon dungeon = plugin.getDungeonManager().getDungeon(dungeonId);
        int stageCount = dungeon != null ? dungeon.getTotalStages() : 0;
        List<Dungeon.Stage> stages = dungeon != null ? dungeon.getStages() : new ArrayList<>();

        for (int i = 0; i < stageCount && i < 18; i++) {
            Dungeon.Stage stage = (i < stages.size()) ? stages.get(i) : null;
            List<String> errors = stage != null ? DungeonValidator.validateStage(plugin, dungeon, stage)
                    : Collections.singletonList("Stage data not found");
            boolean isValid = errors.isEmpty();

            List<String> lore = new ArrayList<>();
            lore.add(GUIUtils.separator());
            if (isValid) {
                lore.add("&#55FF55✔ Siap dimainkan!");
            } else {
                lore.add("&#FF5555⚠ Setup belum selesai:");
                for (int j = 0; j < Math.min(3, errors.size()); j++) {
                    lore.add("  &#FF5555• " + errors.get(j));
                }
                if (errors.size() > 3) {
                    lore.add("  &8...dan " + (errors.size() - 3) + " lainnya");
                }
            }
            lore.add("");
            lore.add("&#FFAA00&l⚔ Klik &7→ Edit Waves");
            lore.add("&#FF5555&l✖ Shift+Klik &7→ Hapus");

            Material icon = isValid ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK;
            String color = isValid ? "&#55FF55" : "&#FF5555";

            inv.setItem(i + 9, GUIUtils.createItem(icon,
                    color + "&lStage " + (i + 1),
                    lore));
        }

        // Add Stage button
        inv.setItem(31, GUIUtils.createItem(Material.EMERALD,
                "&#55FF55&l✚ ᴛᴀᴍʙᴀʜ ꜱᴛᴀɢᴇ",
                GUIUtils.separator(),
                "&7Tambah stage baru.",
                "",
                "&#FFAA00&l➥ KLIK"));

        // Back button
        inv.setItem(27, GUIUtils.createItem(Material.ARROW,
                "&#FF5555&l← ᴋᴇᴍʙᴀʟɪ"));

        player.openInventory(inv);
        GUIUtils.playOpenSound(player);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof StageEditorHolder holder))
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
        if (e.getSlot() == 31) {
            plugin.getDungeonManager().addStage(holder.dungeonId);
            player.sendMessage(ChatUtils.colorize("&#55FF55✔ Stage baru ditambahkan!"));
            GUIUtils.playSuccessSound(player);
            open(player, holder.dungeonId);
            return;
        }
        if (e.getSlot() >= 9 && e.getSlot() <= 26) {
            int stageIndex = e.getSlot() - 9;
            if (e.isShiftClick()) {
                plugin.getDungeonManager().deleteStage(holder.dungeonId, stageIndex);
                player.sendMessage(ChatUtils.colorize("&#FF5555✖ Stage dihapus!"));
                open(player, holder.dungeonId);
            } else {
                new WaveEditorGUI(plugin).open(player, holder.dungeonId, stageIndex);
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof StageEditorHolder)
            e.setCancelled(true);
    }

    public static class StageEditorHolder implements InventoryHolder {
        public final String dungeonId;

        public StageEditorHolder(String id) {
            this.dungeonId = id;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
