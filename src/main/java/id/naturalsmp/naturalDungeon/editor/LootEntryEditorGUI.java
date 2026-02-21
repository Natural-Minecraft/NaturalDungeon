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
 * Edit a single loot entry: type, material/ID, amount, chance, rarity.
 */
public class LootEntryEditorGUI implements Listener {

    private final NaturalDungeon plugin;

    public LootEntryEditorGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String dungeonId, int entryIndex) {
        Inventory inv = GUIUtils.createGUI(new LootEntryHolder(dungeonId, entryIndex), 27,
                "&#FFD700🎁 ʟᴏᴏᴛ ᴇɴᴛʀʏ &f#" + (entryIndex + 1));

        GUIUtils.fillAll(inv, Material.BLACK_STAINED_GLASS_PANE);

        inv.setItem(10, GUIUtils.createItem(Material.PAPER,
                "&#FFAA00&l📋 ᴛʏᴘᴇ",
                GUIUtils.separator(),
                "&7VANILLA / MMOITEMS / COMMAND",
                "",
                "&#FFAA00&l➥ KLIK"));

        inv.setItem(11, GUIUtils.createItem(Material.DIAMOND,
                "&#55CCFF&l💎 ᴍᴀᴛᴇʀɪᴀʟ / ɪᴅ",
                GUIUtils.separator(),
                "&7Material vanilla atau",
                "&7MMOItems &fTYPE:ID&7 format.",
                "",
                "&#FFAA00&l➥ KLIK"));

        inv.setItem(12, GUIUtils.createItem(Material.CHEST,
                "&#55FF55&l📦 ᴀᴍᴏᴜɴᴛ",
                GUIUtils.separator(),
                "&7Jumlah item (angka/range).",
                "&7Contoh: &f1-3",
                "",
                "&#FFAA00&l➥ KLIK"));

        inv.setItem(14, GUIUtils.createItem(Material.RABBIT_FOOT,
                "&#FFAA00&l🎲 ᴄʜᴀɴᴄᴇ",
                GUIUtils.separator(),
                "&7Peluang drop &f(0-100%)&7.",
                "",
                "&#FFAA00&l➥ KLIK"));

        inv.setItem(15, GUIUtils.createItem(Material.NETHER_STAR,
                "&#AA44FF&l⭐ ʀᴀʀɪᴛʏ",
                GUIUtils.separator(),
                "&7COMMON / RARE / EPIC / LEGENDARY",
                "",
                "&#FFAA00&l➥ KLIK"));

        inv.setItem(22, GUIUtils.createItem(Material.ARROW, "&#FF5555&l← ᴋᴇᴍʙᴀʟɪ"));

        player.openInventory(inv);
        GUIUtils.playOpenSound(player);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof LootEntryHolder holder))
            return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getView().getTopInventory())
            return;
        if (e.getCurrentItem() == null)
            return;

        Player player = (Player) e.getWhoClicked();
        GUIUtils.playClickSound(player);
        String path = "loot.completion." + holder.entryIndex + ".";

        switch (e.getSlot()) {
            case 10 -> {
                player.closeInventory();
                plugin.getEditorChatInput().requestInput(player,
                        ChatUtils.colorize("&#FFAA00&l📋 &7Tipe loot (VANILLA/MMOITEMS/COMMAND):"), input -> {
                            plugin.getDungeonManager().setDungeonConfig(holder.dungeonId, path + "type",
                                    input.toUpperCase());
                            player.sendMessage(ChatUtils.colorize("&#55FF55✔ Tipe: &f" + input.toUpperCase()));
                            GUIUtils.playSuccessSound(player);
                            open(player, holder.dungeonId, holder.entryIndex);
                        });
            }
            case 11 -> {
                player.closeInventory();
                plugin.getEditorChatInput().requestInput(player,
                        ChatUtils.colorize("&#55CCFF&l💎 &7Material/ID (misal: DIAMOND_SWORD atau SWORD:DarkBlade):"),
                        input -> {
                            plugin.getDungeonManager().setDungeonConfig(holder.dungeonId, path + "material", input);
                            player.sendMessage(ChatUtils.colorize("&#55FF55✔ Material: &f" + input));
                            GUIUtils.playSuccessSound(player);
                            open(player, holder.dungeonId, holder.entryIndex);
                        });
            }
            case 12 -> {
                player.closeInventory();
                plugin.getEditorChatInput().requestInput(player,
                        ChatUtils.colorize("&#55FF55&l📦 &7Jumlah (angka atau range 1-3):"), input -> {
                            plugin.getDungeonManager().setDungeonConfig(holder.dungeonId, path + "amount", input);
                            player.sendMessage(ChatUtils.colorize("&#55FF55✔ Amount: &f" + input));
                            GUIUtils.playSuccessSound(player);
                            open(player, holder.dungeonId, holder.entryIndex);
                        });
            }
            case 14 -> {
                player.closeInventory();
                plugin.getEditorChatInput().requestInput(player,
                        ChatUtils.colorize("&#FFAA00&l🎲 &7Chance drop (0-100):"), input -> {
                            try {
                                int v = Integer.parseInt(input);
                                plugin.getDungeonManager().setDungeonConfig(holder.dungeonId, path + "chance", v);
                                player.sendMessage(ChatUtils.colorize("&#55FF55✔ Chance: &f" + v + "%"));
                                GUIUtils.playSuccessSound(player);
                            } catch (NumberFormatException ex) {
                                player.sendMessage(ChatUtils.colorize("&#FF5555✖ Angka tidak valid!"));
                                GUIUtils.playErrorSound(player);
                            }
                            open(player, holder.dungeonId, holder.entryIndex);
                        });
            }
            case 15 -> {
                player.closeInventory();
                plugin.getEditorChatInput().requestInput(player,
                        ChatUtils.colorize("&#AA44FF&l⭐ &7Rarity (COMMON/RARE/EPIC/LEGENDARY):"), input -> {
                            plugin.getDungeonManager().setDungeonConfig(holder.dungeonId, path + "rarity",
                                    input.toUpperCase());
                            player.sendMessage(ChatUtils.colorize("&#55FF55✔ Rarity: &f" + input.toUpperCase()));
                            GUIUtils.playSuccessSound(player);
                            open(player, holder.dungeonId, holder.entryIndex);
                        });
            }
            case 22 -> new RewardEditorGUI(plugin).open(player, holder.dungeonId);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof LootEntryHolder)
            e.setCancelled(true);
    }

    public static class LootEntryHolder implements InventoryHolder {
        public final String dungeonId;
        public final int entryIndex;

        public LootEntryHolder(String d, int e) {
            this.dungeonId = d;
            this.entryIndex = e;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
