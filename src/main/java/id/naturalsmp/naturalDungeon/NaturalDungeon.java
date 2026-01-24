package id.naturalsmp.naturaldungeon;

import id.naturalsmp.naturaldungeon.party.PartyManager;
import id.naturalsmp.naturaldungeon.party.PartyConfirmationGUI;
import id.naturalsmp.naturaldungeon.dungeon.DungeonManager;
import id.naturalsmp.naturaldungeon.dungeon.DungeonListener;
import id.naturalsmp.naturaldungeon.dungeon.DifficultyGUI;
import id.naturalsmp.naturaldungeon.loot.LootManager;
import id.naturalsmp.naturaldungeon.region.WorldGuardHook;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import id.naturalsmp.naturaldungeon.utils.ConfigUtils;
import id.naturalsmp.naturaldungeon.integration.VaultHook;
import id.naturalsmp.naturaldungeon.integration.MythicMobsHook;
import id.naturalsmp.naturaldungeon.integration.MMOItemsHook;
import id.naturalsmp.naturaldungeon.integration.AuraSkillsHook;
import id.naturalsmp.naturaldungeon.integration.NaturalCoreHook;
import id.naturalsmp.naturaldungeon.integration.PlaceholderAPIHook;
import id.naturalsmp.naturaldungeon.integration.AuraMobsHook;
import id.naturalsmp.naturaldungeon.commands.DungeonCommand;
import id.naturalsmp.naturaldungeon.commands.PartyCommand;
import id.naturalsmp.naturaldungeon.commands.AdminCommand;
import id.naturalsmp.naturaldungeon.admin.AdminGUI;
import org.bukkit.plugin.java.JavaPlugin;

public final class NaturalDungeon extends JavaPlugin {

    private static NaturalDungeon instance;

    // Managers
    private PartyManager partyManager;
    private DungeonManager dungeonManager;
    private LootManager lootManager;
    private id.naturalsmp.naturaldungeon.leaderboard.LeaderboardManager leaderboardManager;

    // Hooks
    private VaultHook vaultHook;
    private WorldGuardHook worldGuardHook;
    private MythicMobsHook mythicMobsHook;
    private MMOItemsHook mmoItemsHook;
    private AuraSkillsHook auraSkillsHook;
    private NaturalCoreHook naturalCoreHook;
    private AuraMobsHook auraMobsHook;

    private static final int CONFIG_VERSION = 1;

    @Override
    public void onEnable() {
        instance = this;

        // 1. Load Configs
        saveDefaultConfig();
        saveResource("messages.yml", false);
        saveResource("dungeons/forest_cave.yml", false);
        ConfigUtils.init(this);

        // Check Config Version
        int currentConfigVersion = getConfig().getInt("config-version", 0);
        if (currentConfigVersion < CONFIG_VERSION) {
            getLogger().warning("Your config.yml is outdated! (Current: " + currentConfigVersion + ", Required: "
                    + CONFIG_VERSION + ")");
            getLogger().warning("Please regenerate your config or update it manually to avoid issues.");
        }

        getLogger().info(ChatUtils.colorize("&6&lNaturalDungeon &aLoading..."));

        // 2. Setup Hooks
        setupHooks();

        // 3. Init Managers
        this.partyManager = new PartyManager(this);
        this.dungeonManager = new DungeonManager(this);
        this.lootManager = new LootManager(this);
        this.leaderboardManager = new id.naturalsmp.naturaldungeon.leaderboard.LeaderboardManager(this);

        // 4. Register Commands
        registerCommands();

        // 5. Register Listeners
        registerListeners();

        // 6. Register PAPI
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderAPIHook(this).register();
        }

        // 7. Stuck Recovery (Post-crash safety)
        getServer().getScheduler().runTaskLater(this, this::performStuckRecovery, 100L); // Wait 5s

