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
 * Edit basic info: name, description, world, max-players, cooldown, icon
 * material.
 */
public class BasicInfoEditorGUI implements Listener {

    private final NaturalDungeon plugin;

    public BasicInfoEditorGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String dungeonId) {
        Dungeon dungeon = plugin.getDungeonManager().getDungeon(dungeonId);
        Inventory inv = Bukkit.createInventory(new InfoHolder(dungeonId), 27,
                ChatUtils.colorize("&b&lBASIC INFO: &e" + dungeonId));
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++)
            inv.setItem(i, filler);

        String name = dungeon != null ? dungeon.getDisplayName() : dungeonId;
        inv.setItem(10, createItem(Material.NAME_TAG, "&e&lNama", "&7Saat ini: &f" + name, "", "&aKlik untuk ubah"));
        inv.setItem(11, createItem(Material.BOOK, "&e&lDeskripsi", "&aKlik untuk ubah"));
        inv.setItem(12, createItem(Material.GRASS_BLOCK, "&e&lWorld", "&aKlik untuk set world"));
        inv.setItem(14, createItem(Material.IRON_CHESTPLATE, "&e&lMax Players", "&aKlik untuk ubah"));
        inv.setItem(15, createItem(Material.CLOCK, "&e&lCooldown", "&aKlik untuk ubah cooldown"));
        inv.setItem(16, createItem(Material.ITEM_FRAME, "&e&lIcon Material", "&aKlik untuk ubah icon"));
        inv.setItem(22, createItem(Material.ARROW, "&cKembali"));

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof InfoHolder holder))
            return;
        e.setCancelled(true);
        if (e.getCurrentItem() == null)
            return;
        Player player = (Player) e.getWhoClicked();
        String id = holder.dungeonId;

        switch (e.getSlot()) {
            case 10 -> plugin.getEditorChatInput().requestInput(player,
                    ChatUtils.colorize("&eMasukkan nama baru:"), input -> {
                        plugin.getDungeonManager().setDungeonConfig(id, "display-name", input);
                        player.sendMessage(ChatUtils.colorize("&aNama diubah ke: &f" + input));
                        open(player, id);
                    });
            case 11 -> plugin.getEditorChatInput().requestInput(player,
                    ChatUtils.colorize("&eMasukkan deskripsi:"), input -> {
                        plugin.getDungeonManager().setDungeonConfig(id, "description", input);
                        player.sendMessage(ChatUtils.colorize("&aDeskripsi diubah!"));
                        open(player, id);
                    });
            case 12 -> plugin.getEditorChatInput().requestInput(player,
                    ChatUtils.colorize("&eMasukkan nama world:"), input -> {
                        plugin.getDungeonManager().setDungeonConfig(id, "world", input);
                        player.sendMessage(ChatUtils.colorize("&aWorld diset: &f" + input));
                        open(player, id);
                    });
            case 14 -> plugin.getEditorChatInput().requestInput(player,
                    ChatUtils.colorize("&eMasukkan max players (angka):"), input -> {
                        try {
                            int val = Integer.parseInt(input);
                            plugin.getDungeonManager().setDungeonConfig(id, "max-players", val);
                            player.sendMessage(ChatUtils.colorize("&aMax players: &f" + val));
                        } catch (NumberFormatException ex) {
                            player.sendMessage(ChatUtils.colorize("&cAngka tidak valid!"));
                        }
                        open(player, id);
                    });
            case 15 -> plugin.getEditorChatInput().requestInput(player,
                    ChatUtils.colorize("&eMasukkan cooldown (detik):"), input -> {
                        try {
                            int val = Integer.parseInt(input);
                            plugin.getDungeonManager().setDungeonConfig(id, "cooldown", val);
                            player.sendMessage(ChatUtils.colorize("&aCooldown: &f" + val + "s"));
                        } catch (NumberFormatException ex) {
                            player.sendMessage(ChatUtils.colorize("&cAngka tidak valid!"));
                        }
                        open(player, id);
                    });
            case 16 -> plugin.getEditorChatInput().requestInput(player,
                    ChatUtils.colorize("&eMasukkan material icon (misal: DIAMOND_SWORD):"), input -> {
                        plugin.getDungeonManager().setDungeonConfig(id, "icon", input.toUpperCase());
                        player.sendMessage(ChatUtils.colorize("&aIcon: &f" + input.toUpperCase()));
                        open(player, id);
                    });
            case 22 -> new DungeonMainEditorGUI(plugin).open(player, id);
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

    public static class InfoHolder implements InventoryHolder {
        public final String dungeonId;

        public InfoHolder(String id) {
            this.dungeonId = id;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
