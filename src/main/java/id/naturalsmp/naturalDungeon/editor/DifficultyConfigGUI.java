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
 * Configure a single difficulty: name, min-tier, key-req, max-deaths,
 * reward-multiplier.
 */
public class DifficultyConfigGUI implements Listener {

    private final NaturalDungeon plugin;

    public DifficultyConfigGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String dungeonId, String difficultyId) {
        Inventory inv = Bukkit.createInventory(new DiffConfigHolder(dungeonId, difficultyId), 27,
                ChatUtils.colorize("&c&lCONFIG: &e" + difficultyId));
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++)
            inv.setItem(i, filler);

        inv.setItem(10, createItem(Material.NAME_TAG, "&e&lDisplay Name", "&aKlik untuk ubah"));
        inv.setItem(11, createItem(Material.SHIELD, "&e&lMin Tier", "&aKlik untuk set"));
        inv.setItem(12, createItem(Material.TRIPWIRE_HOOK, "&e&lKey Requirement", "&aKlik untuk set"));
        inv.setItem(14, createItem(Material.SKELETON_SKULL, "&e&lMax Deaths", "&aKlik untuk set"));
        inv.setItem(15, createItem(Material.GOLD_INGOT, "&e&lReward Multiplier", "&aKlik untuk set (misal: 1.5)"));
        inv.setItem(22, createItem(Material.ARROW, "&cKembali"));

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof DiffConfigHolder holder))
            return;
        e.setCancelled(true);
        if (e.getCurrentItem() == null)
            return;
        Player player = (Player) e.getWhoClicked();
        String path = "difficulties." + holder.difficultyId + ".";

        switch (e.getSlot()) {
            case 10 -> plugin.getEditorChatInput().requestInput(player,
                    ChatUtils.colorize("&eMasukkan display name:"), input -> {
                        plugin.getDungeonManager().setDungeonConfig(holder.dungeonId, path + "display", input);
                        player.sendMessage(ChatUtils.colorize("&aDisplay name: &f" + input));
                        open(player, holder.dungeonId, holder.difficultyId);
                    });
            case 11 -> plugin.getEditorChatInput().requestInput(player,
                    ChatUtils.colorize("&eMasukkan min tier (angka):"), input -> {
                        try {
                            int v = Integer.parseInt(input);
                            plugin.getDungeonManager().setDungeonConfig(holder.dungeonId, path + "min-tier", v);
                            player.sendMessage(ChatUtils.colorize("&aMin tier: &f" + v));
                        } catch (NumberFormatException ex) {
                            player.sendMessage(ChatUtils.colorize("&cAngka tidak valid!"));
                        }
                        open(player, holder.dungeonId, holder.difficultyId);
                    });
            case 12 -> plugin.getEditorChatInput().requestInput(player,
                    ChatUtils.colorize("&eMasukkan key requirement (atau 'none'):"), input -> {
                        plugin.getDungeonManager().setDungeonConfig(holder.dungeonId, path + "key-req", input);
                        player.sendMessage(ChatUtils.colorize("&aKey req: &f" + input));
                        open(player, holder.dungeonId, holder.difficultyId);
                    });
            case 14 -> plugin.getEditorChatInput().requestInput(player,
                    ChatUtils.colorize("&eMasukkan max deaths (angka):"), input -> {
                        try {
                            int v = Integer.parseInt(input);
                            plugin.getDungeonManager().setDungeonConfig(holder.dungeonId, path + "max-deaths", v);
                            player.sendMessage(ChatUtils.colorize("&aMax deaths: &f" + v));
                        } catch (NumberFormatException ex) {
                            player.sendMessage(ChatUtils.colorize("&cAngka tidak valid!"));
                        }
                        open(player, holder.dungeonId, holder.difficultyId);
                    });
            case 15 -> plugin.getEditorChatInput().requestInput(player,
                    ChatUtils.colorize("&eMasukkan reward multiplier (misal: 1.5):"), input -> {
                        try {
                            double v = Double.parseDouble(input);
                            plugin.getDungeonManager().setDungeonConfig(holder.dungeonId, path + "reward-multiplier",
                                    v);
                            player.sendMessage(ChatUtils.colorize("&aReward multiplier: &f" + v + "x"));
                        } catch (NumberFormatException ex) {
                            player.sendMessage(ChatUtils.colorize("&cAngka tidak valid!"));
                        }
                        open(player, holder.dungeonId, holder.difficultyId);
                    });
            case 22 -> new DifficultyEditorGUI(plugin).open(player, holder.dungeonId);
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

    public static class DiffConfigHolder implements InventoryHolder {
        public final String dungeonId, difficultyId;

        public DiffConfigHolder(String d, String diff) {
            this.dungeonId = d;
            this.difficultyId = diff;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
