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
 * Loot table editor: add/remove/edit loot entries.
 */
public class RewardEditorGUI implements Listener {

    private final NaturalDungeon plugin;

    public RewardEditorGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String dungeonId) {
        Inventory inv = Bukkit.createInventory(new RewardHolder(dungeonId), 36,
                ChatUtils.colorize("&a&lREWARDS: &e" + dungeonId));
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 36; i++)
            inv.setItem(i, filler);

        // List existing loot entries
        List<String> entries = plugin.getDungeonManager().getLootEntries(dungeonId);
        int slot = 9;
        for (int i = 0; i < entries.size() && slot <= 26; i++) {
            inv.setItem(slot, createItem(Material.GOLD_INGOT,
                    "&e" + entries.get(i),
                    "&aKlik untuk edit",
                    "&cShift+Klik untuk hapus"));
            slot++;
        }

        inv.setItem(31, createItem(Material.EMERALD, "&a&lTambah Loot Entry"));
        inv.setItem(27, createItem(Material.ARROW, "&cKembali"));

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof RewardHolder holder))
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
            plugin.getDungeonManager().addLootEntry(holder.dungeonId);
            player.sendMessage(ChatUtils.colorize("&aLoot entry baru ditambahkan!"));
            open(player, holder.dungeonId);
            return;
        }
        if (e.getSlot() >= 9 && e.getSlot() <= 26) {
            int idx = e.getSlot() - 9;
            if (e.isShiftClick()) {
                plugin.getDungeonManager().deleteLootEntry(holder.dungeonId, idx);
                player.sendMessage(ChatUtils.colorize("&cLoot entry dihapus!"));
                open(player, holder.dungeonId);
            } else {
                new LootEntryEditorGUI(plugin).open(player, holder.dungeonId, idx);
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
