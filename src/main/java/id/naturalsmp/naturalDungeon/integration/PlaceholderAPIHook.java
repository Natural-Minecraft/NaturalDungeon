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

        String lowerParams = params.toLowerCase();
        if (lowerParams.startsWith("leaderboard_")) {
            String[] parts = params.split("_");
            if (parts.length >= 4) {
                String type = parts[parts.length - 1];
                int rank;
                try {
                    rank = Integer.parseInt(parts[parts.length - 2]);
                } catch (NumberFormatException e) {
                    return "InvalidRank";
                }

                StringBuilder dungeonIdBuilder = new StringBuilder();
                for (int i = 1; i < parts.length - 2; i++) {
                    dungeonIdBuilder.append(parts[i]);
                    if (i < parts.length - 3)
                        dungeonIdBuilder.append("_");
                }
                String dungeonId = dungeonIdBuilder.toString();

                java.util.List<java.util.Map<?, ?>> topEntries = plugin.getLeaderboardManager()
                        .getTopEntries(dungeonId);
                if (topEntries == null || topEntries.isEmpty() || rank < 1 || rank > topEntries.size()) {
                    return type.equalsIgnoreCase("name") ? "Empty" : "00:00";
                }

                java.util.Map<?, ?> entry = topEntries.get(rank - 1);
                if (type.equalsIgnoreCase("name")) {
                    java.util.List<String> players = (java.util.List<String>) entry.get("players");
                    return String.join(", ", players);
                } else if (type.equalsIgnoreCase("time")) {
                    long timeMs = (long) entry.get("time");
                    return id.naturalsmp.naturaldungeon.utils.ChatUtils.formatTime(timeMs / 1000);
                }
            }
        }

        return switch (lowerParams) {
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
