package id.naturalsmp.naturaldungeon.commands;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.party.Party;
import id.naturalsmp.naturaldungeon.utils.ConfigUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PartyCommand implements CommandExecutor, TabCompleter {

    private final NaturalDungeon plugin;

    public PartyCommand(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ConfigUtils.getMessage("general.player-only"));
            return true;
        }

        if (!player.hasPermission("naturaldungeon.party")) {
            player.sendMessage(ConfigUtils.getMessage("general.no-permission"));
            return true;
        }

        if (args.length == 0) {
            plugin.getPartyManager().openPartyGUI(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        Party party = plugin.getPartyManager().getParty(player.getUniqueId());

        switch (sub) {
            case "create" -> {
                if (party != null) {
                    player.sendMessage(ConfigUtils.getMessage("party.already-in-party"));
                    return true;
                }
                plugin.getPartyManager().createParty(player);
            }
            case "invite" -> {
                if (party == null) {
                    player.sendMessage(ConfigUtils.getMessage("party.not-in-party"));
                    return true;
                }
                if (!party.isLeader(player.getUniqueId())) {
                    player.sendMessage(ConfigUtils.getMessage("party.not-leader"));
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(ConfigUtils.getMessage("party.invite-usage"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage(ConfigUtils.getMessage("general.player-not-found", "%player%", args[1]));
                    return true;
                }
                if (target.equals(player)) {
                    player.sendMessage(ConfigUtils.getMessage("party.cannot-invite-self"));
                    return true;
                }
                plugin.getPartyManager().invitePlayer(party, player, target);
            }
            case "accept" -> {
                List<Party> invites = plugin.getPartyManager().getPartiesInviting(player.getUniqueId());
                if (invites.isEmpty()) {
                    player.sendMessage(ConfigUtils.getMessage("party.no-pending-invite"));
                    return true;
                }
                // Auto-accept the first one or most recent
                plugin.getPartyManager().acceptInvite(player, invites.get(invites.size() - 1));
            }
            case "leave" -> plugin.getPartyManager().leaveParty(player);
            case "kick" -> {
                if (party == null) {
                    player.sendMessage(ConfigUtils.getMessage("party.not-in-party"));
                    return true;
                }
                if (!party.isLeader(player.getUniqueId())) {
                    player.sendMessage(ConfigUtils.getMessage("party.not-leader"));
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(ConfigUtils.getMessage("party.kick-usage"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null || !party.isMember(target.getUniqueId())) {
                    player.sendMessage(ConfigUtils.getMessage("party.player-not-in-party", "%player%", args[1]));
                    return true;
                }
                plugin.getPartyManager().kickPlayer(party, target);
            }
            case "chat" -> {
                if (args.length < 2) {
                    player.sendMessage(ConfigUtils.getMessage("party.chat-usage"));
                    return true;
                }
                String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                plugin.getPartyManager().chatToParty(player, message);
            }
            case "dashboard" -> {
                if (party == null) {
                    player.sendMessage(ConfigUtils.getMessage("party.not-in-party"));
                    return true;
                }
                new id.naturalsmp.naturaldungeon.party.PartyDashboardGUI(plugin).open(player);
            }
            default -> plugin.getPartyManager().openPartyGUI(player);
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.addAll(Arrays.asList("create", "invite", "accept", "leave", "kick", "chat", "dashboard"));
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("invite") || args[0].equalsIgnoreCase("kick"))) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                completions.add(p.getName());
            }
        }
        return completions;
    }
}
