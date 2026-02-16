package id.naturalsmp.naturaldungeon.editor;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.dungeon.Dungeon;
import id.naturalsmp.naturaldungeon.dungeon.DungeonDifficulty;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * List and manage difficulties for a dungeon.
 */
public class DifficultyEditorGUI implements Listener {

    private final NaturalDungeon plugin;

    public DifficultyEditorGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String dungeonId) {
        Dungeon dungeon = plugin.getDungeonManager().getDungeon(dungeonId);
        Inventory inv = Bukkit.createInventory(new DiffEditorHolder(dungeonId), 27,
                ChatUtils.colorize("&c&lDIFFICULTIES: &e" + dungeonId));
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++)
            inv.setItem(i, filler);

        if (dungeon != null) {
            int slot = 10;
            for (DungeonDifficulty diff : dungeon.getDifficulties()) {
                if (slot > 16)
                    break;
                inv.setItem(slot, createItem(Material.IRON_SWORD,
                        "&e" + diff.getDisplayName(),
                        "&7ID: &f" + diff.getId(),
                        "&7Min Tier: &f" + diff.getMinTier(),
                        "&7Max Deaths: &c" + diff.getMaxDeaths(),
                        "&7Reward Multi: &a" + diff.getRewardMultiplier() + "x",
                        "", "&aKlik untuk edit, &cShift+Klik untuk hapus"));
                slot++;
            }
        }

        inv.setItem(22, createItem(Material.EMERALD, "&a&lTambah Difficulty"));
        inv.setItem(18, createItem(Material.ARROW, "&cKembali"));

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof DiffEditorHolder holder))
            return;
        e.setCancelled(true);
        if (e.getCurrentItem() == null)
            return;
        Player player = (Player) e.getWhoClicked();

        if (e.getSlot() == 18) {
            new DungeonMainEditorGUI(plugin).open(player, holder.dungeonId);
            return;
        }
        if (e.getSlot() == 22) {
            plugin.getEditorChatInput().requestInput(player,
                    ChatUtils.colorize("&eMasukkan ID difficulty baru (misal: normal, hard, hell):"),
                    input -> {
                        String id = input.toLowerCase().replace(" ", "_");
                        plugin.getDungeonManager().createDifficulty(holder.dungeonId, id);
                        player.sendMessage(ChatUtils.colorize("&aDifficulty &f" + id + " &adibuat!"));
                        open(player, holder.dungeonId);
                    });
            return;
        }

        // Clicked on a difficulty item
        if (e.getSlot() >= 10 && e.getSlot() <= 16) {
            Dungeon dungeon = plugin.getDungeonManager().getDungeon(holder.dungeonId);
            if (dungeon == null)
                return;
            int idx = e.getSlot() - 10;
            List<DungeonDifficulty> diffs = new ArrayList<>(dungeon.getDifficulties());
            if (idx >= diffs.size())
                return;
            DungeonDifficulty diff = diffs.get(idx);

            if (e.isShiftClick()) {
                plugin.getDungeonManager().deleteDifficulty(holder.dungeonId, diff.getId());
                player.sendMessage(ChatUtils.colorize("&cDifficulty &f" + diff.getId() + " &cdihapus!"));
                open(player, holder.dungeonId);
            } else {
                new DifficultyConfigGUI(plugin).open(player, holder.dungeonId, diff.getId());
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

    public static class DiffEditorHolder implements InventoryHolder {
        public final String dungeonId;

        public DiffEditorHolder(String id) {
            this.dungeonId = id;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
