package id.naturalsmp.naturaldungeon.loot;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.dungeon.Dungeon;
import id.naturalsmp.naturaldungeon.dungeon.DungeonDifficulty;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
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
        Inventory inv = Bukkit.createInventory(new PreviewHolder(dungeon.getId()), 54,
                ChatUtils.colorize("&8Loot Preview: " + difficulty.getDisplayName()));

        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, filler);
            }
        }

        // Title Headers
        inv.setItem(4, createItem(Material.DIAMOND, "&#00FFFF&lLOOT PREVIEW", "&7Menampilkan kemungkinan drop",
                "&7dari " + dungeon.getDisplayName() + " - " + difficulty.getDisplayName()));

        ConfigurationSection lootSection = difficulty.getLootSection();
        if (lootSection == null) {
            inv.setItem(22, createItem(Material.BARRIER, "&cTidak ada loot yang dikonfigurasi."));
        } else {
            List<ItemStack> bossLoot = buildPreviewItems(lootSection, "boss");
            List<ItemStack> compLoot = buildPreviewItems(lootSection, "completion");

            int[] bossSlots = { 11, 12, 13, 14, 15, 20, 21, 22, 23, 24 };
            int[] compSlots = { 29, 30, 31, 32, 33, 38, 39, 40, 41, 42 };

            if (!bossLoot.isEmpty()) {
                inv.setItem(10, createItem(Material.WITHER_SKELETON_SKULL, "&#FF5555&lBoss Drops"));
                for (int i = 0; i < Math.min(bossLoot.size(), bossSlots.length); i++) {
                    inv.setItem(bossSlots[i], bossLoot.get(i));
                }
            }

            if (!compLoot.isEmpty()) {
                inv.setItem(28, createItem(Material.CHEST, "&#55FF55&lCompletion Drops"));
                for (int i = 0; i < Math.min(compLoot.size(), compSlots.length); i++) {
                    inv.setItem(compSlots[i], compLoot.get(i));
                }
            }
        }

        inv.setItem(49, createItem(Material.ARROW, "&cKembali"));
        player.openInventory(inv);
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
                lore.add(ChatUtils.colorize("&6Drop Info &8&m         "));
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
        if (e.getInventory().getHolder() instanceof PreviewHolder holder) {
            e.setCancelled(true);
            if (e.getCurrentItem() != null && e.getCurrentItem().getType() == Material.ARROW) {
                plugin.getDungeonGUI().open((Player) e.getWhoClicked()); // Go back to Dungeon selection for now
            }
        }
    }

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.colorize(name));
        List<String> loreList = new ArrayList<>();
        for (String l : lore)
            loreList.add(ChatUtils.colorize(l));
        meta.setLore(loreList);
        item.setItemMeta(meta);
        return item;
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
