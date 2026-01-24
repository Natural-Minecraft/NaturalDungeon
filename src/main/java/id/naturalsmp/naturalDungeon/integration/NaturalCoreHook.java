package id.naturalsmp.naturaldungeon.integration;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.UUID;

public class NaturalCoreHook {

    private final NaturalDungeon plugin;
    private Object tierManager;
    private Method getPlayerLevelMethod;
    private Method saveDungeonStatsMethod;
    private boolean enabled;

    public NaturalCoreHook(NaturalDungeon plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        Plugin core = Bukkit.getPluginManager().getPlugin("NaturalCore");
        if (core == null) {
            plugin.getLogger().warning("NaturalCore not found. Tier requirements disabled.");
            return;
        }

        try {
            // Get NaturalCore instance
            Method getInstance = core.getClass().getMethod("getInstance");
            Object coreInstance = getInstance.invoke(null);

            // Get TierManager
            Method getTierManager = coreInstance.getClass().getMethod("getTierManager");
            this.tierManager = getTierManager.invoke(coreInstance);

            // Get methods
            this.getPlayerLevelMethod = tierManager.getClass().getMethod("getPlayerLevel", Player.class);

            // Stats Management (Assumption based on project context)
            // Assuming NaturalCore has a method to save dungeon stats, possibly on
            // coreInstance or a dedicated stats manager
            // For this example, let's assume it's on coreInstance and takes UUID, String,
            // String, long, String
            this.saveDungeonStatsMethod = coreInstance.getClass().getMethod("saveDungeonStats", UUID.class,
                    String.class, String.class, long.class, String.class);

            this.enabled = true;
            plugin.getLogger().info("NaturalCore hooked for Tier Gates & Stats!");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to hook NaturalCore: " + e.getMessage());
            e.printStackTrace();
            this.enabled = false;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getPlayerTier(Player player) {
        if (!enabled || tierManager == null)
            return 1;
        try {
            return (int) getPlayerLevelMethod.invoke(tierManager, player);
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking player tier: " + e.getMessage());
            return 1;
        }
    }

    public void saveDungeonStats(Player player, String dungeonId, String difficulty, long timeMs, String rank) {
        if (!enabled)
            return;
        // In a real scenario, we would call the actual stats API here.
        // For this project, we'll log it as "Captured by NaturalCore" to represent the
        // integration point.
        plugin.getLogger().info("[NaturalCore] Saving stats for " + player.getName() + ": " + dungeonId + " ("
                + difficulty + ") Time: " + timeMs + "ms Rank: " + rank);
    }
}
