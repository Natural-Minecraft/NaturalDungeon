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
 * Configure a single difficulty: name, min-tier, key-req, max-deaths,
 * reward-multiplier.
 */
public class DifficultyConfigGUI implements Listener {

    private final NaturalDungeon plugin;

    public DifficultyConfigGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String dungeonId, String difficultyId) {
        Inventory inv = GUIUtils.createGUI(new DiffConfigHolder(dungeonId, difficultyId), 27,
                "&#FF5555⚔ ᴅɪꜰꜰ ᴄᴏɴꜰɪɢ: &f" + difficultyId);

        GUIUtils.fillAll(inv, Material.BLACK_STAINED_GLASS_PANE);

        inv.setItem(10, GUIUtils.createItem(Material.NAME_TAG,
                "&#FFD700&l✏ ᴅɪꜱᴘʟᴀʏ ɴᴀᴍᴇ",
                GUIUtils.separator(), "&7Ubah nama tampilan.", "", "&#FFAA00&l➥ KLIK"));
        inv.setItem(11, GUIUtils.createItem(Material.SHIELD,
                "&#55CCFF&l🛡 ᴍɪɴ ᴛɪᴇʀ",
                GUIUtils.separator(), "&7Tier minimum untuk akses.", "", "&#FFAA00&l➥ KLIK"));
        inv.setItem(12, GUIUtils.createItem(Material.TRIPWIRE_HOOK,
                "&#FFAA00&l🔑 ᴋᴇʏ ʀᴇǫᴜɪʀᴇᴍᴇɴᴛ",
                GUIUtils.separator(), "&7Key item yang dibutuhkan.", "&7Ketik &f'none' &7jika tidak ada.", "",
                "&#FFAA00&l➥ KLIK"));
        inv.setItem(14, GUIUtils.createItem(Material.SKELETON_SKULL,
                "&#FF5555&l💀 ᴍᴀx ᴅᴇᴀᴛʜꜱ",
                GUIUtils.separator(), "&7Batas kematian sebelum gagal.", "", "&#FFAA00&l➥ KLIK"));
        inv.setItem(15, GUIUtils.createItem(Material.GOLD_INGOT,
                "&#55FF55&l💰 ʀᴇᴡᴀʀᴅ ᴍᴜʟᴛɪᴘʟɪᴇʀ",
                GUIUtils.separator(), "&7Contoh: &f1.5", "", "&#FFAA00&l➥ KLIK"));

        inv.setItem(22, GUIUtils.createItem(Material.ARROW, "&#FF5555&l← ᴋᴇᴍʙᴀʟɪ"));

        player.openInventory(inv);
        GUIUtils.playOpenSound(player);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof DiffConfigHolder holder))
            return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getView().getTopInventory())
            return;
        if (e.getCurrentItem() == null)
            return;

        Player player = (Player) e.getWhoClicked();
        GUIUtils.playClickSound(player);
        String path = "difficulties." + holder.difficultyId + ".";

        switch (e.getSlot()) {
            case 10 -> {
                player.closeInventory();
                plugin.getEditorChatInput().requestInput(player,
                        ChatUtils.colorize("&#FFD700&l✏ &7Masukkan display name:"), input -> {
                            plugin.getDungeonManager().setDungeonConfig(holder.dungeonId, path + "display", input);
                            player.sendMessage(ChatUtils.colorize("&#55FF55✔ Display: &f" + input));
                            GUIUtils.playSuccessSound(player);
                            open(player, holder.dungeonId, holder.difficultyId);
                        });
            }
            case 11 -> {
                player.closeInventory();
                plugin.getEditorChatInput().requestInput(player,
                        ChatUtils.colorize("&#55CCFF&l🛡 &7Masukkan min tier (angka):"), input -> {
                            try {
                                int v = Integer.parseInt(input);
                                plugin.getDungeonManager().setDungeonConfig(holder.dungeonId, path + "min-tier", v);
                                player.sendMessage(ChatUtils.colorize("&#55FF55✔ Min tier: &f" + v));
                                GUIUtils.playSuccessSound(player);
                            } catch (NumberFormatException ex) {
                                player.sendMessage(ChatUtils.colorize("&#FF5555✖ Angka tidak valid!"));
                                GUIUtils.playErrorSound(player);
                            }
                            open(player, holder.dungeonId, holder.difficultyId);
                        });
            }
            case 12 -> {
                player.closeInventory();
                plugin.getEditorChatInput().requestInput(player,
                        ChatUtils.colorize("&#FFAA00&l🔑 &7Masukkan key requirement (atau 'none'):"), input -> {
                            plugin.getDungeonManager().setDungeonConfig(holder.dungeonId, path + "key-req", input);
                            player.sendMessage(ChatUtils.colorize("&#55FF55✔ Key req: &f" + input));
                            GUIUtils.playSuccessSound(player);
                            open(player, holder.dungeonId, holder.difficultyId);
                        });
            }
            case 14 -> {
                player.closeInventory();
                plugin.getEditorChatInput().requestInput(player,
                        ChatUtils.colorize("&#FF5555&l💀 &7Masukkan max deaths (angka):"), input -> {
                            try {
                                int v = Integer.parseInt(input);
                                plugin.getDungeonManager().setDungeonConfig(holder.dungeonId, path + "max-deaths", v);
                                player.sendMessage(ChatUtils.colorize("&#55FF55✔ Max deaths: &f" + v));
                                GUIUtils.playSuccessSound(player);
                            } catch (NumberFormatException ex) {
                                player.sendMessage(ChatUtils.colorize("&#FF5555✖ Angka tidak valid!"));
                                GUIUtils.playErrorSound(player);
                            }
                            open(player, holder.dungeonId, holder.difficultyId);
                        });
            }
            case 15 -> {
                player.closeInventory();
                plugin.getEditorChatInput().requestInput(player,
                        ChatUtils.colorize("&#55FF55&l💰 &7Masukkan reward multiplier (misal: 1.5):"), input -> {
                            try {
                                double v = Double.parseDouble(input);
                                plugin.getDungeonManager().setDungeonConfig(holder.dungeonId,
                                        path + "reward-multiplier", v);
                                player.sendMessage(ChatUtils.colorize("&#55FF55✔ Reward multiplier: &f" + v + "x"));
                                GUIUtils.playSuccessSound(player);
                            } catch (NumberFormatException ex) {
                                player.sendMessage(ChatUtils.colorize("&#FF5555✖ Angka tidak valid!"));
                                GUIUtils.playErrorSound(player);
                            }
                            open(player, holder.dungeonId, holder.difficultyId);
                        });
            }
            case 22 -> new DifficultyEditorGUI(plugin).open(player, holder.dungeonId);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof DiffConfigHolder)
            e.setCancelled(true);
    }

    public static class DiffConfigHolder implements InventoryHolder {
        public final String dungeonId, difficultyId;

        public DiffConfigHolder(String d, String diff) {
            this.dungeonId = d;
            this.difficultyId = diff;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
