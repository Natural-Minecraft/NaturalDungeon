package id.naturalsmp.naturaldungeon.admin;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.dungeon.Dungeon;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import id.naturalsmp.naturaldungeon.utils.GUIUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class SetupManager implements Listener {

    private final NaturalDungeon plugin;
    private final Map<UUID, EditorSession> sessions = new HashMap<>();
    private final NamespacedKey toolKey;

    public SetupManager(NaturalDungeon plugin) {
        this.plugin = plugin;
        this.toolKey = new NamespacedKey(plugin, "setup_tool");
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        startVisualizer();
    }

    // ─── Entry Points ───────────────────────────────────────────────

    public void enterStageEditor(Player player, Dungeon dungeon, int stageNum) {
        EditorSession session = new EditorSession(player, dungeon, stageNum, player.getInventory().getContents());
        sessions.put(player.getUniqueId(), session);

        player.getInventory().clear();
        giveEditorTools(player);

        player.sendMessage(ChatUtils.colorize("&#55FF55&l✔ &7Entered Editor Mode: &#FFD700Stage " + stageNum));
        player.sendMessage(ChatUtils.colorize("&#FFAA00⚙ &7Use the hotbar tools to edit the stage."));
        GUIUtils.playSuccessSound(player);

        // Auto-teleport if stage safezone exists
        Dungeon.Stage stage = dungeon.getStages().stream().filter(s -> s.getNumber() == stageNum).findFirst()
                .orElse(null);
        if (stage != null) {
            Dungeon.StageLocation loc = stage.getLocation(1);
            plugin.getWorldGuardHook().getRegionCenter(Bukkit.getWorld(dungeon.getWorld()), loc.getSafeZone())
                    .ifPresent(player::teleport);
        }
    }

    public void exitSetupMode(Player player) {
        EditorSession session = sessions.remove(player.getUniqueId());
        if (session != null) {
            session.cleanup();
            player.getInventory().setContents(session.savedInventory);
            player.sendMessage(ChatUtils.colorize("&#FFAA00⚙ &7Exited Setup Mode."));
            GUIUtils.playClickSound(player);
        }
    }

    // ─── Tools ──────────────────────────────────────────────────────

    private void giveEditorTools(Player player) {
        player.getInventory().setItem(0,
                createTool(Material.BLAZE_ROD, "&#55FF55&l📐 ꜱᴀꜰᴇᴢᴏɴᴇ ᴡᴀɴᴅ", "REGION_WAND",
                        "&7L-Click: &#55CCFF&lPos 1",
                        "&7R-Click: &#FFAA00&lPos 2"));
        player.getInventory().setItem(1,
                createTool(Material.EMERALD, "&#55FF55&l✔ ꜱᴀᴠᴇ ꜱᴀꜰᴇᴢᴏɴᴇ", "SAVE_REGION",
                        "&7Click to confirm safezone region."));

        player.getInventory().setItem(3,
                createTool(Material.STICK, "&#FF5555&l⚔ ᴀʀᴇɴᴀ ᴡᴀɴᴅ", "ARENA_WAND",
                        "&7L-Click: &#55CCFF&lPos 1",
                        "&7R-Click: &#FFAA00&lPos 2"));
        player.getInventory().setItem(4,
                createTool(Material.REDSTONE_BLOCK, "&#FF5555&l✔ ꜱᴀᴠᴇ ᴀʀᴇɴᴀ", "SAVE_ARENA",
                        "&7Click to confirm arena region."));

        player.getInventory().setItem(6,
                createTool(Material.WITHER_SKELETON_SKULL, "&#AA44FF&l🐉 ꜱᴇᴛ ʙᴏꜱꜱ ꜱᴘᴀᴡɴ", "SET_BOSS",
                        "&7R-Click on block to set."));

        player.getInventory().setItem(7,
                createTool(Material.BARRIER, "&#FF5555&l✕ ᴇxɪᴛ ᴇᴅɪᴛᴏʀ", "EXIT",
                        "&7Save & Exit editor mode."));
        player.getInventory().setItem(8,
                createTool(Material.COMMAND_BLOCK, "&#FFD700&l⚙ ᴏᴘᴇɴ ɢᴜɪ", "OPEN_GUI",
                        "&7Click to open editor GUI."));
    }

    private ItemStack createTool(Material mat, String name, String id, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatUtils.colorize(name));
            meta.getPersistentDataContainer().set(toolKey, PersistentDataType.STRING, id);
            List<String> loreList = new ArrayList<>();
            loreList.add(ChatUtils.colorize(GUIUtils.separator()));
            for (String s : lore)
                loreList.add(ChatUtils.colorize(s));
            meta.setLore(loreList);
            item.setItemMeta(meta);
        }
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
            case "OPEN_GUI" -> new id.naturalsmp.naturaldungeon.editor.DungeonListEditorGUI(plugin).open(p);
            case "EXIT" -> exitSetupMode(p);
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
            p.sendMessage(ChatUtils.colorize("&#FF5555✖ &7You are not in setup mode!"));
            GUIUtils.playErrorSound(p);
            return;
        }

        String posColor = posNum == 1 ? "&#55CCFF" : "&#FFAA00";

        if (type.equalsIgnoreCase("safezone")) {
            if (posNum == 1) {
                session.pos1 = loc;
                session.safe1Holo = createOrMoveHolo(session.safe1Holo, loc, "&#55FF55📐 Safezone Pos 1");
            } else {
                session.pos2 = loc;
                session.safe2Holo = createOrMoveHolo(session.safe2Holo, loc, "&#55FF55📐 Safezone Pos 2");
            }
            p.sendMessage(ChatUtils.colorize("&#55FF55📐 &7Safezone Pos " + posNum + ": " + posColor + formatLoc(loc)));
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, posNum == 1 ? 1f : 1.5f);
        } else if (type.equalsIgnoreCase("arena")) {
            if (posNum == 1) {
                session.arenaPos1 = loc;
                session.arena1Holo = createOrMoveHolo(session.arena1Holo, loc, "&#FF5555⚔ Arena Pos 1");
            } else {
                session.arenaPos2 = loc;
                session.arena2Holo = createOrMoveHolo(session.arena2Holo, loc, "&#FF5555⚔ Arena Pos 2");
            }
            p.sendMessage(ChatUtils.colorize("&#FF5555⚔ &7Arena Pos " + posNum + ": " + posColor + formatLoc(loc)));
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, posNum == 1 ? 0.8f : 1.2f);
        }
    }

    private TextDisplay createOrMoveHolo(TextDisplay current, Location loc, String text) {
        if (current == null || current.isDead()) {
            current = loc.getWorld().spawn(loc.clone().add(0.5, 1.5, 0.5), TextDisplay.class);
            current.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
            current.setBackgroundColor(org.bukkit.Color.fromARGB(100, 0, 0, 0));
        } else {
            current.teleport(loc.clone().add(0.5, 1.5, 0.5));
        }
        current.setText(ChatUtils.colorize(text));
        return current;
    }

    // ─── Save Operations ────────────────────────────────────────────

    private void saveSafeZone(Player p, EditorSession session) {
        if (session.pos1 == null || session.pos2 == null) {
            p.sendMessage(ChatUtils.colorize("&#FF5555✖ &7Set both Safezone Pos 1 and Pos 2 first!"));
            GUIUtils.playErrorSound(p);
            return;
        }

        String regionName = session.dungeon.getId() + "_stage" + session.stage + "_safe";
        boolean created = plugin.getWorldGuardHook().createRegion(regionName, session.pos1, session.pos2);
        saveConfig(session.dungeon.getId(), "stages." + session.stage + ".safe-zone", regionName);

        p.sendMessage(ChatUtils.colorize("&#55FF55✔ &7Safezone saved! &8(Region: " + regionName + ")"));
        if (created)
            p.sendMessage(ChatUtils.colorize("&#55FF55✔ &7WG Region created!"));
        else
            p.sendMessage(ChatUtils.colorize("&#FFAA00⚠ &7WG Region creation failed (may already exist)."));
        GUIUtils.playSuccessSound(p);
    }

    private void saveArenaRegion(Player p, EditorSession session) {
        if (session.arenaPos1 == null || session.arenaPos2 == null) {
            p.sendMessage(ChatUtils.colorize("&#FF5555✖ &7Set both Arena Pos 1 and Pos 2 first!"));
            GUIUtils.playErrorSound(p);
            return;
        }

        String regionName = session.dungeon.getId() + "_stage" + session.stage + "_arena";
        boolean created = plugin.getWorldGuardHook().createRegion(regionName, session.arenaPos1, session.arenaPos2);
        saveConfig(session.dungeon.getId(), "stages." + session.stage + ".arena-region", regionName);

        p.sendMessage(ChatUtils.colorize("&#FF5555✔ &7Arena saved! &8(Region: " + regionName + ")"));
        if (created)
            p.sendMessage(ChatUtils.colorize("&#55FF55✔ &7WG Region created!"));
        else
            p.sendMessage(ChatUtils.colorize("&#FFAA00⚠ &7WG Region creation failed."));
        GUIUtils.playSuccessSound(p);
    }

    private void saveBossSpawn(Player p, EditorSession session, Location loc) {
        List<Double> coords = Arrays.asList(loc.getX(), loc.getY(), loc.getZ());
        session.bossHolo = createOrMoveHolo(session.bossHolo, loc, "&#AA44FF🐉 Boss Spawn\n&7Stage " + session.stage);
        saveConfig(session.dungeon.getId(), "stages." + session.stage + ".boss.spawn-location", coords);
        saveConfig(session.dungeon.getId(), "stages." + session.stage + ".boss.id", "ZOMBIE");
        p.sendMessage(ChatUtils.colorize("&#AA44FF🐉 &7Boss Spawn set: &#FFAA00" + formatLoc(loc)));
        GUIUtils.playSuccessSound(p);
    }

    private void saveConfig(String dungeonId, String path, Object value) {
        File file = new File(plugin.getDataFolder(), "dungeons/" + dungeonId + ".yml");
        org.bukkit.configuration.file.YamlConfiguration config = org.bukkit.configuration.file.YamlConfiguration
                .loadConfiguration(file);
        config.set(path, value);
        try {
            config.save(file);
            plugin.getDungeonManager().loadDungeons();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String formatLoc(Location loc) {
        return loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    // ─── Visualizer ─────────────────────────────────────────────────

    private void startVisualizer() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (EditorSession session : sessions.values()) {
                Player p = Bukkit.getPlayer(session.playerUUID);
                if (p == null)
                    continue;

                // Draw position markers
                if (session.pos1 != null)
                    drawParticle(p, session.pos1, Color.LIME);
                if (session.pos2 != null)
                    drawParticle(p, session.pos2, Color.AQUA);
                if (session.arenaPos1 != null)
                    drawParticle(p, session.arenaPos1, Color.RED);
                if (session.arenaPos2 != null)
                    drawParticle(p, session.arenaPos2, Color.ORANGE);

                // Draw box outlines between pos1-pos2
                if (session.pos1 != null && session.pos2 != null)
                    drawBoxOutline(p, session.pos1, session.pos2, Color.LIME);
                if (session.arenaPos1 != null && session.arenaPos2 != null)
                    drawBoxOutline(p, session.arenaPos1, session.arenaPos2, Color.RED);
            }
        }, 10L, 10L);
    }

    private void drawParticle(Player p, Location loc, Color color) {
        p.spawnParticle(Particle.DUST, loc.clone().add(0.5, 1.2, 0.5), 1,
                new Particle.DustOptions(color, 1.0f));
    }

    private void drawBoxOutline(Player p, Location pos1, Location pos2, Color color) {
        double minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        double minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        double minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        double maxX = Math.max(pos1.getBlockX(), pos2.getBlockX()) + 1;
        double maxY = Math.max(pos1.getBlockY(), pos2.getBlockY()) + 1;
        double maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ()) + 1;

        Particle.DustOptions dust = new Particle.DustOptions(color, 0.6f);
        World world = pos1.getWorld();
        double step = 1.0;

        // Draw 12 edges of the cuboid
        for (double x = minX; x <= maxX; x += step) {
            p.spawnParticle(Particle.DUST, new Location(world, x, minY, minZ), 1, dust);
            p.spawnParticle(Particle.DUST, new Location(world, x, maxY, minZ), 1, dust);
            p.spawnParticle(Particle.DUST, new Location(world, x, minY, maxZ), 1, dust);
            p.spawnParticle(Particle.DUST, new Location(world, x, maxY, maxZ), 1, dust);
        }
        for (double y = minY; y <= maxY; y += step) {
            p.spawnParticle(Particle.DUST, new Location(world, minX, y, minZ), 1, dust);
            p.spawnParticle(Particle.DUST, new Location(world, maxX, y, minZ), 1, dust);
            p.spawnParticle(Particle.DUST, new Location(world, minX, y, maxZ), 1, dust);
            p.spawnParticle(Particle.DUST, new Location(world, maxX, y, maxZ), 1, dust);
        }
        for (double z = minZ; z <= maxZ; z += step) {
            p.spawnParticle(Particle.DUST, new Location(world, minX, minY, z), 1, dust);
            p.spawnParticle(Particle.DUST, new Location(world, maxX, minY, z), 1, dust);
            p.spawnParticle(Particle.DUST, new Location(world, minX, maxY, z), 1, dust);
            p.spawnParticle(Particle.DUST, new Location(world, maxX, maxY, z), 1, dust);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        exitSetupMode(e.getPlayer());
    }

    public boolean isInSetup(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    // ─── Editor Session ─────────────────────────────────────────────

    public static class EditorSession {
        final Dungeon dungeon;
        final int stage;
        final ItemStack[] savedInventory;
        final UUID playerUUID;
        Location pos1, pos2;
        Location arenaPos1, arenaPos2;

        TextDisplay safe1Holo, safe2Holo;
        TextDisplay arena1Holo, arena2Holo;
        TextDisplay bossHolo;

        public EditorSession(Player p, Dungeon dungeon, int stage, ItemStack[] savedInventory) {
            this.dungeon = dungeon;
            this.stage = stage;
            this.savedInventory = savedInventory;
            this.playerUUID = p.getUniqueId();
        }

        public void cleanup() {
            if (safe1Holo != null)
                safe1Holo.remove();
            if (safe2Holo != null)
                safe2Holo.remove();
            if (arena1Holo != null)
                arena1Holo.remove();
            if (arena2Holo != null)
                arena2Holo.remove();
            if (bossHolo != null)
                bossHolo.remove();
        }
    }
}
