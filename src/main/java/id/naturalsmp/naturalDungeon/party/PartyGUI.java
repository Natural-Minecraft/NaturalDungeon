package id.naturalsmp.naturaldungeon.party;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import id.naturalsmp.naturaldungeon.utils.ConfigUtils;
import id.naturalsmp.naturaldungeon.utils.GUIUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class PartyGUI implements Listener {

    private final NaturalDungeon plugin;
    private final PartyManager manager;
    private final Map<UUID, Long> lastClick = new HashMap<>();

    public PartyGUI(NaturalDungeon plugin, PartyManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public void open(Player player) {
        Inventory inv = GUIUtils.createGUI(new PartyHolder(), 45,
                "&#55CCFF👥 ᴘᴀʀᴛʏ ᴍᴀɴᴀɢᴇʀ");

        GUIUtils.fillBorder(inv, Material.BLACK_STAINED_GLASS_PANE);
        GUIUtils.fillEmpty(inv, Material.GRAY_STAINED_GLASS_PANE);

        Party party = manager.getParty(player.getUniqueId());

        if (party == null) {
            // No party - show create button
            inv.setItem(22, GUIUtils.createItem(Material.LIME_DYE,
                    "&#55FF55&l✚ ʙᴜᴀᴛ ᴘᴀʀᴛʏ",
                    GUIUtils.separator(),
                    "&7Buat party baru!",
                    "&7Max: &f2 &7pemain (upgrade → &e4-24&7)",
                    "",
                    "&#FFAA00&l➥ KLIK"));
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
                lore.add(ChatUtils.colorize(GUIUtils.separator()));
                lore.add(ChatUtils.colorize(isLeader ? "&#FFD700Leader" : "&7Member"));
                if (party.isLeader(player.getUniqueId()) && !isLeader) {
                    lore.add("");
                    lore.add(ChatUtils.colorize("&#FF5555&l✖ Klik untuk kick"));
                }
                meta.setLore(lore);
                skull.setItemMeta(meta);
                inv.setItem(memberSlots[idx++], skull);
            }

            // Info panel
            inv.setItem(30, GUIUtils.createItem(Material.BOOK,
                    "&#55CCFF&l📋 ᴘᴀʀᴛʏ ɪɴꜰᴏ",
                    GUIUtils.separator(),
                    "&7Tier: &#FFD700" + party.getTier(),
                    "&7Max: &f" + party.getMaxPlayers(),
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
                inv.setItem(31, GUIUtils.createItem(Material.GOLD_INGOT,
                        "&#FFAA00&l⬆ ᴜᴘɢʀᴀᴅᴇ",
                        GUIUtils.separator(),
                        "&7Upgrade ke Tier " + (party.getTier() + 1),
                        "&7Max Players: &#FFD700" + nextMax,
                        "",
                        "&#FFAA00Harga: &f$" + String.format("%,d", cost)));
            }

            // Invite button
            if (party.isLeader(player.getUniqueId())) {
                inv.setItem(32, GUIUtils.createItem(Material.WRITABLE_BOOK,
                        "&#55FF55&l+ ɪɴᴠɪᴛᴇ",
                        GUIUtils.separator(),
                        "&7Ketik &#55CCFF/party invite <player>",
                        "&7untuk mengundang pemain."));
            }

            // Leave button
            inv.setItem(40, GUIUtils.createItem(Material.BARRIER,
                    "&#FF5555&l✕ ʟᴇᴀᴠᴇ",
                    GUIUtils.separator(),
                    party.isLeader(player.getUniqueId()) ? "&#FF5555Disbandkan party" : "&7Keluar dari party"));
        }

        player.openInventory(inv);
        GUIUtils.playOpenSound(player);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof PartyHolder))
            return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getView().getTopInventory())
            return;
        if (e.getCurrentItem() == null)
            return;

        Player player = (Player) e.getWhoClicked();
        GUIUtils.playClickSound(player);

        // Anti-spam (1s cooldown)
        long now = System.currentTimeMillis();
        if (lastClick.containsKey(player.getUniqueId()) && now - lastClick.get(player.getUniqueId()) < 1000)
            return;
        lastClick.put(player.getUniqueId(), now);

        Party party = manager.getParty(player.getUniqueId());
        Material type = e.getCurrentItem().getType();

        if (type == Material.LIME_DYE) {
            player.closeInventory();
            manager.createParty(player);
            GUIUtils.playSuccessSound(player);
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

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof PartyHolder)
            e.setCancelled(true);
    }

    public static class PartyHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
