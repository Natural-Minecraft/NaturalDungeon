package id.naturalsmp.naturaldungeon.utils;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.dungeon.Dungeon;
import id.naturalsmp.naturaldungeon.dungeon.DungeonDifficulty;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;

public class DungeonValidator {

    public static List<String> validate(NaturalDungeon plugin, Dungeon dungeon) {
        List<String> errors = new ArrayList<>();

        if (dungeon.getDifficulties().isEmpty()) {
            errors.add("&cTidak ada difficulty yang diatur.");
            return errors;
        }

        if (dungeon.getStages().isEmpty()) {
            errors.add("&cTidak ada stage satupun!");
            return errors;
        }

        for (int s = 0; s < dungeon.getStages().size(); s++) {
            Dungeon.Stage stage = dungeon.getStages().get(s);
            String stageName = "Stage " + (s + 1);

            List<String> stageErrors = validateStage(plugin, dungeon, stage);
            for (String err : stageErrors) {
                // Determine if the error should be &c (critical) or &e (warning)
                String prefix = err.contains("tidak ditemukan di WorldGuard") ? "&e" : "&c";
                errors.add(prefix + stageName + ": " + err);
            }
        }

        return errors;
    }

    public static List<String> validateStage(NaturalDungeon plugin, Dungeon dungeon, Dungeon.Stage stage) {
        List<String> errors = new ArrayList<>();

        if (stage.getWaves().isEmpty()) {
            errors.add("Tidak ada wave monster.");
        }

        // Check Locations (Assumes Instance 1 is the baseline setup)
        Dungeon.StageLocation loc = stage.getLocation(1);
        if (loc == null) {
            errors.add("Region/Lokasi belum di-setup!");
            return errors;
        }

        if (loc.getSafeZone() == null || loc.getSafeZone().isEmpty()) {
            errors.add("Safezone belum di-setup!");
        } else if (plugin.getWorldGuardHook().getRegionCenter(Bukkit.getWorld(dungeon.getWorld()), loc.getSafeZone())
                .isEmpty()) {
            errors.add("Safezone region tidak ditemukan di WorldGuard.");
        }

        if (loc.getArenaRegion() == null || loc.getArenaRegion().isEmpty()) {
            errors.add("Arena Region belum di-setup!");
        } else if (plugin.getWorldGuardHook().getRegionCenter(Bukkit.getWorld(dungeon.getWorld()), loc.getArenaRegion())
                .isEmpty()) {
            errors.add("Arena region tidak ditemukan di WorldGuard.");
        }

        if (stage.hasBoss()) {
            if (loc.getBossSpawnLocation() == null || loc.getBossSpawnLocation().isEmpty()) {
                errors.add("Lokasi Boss Spawn belum di-set!");
            }
        }

        return errors;
    }

    public static boolean isValid(NaturalDungeon plugin, Dungeon dungeon) {
        return validate(plugin, dungeon).isEmpty();
    }
}
