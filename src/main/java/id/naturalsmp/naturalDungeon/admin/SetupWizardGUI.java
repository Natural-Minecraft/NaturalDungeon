package id.naturalsmp.naturaldungeon.admin;

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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Interactive Setup Wizard — guides admin through dungeon creation
 * step-by-step.
 * Step 1: Name & World
 * Step 2: Difficulty setup
 * Step 3: Stage count
 * Step 4: Review & Create
 */
public class SetupWizardGUI implements Listener {

    private final NaturalDungeon plugin;
    private final Map<UUID, WizardSession> sessions = new HashMap<>();

    public SetupWizardGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    // ─── Open Wizard ────────────────────────────────────────────────

    public void open(Player player) {
        WizardSession session = sessions.computeIfAbsent(player.getUniqueId(), k -> new WizardSession());
        openStep(player, session);
    }

    private void openStep(Player player, WizardSession session) {
        switch (session.step) {
            case 1 -> openStep1(player, session);
            case 2 -> openStep2(player, session);
            case 3 -> openStep3(player, session);
            case 4 -> openStep4(player, session);
        }
    }

    // ─── Step 1: Name & World ───────────────────────────────────────

    private void openStep1(Player player, WizardSession session) {
        Inventory inv = GUIUtils.createGUI(new WizardHolder(1), 45,
                "&#FFD700🧙 ᴡɪᴢᴀʀᴅ — ꜱᴛᴇᴘ 1/4");

        GUIUtils.fillBorder(inv, Material.BLACK_STAINED_GLASS_PANE);
        GUIUtils.fillEmpty(inv, Material.GRAY_STAINED_GLASS_PANE);

        // Progress bar
        setProgress(inv, 1);

        // Name
        String nameDisplay = session.name != null ? "&#55FF55" + session.name : "&#FF5555Belum di-set";
        inv.setItem(20, GUIUtils.createItem(Material.NAME_TAG,
                "&#FFD700&l📝 ɴᴀᴍᴀ ᴅᴜɴɢᴇᴏɴ",
                GUIUtils.separator(),
                "&7Nama: " + nameDisplay,
                "",
                "&#FFAA00&l➥ KLIK untuk ketik di chat"));

        // World
        String worldDisplay = session.world != null ? "&#55FF55" + session.world
                : "&#55CCFF" + player.getWorld().getName();
        if (session.world == null)
            session.world = player.getWorld().getName();
        inv.setItem(22, GUIUtils.createItem(Material.GRASS_BLOCK,
                "&#55CCFF&l🌍 ᴡᴏʀʟᴅ",
                GUIUtils.separator(),
                "&7World: " + worldDisplay,
                "",
                "&#FFAA00&l➥ KLIK untuk ketik di chat"));

        // Max Players
        inv.setItem(24, GUIUtils.createItem(Material.PLAYER_HEAD,
                "&#FFAA00&l👥 ᴍᴀx ᴘʟᴀʏᴇʀꜱ: &f" + session.maxPlayers,
                GUIUtils.separator(),
                "&7L-Click: &f+1",
                "&7R-Click: &f-1",
                "",
                "&#FFAA00&l➥ KLIK"));

        // Navigation
        inv.setItem(36, GUIUtils.createItem(Material.BARRIER, "&#FF5555&l✕ ʙᴀᴛᴀʟ"));
        if (session.name != null) {
            inv.setItem(44, GUIUtils.createItem(Material.LIME_DYE, "&#55FF55&l➡ ʟᴀɴᴊᴜᴛ"));
        }

        player.openInventory(inv);
        GUIUtils.playOpenSound(player);
    }

    // ─── Step 2: Difficulty ─────────────────────────────────────────

