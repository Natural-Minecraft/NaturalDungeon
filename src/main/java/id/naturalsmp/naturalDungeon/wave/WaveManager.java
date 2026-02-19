package id.naturalsmp.naturaldungeon.wave;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.dungeon.Dungeon;
import id.naturalsmp.naturaldungeon.dungeon.DungeonInstance;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import id.naturalsmp.naturaldungeon.utils.ConfigUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class WaveManager {

    private final NaturalDungeon plugin;
    private final DungeonInstance instance;
    private final World dungeonWorld;
    private final Set<UUID> activeMobs = new HashSet<>();
    private UUID bossUUID = null; // Track boss entity for HP bar
    private int initialWaveCount = 0;
    private int pendingSpawns = 0; // [NEW] Track mobs in telegraphing phase
    private BukkitTask waveCheckTask;
    private BukkitTask bossMechanicsTask; // [NEW] Track boss mechanics
    private boolean active = false;

    // Wave specific metadata
    private Dungeon.Wave currentWaveObj;
    private int objectiveTimer = 0; // seconds remaining or captured
    private Entity targetEntity = null; // for DEFEND_TARGET
    private Location captureCenter = null; // for CAPTURE_ZONE
    private double captureRadius = 5.0;

    // Boss specific metadata
    private boolean bossShielded = false;
    private int bossTimer = 0;
    private List<UUID> bossCrystals = new ArrayList<>();

    public WaveManager(NaturalDungeon plugin, DungeonInstance instance, World dungeonWorld) {
        this.plugin = plugin;
        this.instance = instance;
        this.dungeonWorld = dungeonWorld;
    }

    public void spawnWave(Dungeon.Wave wave, Location center, int playerCount, boolean bloodMoon) {
        active = true;
        this.currentWaveObj = wave;
        this.objectiveTimer = wave.getTargetTime();
        this.captureCenter = center;

        double scaleFactor = ConfigUtils.getDouble("scaling.hp-per-player");

        // Calculate total mobs to spawn
        Map<String, Integer> counts = wave.getMobCounts();
        List<String> mobTypes = wave.getMobs();

        // ATOMIC RESET
        activeMobs.clear();
        boolean hasSpawnedInvalid = false;

        // [PREMIUM] Wave Sound
        instance.playSound(Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f);

        int totalExpected = 0;

        if (!counts.isEmpty()) {
            // New Logic: Specific Counts
            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                String mobId = entry.getKey();
                int base = entry.getValue();
                int scaled = (int) Math.ceil(base * (1 + (playerCount - 1) * (scaleFactor - 1)));
                totalExpected += scaled;

                spawnSpecificMobs(mobId, scaled, center, playerCount, bloodMoon);
            }
        } else {
            // Legacy Logic: Random from pool
            int baseCount = wave.getCount();
            int scaledCount = (int) Math.ceil(baseCount * (1 + (playerCount - 1) * (scaleFactor - 1)));
            totalExpected = scaledCount;

            for (int i = 0; i < scaledCount; i++) {
                String mobId = mobTypes.get(ThreadLocalRandom.current().nextInt(mobTypes.size()));
                spawnSpecificMobs(mobId, 1, center, playerCount, bloodMoon);
            }
        }

        this.initialWaveCount = totalExpected;
        this.pendingSpawns = totalExpected;

        // Wave specific setup
        if (wave.getType() == WaveType.DEFEND_TARGET) {
            setupDefendTarget(wave.getTargetName(), center);
        } else if (wave.getType() == WaveType.CAPTURE_ZONE) {
            this.objectiveTimer = 0; // Starts at 0, captures up to targetTime
        }

        updateWaveHUD();

        startWaveCheck();
    }

    private void setupDefendTarget(String targetName, Location center) {
        // Spawn an iron golem or villager as the defend target
        targetEntity = dungeonWorld.spawn(center, org.bukkit.entity.Villager.class,
                org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.CUSTOM);
        if (targetEntity instanceof LivingEntity living) {
            living.setCustomName(ChatUtils.colorize("&a&l" + targetName));
            living.setCustomNameVisible(true);
            try {
                org.bukkit.attribute.AttributeInstance attr = living
                        .getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                if (attr != null) {
                    attr.setBaseValue(100.0);
                }
            } catch (Exception ignored) {
            }
            living.setHealth(100.0);

            // Apply Slowness to keep it mostly stationary
            living.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS,
                    Integer.MAX_VALUE, 10, false, false));
        }
    }

    private void updateWaveHUD() {
        if (!active || currentWaveObj == null)
            return;

        String title = "";
        double progress = 1.0;

        switch (currentWaveObj.getType()) {
            case KILL_ALL:
                progress = initialWaveCount > 0 ? (double) activeMobs.size() / initialWaveCount : 0;
                title = "&eWave " + instance.getCurrentWave() + " &7- &f" + activeMobs.size() + " Mobs left";
                break;
            case HUNT_TARGET:
                progress = initialWaveCount > 0 ? (double) activeMobs.size() / initialWaveCount : 0;
                title = "&c&lHUNT &c" + currentWaveObj.getTargetName() + " &7- &f" + activeMobs.size() + " left";
                break;
            case DEFEND_TARGET:
                if (targetEntity instanceof LivingEntity living) {
                    double hp = living.getHealth();
                    progress = hp / 100.0;
                    title = "&a&lDEFEND &a" + currentWaveObj.getTargetName() + " &7- Time left: " + objectiveTimer
                            + "s";
                }
                break;
            case CAPTURE_ZONE:
                progress = (double) objectiveTimer / currentWaveObj.getTargetTime();
                title = "&b&lCAPTURE ZONE &7- " + (int) (progress * 100) + "%";
                break;
        }

        instance.updateBossBar(title, progress, org.bukkit.boss.BarColor.YELLOW);
    }

    private void spawnSpecificMobs(String mobId, int count, Location center, int playerCount, boolean bloodMoon) {
        for (int i = 0; i < count; i++) {
            // Random point within radius for variation
            double offsetX = ThreadLocalRandom.current().nextDouble(-8, 8);
            double offsetZ = ThreadLocalRandom.current().nextDouble(-8, 8);
            Location targetLoc = center.clone().add(offsetX, 0, offsetZ);
            Location spawnLoc = findGroundLevel(targetLoc);

            // [PREMIUM] Telegraphing Particles
            spawnLoc.getWorld().spawnParticle(org.bukkit.Particle.SOUL, spawnLoc.clone().add(0, 0.5, 0), 20, 0.3, 0.5,
                    0.3, 0.05);
            spawnLoc.getWorld().spawnParticle(org.bukkit.Particle.LARGE_SMOKE, spawnLoc.clone().add(0, 0.5, 0), 10, 0.2,
                    0.3, 0.2, 0.02);

            // Spawn with 1s delay
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Entity spawned = spawnMob(mobId, spawnLoc, playerCount);
                if (spawned != null) {

                    // Mark targets for HUNT_TARGET explicitly
                    if (currentWaveObj != null && currentWaveObj.getType() == WaveType.HUNT_TARGET) {
                        if (spawned instanceof LivingEntity living) {
                            living.setGlowing(true);
                        }
                    }

                    activeMobs.add(spawned.getUniqueId());
                    if (bloodMoon && spawned instanceof LivingEntity living) {
                        if (currentWaveObj == null || currentWaveObj.getType() != WaveType.HUNT_TARGET) {
                            living.setGlowing(true);
                        }
                        org.bukkit.attribute.AttributeInstance attr = living
                                .getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                        if (attr != null) {
                            attr.setBaseValue(attr.getBaseValue() * 1.5);
                            living.setHealth(attr.getBaseValue());
                        }
                    }
                } else {
                    plugin.getLogger().warning("Failed to spawn mob " + mobId + " at " + spawnLoc);
                }
                pendingSpawns--;
            }, 20L);
        }
    }

    public void spawnBoss(String bossId, Location location, int playerCount) {
        active = true;
        Entity boss = spawnMob(bossId, location, playerCount);
        if (boss != null) {
            activeMobs.add(boss.getUniqueId());
            bossUUID = boss.getUniqueId();
            if (boss instanceof LivingEntity living)
                living.setGlowing(true);

            startBossMechanicsTask();
        }
        startWaveCheck();
    }

    private void startBossMechanicsTask() {
        if (bossMechanicsTask != null)
            bossMechanicsTask.cancel();
        bossTimer = 0;
        bossShielded = false;
        bossCrystals.clear();

        bossMechanicsTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!active || bossUUID == null) {
                if (bossMechanicsTask != null)
                    bossMechanicsTask.cancel();
                return;
            }

            Entity boss = Bukkit.getEntity(bossUUID);
            if (boss == null || boss.isDead() || !(boss instanceof LivingEntity living)) {
                if (bossMechanicsTask != null)
                    bossMechanicsTask.cancel();
                return;
            }

            bossTimer++;

            // Enrage Mechanic
            if (bossTimer == 120) { // Enrage at 120 seconds
                instance.broadcastTitle("&c&lENRAGED", "&7The boss grows stronger!", 10, 40, 10);
                instance.playSound(Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f);
                living.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.STRENGTH, Integer.MAX_VALUE, 4, false, false));
                living.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED,
                        Integer.MAX_VALUE, 1, false, false));
                living.getWorld().spawnParticle(org.bukkit.Particle.LAVA, living.getLocation().add(0, 2, 0), 30, 0.5,
                        0.5, 0.5, 0.1);
            }

            // Shield Phase at 50% HP
            double maxHp = living.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
            if (!bossShielded && living.getHealth() <= maxHp * 0.5) {
                bossShielded = true;
                living.setInvulnerable(true); // Bukkit built-in invuln
                instance.broadcastTitle("&b&lSHIELD PHASE", "&7Destroy the crystals to deal damage!", 10, 40, 10);
                instance.playSound(Sound.BLOCK_BEACON_ACTIVATE, 1.2f);

                // Spawn crystals
                Location center = living.getLocation();
                double radius = 6.0;
                for (int i = 0; i < 4; i++) {
                    double angle = i * Math.PI / 2;
                    double x = center.getX() + radius * Math.cos(angle);
                    double z = center.getZ() + radius * Math.sin(angle);
                    Location cLoc = findGroundLevel(new Location(dungeonWorld, x, center.getY(), z));

                    org.bukkit.entity.EnderCrystal crystal = dungeonWorld.spawn(cLoc,
                            org.bukkit.entity.EnderCrystal.class);
                    crystal.setShowingBottom(true);
                    crystal.setCustomName(ChatUtils.colorize("&b&lSHIELD CRYSTAL"));
                    crystal.setCustomNameVisible(true);
                    bossCrystals.add(crystal.getUniqueId());

                    // Particle beam to boss (visual)
                    Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
                        @Override
                        public void run() {
                            if (crystal.isDead() || living.isDead()) {
                                Bukkit.getScheduler().cancelTasks(plugin); // Hacky, better to use BukkitRunnable or
                                                                           // specific ID
                                return;
                            }
                            // Draw beam
                            Location p1 = crystal.getLocation().add(0, 0.5, 0);
                            Location p2 = living.getLocation().add(0, 1, 0);
                            org.bukkit.util.Vector dir = p2.toVector().subtract(p1.toVector());
                            double dist = dir.length();
                            dir.normalize().multiply(0.5);
                            for (double d = 0; d < dist; d += 0.5) {
                                p1.add(dir);
                                dungeonWorld.spawnParticle(org.bukkit.Particle.END_ROD, p1, 1, 0, 0, 0, 0);
                            }
                        }
                    }, 0L, 5L);
                }
            }

            // Check if shielded and crystals are dead
            if (bossShielded && living.isInvulnerable()) {
                bossCrystals.removeIf(uuid -> {
                    Entity e = Bukkit.getEntity(uuid);
                    return e == null || e.isDead();
                });

                if (bossCrystals.isEmpty()) {
                    living.setInvulnerable(false);
                    instance.broadcastTitle("&a&lSHIELD BROKEN", "&7Finish it off!", 10, 40, 10);
                    instance.playSound(Sound.BLOCK_BEACON_DEACTIVATE, 1.2f);
                    living.getWorld().spawnParticle(org.bukkit.Particle.EXPLOSION,
                            living.getLocation().add(0, 1, 0), 2, 0.5, 0.5, 0.5, 0);
                }
            }
        }, 20L, 20L);
    }

    private Entity spawnMob(String mobId, Location location, int playerCount) {
        if (location == null || location.getWorld() == null) {
            plugin.getLogger().warning("Cannot spawn mob: Location or World is null.");
            return null;
        }

        if (mobId.startsWith("MM:") && plugin.hasMythicMobs()) {
            Entity mmEffect = plugin.getMythicMobsHook().spawnMob(mobId.substring(3), location, 1);
            if (mmEffect != null)
                return mmEffect;
        }

        EntityType entityType = parseVanillaMob(mobId);
        if (entityType == null) {
            plugin.getLogger().warning("Unknown mob type: " + mobId);
            return null;
        }

        try {
            Entity entity = dungeonWorld.spawn(location, entityType.getEntityClass(),
                    org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.CUSTOM);
            if (entity instanceof LivingEntity living) {
                double hpMultiplier = 1 + (playerCount - 1) * (ConfigUtils.getDouble("scaling.hp-per-player") - 1);
                double maxHealthValue = 20.0; // Fallback

                try {
                    org.bukkit.attribute.AttributeInstance attr = living
                            .getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                    if (attr != null) {
                        maxHealthValue = attr.getBaseValue() * hpMultiplier;
                        attr.setBaseValue(maxHealthValue);
                    } else {
                        maxHealthValue = living.getMaxHealth() * hpMultiplier;
                        living.setMaxHealth(maxHealthValue);
                    }
                } catch (Throwable e) {
                    // Fallback for older versions or issues
                    maxHealthValue = living.getMaxHealth() * hpMultiplier;
                    living.setMaxHealth(maxHealthValue);
                }
                living.setHealth(maxHealthValue);

                // [NEW] AuraMobs Integration
                if (plugin.getAuraMobsHook().isEnabled()) {
                    int level = instance.getDungeon().getMinTier() * 10 + (instance.getCurrentStage() * 5);
                    plugin.getAuraMobsHook().setLevel(living, level);
                }

                // [NEW] Ultimate Meta-Tag
                String name = ChatUtils.colorize("&7[Lv." + instance.getDungeon().getMinTier() + "] &f"
                        + entityType.name() + " &c❤ " + (int) living.getHealth() + "/"
                        + (int) living.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getBaseValue());
                living.setCustomName(name);
                living.setCustomNameVisible(true);
            }
            return entity;
        } catch (Exception e) {
            plugin.getLogger()
                    .severe("Error spawning entity " + entityType + " at " + location + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private EntityType parseVanillaMob(String mobId) {
        if (mobId.startsWith("VANILLA:"))
            mobId = mobId.substring(8);
        try {
            return EntityType.valueOf(mobId.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Location findGroundLevel(Location loc) {
        World world = loc.getWorld();
        if (world == null)
            return loc;

        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        int startY = loc.getBlockY();

        // Search down from loc + 5 down to -64
        for (int y = startY + 5; y > world.getMinHeight(); y--) {
            Location check = new Location(world, x + 0.5, y, z + 0.5);
            if (world.getBlockAt(check).getType().isSolid()) {
                // Found floor, check if 2 blocks above are air
                if (!world.getBlockAt(check.clone().add(0, 1, 0)).getType().isSolid() &&
                        !world.getBlockAt(check.clone().add(0, 2, 0)).getType().isSolid()) {
                    return check.add(0, 1, 0);
                }
            }
        }
        return loc;
    }

    private void startWaveCheck() {
        if (waveCheckTask != null)
            waveCheckTask.cancel();
        waveCheckTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!active) {
                cancelWaveCheck();
                return;
            }
            activeMobs.removeIf(uuid -> {
                Entity entity = Bukkit.getEntity(uuid);
                return entity == null || entity.isDead();
            });

            // Process Objective Mechanics
            if (currentWaveObj != null) {
                boolean endWave = false;

                switch (currentWaveObj.getType()) {
                    case KILL_ALL:
                    case HUNT_TARGET:
                        if (pendingSpawns <= 0 && activeMobs.isEmpty()) {
                            endWave = true;
                        }
                        break;
                    case DEFEND_TARGET:
                        if (targetEntity == null || targetEntity.isDead()) {
                            // FAILED DEFENSE
                            instance.broadcastTitle("&c&lDEFENSE FAILED", "&7The target was destroyed!", 10, 40, 10);
                            instance.forceEnd();
                            return;
                        }
                        objectiveTimer--;
                        if (objectiveTimer <= 0) {
                            endWave = true;
                        } else if (objectiveTimer % 5 == 0 && pendingSpawns <= 0
                                && activeMobs.size() < initialWaveCount) {
                            // Respawn mobs to maintain pressure
                            int toSpawn = 1 + (instance.getParticipants().size() / 2);
                            spawnRandomActiveMobType(toSpawn);
                        }
                        break;
                    case CAPTURE_ZONE:
                        // Check players in zone
                        boolean anyoneInZone = false;
                        if (captureCenter != null) {
                            // Draw boundary
                            drawCaptureRing(captureCenter, captureRadius);
                            for (UUID pId : instance.getParticipants()) {
                                Player p = Bukkit.getPlayer(pId);
                                if (p != null && p.getLocation().distanceSquared(captureCenter) <= captureRadius
                                        * captureRadius) {
                                    anyoneInZone = true;
                                    break;
                                }
                            }
                        }

                        if (anyoneInZone) {
                            objectiveTimer++;
                            instance.playSound(Sound.BLOCK_NOTE_BLOCK_HAT, 1f);
                        } else if (objectiveTimer > 0) {
                            objectiveTimer--; // Degrade if no one is inside
                        }

                        if (objectiveTimer >= currentWaveObj.getTargetTime()) {
                            endWave = true;
                        } else if (objectiveTimer % 5 == 0 && pendingSpawns <= 0
                                && activeMobs.size() < initialWaveCount) {
                            // Keep pressure up
                            spawnRandomActiveMobType(1);
                        }
                        break;
                }

                updateWaveHUD();

                if (endWave) {
                    completeWave();
                    return;
                }
            } else {
                // Fallback Legacy
                if (pendingSpawns <= 0 && activeMobs.isEmpty()) {
                    completeWave();
                    return;
                }
                updateWaveHUD();
            }

        }, 20L, 20L);
    }

    private void drawCaptureRing(Location center, double radius) {
        World world = center.getWorld();
        if (world == null)
            return;
        double progress = (double) objectiveTimer / currentWaveObj.getTargetTime();
        org.bukkit.Particle particle = progress >= 1.0 ? org.bukkit.Particle.HAPPY_VILLAGER
                : org.bukkit.Particle.WITCH;

        for (int i = 0; i < 360; i += 10) {
            double angle = i * Math.PI / 180;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            world.spawnParticle(particle, new Location(world, x, center.getY() + 0.1, z), 1, 0, 0, 0, 0);
        }
    }

    private void spawnRandomActiveMobType(int count) {
        if (currentWaveObj != null && !currentWaveObj.getMobs().isEmpty()) {
            List<String> types = currentWaveObj.getMobs();
            String type = types.get(ThreadLocalRandom.current().nextInt(types.size()));
            spawnSpecificMobs(type, count, captureCenter != null ? captureCenter
                    : instance.getParticipants().stream()
                            .map(Bukkit::getPlayer).filter(Objects::nonNull).findFirst().map(Player::getLocation)
                            .orElse(dungeonWorld.getSpawnLocation()),
                    instance.getParticipants().size(), false);
        }
    }

    private void completeWave() {
        if (targetEntity != null && !targetEntity.isDead()) {
            targetEntity.remove(); // Cleanup defend target
        }
        killAllMobs(); // Clean up existing mobs
        cancelWaveCheck();
        instance.onWaveComplete();
    }

    private void cancelWaveCheck() {
        if (waveCheckTask != null) {
            waveCheckTask.cancel();
            waveCheckTask = null;
        }
        if (bossMechanicsTask != null) {
            bossMechanicsTask.cancel();
            bossMechanicsTask = null;
        }
    }

    public void killAllMobs() {
        for (UUID uuid : activeMobs) {
            Entity entity = Bukkit.getEntity(uuid);
            if (entity != null && !entity.isDead())
                entity.remove();
        }
        for (UUID uuid : bossCrystals) {
            Entity entity = Bukkit.getEntity(uuid);
            if (entity != null && !entity.isDead())
                entity.remove();
        }
        bossCrystals.clear();
        activeMobs.clear();
        cancelWaveCheck();
    }

    public boolean hasMobsAlive() {
        return !activeMobs.isEmpty();
    }

    public boolean isDungeonMob(UUID uuid) {
        return activeMobs.contains(uuid);
    }

    public void shutdown() {
        active = false;
        killAllMobs();
        bossUUID = null;
    }

    public boolean isBossMob(UUID uuid) {
        return bossUUID != null && bossUUID.equals(uuid);
    }
}
