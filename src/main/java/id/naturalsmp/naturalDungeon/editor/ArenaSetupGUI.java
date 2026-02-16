package id.naturalsmp.naturaldungeon.editor;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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
 * Arena setup: set region points, spawn points, safe zone.
 */
public class ArenaSetupGUI implements Listener {

    private final NaturalDungeon plugin;

    public ArenaSetupGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String dungeonId) {
        Inventory inv = Bukkit.createInventory(new ArenaHolder(dungeonId), 27,
                ChatUtils.colorize("&6&lARENA SETUP: &e" + dungeonId));
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++)
            inv.setItem(i, filler);

        inv.setItem(10, createItem(Material.GOLDEN_AXE, "&e&lSet Spawn Point",
                "&7Gunakan posisi kamu saat ini", "&aKlik untuk set"));
        inv.setItem(11, createItem(Material.COMPASS, "&e&lSet Lobby Point",
                "&7Posisi lobby/exit point", "&aKlik untuk set"));
        inv.setItem(12, createItem(Material.EMERALD_BLOCK, "&e&lSet Safe Zone",
                "&7Posisi safe zone (antara stages)", "&aKlik untuk set"));
        inv.setItem(14, createItem(Material.DIAMOND_AXE, "&e&lSet Region Corner 1",
                "&7Posisi pojok 1 arena", "&aKlik untuk set"));
        inv.setItem(15, createItem(Material.DIAMOND_AXE, "&e&lSet Region Corner 2",
                "&7Posisi pojok 2 arena", "&aKlik untuk set"));

        inv.setItem(22, createItem(Material.ARROW, "&cKembali"));
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof ArenaHolder holder))
            return;
        e.setCancelled(true);
        if (e.getCurrentItem() == null)
            return;
        Player player = (Player) e.getWhoClicked();
        Location loc = player.getLocation();
        String locStr = loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ()
                + "," + String.format("%.1f", loc.getYaw()) + "," + String.format("%.1f", loc.getPitch());

        switch (e.getSlot()) {
            case 10 -> {
                plugin.getDungeonManager().setDungeonConfig(holder.dungeonId, "spawn-point", locStr);
                player.sendMessage(ChatUtils.colorize("&aSpawn point set: &f" + locStr));
                player.closeInventory();
            }
            case 11 -> {
                plugin.getDungeonManager().setDungeonConfig(holder.dungeonId, "lobby-point", locStr);
                player.sendMessage(ChatUtils.colorize("&aLobby point set: &f" + locStr));
                player.closeInventory();
            }
            case 12 -> {
                plugin.getDungeonManager().setDungeonConfig(holder.dungeonId, "safe-zone", locStr);
                player.sendMessage(ChatUtils.colorize("&aSafe zone set: &f" + locStr));
                player.closeInventory();
            }
            case 14 -> {
                String corner = loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
                plugin.getDungeonManager().setDungeonConfig(holder.dungeonId, "region.corner1", corner);
                player.sendMessage(ChatUtils.colorize("&aCorner 1 set: &f" + corner));
                player.closeInventory();
            }
            case 15 -> {
                String corner = loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
                plugin.getDungeonManager().setDungeonConfig(holder.dungeonId, "region.corner2", corner);
                player.sendMessage(ChatUtils.colorize("&aCorner 2 set: &f" + corner));
                player.closeInventory();
            }
            case 22 -> new DungeonMainEditorGUI(plugin).open(player, holder.dungeonId);
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

    public static class ArenaHolder implements InventoryHolder {
        public final String dungeonId;

        public ArenaHolder(String id) {
            this.dungeonId = id;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
