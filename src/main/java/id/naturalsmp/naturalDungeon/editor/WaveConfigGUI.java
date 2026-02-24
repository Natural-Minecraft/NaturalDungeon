package id.naturalsmp.naturaldungeon.editor;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import id.naturalsmp.naturaldungeon.utils.GUIUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * Configure a single wave: add mobs, set counts, set delay.
 * Updated to support adding multiple mob types in a single wave.
 */
public class WaveConfigGUI implements Listener {

    private final NaturalDungeon plugin;

    public WaveConfigGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String dungeonId, int stageIndex, int waveIndex) {
        Inventory inv = GUIUtils.createGUI(
                new WaveConfigHolder(dungeonId, stageIndex, waveIndex), 54,
                "&#FFAA00🌊 ᴡᴀᴠᴇ ᴄᴏɴꜰɪɢ: &fS" + (stageIndex + 1) + " W" + (waveIndex + 1));

        GUIUtils.fillBorder(inv, Material.BLACK_STAINED_GLASS_PANE);

        YamlConfiguration config = plugin.getDungeonManager().loadDungeonConfig(dungeonId);
        String wavePath = "stages." + (stageIndex + 1) + ".waves." + (waveIndex + 1);
        int currentDelay = config.getInt(wavePath + ".delay", 5);

        // --- Top Controls ---
        inv.setItem(4, GUIUtils.createItem(Material.REPEATER,
                "&#FFAA00&l⏱ ᴅᴇʟᴀʏ: &f" + currentDelay + "t",
                GUIUtils.separator(),
                "&7Delay sebelum wave mulai.",
                "&7(20 ticks = 1 detik)",
                "",
                "&#FFAA00&l➥ KLIK UNTUK MENGUBAH"));

        inv.setItem(8, GUIUtils.createItem(Material.ZOMBIE_HEAD,
                "&#55FF55&l✚ ᴛᴀᴍʙᴀʜ ᴍᴏʙ",
                GUIUtils.separator(),
                "&7Tambahkan mob baru ke wave ini.",
                "",
                "&#FFAA00&l➥ KLIK UNTUK MEMILIH"));

        // --- Mob List (Slots 10-43, excluding borders) ---
        int[] slots = { 10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43 };

        ConfigurationSection mobsSec = config.getConfigurationSection(wavePath + ".mobs");
        int slotIdx = 0;

        if (mobsSec != null) {
            for (String key : mobsSec.getKeys(false)) {
                if (slotIdx >= slots.length)
                    break;
                int amount = mobsSec.getInt(key);

                Material icon = Material.SPAWNER;
                if (key.startsWith("MM:"))
                    icon = Material.DRAGON_EGG;
                else if (key.startsWith("VANILLA:"))
                    icon = Material.SKELETON_SKULL;

                inv.setItem(slots[slotIdx], GUIUtils.createItem(icon,
                        "&#55CCFF&l" + key,
                        GUIUtils.separator(),
                        "&7Jumlah: &f" + amount,
                        "",
                        "&#55FF55&l➥ KIRI CLICK &7untuk Ubah Jumlah",
                        "&#FF5555&l➥ SHIFT-KIRI &7untuk Hapus"));
                slotIdx++;
            }
        }

        // --- Navigation ---
        inv.setItem(49, GUIUtils.createItem(Material.ARROW, "&#FF5555&l← ᴋᴇᴍʙᴀʟɪ"));

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
            case 4 -> {
                player.closeInventory();
                plugin.getEditorChatInput().requestInput(player,
                        ChatUtils.colorize("&#FFAA00&l⏱ &7Masukkan delay (ticks, 20 = 1 detik):"), input -> {
                            try {
                                int v = Integer.parseInt(input);
                                plugin.getDungeonManager().setDungeonConfig(holder.dungeonId, wavePath + "delay", v);
                                player.sendMessage(
                                        ChatUtils.colorize("&#55FF55✔ Delay diubah menjadi: &f" + v + " ticks"));
                                GUIUtils.playSuccessSound(player);
                            } catch (NumberFormatException ex) {
                                player.sendMessage(ChatUtils.colorize("&#FF5555✖ Angka tidak valid!"));
                                GUIUtils.playErrorSound(player);
                            }
                            open(player, holder.dungeonId, holder.stageIndex, holder.waveIndex);
                        });
            }
            case 8 -> {
                player.closeInventory();
                new MobPickerGUI(plugin).open(player, selectedMobId -> {
                    plugin.getEditorChatInput().requestInput(player,
                            ChatUtils.colorize("&#55CCFF&l📋 &7Masukkan jumlah untuk &f" + selectedMobId + "&7:"),
                            inputAmt -> {
                                try {
                                    int amt = Integer.parseInt(inputAmt);
                                    if (amt <= 0) {
                                        player.sendMessage(ChatUtils.colorize("&#FF5555✖ Jumlah harus lebih dari 0!"));
                                        open(player, holder.dungeonId, holder.stageIndex, holder.waveIndex);
                                        return;
                                    }
                                    plugin.getDungeonManager().setWaveMobCount(holder.dungeonId, holder.stageIndex,
                                            holder.waveIndex, selectedMobId, amt);
                                    player.sendMessage(ChatUtils.colorize("&#55FF55✔ Mob ditambahkan!"));
                                    GUIUtils.playSuccessSound(player);
                                } catch (NumberFormatException ex) {
                                    player.sendMessage(ChatUtils.colorize("&#FF5555✖ Angka tidak valid!"));
                                    GUIUtils.playErrorSound(player);
                                }
                                open(player, holder.dungeonId, holder.stageIndex, holder.waveIndex);
                            });
                });
            }
            case 49 -> new WaveEditorGUI(plugin).open(player, holder.dungeonId, holder.stageIndex);

            default -> {
                if (e.getSlot() >= 10 && e.getSlot() <= 43) {
                    String itemName = ChatUtils.stripColor(e.getCurrentItem().getItemMeta().getDisplayName());
                    if (itemName != null && !itemName.isEmpty()) {
                        if (e.isShiftClick()) {
                            plugin.getDungeonManager().removeWaveMob(holder.dungeonId, holder.stageIndex,
                                    holder.waveIndex, itemName);
                            player.sendMessage(ChatUtils.colorize("&#FF5555✖ Mob dihapus!"));
                            open(player, holder.dungeonId, holder.stageIndex, holder.waveIndex);
                        } else {
                            player.closeInventory();
                            plugin.getEditorChatInput().requestInput(player,
                                    ChatUtils.colorize("&#55CCFF&l📋 &7Ubah jumlah untuk &f" + itemName + "&7:"),
                                    inputAmt -> {
                                        try {
                                            int amt = Integer.parseInt(inputAmt);
                                            if (amt <= 0) {
                                                plugin.getDungeonManager().removeWaveMob(holder.dungeonId,
                                                        holder.stageIndex, holder.waveIndex, itemName);
                                                player.sendMessage(ChatUtils
                                                        .colorize("&#FF5555✖ Mob dihapus karena jumlah <= 0!"));
                                            } else {
                                                plugin.getDungeonManager().setWaveMobCount(holder.dungeonId,
                                                        holder.stageIndex, holder.waveIndex, itemName, amt);
                                                player.sendMessage(
                                                        ChatUtils.colorize("&#55FF55✔ Jumlah mob diperbarui!"));
                                            }
                                            GUIUtils.playSuccessSound(player);
                                        } catch (NumberFormatException ex) {
                                            player.sendMessage(ChatUtils.colorize("&#FF5555✖ Angka tidak valid!"));
                                            GUIUtils.playErrorSound(player);
                                        }
                                        open(player, holder.dungeonId, holder.stageIndex, holder.waveIndex);
                                    });
                        }
                    }
                }
            }
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
