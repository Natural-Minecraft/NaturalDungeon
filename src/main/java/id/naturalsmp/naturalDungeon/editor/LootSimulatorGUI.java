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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Loot Table Editor + Simulator — shows current loot entries and runs Monte
 * Carlo simulation.
 * Per vision_admin_experience.md §5: Loot Table Editor
 */
public class LootSimulatorGUI implements Listener {

    private final NaturalDungeon plugin;
    private static final Random RNG = new Random();

    public LootSimulatorGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String dungeonId) {
        Dungeon dungeon = plugin.getDungeonManager().getDungeon(dungeonId);
        if (dungeon == null) {
            player.sendMessage(ChatUtils.colorize("&#FF5555✖ &7Dungeon not found."));
            return;
        }

        List<Map<?, ?>> entries = plugin.getDungeonManager().getLootEntries(dungeonId);

        Inventory inv = GUIUtils.createGUI(new SimHolder(dungeonId), 54,
                "&#FFD700🎁 ʟᴏᴏᴛ ᴇᴅɪᴛᴏʀ + ꜱɪᴍ");

        GUIUtils.fillBorder(inv, Material.BLACK_STAINED_GLASS_PANE);
        GUIUtils.fillEmpty(inv, Material.GRAY_STAINED_GLASS_PANE);

        // ─── Loot Entries (slots 10-34) ───
        int[] slots = { 10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34 };

        for (int i = 0; i < Math.min(entries.size(), slots.length); i++) {
            Map<?, ?> entry = entries.get(i);
            String type = entry.get("type") != null ? entry.get("type").toString() : "VANILLA";
            String item = entry.get("item") != null ? entry.get("item").toString() : "DIAMOND";
            int minAmt = entry.get("min-amount") != null ? ((Number) entry.get("min-amount")).intValue() : 1;
            int maxAmt = entry.get("max-amount") != null ? ((Number) entry.get("max-amount")).intValue() : 1;
            double chance = entry.get("chance") != null ? ((Number) entry.get("chance")).doubleValue() : 100.0;

            String icon_type = switch (type.toUpperCase()) {
                case "MMOITEMS" -> "&#AA44FF";
                case "COMMAND" -> "&#55CCFF";
                default -> "&#55FF55";
            };

            Material mat;
            try {
                mat = type.equalsIgnoreCase("VANILLA") ? Material.valueOf(item.toUpperCase()) : Material.PAPER;
            } catch (IllegalArgumentException e) {
                mat = Material.PAPER;
            }

            String amtStr = minAmt == maxAmt ? String.valueOf(minAmt) : minAmt + "-" + maxAmt;
            String chanceColor = chance >= 75 ? "&#55FF55" : chance >= 30 ? "&#FFAA00" : "&#FF5555";

            inv.setItem(slots[i], GUIUtils.createItem(mat,
                    icon_type + "&l" + item,
                    GUIUtils.separator(),
                    "&7Type: &f" + type,
                    "&7Amount: &f" + amtStr,
                    "&7Chance: " + chanceColor + String.format("%.0f", chance) + "%",
                    "",
                    "&#FFAA00&l➥ KLIK untuk edit",
                    "&#FF5555&lShift+Klik → Hapus"));
        }

        // ─── Bottom Actions ───
        inv.setItem(37, GUIUtils.createItem(Material.EMERALD,
                "&#55FF55&l✚ ᴛᴀᴍʙᴀʜ ᴅʀᴏᴘ",
                GUIUtils.separator(),
                "&7Tambah loot entry baru.",
                "",
                "&#FFAA00&l➥ KLIK"));

        inv.setItem(40, GUIUtils.createItem(Material.HOPPER,
                "&#FFD700&l🔄 ꜱɪᴍᴜʟᴀᴛᴇ (100x)",
                GUIUtils.separator(),
                "&7Jalankan 100 simulasi & lihat",
                "&7rata-rata drop per run.",
                "",
                "&#FFAA00&l➥ KLIK"));

        inv.setItem(43, GUIUtils.createItem(Material.ARROW, "&#FF5555&l← ᴋᴇᴍʙᴀʟɪ"));

        player.openInventory(inv);
        GUIUtils.playOpenSound(player);
    }

    private void runSimulation(Player player, String dungeonId) {
        List<Map<?, ?>> entries = plugin.getDungeonManager().getLootEntries(dungeonId);
        if (entries.isEmpty()) {
            player.sendMessage(ChatUtils.colorize("&#FF5555✖ &7No loot entries to simulate."));
            GUIUtils.playErrorSound(player);
            return;
        }

        int runs = 100;
        // Track total drops per entry
        int[] totalDrops = new int[entries.size()];
        int[] totalAmount = new int[entries.size()];

        for (int r = 0; r < runs; r++) {
            for (int i = 0; i < entries.size(); i++) {
                Map<?, ?> entry = entries.get(i);
                double chance = entry.get("chance") != null ? ((Number) entry.get("chance")).doubleValue() : 100.0;
                int minAmt = entry.get("min-amount") != null ? ((Number) entry.get("min-amount")).intValue() : 1;
                int maxAmt = entry.get("max-amount") != null ? ((Number) entry.get("max-amount")).intValue() : 1;

                if (RNG.nextDouble() * 100 <= chance) {
                    totalDrops[i]++;
                    totalAmount[i] += minAmt + RNG.nextInt(Math.max(1, maxAmt - minAmt + 1));
                }
            }
        }

        // Display results
        player.sendMessage(ChatUtils.colorize(""));
        player.sendMessage(ChatUtils.colorize("&#FFD700&l🔄 Simulasi " + runs + " runs:"));
        player.sendMessage(ChatUtils.colorize("&#777" + GUIUtils.separator()));

        for (int i = 0; i < entries.size(); i++) {
            Map<?, ?> entry = entries.get(i);
            String item = entry.get("item") != null ? entry.get("item").toString() : "?";
            double avgAmt = runs > 0 ? (double) totalAmount[i] / runs : 0;
            double dropRate = runs > 0 ? (double) totalDrops[i] / runs * 100 : 0;

            player.sendMessage(ChatUtils.colorize(
                    "  &#FFAA00" + item + " &7— Avg: &#55FF55" + String.format("%.1f", avgAmt) +
                            " &7| Drop: &#55CCFF" + String.format("%.0f", dropRate) + "%"));
        }

        player.sendMessage(ChatUtils.colorize(""));
        GUIUtils.playSuccessSound(player);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof SimHolder holder))
            return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getView().getTopInventory())
            return;
        if (e.getCurrentItem() == null)
            return;

        Player player = (Player) e.getWhoClicked();
        GUIUtils.playClickSound(player);

        switch (e.getSlot()) {
            case 37 -> {
                // Add loot entry
                plugin.getDungeonManager().addLootEntry(holder.dungeonId);
                player.sendMessage(ChatUtils.colorize("&#55FF55✔ Loot entry ditambahkan!"));
                GUIUtils.playSuccessSound(player);
                open(player, holder.dungeonId);
            }
            case 40 -> {
                // Simulate
                player.closeInventory();
                runSimulation(player, holder.dungeonId);
            }
            case 43 -> {
                // Back
                new DungeonMainEditorGUI(plugin).open(player, holder.dungeonId);
            }
            default -> {
                // Edit or delete loot entry
                int[] slots = { 10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34 };
                int entryIdx = -1;
                for (int i = 0; i < slots.length; i++) {
                    if (slots[i] == e.getSlot()) {
                        entryIdx = i;
                        break;
                    }
                }
                if (entryIdx >= 0) {
                    List<Map<?, ?>> entries = plugin.getDungeonManager().getLootEntries(holder.dungeonId);
                    if (entryIdx < entries.size()) {
                        if (e.isShiftClick()) {
                            plugin.getDungeonManager().deleteLootEntry(holder.dungeonId, entryIdx);
                            player.sendMessage(ChatUtils.colorize("&#FF5555✖ Loot entry dihapus!"));
                            open(player, holder.dungeonId);
                        } else {
                            new LootEntryEditorGUI(plugin).open(player, holder.dungeonId, entryIdx);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof SimHolder)
            e.setCancelled(true);
    }

    public static class SimHolder implements InventoryHolder {
        public final String dungeonId;

        public SimHolder(String id) {
            this.dungeonId = id;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
