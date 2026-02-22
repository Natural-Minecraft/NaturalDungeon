package id.naturalsmp.naturaldungeon.loot;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.dungeon.Dungeon;
import id.naturalsmp.naturaldungeon.dungeon.DungeonDifficulty;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import id.naturalsmp.naturaldungeon.utils.GUIUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class LootPreviewGUI implements Listener {

    private final NaturalDungeon plugin;

    public LootPreviewGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, Dungeon dungeon, DungeonDifficulty difficulty) {
        Inventory inv = GUIUtils.createGUI(new PreviewHolder(dungeon.getId()), 54,
                "&#FFD700🎁 ʟᴏᴏᴛ ᴘʀᴇᴠɪᴇᴡ: &f" + difficulty.getDisplayName());

        GUIUtils.fillBorder(inv, Material.BLACK_STAINED_GLASS_PANE);
        GUIUtils.fillEmpty(inv, Material.GRAY_STAINED_GLASS_PANE);

        // Title Header
        inv.setItem(4, GUIUtils.createItem(Material.DIAMOND,
                "&#55CCFF&lʟᴏᴏᴛ ᴘʀᴇᴠɪᴇᴡ",
                GUIUtils.separator(),
                "&7Kemungkinan drop dari",
                "&f" + dungeon.getDisplayName() + " &7- &f" + difficulty.getDisplayName()));

        ConfigurationSection lootSection = difficulty.getLootSection();
        if (lootSection == null) {
            inv.setItem(22, GUIUtils.createItem(Material.BARRIER,
                    "&#FF5555&l✖ Tidak ada loot."));
        } else {
            List<ItemStack> bossLoot = buildPreviewItems(lootSection, "boss");
            List<ItemStack> compLoot = buildPreviewItems(lootSection, "completion");

            int[] bossSlots = { 11, 12, 13, 14, 15, 20, 21, 22, 23, 24 };
            int[] compSlots = { 29, 30, 31, 32, 33, 38, 39, 40, 41, 42 };

            if (!bossLoot.isEmpty()) {
                inv.setItem(10, GUIUtils.createItem(Material.WITHER_SKELETON_SKULL,
                        "&#FF5555&l🐉 ʙᴏꜱꜱ ᴅʀᴏᴘꜱ"));
                for (int i = 0; i < Math.min(bossLoot.size(), bossSlots.length); i++) {
                    inv.setItem(bossSlots[i], bossLoot.get(i));
                }
            }

            if (!compLoot.isEmpty()) {
                inv.setItem(28, GUIUtils.createItem(Material.CHEST,
                        "&#55FF55&l🎁 ᴄᴏᴍᴘʟᴇᴛɪᴏɴ ᴅʀᴏᴘꜱ"));
                for (int i = 0; i < Math.min(compLoot.size(), compSlots.length); i++) {
                    inv.setItem(compSlots[i], compLoot.get(i));
                }
            }
        }

        inv.setItem(49, GUIUtils.createItem(Material.ARROW, "&#FF5555&l← ᴋᴇᴍʙᴀʟɪ"));

        player.openInventory(inv);
        GUIUtils.playOpenSound(player);
    }

    private List<ItemStack> buildPreviewItems(ConfigurationSection section, String path) {
        List<ItemStack> items = new ArrayList<>();
        List<?> list = section.getList(path);
        if (list == null)
            return items;

        for (int i = 0; i < list.size(); i++) {
            String fullPath = path + "." + i;
            String type = section.getString(fullPath + ".type", "VANILLA");
            int chance = section.getInt(fullPath + ".chance", 100);
            String amountStr = section.getString(fullPath + ".amount", "1");

            ItemStack baseItem = null;
            String itemName = "";

            switch (type.toUpperCase()) {
                case "MMOITEMS" -> {
                    if (plugin.hasMMOItems()) {
                        String id = section.getString(fullPath + ".id", "");
                        String[] parts = id.split(":");
                        if (parts.length == 2) {
                            baseItem = plugin.getMmoItemsHook().getItem(parts[0], parts[1]);
                        }
                    }
                }
                case "VANILLA" -> {
                    String materialStr = section.getString(fullPath + ".material", "DIAMOND");
                    try {
                        Material mat = Material.valueOf(materialStr.toUpperCase());
                        baseItem = new ItemStack(mat);
                        itemName = mat.name();
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                case "COMMAND" -> {
                    baseItem = new ItemStack(Material.PAPER);
                    itemName = section.getString(fullPath + ".command", "Command Execution");
                }
            }

            if (baseItem == null) {
                baseItem = new ItemStack(Material.STRUCTURE_VOID);
            }

            ItemMeta meta = baseItem.getItemMeta();
            if (meta != null) {
                if (type.equalsIgnoreCase("COMMAND") || baseItem.getType() == Material.STRUCTURE_VOID) {
                    meta.setDisplayName(ChatUtils.colorize("&f" + itemName));
                }
                List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                lore.add("");
                lore.add(ChatUtils.colorize(GUIUtils.separator()));
                lore.add(ChatUtils.colorize("&7Amount: &f" + amountStr));
                lore.add(ChatUtils.colorize("&7Chance: &f" + chance + "%"));
                meta.setLore(lore);
                baseItem.setItemMeta(meta);
            }

            items.add(baseItem);
        }

        return items;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof PreviewHolder))
            return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getView().getTopInventory())
            return;
        if (e.getCurrentItem() != null && e.getCurrentItem().getType() == Material.ARROW) {
            GUIUtils.playClickSound((Player) e.getWhoClicked());
            plugin.getDungeonGUI().open((Player) e.getWhoClicked());
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof PreviewHolder)
            e.setCancelled(true);
    }

    private static class PreviewHolder implements InventoryHolder {
        private final String dungeonId;

        public PreviewHolder(String dungeonId) {
            this.dungeonId = dungeonId;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }

        public String getDungeonId() {
            return dungeonId;
        }
    }
}
