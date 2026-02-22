package id.naturalsmp.naturaldungeon.admin;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.dungeon.Dungeon;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.Color;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Handles the creation and updates of Native TextDisplay holograms for the
 * Dungeon Hub.
 */
public class HubManager {

    private final NaturalDungeon plugin;
    private final File hubFile;
    private FileConfiguration hubConfig;

    private Location hubLocation;

    private final NamespacedKey holoTypeKey;
    private final Map<String, TextDisplay> activeHolograms = new HashMap<>();

    public HubManager(NaturalDungeon plugin) {
        this.plugin = plugin;
        this.hubFile = new File(plugin.getDataFolder(), "hub.yml");
        this.holoTypeKey = new NamespacedKey(plugin, "nd_holo_type");

        loadConfig();
        cleanupOldOrphanedHolograms();
        spawnAllHolograms();
        startUpdateTask();
    }

    private void loadConfig() {
        if (!hubFile.exists()) {
            plugin.saveResource("hub.yml", false);
        }
        hubConfig = YamlConfiguration.loadConfiguration(hubFile);

        if (hubConfig.contains("hub-location")) {
            hubLocation = hubConfig.getLocation("hub-location");
        }
    }

    public void saveConfig() {
        try {
            hubConfig.save(hubFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save hub.yml: " + e.getMessage());
        }
    }

    public void setHubLocation(Location loc) {
        this.hubLocation = loc;
        hubConfig.set("hub-location", loc);
        saveConfig();
    }

    public Location getHubLocation() {
        return hubLocation;
    }

    public void createOrUpdateBoard(String type, Location loc) {
        hubConfig.set("boards." + type, loc);
        saveConfig();

        if (activeHolograms.containsKey(type)) {
            activeHolograms.get(type).remove();
        }

        spawnHologram(type, loc);
    }

    private void cleanupOldOrphanedHolograms() {
        // Find any existing text displays from this plugin and kill them so we don't
        // have duplicates
        for (World world : Bukkit.getWorlds()) {
            for (TextDisplay display : world.getEntitiesByClass(TextDisplay.class)) {
                if (display.getPersistentDataContainer().has(holoTypeKey, PersistentDataType.STRING)) {
                    display.remove();
                }
            }
        }
        activeHolograms.clear();
    }

    private void spawnAllHolograms() {
        if (!hubConfig.contains("boards"))
            return;

        for (String type : hubConfig.getConfigurationSection("boards").getKeys(false)) {
            Location loc = hubConfig.getLocation("boards." + type);
            if (loc != null) {
                spawnHologram(type, loc);
            }
        }
    }

    private void spawnHologram(String type, Location loc) {
        if (loc.getWorld() == null)
            return;
        TextDisplay display = (TextDisplay) loc.getWorld().spawnEntity(loc, EntityType.TEXT_DISPLAY);

        display.getPersistentDataContainer().set(holoTypeKey, PersistentDataType.STRING, type);
        display.setBillboard(Display.Billboard.CENTER);
        display.setAlignment(TextDisplay.TextAlignment.CENTER);
        // Set slightly transparent black background
        display.setBackgroundColor(Color.fromARGB(100, 0, 0, 0));

        // Move slightly up so it's not inside blocks
        loc.add(0, 1.5, 0);

        activeHolograms.put(type, display);
        updateContent(type, display);
    }

    public void reload() {
        cleanupOldOrphanedHolograms();
        loadConfig();
        spawnAllHolograms();
    }

    public void disable() {
        cleanupOldOrphanedHolograms();
    }

    private void startUpdateTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<String, TextDisplay> entry : activeHolograms.entrySet()) {
                    if (entry.getValue() != null && entry.getValue().isValid()) {
                        updateContent(entry.getKey(), entry.getValue());
                    }
                }
            }
        }.runTaskTimer(plugin, 100L, 100L); // Update every 5 seconds
    }

    private void updateContent(String type, TextDisplay display) {
        switch (type.toLowerCase()) {
            case "portal":
                display.text(buildPortalContent());
                break;
            case "stats":
                display.text(buildStatsContent());
                break;
            case "weekly":
                display.text(buildWeeklyContent());
                break;
            case "leaderboard":
                display.text(buildLeaderboardContent());
                break;
            default:
                display.text(Component.text("Unknown Board"));
        }
    }

    private Component buildPortalContent() {
        StringBuilder sb = new StringBuilder();
        sb.append("&#55FF55❂ ᴅᴜɴɢᴇᴏɴ ᴘᴏʀᴛᴀʟꜱ ❂\n");
        sb.append("&f\n");

        for (Dungeon dungeon : plugin.getDungeonManager().getDungeons()) {
            int activePlayers = 0; // would query DungeonManager
            String status = "&#55FF55● OPEN";
            sb.append("&f⚔ ").append(dungeon.getDisplayName()).append("  ").append(status).append("  &7")
                    .append(activePlayers).append("/").append(dungeon.getMaxPlayers()).append(" players\n");
        }
        sb.append("&f\n");
        sb.append("&#FFAA00&l➥ Gunakan /nd menu untuk masuk");
        return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand()
                .deserialize(ChatUtils.colorize(sb.toString()));
    }

    private Component buildStatsContent() {
        StringBuilder sb = new StringBuilder();
        sb.append("&#FFD700📊 ꜱᴇʀᴠᴇʀ ꜱᴛᴀᴛꜱ 📊\n");
        sb.append("&f\n");
        int activeCount = plugin.getDungeonManager().getActiveInstanceCount();
        sb.append("&7Active Dungeons: &f").append(activeCount).append("\n");
        sb.append("&7Season: &e1 (Blood Moon)\n");
        return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand()
                .deserialize(ChatUtils.colorize(sb.toString()));
    }

    private Component buildWeeklyContent() {
        StringBuilder sb = new StringBuilder();
        sb.append("&#AA00FF🌟 ᴡᴇᴇᴋʟʏ ᴄʜᴀʟʟᴇɴɢᴇ 🌟\n");
        sb.append("&f\n");
        sb.append("&7Target: &fClears\n");
        sb.append("&7Progress: &a0/10\n");
        sb.append("&7Reward: &e1000 NaturalCoin\n");
        return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand()
                .deserialize(ChatUtils.colorize(sb.toString()));
    }

    private Component buildLeaderboardContent() {
        StringBuilder sb = new StringBuilder();
        sb.append("&#FF4444👑 ᴛᴏᴘ 10 ᴅᴜɴɢᴇᴏɴ ᴄʟᴇᴀʀꜱ 👑\n");
        sb.append("&f\n");
        // We will just show the top times globally or for the first dungeon as an
        // example
        // Or alternatively, summarize top players
        boolean foundAny = false;
        if (!plugin.getDungeonManager().getDungeons().isEmpty()) {
            Dungeon firstDungeon = plugin.getDungeonManager().getDungeons().iterator().next();
            List<Map<?, ?>> topEntries = plugin.getLeaderboardManager().getTopEntries(firstDungeon.getId());
            if (topEntries != null && !topEntries.isEmpty()) {
                foundAny = true;
                sb.append("&7Dungeon: &e").append(firstDungeon.getDisplayName()).append("\n");
                int rank = 1;
                for (Map<?, ?> entry : topEntries) {
                    if (rank > 10)
                        break;
                    List<String> players = (List<String>) entry.get("players");
                    long timeMs = (long) entry.get("time");
                    String timeStr = ChatUtils.formatTime(timeMs / 1000);
                    sb.append("&e").append(rank).append(". &f").append(String.join(", ", players))
                            .append(" &7(").append(timeStr).append(")\n");
                    rank++;
                }
            }
        }

        if (!foundAny) {
            sb.append("&7Belum ada rekor yang tercatat!\n");
        }

        return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand()
                .deserialize(ChatUtils.colorize(sb.toString()));
    }
}
