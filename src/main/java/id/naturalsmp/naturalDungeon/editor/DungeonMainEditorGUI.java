package id.naturalsmp.naturaldungeon.editor;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Main dashboard for editing a single dungeon.
 */
public class DungeonMainEditorGUI implements Listener {

    private final NaturalDungeon plugin;

    public DungeonMainEditorGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String dungeonId) {
        Inventory inv = Bukkit.createInventory(new MainHolder(dungeonId), 27,
                ChatUtils.colorize("&6&lEDIT: &e" + dungeonId));

        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++)
            inv.setItem(i, filler);

        inv.setItem(10, createItem(Material.NAME_TAG, "&b&lBasic Info",
                "&7Nama, deskripsi, world, max player", "&7cooldown, icon material"));
        inv.setItem(11, createItem(Material.IRON_SWORD, "&c&lDifficulties",
                "&7Kelola difficulty dungeon"));
        inv.setItem(12, createItem(Material.LADDER, "&e&lStages",
                "&7Kelola stage per difficulty"));
        inv.setItem(14, createItem(Material.CHEST, "&a&lRewards",
                "&7Edit loot table"));
        inv.setItem(15, createItem(Material.ZOMBIE_HEAD, "&d&lMobs",
                "&7Kelola custom mobs"));

        inv.setItem(13, createItem(Material.WITHER_SKELETON_SKULL, "&4&lBoss Editor",
                "&7Complex Editor & All Skills"));

        inv.setItem(22, createItem(Material.ARROW, "&cKembali"));

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof MainHolder holder))
            return;
        e.setCancelled(true);
        if (e.getCurrentItem() == null)
            return;
        Player player = (Player) e.getWhoClicked();
        String dungeonId = holder.dungeonId;

        switch (e.getSlot()) {
            case 10 -> new BasicInfoEditorGUI(plugin).open(player, dungeonId);
            case 11 -> new DifficultyEditorGUI(plugin).open(player, dungeonId);
            case 12 -> new StageEditorGUI(plugin).open(player, dungeonId);
            case 14 -> new RewardEditorGUI(plugin).open(player, dungeonId);
            case 15 -> new MobEditorGUI(plugin).open(player, dungeonId);
            case 13 -> new BossEditorGUI(plugin).open(player, dungeonId);
            case 22 -> new DungeonListEditorGUI(plugin).open(player);
        }
    }

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.colorize(name));
        List<String> list = new ArrayList<>();
        for (String l : lore)
            list.add(ChatUtils.colorize(l));
        meta.setLore(list);
        item.setItemMeta(meta);
        return item;
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
