package id.naturalsmp.naturaldungeon.admin;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.dungeon.Dungeon;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class SetupManager implements Listener {

    private final NaturalDungeon plugin;
    private final SetupGUI setupGUI;
    private final Map<UUID, EditorSession> sessions = new HashMap<>();
    private final NamespacedKey toolKey;

    public SetupManager(NaturalDungeon plugin) {
        this.plugin = plugin;
        this.setupGUI = new SetupGUI(plugin, this);
        this.toolKey = new NamespacedKey(plugin, "setup_tool");
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getServer().getPluginManager().registerEvents(setupGUI, plugin);
        startVisualizer();
    }

    // --- Entry Points ---

    public void openDashboard(Player player, String dungeonId) {
        Dungeon dungeon = plugin.getDungeonManager().getDungeon(dungeonId);
        if (dungeon == null) {
            player.sendMessage(ChatUtils.colorize("&cDungeon not found: " + dungeonId));
            return;
        }
        setupGUI.open(player, dungeon);
    }

    public void enterStageEditor(Player player, Dungeon dungeon, int stageNum) {
        // Save Inventory
        EditorSession session = new EditorSession(dungeon, stageNum, player.getInventory().getContents());
        sessions.put(player.getUniqueId(), session);

        player.getInventory().clear();
        giveEditorTools(player);

        player.sendMessage(ChatUtils.colorize("&aEntered Editor Mode for &eStage " + stageNum));
        player.sendMessage(ChatUtils.colorize("&7Use the hotbar tools to edit the stage."));

        // Auto-teleport if stage safezone exists
        Dungeon.Stage stage = dungeon.getStages().stream().filter(s -> s.getNumber() == stageNum).findFirst()
                .orElse(null);
        if (stage != null) {
            // Default to modifying Instance 1
            Dungeon.StageLocation loc = stage.getLocation(1);
            // Try to resolve region
            plugin.getWorldGuardHook().getRegionCenter(Bukkit.getWorld(dungeon.getWorld()), loc.getSafeZone())
                    .ifPresent(player::teleport);
        }
    }

    public void exitSetupMode(Player player) {
        EditorSession session = sessions.remove(player.getUniqueId());
        if (session != null) {
            player.getInventory().setContents(session.savedInventory);
            player.sendMessage(ChatUtils.colorize("&eExited Setup Mode."));
        }
    }

    // --- Tools & Logic ---

    private void giveEditorTools(Player player) {
        player.getInventory().setItem(0,
                createTool(Material.BLAZE_ROD, "&aSafezone Wand", "REGION_WAND", "&7L-Click: Pos 1",
                        "&7R-Click: Pos 2"));
        player.getInventory().setItem(1,
                createTool(Material.EMERALD, "&aSave Safezone", "SAVE_REGION", "&7Click to confirm safezone"));

        player.getInventory().setItem(3,
                createTool(Material.STICK, "&cArena Wand", "ARENA_WAND", "&7L-Click: Pos 1", "&7R-Click: Pos 2"));
        player.getInventory().setItem(4,
                createTool(Material.REDSTONE_BLOCK, "&cSave Arena", "SAVE_ARENA", "&7Click to confirm arena region"));

        player.getInventory().setItem(6, createTool(Material.WITHER_SKELETON_SKULL, "&eSet Boss Spawn", "SET_BOSS",
                "&7R-Click on block to set"));
        player.getInventory().setItem(8, createTool(Material.BARRIER, "&cBack to Dashboard", "EXIT", "&7Save & Exit"));
    }

    private ItemStack createTool(Material mat, String name, String id, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.colorize(name));
        meta.getPersistentDataContainer().set(toolKey, PersistentDataType.STRING, id);
        List<String> l = new ArrayList<>();
        for (String s : lore)
            l.add(ChatUtils.colorize(s));
        meta.setLore(l);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        EditorSession session = sessions.get(p.getUniqueId());
        if (session == null)
            return;

        ItemStack item = e.getItem();
        if (item == null || !item.hasItemMeta())
            return;
        String toolId = item.getItemMeta().getPersistentDataContainer().get(toolKey, PersistentDataType.STRING);
        if (toolId == null)
            return;

        e.setCancelled(true);

        switch (toolId) {
            case "REGION_WAND" -> handleWand(p, session, e, "safezone");
            case "SAVE_REGION" -> saveSafeZone(p, session);
            case "ARENA_WAND" -> handleWand(p, session, e, "arena");
            case "SAVE_ARENA" -> saveArenaRegion(p, session);
            case "SET_BOSS" -> {
                if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR) {
                    Location loc = e.getClickedBlock() != null ? e.getClickedBlock().getLocation().add(0.5, 1, 0.5)
                            : p.getLocation();
                    saveBossSpawn(p, session, loc);
                }
            }
            case "EXIT" -> {
                String dungeonId = session.dungeon.getId();
                exitSetupMode(p);
                openDashboard(p, dungeonId);
            }
        }
    }

    private void handleWand(Player p, EditorSession session, PlayerInteractEvent e, String type) {
        if (e.getClickedBlock() == null)
            return;
        Location loc = e.getClickedBlock().getLocation();
        if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
            setPos(p, type, 1, loc);
        } else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            setPos(p, type, 2, loc);
        }
    }

    public void setPos(Player p, String type, int posNum, Location loc) {
        EditorSession session = sessions.get(p.getUniqueId());
        if (session == null) {
            p.sendMessage(ChatUtils.colorize("&cYou are not in setup mode!"));
            return;
        }

        if (type.equalsIgnoreCase("safezone")) {
            if (posNum == 1)
                session.pos1 = loc;
            else
                session.pos2 = loc;
            p.sendMessage(ChatUtils.colorize("&aSafezone Pos " + posNum + " Set: &7" + formatLoc(loc)));
        } else if (type.equalsIgnoreCase("arena")) {
            if (posNum == 1)
                session.arenaPos1 = loc;
            else
                session.arenaPos2 = loc;
            p.sendMessage(ChatUtils.colorize("&cArena Pos " + posNum + " Set: &7" + formatLoc(loc)));
        }
    }

    private void saveSafeZone(Player p, EditorSession session) {
        if (session.pos1 == null || session.pos2 == null) {
            p.sendMessage(ChatUtils.colorize("&cError: Set both Safezone Pos 1 and Pos 2 first!"));
            return;
        }

        String regionName = session.dungeon.getId() + "_stage" + session.stage + "_safe";

        boolean created = plugin.getWorldGuardHook().createRegion(regionName, session.pos1, session.pos2);
        saveConfig(session.dungeon.getId(), "stages." + session.stage + ".safe-zone", regionName);

        p.sendMessage(ChatUtils.colorize("&aSafezone Config Updated! &7(Region: " + regionName + ")"));
        if (created)
            p.sendMessage(ChatUtils.colorize("&a✔ WG Region created!"));
        else
            p.sendMessage(ChatUtils.colorize("&c⚠ WG Region creation failed (may already exist)."));
    }

    private void saveArenaRegion(Player p, EditorSession session) {
        if (session.arenaPos1 == null || session.arenaPos2 == null) {
            p.sendMessage(ChatUtils.colorize("&cError: Set both Arena Pos 1 and Pos 2 first!"));
            return;
        }

        String regionName = session.dungeon.getId() + "_stage" + session.stage + "_arena";

        boolean created = plugin.getWorldGuardHook().createRegion(regionName, session.arenaPos1, session.arenaPos2);
        saveConfig(session.dungeon.getId(), "stages." + session.stage + ".arena-region", regionName);

        p.sendMessage(ChatUtils.colorize("&cArena Config Updated! &7(Region: " + regionName + ")"));
        if (created)
            p.sendMessage(ChatUtils.colorize("&a✔ WG Region created!"));
        else
            p.sendMessage(ChatUtils.colorize("&c⚠ WG Region creation failed."));
    }

    private void saveBossSpawn(Player p, EditorSession session, Location loc) {
        List<Double> coords = Arrays.asList(loc.getX(), loc.getY(), loc.getZ());
        saveConfig(session.dungeon.getId(), "stages." + session.stage + ".boss.spawn-location", coords);
        saveConfig(session.dungeon.getId(), "stages." + session.stage + ".boss.id", "ZOMBIE"); // Default
        p.sendMessage(ChatUtils.colorize("&aBoss Spawn Set! &7" + formatLoc(loc)));
    }

    private void saveConfig(String dungeonId, String path, Object value) {
        File file = new File(plugin.getDataFolder(), "dungeons/" + dungeonId + ".yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set(path, value);
        try {
            config.save(file);
            // Reload dungeon to update cache
            plugin.getDungeonManager().loadDungeons();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String formatLoc(Location loc) {
        return loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    // --- Visualizer ---

    private void startVisualizer() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (EditorSession session : sessions.values()) {
                Player p = Bukkit.getPlayer(session.playerUUID);
                if (p == null)
                    continue;

                // Draw Pos 1 & 2
                if (session.pos1 != null)
                    drawParticle(p, session.pos1, Color.LIME);
                if (session.pos2 != null)
                    drawParticle(p, session.pos2, Color.BLUE);

                if (session.arenaPos1 != null)
                    drawParticle(p, session.arenaPos1, Color.RED);
                if (session.arenaPos2 != null)
                    drawParticle(p, session.arenaPos2, Color.ORANGE);

                // Draw Box interaction if valid
                if (session.pos1 != null && session.pos2 != null) {
                    // Simple line drawing logic could go here
                }
            }
        }, 10L, 10L);
    }

    private void drawParticle(Player p, Location loc, Color color) {
        // Using Redstone particle for colored dust
        p.spawnParticle(Particle.DUST, loc.clone().add(0.5, 1.2, 0.5), 1,
                new Particle.DustOptions(color, 1.0f));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        exitSetupMode(e.getPlayer());
    }

    public static class EditorSession {
        final Dungeon dungeon;
        final int stage;
        final ItemStack[] savedInventory;
        final UUID playerUUID;
        final UUID playerUUID;
        Location pos1, pos2; // Safezone
        Location arenaPos1, arenaPos2; // Arena

        public EditorSession(Dungeon dungeon, int stage, ItemStack[] savedInventory) {
            this.dungeon = dungeon;
            this.stage = stage;
            this.savedInventory = savedInventory;
            this.playerUUID = UUID.randomUUID(); // Just placeholder, key is map key
        }
    }
}
