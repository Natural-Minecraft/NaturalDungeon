package id.naturalsmp.naturaldungeon.integration;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.dungeon.DungeonInstance;
import id.naturalsmp.naturaldungeon.party.Party;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlaceholderAPIHook extends PlaceholderExpansion {

    private final NaturalDungeon plugin;

    public PlaceholderAPIHook(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getAuthor() {
        return "NaturalSMP";
    }

    @Override
    public @NotNull String getIdentifier() {
        return "naturaldungeon";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null)
            return "";

        Player p = player.getPlayer();
        DungeonInstance instance = p != null ? plugin.getDungeonManager().getActiveInstance(p) : null;
        Party party = p != null ? plugin.getPartyManager().getParty(p.getUniqueId()) : null;

        return switch (params.toLowerCase()) {
            case "in_dungeon" -> String.valueOf(instance != null);
            case "dungeon_id" -> instance != null ? instance.getDungeon().getId() : "None";
            case "current_stage" -> instance != null ? String.valueOf(instance.getCurrentStage()) : "0";
            case "current_wave" -> instance != null ? String.valueOf(instance.getCurrentWave()) : "0";
            case "lives" -> instance != null ? String.valueOf(instance.getLives(p.getUniqueId())) : "0";
            case "rank" -> instance != null ? instance.getPerformanceRank() : "N/A";
            case "party_size" -> party != null ? String.valueOf(party.getMembers().size()) : "0";
            case "party_leader" -> {
                if (party == null)
                    yield "None";
                org.bukkit.OfflinePlayer leader = org.bukkit.Bukkit.getOfflinePlayer(party.getLeader());
                yield leader.getName() != null ? leader.getName() : "Unknown";
            }
            default -> null;
        };
    }
}
