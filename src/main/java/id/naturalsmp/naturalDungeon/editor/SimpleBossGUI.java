package id.naturalsmp.naturaldungeon.editor;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import id.naturalsmp.naturaldungeon.utils.GUIUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class SimpleBossGUI implements Listener {
    private final NaturalDungeon plugin;

    public SimpleBossGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String dungeonId, int stageIndex) {
        String title = ChatUtils.colorize("&8⮞ ꜰɪɴᴀʟ ʙᴏꜱꜱ ꜱᴛᴀɢᴇ " + (stageIndex + 1));
        Inventory inv = Bukkit.createInventory(new BossHolder(dungeonId, stageIndex), 27, title);

        ConfigurationSection cfg = plugin.getDungeonManager().loadDungeonConfig(dungeonId);
        String path = "stages." + (stageIndex + 1);
        String bossId = cfg.getString(path + ".boss.id", "None");
        String bLocStr = cfg.getString(path + ".boss.spawn-location", "Not Set");

        // Info & Select
        inv.setItem(11, GUIUtils.createItem(Material.WITHER_SKELETON_SKULL,
                "&#AA44FF&l⚔ ᴘɪʟɪʜ ʙᴏꜱꜱ",
                GUIUtils.separator(),
                "&7Boss saat ini:",
                "&f" + bossId,
                "",
                "&#FFAA00&l➥ KLIK UNTUK MEMILIH"));

        // Location
        inv.setItem(13, GUIUtils.createItem(Material.ENDER_PEARL,
                "&#55FF55&l📍 ʟᴏᴋᴀꜱɪ ꜱᴘᴀᴡɴ",
                GUIUtils.separator(),
                "&7Lokasi saat ini:",
                "&f" + bLocStr,
                "",
                "&#FFAA00&l➥ KLIK (Gunakan lokasi Anda)"));

        // Remove
        inv.setItem(15, GUIUtils.createItem(Material.BARRIER,
                "&#FF5555&l🗑 ʜᴀᴘᴜꜱ ʙᴏꜱꜱ",
                GUIUtils.separator(),
                "&7Hapus boss dari stage ini.",
                "",
                "&#FF5555&l➥ KLIK"));

        // Back
        inv.setItem(18, GUIUtils.createItem(Material.ARROW,
                "&#FF5555&l← ᴋᴇᴍʙᴀʟɪ",
                "&7Kembali ke editor wave."));

        player.openInventory(inv);
        GUIUtils.playOpenSound(player);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof BossHolder holder))
            return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getView().getTopInventory())
            return;
        if (e.getCurrentItem() == null)
            return;

        Player player = (Player) e.getWhoClicked();
        GUIUtils.playClickSound(player);

        String path = "stages." + (holder.stageIndex + 1) + ".boss";

        switch (e.getSlot()) {
            case 11 -> {
                player.closeInventory();
                new MobPickerGUI(plugin).open(player, selectedMobId -> {
                    plugin.getDungeonManager().setDungeonConfig(holder.dungeonId, path + ".id", selectedMobId);
                    player.sendMessage(ChatUtils.colorize("&#55FF55✔ Boss diubah menjadi: &f" + selectedMobId));
                    open(player, holder.dungeonId, holder.stageIndex);
                });
            }
            case 13 -> {
                org.bukkit.Location loc = player.getLocation();
                String locStr = loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + ","
                        + loc.getBlockZ() + "," + String.format("%.1f", loc.getYaw()) + ","
                        + String.format("%.1f", loc.getPitch());
                plugin.getDungeonManager().setDungeonConfig(holder.dungeonId, path + ".spawn-location", locStr);
                player.sendMessage(ChatUtils.colorize("&#55FF55✔ Lokasi spawn boss diatur!"));
                open(player, holder.dungeonId, holder.stageIndex);
            }
            case 15 -> {
                plugin.getDungeonManager().setDungeonConfig(holder.dungeonId, path, null);
                player.sendMessage(ChatUtils.colorize("&#FF5555✖ Boss dihapus dari stage ini."));
                open(player, holder.dungeonId, holder.stageIndex);
            }
            case 18 -> new WaveEditorGUI(plugin).open(player, holder.dungeonId, holder.stageIndex);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof BossHolder)
            e.setCancelled(true);
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