        getLogger().info(ChatUtils.colorize("&6&lNaturalDungeon &aEnabled! &7v" + getDescription().getVersion()));
        getLogger().info("Loaded " + dungeonManager.getDungeonIds().size() + " dungeons.");
    }

    public int getConfigVersion() {
        return CONFIG_VERSION;
    }

    @Override
    public void onDisable() {
        if (dungeonManager != null) {
            dungeonManager.shutdown();
        }
        getLogger().info(ChatUtils.colorize("&6&lNaturalDungeon &cDisabled!"));
    }

    private void setupHooks() {
        this.worldGuardHook = new WorldGuardHook(this);
        this.vaultHook = new VaultHook(this);
        if (!vaultHook.isEnabled()) {
            getLogger().warning("Vault not found! Economy features disabled.");
        }

        if (getServer().getPluginManager().getPlugin("MythicMobs") != null) {
            this.mythicMobsHook = new MythicMobsHook(this);
            getLogger().info("MythicMobs hooked!");
        } else {
            getLogger().info("MythicMobs not found. Using vanilla mobs.");
        }

        if (getServer().getPluginManager().getPlugin("MMOItems") != null) {
            this.mmoItemsHook = new MMOItemsHook(this);
            getLogger().info("MMOItems hooked!");
        } else {
            getLogger().info("MMOItems not found. Using vanilla loot.");
        }

        if (getServer().getPluginManager().getPlugin("AuraSkills") != null) {
            this.auraSkillsHook = new AuraSkillsHook(this);
            getLogger().info("AuraSkills hooked!");
        } else {
            getLogger().info("AuraSkills not found. XP rewards disabled.");
        }

        this.naturalCoreHook = new NaturalCoreHook(this);
        this.auraMobsHook = new AuraMobsHook(this);
    }

    private void registerCommands() {
        getCommand("dungeon").setExecutor(new DungeonCommand(this));
        PartyCommand partyCommand = new PartyCommand(this);
        getCommand("party").setExecutor(partyCommand);
        getCommand("party").setTabCompleter(partyCommand);
        AdminCommand adminCommand = new AdminCommand(this);
        getCommand("nd").setExecutor(adminCommand);
        getCommand("nd").setTabCompleter(adminCommand);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(partyManager, this);
        getServer().getPluginManager().registerEvents(dungeonManager, this);
        getServer().getPluginManager().registerEvents(new DungeonListener(this), this);
        getServer().getPluginManager().registerEvents(new DifficultyGUI(this), this);
        getServer().getPluginManager().registerEvents(new PartyConfirmationGUI(this), this);
        getServer().getPluginManager().registerEvents(new AdminGUI(this), this);
    }

    public void reload() {
        reloadConfig();
        ConfigUtils.reload();
        dungeonManager.loadDungeons();
        getLogger().info("NaturalDungeon reloaded!");
    }

    public static NaturalDungeon getInstance() {
        return instance;
    }

    private void performStuckRecovery() {
        getLogger().info("Checking for stuck players in dungeon worlds...");
        for (org.bukkit.entity.Player p : getServer().getOnlinePlayers()) {
            if (dungeonManager.isInDungeon(p))
                continue; // Actually in an active session

            String currentWorld = p.getWorld().getName();
            // Check if player is in a designated dungeon world but not in a session
            // We assume dungeons have specific keywords or we check against loaded dungeon
            // worlds
            boolean inDungeonWorld = dungeonManager.getDungeons().stream()
                    .anyMatch(d -> d.getWorld().equalsIgnoreCase(currentWorld));

            if (inDungeonWorld) {
                getLogger().warning("Stuck player detected: " + p.getName() + " in " + currentWorld);
                p.teleport(getServer().getWorlds().get(0).getSpawnLocation()); // Return to main world spawn
                p.sendMessage(ChatUtils.colorize(
                        "&6&lNaturalDungeon &8» &eSesi dungeon kamu berakhir (Server restart/crash). Kembali ke Spawn!"));
            }
        }
    }

    public PartyManager getPartyManager() {
        return partyManager;
    }

    public DungeonManager getDungeonManager() {
        return dungeonManager;
    }

    public LootManager getLootManager() {
        return lootManager;
    }

    public id.naturalsmp.naturaldungeon.leaderboard.LeaderboardManager getLeaderboardManager() {
        return leaderboardManager;
    }

    public VaultHook getVaultHook() {
        return vaultHook;
    }

    public WorldGuardHook getWorldGuardHook() {
        return worldGuardHook;
    }

    public MythicMobsHook getMythicMobsHook() {
        return mythicMobsHook;
    }

    public MMOItemsHook getMmoItemsHook() {
        return mmoItemsHook;
    }

    public AuraSkillsHook getAuraSkillsHook() {
        return auraSkillsHook;
    }

    public boolean hasMythicMobs() {
        return mythicMobsHook != null;
    }

    public boolean hasMMOItems() {
        return mmoItemsHook != null;
    }

    public boolean hasAuraSkills() {
        return auraSkillsHook != null;
    }

    public boolean hasWorldGuard() {
        return worldGuardHook != null && worldGuardHook.isEnabled();
    }

    public NaturalCoreHook getNaturalCoreHook() {
        return naturalCoreHook;
    }

    public AuraMobsHook getAuraMobsHook() {
        return auraMobsHook;
    }
}
