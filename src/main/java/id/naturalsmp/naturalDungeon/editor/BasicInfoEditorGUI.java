package id.naturalsmp.naturaldungeon.editor;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.dungeon.Dungeon;
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

        Inventory inv = GUIUtils.createGUI(new InfoHolder(dungeonId), 27,
                "&#55CCFF📝 ʙᴀꜱɪᴄ ɪɴꜰᴏ: &f" + dungeonId);

        GUIUtils.fillAll(inv, Material.BLACK_STAINED_GLASS_PANE);

        String currentName = dungeon != null ? dungeon.getDisplayName() : dungeonId;
        String currentWorld = dungeon != null ? dungeon.getWorld() : "N/A";
        int maxPlayers = dungeon != null ? dungeon.getMaxPlayers() : 4;

        // ─── Editor Fields ───

        inv.setItem(10, GUIUtils.createItem(Material.NAME_TAG,
                "&#FFD700&l✏ ɴᴀᴍᴀ",
                GUIUtils.separator(),
                "&7Saat ini: &f" + currentName,
                "",
                "&#FFAA00&l➥ KLIK UNTUK UBAH"));

        inv.setItem(11, GUIUtils.createItem(Material.BOOK,
                "&#55CCFF&l📖 ᴅᴇꜱᴋʀɪᴘꜱɪ",
                GUIUtils.separator(),
                "&7Edit deskripsi dungeon.",
                "",
                "&#FFAA00&l➥ KLIK UNTUK UBAH"));

        inv.setItem(12, GUIUtils.createItem(Material.GRASS_BLOCK,
                "&#55FF55&l🌍 ᴡᴏʀʟᴅ",
                GUIUtils.separator(),
                "&7Saat ini: &f" + currentWorld,
                "",
                "&#FFAA00&l➥ KLIK UNTUK SET"));

        inv.setItem(14, GUIUtils.createItem(Material.IRON_CHESTPLATE,
                "&#AAAAAA&l👥 ᴍᴀx ᴘʟᴀʏᴇʀꜱ",
                GUIUtils.separator(),
                "&7Saat ini: &f" + maxPlayers,
                "",
                "&#FFAA00&l➥ KLIK UNTUK UBAH"));

        inv.setItem(15, GUIUtils.createItem(Material.CLOCK,
                "&#FFAA00&l⏱ ᴄᴏᴏʟᴅᴏᴡɴ",
                GUIUtils.separator(),
                "&7Waktu tunggu antar run.",
                "",
                "&#FFAA00&l➥ KLIK UNTUK UBAH"));

        inv.setItem(16, GUIUtils.createItem(Material.ITEM_FRAME,
                "&#AA44FF&l🎨 ɪᴄᴏɴ",
                GUIUtils.separator(),
                "&7Material icon di GUI.",
                "",
                "&#FFAA00&l➥ KLIK UNTUK UBAH"));

        // Back button
        inv.setItem(22, GUIUtils.createItem(Material.ARROW,
                "&#FF5555&l← ᴋᴇᴍʙᴀʟɪ",
                "&7Kembali ke editor utama."));

        player.openInventory(inv);
        GUIUtils.playOpenSound(player);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof InfoHolder holder))
            return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getView().getTopInventory())
            return;
        if (e.getCurrentItem() == null)
            return;

        Player player = (Player) e.getWhoClicked();
        String id = holder.dungeonId;
        GUIUtils.playClickSound(player);

        switch (e.getSlot()) {
            case 10 -> {
                player.closeInventory();
                plugin.getEditorChatInput().requestInput(player,
                        ChatUtils.colorize("&#FFD700&l✏ &7Masukkan nama baru:"), input -> {
                            plugin.getDungeonManager().setDungeonConfig(id, "display-name", input);
                            player.sendMessage(ChatUtils.colorize("&#55FF55✔ Nama diubah ke: &f" + input));
                            GUIUtils.playSuccessSound(player);
                            open(player, id);
                        });
            }
            case 11 -> {
                player.closeInventory();
                plugin.getEditorChatInput().requestInput(player,
                        ChatUtils.colorize("&#55CCFF&l📖 &7Masukkan deskripsi:"), input -> {
                            plugin.getDungeonManager().setDungeonConfig(id, "description", input);
                            player.sendMessage(ChatUtils.colorize("&#55FF55✔ Deskripsi diubah!"));
                            GUIUtils.playSuccessSound(player);
                            open(player, id);
                        });
            }
            case 12 -> {
                player.closeInventory();
                plugin.getEditorChatInput().requestInput(player,
                        ChatUtils.colorize("&#55FF55&l🌍 &7Masukkan nama world:"), input -> {
                            plugin.getDungeonManager().setDungeonConfig(id, "world", input);
                            player.sendMessage(ChatUtils.colorize("&#55FF55✔ World diset: &f" + input));
                            GUIUtils.playSuccessSound(player);
                            open(player, id);
                        });
            }
            case 14 -> {
                player.closeInventory();
                plugin.getEditorChatInput().requestInput(player,
                        ChatUtils.colorize("&#AAAAAA&l👥 &7Masukkan max players (angka):"), input -> {
                            try {
                                int val = Integer.parseInt(input);
                                plugin.getDungeonManager().setDungeonConfig(id, "max-players", val);
                                player.sendMessage(ChatUtils.colorize("&#55FF55✔ Max players: &f" + val));
                                GUIUtils.playSuccessSound(player);
                            } catch (NumberFormatException ex) {
                                player.sendMessage(ChatUtils.colorize("&#FF5555✖ Angka tidak valid!"));
                                GUIUtils.playErrorSound(player);
                            }
                            open(player, id);
                        });
            }
            case 15 -> {
                player.closeInventory();
                plugin.getEditorChatInput().requestInput(player,
                        ChatUtils.colorize("&#FFAA00&l⏱ &7Masukkan cooldown (detik):"), input -> {
                            try {
                                int val = Integer.parseInt(input);
                                plugin.getDungeonManager().setDungeonConfig(id, "cooldown", val);
                                player.sendMessage(ChatUtils.colorize("&#55FF55✔ Cooldown: &f" + val + "s"));
                                GUIUtils.playSuccessSound(player);
                            } catch (NumberFormatException ex) {
                                player.sendMessage(ChatUtils.colorize("&#FF5555✖ Angka tidak valid!"));
                                GUIUtils.playErrorSound(player);
                            }
                            open(player, id);
                        });
            }
            case 16 -> {
                player.closeInventory();
                plugin.getEditorChatInput().requestInput(player,
                        ChatUtils.colorize("&#AA44FF&l🎨 &7Masukkan material (misal: DIAMOND_SWORD):"), input -> {
                            Material mat = Material.matchMaterial(input.toUpperCase());
                            if (mat != null) {
                                plugin.getDungeonManager().setDungeonConfig(id, "icon", input.toUpperCase());
                                player.sendMessage(ChatUtils.colorize("&#55FF55✔ Icon: &f" + input.toUpperCase()));
                                GUIUtils.playSuccessSound(player);
                            } else {
                                player.sendMessage(ChatUtils.colorize("&#FF5555✖ Material tidak ditemukan!"));
                                GUIUtils.playErrorSound(player);
                            }
                            open(player, id);
                        });
            }
            case 22 -> new DungeonMainEditorGUI(plugin).open(player, id);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof InfoHolder)
            e.setCancelled(true);
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
