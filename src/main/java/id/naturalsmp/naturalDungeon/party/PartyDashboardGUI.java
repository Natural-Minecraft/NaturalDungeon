package id.naturalsmp.naturaldungeon.party;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import id.naturalsmp.naturaldungeon.utils.GUIUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PartyDashboardGUI implements Listener {

    private final NaturalDungeon plugin;
    private final Map<UUID, BukkitTask> updateTasks = new HashMap<>();

    public PartyDashboardGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        // Needs a party
        id.naturalsmp.naturaldungeon.party.Party party = plugin.getPartyManager().getParty(player);
        if (party == null) {
            player.sendMessage(ChatUtils.colorize("&cYou must be in a party to view the dashboard!"));
            return;
        }

        Inventory inv = GUIUtils.createGUI(54, "&#55FF55&lᴘᴀʀᴛʏ ᴅᴀsʜʙᴏᴀʀᴅ", new DashboardHolder(party.getId()));
        GUIUtils.fillBorder(inv);

        // Render immediately
        renderDashboard(inv, party);

        player.openInventory(inv);

        // Start live update task (every 1 second)
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof DashboardHolder holder) {
                if (holder.partyId.equals(party.getId())) {
                    renderDashboard(inv, party);
                } else {
                    stopTask(player.getUniqueId());
                }
            } else {
                stopTask(player.getUniqueId());
            }
        }, 20L, 20L);

        updateTasks.put(player.getUniqueId(), task);
        GUIUtils.playSound(player, "click");
    }

    private void renderDashboard(Inventory inv, id.naturalsmp.naturaldungeon.party.Party party) {
        List<UUID> members = new ArrayList<>(party.getMembers());
        members.add(0, party.getLeader()); // Leader first

        int slot = 10;
        for (UUID memberId : members) {
            if (slot > 43)
                break; // max fit

            Player target = Bukkit.getPlayer(memberId);
            ItemStack item;

            if (target != null && target.isOnline()) {
                item = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) item.getItemMeta();
                if (meta != null) {
                    meta.setOwningPlayer(target);
                    meta.setDisplayName(ChatUtils.colorize("&#00FFFF&l" + target.getName()));

                    List<String> lore = new ArrayList<>();
                    // Role
                    PartyRoleManager.Role role = plugin.getPartyRoleManager().getRole(target.getUniqueId());
                    String roleName = role != null ? role.getDisplayName() : "&7Unassigned";

                    int hp = (int) target.getHealth();
                    int maxHp = (int) target.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
                    String hpColor = hp > maxHp * 0.5 ? "&a" : (hp > maxHp * 0.2 ? "&e" : "&c");

                    lore.add(ChatUtils.colorize(""));
                    lore.add(ChatUtils.colorize(" &fRole: " + roleName));
                    lore.add(ChatUtils.colorize(" &fStatus: &a&lONLINE"));
                    lore.add(ChatUtils.colorize(" &fHP: " + hpColor + hp + "&7/&f" + maxHp + " ❤"));
                    lore.add(ChatUtils.colorize(memberId.equals(party.getLeader()) ? " &e&lLEADER" : " &7Member"));
                    lore.add(ChatUtils.colorize(""));

                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
            } else {
                item = GUIUtils.createItem(Material.SKELETON_SKULL, "&c&lOffline Member",
                        "", "&7Waiting to reconnect...");
            }

            inv.setItem(slot, item);
            slot++;
            if ((slot % 9) == 8)
                slot += 2; // Jump to next row inside border
        }

        // Blank out remaining slots if any
        while (slot <= 43) {
            inv.setItem(slot, new ItemStack(Material.AIR));
            slot++;
            if ((slot % 9) == 8)
                slot += 2;
        }

        // Info item
        inv.setItem(49, GUIUtils.createItem(Material.BOOK, "&#FFD700&lParty Info",
                "", "&fSize: &e" + members.size() + "&7/&e5",
                "&fAvg Level: &e" + calculateAvgLevel(members)));
    }

    private int calculateAvgLevel(List<UUID> members) {
        int total = 0;
        int count = 0;
        for (UUID id : members) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                total += p.getLevel(); // Using vanilla XP level as benchmark, or AuraMobs level
                count++;
            }
        }
        return count == 0 ? 0 : total / count;
    }

    private void stopTask(UUID uuid) {
        if (updateTasks.containsKey(uuid)) {
            updateTasks.get(uuid).cancel();
            updateTasks.remove(uuid);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getClickedInventory() == null)
            return;
        if (e.getInventory().getHolder() instanceof DashboardHolder) {
            e.setCancelled(true);
            if (e.getCurrentItem() != null && e.getCurrentItem().getType() != Material.AIR) {
                Player p = (Player) e.getWhoClicked();
                GUIUtils.playSound(p, "error");
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() instanceof DashboardHolder) {
            stopTask(e.getPlayer().getUniqueId());
        }
    }

    private static class DashboardHolder implements InventoryHolder {
        private final UUID partyId;

        public DashboardHolder(UUID partyId) {
            this.partyId = partyId;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
