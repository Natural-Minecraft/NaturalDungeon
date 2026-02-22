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

/**
 * Template/Preset system — quick dungeon creation from pre-configured
 * templates.
 * Per vision_admin_experience.md §10
 */
public class TemplatePresetGUI implements Listener {

    private final NaturalDungeon plugin;

    public TemplatePresetGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = GUIUtils.createGUI(new TemplateHolder(), 36,
                "&#FFD700📦 ᴘɪʟɪʜ ᴛᴇᴍᴘʟᴀᴛᴇ");

        GUIUtils.fillBorder(inv, Material.BLACK_STAINED_GLASS_PANE);
        GUIUtils.fillEmpty(inv, Material.GRAY_STAINED_GLASS_PANE);

        // Forest Dungeon (Easy)
        inv.setItem(10, GUIUtils.createItem(Material.OAK_SAPLING,
                "&#55FF55&l🌲 Forest Dungeon",
                GUIUtils.separator(),
                "&73 stages, vanilla mobs, simple boss",
                "&7Difficulty: Easy → Normal",
                "&7Cocok untuk server baru",
                "",
                "&#FFAA00&l➥ KLIK"));

        // Nether Fortress (Medium)
        inv.setItem(12, GUIUtils.createItem(Material.NETHER_BRICK,
                "&#FF5555&l🔥 Nether Fortress",
                GUIUtils.separator(),
                "&74 stages, nether mobs, multi-phase boss",
                "&7Difficulty: Normal → Hard",
                "&7Boss: Blaze King",
                "",
                "&#FFAA00&l➥ KLIK"));

        // Void Temple (Hard)
        inv.setItem(14, GUIUtils.createItem(Material.END_STONE,
                "&#AA44FF&l🌑 Void Temple",
                GUIUtils.separator(),
                "&75 stages, end mobs, complex boss",
                "&7Difficulty: Hard → Nightmare",
                "&7Boss: Void Guardian",
                "",
                "&#FFAA00&l➥ KLIK"));

        // Blank
        inv.setItem(16, GUIUtils.createItem(Material.PAPER,
                "&#FFFFFF&l📄 Blank",
                GUIUtils.separator(),
                "&7Mulai dari nol.",
                "&71 stage, 1 wave, no boss.",
                "",
                "&#FFAA00&l➥ KLIK"));

        // Back
        inv.setItem(31, GUIUtils.createItem(Material.ARROW, "&#FF5555&l← ᴋᴇᴍʙᴀʟɪ"));

