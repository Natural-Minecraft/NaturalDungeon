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
 * Configure a single wave: add mobs, set counts, set delay.
 */
public class WaveConfigGUI implements Listener {

    private final NaturalDungeon plugin;

    public WaveConfigGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String dungeonId, int stageIndex, int waveIndex) {
        Inventory inv = Bukkit.createInventory(
                new WaveConfigHolder(dungeonId, stageIndex, waveIndex), 27,
                ChatUtils.colorize("&6&lWAVE CONFIG: S" + (stageIndex + 1) + " W" + (waveIndex + 1)));
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++)
            inv.setItem(i, filler);

        inv.setItem(10, createItem(Material.ZOMBIE_HEAD, "&e&lTambah Mob",
                "&7Tambahkan mob ke wave ini",
                "&aKlik untuk masukkan mob ID"));
        inv.setItem(11, createItem(Material.REPEATER, "&e&lDelay (ticks)",
                "&7Set delay sebelum wave mulai",
                "&aKlik untuk set"));
        inv.setItem(12, createItem(Material.PAPER, "&e&lMob Count",
                "&7Jumlah mob spawn per wave",
                "&aKlik untuk set"));
        inv.setItem(14, createItem(Material.SPIDER_EYE, "&e&lMob Type",
                "&7VANILLA / MYTHICMOBS / CUSTOM",
                "&aKlik untuk set"));

        inv.setItem(22, createItem(Material.ARROW, "&cKembali"));
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof WaveConfigHolder holder))
            return;
        e.setCancelled(true);
        if (e.getCurrentItem() == null)
            return;
        Player player = (Player) e.getWhoClicked();
        String wavePath = "stages." + (holder.stageIndex + 1) + ".waves." + (holder.waveIndex + 1) + ".";

        switch (e.getSlot()) {
            case 10 -> plugin.getEditorChatInput().requestInput(player,
                    ChatUtils.colorize("&eMasukkan mob ID (misal: ZOMBIE, mythicmobs:SkeletonKing):"),
                    input -> {
                        plugin.getDungeonManager().addWaveMob(holder.dungeonId, holder.stageIndex, holder.waveIndex,
                                input);
                        player.sendMessage(ChatUtils.colorize("&aMob &f" + input + " &aditambahkan!"));
                        open(player, holder.dungeonId, holder.stageIndex, holder.waveIndex);
                    });
            case 11 -> plugin.getEditorChatInput().requestInput(player,
                    ChatUtils.colorize("&eMasukkan delay (ticks, 20 = 1 detik):"),
                    input -> {
                        try {
                            int v = Integer.parseInt(input);
                            plugin.getDungeonManager().setDungeonConfig(holder.dungeonId, wavePath + "delay", v);
                            player.sendMessage(ChatUtils.colorize("&aDelay: &f" + v + " ticks"));
                        } catch (NumberFormatException ex) {
                            player.sendMessage(ChatUtils.colorize("&cAngka tidak valid!"));
                        }
                        open(player, holder.dungeonId, holder.stageIndex, holder.waveIndex);
                    });
            case 12 -> plugin.getEditorChatInput().requestInput(player,
                    ChatUtils.colorize("&eMasukkan jumlah mob:"),
                    input -> {
                        try {
                            int v = Integer.parseInt(input);
                            plugin.getDungeonManager().setDungeonConfig(holder.dungeonId, wavePath + "count", v);
                            player.sendMessage(ChatUtils.colorize("&aCount: &f" + v));
                        } catch (NumberFormatException ex) {
                            player.sendMessage(ChatUtils.colorize("&cAngka tidak valid!"));
                        }
                        open(player, holder.dungeonId, holder.stageIndex, holder.waveIndex);
                    });
            case 14 -> plugin.getEditorChatInput().requestInput(player,
                    ChatUtils.colorize("&eMasukkan tipe mob (VANILLA/MYTHICMOBS/CUSTOM):"),
                    input -> {
                        plugin.getDungeonManager().setDungeonConfig(holder.dungeonId, wavePath + "type",
                                input.toUpperCase());
                        player.sendMessage(ChatUtils.colorize("&aTipe: &f" + input.toUpperCase()));
                        open(player, holder.dungeonId, holder.stageIndex, holder.waveIndex);
                    });
            case 22 -> new WaveEditorGUI(plugin).open(player, holder.dungeonId, holder.stageIndex);
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

    public static class WaveConfigHolder implements InventoryHolder {
        public final String dungeonId;
        public final int stageIndex, waveIndex;

        public WaveConfigHolder(String d, int s, int w) {
            this.dungeonId = d;
            this.stageIndex = s;
            this.waveIndex = w;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
