package id.naturalsmp.naturaldungeon.player;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
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

public class AchievementGUI implements Listener {

    private final NaturalDungeon plugin;

    public AchievementGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = GUIUtils.createGUI(new AchievementHolder(), 45,
                "&#FFD700🏆 ᴅᴜɴɢᴇᴏɴ ᴀᴄʜɪᴇᴠᴇᴍᴇɴᴛꜱ");

        GUIUtils.fillBorder(inv, Material.BLACK_STAINED_GLASS_PANE);
        GUIUtils.fillEmpty(inv, Material.GRAY_STAINED_GLASS_PANE);

        AchievementManager manager = plugin.getAchievementManager();
        List<AchievementManager.Achievement> achievements = manager.getAchievements();

        int[] slots = { 10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25 };
        int slotIdx = 0;

        for (AchievementManager.Achievement ach : achievements) {
            if (slotIdx >= slots.length)
                break;

            boolean unlocked = manager.hasAchievement(player.getUniqueId(), ach.getId());
            Material mat = unlocked ? Material.NETHER_STAR : Material.MINECART;
            String color = unlocked ? "&#FFD700" : "&#555555";
            String status = unlocked ? "&#55FF55✔ Sudah Terbuka" : "&#FF5555✘ Belum Terbuka";

            List<String> lore = new ArrayList<>();
            lore.add(GUIUtils.separator());
            lore.add("&7" + ach.getDescription());
            lore.add("");
            lore.add("&fHadiah: &#55FF55Rp " + ach.getRewardMoney());
            lore.add(status);

            inv.setItem(slots[slotIdx], GUIUtils.createItem(mat,
                    color + "&l" + ach.getName(), lore));
            slotIdx++;
        }

        inv.setItem(40, GUIUtils.createItem(Material.BARRIER,
                "&#FF5555&l✕ ᴛᴜᴛᴜᴘ"));

        player.openInventory(inv);
        GUIUtils.playOpenSound(player);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof AchievementHolder))
            return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getView().getTopInventory())
            return;
        if (e.getCurrentItem() != null && e.getCurrentItem().getType() == Material.BARRIER) {
            GUIUtils.playClickSound((Player) e.getWhoClicked());
            e.getWhoClicked().closeInventory();
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof AchievementHolder)
            e.setCancelled(true);
    }

    private static class AchievementHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