        player.openInventory(inv);
        GUIUtils.playOpenSound(player);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof TemplateHolder))
            return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getView().getTopInventory())
            return;
        if (e.getCurrentItem() == null)
            return;

        Player player = (Player) e.getWhoClicked();
        GUIUtils.playClickSound(player);

        switch (e.getSlot()) {
            case 10 -> createFromTemplate(player, "forest");
            case 12 -> createFromTemplate(player, "nether");
            case 14 -> createFromTemplate(player, "void");
            case 16 -> createFromTemplate(player, "blank");
            case 31 -> player.closeInventory();
        }
    }

    private void createFromTemplate(Player player, String template) {
        player.closeInventory();
        plugin.getEditorChatInput().requestInput(player,
                ChatUtils.colorize("&#FFD700&l📦 &7Ketik ID untuk dungeon baru (tanpa spasi):"), input -> {
                    String id = input.toLowerCase().replace(" ", "_");

                    if (plugin.getDungeonManager().getDungeon(id) != null) {
                        player.sendMessage(ChatUtils.colorize("&#FF5555✖ &7Dungeon &f" + id + " &7sudah ada!"));
                        GUIUtils.playErrorSound(player);
                        return;
                    }

                    // Basic info
                    plugin.getDungeonManager().setDungeonConfig(id, "display-name", id);
                    plugin.getDungeonManager().setDungeonConfig(id, "world", player.getWorld().getName());
                    plugin.getDungeonManager().setDungeonConfig(id, "max-players", 4);
                    plugin.getDungeonManager().setDungeonConfig(id, "cooldown", 1800);

                    switch (template) {
                        case "forest" -> applyForestTemplate(id);
                        case "nether" -> applyNetherTemplate(id);
                        case "void" -> applyVoidTemplate(id);
                        case "blank" -> applyBlankTemplate(id);
                    }

                    plugin.getDungeonManager().loadDungeons();
                    player.sendMessage(ChatUtils.colorize(""));
                    player.sendMessage(ChatUtils.colorize("&#55FF55&l✔ Dungeon &f" + id +
                            " &#55FF55&lberhasil dibuat dari template &f" + template + "&l!"));
                    player.sendMessage(
                            ChatUtils.colorize("&#55CCFF📐 &7Gunakan &f/nd wand " + id + " 1 &7untuk set region"));
                    player.sendMessage(ChatUtils.colorize("&#FFD700⚙ &7Atau edit via &f/nd editor"));
                    player.sendMessage(ChatUtils.colorize(""));
                    GUIUtils.playSuccessSound(player);
                });
    }

    private void applyForestTemplate(String id) {
        // 2 difficulties
        setDiff(id, "easy", "Easy", 5, 1.0, 0, "none");
        setDiff(id, "normal", "Normal", 3, 1.0, 1, "none");
        // 3 stages with zombie/spider/skeleton waves
        setWave(id, 1, 1, "ZOMBIE", 5);
        setWave(id, 1, 2, "SPIDER", 4);
        setWave(id, 2, 1, "SKELETON", 6);
        setWave(id, 2, 2, "ZOMBIE", 8);
        setWave(id, 3, 1, "CAVE_SPIDER", 5);
        setWave(id, 3, 2, "WITCH", 3);
        // Auto region names
        for (int s = 1; s <= 3; s++) {
            setRegion(id, s);
        }
    }

    private void applyNetherTemplate(String id) {
        setDiff(id, "normal", "Normal", 3, 1.0, 1, "none");
        setDiff(id, "hard", "Hard", 1, 1.5, 3, "none");
        // 4 stages
        setWave(id, 1, 1, "BLAZE", 4);
        setWave(id, 1, 2, "WITHER_SKELETON", 3);
        setWave(id, 2, 1, "PIGLIN_BRUTE", 5);
        setWave(id, 2, 2, "HOGLIN", 4);
        setWave(id, 3, 1, "MAGMA_CUBE", 8);
        setWave(id, 3, 2, "GHAST", 2);
        setWave(id, 4, 1, "WITHER_SKELETON", 6);
        setWave(id, 4, 2, "BLAZE", 5);
        for (int s = 1; s <= 4; s++) {
            setRegion(id, s);
        }
    }

    private void applyVoidTemplate(String id) {
        setDiff(id, "hard", "Hard", 1, 1.5, 3, "none");
        setDiff(id, "nightmare", "Nightmare", 1, 3.0, 5, "none");
        // 5 stages
        setWave(id, 1, 1, "ENDERMAN", 3);
        setWave(id, 1, 2, "SHULKER", 4);
        setWave(id, 2, 1, "PHANTOM", 6);
        setWave(id, 2, 2, "VEX", 5);
        setWave(id, 3, 1, "ENDERMAN", 8);
        setWave(id, 3, 2, "SILVERFISH", 10);
        setWave(id, 4, 1, "SHULKER", 6);
        setWave(id, 4, 2, "GUARDIAN", 4);
        setWave(id, 5, 1, "WARDEN", 1);
        for (int s = 1; s <= 5; s++) {
            setRegion(id, s);
        }
    }

    private void applyBlankTemplate(String id) {
        setDiff(id, "normal", "Normal", 3, 1.0, 0, "none");
        setWave(id, 1, 1, "ZOMBIE", 3);
        setRegion(id, 1);
    }

    private void setDiff(String id, String diffId, String display, int maxDeaths,
            double reward, int tier, String key) {
        String base = "difficulties." + diffId + ".";
        plugin.getDungeonManager().setDungeonConfig(id, base + "display", display);
        plugin.getDungeonManager().setDungeonConfig(id, base + "max-deaths", maxDeaths);
        plugin.getDungeonManager().setDungeonConfig(id, base + "reward-multiplier", reward);
        plugin.getDungeonManager().setDungeonConfig(id, base + "min-tier", tier);
        plugin.getDungeonManager().setDungeonConfig(id, base + "key-req", key);
    }

    private void setWave(String id, int stage, int wave, String mob, int count) {
        String base = "stages." + stage + ".waves." + wave + ".";
        plugin.getDungeonManager().setDungeonConfig(id, base + "type", "VANILLA");
        plugin.getDungeonManager().setDungeonConfig(id, base + "mob", mob);
        plugin.getDungeonManager().setDungeonConfig(id, base + "count", count);
    }

    private void setRegion(String id, int stage) {
        plugin.getDungeonManager().setDungeonConfig(id,
                "stages." + stage + ".locations.1.safe-zone", id + "_stage" + stage + "_safe");
        plugin.getDungeonManager().setDungeonConfig(id,
                "stages." + stage + ".locations.1.arena-region", id + "_stage" + stage + "_arena");
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof TemplateHolder)
            e.setCancelled(true);
    }

    public static class TemplateHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
