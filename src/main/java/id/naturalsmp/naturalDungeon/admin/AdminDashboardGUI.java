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
import java.util.Collection;
import java.util.List;

/**
 * Admin Dashboard — overview of all dungeons, active instances, quick actions.
 * Accessed via /nd admin or /nd dashboard.
 */
public class AdminDashboardGUI implements Listener {

    private final NaturalDungeon plugin;

    public AdminDashboardGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = GUIUtils.createGUI(new DashboardHolder(), 54,
                "&#FFD700⚙ ᴀᴅᴍɪɴ ᴅᴀꜱʜʙᴏᴀʀᴅ");

        GUIUtils.fillBorder(inv, Material.BLACK_STAINED_GLASS_PANE);
        GUIUtils.fillEmpty(inv, Material.GRAY_STAINED_GLASS_PANE);

        Collection<Dungeon> dungeons = plugin.getDungeonManager().getDungeons();
        int activeInstances = plugin.getDungeonManager().getActiveInstances().size();
        int totalPlayers = plugin.getServer().getOnlinePlayers().size();

        // ─── Overview Panel (top row) ───

        // Plugin Info (slot 4)
        inv.setItem(4, GUIUtils.createItem(Material.COMMAND_BLOCK,
                "&#FFD700&l⚙ NaturalDungeon",
                GUIUtils.separator(),
                "&7Version: &f" + plugin.getDescription().getVersion(),
                "&7Dungeons: &#55FF55" + dungeons.size(),
                "&7Active: &#FFAA00" + activeInstances,
                "&7Online: &#55CCFF" + totalPlayers));

        // Quick Stats (slot 2)
        inv.setItem(2, GUIUtils.createItem(Material.CLOCK,
                "&#55CCFF&l📊 ꜱᴛᴀᴛɪꜱᴛɪᴄꜱ",
                GUIUtils.separator(),
                "&7Active Instances: &#FFAA00" + activeInstances,
                "&7Total Dungeons: &f" + dungeons.size(),
                "&7Online Players: &#55FF55" + totalPlayers));

        // Reload (slot 6)
        inv.setItem(6, GUIUtils.createItem(Material.REDSTONE,
                "&#FF5555&l🔄 ʀᴇʟᴏᴀᴅ",
                GUIUtils.separator(),
                "&7Reload semua dungeon config.",
                "",
                "&#FFAA00&l➥ KLIK"));

        // ─── Dungeon List (slots 19-34) ───
        int[] slots = { 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34 };
        int idx = 0;
        for (Dungeon dungeon : dungeons) {
            if (idx >= slots.length)
                break;

            boolean valid = DungeonValidator.isValid(plugin, dungeon);
            List<String> errors = valid ? List.of() : DungeonValidator.validate(plugin, dungeon);
            int stageCount = dungeon.getStages().size();
            int diffCount = dungeon.getDifficulties().size();

            // Check if this dungeon has active instances
            long active = plugin.getDungeonManager().getActiveInstances().stream()
                    .filter(i -> i.getDungeon().getId().equals(dungeon.getId()))
                    .count();

            List<String> lore = new ArrayList<>();
            lore.add(GUIUtils.separator());
            lore.add("&7Status: " + (valid ? "&#55FF55✔ Valid" : "&#FF5555✖ " + errors.size() + " issues"));
            lore.add("&7Stages: &f" + stageCount);
            lore.add("&7Difficulties: &f" + diffCount);
            if (active > 0) {
                lore.add("&#FFAA00▶ " + active + " active instance" + (active > 1 ? "s" : ""));
            }
            lore.add("");
            lore.add("&#FFAA00&l➥ KLIK untuk edit");

            Material icon = Material.CHEST;

            String color = valid ? "&#55FF55" : "&#FF5555";
            inv.setItem(slots[idx], GUIUtils.createItem(icon,
                    color + "&l" + dungeon.getDisplayName(), lore));
            idx++;
        }

        // ─── Bottom Actions ───

        // Open Editor List (slot 47)
        inv.setItem(47, GUIUtils.createItem(Material.WRITABLE_BOOK,
                "&#55CCFF&l📋 ᴇᴅɪᴛᴏʀ ʟɪꜱᴛ",
                GUIUtils.separator(),
                "&7Buka daftar dungeon editor.",
                "",
                "&#FFAA00&l➥ KLIK"));

        // Setup Tools (slot 49)
        inv.setItem(49, GUIUtils.createItem(Material.GOLDEN_AXE,
                "&#FFD700&l🔧 ꜱᴇᴛᴜᴘ ᴛᴏᴏʟꜱ",
                GUIUtils.separator(),
                "&7Dapatkan wand tools.",
                "",
                "&#FFAA00&l➥ KLIK"));

        // Close (slot 53)
        inv.setItem(53, GUIUtils.createItem(Material.BARRIER,
                "&#FF5555&l✕ ᴛᴜᴛᴜᴘ"));

        player.openInventory(inv);
        GUIUtils.playOpenSound(player);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof DashboardHolder))
            return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getView().getTopInventory())
            return;
        if (e.getCurrentItem() == null)
            return;

        Player player = (Player) e.getWhoClicked();
        GUIUtils.playClickSound(player);

        switch (e.getSlot()) {
            case 6 -> {
                // Reload
                plugin.getDungeonManager().loadDungeons();
                player.sendMessage(ChatUtils.colorize("&#55FF55✔ &7Dungeon configs reloaded!"));
                GUIUtils.playSuccessSound(player);
                open(player); // Refresh
            }
            case 47 -> new id.naturalsmp.naturaldungeon.editor.DungeonListEditorGUI(plugin).open(player);
            case 49 -> {
                player.closeInventory();
                player.sendMessage(ChatUtils.colorize("&#FFD700&l⚙ &7Gunakan &f/nd wand &7untuk setup tools."));
            }
            case 53 -> player.closeInventory();
            default -> {
                // Check if clicked a dungeon slot
                int[] slots = { 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34 };
                int clickedIdx = -1;
                for (int i = 0; i < slots.length; i++) {
                    if (slots[i] == e.getSlot()) {
                        clickedIdx = i;
                        break;
                    }
                }
                if (clickedIdx >= 0) {
                    Collection<Dungeon> dungeons = plugin.getDungeonManager().getDungeons();
                    int idx = 0;
                    for (Dungeon dungeon : dungeons) {
                        if (idx == clickedIdx) {
                            new id.naturalsmp.naturaldungeon.editor.DungeonMainEditorGUI(plugin)
                                    .open(player, dungeon.getId());
                            return;
                        }
                        idx++;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof DashboardHolder)
            e.setCancelled(true);
    }

    public static class DashboardHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
