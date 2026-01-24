package id.naturalsmp.naturaldungeon.party;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.dungeon.Dungeon;
import id.naturalsmp.naturaldungeon.dungeon.DungeonDifficulty;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import id.naturalsmp.naturaldungeon.utils.ConfigUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class PartyConfirmationGUI implements Listener {

    private final NaturalDungeon plugin;
    private final Map<UUID, ConfirmationSession> sessions = new HashMap<>();

    public PartyConfirmationGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void startConfirmation(Player leader, Dungeon dungeon, DungeonDifficulty difficulty) {
        Party party = plugin.getPartyManager().getParty(leader.getUniqueId());
        List<UUID> participants = party != null ? party.getMembers() : Collections.singletonList(leader.getUniqueId());

        // Validate Key (Leader Only)
        if (!checkKey(leader, difficulty)) {
            leader.sendMessage(ConfigUtils.getMessage("dungeon.key-required", "%key%", difficulty.getKeyReq()));
            return;
        }

        ConfirmationSession session = new ConfirmationSession(dungeon, difficulty, participants);
        for (UUID uuid : participants) {
            sessions.put(uuid, session);
            Player p = Bukkit.getPlayer(uuid);
            if (p != null)
                openInventory(p, session);
        }
    }

    private void openInventory(Player player, ConfirmationSession session) {
        Inventory inv = Bukkit.createInventory(new ConfirmationHolder(), 27,
                ChatUtils.colorize("&8Ready Check: " + session.dungeon.getDisplayName()));

        // Members Status
        int[] slots = { 10, 11, 12, 13, 14, 15 };
        int idx = 0;
        for (UUID uuid : session.participants) {
            if (idx >= slots.length)
                break;
            boolean isReady = session.readyStatus.getOrDefault(uuid, false);
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            ItemStack head = createItem(isReady ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE,
                    (isReady ? "&a" : "&c") + name,
                    isReady ? "&7Status: &aREADY" : "&7Status: &cWAITING");
            inv.setItem(slots[idx++], head);
        }

        // Ready Button
        boolean myStatus = session.readyStatus.getOrDefault(player.getUniqueId(), false);
        inv.setItem(22,
                createItem(myStatus ? Material.LIME_DYE : Material.GRAY_DYE, "&a&lSIAP!", "&7Klik untuk konfirmasi."));

        player.openInventory(inv);
    }

    private void updateAllInventories(ConfirmationSession session) {
        for (UUID uuid : session.participants) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.getOpenInventory().getTopInventory().getHolder() instanceof ConfirmationHolder) {
                openInventory(p, session);
            }
        }
    }

    private boolean checkKey(Player player, DungeonDifficulty diff) {
        String req = diff.getKeyReq();
        if (req == null || req.equalsIgnoreCase("none"))
            return true;

        String[] parts = req.split(":"); // TYPE:ID:AMOUNT
        if (parts.length < 3)
            return true;

        String type = parts[0];
        String id = parts[1];
        int amount = Integer.parseInt(parts[2]);

        if (type.equalsIgnoreCase("VANILLA")) {
            Material mat = Material.matchMaterial(id);
            return mat != null && player.getInventory().containsAtLeast(new ItemStack(mat), amount);
        } else if (type.equalsIgnoreCase("MMOITEMS") && plugin.hasMMOItems()) {
            return plugin.getMmoItemsHook().hasItem(player, parts[1], parts[2], amount);
        }
        return true;
    }

    private void consumeKey(Player player, DungeonDifficulty diff) {
        String req = diff.getKeyReq();
        if (req == null || req.equalsIgnoreCase("none"))
            return;

        String[] parts = req.split(":");
        if (parts.length < 3)
            return;

        String type = parts[0];
        String id = parts[1];
        int amount = Integer.parseInt(parts[2]);

        if (type.equalsIgnoreCase("VANILLA")) {
            Material mat = Material.matchMaterial(id);
            if (mat != null)
                player.getInventory().removeItem(new ItemStack(mat, amount));
        } else if (type.equalsIgnoreCase("MMOITEMS") && plugin.hasMMOItems()) {
            plugin.getMmoItemsHook().consumeItem(player, parts[1], parts[2], amount);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof ConfirmationHolder))
            return;
        e.setCancelled(true);
        Player player = (Player) e.getWhoClicked();
        ConfirmationSession session = sessions.get(player.getUniqueId());

        if (session == null) {
            player.closeInventory();
            return;
        }

        if (e.getCurrentItem() != null && (e.getCurrentItem().getType() == Material.LIME_DYE
                || e.getCurrentItem().getType() == Material.GRAY_DYE)) {
            session.readyStatus.put(player.getUniqueId(), true);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
            updateAllInventories(session);

            // Check if all ready
            if (session.isAllReady()) {
                startDungeon(session);
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
    }

    private void startDungeon(ConfirmationSession session) {
        // Cleanup sessions
        for (UUID uuid : session.participants) {
            sessions.remove(uuid);
            Player p = Bukkit.getPlayer(uuid);
            if (p != null)
                p.closeInventory();
        }

        // Call startDungeon with difficulty support (requires update to DungeonManager)
        plugin.getDungeonManager().startDungeon(session.participants, session.dungeon, session.difficulty);
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

    private static class ConfirmationSession {
        final Dungeon dungeon;
        final DungeonDifficulty difficulty;
        final List<UUID> participants;
        final Map<UUID, Boolean> readyStatus = new HashMap<>();

        ConfirmationSession(Dungeon dungeon, DungeonDifficulty difficulty, List<UUID> participants) {
            this.dungeon = dungeon;
            this.difficulty = difficulty;
            this.participants = participants;
        }

        boolean isAllReady() {
            for (UUID uuid : participants) {
                if (!readyStatus.getOrDefault(uuid, false))
                    return false;
            }
            return true;
        }
    }

    public static class ConfirmationHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
