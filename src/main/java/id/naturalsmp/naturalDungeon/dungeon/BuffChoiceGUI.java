package id.naturalsmp.naturaldungeon.dungeon;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class BuffChoiceGUI implements Listener {

    private final NaturalDungeon plugin;
    private final DungeonInstance instance;
    private final NamespacedKey buffKey;

    public BuffChoiceGUI(NaturalDungeon plugin, DungeonInstance instance) {
        this.plugin = plugin;
        this.instance = instance;
        this.buffKey = new NamespacedKey(plugin, "buff_id");
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(new BuffHolder(), 27,
                ChatUtils.colorize("&#FFBB00&lPILIH BUFF DUNGEON"));

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);
        for (int i = 0; i < 27; i++)
            inv.setItem(i, filler);

        // Choice 1: Strength
        inv.setItem(11, createBuffItem(Material.RED_DYE, "&c&lSTRENGTH OF THE WARRIOR",
                "&7Efek Strength II selama 3 menit.", "STRENGTH"));

        // Choice 2: Regeneration
        inv.setItem(13, createBuffItem(Material.PINK_DYE, "&d&lHEART OF NATURE",
                "&7Efek Regeneration II selama 3 menit.", "REGENERATION"));

        // Choice 3: Speed
        inv.setItem(15, createBuffItem(Material.LIGHT_BLUE_DYE, "&b&lWIND'S BLESSING",
                "&7Efek Speed II selama 3 menit.", "SPEED"));

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof BuffHolder))
            return;
        e.setCancelled(true);

        ItemStack item = e.getCurrentItem();
        if (item == null || item.getType() == Material.GRAY_STAINED_GLASS_PANE)
            return;

        Player p = (Player) e.getWhoClicked();
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.getPersistentDataContainer().has(buffKey, PersistentDataType.STRING))
            return;

        String buffType = meta.getPersistentDataContainer().get(buffKey, PersistentDataType.STRING);

        applyBuff(p, buffType);
        p.closeInventory();
        p.sendMessage(ChatUtils.colorize("&aAnda telah memilih buff: " + item.getItemMeta().getDisplayName()));
    }

    private void applyBuff(Player player, String type) {
        switch (type) {
            case "STRENGTH" -> player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 180, 1));
            case "REGENERATION" -> player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 180, 1));
            case "SPEED" -> player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 180, 1));
        }
    }

    private ItemStack createBuffItem(Material mat, String name, String lore, String id) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.colorize(name));
        meta.setLore(Collections.singletonList(ChatUtils.colorize(lore)));
        meta.getPersistentDataContainer().set(buffKey, PersistentDataType.STRING, id);
        item.setItemMeta(meta);
        return item;
    }

    public static class BuffHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
