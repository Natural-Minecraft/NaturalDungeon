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
    private int initialWaveCount = 0;
    private int pendingSpawns = 0; // [NEW] Track mobs in telegraphing phase
    private BukkitTask waveCheckTask;
    private boolean active = false;

    public WaveManager(NaturalDungeon plugin, DungeonInstance instance, World dungeonWorld) {
        this.plugin = plugin;
        this.instance = instance;
        this.dungeonWorld = dungeonWorld;
    }

    public void spawnWave(Dungeon.Wave wave, Location center, int playerCount, boolean bloodMoon) {
        active = true;
        double scaleFactor = ConfigUtils.getDouble("scaling.hp-per-player");
        int baseCount = wave.getCount();
        int scaledCount = (int) Math.ceil(baseCount * (1 + (playerCount - 1) * (scaleFactor - 1)));
        List<String> mobTypes = wave.getMobs();

        // ATOMIC RESET
        initialWaveCount = scaledCount;
        pendingSpawns = scaledCount;
        activeMobs.clear();

        plugin.getLogger().info("Spawning Wave: Base=" + baseCount + ", Scaled=" + scaledCount + " for " + playerCount
                + " players at center: " + center);

        // [PREMIUM] Wave Sound
        instance.playSound(Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f);

        for (int i = 0; i < scaledCount; i++) {
            String mobId = mobTypes.get(ThreadLocalRandom.current().nextInt(mobTypes.size()));
            double offsetX = ThreadLocalRandom.current().nextDouble(-10, 10);
            double offsetZ = ThreadLocalRandom.current().nextDouble(-10, 10);
            Location targetLoc = center.clone().add(offsetX, 0, offsetZ);
            Location spawnLoc = findGroundLevel(targetLoc);

            // [PREMIUM] Telegraphing Particles
            spawnLoc.getWorld().spawnParticle(org.bukkit.Particle.SOUL, spawnLoc.clone().add(0, 0.5, 0), 20, 0.3, 0.5,
                    0.3, 0.05);
            spawnLoc.getWorld().spawnParticle(org.bukkit.Particle.LARGE_SMOKE, spawnLoc.clone().add(0, 0.5, 0), 10, 0.2,
                    0.3, 0.2, 0.02);

            // Spawn with 1s delay for telegraphing to be visible
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Entity spawned = spawnMob(mobId, spawnLoc, playerCount);
                if (spawned != null) {
                    activeMobs.add(spawned.getUniqueId());
                    if (bloodMoon && spawned instanceof LivingEntity living) {
                        living.setGlowing(true);
                        org.bukkit.attribute.AttributeInstance attr = living
                                .getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                        if (attr != null) {
                            attr.setBaseValue(attr.getBaseValue() * 1.5);
                            living.setHealth(attr.getBaseValue());
                        }
                    }
                }
                pendingSpawns--; // DECREMENT after spawn
            }, 20L); // 1 Second delay
        }

        instance.updateBossBar(ChatUtils.colorize("&eWave " + (instance.getCurrentWave()) + " &f- &7Spawning..."), 1.0,
                org.bukkit.boss.BarColor.YELLOW);

        startWaveCheck();
    }

    public void spawnBoss(String bossId, Location location, int playerCount) {
        active = true;
        Entity boss = spawnMob(bossId, location, playerCount);
        if (boss != null) {
            activeMobs.add(boss.getUniqueId());
            if (boss instanceof LivingEntity living)
                living.setGlowing(true);
        }
        startWaveCheck();
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

            // RECRUITMENT LOCK: Don't clear wave if spawns are still pending
            if (pendingSpawns > 0)
                return;

            if (activeMobs.isEmpty()) {
                cancelWaveCheck();
                instance.onWaveComplete();
            } else {
                double progress = initialWaveCount > 0 ? (double) activeMobs.size() / initialWaveCount : 0;
                instance.updateBossBar(
                        "&eWave " + instance.getCurrentWave() + " &7- &f" + activeMobs.size() + " Mobs left", progress,
                        org.bukkit.boss.BarColor.YELLOW);
            }
        }, 20L, 20L);
    }

    private void cancelWaveCheck() {
        if (waveCheckTask != null) {
            waveCheckTask.cancel();
            waveCheckTask = null;
        }
    }

    public void killAllMobs() {
        for (UUID uuid : activeMobs) {
            Entity entity = Bukkit.getEntity(uuid);
            if (entity != null && !entity.isDead())
                entity.remove();
        }
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
    }
}
