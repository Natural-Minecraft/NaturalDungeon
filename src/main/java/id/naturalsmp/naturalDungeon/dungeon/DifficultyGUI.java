package id.naturalsmp.naturaldungeon.dungeon;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.party.PartyConfirmationGUI;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import id.naturalsmp.naturaldungeon.utils.ConfigUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class DifficultyGUI implements Listener {

    private final NaturalDungeon plugin;
    private final NamespacedKey diffKey;

    public DifficultyGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
        this.diffKey = new NamespacedKey(plugin, "difficulty_id");
    }

    public void open(Player player, String dungeonId) {
        Dungeon dungeon = plugin.getDungeonManager().getDungeon(dungeonId);
        if (dungeon == null)
            return;

        String title = ConfigUtils.getMessage("gui.difficulty.title");
        Inventory inv = Bukkit.createInventory(new DifficultyHolder(dungeonId), 27, title);

        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++)
            inv.setItem(i, filler);

        int[] slots = { 11, 13, 15 }; // Normal, Hard, Hell (example)
        int idx = 0;

        for (DungeonDifficulty diff : dungeon.getDifficulties()) {
            if (idx >= slots.length)
                break;

            String keyDisplay = diff.getKeyReq() != null ? diff.getKeyReq() : "None";
            String tierDisplay = String.valueOf(diff.getMinTier());

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatUtils.colorize("&7Min Tier: &f" + tierDisplay));
            lore.add(ChatUtils.colorize("&7Key Required: &f" + keyDisplay));
            lore.add(ChatUtils.colorize("&7Max Deaths: &c" + diff.getMaxDeaths()));
            lore.add(ChatUtils.colorize("&7Stages: &f" + dungeon.getTotalStages()));
            lore.add("");
            lore.add(ChatUtils.colorize("&eKiri-Klik untuk memilih!"));
            lore.add(ChatUtils.colorize("&bKanan-Klik untuk preview Loot!"));

            Material mat = Material.matchMaterial(diff.getId().toUpperCase() + "_BANNER");
            if (mat == null)
                mat = Material.PAPER;

            ItemStack item = createItem(mat, diff.getDisplayName(), lore.toArray(new String[0]));
            ItemMeta meta = item.getItemMeta();
            meta.getPersistentDataContainer().set(diffKey, PersistentDataType.STRING, diff.getId());
            item.setItemMeta(meta);

            inv.setItem(slots[idx++], item);
        }

        inv.setItem(22, createItem(Material.ARROW, "&cKembali"));
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof DifficultyHolder holder))
            return;
        e.setCancelled(true);
        if (e.getCurrentItem() == null)
            return;

        Player player = (Player) e.getWhoClicked();
        if (e.getCurrentItem().getType() == Material.ARROW) {
            plugin.getDungeonManager().openDungeonGUI(player);
            return;
        }

        ItemMeta meta = e.getCurrentItem().getItemMeta();
        if (meta == null || !meta.getPersistentDataContainer().has(diffKey, PersistentDataType.STRING))
            return;

        String diffId = meta.getPersistentDataContainer().get(diffKey, PersistentDataType.STRING);
        Dungeon dungeon = plugin.getDungeonManager().getDungeon(holder.dungeonId);

        if (dungeon != null) {
            DungeonDifficulty diff = dungeon.getDifficulty(diffId);
            if (diff != null) {
                if (e.isRightClick()) {
                    // Open Loot Preview
                    new id.naturalsmp.naturaldungeon.loot.LootPreviewGUI(plugin).open(player, dungeon, diff);
                    return;
                }

                // Validate Requirements
                if (!checkRequirements(player, diff)) {
                    player.closeInventory();
                    return;
                }

                // Open Confirmation
                new PartyConfirmationGUI(plugin).startConfirmation(player, dungeon, diff);
            }
        }
    }

    private boolean checkRequirements(Player player, DungeonDifficulty diff) {
        // 1. Tier Check using Hook
        if (plugin.getNaturalCoreHook().isEnabled()) {
            int playerTier = plugin.getNaturalCoreHook().getPlayerTier(player);
            if (playerTier < diff.getMinTier()) {
                player.sendMessage(
                        ConfigUtils.getMessage("dungeon.tier-too-low", "%tier%", String.valueOf(diff.getMinTier())));
                return false;
            }
        }

        // 2. Key Check (Leader Only)
        // This is a visual check, actual consumption happens on start
        String keyReq = diff.getKeyReq();
        if (keyReq != null && !keyReq.isEmpty() && !keyReq.equalsIgnoreCase("none")) {
            // Check logic will be in ConfirmationGUI to handle consumption
            // Here we just warn if missing? Or let confirmation handle it?
            // Let's assume confirmation handles it for now.
        }
        return true;
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

    public static class DifficultyHolder implements InventoryHolder {
        public final String dungeonId;

        public DifficultyHolder(String dungeonId) {
            this.dungeonId = dungeonId;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
