package id.naturalsmp.naturaldungeon.dungeon;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.party.Party;
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

import id.naturalsmp.naturaldungeon.leaderboard.LeaderboardGUI;
import java.util.*;

public class DungeonGUI implements Listener {

    private final NaturalDungeon plugin;
    private final NamespacedKey buttonKey;

    public DungeonGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
        this.buttonKey = new NamespacedKey(plugin, "button_id");
    }

    public void open(Player player) {
        String title = ConfigUtils.getMessage("gui.dungeon.title");
        Inventory inv = Bukkit.createInventory(new DungeonHolder(), 45, title);

        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 45; i++)
            inv.setItem(i, filler);

        int[] slots = { 10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25 };
        int idx = 0;
        for (Dungeon dungeon : plugin.getDungeonManager().getDungeons()) {
            if (idx >= slots.length)
                break;

            Material mat;
            try {
                mat = Material.valueOf(dungeon.getMaterial());
            } catch (Exception e) {
                mat = Material.SPAWNER;
            }

            List<String> lore = new ArrayList<>();
            for (String line : dungeon.getDescription())
                lore.add(ChatUtils.colorize(line));
            lore.add("");
            lore.add(ChatUtils.colorize("&7Tier Minimal: &e" + dungeon.getMinTier()));
            lore.add(ChatUtils.colorize("&7Max Players: &f" + dungeon.getMaxPlayers()));
            lore.add(ChatUtils.colorize("&7Stages: &f" + dungeon.getTotalStages()));

            long cooldown = plugin.getDungeonManager().getRemainingCooldown(player, dungeon);
            if (cooldown > 0) {
                lore.add("");
                lore.add(ChatUtils.colorize("&cCooldown: &f" + ChatUtils.formatTime(cooldown)));
            } else {
                lore.add("");
                lore.add(ChatUtils.colorize("&aKlik untuk masuk!"));
            }

            ItemStack item = createItem(mat, "&#FFAA00&l" + dungeon.getDisplayName(), lore.toArray(new String[0]));
            ItemMeta meta = item.getItemMeta();
            meta.getPersistentDataContainer().set(buttonKey, PersistentDataType.STRING, dungeon.getId());
            item.setItemMeta(meta);
            inv.setItem(slots[idx++], item);
        }

        // Party info
        Party party = plugin.getPartyManager().getParty(player.getUniqueId());
        if (party != null) {
            ItemStack partyItem = createItem(Material.PLAYER_HEAD, "&#00AAFF&lPARTY INFO",
                    "&7Members: &f" + party.getMembers().size() + "/" + party.getMaxPlayers(),
                    "&7Tier Rata-rata: &e" + party.getTier(),
                    "",
                    "&7Status: &aDalam Group");
            ItemMeta pMeta = partyItem.getItemMeta();
            pMeta.getPersistentDataContainer().set(buttonKey, PersistentDataType.STRING, "btn_party");
            partyItem.setItemMeta(pMeta);
            inv.setItem(36, partyItem);
        } else {
            ItemStack soloItem = createItem(Material.GRAY_DYE, "&7NO PARTY",
                    "&7Kamu tidak berada dalam party.",
                    "&7Kamu akan masuk secara &fSolo&7.");
            ItemMeta sMeta = soloItem.getItemMeta();
            sMeta.getPersistentDataContainer().set(buttonKey, PersistentDataType.STRING, "btn_solo");
            soloItem.setItemMeta(sMeta);
            inv.setItem(36, soloItem);
        }

        ItemStack lbItem = createItem(Material.GOLD_INGOT, "&#FFBB00&lLEADERBOARD",
                "&7Klik untuk melihat rekor tercepat!");
        ItemMeta lbMeta = lbItem.getItemMeta();
        lbMeta.getPersistentDataContainer().set(buttonKey, PersistentDataType.STRING, "btn_leaderboard");
        lbItem.setItemMeta(lbMeta);
        inv.setItem(40, lbItem);

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof DungeonHolder))
            return;
        e.setCancelled(true);

        if (e.getCurrentItem() == null)
            return;
        Player player = (Player) e.getWhoClicked();
        ItemMeta meta = e.getCurrentItem().getItemMeta();
        if (meta == null || !meta.getPersistentDataContainer().has(buttonKey, PersistentDataType.STRING))
            return;

        String cmdId = meta.getPersistentDataContainer().get(buttonKey, PersistentDataType.STRING);

        // Handle specific buttons
        if (cmdId.equals("btn_leaderboard")) {
            List<Dungeon> dungeons = new ArrayList<>(plugin.getDungeonManager().getDungeons());
            if (!dungeons.isEmpty()) {
                new LeaderboardGUI(plugin).open(player, dungeons.get(0).getId());
            }
            return;
        }
        if (cmdId.equals("btn_party") || cmdId.equals("btn_solo")) {
            // Option to open party menu or just info
            return;
        }

        Dungeon dungeon = plugin.getDungeonManager().getDungeon(cmdId);
        if (dungeon == null)
            return;

        player.closeInventory();

        // Check cooldown
        if (plugin.getDungeonManager().isOnCooldown(player, dungeon)) {
            long remaining = plugin.getDungeonManager().getRemainingCooldown(player, dungeon);
            player.sendMessage(
                    ConfigUtils.getMessage("dungeon.on-cooldown", "%time%", ChatUtils.formatTime(remaining)));
            return;
        }

        // Check already in dungeon
        if (plugin.getDungeonManager().isInDungeon(player)) {
            player.sendMessage(ConfigUtils.getMessage("dungeon.already-in-dungeon"));
            return;
        }

        // If dungeon has multiple difficulties, open difficulty selector
        if (dungeon.getDifficulties().size() > 1) {
            new DifficultyGUI(plugin).open(player, dungeon.getId());
        } else {
            // Single difficulty — go straight to confirmation/start
            plugin.getDungeonManager().startDungeon(player, dungeon);
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

    public static class DungeonHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
