package id.naturalsmp.naturaldungeon.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.ArrayList;
import java.util.List;

public class SpawnScanner {

    /**
     * Scan for valid 3x3 flat spawn points in a radius.
     * 
     * @param center Center of scan area
     * @param radius Scan radius
     * @return List of valid spawn locations
     */
    public static List<Location> scanForSpawnPoints(Location center, int radius) {
        List<Location> spawnPoints = new ArrayList<>();

        for (int x = -radius; x <= radius; x += 3) {
            for (int z = -radius; z <= radius; z += 3) {
                Location check = center.clone().add(x, 0, z);

                // Find ground level
                Location ground = findGround(check);
                if (ground == null)
                    continue;

                // Check if 3x3 area is flat and valid
                if (isValidSpawnArea(ground)) {
                    spawnPoints.add(ground.add(0.5, 0, 0.5));
                }
            }
        }

        return spawnPoints;
    }

    private static Location findGround(Location start) {
        Location check = start.clone();
        // Search down for solid ground
        for (int y = 0; y < 30; y++) {
            check.setY(start.getY() - y);
            Block block = check.getBlock();
            if (block.getType().isSolid() && !block.isLiquid()) {
                return check.clone().add(0, 1, 0);
            }
        }
        return null;
    }

    private static boolean isValidSpawnArea(Location center) {
        Block centerBlock = center.getBlock().getRelative(BlockFace.DOWN);
        int centerY = centerBlock.getY();

        // Check 3x3 area
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Block ground = center.getWorld().getBlockAt(
                        centerBlock.getX() + dx,
                        centerY,
                        centerBlock.getZ() + dz);
                Block above = ground.getRelative(BlockFace.UP);
                Block above2 = above.getRelative(BlockFace.UP);

                // Ground must be solid
                if (!ground.getType().isSolid() || ground.isLiquid())
                    return false;

                // Must have 2 blocks of air above
                if (above.getType().isSolid() || above2.getType().isSolid())
                    return false;
            }
        }
        return true;
    }

    /**
     * Serialize location to config string format.
     */
    public static String serializeLocation(Location loc) {
        return loc.getWorld().getName() + "," +
                loc.getX() + "," +
                loc.getY() + "," +
                loc.getZ() + ",0,0";
    }
}
