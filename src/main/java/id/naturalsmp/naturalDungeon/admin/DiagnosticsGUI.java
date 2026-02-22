package id.naturalsmp.naturaldungeon.admin;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.dungeon.Dungeon;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import id.naturalsmp.naturaldungeon.utils.DungeonValidator;
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
 * Visual Diagnostics Dashboard — shows all validation checks with auto-fix
 * buttons.
 * Per vision_admin_experience.md §7: Smart Diagnostics & Auto-Fix
 */
public class DiagnosticsGUI implements Listener {

    private final NaturalDungeon plugin;

    public DiagnosticsGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String dungeonId) {
        Dungeon dungeon = plugin.getDungeonManager().getDungeon(dungeonId);
        if (dungeon == null) {
            player.sendMessage(ChatUtils.colorize("&#FF5555✖ &7Dungeon not found."));
            GUIUtils.playErrorSound(player);
            return;
        }

        List<String> errors = DungeonValidator.validate(plugin, dungeon);
        boolean valid = errors.isEmpty();
        int totalChecks = 10;
        int passed = totalChecks - errors.size();

        Inventory inv = GUIUtils.createGUI(new DiagHolder(dungeonId), 54,
                "&#55CCFF🔍 ᴅɪᴀɢɴᴏꜱᴛɪᴄꜱ: &f" + dungeon.getDisplayName());

        GUIUtils.fillBorder(inv, Material.BLACK_STAINED_GLASS_PANE);
        GUIUtils.fillEmpty(inv, Material.GRAY_STAINED_GLASS_PANE);

        // ─── Overall Status (slot 4) ───
        if (valid) {
            inv.setItem(4, GUIUtils.createItem(Material.LIME_CONCRETE,
                    "&#55FF55&l✔ SEMUA CHECK LULUS",
                    GUIUtils.separator(),
                    "&#55FF55" + passed + "/" + totalChecks + " check passed",
                    "",
                    "&7Dungeon siap dimainkan!"));
        } else {
            inv.setItem(4, GUIUtils.createItem(Material.RED_CONCRETE,
                    "&#FF5555&l⚠ " + errors.size() + " MASALAH DITEMUKAN",
                    GUIUtils.separator(),
                    "&#FFAA00" + passed + "/" + totalChecks + " check passed",
                    "",
                    "&7Perbaiki masalah di bawah."));
        }

        // ─── Checks Grid (slots 19-34) ───
        int slotIdx = 19;

        // Check 1: World loaded
        boolean worldOk = dungeon.getWorld() != null && org.bukkit.Bukkit.getWorld(dungeon.getWorld()) != null;
        inv.setItem(slotIdx++, createCheck(worldOk, "World Loaded", dungeon.getWorld()));

        // Check 2: Difficulties
        boolean diffOk = !dungeon.getDifficulties().isEmpty();
        inv.setItem(slotIdx++, createCheck(diffOk, "Difficulties Configured",
                diffOk ? dungeon.getDifficulties().size() + " difficulties" : "Tidak ada!"));

        // Check 3: Stages
        boolean stagesOk = !dungeon.getStages().isEmpty();
        inv.setItem(slotIdx++, createCheck(stagesOk, "Stages Defined",
                stagesOk ? dungeon.getStages().size() + " stages" : "Tidak ada!"));

        // Per-stage checks
        for (int s = 0; s < dungeon.getStages().size() && slotIdx <= 34; s++) {
            Dungeon.Stage stage = dungeon.getStages().get(s);
            List<String> stageErrors = DungeonValidator.validateStage(plugin, dungeon, stage);
            boolean stageOk = stageErrors.isEmpty();

            List<String> lore = new ArrayList<>();
            lore.add(GUIUtils.separator());
            if (stageOk) {
                lore.add("&#55FF55✔ Semua check lulus");
            } else {
                for (String err : stageErrors) {
                    lore.add("&#FF5555• " + err);
                }
            }

            inv.setItem(slotIdx++, GUIUtils.createItem(
                    stageOk ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE,
                    (stageOk ? "&#55FF55" : "&#FF5555") + "&l" + (stageOk ? "✔" : "✖") + " Stage " + (s + 1),
                    lore));
        }

        // ─── Bottom Actions ───

        // Fix All (slot 47)
        if (!valid) {
            inv.setItem(47, GUIUtils.createItem(Material.ANVIL,
                    "&#FFAA00&l🔧 ꜰɪx ᴀʟʟ",
                    GUIUtils.separator(),
                    "&7Auto-fix semua masalah yang bisa",
                    "&7diperbaiki otomatis.",
                    "",
                    "&#FFAA00&l➥ KLIK"));
        }

        // Test Dungeon (slot 49)
        if (valid) {
            inv.setItem(49, GUIUtils.createItem(Material.ENDER_EYE,
                    "&#55FF55&l▶ ᴛᴇꜱᴛ ᴅᴜɴɢᴇᴏɴ",
                    GUIUtils.separator(),
                    "&7Langsung test dungeon ini.",
                    "",
                    "&#55FF55&l➥ KLIK"));
        }

        // Back (slot 53)
        inv.setItem(53, GUIUtils.createItem(Material.ARROW, "&#FF5555&l← ᴋᴇᴍʙᴀʟɪ"));

        player.openInventory(inv);
        GUIUtils.playOpenSound(player);
    }

    private org.bukkit.inventory.ItemStack createCheck(boolean ok, String name, String detail) {
        return GUIUtils.createItem(
                ok ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE,
                (ok ? "&#55FF55&l✔ " : "&#FF5555&l✖ ") + name,
                GUIUtils.separator(),
                "&7" + (detail != null ? detail : "N/A"));
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof DiagHolder holder))
            return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getView().getTopInventory())
            return;
        if (e.getCurrentItem() == null)
            return;

        Player player = (Player) e.getWhoClicked();
        GUIUtils.playClickSound(player);

        switch (e.getSlot()) {
            case 47 -> {
                // Fix All
                Dungeon dungeon = plugin.getDungeonManager().getDungeon(holder.dungeonId);
                if (dungeon != null) {
                    List<String> fixes = DungeonValidator.autoFix(plugin, dungeon);
                    if (fixes.isEmpty()) {
                        player.sendMessage(ChatUtils.colorize("&#55FF55✔ &7Tidak ada yang bisa di-fix otomatis."));
                    } else {
                        player.sendMessage(ChatUtils.colorize("&#FFAA00&l🔧 Auto-Fix Applied:"));
                        for (String fix : fixes) {
                            player.sendMessage(ChatUtils.colorize("  " + fix));
                        }
                    }
                    GUIUtils.playSuccessSound(player);
                    open(player, holder.dungeonId); // Refresh
                }
            }
            case 49 -> {
                // Test
                player.closeInventory();
                player.performCommand("nd test " + holder.dungeonId);
            }
            case 53 -> {
                // Back
                new id.naturalsmp.naturaldungeon.editor.DungeonMainEditorGUI(plugin).open(player, holder.dungeonId);
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof DiagHolder)
            e.setCancelled(true);
    }

    public static class DiagHolder implements InventoryHolder {
        public final String dungeonId;

        public DiagHolder(String id) {
            this.dungeonId = id;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
