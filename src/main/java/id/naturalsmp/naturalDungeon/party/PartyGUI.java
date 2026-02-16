package id.naturalsmp.naturaldungeon.party;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import id.naturalsmp.naturaldungeon.utils.ConfigUtils;
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
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class PartyGUI implements Listener {

    private final NaturalDungeon plugin;
    private final PartyManager manager;
    private final Map<UUID, Long> lastClick = new HashMap<>();

    public PartyGUI(NaturalDungeon plugin, PartyManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        // Listener registered as singleton in NaturalDungeon.registerListeners()
    }

    public void open(Player player) {
        String title = ConfigUtils.getMessage("gui.party.title");
        Inventory inv = Bukkit.createInventory(new PartyHolder(), 45, title);

        Party party = manager.getParty(player.getUniqueId());

        // Fill background
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 45; i++)
            inv.setItem(i, filler);

        if (party == null) {
            // No party - show create button
            inv.setItem(22, createItem(Material.LIME_DYE, "&#55FF55&l✚ BUAT PARTY",
                    "&7Klik untuk membuat party baru!",
                    "&7Max: &f2 &7pemain (upgrade untuk &e4-24&7)"));
        } else {
            // Show party members
            int[] memberSlots = { 10, 11, 12, 13, 14, 15, 16 };
            int idx = 0;
            for (UUID uuid : party.getMembers()) {
                if (idx >= memberSlots.length)
                    break;
                Player member = Bukkit.getPlayer(uuid);
                boolean isLeader = party.isLeader(uuid);
                ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) skull.getItemMeta();
                if (member != null)
                    meta.setOwningPlayer(member);
                meta.setDisplayName(ChatUtils.colorize(
                        (isLeader ? "&#FFAA00&l★ " : "&f") + (member != null ? member.getName() : "Offline")));
                List<String> lore = new ArrayList<>();
                lore.add(ChatUtils.colorize(isLeader ? "&6Leader" : "&7Member"));
                if (party.isLeader(player.getUniqueId()) && !isLeader) {
                    lore.add("");
                    lore.add(ChatUtils.colorize("&cKlik untuk kick"));
                }
                meta.setLore(lore);
                skull.setItemMeta(meta);
                inv.setItem(memberSlots[idx++], skull);
            }

            // Info panel
            inv.setItem(30, createItem(Material.BOOK, "&#00AAFF&lPARTY INFO",
                    "&7Tier: &e" + party.getTier(),
                    "&7Max Players: &f" + party.getMaxPlayers(),
                    "&7Members: &f" + party.getMembers().size() + "/" + party.getMaxPlayers()));

            // Upgrade button (leader only)
            if (party.isLeader(player.getUniqueId()) && party.getTier() < 5) {
                int cost = party.getUpgradeCost();
                int nextMax = switch (party.getTier()) {
                    case 1 -> 4;
                    case 2 -> 8;
                    case 3 -> 16;
                    case 4 -> 24;
                    default -> 24;
                };
                inv.setItem(31, createItem(Material.GOLD_INGOT, "&#FFAA00&l⬆ UPGRADE",
                        "&7Upgrade ke Tier " + (party.getTier() + 1),
                        "&7Max Players: &e" + nextMax,
                        "",
                        "&6Harga: &f$" + ChatUtils.formatLarge(cost)));
            }

            // Invite button
            if (party.isLeader(player.getUniqueId())) {
                inv.setItem(32, createItem(Material.WRITABLE_BOOK, "&#55FF55&l+ INVITE",
                        "&7Ketik &b/party invite <player>",
                        "&7untuk mengundang pemain."));
            }

            // Leave button
            inv.setItem(40, createItem(Material.BARRIER, "&#FF5555&l✕ LEAVE",
                    party.isLeader(player.getUniqueId()) ? "&cDisbandkan party" : "&7Keluar dari party"));
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof PartyHolder))
            return;
        e.setCancelled(true);

        if (e.getCurrentItem() == null)
            return;
        Player player = (Player) e.getWhoClicked();

        // Anti-spam click (1 second cooldown)
        long now = System.currentTimeMillis();
        if (lastClick.containsKey(player.getUniqueId()) && now - lastClick.get(player.getUniqueId()) < 1000) {
            return;
        }
        lastClick.put(player.getUniqueId(), now);

        Party party = manager.getParty(player.getUniqueId());
        Material type = e.getCurrentItem().getType();

        if (type == Material.LIME_DYE) {
            player.closeInventory();
            manager.createParty(player);
            Bukkit.getScheduler().runTaskLater(plugin, () -> open(player), 5L);
        } else if (type == Material.GOLD_INGOT) {
            if (party != null)
                manager.upgradeParty(party, player);
            Bukkit.getScheduler().runTaskLater(plugin, () -> open(player), 5L);
        } else if (type == Material.BARRIER) {
            player.closeInventory();
            manager.leaveParty(player);
        } else if (type == Material.WRITABLE_BOOK) {
            player.closeInventory();
            player.sendMessage(ConfigUtils.getMessage("party.invite-prompt"));
        } else if (type == Material.PLAYER_HEAD) {
            // Kick member
            if (party != null && party.isLeader(player.getUniqueId())) {
                SkullMeta meta = (SkullMeta) e.getCurrentItem().getItemMeta();
                if (meta.getOwningPlayer() != null) {
                    Player target = meta.getOwningPlayer().getPlayer();
                    if (target != null && !party.isLeader(target.getUniqueId())) {
                        manager.kickPlayer(party, target);
                        Bukkit.getScheduler().runTaskLater(plugin, () -> open(player), 5L);
                    }
                }
            }
        }
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.colorize(name));
        List<String> loreList = new ArrayList<>();
        for (String l : lore)
            loreList.add(ChatUtils.colorize(l));
        meta.setLore(loreList);
        item.setItemMeta(meta);
        return item;
    }

    public static class PartyHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
