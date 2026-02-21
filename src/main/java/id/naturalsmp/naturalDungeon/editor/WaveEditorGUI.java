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
 * Manage waves within a stage.
 */
public class WaveEditorGUI implements Listener {

    private final NaturalDungeon plugin;

    public WaveEditorGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String dungeonId, int stageIndex) {
        Inventory inv = Bukkit.createInventory(new WaveEditorHolder(dungeonId, stageIndex), 36,
                ChatUtils.colorize("&e&lWAVES: Stage " + (stageIndex + 1)));
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 36; i++)
            inv.setItem(i, filler);

        int waveCount = plugin.getDungeonManager().getWaveCount(dungeonId, stageIndex);
        for (int i = 0; i < waveCount && i < 18; i++) {
            inv.setItem(i + 9, createItem(Material.IRON_SWORD,
                    "&6&lWave " + (i + 1),
                    "&aKlik untuk edit mobs",
                    "&cShift+Klik untuk hapus"));
        }

        inv.setItem(31, createItem(Material.EMERALD, "&a&lTambah Wave"));
        inv.setItem(30, createItem(Material.WITHER_SKELETON_SKULL, "&d&lBoss Config",
                "&7Set boss untuk stage ini"));
        inv.setItem(27, createItem(Material.ARROW, "&cKembali"));

        // [NEW] Spawner & Arena controls for this stage
        inv.setItem(28, createItem(Material.BLAZE_POWDER, "&e&lAdd Mob Spawner",
                "&7Tambah lokasi mob spawn", "&aKlik untuk tambah di lokasi kamu"));
        inv.setItem(29, createItem(Material.BARRIER, "&c&lClear Mob Spawners",
                "&7Hapus semua lokasi spawner", "&aKlik untuk clear"));

        inv.setItem(33, createItem(Material.EMERALD_BLOCK, "&e&lSet Safe Zone",
                "&7Set titik start/aman stage", "&aKlik untuk set di lokasi kamu"));
        inv.setItem(34, createItem(Material.DIAMOND_AXE, "&e&lSet Arena Corner 1",
                "&7Set pojok arena 1", "&aKlik untuk set"));
        inv.setItem(35, createItem(Material.DIAMOND_AXE, "&e&lSet Arena Corner 2",
                "&7Set pojok arena 2", "&aKlik untuk set"));

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof WaveEditorHolder holder))
            return;
        e.setCancelled(true);
        if (e.getCurrentItem() == null)
            return;
        Player player = (Player) e.getWhoClicked();

        if (e.getSlot() == 27) {
            new StageEditorGUI(plugin).open(player, holder.dungeonId);
            return;
        }
        if (e.getSlot() == 30) {
            new BossConfigGUI(plugin).open(player, holder.dungeonId, holder.stageIndex);
            return;
        }
        if (e.getSlot() == 31) {
            plugin.getDungeonManager().addWave(holder.dungeonId, holder.stageIndex);
            player.sendMessage(ChatUtils.colorize("&aWave baru ditambahkan!"));
            open(player, holder.dungeonId, holder.stageIndex);
            return;
        }

        // Feature: Add Mob Spawner
        if (e.getSlot() == 28) {
            player.closeInventory();
            org.bukkit.Location loc = player.getLocation();
            String locStr = loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + ","
                    + loc.getBlockZ()
                    + "," + String.format("%.1f", loc.getYaw()) + "," + String.format("%.1f", loc.getPitch());

            String path = "stages." + (holder.stageIndex + 1) + ".mob-spawns";
            List<String> spawns = plugin.getDungeonManager().loadDungeonConfig(holder.dungeonId).getStringList(path);
            spawns.add(locStr);
            plugin.getDungeonManager().setDungeonConfig(holder.dungeonId, path, spawns);
            player.sendMessage(ChatUtils.colorize("&aDitambahkan Mob Spawner #" + spawns.size() + ": &f" + locStr));
            return;
        }

        // Feature: Clear Mob Spawners
        if (e.getSlot() == 29) {
            String path = "stages." + (holder.stageIndex + 1) + ".mob-spawns";
            plugin.getDungeonManager().setDungeonConfig(holder.dungeonId, path, new ArrayList<String>());
            player.sendMessage(ChatUtils.colorize("&cSemua Mob Spawner di Stage ini telah dihapus!"));
            open(player, holder.dungeonId, holder.stageIndex);
            return;
        }

        // Feature: Set Safe Zone
        if (e.getSlot() == 33) {
            player.closeInventory();
            org.bukkit.Location loc = player.getLocation();
            String locStr = loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + ","
                    + loc.getBlockZ()
                    + "," + String.format("%.1f", loc.getYaw()) + "," + String.format("%.1f", loc.getPitch());
            plugin.getDungeonManager().setDungeonConfig(holder.dungeonId,
                    "stages." + (holder.stageIndex + 1) + ".safe-zone",
                    locStr);
            player.sendMessage(ChatUtils.colorize("&aSafe zone stage set: &f" + locStr));
            return;
        }

        // Feature: Set Arena Corner 1 & 2
        if (e.getSlot() == 34 || e.getSlot() == 35) {
            player.closeInventory();
            org.bukkit.Location loc = player.getLocation();
            String cornerStr = loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
            String cfgKey = e.getSlot() == 34 ? "corner1" : "corner2";
            plugin.getDungeonManager().setDungeonConfig(holder.dungeonId,
                    "stages." + (holder.stageIndex + 1) + ".arena-region." + cfgKey, cornerStr);
            player.sendMessage(ChatUtils.colorize("&aArena " + cfgKey + " set: &f" + cornerStr));
            return;
        }
        if (e.getSlot() >= 9 && e.getSlot() <= 26) {
            int waveIndex = e.getSlot() - 9;
            if (e.isShiftClick()) {
                plugin.getDungeonManager().deleteWave(holder.dungeonId, holder.stageIndex, waveIndex);
                player.sendMessage(ChatUtils.colorize("&cWave dihapus!"));
                open(player, holder.dungeonId, holder.stageIndex);
            } else {
                new WaveConfigGUI(plugin).open(player, holder.dungeonId, holder.stageIndex, waveIndex);
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

    public static class WaveEditorHolder implements InventoryHolder {
        public final String dungeonId;
        public final int stageIndex;

        public WaveEditorHolder(String d, int s) {
            this.dungeonId = d;
            this.stageIndex = s;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
