package id.naturalsmp.naturaldungeon.dungeon;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.loot.LootManager;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import id.naturalsmp.naturaldungeon.utils.ConfigUtils;
import id.naturalsmp.naturaldungeon.wave.WaveManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.*;

public class DungeonInstance {

    private final NaturalDungeon plugin;
    private final Dungeon dungeon;
    private final DungeonDifficulty difficulty;
    private final int instanceId;
    private final List<UUID> participants;
    private final Map<UUID, Integer> lives = new HashMap<>();
    private final Map<UUID, Location> returnLocations = new HashMap<>();
    private final Map<UUID, Boolean> inSafeZone = new HashMap<>();
    private final Set<UUID> invulnerable = new HashSet<>(); // Respawn invulnerability

    private WaveManager waveManager;
    private LootManager lootManager;
    private BuffChoiceGUI buffChoiceGUI; // Singleton reference
    private World dungeonWorld;

    private int currentStage = 0;
    private int currentWave = 0;
    private long startTime;
    private boolean active = false;
    private boolean bossSpawned = false;
    private BukkitTask safeZoneCheckTask;
    private BukkitTask hudTask; // Fixed: now tracked for proper cancellation
    private BukkitTask timerTask; // Dungeon time limit task
    private BossBar bossBar;

    private int totalDeaths = 0;
    private List<ItemStack> collectedLoot = new ArrayList<>();
    private Location lootChestLocation;

    public DungeonInstance(NaturalDungeon plugin, Dungeon dungeon, DungeonDifficulty difficulty, int instanceId,
            List<UUID> participants) {
        this.plugin = plugin;
        this.dungeon = dungeon;
        this.difficulty = difficulty;
        this.instanceId = instanceId;
        this.participants = new ArrayList<>(participants);
        this.lootManager = new LootManager(plugin);
        this.buffChoiceGUI = plugin.getBuffChoiceGUI();

        int maxLives = difficulty.getMaxDeaths();
        if (maxLives <= 0)
            maxLives = ConfigUtils.getInt("dungeon.default-lives"); // fallback

        for (UUID uuid : participants) {
            lives.put(uuid, maxLives);
            inSafeZone.put(uuid, true);
        }
    }

    // Keep old constructor for compat, default to instance 1
    public DungeonInstance(NaturalDungeon plugin, Dungeon dungeon, List<UUID> participants) {
        this(plugin, dungeon, dungeon.getDifficulty("normal"), 1, participants);
    }

