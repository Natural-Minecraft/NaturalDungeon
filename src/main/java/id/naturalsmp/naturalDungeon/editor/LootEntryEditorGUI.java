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
 * Edit a single loot entry: type, material/ID, amount, chance, rarity.
 */
public class LootEntryEditorGUI implements Listener {

    private final NaturalDungeon plugin;

    public LootEntryEditorGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String dungeonId, int entryIndex) {
        Inventory inv = Bukkit.createInventory(new LootEntryHolder(dungeonId, entryIndex), 27,
                ChatUtils.colorize("&a&lLOOT ENTRY #" + (entryIndex + 1)));
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++)
            inv.setItem(i, filler);

        inv.setItem(10, createItem(Material.PAPER, "&e&lType",
                "&7VANILLA / MMOITEMS / COMMAND", "&aKlik untuk set"));
        inv.setItem(11, createItem(Material.DIAMOND, "&e&lMaterial / ID",
                "&7Material vanilla atau MMOItems ID", "&aKlik untuk set"));
        inv.setItem(12, createItem(Material.CHEST, "&e&lAmount",
                "&7Jumlah item (bisa range: 1-3)", "&aKlik untuk set"));
        inv.setItem(14, createItem(Material.RABBIT_FOOT, "&e&lChance (%)",
                "&7Peluang drop (0-100)", "&aKlik untuk set"));
        inv.setItem(15, createItem(Material.NETHER_STAR, "&e&lRarity",
                "&7COMMON/RARE/EPIC/LEGENDARY", "&aKlik untuk set"));

        inv.setItem(22, createItem(Material.ARROW, "&cKembali"));
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof LootEntryHolder holder))
            return;
        e.setCancelled(true);
        if (e.getCurrentItem() == null)
            return;
        Player player = (Player) e.getWhoClicked();
        String path = "loot.completion." + holder.entryIndex + ".";

        switch (e.getSlot()) {
            case 10 -> plugin.getEditorChatInput().requestInput(player,
                    ChatUtils.colorize("&eTipe loot (VANILLA/MMOITEMS/COMMAND):"),
                    input -> {
                        plugin.getDungeonManager().setDungeonConfig(holder.dungeonId, path + "type",
                                input.toUpperCase());
                        player.sendMessage(ChatUtils.colorize("&aTipe: &f" + input.toUpperCase()));
                        open(player, holder.dungeonId, holder.entryIndex);
                    });
            case 11 -> plugin.getEditorChatInput().requestInput(player,
                    ChatUtils.colorize("&eMaterial/ID (misal: DIAMOND_SWORD atau SWORD:DarkBlade):"),
                    input -> {
                        plugin.getDungeonManager().setDungeonConfig(holder.dungeonId, path + "material", input);
                        player.sendMessage(ChatUtils.colorize("&aMaterial: &f" + input));
                        open(player, holder.dungeonId, holder.entryIndex);
                    });
            case 12 -> plugin.getEditorChatInput().requestInput(player,
                    ChatUtils.colorize("&eJumlah (angka atau range misal: 1-3):"),
                    input -> {
                        plugin.getDungeonManager().setDungeonConfig(holder.dungeonId, path + "amount", input);
                        player.sendMessage(ChatUtils.colorize("&aAmount: &f" + input));
                        open(player, holder.dungeonId, holder.entryIndex);
                    });
            case 14 -> plugin.getEditorChatInput().requestInput(player,
                    ChatUtils.colorize("&eChance drop (0-100):"),
                    input -> {
                        try {
                            int v = Integer.parseInt(input);
                            plugin.getDungeonManager().setDungeonConfig(holder.dungeonId, path + "chance", v);
                            player.sendMessage(ChatUtils.colorize("&aChance: &f" + v + "%"));
                        } catch (NumberFormatException ex) {
                            player.sendMessage(ChatUtils.colorize("&cAngka tidak valid!"));
                        }
                        open(player, holder.dungeonId, holder.entryIndex);
                    });
            case 15 -> plugin.getEditorChatInput().requestInput(player,
                    ChatUtils.colorize("&eRarity (COMMON/RARE/EPIC/LEGENDARY):"),
                    input -> {
                        plugin.getDungeonManager().setDungeonConfig(holder.dungeonId, path + "rarity",
                                input.toUpperCase());
                        player.sendMessage(ChatUtils.colorize("&aRarity: &f" + input.toUpperCase()));
                        open(player, holder.dungeonId, holder.entryIndex);
                    });
            case 22 -> new RewardEditorGUI(plugin).open(player, holder.dungeonId);
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

    public static class LootEntryHolder implements InventoryHolder {
        public final String dungeonId;
        public final int entryIndex;

        public LootEntryHolder(String d, int e) {
            this.dungeonId = d;
            this.entryIndex = e;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
