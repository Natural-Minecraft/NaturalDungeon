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
 * Configure boss for a stage: select mob, set spawn location.
 */
public class BossConfigGUI implements Listener {

    private final NaturalDungeon plugin;

    public BossConfigGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String dungeonId, int stageIndex) {
        Inventory inv = Bukkit.createInventory(new BossHolder(dungeonId, stageIndex), 27,
                ChatUtils.colorize("&d&lBOSS CONFIG: Stage " + (stageIndex + 1)));
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++)
            inv.setItem(i, filler);

        inv.setItem(11, createItem(Material.WITHER_SKELETON_SKULL, "&c&lBoss Mob ID",
                "&7Set boss mob (MythicMobs ID atau VANILLA)",
                "&aKlik untuk set"));
        inv.setItem(13, createItem(Material.COMPASS, "&e&lSpawn Location",
                "&7Gunakan lokasi kamu saat ini",
                "&aKlik untuk set lokasi"));
        inv.setItem(15, createItem(Material.REDSTONE, "&6&lBoss HP Multiplier",
                "&aKlik untuk set (misal: 2.0)"));

        inv.setItem(22, createItem(Material.ARROW, "&cKembali"));
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof BossHolder holder))
            return;
        e.setCancelled(true);
        if (e.getCurrentItem() == null)
            return;
        Player player = (Player) e.getWhoClicked();
        String path = "stages." + (holder.stageIndex + 1) + ".boss.";

        switch (e.getSlot()) {
            case 11 -> plugin.getEditorChatInput().requestInput(player,
                    ChatUtils.colorize("&eMasukkan Boss mob ID:"),
                    input -> {
                        plugin.getDungeonManager().setDungeonConfig(holder.dungeonId, path + "mob", input);
                        player.sendMessage(ChatUtils.colorize("&aBoss mob: &f" + input));
                        open(player, holder.dungeonId, holder.stageIndex);
                    });
            case 13 -> {
                player.closeInventory();
                org.bukkit.Location loc = player.getLocation();
                List<Double> locList = Arrays.asList(loc.getX(), loc.getY(), loc.getZ());
                // Save to boss-spawn directly on the stage level
                plugin.getDungeonManager().setDungeonConfig(holder.dungeonId,
                        "stages." + (holder.stageIndex + 1) + ".boss-spawn", locList);
                String locStr = loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
                player.sendMessage(ChatUtils.colorize("&aBoss spawn location set: &f" + locStr));
            }
            case 15 -> plugin.getEditorChatInput().requestInput(player,
                    ChatUtils.colorize("&eMasukkan HP multiplier (misal: 2.0):"),
                    input -> {
                        try {
                            double v = Double.parseDouble(input);
                            plugin.getDungeonManager().setDungeonConfig(holder.dungeonId, path + "hp-multiplier", v);
                            player.sendMessage(ChatUtils.colorize("&aBoss HP multiplier: &f" + v + "x"));
                        } catch (NumberFormatException ex) {
                            player.sendMessage(ChatUtils.colorize("&cAngka tidak valid!"));
                        }
                        open(player, holder.dungeonId, holder.stageIndex);
                    });
            case 22 -> new WaveEditorGUI(plugin).open(player, holder.dungeonId, holder.stageIndex);
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

    public static class BossHolder implements InventoryHolder {
        public final String dungeonId;
        public final int stageIndex;

        public BossHolder(String d, int s) {
            this.dungeonId = d;
            this.stageIndex = s;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
