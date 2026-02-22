package id.naturalsmp.naturaldungeon.commands;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.admin.AdminDashboardGUI;
import id.naturalsmp.naturaldungeon.admin.DiagnosticsGUI;
import id.naturalsmp.naturaldungeon.admin.SetupManager;
import id.naturalsmp.naturaldungeon.admin.SetupWizardGUI;
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
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "reload" -> {
                plugin.reload();
                sender.sendMessage(ConfigUtils.getMessage("general.reload"));
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
                plugin.getDungeonManager().startTestDungeon(player, dungeon);
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
            case "skipwave" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ConfigUtils.getMessage("general.player-only"));
                    return true;
                }
                var instance = plugin.getDungeonManager().getActiveInstance(player);
                if (instance == null) {
                    player.sendMessage(ChatUtils.colorize("&cKamu tidak dalam dungeon!"));
                    return true;
                }
                if (instance.getWaveManager() == null) {
                    player.sendMessage(ChatUtils.colorize("&cWave manager belum aktif!"));
                    return true;
                }
                player.sendMessage(ChatUtils.colorize("&e&l[TEST MODE] &7Memaksa Wave Complete..."));
                instance.getWaveManager().completeWave();
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
                Collection<DungeonInstance> active = plugin.getDungeonManager().getActiveInstances();
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
            case "admin", "dashboard" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ConfigUtils.getMessage("general.player-only"));
                    return true;
                }
                new AdminDashboardGUI(plugin).open(player);
            }
            case "create", "wizard" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ConfigUtils.getMessage("general.player-only"));
                    return true;
                }
                new SetupWizardGUI(plugin).open(player);
            }
            case "diag", "diagnostics" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ConfigUtils.getMessage("general.player-only"));
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(ChatUtils.colorize("&#FF5555✖ &7Gunakan: /nd diag <dungeon_id>"));
                    return true;
                }
                new DiagnosticsGUI(plugin).open(player, args[1]);
            }
            case "wand" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ConfigUtils.getMessage("general.player-only"));
                    return true;
                }
                if (args.length < 3) {
                    player.sendMessage(ChatUtils.colorize("&#FF5555✖ &7Gunakan: /nd wand <dungeon_id> <stage>"));
                    return true;
                }
                Dungeon dungeon = plugin.getDungeonManager().getDungeon(args[1]);
                if (dungeon == null) {
                    player.sendMessage(ChatUtils.colorize("&#FF5555✖ &7Dungeon tidak ditemukan: &f" + args[1]));
                    return true;
                }
                try {
                    int stageNum = Integer.parseInt(args[2]);
                    setupManager.enterStageEditor(player, dungeon, stageNum);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatUtils.colorize("&#FF5555✖ &7Stage harus berupa angka!"));
                }
            }
            case "editor" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ConfigUtils.getMessage("general.player-only"));
                    return true;
                }
                if (args.length == 1) {
                    new DungeonListEditorGUI(plugin).open(player);
                } else if (args.length >= 3) {
                    Dungeon dungeon = plugin.getDungeonManager().getDungeon(args[1]);
                    if (dungeon == null) {
                        player.sendMessage(ChatUtils.colorize("&cDungeon tidak ditemukan: " + args[1]));
                        return true;
                    }
                    try {
                        int stageNum = Integer.parseInt(args[2]);
                        setupManager.enterStageEditor(player, dungeon, stageNum);
                    } catch (NumberFormatException e) {
                        player.sendMessage(ChatUtils.colorize("&cStage harus berupa angka!"));
                    }
                } else {
                    player.sendMessage(ChatUtils.colorize("&cGunakan: /nd editor [dungeon_id] [stage_number]"));
                }
            }
            case "maintenance" -> {
                boolean state = !plugin.isMaintenanceMode();
                plugin.setMaintenanceMode(state);
                if (state) {
                    sender.sendMessage(ChatUtils
                            .colorize("&c&l[!] &cMaintenance Mode Diaktifkan! Tidak ada yang bisa memulai dungeon."));
                } else {
                    sender.sendMessage(
                            ChatUtils.colorize("&a&l[!] &aMaintenance Mode Dinonaktifkan! Dungeon normal kembali."));
                }
            }
            case "debug" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ConfigUtils.getMessage("general.player-only"));
                    return true;
                }
                if (args.length >= 3 && args[1].equalsIgnoreCase("bossspawn")) {
                    String bossId = args[2];
                    player.sendMessage(ChatUtils.colorize("&aMencoba spawn boss: &e" + bossId));
                    if (plugin.getCustomMobManager().getMob(bossId) != null) {
                        plugin.getCustomMobSpawner().spawnMob(bossId, player.getLocation());
                        player.sendMessage(ChatUtils.colorize("&a✔ Boss (Custom Mob) berhasil di-spawn."));
                    } else if (plugin.getServer().getPluginManager().getPlugin("MythicMobs") != null
                            && plugin.getMythicMobsHook().isValidMob(bossId)) {
                        plugin.getMythicMobsHook().spawnMythicMob(bossId, player.getLocation());
                        player.sendMessage(ChatUtils.colorize("&a✔ Boss (MythicMobs) berhasil di-spawn."));
                    } else {
                        player.sendMessage(ChatUtils
                                .colorize("&c✖ Gagal! Boss ID tidak dikenali (bukan CustomMob atau MythicMob)."));
                    }
                } else {
                    player.sendMessage(ChatUtils.colorize("&cGunakan: /nd debug bossspawn <bossId>"));
                }
            }
            case "check" -> {
                handleCheck(sender, args);
            }

            case "sethub" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ConfigUtils.getMessage("general.player-only"));
                    return true;
                }
                plugin.getHubManager().setHubLocation(player.getLocation());
                player.sendMessage(ChatUtils.colorize("&#55FF55✔ &7Lokasi Dungeon Hub berhasil diatur!"));
            }
            case "hub" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ConfigUtils.getMessage("general.player-only"));
                    return true;
                }
                if (args.length < 3 || !args[1].equalsIgnoreCase("setboard")) {
                    player.sendMessage(
                            ChatUtils.colorize("&#FF5555✖ &7Gunakan: /nd hub setboard <portal|stats|weekly>"));
                    return true;
                }
                String type = args[2].toLowerCase();
                if (!type.equals("portal") && !type.equals("stats") && !type.equals("weekly")) {
                    player.sendMessage(
                            ChatUtils.colorize("&#FF5555✖ &7Tipe board tidak valid! Pilih: portal, stats, weekly"));
                    return true;
                }
                plugin.getHubManager().createOrUpdateBoard(type, player.getLocation());
                player.sendMessage(
                        ChatUtils.colorize("&#55FF55✔ &7Board Hologram (&e" + type + "&7) berhasil ditempatkan!"));
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    // Removed old debugspawn
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatUtils.colorize("&6&lNaturalDungeon Admin"));
        sender.sendMessage(ChatUtils.colorize("&e/nd reload &7- Reload configs"));
        sender.sendMessage(ChatUtils.colorize("&e/nd check &7- Run diagnostic checks"));

        sender.sendMessage(ChatUtils.colorize("&e/nd test <dungeon> &7- Test dungeon"));
        sender.sendMessage(ChatUtils.colorize("&e/nd forceend &7- Force end dungeon"));
        sender.sendMessage(ChatUtils.colorize("&e/nd list &7- List dungeons"));
        sender.sendMessage(ChatUtils.colorize("&e/nd info <dungeon> &7- Dungeon info"));

        sender.sendMessage(ChatUtils.colorize("&e/nd editor [dungeon] [stage] &7- Editor/Setup Mode"));
        sender.sendMessage(ChatUtils.colorize("&e/nd dashboard &7- Admin Dashboard GUI"));
        sender.sendMessage(ChatUtils.colorize("&e/nd create &7- Setup Wizard (buat dungeon baru)"));
        sender.sendMessage(ChatUtils.colorize("&e/nd diag <dungeon> &7- Visual Diagnostics GUI"));
        sender.sendMessage(ChatUtils.colorize("&e/nd wand <dungeon> <stage> &7- Setup wand tools"));
        sender.sendMessage(ChatUtils.colorize("&e/nd clone <source> <new_id> &7- Clone dungeon config"));
        sender.sendMessage(ChatUtils.colorize("&e/nd status &7- Show active dungeon instances"));
        sender.sendMessage(ChatUtils.colorize("&e/nd maintenance &7- Toggle maintenance mode"));
        sender.sendMessage(ChatUtils.colorize("&e/nd debug bossspawn <bossId> &7- Spawn test boss"));
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
            completions.addAll(Arrays.asList("reload", "version", "test", "forceend", "list", "info",
                    "editor", "dashboard", "create", "diag", "wand", "clone", "status", "maintenance", "debug",
                    "check"));
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("editor") || sub.equals("test") || sub.equals("info") || sub.equals("clone")
                    || sub.equals("diag") || sub.equals("wand")
                    || sub.equals("debug")) {
                if (sub.equals("debug")) {
                    completions.add("bossspawn");
                    return completions;
                }
                completions.addAll(plugin.getDungeonManager().getDungeonIds());
            }
        } else if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("debug") && args[1].equalsIgnoreCase("bossspawn")) {
                for (id.naturalsmp.naturaldungeon.mob.CustomMob m : plugin.getCustomMobManager().getAllMobs()) {
                    if (m.isBoss())
                        completions.add(m.getId());
                }
                if (plugin.getServer().getPluginManager().getPlugin("MythicMobs") != null) {
                    try {
                        for (String mm : io.lumine.mythic.bukkit.MythicBukkit.inst().getMobManager().getMobNames()) {
                            completions.add(mm);
                        }
                    } catch (Exception ignored) {
                    }
                }
                return completions;
            } else if (sub.equalsIgnoreCase("editor")) {
                completions.addAll(Arrays.asList("1", "2", "3", "4", "5"));
            }
        }
        return completions;
    }
}
