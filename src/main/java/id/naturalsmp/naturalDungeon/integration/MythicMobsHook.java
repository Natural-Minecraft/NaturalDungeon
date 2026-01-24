package id.naturalsmp.naturaldungeon.integration;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import io.lumine.mythic.api.MythicProvider;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.Optional;

public class MythicMobsHook {

    private final NaturalDungeon plugin;

    public MythicMobsHook(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public Entity spawnMob(String mobId, Location location, double level) {
        try {
            Optional<MythicMob> mythicMob = MythicProvider.get().getMobManager().getMythicMob(mobId);
            if (mythicMob.isPresent()) {
                ActiveMob activeMob = mythicMob.get().spawn(BukkitAdapter.adapt(location), level);
                return activeMob.getEntity().getBukkitEntity();
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to spawn MythicMob: " + mobId + " - " + e.getMessage());
        }
        return null;
    }

    public boolean mobExists(String mobId) {
        return MythicProvider.get().getMobManager().getMythicMob(mobId).isPresent();
    }
}
