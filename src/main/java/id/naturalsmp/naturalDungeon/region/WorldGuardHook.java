package id.naturalsmp.naturaldungeon.region;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import id.naturalsmp.naturaldungeon.NaturalDungeon;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Optional;

public class WorldGuardHook {

    private final NaturalDungeon plugin;
    private boolean enabled;

    public WorldGuardHook(NaturalDungeon plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getServer().getPluginManager().getPlugin("WorldGuard") != null;
        if (enabled) {
            plugin.getLogger().info("WorldGuard hooked!");
        } else {
            plugin.getLogger().warning("WorldGuard not found! Safe zones will not work.");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isInRegion(Player player, String regionName) {
        if (!enabled)
            return false;
        try {
            Location loc = player.getLocation();
            World world = loc.getWorld();
            if (world == null)
                return false;
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regions = container.get(BukkitAdapter.adapt(world));
            if (regions == null)
                return false;
            ProtectedRegion region = regions.getRegion(regionName);
            if (region == null)
                return false;
            BlockVector3 position = BlockVector3.at(loc.getX(), loc.getY(), loc.getZ());
            return region.contains(position);
        } catch (Exception e) {
            return false;
        }
    }

    public Optional<Location> getRegionCenter(World world, String regionName) {
        if (!enabled)
            return Optional.empty();
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regions = container.get(BukkitAdapter.adapt(world));
            if (regions == null)
                return Optional.empty();
            ProtectedRegion region = regions.getRegion(regionName);
            if (region == null)
                return Optional.empty();
            BlockVector3 min = region.getMinimumPoint();
            BlockVector3 max = region.getMaximumPoint();
            double centerX = (min.getX() + max.getX()) / 2.0;
            double centerY = (min.getY() + max.getY()) / 2.0;
            double centerZ = (min.getZ() + max.getZ()) / 2.0;
            return Optional.of(findSafeLocation(new Location(world, centerX, centerY, centerZ)));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<Location> getRegionSpawnPoint(World world, String regionName) {
        if (!enabled)
            return Optional.empty();
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regions = container.get(BukkitAdapter.adapt(world));
            if (regions == null)
                return Optional.empty();
            ProtectedRegion region = regions.getRegion(regionName);
            if (region == null)
                return Optional.empty();
            BlockVector3 min = region.getMinimumPoint();
            BlockVector3 max = region.getMaximumPoint();
            double centerX = (min.getX() + max.getX()) / 2.0;
            double centerZ = (min.getZ() + max.getZ()) / 2.0;
            return Optional.of(findSafeLocation(new Location(world, centerX, min.getY() + 1, centerZ)));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public boolean regionExists(World world, String regionName) {
        if (!enabled)
            return false;
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regions = container.get(BukkitAdapter.adapt(world));
            return regions != null && regions.getRegion(regionName) != null;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean createRegion(String regionName, Location pos1, Location pos2) {
        if (!enabled)
            return false;
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regions = container.get(BukkitAdapter.adapt(pos1.getWorld()));
            if (regions == null)
                return false;

            BlockVector3 min = BlockVector3.at(
                    Math.min(pos1.getBlockX(), pos2.getBlockX()),
                    Math.min(pos1.getBlockY(), pos2.getBlockY()),
                    Math.min(pos1.getBlockZ(), pos2.getBlockZ()));
            BlockVector3 max = BlockVector3.at(
                    Math.max(pos1.getBlockX(), pos2.getBlockX()),
                    Math.max(pos1.getBlockY(), pos2.getBlockY()),
                    Math.max(pos1.getBlockZ(), pos2.getBlockZ()));

            ProtectedRegion region = new com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion(regionName, min,
                    max);

            // Set flag to allow mob spawning programmatically
            try {
                com.sk89q.worldguard.protection.flags.Flag<?> mobSpawnFlag = com.sk89q.worldguard.protection.flags.Flags.MOB_SPAWNING;
                region.setFlag((com.sk89q.worldguard.protection.flags.StateFlag) mobSpawnFlag,
                        com.sk89q.worldguard.protection.flags.StateFlag.State.ALLOW);
            } catch (Exception ignored) {
            }

            regions.addRegion(region);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private Location findSafeLocation(Location loc) {
        World world = loc.getWorld();
        if (world == null)
            return loc;
        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        for (int y = loc.getBlockY(); y < world.getMaxHeight() - 2; y++) {
            Location check = new Location(world, x + 0.5, y, z + 0.5);
            if (!world.getBlockAt(check).getType().isSolid() &&
                    !world.getBlockAt(check.clone().add(0, 1, 0)).getType().isSolid() &&
                    world.getBlockAt(check.clone().add(0, -1, 0)).getType().isSolid()) {
                return check;
            }
        }
        return loc;
    }
}
