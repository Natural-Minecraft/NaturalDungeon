package id.naturalsmp.naturaldungeon.editor;

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

import java.util.*;

/**
 * Main dashboard for editing a single dungeon.
 * Premium admin panel with status overview and navigation to sub-editors.
 */
public class DungeonMainEditorGUI implements Listener {

    private final NaturalDungeon plugin;

    public DungeonMainEditorGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String dungeonId) {
        Dungeon dungeon = plugin.getDungeonManager().getDungeon(dungeonId);
        String displayName = dungeon != null ? dungeon.getDisplayName() : dungeonId;

        Inventory inv = GUIUtils.createGUI(new MainHolder(dungeonId), 45,
                "&#FFD700⚙ ᴇᴅɪᴛ: &f" + displayName);

        GUIUtils.fillBorder(inv, Material.BLACK_STAINED_GLASS_PANE);
        GUIUtils.fillEmpty(inv, Material.GRAY_STAINED_GLASS_PANE);

        // ─── Validation Status (slot 4) ───
        List<String> errors = dungeon != null ? DungeonValidator.validate(plugin, dungeon)
                : List.of("Dungeon not found");
        boolean isValid = errors.isEmpty();

        List<String> statusLore = new ArrayList<>();
        statusLore.add(GUIUtils.separator());
        if (isValid) {
            statusLore.add("&#55FF55✔ Dungeon siap dimainkan!");
            statusLore.add("");
            statusLore.add("&7Semua konfigurasi valid.");
        } else {
            statusLore.add("&#FF5555⚠ " + errors.size() + " masalah ditemukan:");
            for (int i = 0; i < Math.min(5, errors.size()); i++) {
                statusLore.add("  &#FF5555• " + errors.get(i));
            }
            if (errors.size() > 5) {
                statusLore.add("  &8...dan " + (errors.size() - 5) + " lainnya");
            }
        }
        inv.setItem(4, GUIUtils.createItem(
                isValid ? Material.LIME_CONCRETE : Material.RED_CONCRETE,
                (isValid ? "&#55FF55" : "&#FF5555") + "&l" + (isValid ? "✔ VALID" : "⚠ PERLU PERBAIKAN"),
                statusLore));

        // ─── Editor Buttons ───

        // Basic Info (slot 19)
        inv.setItem(19, GUIUtils.createItem(Material.NAME_TAG,
                "&#55CCFF&l📝 ʙᴀꜱɪᴄ ɪɴꜰᴏ",
                GUIUtils.separator(),
                "&7Edit nama, deskripsi, world,",
                "&7max player, cooldown, icon.",
                "",
                "&#FFAA00&l➥ KLIK"));

        // Difficulties (slot 20)
        inv.setItem(20, GUIUtils.createItem(Material.IRON_SWORD,
                "&#FF5555&l⚔ ᴅɪꜰꜰɪᴄᴜʟᴛɪᴇꜱ",
                GUIUtils.separator(),
                "&7Kelola tingkat kesulitan.",
                "&7Tier, lives, loot, keys.",
                "",
                "&#FFAA00&l➥ KLIK"));

        // Stages (slot 21)
        inv.setItem(21, GUIUtils.createItem(Material.LADDER,
                "&#FFAA00&l🏗 ꜱᴛᴀɢᴇꜱ",
                GUIUtils.separator(),
                "&7Kelola stage & regions.",
                "&7Arena, safe zone, spawn.",
                "",
                "&#FFAA00&l➥ KLIK"));

        // Boss Editor (slot 22)
        inv.setItem(22, GUIUtils.createItem(Material.WITHER_SKELETON_SKULL,
                "&#AA44FF&l🐉 ʙᴏꜱꜱ ᴇᴅɪᴛᴏʀ",
                GUIUtils.separator(),
                "&7Edit boss config & skills.",
                "&7Multi-phase, health, drops.",
                "",
                "&#FFAA00&l➥ KLIK"));

        // Rewards (slot 23)
        inv.setItem(23, GUIUtils.createItem(Material.CHEST,
                "&#55FF55&l🎁 ʀᴇᴡᴀʀᴅꜱ",
                GUIUtils.separator(),
                "&7Edit loot table.",
                "&7Vanilla, MMOItems, Command.",
                "",
                "&#FFAA00&l➥ KLIK"));

        // Mobs (slot 24)
        inv.setItem(24, GUIUtils.createItem(Material.ZOMBIE_HEAD,
                "&#FF69B4&l👹 ᴍᴏʙꜱ",
                GUIUtils.separator(),
                "&7Kelola custom mobs.",
                "&7MythicMobs, Vanilla, Skills.",
                "",
                "&#FFAA00&l➥ KLIK"));

        // Setup Tools (slot 25)
        inv.setItem(25, GUIUtils.createItem(Material.GOLDEN_AXE,
                "&#FFD700&l🔧 ꜱᴇᴛᴜᴘ ᴛᴏᴏʟꜱ",
                GUIUtils.separator(),
                "&7Wand tools, region marker,",
                "&7particle visualizer.",
                "",
                "&#FFAA00&l➥ KLIK"));

        // ─── Bottom Buttons ───

        // Back (slot 36)
        inv.setItem(36, GUIUtils.createItem(Material.ARROW,
                "&#FF5555&l← ᴋᴇᴍʙᴀʟɪ",
                "&7Kembali ke daftar dungeon."));

        // Test Mode (slot 40)
        inv.setItem(40, GUIUtils.createItem(Material.ENDER_EYE,
                "&#55FF55&l▶ ᴛᴇꜱᴛ ᴍᴏᴅᴇ",
                GUIUtils.separator(),
                "&7Jalankan dungeon dalam test mode.",
                "&7God Mode, Skip Wave, dll.",
                "",
                "&#FFAA00&l➥ KLIK"));

        // Auto-Fix (slot 44)
        inv.setItem(44, GUIUtils.createItem(Material.ANVIL,
                "&#FFAA00&l🔧 ᴀᴜᴛᴏ-ꜰɪx",
                GUIUtils.separator(),
                "&7Perbaiki masalah umum.",
                "&7Buat default difficulty,",
                "&7stage, region names.",
                "",
                "&#FFAA00&l➥ KLIK"));

        player.openInventory(inv);
        GUIUtils.playOpenSound(player);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof MainHolder holder))
            return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getView().getTopInventory())
            return;
        if (e.getCurrentItem() == null)
            return;

        Player player = (Player) e.getWhoClicked();
        String dungeonId = holder.dungeonId;
        GUIUtils.playClickSound(player);

        switch (e.getSlot()) {
            case 19 -> new BasicInfoEditorGUI(plugin).open(player, dungeonId);
            case 20 -> new DifficultyEditorGUI(plugin).open(player, dungeonId);
            case 21 -> new StageEditorGUI(plugin).open(player, dungeonId);
            case 22 -> new BossEditorGUI(plugin).open(player, dungeonId);
            case 23 -> new RewardEditorGUI(plugin).open(player, dungeonId);
            case 24 -> new MobEditorGUI(plugin).open(player, dungeonId);
            case 25 -> {
                // Setup tools info
                player.closeInventory();
                player.sendMessage(
                        ChatUtils.colorize("&#FFD700&l⚙ &7Gunakan &f/nd wand &7untuk mendapatkan setup tools."));
                player.sendMessage(
                        ChatUtils.colorize("&7Gunakan wand untuk menandai &fregion &7dan &fspawn points&7."));
            }
            case 36 -> new DungeonListEditorGUI(plugin).open(player);
            case 40 -> {
                // Start test mode
                player.closeInventory();
                player.performCommand("nd test " + dungeonId);
            }
            case 44 -> {
                // Auto-fix
                Dungeon dungeon = plugin.getDungeonManager().getDungeon(dungeonId);
                if (dungeon != null) {
                    java.util.List<String> fixes = DungeonValidator.autoFix(plugin, dungeon);
                    if (fixes.isEmpty()) {
                        player.sendMessage(ChatUtils.colorize("&#55FF55✔ &7Tidak ada yang perlu diperbaiki!"));
                        GUIUtils.playSuccessSound(player);
                    } else {
                        player.sendMessage(ChatUtils.colorize("&#FFAA00&l🔧 Auto-Fix Applied:"));
                        for (String fix : fixes) {
                            player.sendMessage(ChatUtils.colorize("  " + fix));
                        }
                        GUIUtils.playSuccessSound(player);
                    }
                    // Refresh the GUI
                    open(player, dungeonId);
                }
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof MainHolder)
            e.setCancelled(true);
    }

    public static class MainHolder implements InventoryHolder {
        public final String dungeonId;

        public MainHolder(String dungeonId) {
            this.dungeonId = dungeonId;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
