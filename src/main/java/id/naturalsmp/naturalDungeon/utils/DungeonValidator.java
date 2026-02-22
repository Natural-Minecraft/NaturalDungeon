package id.naturalsmp.naturaldungeon.utils;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.dungeon.Dungeon;

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

    /**
     * Attempt to auto-fix common issues in a dungeon configuration.
     * Returns a list of fixes applied.
     */
    public static List<String> autoFix(NaturalDungeon plugin, Dungeon dungeon) {
        List<String> fixes = new ArrayList<>();
        String dungeonId = dungeon.getId();

        // Fix: Missing difficulty → create default NORMAL
        if (dungeon.getDifficulties().isEmpty()) {
            plugin.getDungeonManager().setDungeonConfig(dungeonId, "difficulties.normal.display", "Normal");
            plugin.getDungeonManager().setDungeonConfig(dungeonId, "difficulties.normal.min-tier", 0);
            plugin.getDungeonManager().setDungeonConfig(dungeonId, "difficulties.normal.max-deaths", 5);
            plugin.getDungeonManager().setDungeonConfig(dungeonId, "difficulties.normal.reward-multiplier", 1.0);
            plugin.getDungeonManager().setDungeonConfig(dungeonId, "difficulties.normal.key-req", "none");
            fixes.add("&#55FF55✔ &7Created default difficulty: &fNORMAL");
        }

        // Fix: No stages → create stage 1
        if (dungeon.getStages().isEmpty()) {
            plugin.getDungeonManager().setDungeonConfig(dungeonId, "stages.1.waves.1.type", "VANILLA");
            plugin.getDungeonManager().setDungeonConfig(dungeonId, "stages.1.waves.1.mob", "ZOMBIE");
            plugin.getDungeonManager().setDungeonConfig(dungeonId, "stages.1.waves.1.count", 5);
            fixes.add("&#55FF55✔ &7Created default Stage 1 with ZOMBIE wave");
        }

        // Fix: Missing region names per stage
        for (int s = 0; s < dungeon.getStages().size(); s++) {
            Dungeon.Stage stage = dungeon.getStages().get(s);
            Dungeon.StageLocation loc = stage.getLocation(1);
            int stageNum = s + 1;

            if (loc == null) {
                // Create location entry with default region names
                String safeName = dungeonId + "_stage" + stageNum + "_safe";
                String arenaName = dungeonId + "_stage" + stageNum + "_arena";
                plugin.getDungeonManager().setDungeonConfig(dungeonId, "stages." + stageNum + ".locations.1.safe-zone",
                        safeName);
                plugin.getDungeonManager().setDungeonConfig(dungeonId,
                        "stages." + stageNum + ".locations.1.arena-region", arenaName);
                fixes.add("&#55FF55✔ &7Stage " + stageNum + ": Generated default region names");
            } else {
                if (loc.getSafeZone() == null || loc.getSafeZone().isEmpty()) {
                    String safeName = dungeonId + "_stage" + stageNum + "_safe";
                    plugin.getDungeonManager().setDungeonConfig(dungeonId,
                            "stages." + stageNum + ".locations.1.safe-zone", safeName);
                    fixes.add("&#55FF55✔ &7Stage " + stageNum + ": Set safezone name → &f" + safeName);
                }
                if (loc.getArenaRegion() == null || loc.getArenaRegion().isEmpty()) {
                    String arenaName = dungeonId + "_stage" + stageNum + "_arena";
                    plugin.getDungeonManager().setDungeonConfig(dungeonId,
                            "stages." + stageNum + ".locations.1.arena-region", arenaName);
                    fixes.add("&#55FF55✔ &7Stage " + stageNum + ": Set arena name → &f" + arenaName);
                }
            }
        }

        // Reload dungeons after fixes
        if (!fixes.isEmpty()) {
            plugin.getDungeonManager().loadDungeons();
        }

        return fixes;
    }
}
