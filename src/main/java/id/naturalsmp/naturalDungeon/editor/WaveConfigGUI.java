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

/**
 * Configure a single wave: add mobs, set counts, set delay.
 */
public class WaveConfigGUI implements Listener {

    private final NaturalDungeon plugin;

    public WaveConfigGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String dungeonId, int stageIndex, int waveIndex) {
        Inventory inv = GUIUtils.createGUI(
                new WaveConfigHolder(dungeonId, stageIndex, waveIndex), 27,
                "&#FFAA00🌊 ᴡᴀᴠᴇ ᴄᴏɴꜰɪɢ: &fS" + (stageIndex + 1) + " W" + (waveIndex + 1));

        GUIUtils.fillAll(inv, Material.BLACK_STAINED_GLASS_PANE);

        inv.setItem(10, GUIUtils.createItem(Material.ZOMBIE_HEAD,
                "&#55FF55&l👹 ᴛᴀᴍʙᴀʜ ᴍᴏʙ",
                GUIUtils.separator(),
                "&7Tambahkan mob ke wave ini.",
                "&7Contoh: &fZOMBIE&7, &fmythicmobs:King",
                "",
                "&#FFAA00&l➥ KLIK"));

        inv.setItem(11, GUIUtils.createItem(Material.REPEATER,
                "&#FFAA00&l⏱ ᴅᴇʟᴀʏ",
                GUIUtils.separator(),
                "&7Delay sebelum wave mulai.",
                "&7(20 ticks = 1 detik)",
                "",
                "&#FFAA00&l➥ KLIK"));

        inv.setItem(12, GUIUtils.createItem(Material.PAPER,
                "&#55CCFF&l📋 ᴍᴏʙ ᴄᴏᴜɴᴛ",
                GUIUtils.separator(),
                "&7Jumlah mob per wave.",
                "",
                "&#FFAA00&l➥ KLIK"));

        inv.setItem(14, GUIUtils.createItem(Material.SPIDER_EYE,
                "&#AA44FF&l🐾 ᴍᴏʙ ᴛʏᴘᴇ",
                GUIUtils.separator(),
                "&7VANILLA / MYTHICMOBS / CUSTOM",
                "",
                "&#FFAA00&l➥ KLIK"));

        inv.setItem(22, GUIUtils.createItem(Material.ARROW, "&#FF5555&l← ᴋᴇᴍʙᴀʟɪ"));

        player.openInventory(inv);
        GUIUtils.playOpenSound(player);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof WaveConfigHolder holder))
            return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getView().getTopInventory())
            return;
        if (e.getCurrentItem() == null)
            return;

        Player player = (Player) e.getWhoClicked();
        GUIUtils.playClickSound(player);
        String wavePath = "stages." + (holder.stageIndex + 1) + ".waves." + (holder.waveIndex + 1) + ".";

        switch (e.getSlot()) {
            case 10 -> {
                player.closeInventory();
                plugin.getEditorChatInput().requestInput(player,
                        ChatUtils.colorize("&#55FF55&l👹 &7Masukkan mob ID (ZOMBIE, mythicmobs:SkeletonKing):"),
                        input -> {
                            plugin.getDungeonManager().addWaveMob(holder.dungeonId, holder.stageIndex, holder.waveIndex,
                                    input);
                            player.sendMessage(
                                    ChatUtils.colorize("&#55FF55✔ Mob &f" + input + " &#55FF55ditambahkan!"));
                            GUIUtils.playSuccessSound(player);
                            open(player, holder.dungeonId, holder.stageIndex, holder.waveIndex);
                        });
            }
            case 11 -> {
                player.closeInventory();
                plugin.getEditorChatInput().requestInput(player,
                        ChatUtils.colorize("&#FFAA00&l⏱ &7Masukkan delay (ticks, 20 = 1 detik):"), input -> {
                            try {
                                int v = Integer.parseInt(input);
                                plugin.getDungeonManager().setDungeonConfig(holder.dungeonId, wavePath + "delay", v);
                                player.sendMessage(ChatUtils.colorize("&#55FF55✔ Delay: &f" + v + " ticks"));
                                GUIUtils.playSuccessSound(player);
                            } catch (NumberFormatException ex) {
                                player.sendMessage(ChatUtils.colorize("&#FF5555✖ Angka tidak valid!"));
                                GUIUtils.playErrorSound(player);
                            }
                            open(player, holder.dungeonId, holder.stageIndex, holder.waveIndex);
                        });
            }
            case 12 -> {
                player.closeInventory();
                plugin.getEditorChatInput().requestInput(player,
                        ChatUtils.colorize("&#55CCFF&l📋 &7Masukkan jumlah mob:"), input -> {
                            try {
                                int v = Integer.parseInt(input);
                                plugin.getDungeonManager().setDungeonConfig(holder.dungeonId, wavePath + "count", v);
                                player.sendMessage(ChatUtils.colorize("&#55FF55✔ Count: &f" + v));
                                GUIUtils.playSuccessSound(player);
                            } catch (NumberFormatException ex) {
                                player.sendMessage(ChatUtils.colorize("&#FF5555✖ Angka tidak valid!"));
                                GUIUtils.playErrorSound(player);
                            }
                            open(player, holder.dungeonId, holder.stageIndex, holder.waveIndex);
                        });
            }
            case 14 -> {
                player.closeInventory();
                plugin.getEditorChatInput().requestInput(player,
                        ChatUtils.colorize("&#AA44FF&l🐾 &7Tipe mob (VANILLA/MYTHICMOBS/CUSTOM):"), input -> {
                            plugin.getDungeonManager().setDungeonConfig(holder.dungeonId, wavePath + "type",
                                    input.toUpperCase());
                            player.sendMessage(ChatUtils.colorize("&#55FF55✔ Tipe: &f" + input.toUpperCase()));
                            GUIUtils.playSuccessSound(player);
                            open(player, holder.dungeonId, holder.stageIndex, holder.waveIndex);
                        });
            }
            case 22 -> new WaveEditorGUI(plugin).open(player, holder.dungeonId, holder.stageIndex);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof WaveConfigHolder)
            e.setCancelled(true);
    }

    public static class WaveConfigHolder implements InventoryHolder {
        public final String dungeonId;
        public final int stageIndex, waveIndex;

        public WaveConfigHolder(String d, int s, int w) {
            this.dungeonId = d;
            this.stageIndex = s;
            this.waveIndex = w;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
