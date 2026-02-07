package id.naturalsmp.naturaldungeon.commands;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.utils.ConfigUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DungeonCommand implements CommandExecutor {

    private final NaturalDungeon plugin;

    public DungeonCommand(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ConfigUtils.getMessage("general.player-only"));
            return true;
        }

        if (!player.hasPermission("naturaldungeon.dungeon")) {
            player.sendMessage(ConfigUtils.getMessage("general.no-permission"));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("create")) {
            if (!player.hasPermission("naturaldungeon.admin")) {
                player.sendMessage(ConfigUtils.getMessage("general.no-permission"));
                return true;
            }

            if (args.length < 3) {
                player.sendMessage(ConfigUtils.colorize("&cUsage: /dg create <id> <type>"));
                player.sendMessage(ConfigUtils.colorize("&7Types: &fBASIC, WAVE_DEFENSE, BOSS_RUSH"));
                return true;
            }

            String id = args[1].toLowerCase();
            String typeStr = args[2].toUpperCase();
            id.naturalsmp.naturaldungeon.dungeon.DungeonType type;

            try {
                type = id.naturalsmp.naturaldungeon.dungeon.DungeonType.valueOf(typeStr);
            } catch (IllegalArgumentException e) {
                player.sendMessage(ConfigUtils.colorize("&cInvalid Type! Available: &fBASIC, WAVE_DEFENSE, BOSS_RUSH"));
                return true;
            }

            id.naturalsmp.naturaldungeon.utils.DungeonGenerator generator = new id.naturalsmp.naturaldungeon.utils.DungeonGenerator(
                    plugin);
            if (generator.generate(id, type, player)) {
                player.sendMessage(
                        ConfigUtils.colorize("&a&lSUCCESS! &fDungeon &e" + id + " &fgenerated successfully!"));
                player.sendMessage(
                        ConfigUtils.colorize("&7Location set to your position. Edit &fplugins/NaturalDungeon/dungeons/"
                                + id + ".yml &7to customize."));
            } else {
                player.sendMessage(
                        ConfigUtils.colorize("&cError! Dungeon ID &e" + id + " &calready exists or file error."));
            }
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("loot") && args.length > 1
                && args[1].equalsIgnoreCase("autogen")) {
            if (!player.hasPermission("naturaldungeon.admin")) {
                player.sendMessage(ConfigUtils.getMessage("general.no-permission"));
                return true;
            }

            if (args.length < 4) {
                player.sendMessage(ConfigUtils.colorize("&cUsage: /dg loot autogen <id> <tier>"));
                player.sendMessage(ConfigUtils.colorize("&7Tier: 1-10"));
                return true;
            }

            String id = args[2].toLowerCase();
            int tier;
            try {
                tier = Integer.parseInt(args[3]);
                if (tier < 1 || tier > 10)
                    throw new NumberFormatException();
            } catch (NumberFormatException e) {
                player.sendMessage(ConfigUtils.colorize("&cInvalid Tier! Must be 1-10."));
                return true;
            }

            id.naturalsmp.naturaldungeon.utils.LootGenerator generator = new id.naturalsmp.naturaldungeon.utils.LootGenerator(
                    plugin);
            if (generator.generate(id, tier, player)) {
                player.sendMessage(ConfigUtils
                        .colorize("&a&lSUCCESS! &fLoot table for &e" + id + " &fpopulated for Tier &b" + tier + "&f."));
            } else {
                player.sendMessage(ConfigUtils.colorize("&cError! Dungeon ID &e" + id + " &cnot found."));
            }
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("scan")) {
            if (!player.hasPermission("naturaldungeon.admin")) {
                player.sendMessage(ConfigUtils.getMessage("general.no-permission"));
                return true;
            }

            if (args.length < 2) {
                player.sendMessage(ConfigUtils.colorize("&cUsage: /dg scan <dungeonId>"));
                return true;
            }

            String dungeonId = args[1].toLowerCase();
            java.io.File file = new java.io.File(plugin.getDataFolder(), "dungeons/" + dungeonId + ".yml");
            if (!file.exists()) {
                player.sendMessage(ConfigUtils.colorize("&cDungeon not found: &e" + dungeonId));
                return true;
            }

            java.util.List<org.bukkit.Location> spawnPoints = id.naturalsmp.naturaldungeon.utils.SpawnScanner
                    .scanForSpawnPoints(player.getLocation(), 20);

            if (spawnPoints.isEmpty()) {
                player.sendMessage(ConfigUtils.colorize("&cNo valid spawn points found in radius."));
                return true;
            }

            // Save to config
            org.bukkit.configuration.file.YamlConfiguration config = org.bukkit.configuration.file.YamlConfiguration
                    .loadConfiguration(file);
            java.util.List<String> mobSpawns = new java.util.ArrayList<>();
            for (org.bukkit.Location loc : spawnPoints) {
                mobSpawns.add(id.naturalsmp.naturaldungeon.utils.SpawnScanner.serializeLocation(loc));
            }
            config.set("difficulties.normal.stages.1.locations.1.mob-spawns", mobSpawns);

            try {
                config.save(file);
                player.sendMessage(ConfigUtils.colorize("&a&lSCAN COMPLETE! &fFound &e" + spawnPoints.size()
                        + " &fspawn points. Saved to &b" + dungeonId + ".yml"));
            } catch (java.io.IOException e) {
                player.sendMessage(ConfigUtils.colorize("&cError saving config!"));
            }
            return true;
        }

        plugin.getDungeonManager().openDungeonGUI(player);
        return true;
    }
}
