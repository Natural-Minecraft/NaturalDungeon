package id.naturalsmp.naturaldungeon.dungeon;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import id.naturalsmp.naturaldungeon.utils.ConfigUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class DungeonCompletionGUI implements Listener {

    private final NaturalDungeon plugin;

    public DungeonCompletionGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, DungeonInstance instance, boolean victory) {
        String title = ConfigUtils.getMessage("gui.completion.title");
        Inventory inv = Bukkit.createInventory(new CompletionHolder(), 27, title);

        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++)
            inv.setItem(i, filler);

        if (victory) {
            inv.setItem(4, createItem(Material.LIME_BANNER, "&#55FF55&l✔ DUNGEON SELESAI!",
                    "&7Selamat! Kamu berhasil menyelesaikan",
                    "&7dungeon &f" + instance.getDungeon().getDisplayName() + "&7!",
                    "",
                    "&aWaktu: &f" + ChatUtils.formatTime(instance.getDuration() / 1000),
                    "&aStages: &f" + instance.getDifficulty().getTotalStages(),
                    "&aDeaths: &f" + instance.getTotalDeaths(),
                    "",
                    "&#FFBB00&lRANK: &f&n" + instance.getPerformanceRank()));

            List<ItemStack> loot = instance.getCollectedLoot();
            int[] lootSlots = { 10, 11, 12, 13, 14, 15, 16 };
            for (int i = 0; i < lootSlots.length && i < loot.size(); i++) {
                inv.setItem(lootSlots[i], loot.get(i));
            }

            int baseXp = ConfigUtils.getInt("rewards.base-xp");
            int totalXp = baseXp * instance.getDifficulty().getTotalStages();
            String skill = ConfigUtils.getString("rewards.xp-skill");
            inv.setItem(21, createItem(Material.EXPERIENCE_BOTTLE, "&#00AAFF&l⬆ XP REWARD",
                    "&7Kamu mendapatkan:",
                    "&b+" + totalXp + " " + skill.toUpperCase() + " XP"));
        } else {
            inv.setItem(4, createItem(Material.RED_BANNER, "&#FF5555&l✘ DUNGEON GAGAL",
                    "&7Sayangnya kamu tidak berhasil",
                    "&7menyelesaikan dungeon ini.",
                    "",
                    "&cWaktu: &f" + ChatUtils.formatTime(instance.getDuration() / 1000),
                    "&cStage Terakhir: &f" + instance.getCurrentStage(),
                    "&cDeaths: &f" + instance.getTotalDeaths()));
        }

        inv.setItem(22, createItem(Material.BARRIER, "&#AAAAAA&l✕ TUTUP", "&7Klik untuk menutup dan kembali."));

        player.openInventory(inv);
        player.playSound(player.getLocation(), victory ? Sound.UI_TOAST_CHALLENGE_COMPLETE : Sound.ENTITY_VILLAGER_NO,
                1f, 1f);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof CompletionHolder))
            return;
        e.setCancelled(true);
        if (e.getCurrentItem() != null && e.getCurrentItem().getType() == Material.BARRIER) {
            e.getWhoClicked().closeInventory();
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

    public static class CompletionHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
