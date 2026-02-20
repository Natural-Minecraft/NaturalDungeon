package id.naturalsmp.naturaldungeon.player;

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

import java.util.ArrayList;
import java.util.List;

public class AchievementGUI implements Listener {

    private final NaturalDungeon plugin;

    public AchievementGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(new AchievementHolder(), 45,
                ChatUtils.colorize("&8Dungeon Achievements"));

        // BG Filler
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 45; i++) {
            inv.setItem(i, filler);
        }

        AchievementManager manager = plugin.getAchievementManager();
        List<AchievementManager.Achievement> achievements = manager.getAchievements();

        int[] slots = { 10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25 };
        int slotIdx = 0;

        for (AchievementManager.Achievement ach : achievements) {
            if (slotIdx >= slots.length)
                break;

            boolean unlocked = manager.hasAchievement(player.getUniqueId(), ach.getId());
            Material mat = unlocked ? Material.NETHER_STAR : Material.MINECART;
            String color = unlocked ? "&#FFFF00&l" : "&8&l";
            String status = unlocked ? "&a✔ Sudah Terbuka" : "&c✘ Belum Terbuka";

            List<String> lore = new ArrayList<>();
            lore.add("&7" + ach.getDescription());
            lore.add("");
            lore.add("&fHadiah: &eRp " + ach.getRewardMoney());
            lore.add(status);

            inv.setItem(slots[slotIdx], createItem(mat, color + ach.getName(), lore.toArray(new String[0])));
            slotIdx++;
        }

        inv.setItem(40, createItem(Material.BARRIER, "&cTutup", "&7Klik untuk menutup."));

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getInventory().getHolder() instanceof AchievementHolder) {
            e.setCancelled(true);
            if (e.getCurrentItem() != null && e.getCurrentItem().getType() == Material.BARRIER) {
                e.getWhoClicked().closeInventory();
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

    private static class AchievementHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