    private void openStep2(Player player, WizardSession session) {
        Inventory inv = GUIUtils.createGUI(new WizardHolder(2), 45,
                "&#FFD700🧙 ᴡɪᴢᴀʀᴅ — ꜱᴛᴇᴘ 2/4");

        GUIUtils.fillBorder(inv, Material.BLACK_STAINED_GLASS_PANE);
        GUIUtils.fillEmpty(inv, Material.GRAY_STAINED_GLASS_PANE);
        setProgress(inv, 2);

        // Difficulty presets
        inv.setItem(20, createDiffToggle("&#55FF55&l🟢 ᴇᴀꜱʏ", Material.LIME_CONCRETE,
                session.easyEnabled, "5 lives, 0.8x mob, 1.0x loot"));
        inv.setItem(21, createDiffToggle("&#FFAA00&l🟡 ɴᴏʀᴍᴀʟ", Material.YELLOW_CONCRETE,
                session.normalEnabled, "3 lives, 1.0x mob, 1.0x loot"));
        inv.setItem(22, createDiffToggle("&#FF5555&l🔴 ʜᴀʀᴅ", Material.RED_CONCRETE,
                session.hardEnabled, "1 life, 1.5x mob, 1.5x loot"));
        inv.setItem(23, createDiffToggle("&#AA44FF&l🟣 ɴɪɢʜᴛᴍᴀʀᴇ", Material.PURPLE_CONCRETE,
                session.nightmareEnabled, "1 life, 3.0x mob, 3.0x loot"));

        inv.setItem(36, GUIUtils.createItem(Material.ARROW, "&#FF5555&l← ᴋᴇᴍʙᴀʟɪ"));
        inv.setItem(44, GUIUtils.createItem(Material.LIME_DYE, "&#55FF55&l➡ ʟᴀɴᴊᴜᴛ"));

        player.openInventory(inv);
        GUIUtils.playOpenSound(player);
    }

    private org.bukkit.inventory.ItemStack createDiffToggle(String name, Material mat, boolean enabled, String desc) {
        String status = enabled ? "&#55FF55✔ Aktif" : "&#FF5555✖ Nonaktif";
        return GUIUtils.createItem(mat, name,
                GUIUtils.separator(), "&7" + desc, "", status, "", "&#FFAA00&l➥ KLIK toggle");
    }

    // ─── Step 3: Stage Count ────────────────────────────────────────

    private void openStep3(Player player, WizardSession session) {
        Inventory inv = GUIUtils.createGUI(new WizardHolder(3), 45,
                "&#FFD700🧙 ᴡɪᴢᴀʀᴅ — ꜱᴛᴇᴘ 3/4");

        GUIUtils.fillBorder(inv, Material.BLACK_STAINED_GLASS_PANE);
        GUIUtils.fillEmpty(inv, Material.GRAY_STAINED_GLASS_PANE);
        setProgress(inv, 3);

        inv.setItem(22, GUIUtils.createItem(Material.LADDER,
                "&#FFAA00&l🏗 ᴊᴜᴍʟᴀʜ ꜱᴛᴀɢᴇ: &f" + session.stageCount,
                GUIUtils.separator(),
                "&7L-Click: &f+1",
                "&7R-Click: &f-1",
                "&7(Min: 1, Max: 10)",
                "",
                "&7Setiap stage akan dibuat dengan",
                "&7default ZOMBIE wave."));

        inv.setItem(36, GUIUtils.createItem(Material.ARROW, "&#FF5555&l← ᴋᴇᴍʙᴀʟɪ"));
        inv.setItem(44, GUIUtils.createItem(Material.LIME_DYE, "&#55FF55&l➡ ʟᴀɴᴊᴜᴛ"));

        player.openInventory(inv);
        GUIUtils.playOpenSound(player);
    }

    // ─── Step 4: Review & Create ────────────────────────────────────

