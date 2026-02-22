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

import java.util.ArrayList;
import java.util.List;

/**
 * Difficulty Matrix Editor — shows all difficulties side-by-side for
 * comparison.
 * Per vision_admin_experience.md §6
 */
public class DifficultyMatrixGUI implements Listener {

    private final NaturalDungeon plugin;

    public DifficultyMatrixGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String dungeonId) {
        Dungeon dungeon = plugin.getDungeonManager().getDungeon(dungeonId);
        if (dungeon == null) {
            player.sendMessage(ChatUtils.colorize("&#FF5555✖ &7Dungeon not found."));
            return;
        }

        Inventory inv = GUIUtils.createGUI(new MatrixHolder(dungeonId), 54,
                "&#FFAA00⚙ ᴅɪꜰꜰɪᴄᴜʟᴛʏ ᴍᴀᴛʀɪx");

        GUIUtils.fillBorder(inv, Material.BLACK_STAINED_GLASS_PANE);
        GUIUtils.fillEmpty(inv, Material.GRAY_STAINED_GLASS_PANE);

        List<DungeonDifficulty> diffs = dungeon.getDifficulties();

        // ─── Row Labels (column 0, slots 10/19/28/37) ───
        inv.setItem(10,
                GUIUtils.createItem(Material.PAPER, "&7&lField", GUIUtils.separator(), "&7↓ Baris", "&7→ Difficulty"));

        // ─── Column Headers + Data ───
        int[] colSlots = { 11, 12, 13, 14, 15, 16 }; // Max 6 difficulties
        for (int d = 0; d < Math.min(diffs.size(), colSlots.length); d++) {
            DungeonDifficulty diff = diffs.get(d);
            String name = diff.getDisplay() != null ? diff.getDisplay() : diff.getId();
            String color = getDiffColor(d);

            // Header
            inv.setItem(colSlots[d], GUIUtils.createItem(getDiffMaterial(d),
                    color + "&l" + name,
                    GUIUtils.separator(),
                    "&7Klik untuk edit"));

            // Row data for each field
            // Max Deaths
            inv.setItem(colSlots[d] + 9, GUIUtils.createItem(Material.RED_DYE,
                    color + "❤ " + diff.getMaxDeaths(),
                    "&7Max Deaths"));

            // Reward Multiplier
            inv.setItem(colSlots[d] + 18, GUIUtils.createItem(Material.GOLD_NUGGET,
                    color + "💰 " + diff.getRewardMultiplier() + "x",
                    "&7Reward Multiplier"));

            // Min Tier
            inv.setItem(colSlots[d] + 27, GUIUtils.createItem(Material.EXPERIENCE_BOTTLE,
                    color + "⚡ Tier " + diff.getMinTier() + "+",
                    "&7Min Tier"));

            // Key Req
            String keyReq = diff.getKeyReq();
            boolean hasKey = keyReq != null && !keyReq.isEmpty() && !keyReq.equalsIgnoreCase("none");
            inv.setItem(colSlots[d] + 36, GUIUtils.createItem(
                    hasKey ? Material.TRIPWIRE_HOOK : Material.IRON_INGOT,
                    color + "🔑 " + (hasKey ? "✔" : "✘"),
                    "&7Key: " + (hasKey ? keyReq : "Tidak perlu")));
        }

        // Row Labels
        inv.setItem(19, GUIUtils.createItem(Material.RED_DYE, "&c&l❤ Max Deaths"));
        inv.setItem(28, GUIUtils.createItem(Material.GOLD_NUGGET, "&e&l💰 Reward"));
        inv.setItem(37, GUIUtils.createItem(Material.EXPERIENCE_BOTTLE, "&b&l⚡ Min Tier"));
        inv.setItem(46, GUIUtils.createItem(Material.TRIPWIRE_HOOK, "&6&l🔑 Key"));

        // ─── Bottom Actions ───
        inv.setItem(45, GUIUtils.createItem(Material.ARROW, "&#FF5555&l← ᴋᴇᴍʙᴀʟɪ"));

        inv.setItem(49, GUIUtils.createItem(Material.EMERALD,
                "&#55FF55&l✚ ᴛᴀᴍʙᴀʜ ᴅɪꜰꜰ",
                GUIUtils.separator(),
                "&7Buat difficulty baru.",
                "",
                "&#FFAA00&l➥ KLIK"));

        player.openInventory(inv);
        GUIUtils.playOpenSound(player);
    }

    private String getDiffColor(int index) {
        return switch (index) {
            case 0 -> "&#55FF55"; // Green
            case 1 -> "&#FFAA00"; // Orange
            case 2 -> "&#FF5555"; // Red
            case 3 -> "&#AA44FF"; // Purple
            case 4 -> "&#FF0000"; // Dark Red
            default -> "&#FFFFFF";
        };
    }

    private Material getDiffMaterial(int index) {
        return switch (index) {
            case 0 -> Material.LIME_CONCRETE;
            case 1 -> Material.YELLOW_CONCRETE;
            case 2 -> Material.RED_CONCRETE;
            case 3 -> Material.PURPLE_CONCRETE;
            case 4 -> Material.BLACK_CONCRETE;
            default -> Material.WHITE_CONCRETE;
        };
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof MatrixHolder holder))
            return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getView().getTopInventory())
            return;
        if (e.getCurrentItem() == null)
            return;

        Player player = (Player) e.getWhoClicked();
        GUIUtils.playClickSound(player);

        if (e.getSlot() == 45) {
            // Back → DifficultyEditorGUI
            new DifficultyEditorGUI(plugin).open(player, holder.dungeonId);
            return;
        }

        if (e.getSlot() == 49) {
            // Add difficulty
            player.closeInventory();
            plugin.getEditorChatInput().requestInput(player,
                    ChatUtils.colorize("&#FFD700&l⚙ &7Ketik nama difficulty baru (ID):"), input -> {
                        String id = input.toLowerCase().replace(" ", "_");
                        plugin.getDungeonManager().setDungeonConfig(holder.dungeonId,
                                "difficulties." + id + ".display", input);
                        plugin.getDungeonManager().setDungeonConfig(holder.dungeonId,
                                "difficulties." + id + ".max-deaths", 3);
                        plugin.getDungeonManager().setDungeonConfig(holder.dungeonId,
                                "difficulties." + id + ".reward-multiplier", 1.0);
                        plugin.getDungeonManager().setDungeonConfig(holder.dungeonId,
                                "difficulties." + id + ".min-tier", 0);
                        plugin.getDungeonManager().setDungeonConfig(holder.dungeonId,
                                "difficulties." + id + ".key-req", "none");
                        plugin.getDungeonManager().loadDungeons();
                        player.sendMessage(
                                ChatUtils.colorize("&#55FF55✔ Difficulty &f" + id + " &#55FF55ditambahkan!"));
                        GUIUtils.playSuccessSound(player);
                        open(player, holder.dungeonId);
                    });
            return;
        }

        // Click on column header → open individual difficulty config
        int[] colSlots = { 11, 12, 13, 14, 15, 16 };
        Dungeon dungeon = plugin.getDungeonManager().getDungeon(holder.dungeonId);
        if (dungeon == null)
            return;
        List<DungeonDifficulty> diffs = dungeon.getDifficulties();

        for (int d = 0; d < Math.min(diffs.size(), colSlots.length); d++) {
            if (e.getSlot() == colSlots[d]) {
                new DifficultyConfigGUI(plugin).open(player, holder.dungeonId, diffs.get(d).getId());
                return;
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof MatrixHolder)
            e.setCancelled(true);
    }

    public static class MatrixHolder implements InventoryHolder {
        public final String dungeonId;

        public MatrixHolder(String id) {
            this.dungeonId = id;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