    public void start() {
        dungeonWorld = Bukkit.getWorld(dungeon.getWorld());
        if (dungeonWorld == null) {
            broadcast(ConfigUtils.getMessage("dungeon.world-not-found", "%world%", dungeon.getWorld()));
            plugin.getDungeonManager().endDungeon(this, false);
            return;
        }

        this.waveManager = new WaveManager(plugin, this, dungeonWorld);
        this.active = true;
        this.startTime = System.currentTimeMillis();
        this.currentStage = 1;
        this.currentWave = 0;

        // Countdown
        for (int i = 5; i >= 1; i--) {
            int sec = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                String color = sec <= 3 ? "&c" : "&e";
                broadcastTitle(color + sec, "&7Dungeon Starting...", 0, 20, 10);
                playSound(Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f + (5 - sec) * 0.2f);
            }, (5 - i) * 20L);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            teleportToStage(currentStage);
            broadcastTitle("&a&lDUNGEON STARTED", "&7Good luck, stay alive!", 10, 40, 20);
            playSound(Sound.ENTITY_WITHER_SPAWN, 1f);
            startSafeZoneCheck();
            startHUDTask();
            startTimerLimit();
            Bukkit.getScheduler().runTaskLater(plugin, () -> startStage(currentStage), 60L);
        }, 100L);
    }

    /**
     * Start a configurable time limit for the dungeon.
     * If time runs out, dungeon is force-ended.
     */
    private void startTimerLimit() {
        int timeLimitSeconds = ConfigUtils.getInt("dungeon.time-limit", 0);
        if (timeLimitSeconds <= 0)
            return; // 0 = no limit
        timerTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!active)
                return;
            broadcast(ConfigUtils.getMessage("dungeon.force-ended"));
            broadcastTitle("&c&lTIME'S UP", "&7Dungeon time limit reached!", 10, 60, 20);
            playSound(Sound.ENTITY_WITHER_DEATH, 0.5f);
            plugin.getDungeonManager().endDungeon(this, false);
        }, timeLimitSeconds * 20L);
    }

    private void teleportToStage(int stageNum) {
        Dungeon.Stage stage = getStage(stageNum);
        if (stage == null)
            return;

        Dungeon.StageLocation loc = stage.getLocation(instanceId);
        Optional<Location> spawnOpt = plugin.getWorldGuardHook().getRegionSpawnPoint(dungeonWorld, loc.getSafeZone());
        Location spawn = spawnOpt.orElse(dungeonWorld.getSpawnLocation());

        for (UUID uuid : participants) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                if (stageNum == 1 && !returnLocations.containsKey(uuid)) {
                    returnLocations.put(uuid, player.getLocation());
                }
                player.teleport(spawn);
                inSafeZone.put(uuid, true);
            }
        }
    }

    private void startStage(int stageNum) {
        if (!active)
            return;
        Dungeon.Stage stage = getStage(stageNum);
        if (stage == null) {
            plugin.getDungeonManager().endDungeon(this, true);
            return;
        }
        broadcastTitle("&6&lSTAGE " + stageNum, "&7Get ready for the waves!", 10, 40, 10);
        playSound(Sound.EVENT_RAID_HORN, 0.6f);

        // Open Buff Choice for all players
        for (UUID uuid : participants) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null)
                buffChoiceGUI.open(p, this);
        }

        currentWave = 0;
        bossSpawned = false;
        Bukkit.getScheduler().runTaskLater(plugin, this::startNextWave, 60L);
    }

    private void startNextWave() {
        if (!active)
            return;
        Dungeon.Stage stage = getStage(currentStage);
        if (stage == null)
            return;

        List<Dungeon.Wave> waves = stage.getWaves();
        if (currentWave >= waves.size()) {
            if (stage.hasBoss() && !bossSpawned)
                spawnBoss(stage);
            else if (!stage.hasBoss() || bossSpawned)
                onStageComplete();
            return;
        }

        currentWave++;
        Dungeon.Wave wave = waves.get(currentWave - 1);

        // 5% chance for Blood Moon
        boolean bloodMoon = Math.random() < 0.05;
        if (bloodMoon) {
            broadcastTitle("&4&lBLOOD MOON WAVE", "&7Monsters are empowered!", 10, 40, 10);
            playSound(Sound.ENTITY_WITHER_SPAWN, 1.2f);
            updateBossBar("&4&lBLOOD MOON Progress", 1.0, org.bukkit.boss.BarColor.RED);
        } else {
            broadcastTitle("&e WAVE " + currentWave, "&7Kill all mobs!", 5, 20, 5);
            playSound(Sound.ENTITY_ZOMBIE_HORSE_DEATH, 0.8f);
        }

        Dungeon.StageLocation loc = stage.getLocation(instanceId);
        String regionName = loc.getArenaRegion(); // Prioritize Arena Region
        if (regionName == null || regionName.isEmpty()) {
            regionName = loc.getSafeZone(); // Fallback to SafeZone
        }

        Optional<Location> centerOpt = plugin.getWorldGuardHook().getRegionCenter(dungeonWorld, regionName);
        Location center = centerOpt.orElse(dungeonWorld.getSpawnLocation()).add(15, 0, 15);

        waveManager.spawnWave(wave, center, participants.size(), bloodMoon);
    }

    public void onWaveComplete() {
        if (!active)
            return;
        broadcastTitle("&a✔ WAVE CLEAR", "", 5, 20, 5);
        playSound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f);

        Dungeon.Stage stage = getStage(currentStage);
        if (stage == null)
            return;

        if (currentWave < stage.getWaves().size()) {
            int delay = stage.getWaves().get(currentWave).getDelay();
            int delaySec = delay > 0 ? delay : 3;
            long delayTicks = delaySec * 20L;

            // Wave countdown timer for players
            for (int i = delaySec; i >= 1; i--) {
                int sec = i;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!active)
                        return;
                    String color = sec <= 2 ? "&c" : "&e";
                    broadcastTitle(color + "Wave " + (currentWave + 1) + " in " + sec + "s", "&7Prepare yourself!", 0,
                            25, 5);
                }, (delaySec - i) * 20L);
            }

            Bukkit.getScheduler().runTaskLater(plugin, this::startNextWave, delayTicks);
        } else if (stage.hasBoss() && !bossSpawned) {
            // Boss countdown
            broadcastTitle("&d&lBOSS INCOMING", "&7Prepare for battle!", 10, 40, 10);
            Bukkit.getScheduler().runTaskLater(plugin, () -> spawnBoss(stage), 60L);
        } else {
            playSound(Sound.ENTITY_PLAYER_LEVELUP, 1f);
            onStageComplete();
        }
    }

    private void spawnBoss(Dungeon.Stage stage) {
        if (!active || bossSpawned)
            return;

        bossSpawned = true;
        broadcastTitle("&d&lBOSS SPAWNED", "&7Defeat the dungeon guardian!", 10, 40, 10);
        playSound(Sound.ENTITY_ENDER_DRAGON_GROWL, 1f);
        updateBossBar("&d&lBOSS HP", 1.0, org.bukkit.boss.BarColor.PURPLE);

        // Boss VFX
        if (dungeonWorld != null) {
            Dungeon.StageLocation bossLocDetails = stage.getLocation(instanceId);
            List<Double> bossCoords = bossLocDetails.getBossSpawnLocation();
            Location bossVfxLoc;
            if (bossCoords != null && bossCoords.size() >= 3) {
                bossVfxLoc = new Location(dungeonWorld, bossCoords.get(0), bossCoords.get(1), bossCoords.get(2));
            } else {
                String regionName = bossLocDetails.getArenaRegion();
                if (regionName == null || regionName.isEmpty())
                    regionName = bossLocDetails.getSafeZone();

                bossVfxLoc = plugin.getWorldGuardHook().getRegionCenter(dungeonWorld, regionName)
                        .orElse(dungeonWorld.getSpawnLocation()).add(20, 0, 20);
            }
            dungeonWorld.strikeLightningEffect(bossVfxLoc);
        }

        Dungeon.StageLocation loc = stage.getLocation(instanceId);
        List<Double> bossLoc = loc.getBossSpawnLocation();
        Location spawn;
        if (bossLoc != null && bossLoc.size() >= 3) {
            spawn = new Location(dungeonWorld, bossLoc.get(0), bossLoc.get(1), bossLoc.get(2));
        } else {
            String regionName = loc.getArenaRegion();
            if (regionName == null || regionName.isEmpty())
                regionName = loc.getSafeZone();

            spawn = plugin.getWorldGuardHook().getRegionCenter(dungeonWorld, regionName)
                    .orElse(dungeonWorld.getSpawnLocation()).add(20, 0, 20);
        }
        waveManager.spawnBoss(stage.getBossId(), spawn, participants.size());
    }

    private void onStageComplete() {
        if (!active)
            return;
        removeBossBar();
        broadcastTitle("&6&lSTAGE COMPLETE", "&7Moving to next stage...", 10, 40, 10);
        playSound(Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f);

        currentStage++;
        if (currentStage > difficulty.getTotalStages()) {
            plugin.getDungeonManager().endDungeon(this, true);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                teleportToStage(currentStage);
                Bukkit.getScheduler().runTaskLater(plugin, () -> startStage(currentStage), 60L);
            }, 100L);
        }
    }

    private void startSafeZoneCheck() {
        if (safeZoneCheckTask != null)
            safeZoneCheckTask.cancel();
        safeZoneCheckTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!active) {
                safeZoneCheckTask.cancel();
                return;
            }
            Dungeon.Stage stage = getStage(currentStage);
            if (stage == null)
                return;

            String safeZone = stage.getLocation(instanceId).getSafeZone();

            for (UUID uuid : participants) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null)
                    continue;
                boolean wasInSafeZone = inSafeZone.getOrDefault(uuid, true);
                boolean isNowInSafeZone = plugin.getWorldGuardHook().isInRegion(player, safeZone);
                if (wasInSafeZone && !isNowInSafeZone) {
                    player.sendMessage(ConfigUtils.getMessage("dungeon.safe-zone-exit"));
                    inSafeZone.put(uuid, false);
                }
            }
        }, 20L, 10L);
    }

    private void startHUDTask() {
        // Cancel previous HUD task if any (safety)
        if (hudTask != null) {
            hudTask.cancel();
        }
        hudTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!active) {
                hudTask.cancel();
                hudTask = null;
                return;
            }

            String time = ChatUtils.formatTime(getDuration() / 1000);
            String rank = getPerformanceRank();

            for (UUID uuid : participants) {
                Player p = Bukkit.getPlayer(uuid);
                if (p == null)
                    continue;

                int playerLives = lives.getOrDefault(uuid, 0);
                String heartColor = playerLives > 1 ? "&a" : "&c";

                String actionbar = ChatUtils.colorize(
                        "&7Waktu: &f" + time + " &8| " +
                                "&7Lives: " + heartColor + playerLives + " ❤ &8| " +
                                "&7Rank: &#FFBB00" + rank);

                p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        net.md_5.bungee.api.chat.TextComponent.fromLegacyText(actionbar));
            }
        }, 20L, 20L);
    }

    public void handleFakeDeath(Player player) {
        if (!active)
            return;
        totalDeaths++;
        int remaining = lives.getOrDefault(player.getUniqueId(), 0) - 1;
        lives.put(player.getUniqueId(), remaining);

        // Heal and clear effects
        player.setHealth(player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
        player.setFoodLevel(20);
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_HURT, 1f, 1f);
        player.sendTitle(ChatUtils.colorize("&c&lYOU DIED"), ChatUtils.colorize("&7Respawning..."), 5, 40, 5);

        broadcast(ConfigUtils.getMessage("dungeon.player-died", "%player%", player.getName(), "%lives%",
                String.valueOf(Math.max(0, remaining))));

        if (remaining <= 0) {
            player.sendMessage(ConfigUtils.getMessage("dungeon.no-more-lives"));
            Location returnLoc = returnLocations.get(player.getUniqueId());
            if (returnLoc != null)
                Bukkit.getScheduler().runTaskLater(plugin, () -> player.teleport(returnLoc), 10L);
            participants.remove(player.getUniqueId());
        } else {
            // Respawn with invulnerability
            Dungeon.Stage stage = getStage(currentStage);
            if (stage != null) {
                Optional<Location> spawnOpt = plugin.getWorldGuardHook().getRegionSpawnPoint(dungeonWorld,
                        stage.getLocation(instanceId).getSafeZone());
                Location safeSpawn = spawnOpt.orElse(dungeonWorld.getSpawnLocation());

                // Countdown before respawn teleport
                for (int i = 3; i >= 1; i--) {
                    int sec = i;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (player.isOnline()) {
                            player.sendTitle(ChatUtils.colorize("&c&lYOU DIED"),
                                    ChatUtils.colorize("&7Respawn in &f" + sec + "s"), 0, 25, 0);
                        }
                    }, (3 - i) * 20L);
                }

                // Teleport after 3 second delay
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!player.isOnline() || !active)
                        return;
                    player.teleport(safeSpawn);
                    inSafeZone.put(player.getUniqueId(), true);

                    // Grant 3 seconds of invulnerability
                    invulnerable.add(player.getUniqueId());
                    player.sendTitle(ChatUtils.colorize("&a&lRESPAWNED"),
                            ChatUtils.colorize("&7Invulnerable for 3s"), 5, 30, 10);
                    player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.5f);

                    // Visual invulnerability indicator
                    player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.GLOWING, 60, 0, true, false));

                    // Re-apply chosen buff
                    buffChoiceGUI.reapplyBuff(player);

                    // Remove invulnerability after 3 seconds
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        invulnerable.remove(player.getUniqueId());
                        if (player.isOnline()) {
                            player.sendMessage(ChatUtils.colorize("&e&lInvulnerability expired! &7Be careful!"));
                        }
                    }, 60L);
                }, 60L);
            }
        }

        boolean allDead = participants.isEmpty()
                || participants.stream().allMatch(uuid -> lives.getOrDefault(uuid, 0) <= 0);
        if (allDead)
            plugin.getDungeonManager().endDungeon(this, false);
    }

    public boolean isInvulnerable(UUID uuid) {
        return invulnerable.contains(uuid);
    }

    public void handlePlayerLeave(Player player) {
        participants.remove(player.getUniqueId());
        lives.remove(player.getUniqueId());
        inSafeZone.remove(player.getUniqueId());
        invulnerable.remove(player.getUniqueId());
        Location returnLoc = returnLocations.remove(player.getUniqueId());
        if (returnLoc != null && player.isOnline())
            player.teleport(returnLoc);
        broadcast(ConfigUtils.getMessage("dungeon.player-left", "%player%", player.getName()));
        if (participants.isEmpty())
            plugin.getDungeonManager().endDungeon(this, false);
    }

    public void end(boolean victory) {
        this.active = false;
        cancelAllTasks();
        if (waveManager != null)
            waveManager.shutdown();

        long duration = System.currentTimeMillis() - startTime;
        String timeStr = ChatUtils.formatTime(duration / 1000);

        if (victory) {
            broadcastTitle("&6&lVICTORY", "&7Dungeon Completed in " + timeStr, 10, 60, 20);
            playSound(Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f);

            // 1. Determine loot center location
            Location center = dungeonWorld.getSpawnLocation(); // Fallback
            if (!participants.isEmpty()) {
                Player lead = Bukkit.getPlayer(participants.get(0));
                if (lead != null) {
                    center = lead.getLocation();
                }
            }

            // Try to find a good center point (Arena Center)
            Dungeon.Stage lastStage = getStage(currentStage > 1 ? currentStage - 1 : 1);
            if (lastStage != null) {
                String region = lastStage.getLocation(instanceId).getArenaRegion();
                if (region != null && !region.isEmpty()) {
                    Optional<Location> regCenter = plugin.getWorldGuardHook().getRegionCenter(dungeonWorld, region);
                    if (regCenter.isPresent())
                        center = regCenter.get();
                }
            }

            // 2. Spawn Loot Chest with premium VFX
            this.lootChestLocation = center;
            if (difficulty.getLootSection() != null) {
                collectedLoot = lootManager.distributeLoot(participants, difficulty.getLootSection(), false);
                lootManager.spawnLootChest(center, collectedLoot);
            } else {
                // No loot configured, just place chest
                center.getBlock().setType(org.bukkit.Material.ENDER_CHEST);
                dungeonWorld.strikeLightningEffect(center);
            }
            lootManager.giveXpReward(participants, difficulty.getTotalStages());

            // 3. Events & Announcement
            Bukkit.getPluginManager().callEvent(
                    new id.naturalsmp.naturaldungeon.event.DungeonCompleteEvent(
                            dungeon.getId(),
                            difficulty.getId(),
                            new ArrayList<>(participants)));

            // No-death bonus: Flawless Clear
            if (totalDeaths == 0) {
                broadcastTitle("&d&l✦ FLAWLESS CLEAR ✦", "&7No deaths! Bonus rewards!", 10, 60, 20);
                playSound(Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.5f);
                for (UUID uuid : participants) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) {
                        p.sendMessage(ChatUtils.colorize(
                                "&d&l✦ &fFlawless Clear! &7Kamu mendapat bonus XP & loot!"));
                    }
                }
                // Give bonus XP for flawless clear
                lootManager.giveXpReward(participants, 2); // extra 2 stages worth of XP
            }

            // Server-wide completion announcement
            if (ConfigUtils.getBoolean("dungeon.announce-completion")) {
                Player announcePlayer = !participants.isEmpty() ? Bukkit.getPlayer(participants.get(0)) : null;
                String playerName = announcePlayer != null ? announcePlayer.getName() : "Unknown";
                String announcement = ConfigUtils.getMessage("broadcast.completion",
                        "%player%", playerName,
                        "%dungeon%", dungeon.getDisplayName(),
                        "%time%", timeStr);
                Bukkit.broadcastMessage(announcement);
            }

            // 4. Open Completion GUI
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (UUID uuid : participants) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null)
                        new DungeonCompletionGUI(plugin).open(player, this, true);
                }
            }, 40L);

            // 5. Cleanup Sequence (10s delay)
            Bukkit.getScheduler().runTaskLater(plugin, this::cleanupAndTeleport, 200L);

        } else {
            broadcast(ConfigUtils.getMessage("dungeon.dungeon-failed"));
            playSound(Sound.ENTITY_WITHER_DEATH, 0.5f);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (UUID uuid : participants) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null)
                        new DungeonCompletionGUI(plugin).open(player, this, false);
                }
            }, 20L);
            Bukkit.getScheduler().runTaskLater(plugin, this::cleanupAndTeleport, 100L);
        }

        // Clear buffs
        buffChoiceGUI.clearBuffs(participants);
    }

    private void cleanupAndTeleport() {
        for (UUID uuid : new ArrayList<>(participants)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                Location returnLoc = returnLocations.get(uuid);
                if (returnLoc != null)
                    player.teleport(returnLoc);
                player.closeInventory();
                player.setItemOnCursor(null);
                player.sendMessage(ChatUtils.colorize("&a&lNaturalDungeon &8» &7Thank you for playing!"));
            }
        }
        // Cleanup loot chest
        if (lootChestLocation != null && lootChestLocation.getBlock().getType() == org.bukkit.Material.ENDER_CHEST) {
            lootChestLocation.getBlock().setType(org.bukkit.Material.AIR);
        }
    }

    public void forceEnd() {
        this.active = false;
        cancelAllTasks();
        if (waveManager != null)
            waveManager.shutdown();
        for (UUID uuid : participants) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                Location returnLoc = returnLocations.get(uuid);
                if (returnLoc != null)
                    player.teleport(returnLoc);
                player.sendMessage(ConfigUtils.getMessage("dungeon.force-ended"));
                player.closeInventory();
                player.setItemOnCursor(null);
            }
        }
        // Clear buffs
        buffChoiceGUI.clearBuffs(participants);
    }

    private void cancelAllTasks() {
        if (safeZoneCheckTask != null) {
            safeZoneCheckTask.cancel();
            safeZoneCheckTask = null;
        }
        if (hudTask != null) {
            hudTask.cancel();
            hudTask = null;
        }
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
        removeBossBar();
    }

    private Dungeon.Stage getStage(int stageNum) {
        return difficulty.getStages().stream().filter(s -> s.getNumber() == stageNum).findFirst().orElse(null);
    }

    private void broadcast(String message) {
        for (UUID uuid : participants) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null)
                p.sendMessage(message);
        }
    }

    public void playSound(Sound sound, float pitch) {
        for (UUID uuid : participants) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null)
                p.playSound(p.getLocation(), sound, 1f, pitch);
        }
    }

    public Dungeon getDungeon() {
        return dungeon;
    }

    public DungeonDifficulty getDifficulty() {
        return difficulty;
    }

    public List<UUID> getParticipants() {
        return participants;
    }

    public int getCurrentStage() {
        return currentStage;
    }

    public int getCurrentWave() {
        return currentWave;
    }

    public boolean isActive() {
        return active;
    }

    public int getTotalDeaths() {
        return totalDeaths;
    }

    public int getLives(UUID playerUUID) {
        return lives.getOrDefault(playerUUID, 0);
    }

    public long getStartTime() {
        return startTime;
    }

    public long getDuration() {
        return System.currentTimeMillis() - startTime;
    }

    public String getPerformanceRank() {
        if (!active && startTime == 0)
            return "C";

        long durationSec = getDuration() / 1000;
        int deaths = getTotalDeaths();
        int stages = difficulty.getTotalStages();

        // Dynamic threshold: 2 mins per stage for Rank S
        long sThreshold = stages * 120L;
        long aThreshold = stages * 240L;

        if (deaths == 0 && durationSec <= sThreshold)
            return "S";
        if (deaths <= 1 && durationSec <= aThreshold)
            return "A";
        if (deaths <= 3)
            return "B";
        return "C";
    }

    public int getInstanceId() {
        return instanceId;
    }

    public void updateBossBar(String title, double progress, BarColor color) {
        if (bossBar == null) {
            bossBar = Bukkit.createBossBar(title, color, BarStyle.SOLID);
            for (UUID uuid : participants) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null)
                    bossBar.addPlayer(p);
            }
        }
        bossBar.setTitle(ChatUtils.colorize(title));
        bossBar.setProgress(Math.max(0, Math.min(1, progress)));
        bossBar.setColor(color);
        bossBar.setVisible(true);
    }

    private void removeBossBar() {
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
    }

    public void broadcastTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        String colorTitle = ChatUtils.colorize(title);
        String colorSub = ChatUtils.colorize(subtitle);
        for (UUID uuid : participants) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null)
                p.sendTitle(colorTitle, colorSub, fadeIn, stay, fadeOut);
        }
    }

    public List<ItemStack> getCollectedLoot() {
        return collectedLoot;
    }

    public WaveManager getWaveManager() {
        return waveManager;
    }
}