    private void openStep4(Player player, WizardSession session) {
        Inventory inv = GUIUtils.createGUI(new WizardHolder(4), 45,
                "&#FFD700🧙 ᴡɪᴢᴀʀᴅ — ʀᴇᴠɪᴇᴡ");

        GUIUtils.fillBorder(inv, Material.BLACK_STAINED_GLASS_PANE);
        GUIUtils.fillEmpty(inv, Material.GRAY_STAINED_GLASS_PANE);
        setProgress(inv, 4);

        // Summary
        int diffCount = (session.easyEnabled ? 1 : 0) + (session.normalEnabled ? 1 : 0)
                + (session.hardEnabled ? 1 : 0) + (session.nightmareEnabled ? 1 : 0);

        inv.setItem(20, GUIUtils.createItem(Material.BOOK,
                "&#FFD700&l📋 ʀɪɴɢᴋᴀꜱᴀɴ",
                GUIUtils.separator(),
                "&7Nama: &#FFD700" + session.name,
                "&7World: &#55CCFF" + session.world,
                "&7Max Players: &f" + session.maxPlayers,
                "&7Difficulties: &f" + diffCount,
                "&7Stages: &f" + session.stageCount));

        inv.setItem(24, GUIUtils.createItem(Material.EMERALD_BLOCK,
                "&#55FF55&l✔ ʙᴜᴀᴛ ᴅᴜɴɢᴇᴏɴ",
                GUIUtils.separator(),
                "&7Klik untuk membuat dungeon.",
                "&7Config bisa diedit setelahnya.",
                "",
                "&#55FF55&l➥ KLIK UNTUK BUAT"));

        inv.setItem(36, GUIUtils.createItem(Material.ARROW, "&#FF5555&l← ᴋᴇᴍʙᴀʟɪ"));

        player.openInventory(inv);
        GUIUtils.playOpenSound(player);
    }

    // ─── Progress Bar ───────────────────────────────────────────────

    private void setProgress(Inventory inv, int currentStep) {
        for (int i = 1; i <= 4; i++) {
            Material mat = i < currentStep ? Material.LIME_STAINED_GLASS_PANE
                    : i == currentStep ? Material.YELLOW_STAINED_GLASS_PANE
                            : Material.WHITE_STAINED_GLASS_PANE;
            String stepName = switch (i) {
                case 1 -> "Info";
                case 2 -> "Difficulty";
                case 3 -> "Stages";
                case 4 -> "Review";
                default -> "";
            };
            String color = i <= currentStep ? "&#55FF55" : "&#555555";
            inv.setItem(i + 1, GUIUtils.createItem(mat, color + "● Step " + i, "&7" + stepName));
        }
    }

