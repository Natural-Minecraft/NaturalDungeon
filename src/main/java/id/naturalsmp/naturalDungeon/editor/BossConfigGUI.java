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
 * Configure boss for a stage: select mob, set spawn location.
 */
public class BossConfigGUI implements Listener {

    private final NaturalDungeon plugin;

    public BossConfigGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String dungeonId, int stageIndex) {
        Inventory inv = GUIUtils.createGUI(new BossHolder(dungeonId, stageIndex), 27,
                "&#AA44FF🐉 ʙᴏꜱꜱ ᴄᴏɴꜰɪɢ: &fStage " + (stageIndex + 1));

        GUIUtils.fillAll(inv, Material.BLACK_STAINED_GLASS_PANE);

        inv.setItem(11, GUIUtils.createItem(Material.WITHER_SKELETON_SKULL,
                "&#FF5555&l🐉 ʙᴏꜱꜱ ᴍᴏʙ ɪᴅ",
                GUIUtils.separator(),
                "&7Set boss mob ID.",
                "&7Support: MythicMobs/Vanilla/Custom",
                "",
                "&#FFAA00&l➥ KLIK"));

        inv.setItem(13, GUIUtils.createItem(Material.COMPASS,
                "&#55CCFF&l📍 ꜱᴘᴀᴡɴ ʟᴏᴄᴀᴛɪᴏɴ",
                GUIUtils.separator(),
                "&7Gunakan lokasi kamu saat ini.",
                "",
                "&#FFAA00&l➥ KLIK"));

        inv.setItem(15, GUIUtils.createItem(Material.REDSTONE,
                "&#FFAA00&l❤ ʜᴘ ᴍᴜʟᴛɪᴘʟɪᴇʀ",
                GUIUtils.separator(),
                "&7Contoh: &f2.0",
                "",
                "&#FFAA00&l➥ KLIK"));

        inv.setItem(22, GUIUtils.createItem(Material.ARROW, "&#FF5555&l← ᴋᴇᴍʙᴀʟɪ"));

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
        String path = "stages." + (holder.stageIndex + 1) + ".boss.";

        switch (e.getSlot()) {
            case 11 -> {
                player.closeInventory();
                plugin.getEditorChatInput().requestInput(player,
                        ChatUtils.colorize("&#AA44FF&l🐉 &7Masukkan Boss mob ID:"), input -> {
                            plugin.getDungeonManager().setDungeonConfig(holder.dungeonId, path + "mob", input);
                            player.sendMessage(ChatUtils.colorize("&#55FF55✔ Boss mob: &f" + input));
                            GUIUtils.playSuccessSound(player);
                            open(player, holder.dungeonId, holder.stageIndex);
                        });
            }
            case 13 -> {
                player.closeInventory();
                org.bukkit.Location loc = player.getLocation();
                List<Double> locList = Arrays.asList(loc.getX(), loc.getY(), loc.getZ());
                plugin.getDungeonManager().setDungeonConfig(holder.dungeonId,
                        "stages." + (holder.stageIndex + 1) + ".boss-spawn", locList);
                String locStr = loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
                player.sendMessage(ChatUtils.colorize("&#55FF55✔ Boss spawn: &f" + locStr));
                GUIUtils.playSuccessSound(player);
            }
            case 15 -> {
                player.closeInventory();
                plugin.getEditorChatInput().requestInput(player,
                        ChatUtils.colorize("&#FFAA00&l❤ &7Masukkan HP multiplier (misal: 2.0):"), input -> {
                            try {
                                double v = Double.parseDouble(input);
                                plugin.getDungeonManager().setDungeonConfig(holder.dungeonId, path + "hp-multiplier",
                                        v);
                                player.sendMessage(ChatUtils.colorize("&#55FF55✔ HP multiplier: &f" + v + "x"));
                                GUIUtils.playSuccessSound(player);
                            } catch (NumberFormatException ex) {
                                player.sendMessage(ChatUtils.colorize("&#FF5555✖ Angka tidak valid!"));
                                GUIUtils.playErrorSound(player);
                            }
                            open(player, holder.dungeonId, holder.stageIndex);
                        });
            }
            case 22 -> new WaveEditorGUI(plugin).open(player, holder.dungeonId, holder.stageIndex);
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
