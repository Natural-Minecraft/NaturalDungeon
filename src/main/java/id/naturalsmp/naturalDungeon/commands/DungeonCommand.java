package id.naturalsmp.naturaldungeon.commands;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.dungeon.DungeonType;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import id.naturalsmp.naturaldungeon.utils.ConfigUtils;
import id.naturalsmp.naturaldungeon.utils.DungeonGenerator;
import id.naturalsmp.naturaldungeon.utils.LootGenerator;
import id.naturalsmp.naturaldungeon.utils.SpawnScanner;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DungeonCommand implements CommandExecutor, org.bukkit.command.TabCompleter {

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
                player.sendMessage(ChatUtils.colorize("&cUsage: /dg create <id> <type>"));
                player.sendMessage(ChatUtils.colorize("&7Types: &fBASIC, WAVE_DEFENSE, BOSS_RUSH"));
                return true;
            }

            String dungeonId = args[1].toLowerCase();
            String typeStr = args[2].toUpperCase();
            DungeonType type;

            try {
                type = DungeonType.valueOf(typeStr);
            } catch (IllegalArgumentException e) {
                player.sendMessage(ChatUtils.colorize("&cInvalid Type! Available: &fBASIC, WAVE_DEFENSE, BOSS_RUSH"));
                return true;
            }

            DungeonGenerator generator = new DungeonGenerator(plugin);
            if (generator.generate(dungeonId, type, player)) {
                player.sendMessage(
                        ChatUtils.colorize("&a&lSUCCESS! &fDungeon &e" + dungeonId + " &fgenerated successfully!"));
                player.sendMessage(
                        ChatUtils.colorize("&7Location set to your position. Edit &fplugins/NaturalDungeon/dungeons/"
                                + dungeonId + ".yml &7to customize."));
            } else {
                player.sendMessage(
                        ChatUtils.colorize("&cError! Dungeon ID &e" + dungeonId + " &calready exists or file error."));
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
                player.sendMessage(ChatUtils.colorize("&cUsage: /dg loot autogen <id> <tier>"));
                player.sendMessage(ChatUtils.colorize("&7Tier: 1-10"));
                return true;
            }

            String dungeonId = args[2].toLowerCase();
            int tier;
            try {
                tier = Integer.parseInt(args[3]);
                if (tier < 1 || tier > 10)
                    throw new NumberFormatException();
            } catch (NumberFormatException e) {
                player.sendMessage(ChatUtils.colorize("&cInvalid Tier! Must be 1-10."));
                return true;
            }

            LootGenerator generator = new LootGenerator(plugin);
            if (generator.generate(dungeonId, tier, player)) {
                player.sendMessage(ChatUtils.colorize(
                        "&a&lSUCCESS! &fLoot table for &e" + dungeonId + " &fpopulated for Tier &b" + tier + "&f."));
            } else {
                player.sendMessage(ChatUtils.colorize("&cError! Dungeon ID &e" + dungeonId + " &cnot found."));
            }
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("scan")) {
            if (!player.hasPermission("naturaldungeon.admin")) {
                player.sendMessage(ConfigUtils.getMessage("general.no-permission"));
                return true;
            }

            if (args.length < 2) {
                player.sendMessage(ChatUtils.colorize("&cUsage: /dg scan <dungeonId>"));
                return true;
            }

            String dungeonId = args[1].toLowerCase();
            File file = new File(plugin.getDataFolder(), "dungeons/" + dungeonId + ".yml");
            if (!file.exists()) {
                player.sendMessage(ChatUtils.colorize("&cDungeon not found: &e" + dungeonId));
                return true;
            }

            List<Location> spawnPoints = SpawnScanner.scanForSpawnPoints(player.getLocation(), 20);

            if (spawnPoints.isEmpty()) {
                player.sendMessage(ChatUtils.colorize("&cNo valid spawn points found in radius."));
                return true;
            }

            // Save to config
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            List<String> mobSpawns = new ArrayList<>();
            for (Location loc : spawnPoints) {
                mobSpawns.add(SpawnScanner.serializeLocation(loc));
            }
            config.set("difficulties.normal.stages.1.locations.1.mob-spawns", mobSpawns);

            try {
                config.save(file);
                player.sendMessage(ChatUtils.colorize("&a&lSCAN COMPLETE! &fFound &e" + spawnPoints.size()
                        + " &fspawn points. Saved to &b" + dungeonId + ".yml"));
            } catch (IOException e) {
                player.sendMessage(ChatUtils.colorize("&cError saving config!"));
            }
            return true;
        }

        plugin.getDungeonManager().openDungeonGUI(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (!sender.hasPermission("naturaldungeon.admin"))
            return completions;

        if (args.length == 1) {
            completions.addAll(Arrays.asList("create", "loot", "scan"));
            return filter(completions, args[0]);
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("create")) {
                completions.add("<id>");
            } else if (args[0].equalsIgnoreCase("scan")) {
                completions.addAll(getDungeonIds());
            } else if (args[0].equalsIgnoreCase("loot")) {
                completions.add("autogen");
            }
            return filter(completions, args[1]);
        }

        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("create")) {
                completions.addAll(Arrays.asList("BASIC", "WAVE_DEFENSE", "BOSS_RUSH"));
            } else if (args[0].equalsIgnoreCase("loot") && args[1].equalsIgnoreCase("autogen")) {
                completions.addAll(getDungeonIds());
            }
            return filter(completions, args[2]);
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("loot") && args[1].equalsIgnoreCase("autogen")) {
            for (int i = 1; i <= 10; i++)
                completions.add(String.valueOf(i));
            return filter(completions, args[3]);
        }

        return completions;
    }

    private List<String> getDungeonIds() {
        List<String> ids = new ArrayList<>();
        File dungeonDir = new File(plugin.getDataFolder(), "dungeons");
        if (dungeonDir.exists() && dungeonDir.isDirectory()) {
            File[] files = dungeonDir.listFiles((dir, name) -> name.endsWith(".yml"));
            if (files != null) {
                for (File f : files) {
                    ids.add(f.getName().replace(".yml", ""));
                }
            }
        }
        return ids;
    }

    private List<String> filter(List<String> list, String prefix) {
        return list.stream()
                .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }
}