    // ─── Click Handler ──────────────────────────────────────────────

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof WizardHolder holder))
            return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getView().getTopInventory())
            return;
        if (e.getCurrentItem() == null)
            return;

        Player player = (Player) e.getWhoClicked();
        WizardSession session = sessions.computeIfAbsent(player.getUniqueId(), k -> new WizardSession());
        GUIUtils.playClickSound(player);

        int slot = e.getSlot();

        // Navigation
        if (slot == 36 && holder.step == 1) {
            // Cancel
            sessions.remove(player.getUniqueId());
            player.closeInventory();
            return;
        }
        if (slot == 36) {
            // Back
            session.step = holder.step - 1;
            openStep(player, session);
            return;
        }
        if (slot == 44) {
            // Next (except step 4)
            if (holder.step == 1 && session.name == null) {
                player.sendMessage(ChatUtils.colorize("&#FF5555✖ &7Set nama dungeon dulu!"));
                GUIUtils.playErrorSound(player);
                return;
            }
            session.step = holder.step + 1;
            openStep(player, session);
            return;
        }

        // Step-specific interactions
        switch (holder.step) {
            case 1 -> handleStep1Click(player, session, slot, e.isLeftClick());
            case 2 -> handleStep2Click(player, session, slot);
            case 3 -> handleStep3Click(player, session, slot, e.isLeftClick());
            case 4 -> handleStep4Click(player, session, slot);
        }
    }

    private void handleStep1Click(Player player, WizardSession session, int slot, boolean left) {
        switch (slot) {
            case 20 -> {
                // Set name
                player.closeInventory();
                plugin.getEditorChatInput().requestInput(player,
                        ChatUtils.colorize("&#FFD700&l📝 &7Ketik nama dungeon (ID, tanpa spasi):"), input -> {
                            String id = input.toLowerCase().replace(" ", "_");
                            session.name = id;
                            player.sendMessage(ChatUtils.colorize("&#55FF55✔ &7Nama: &f" + id));
                            GUIUtils.playSuccessSound(player);
                            openStep1(player, session);
                        });
            }
            case 22 -> {
                // Set world
                player.closeInventory();
                plugin.getEditorChatInput().requestInput(player,
                        ChatUtils.colorize("&#55CCFF&l🌍 &7Ketik nama world:"), input -> {
                            session.world = input;
                            player.sendMessage(ChatUtils.colorize("&#55FF55✔ &7World: &f" + input));
                            GUIUtils.playSuccessSound(player);
                            openStep1(player, session);
                        });
            }
            case 24 -> {
                // Max players +/-
                session.maxPlayers = Math.max(1, Math.min(24, session.maxPlayers + (left ? 1 : -1)));
                openStep1(player, session);
            }
        }
    }

    private void handleStep2Click(Player player, WizardSession session, int slot) {
        switch (slot) {
            case 20 -> session.easyEnabled = !session.easyEnabled;
            case 21 -> session.normalEnabled = !session.normalEnabled;
            case 22 -> session.hardEnabled = !session.hardEnabled;
            case 23 -> session.nightmareEnabled = !session.nightmareEnabled;
            default -> {
                return;
            }
        }
        openStep2(player, session);
    }

    private void handleStep3Click(Player player, WizardSession session, int slot, boolean left) {
        if (slot == 22) {
            session.stageCount = Math.max(1, Math.min(10, session.stageCount + (left ? 1 : -1)));
            openStep3(player, session);
        }
    }

    private void handleStep4Click(Player player, WizardSession session, int slot) {
        if (slot == 24) {
            // CREATE DUNGEON!
            createDungeon(player, session);
        }
    }

    // ─── Create Dungeon ─────────────────────────────────────────────

    private void createDungeon(Player player, WizardSession session) {
        player.closeInventory();
        String id = session.name;

        // Check if already exists
        if (plugin.getDungeonManager().getDungeon(id) != null) {
            player.sendMessage(ChatUtils.colorize("&#FF5555✖ &7Dungeon &f" + id + " &7sudah ada!"));
            GUIUtils.playErrorSound(player);
            return;
        }

        // Set basic info
        plugin.getDungeonManager().setDungeonConfig(id, "display-name", session.name);
        plugin.getDungeonManager().setDungeonConfig(id, "world", session.world);
        plugin.getDungeonManager().setDungeonConfig(id, "max-players", session.maxPlayers);
        plugin.getDungeonManager().setDungeonConfig(id, "cooldown", 1800);

        // Create difficulties
        if (session.easyEnabled) {
            plugin.getDungeonManager().setDungeonConfig(id, "difficulties.easy.display", "Easy");
            plugin.getDungeonManager().setDungeonConfig(id, "difficulties.easy.max-deaths", 5);
            plugin.getDungeonManager().setDungeonConfig(id, "difficulties.easy.reward-multiplier", 1.0);
            plugin.getDungeonManager().setDungeonConfig(id, "difficulties.easy.min-tier", 0);
            plugin.getDungeonManager().setDungeonConfig(id, "difficulties.easy.key-req", "none");
        }
        if (session.normalEnabled) {
            plugin.getDungeonManager().setDungeonConfig(id, "difficulties.normal.display", "Normal");
            plugin.getDungeonManager().setDungeonConfig(id, "difficulties.normal.max-deaths", 3);
            plugin.getDungeonManager().setDungeonConfig(id, "difficulties.normal.reward-multiplier", 1.0);
            plugin.getDungeonManager().setDungeonConfig(id, "difficulties.normal.min-tier", 1);
            plugin.getDungeonManager().setDungeonConfig(id, "difficulties.normal.key-req", "none");
        }
        if (session.hardEnabled) {
            plugin.getDungeonManager().setDungeonConfig(id, "difficulties.hard.display", "Hard");
            plugin.getDungeonManager().setDungeonConfig(id, "difficulties.hard.max-deaths", 1);
            plugin.getDungeonManager().setDungeonConfig(id, "difficulties.hard.reward-multiplier", 1.5);
            plugin.getDungeonManager().setDungeonConfig(id, "difficulties.hard.min-tier", 3);
            plugin.getDungeonManager().setDungeonConfig(id, "difficulties.hard.key-req", "none");
        }
        if (session.nightmareEnabled) {
            plugin.getDungeonManager().setDungeonConfig(id, "difficulties.nightmare.display", "Nightmare");
            plugin.getDungeonManager().setDungeonConfig(id, "difficulties.nightmare.max-deaths", 1);
            plugin.getDungeonManager().setDungeonConfig(id, "difficulties.nightmare.reward-multiplier", 3.0);
            plugin.getDungeonManager().setDungeonConfig(id, "difficulties.nightmare.min-tier", 5);
            plugin.getDungeonManager().setDungeonConfig(id, "difficulties.nightmare.key-req", "none");
        }
        // Fallback: at least one difficulty
        if (!session.easyEnabled && !session.normalEnabled && !session.hardEnabled && !session.nightmareEnabled) {
            plugin.getDungeonManager().setDungeonConfig(id, "difficulties.normal.display", "Normal");
            plugin.getDungeonManager().setDungeonConfig(id, "difficulties.normal.max-deaths", 3);
            plugin.getDungeonManager().setDungeonConfig(id, "difficulties.normal.reward-multiplier", 1.0);
            plugin.getDungeonManager().setDungeonConfig(id, "difficulties.normal.min-tier", 0);
            plugin.getDungeonManager().setDungeonConfig(id, "difficulties.normal.key-req", "none");
        }

        // Create stages with default waves
        for (int s = 1; s <= session.stageCount; s++) {
            plugin.getDungeonManager().setDungeonConfig(id, "stages." + s + ".waves.1.type", "VANILLA");
            plugin.getDungeonManager().setDungeonConfig(id, "stages." + s + ".waves.1.mob", "ZOMBIE");
            plugin.getDungeonManager().setDungeonConfig(id, "stages." + s + ".waves.1.count", 3 + (s * 2));
            // Auto-generate region names
            plugin.getDungeonManager().setDungeonConfig(id, "stages." + s + ".locations.1.safe-zone",
                    id + "_stage" + s + "_safe");
            plugin.getDungeonManager().setDungeonConfig(id, "stages." + s + ".locations.1.arena-region",
                    id + "_stage" + s + "_arena");
        }

        // Reload
        plugin.getDungeonManager().loadDungeons();
        sessions.remove(player.getUniqueId());

        // Success messages
        player.sendMessage(ChatUtils.colorize(""));
        player.sendMessage(ChatUtils.colorize("&#55FF55&l✔ Dungeon &f" + id + " &#55FF55&lberhasil dibuat!"));
        player.sendMessage(ChatUtils.colorize("&#FFAA00⚙ &7" + session.stageCount + " stages, default waves siap."));
        player.sendMessage(ChatUtils.colorize("&#55CCFF📐 &7Selanjutnya: set region dengan &f/nd wand"));
        player.sendMessage(ChatUtils.colorize("&#FFD700⚙ &7Atau edit via &f/nd editor"));
        player.sendMessage(ChatUtils.colorize(""));
        GUIUtils.playSuccessSound(player);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof WizardHolder)
            e.setCancelled(true);
    }

    // ─── Data Classes ───────────────────────────────────────────────

    public static class WizardHolder implements InventoryHolder {
        public final int step;

        public WizardHolder(int step) {
            this.step = step;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private static class WizardSession {
        int step = 1;
        String name;
        String world;
        int maxPlayers = 4;
        boolean easyEnabled = true;
        boolean normalEnabled = true;
        boolean hardEnabled = false;
        boolean nightmareEnabled = false;
        int stageCount = 3;
    }
}
