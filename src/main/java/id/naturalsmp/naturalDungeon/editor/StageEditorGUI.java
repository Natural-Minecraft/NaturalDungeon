package id.naturalsmp.naturaldungeon.editor;

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

import java.util.*;

/**
 * List and manage stages per difficulty.
 */
public class StageEditorGUI implements Listener {

    private final NaturalDungeon plugin;

    public StageEditorGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String dungeonId) {
        Inventory inv = Bukkit.createInventory(new StageEditorHolder(dungeonId), 36,
                ChatUtils.colorize("&e&lSTAGES: &e" + dungeonId));
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 36; i++)
            inv.setItem(i, filler);

        // Get stage count from config
        int stageCount = plugin.getDungeonManager().getStageCount(dungeonId);
        for (int i = 0; i < stageCount && i < 18; i++) {
            inv.setItem(i + 9, createItem(Material.LADDER,
                    "&e&lStage " + (i + 1),
                    "&aKlik untuk edit waves",
                    "&cShift+Klik untuk hapus"));
        }

        inv.setItem(31, createItem(Material.EMERALD, "&a&lTambah Stage"));
        inv.setItem(27, createItem(Material.ARROW, "&cKembali"));

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof StageEditorHolder holder))
            return;
        e.setCancelled(true);
        if (e.getCurrentItem() == null)
            return;
        Player player = (Player) e.getWhoClicked();

        if (e.getSlot() == 27) {
            new DungeonMainEditorGUI(plugin).open(player, holder.dungeonId);
            return;
        }
        if (e.getSlot() == 31) {
            plugin.getDungeonManager().addStage(holder.dungeonId);
            player.sendMessage(ChatUtils.colorize("&aStage baru ditambahkan!"));
            open(player, holder.dungeonId);
            return;
        }
        if (e.getSlot() >= 9 && e.getSlot() <= 26) {
            int stageIndex = e.getSlot() - 9;
            if (e.isShiftClick()) {
                plugin.getDungeonManager().deleteStage(holder.dungeonId, stageIndex);
                player.sendMessage(ChatUtils.colorize("&cStage dihapus!"));
                open(player, holder.dungeonId);
            } else {
                new WaveEditorGUI(plugin).open(player, holder.dungeonId, stageIndex);
            }
        }
    }

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.colorize(name));
        List<String> list = new ArrayList<>();
        for (String l : lore)
            list.add(ChatUtils.colorize(l));
        meta.setLore(list);
        item.setItemMeta(meta);
        return item;
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
