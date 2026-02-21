package id.naturalsmp.naturaldungeon.editor;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.dungeon.Dungeon;
import id.naturalsmp.naturaldungeon.dungeon.DungeonDifficulty;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import id.naturalsmp.naturaldungeon.utils.DungeonValidator;
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

        Dungeon dungeon = plugin.getDungeonManager().getDungeon(dungeonId);
        int stageCount = dungeon != null ? dungeon.getTotalStages() : 0;
        List<Dungeon.Stage> stages = dungeon != null ? dungeon.getStages() : new ArrayList<>();

        for (int i = 0; i < stageCount && i < 18; i++) {
            Dungeon.Stage stage = (i < stages.size()) ? stages.get(i) : null;
            List<String> errors = stage != null ? DungeonValidator.validateStage(plugin, dungeon, stage)
                    : Collections.singletonList("Stage data not found");
            boolean isValid = errors.isEmpty();

            List<String> lore = new ArrayList<>();
            if (isValid) {
                lore.add("&a✔ Siap dimainkan!");
            } else {
                lore.add("&c⚠ Setup Belum Selesai:");
                for (int j = 0; j < Math.min(3, errors.size()); j++) {
                    String prefix = errors.get(j).contains("tidak ditemukan di WorldGuard") ? "&e" : "&c";
                    lore.add(" " + prefix + errors.get(j));
                }
                if (errors.size() > 3) {
                    lore.add(" &c...dan " + (errors.size() - 3) + " error lainnya");
                }
            }
            lore.add("");
            lore.add("&aKlik untuk edit waves");
            lore.add("&cShift+Klik untuk hapus");

            Material icon = isValid ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK;

            inv.setItem(i + 9, createItem(icon,
                    (isValid ? "&a&l" : "&c&l") + "Stage " + (i + 1),
                    lore.toArray(new String[0])));
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
