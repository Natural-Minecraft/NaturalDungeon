package id.naturalsmp.naturaldungeon.editor;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.dungeon.Dungeon;
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
 * Lists all dungeons with option to create/edit/delete.
 */
public class DungeonListEditorGUI implements Listener {

    private final NaturalDungeon plugin;

    public DungeonListEditorGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(new ListHolder(), 54, ChatUtils.colorize("&6&lDUNGEON EDITOR"));

        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++)
            inv.setItem(i, filler);

        int slot = 10;
        for (String id : plugin.getDungeonManager().getDungeonIds()) {
            Dungeon dungeon = plugin.getDungeonManager().getDungeon(id);
            if (dungeon == null || slot > 43)
                continue;

            ItemStack item = createItem(Material.CHEST,
                    "&e&l" + dungeon.getDisplayName(),
                    "&7ID: &f" + id,
                    "&7Difficulties: &f" + dungeon.getDifficulties().size(),
                    "", "&aKlik untuk edit!");
            ItemMeta meta = item.getItemMeta();
            meta.getPersistentDataContainer().set(
                    new org.bukkit.NamespacedKey(plugin, "dungeon_id"),
                    org.bukkit.persistence.PersistentDataType.STRING, id);
            item.setItemMeta(meta);
            inv.setItem(slot, item);

            slot++;
            if (slot % 9 == 8)
                slot += 2; // Skip borders
        }

        // Create New button
        inv.setItem(49, createItem(Material.EMERALD, "&a&lBuat Dungeon Baru", "&7Klik untuk membuat dungeon baru"));

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof ListHolder))
            return;
        e.setCancelled(true);
        if (e.getCurrentItem() == null)
            return;

        Player player = (Player) e.getWhoClicked();

        if (e.getCurrentItem().getType() == Material.EMERALD) {
            // Create new dungeon
            plugin.getEditorChatInput().requestInput(player,
                    ChatUtils.colorize("&e&lMasukkan ID dungeon baru (huruf kecil, tanpa spasi):"),
                    input -> {
                        String id = input.toLowerCase().replace(" ", "_");
                        player.sendMessage(ChatUtils.colorize("&aDungeon &f" + id + " &adibuat!"));
                        // Create a bare config file, then open editor
                        plugin.getDungeonManager().createEmptyDungeon(id);
                        new DungeonMainEditorGUI(plugin).open(player, id);
                    });
            return;
        }

        ItemMeta meta = e.getCurrentItem().getItemMeta();
        if (meta == null)
            return;
        String dungeonId = meta.getPersistentDataContainer().get(
                new org.bukkit.NamespacedKey(plugin, "dungeon_id"),
                org.bukkit.persistence.PersistentDataType.STRING);
        if (dungeonId != null) {
            new DungeonMainEditorGUI(plugin).open(player, dungeonId);
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

    public static class ListHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
