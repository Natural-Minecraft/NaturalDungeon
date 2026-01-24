package id.naturalsmp.naturaldungeon.admin;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.dungeon.Dungeon;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import id.naturalsmp.naturaldungeon.utils.ConfigUtils;
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

import java.util.ArrayList;
import java.util.List;

public class AdminGUI implements Listener {

    private final NaturalDungeon plugin;

    public AdminGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(new AdminHolder(), 27, ChatUtils.colorize("&cNaturalDungeon Admin"));

        // Dashboard Items
        inv.setItem(11, createItem(Material.CHEST, "&eDungeon List", "&7Manage and configure dungeons"));
        inv.setItem(13, createItem(Material.PAPER, "&bActive Instances", "&7Monitor running dungeons",
                "&7Currently Active: &f" + getActiveCount()));
        inv.setItem(15, createItem(Material.BOOK, "&dItem Browser", "&7View MMOItems and Recipes"));

        // Fillers
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            if (inv.getItem(i) == null)
                inv.setItem(i, filler);
        }

        player.openInventory(inv);
    }

    private int getActiveCount() {
        return plugin.getDungeonManager().getActiveInstanceCount();
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof AdminHolder))
            return;
        e.setCancelled(true);
        if (e.getCurrentItem() == null)
            return;

        Player player = (Player) e.getWhoClicked();
        if (e.getCurrentItem().getType() == Material.CHEST) {
            // Open Dungeon List (To be implemented or just show list in chat)
            player.sendMessage(ChatUtils.colorize("&eCheck console or /nd list for dungeons. GUI coming soon."));
        } else if (e.getCurrentItem().getType() == Material.PAPER) {
            // Open Instances List
            player.sendMessage(ChatUtils.colorize("&eFeature coming soon."));
        } else if (e.getCurrentItem().getType() == Material.BOOK) {
            // Open Item Browser
            if (plugin.hasMMOItems()) {
                // We could implement a pager GUI for all MMOItems, but for now let's just say
                // "Use /mi browse"
                player.sendMessage(ChatUtils.colorize("&eOpening MMOItems Browser..."));
                player.performCommand("mi browse"); // Shortcut to MI's own browser because it's better
            } else {
                player.sendMessage(ChatUtils.colorize("&cMMOItems not installed."));
            }
        }
    }

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.colorize(name));
        List<String> loreList = new ArrayList<>();
        for (String l : lore)
            loreList.add(ChatUtils.colorize(l));
        meta.setLore(loreList);
        item.setItemMeta(meta);
        return item;
    }

    public static class AdminHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
