package id.naturalsmp.naturaldungeon.party;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import id.naturalsmp.naturaldungeon.utils.ConfigUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class PartyManager implements Listener {

    private final NaturalDungeon plugin;
    private final Map<UUID, Party> parties = new HashMap<>();
    private final Map<UUID, UUID> playerPartyMap = new HashMap<>();

    public PartyManager(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public List<Party> getPartiesInviting(UUID player) {
        List<Party> inviting = new ArrayList<>();
        for (Party p : parties.values()) {
            if (p.hasInvite(player)) {
                inviting.add(p);
            }
        }
        return inviting;
    }

    public Party getParty(UUID player) {
        UUID partyLeader = playerPartyMap.get(player);
        return partyLeader != null ? parties.get(partyLeader) : null;
    }

    public Party createParty(Player player) {
        if (playerPartyMap.containsKey(player.getUniqueId()))
            return null;
        Party party = new Party(player.getUniqueId());
        parties.put(player.getUniqueId(), party);
        playerPartyMap.put(player.getUniqueId(), player.getUniqueId());
        player.sendMessage(ConfigUtils.getMessage("party.created"));
        return party;
    }

    public void disbandParty(Party party) {
        for (UUID member : party.getMembers()) {
            Player p = Bukkit.getPlayer(member);
            if (p != null)
                p.sendMessage(ConfigUtils.getMessage("party.disbanded"));
            playerPartyMap.remove(member);
        }
        parties.remove(party.getLeader());
    }

    public void invitePlayer(Party party, Player inviter, Player target) {
        if (party.hasInvite(target.getUniqueId())) {
            inviter.sendMessage(ConfigUtils.getMessage("party.already-invited", "%player%", target.getName()));
            return;
        }
        if (playerPartyMap.containsKey(target.getUniqueId())) {
            inviter.sendMessage(ConfigUtils.getMessage("party.player-already-in-party", "%player%", target.getName()));
            return;
        }
        party.addInvite(target.getUniqueId());
        inviter.sendMessage(ConfigUtils.getMessage("party.invite-sent", "%player%", target.getName()));
        target.sendMessage(ConfigUtils.getMessage("party.invite-received", "%player%", inviter.getName()));

        // [NEW] Ultimate Interactive Component
        TextComponent joinButton = new TextComponent(ChatUtils.colorize(" &a&l[KLIK UNTUK JOIN]"));
        joinButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/party accept"));
        joinButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new Text(ChatUtils.colorize("&7Klik untuk segera bergabung!"))));

        target.spigot().sendMessage(joinButton);

        // Auto-expire invite after 60 seconds
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (party.hasInvite(target.getUniqueId())) {
                party.removeInvite(target.getUniqueId());
                if (target.isOnline()) {
                    target.sendMessage(ConfigUtils.getMessage("party.invite-expired", "%player%", inviter.getName()));
                }
            }
        }, 1200L);
    }

    public void acceptInvite(Player player, Party party) {
        if (!party.hasInvite(player.getUniqueId())) {
            player.sendMessage(ConfigUtils.getMessage("party.no-pending-invite"));
            return;
        }
        if (party.isFull()) {
            player.sendMessage(ConfigUtils.getMessage("party.party-full"));
            return;
        }
        party.addMember(player.getUniqueId());
        playerPartyMap.put(player.getUniqueId(), party.getLeader());
        broadcastToParty(party, ConfigUtils.getMessage("party.player-joined", "%player%", player.getName()));
    }

    public void leaveParty(Player player) {
        Party party = getParty(player.getUniqueId());
        if (party == null) {
            player.sendMessage(ConfigUtils.getMessage("party.not-in-party"));
            return;
        }
        if (party.isLeader(player.getUniqueId())) {
            // Try to transfer leadership first
            List<UUID> others = party.getMembers().stream()
                    .filter(u -> !u.equals(player.getUniqueId())).toList();
            if (others.isEmpty()) {
                disbandParty(party);
            } else {
                transferAndRemove(party, player, others.get(0));
            }
        } else {
            party.removeMember(player.getUniqueId());
            playerPartyMap.remove(player.getUniqueId());
            player.sendMessage(ConfigUtils.getMessage("party.you-left"));
            broadcastToParty(party, ConfigUtils.getMessage("party.player-left", "%player%", player.getName()));
        }
    }

    public void kickPlayer(Party party, Player kicked) {
        party.removeMember(kicked.getUniqueId());
        playerPartyMap.remove(kicked.getUniqueId());
        Player leader = Bukkit.getPlayer(party.getLeader());
        kicked.sendMessage(
                ConfigUtils.getMessage("party.you-kicked", "%leader%", leader != null ? leader.getName() : "Unknown"));
        broadcastToParty(party, ConfigUtils.getMessage("party.player-kicked", "%player%", kicked.getName()));
    }

    public boolean upgradeParty(Party party, Player player) {
        if (party.getTier() >= 5) {
            player.sendMessage(ConfigUtils.getMessage("party.upgrade-max"));
            return false;
        }
        int cost = party.getUpgradeCost();
        if (!plugin.getVaultHook().has(player, cost)) {
            player.sendMessage(ConfigUtils.getMessage("party.upgrade-not-enough", "%cost%", String.valueOf(cost)));
            return false;
        }
        plugin.getVaultHook().withdraw(player, cost);
        party.upgradeTier();
        broadcastToParty(party, ConfigUtils.getMessage("party.upgrade-success",
                "%tier%", String.valueOf(party.getTier()),
                "%max%", String.valueOf(party.getMaxPlayers())));
        return true;
    }

    /**
     * Transfer leader and remove old leader from party.
     */
    private void transferAndRemove(Party party, Player oldLeader, UUID newLeaderUUID) {
        // Update maps: remove old party entry, transfer leader, add new
        parties.remove(party.getLeader());
        party.transferLeader(newLeaderUUID);
        party.removeMember(oldLeader.getUniqueId());
        playerPartyMap.remove(oldLeader.getUniqueId());
        parties.put(newLeaderUUID, party);
        // Update all member mappings to point to new leader
        for (UUID member : party.getMembers()) {
            playerPartyMap.put(member, newLeaderUUID);
        }
        Player newLeader = Bukkit.getPlayer(newLeaderUUID);
        String newLeaderName = newLeader != null ? newLeader.getName() : "Unknown";
        oldLeader.sendMessage(ConfigUtils.getMessage("party.you-left"));
        broadcastToParty(party,
                ConfigUtils.getMessage("party.leader-transferred", "%player%", newLeaderName));
    }

    /**
     * Send a chat message to all party members.
     */
    public void chatToParty(Player sender, String message) {
        Party party = getParty(sender.getUniqueId());
        if (party == null) {
            sender.sendMessage(ConfigUtils.getMessage("party.not-in-party"));
            return;
        }
        String formattedMsg = ChatUtils.colorize("&6&l[PARTY] &f" + sender.getName() + "&7: &f" + message);
        broadcastToParty(party, formattedMsg);
    }

    public void broadcastToParty(Party party, String message) {
        for (UUID uuid : party.getMembers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null)
                p.sendMessage(message);
        }
    }

    public void openPartyGUI(Player player) {
        plugin.getPartyGUI().open(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        Party party = getParty(player.getUniqueId());
        if (party != null) {
            if (party.isLeader(player.getUniqueId())) {
                // Transfer leadership instead of disbanding
                List<UUID> others = party.getMembers().stream()
                        .filter(u -> !u.equals(player.getUniqueId())).toList();
                if (others.isEmpty()) {
                    disbandParty(party);
                } else {
                    transferAndRemove(party, player, others.get(0));
                }
            } else {
                party.removeMember(player.getUniqueId());
                playerPartyMap.remove(player.getUniqueId());
                broadcastToParty(party, ConfigUtils.getMessage("party.player-left", "%player%", player.getName()));
            }
        }
    }
}
