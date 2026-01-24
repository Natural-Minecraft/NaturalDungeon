package id.naturalsmp.naturaldungeon.commands;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.admin.SetupManager;
import id.naturalsmp.naturaldungeon.dungeon.Dungeon;
import id.naturalsmp.naturaldungeon.dungeon.DungeonInstance;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import id.naturalsmp.naturaldungeon.utils.ConfigUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class AdminCommand implements CommandExecutor, TabCompleter {

    private final NaturalDungeon plugin;
    private final SetupManager setupManager;

    public AdminCommand(NaturalDungeon plugin) {
        this.plugin = plugin;
        this.setupManager = new SetupManager(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!sender.hasPermission("naturaldungeon.admin")) {
            sender.sendMessage(ConfigUtils.getMessage("general.no-permission"));
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "reload" -> {
                plugin.reload();
                sender.sendMessage(ConfigUtils.getMessage("general.reload"));
            }
            case "setup" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ConfigUtils.getMessage("general.player-only"));
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(ChatUtils.colorize("&cGunakan: /nd setup <dungeon_id>"));
                    for (String id : plugin.getDungeonManager().getDungeonIds()) {
                        player.sendMessage(ChatUtils.colorize("  &e- " + id));
                    }
                    return true;
                }
                setupManager.openDashboard(player, args[1]);
            }
            case "test" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ConfigUtils.getMessage("general.player-only"));
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(ChatUtils.colorize("&cGunakan: /nd test <dungeon_id>"));
                    return true;
                }
                Dungeon dungeon = plugin.getDungeonManager().getDungeon(args[1]);
                if (dungeon == null) {
                    player.sendMessage(ChatUtils.colorize("&cDungeon tidak ditemukan: " + args[1]));
                    return true;
                }
                player.sendMessage(ChatUtils.colorize("&aMemulai test dungeon: " + args[1]));
                plugin.getDungeonManager().startDungeon(player, dungeon);
            }
            case "forceend" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ConfigUtils.getMessage("general.player-only"));
                    return true;
                }
                var instance = plugin.getDungeonManager().getActiveInstance(player);
                if (instance == null) {
                    player.sendMessage(ChatUtils.colorize("&cKamu tidak dalam dungeon!"));
                    return true;
                }
                plugin.getDungeonManager().endDungeon(instance, false);
                player.sendMessage(ChatUtils.colorize("&aDungeon di-force end."));
            }
            case "list" -> {
                sender.sendMessage(ChatUtils.colorize("&6&lDungeon List:"));
                for (Dungeon dungeon : plugin.getDungeonManager().getDungeons()) {
                    sender.sendMessage(
                            ChatUtils.colorize("  &e" + dungeon.getId() + " &7- " + dungeon.getDisplayName()));
                }
            }
            case "version" -> {
                String pluginVer = plugin.getDescription().getVersion();
                int configVer = plugin.getConfig().getInt("config-version", 0);
                int requiredVer = plugin.getConfigVersion();

                sender.sendMessage(ChatUtils.colorize("&6&lNaturalDungeon Info"));
                sender.sendMessage(ChatUtils.colorize("&7Plugin Version: &f" + pluginVer));
                sender.sendMessage(ChatUtils
                        .colorize("&7Config Version: &f" + configVer + " &7(Required: &e" + requiredVer + "&7)"));

                if (configVer < requiredVer) {
                    sender.sendMessage(ChatUtils.colorize("&c⚠ Config outdated! Please check console."));
                } else if (configVer > requiredVer) {
                    sender.sendMessage(ChatUtils.colorize("&e⚠ Config is newer than plugin? (Dev Build?)"));
                } else {
                    sender.sendMessage(ChatUtils.colorize("&a✔ Config is up to date."));
                }
            }
            case "info" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatUtils.colorize("&cGunakan: /nd info <dungeon_id>"));
                    return true;
                }
                Dungeon dungeon = plugin.getDungeonManager().getDungeon(args[1]);
                if (dungeon == null) {
                    sender.sendMessage(ChatUtils.colorize("&cDungeon tidak ditemukan: " + args[1]));
                    return true;
                }
                sender.sendMessage(ChatUtils.colorize("&6&l" + dungeon.getDisplayName()));
                sender.sendMessage(ChatUtils.colorize("&7ID: &f" + dungeon.getId()));
                sender.sendMessage(ChatUtils.colorize("&7World: &f" + dungeon.getWorld()));
                sender.sendMessage(ChatUtils.colorize("&7Tier: &f" + dungeon.getMinTier()));
                sender.sendMessage(ChatUtils.colorize("&7Stages: &f" + dungeon.getTotalStages()));
            }
            case "admin" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ConfigUtils.getMessage("general.player-only"));
                    return true;
                }
                openAdminGUI(player);
            }
            case "debugspawn" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ConfigUtils.getMessage("general.player-only"));
                    return true;
                }
                handleDebugSpawn(player, args);
            }
            case "check" -> {
                handleCheck(sender, args);
            }
            default -> sendUsage(sender);
        }
        return true;
    }

    private void openAdminGUI(Player player) {
        new id.naturalsmp.naturaldungeon.admin.AdminGUI(plugin).open(player);
    }

    private void handleDebugSpawn(Player player, String[] args) {
        DungeonInstance instance = plugin.getDungeonManager().getActiveInstance(player);
        if (instance == null) {
            player.sendMessage(ChatUtils.colorize("&cYou must be in a dungeon to use this!"));
            return;
        }

        player.sendMessage(ChatUtils.colorize("&aAttempting to spawn a test wave at your location..."));
        Dungeon.Wave testWave = new Dungeon.Wave(Collections.singletonList("ZOMBIE"), 5, 0);
        instance.getWaveManager().spawnWave(testWave, player.getLocation(), 1, false);
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatUtils.colorize("&6&lNaturalDungeon Admin"));
        sender.sendMessage(ChatUtils.colorize("&e/nd reload &7- Reload configs"));
        sender.sendMessage(ChatUtils.colorize("&e/nd check &7- Run diagnostic checks"));
        sender.sendMessage(ChatUtils.colorize("&e/nd setup <dungeon> &7- Setup mode"));
        sender.sendMessage(ChatUtils.colorize("&e/nd test <dungeon> &7- Test dungeon"));
        sender.sendMessage(ChatUtils.colorize("&e/nd forceend &7- Force end dungeon"));
        sender.sendMessage(ChatUtils.colorize("&e/nd list &7- List dungeons"));
        sender.sendMessage(ChatUtils.colorize("&e/nd info <dungeon> &7- Dungeon info"));
        sender.sendMessage(ChatUtils.colorize("&e/nd debugspawn &7- Spawn test mobs"));
    }

    private void handleCheck(CommandSender sender, String[] args) {
        sender.sendMessage(ChatUtils.colorize("&6&lNaturalDungeon Diagnostics..."));
        int dungeons = plugin.getDungeonManager().getDungeons().size();
        sender.sendMessage(ChatUtils.colorize("&7Loaded Dungeons: &f" + dungeons));

        for (Dungeon d : plugin.getDungeonManager().getDungeons()) {
            sender.sendMessage(ChatUtils.colorize("&e- Checking: " + d.getId()));
            org.bukkit.World w = Bukkit.getWorld(d.getWorld());
            if (w == null) {
                sender.sendMessage(ChatUtils.colorize("  &c✖ World not found: " + d.getWorld()));
                continue;
            }
            sender.sendMessage(ChatUtils.colorize("  &a✔ World loaded: " + d.getWorld()));

            for (Dungeon.Stage s : d.getStages()) {
                for (int i = 1; i <= 3; i++) {
                    Dungeon.StageLocation loc = s.getLocation(i);
                    if (loc == null || loc.getSafeZone() == null || loc.getSafeZone().isEmpty())
                        continue;

                    boolean exists = plugin.getWorldGuardHook().getRegionCenter(w, loc.getSafeZone()).isPresent();
                    if (!exists) {
                        sender.sendMessage(ChatUtils
                                .colorize("  &c✖ SafeZone missing: " + loc.getSafeZone() + " (Instance " + i + ")"));
                    } else {
                        sender.sendMessage(ChatUtils
                                .colorize("  &a✔ SafeZone OK: " + loc.getSafeZone() + " (Instance " + i + ")"));
                    }
                }
            }
        }
        sender.sendMessage(ChatUtils.colorize("&aDiagnostics complete!"));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (!sender.hasPermission("naturaldungeon.admin"))
            return completions;

        if (args.length == 1) {
            completions
                    .addAll(Arrays.asList("reload", "version", "setup", "test", "forceend", "list", "info", "admin"));
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("setup") || sub.equals("test") || sub.equals("info")) {
                completions.addAll(plugin.getDungeonManager().getDungeonIds());
            }
        }
        return completions;
    }
}
