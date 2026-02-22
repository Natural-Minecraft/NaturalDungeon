package id.naturalsmp.naturaldungeon.editor;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.dungeon.Dungeon;
import id.naturalsmp.naturaldungeon.dungeon.DungeonDifficulty;
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
 * List and manage difficulties for a dungeon.
 */
public class DifficultyEditorGUI implements Listener {

    private final NaturalDungeon plugin;

    public DifficultyEditorGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String dungeonId) {
        Dungeon dungeon = plugin.getDungeonManager().getDungeon(dungeonId);

        Inventory inv = GUIUtils.createGUI(new DiffEditorHolder(dungeonId), 27,
                "&#FF5555⚔ ᴅɪꜰꜰɪᴄᴜʟᴛɪᴇꜱ: &f" + dungeonId);

        GUIUtils.fillAll(inv, Material.BLACK_STAINED_GLASS_PANE);

        if (dungeon != null) {
            int slot = 10;
            for (DungeonDifficulty diff : dungeon.getDifficulties()) {
                if (slot > 16)
                    break;

                String color = switch (diff.getId().toLowerCase()) {
                    case "easy" -> "&#55FF55";
                    case "normal" -> "&#FFFF55";
                    case "hard" -> "&#FFAA00";
                    case "hell", "nightmare" -> "&#FF5555";
                    default -> "&#FFFFFF";
                };

                inv.setItem(slot, GUIUtils.createItem(Material.IRON_SWORD,
                        color + "&l" + diff.getDisplayName(),
                        GUIUtils.separator(),
                        "&7ID: &f" + diff.getId(),
                        "&7Min Tier: &f" + diff.getMinTier(),
                        "&7Max Deaths: &#FF5555" + diff.getMaxDeaths(),
                        "&7Reward: &#55FF55" + diff.getRewardMultiplier() + "x",
                        "",
                        "&#FFAA00&l⚔ Klik &7→ Edit",
                        "&#FF5555&l✖ Shift+Klik &7→ Hapus"));
                slot++;
            }
        }

        inv.setItem(22, GUIUtils.createItem(Material.EMERALD,
                "&#55FF55&l✚ ᴛᴀᴍʙᴀʜ ᴅɪꜰꜰɪᴄᴜʟᴛʏ",
                GUIUtils.separator(),
                "&7Buat difficulty baru.",
                "",
                "&#FFAA00&l➥ KLIK"));

        inv.setItem(18, GUIUtils.createItem(Material.ARROW,
                "&#FF5555&l← ᴋᴇᴍʙᴀʟɪ"));

        inv.setItem(26, GUIUtils.createItem(Material.COMPARATOR,
                "&#FFAA00&l⚙ ᴅɪꜰꜰɪᴄᴜʟᴛʏ ᴍᴀᴛʀɪx",
                GUIUtils.separator(),
                "&7Bandingkan semua difficulties",
                "&7secara side-by-side.",
                "",
                "&#FFAA00&l➥ KLIK"));

        player.openInventory(inv);
        GUIUtils.playOpenSound(player);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof DiffEditorHolder holder))
            return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getView().getTopInventory())
            return;
        if (e.getCurrentItem() == null)
            return;

        Player player = (Player) e.getWhoClicked();
        GUIUtils.playClickSound(player);

        if (e.getSlot() == 18) {
            new DungeonMainEditorGUI(plugin).open(player, holder.dungeonId);
            return;
        }
        if (e.getSlot() == 26) {
            new DifficultyMatrixGUI(plugin).open(player, holder.dungeonId);
            return;
        }
        if (e.getSlot() == 22) {
            player.closeInventory();
            plugin.getEditorChatInput().requestInput(player,
                    ChatUtils.colorize("&#55FF55&l✚ &7Masukkan ID difficulty (misal: normal, hard, hell):"),
                    input -> {
                        String id = input.toLowerCase().replace(" ", "_");
                        plugin.getDungeonManager().createDifficulty(holder.dungeonId, id);
                        player.sendMessage(ChatUtils.colorize("&#55FF55✔ Difficulty &f" + id + " &#55FF55dibuat!"));
                        GUIUtils.playSuccessSound(player);
                        open(player, holder.dungeonId);
                    });
            return;
        }

        if (e.getSlot() >= 10 && e.getSlot() <= 16) {
            Dungeon dungeon = plugin.getDungeonManager().getDungeon(holder.dungeonId);
            if (dungeon == null)
                return;
            int idx = e.getSlot() - 10;
            List<DungeonDifficulty> diffs = new ArrayList<>(dungeon.getDifficulties());
            if (idx >= diffs.size())
                return;
            DungeonDifficulty diff = diffs.get(idx);

            if (e.isShiftClick()) {
                plugin.getDungeonManager().deleteDifficulty(holder.dungeonId, diff.getId());
                player.sendMessage(ChatUtils.colorize("&#FF5555✖ Difficulty &f" + diff.getId() + " &#FF5555dihapus!"));
                open(player, holder.dungeonId);
            } else {
                new DifficultyConfigGUI(plugin).open(player, holder.dungeonId, diff.getId());
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof DiffEditorHolder)
            e.setCancelled(true);
    }

    public static class DiffEditorHolder implements InventoryHolder {
        public final String dungeonId;

        public DiffEditorHolder(String id) {
            this.dungeonId = id;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
