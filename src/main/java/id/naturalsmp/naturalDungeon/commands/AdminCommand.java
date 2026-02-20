package id.naturalsmp.naturaldungeon.commands;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.admin.SetupManager;
import id.naturalsmp.naturaldungeon.dungeon.Dungeon;
import id.naturalsmp.naturaldungeon.dungeon.DungeonInstance;
import id.naturalsmp.naturaldungeon.editor.DungeonListEditorGUI;
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
            case "clone" -> {
                if (args.length < 3) {
                    sender.sendMessage(ChatUtils.colorize("&cGunakan: /nd clone <source_id> <new_id>"));
                    return true;
                }
                String sourceId = args[1];
                String newId = args[2];
                if (plugin.getDungeonManager().getDungeon(sourceId) == null) {
                    sender.sendMessage(ChatUtils.colorize("&cDungeon sumber tidak ditemukan: " + sourceId));
                    return true;
                }
                if (plugin.getDungeonManager().getDungeon(newId) != null) {
                    sender.sendMessage(ChatUtils.colorize("&cDungeon dengan ID " + newId + " sudah ada!"));
                    return true;
                }
                boolean success = plugin.getDungeonManager().cloneDungeon(sourceId, newId);
                if (success) {
                    sender.sendMessage(
                            ChatUtils.colorize("&a✔ Berhasil meng-clone dungeon &e" + sourceId + " &ake &e" + newId));
                } else {
                    sender.sendMessage(ChatUtils.colorize("&c✖ Gagal meng-clone dungeon. Cek console."));
                }
            }
            case "status" -> {
                sender.sendMessage(ChatUtils.colorize("&6&lStatus Dungeon Aktif:"));
                List<DungeonInstance> active = plugin.getDungeonManager().getActiveInstances();
                if (active.isEmpty()) {
                    sender.sendMessage(ChatUtils.colorize("&7Tidak ada dungeon yang sedang berjalan."));
                    return true;
                }
                for (DungeonInstance instance : active) {
                    String time = ChatUtils.formatTime(instance.getDuration() / 1000);
                    int players = instance.getParticipants().size();
                    sender.sendMessage(ChatUtils.colorize("  &e" + instance.getDungeon().getId() +
                            " &7| Stage: &f" + instance.getCurrentStage() +
                            " &7| Wave: &f" + instance.getCurrentWave() +
                            " &7| Players: &f" + players +
                            " &7| Waktu: &f" + time));
                }
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
            case "editor" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ConfigUtils.getMessage("general.player-only"));
                    return true;
                }
                new DungeonListEditorGUI(plugin).open(player);
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
            case "set" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ConfigUtils.getMessage("general.player-only"));
                    return true;
                }
                if (args.length < 3) {
                    player.sendMessage(ChatUtils.colorize("&cGunakan: /nd set <safezone|arena> <pos1|pos2>"));
                    return true;
                }
                String type = args[1].toLowerCase();
                String posStr = args[2].toLowerCase();
                int posNum = posStr.equals("pos1") ? 1 : (posStr.equals("pos2") ? 2 : -1);

                if (!type.equals("safezone") && !type.equals("arena") && !type.matches("stage\\d+")) {
                    player.sendMessage(ChatUtils.colorize("&cInvalid type. Use 'safezone' or 'arena'."));
                    return true;
                }

                // Allow "stage1", "stage2" aliases to map to "arena"
                if (type.startsWith("stage"))
                    type = "arena";

                if (posNum == -1) {
                    player.sendMessage(ChatUtils.colorize("&cInvalid pos. Use 'pos1' or 'pos2'."));
                    return true;
                }

                setupManager.setPos(player, type, posNum, player.getLocation());
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
        instance.getWaveManager().spawnWave(testWave, player.getLocation(), 1, false, Collections.emptyList());
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
        sender.sendMessage(ChatUtils.colorize("&e/nd set <safezone|arena> <pos1|pos2> &7- Set region points"));
        sender.sendMessage(ChatUtils.colorize("&e/nd editor &7- Open dungeon editor GUI"));
        sender.sendMessage(ChatUtils.colorize("&e/nd clone <source> <new_id> &7- Clone dungeon config"));
        sender.sendMessage(ChatUtils.colorize("&e/nd status &7- Show active dungeon instances"));
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
                    .addAll(Arrays.asList("reload", "version", "setup", "test", "forceend", "list", "info", "admin",
                            "editor", "clone", "status"));
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("setup") || sub.equals("test") || sub.equals("info") || sub.equals("clone")) {
                completions.addAll(plugin.getDungeonManager().getDungeonIds());
            } else if (sub.equals("set")) {
                completions.addAll(Arrays.asList("safezone", "arena"));
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            completions.addAll(Arrays.asList("pos1", "pos2"));
        }
        return completions;
    }
}
