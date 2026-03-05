package id.naturalsmp.naturaldungeon.editor;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import id.naturalsmp.naturaldungeon.utils.GUIUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

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
        Inventory inv = GUIUtils.createGUI(new WaveEditorHolder(dungeonId, stageIndex), 36,
                "&#FFAA00🌊 ᴡᴀᴠᴇꜱ: &fStage " + (stageIndex + 1));

        GUIUtils.fillAll(inv, Material.BLACK_STAINED_GLASS_PANE);

        int waveCount = plugin.getDungeonManager().getWaveCount(dungeonId, stageIndex);
        for (int i = 0; i < waveCount && i < 18; i++) {
            inv.setItem(i + 9, GUIUtils.createItem(Material.IRON_SWORD,
                    "&#FFAA00&lWave " + (i + 1),
                    GUIUtils.separator(),
                    "&7Edit mobs di wave ini.",
                    "",
                    "&#FFAA00&l⚔ Klik &7→ Edit",
                    "&#FF5555&l✖ Shift+Klik &7→ Hapus"));
        }

        // ─── Bottom Action Bar ───

        // Back
        inv.setItem(27, GUIUtils.createItem(Material.ARROW,
                "&#FF5555&l← ᴋᴇᴍʙᴀʟɪ"));

        // Add Mob Spawner
        inv.setItem(28, GUIUtils.createItem(Material.BLAZE_POWDER,
                "&#FFAA00&l📍 ᴀᴅᴅ ꜱᴘᴀᴡɴᴇʀ",
                GUIUtils.separator(),
                "&7Gunakan Wand Tool untuk",
                "&7menambah titik mob spawner",
                "&7secara fisik.",
                "",
                "&#FFAA00&l➥ KLIK (Ambil Wand Tool)"));

        // Clear Spawners
        inv.setItem(29, GUIUtils.createItem(Material.BARRIER,
                "&#FF5555&l🗑 ᴄʟᴇᴀʀ ꜱᴘᴀᴡɴᴇʀꜱ",
                GUIUtils.separator(),
                "&7Hapus semua lokasi spawner.",
                "",
                "&#FF5555&l➥ KLIK"));

        // Add Wave
        inv.setItem(31, GUIUtils.createItem(Material.EMERALD,
                "&#55FF55&l✚ ᴛᴀᴍʙᴀʜ ᴡᴀᴠᴇ",
                GUIUtils.separator(),
                "&7Tambah wave baru.",
                "",
                "&#FFAA00&l➥ KLIK"));

        player.openInventory(inv);
        GUIUtils.playOpenSound(player);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof WaveEditorHolder holder))
            return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getView().getTopInventory())
            return;
        if (e.getCurrentItem() == null)
            return;

        Player player = (Player) e.getWhoClicked();
        GUIUtils.playClickSound(player);

        switch (e.getSlot()) {
            case 27 -> new StageEditorGUI(plugin).open(player, holder.dungeonId);
            case 32 -> new SimpleBossGUI(plugin).open(player, holder.dungeonId, holder.stageIndex);
            case 31 -> {
                plugin.getDungeonManager().addWave(holder.dungeonId, holder.stageIndex);
                player.sendMessage(ChatUtils.colorize("&#55FF55✔ Wave baru ditambahkan!"));
                GUIUtils.playSuccessSound(player);
                open(player, holder.dungeonId, holder.stageIndex);
            }
            case 28 -> {
                player.closeInventory();
                plugin.getSetupManager().enterStageEditor(player,
                        plugin.getDungeonManager().getDungeon(holder.dungeonId), holder.stageIndex + 1);
            }
            case 29 -> {
                String path = "stages." + (holder.stageIndex + 1) + ".mob-spawns";
                plugin.getDungeonManager().setDungeonConfig(holder.dungeonId, path, new ArrayList<String>());
                player.sendMessage(ChatUtils.colorize("&#FF5555✖ Semua spawner dihapus!"));
                open(player, holder.dungeonId, holder.stageIndex);
            }
            default -> {
                if (e.getSlot() >= 9 && e.getSlot() <= 26) {
                    int waveIndex = e.getSlot() - 9;
                    if (e.isShiftClick()) {
                        plugin.getDungeonManager().deleteWave(holder.dungeonId, holder.stageIndex, waveIndex);
                        player.sendMessage(ChatUtils.colorize("&#FF5555✖ Wave dihapus!"));
                        open(player, holder.dungeonId, holder.stageIndex);
                    } else {
                        new WaveConfigGUI(plugin).open(player, holder.dungeonId, holder.stageIndex, waveIndex);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof WaveEditorHolder)
            e.setCancelled(true);
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
