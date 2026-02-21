package id.naturalsmp.naturaldungeon.dungeon;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.party.Party;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import id.naturalsmp.naturaldungeon.utils.ConfigUtils;
import id.naturalsmp.naturaldungeon.utils.GUIUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class DungeonGUI implements Listener {

    private final NaturalDungeon plugin;
    private final NamespacedKey buttonKey;

    public DungeonGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
        this.buttonKey = new NamespacedKey(plugin, "button_id");
    }

    public void open(Player player) {
        // ─── Premium Title: ❂ ᴅᴜɴɢᴇᴏɴ ꜱᴇʟᴇᴄᴛɪᴏɴ ❂ ───
        Inventory inv = GUIUtils.createGUI(new DungeonHolder(), 54,
                "&#FFD700❂ ᴅᴜɴɢᴇᴏɴ ꜱᴇʟᴇᴄᴛɪᴏɴ ❂");

        // Fill border with dark glass
        GUIUtils.fillBorder(inv, Material.BLACK_STAINED_GLASS_PANE);
        // Fill inner with gray glass
        GUIUtils.fillEmpty(inv, Material.GRAY_STAINED_GLASS_PANE);

        // ─── Dungeon Items (slots 10-16, 19-25) ───
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
            lore.add(GUIUtils.separator());
            for (String line : dungeon.getDescription())
                lore.add("&7" + line);
            lore.add("");
            lore.add("&7Tier: &#FFAA00" + dungeon.getMinTier() + "+");
            lore.add("&7Players: &f1-" + dungeon.getMaxPlayers());
            lore.add("&7Stages: &f" + dungeon.getTotalStages());

            // Cooldown check
            long cooldown = plugin.getDungeonManager().getRemainingCooldown(player, dungeon);
            if (cooldown > 0) {
                lore.add("");
                lore.add("&#FF5555⏳ Cooldown: &f" + ChatUtils.formatTime(cooldown));
            } else {
                lore.add("");
                lore.add("&#55FF55&l➥ KLIK UNTUK MASUK");
            }

            ItemStack item = GUIUtils.createItem(mat,
                    "&#FFAA00&l⚔ " + dungeon.getDisplayName(),
                    lore);
            ItemMeta meta = item.getItemMeta();
            meta.getPersistentDataContainer().set(buttonKey, PersistentDataType.STRING, dungeon.getId());
            item.setItemMeta(meta);
            inv.setItem(slots[idx++], item);
        }

        // ─── Bottom Action Bar ───

        // Party Button (slot 47)
        Party party = plugin.getPartyManager().getParty(player.getUniqueId());
        if (party != null) {
            ItemStack partyItem = GUIUtils.createItem(Material.PLAYER_HEAD,
                    "&#00AAFF&l👥 ᴘᴀʀᴛʏ",
                    GUIUtils.separator(),
                    "&7Members: &f" + party.getMembers().size() + "/" + party.getMaxPlayers(),
                    "&7Tier: &#FFAA00" + party.getTier(),
                    "",
                    "&#55FF55&l➥ KLIK UNTUK DETAIL");
            ItemMeta pMeta = partyItem.getItemMeta();
            pMeta.getPersistentDataContainer().set(buttonKey, PersistentDataType.STRING, "btn_party");
            partyItem.setItemMeta(pMeta);
            inv.setItem(47, partyItem);
        } else {
            ItemStack soloItem = GUIUtils.createItem(Material.GRAY_DYE,
                    "&7👤 ꜱᴏʟᴏ ᴍᴏᴅᴇ",
                    GUIUtils.separator(),
                    "&7Kamu akan masuk secara &fSolo&7.",
                    "&7Gunakan &e/party &7untuk membuat grup.");
            ItemMeta sMeta = soloItem.getItemMeta();
            sMeta.getPersistentDataContainer().set(buttonKey, PersistentDataType.STRING, "btn_solo");
            soloItem.setItemMeta(sMeta);
            inv.setItem(47, soloItem);
        }

        // Leaderboard Button (slot 49)
        ItemStack lbItem = GUIUtils.createItem(Material.GOLD_INGOT,
                "&#FFD700&l🏆 ʟᴇᴀᴅᴇʀʙᴏᴀʀᴅ",
                GUIUtils.separator(),
                "&7Lihat rekor tercepat!",
                "",
                "&#FFAA00&l➥ KLIK");
        ItemMeta lbMeta = lbItem.getItemMeta();
        lbMeta.getPersistentDataContainer().set(buttonKey, PersistentDataType.STRING, "btn_leaderboard");
        lbItem.setItemMeta(lbMeta);
        inv.setItem(49, lbItem);

        // Stats Button (slot 51)
        ItemStack statsItem = GUIUtils.createItem(Material.BOOK,
                "&#AA44FF&l📊 ꜱᴛᴀᴛɪꜱᴛɪᴋ",
                GUIUtils.separator(),
                "&7Lihat statistik dungeon kamu.",
                "",
                "&#FFAA00&l➥ KLIK");
        ItemMeta stMeta = statsItem.getItemMeta();
        stMeta.getPersistentDataContainer().set(buttonKey, PersistentDataType.STRING, "btn_stats");
        statsItem.setItemMeta(stMeta);
        inv.setItem(51, statsItem);

        player.openInventory(inv);
        GUIUtils.playOpenSound(player);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof DungeonHolder))
            return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getView().getTopInventory())
            return;

        if (e.getCurrentItem() == null)
            return;
        Player player = (Player) e.getWhoClicked();
        ItemMeta meta = e.getCurrentItem().getItemMeta();
        if (meta == null || !meta.getPersistentDataContainer().has(buttonKey, PersistentDataType.STRING))
            return;

        String cmdId = meta.getPersistentDataContainer().get(buttonKey, PersistentDataType.STRING);
        GUIUtils.playClickSound(player);

        // Handle specific buttons
        if (cmdId.equals("btn_leaderboard")) {
            List<Dungeon> dungeons = new ArrayList<>(plugin.getDungeonManager().getDungeons());
            if (!dungeons.isEmpty()) {
                plugin.getLeaderboardGUI().open(player, dungeons.get(0).getId());
            }
            return;
        }
        if (cmdId.equals("btn_party")) {
            plugin.getPartyGUI().open(player);
            return;
        }
        if (cmdId.equals("btn_stats")) {
            plugin.getStatsGUI().open(player);
            return;
        }
        if (cmdId.equals("btn_solo")) {
            return; // No action for solo mode info
        }

        Dungeon dungeon = plugin.getDungeonManager().getDungeon(cmdId);
        if (dungeon == null)
            return;

        // Check cooldown
        if (plugin.getDungeonManager().isOnCooldown(player, dungeon)) {
            long remaining = plugin.getDungeonManager().getRemainingCooldown(player, dungeon);
            player.sendMessage(
                    ConfigUtils.getMessage("dungeon.on-cooldown", "%time%", ChatUtils.formatTime(remaining)));
            GUIUtils.playErrorSound(player);
            return;
        }

        // Check already in dungeon
        if (plugin.getDungeonManager().isInDungeon(player)) {
            player.sendMessage(ConfigUtils.getMessage("dungeon.already-in-dungeon"));
            GUIUtils.playErrorSound(player);
            return;
        }

        player.closeInventory();

        // If dungeon has multiple difficulties, open difficulty selector
        if (dungeon.getDifficulties().size() > 1) {
            new DifficultyGUI(plugin).open(player, dungeon.getId());
        } else {
            // Single difficulty — go straight to start
            plugin.getDungeonManager().startDungeon(player, dungeon);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof DungeonHolder)
            e.setCancelled(true);
    }

    public static class DungeonHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
