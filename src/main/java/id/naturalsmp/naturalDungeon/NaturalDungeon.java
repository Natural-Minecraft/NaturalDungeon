package id.naturalsmp.naturaldungeon;

import id.naturalsmp.naturaldungeon.party.PartyManager;
import id.naturalsmp.naturaldungeon.party.PartyGUI;
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
import id.naturalsmp.naturaldungeon.integration.ModelEngineHook;
import id.naturalsmp.naturaldungeon.commands.DungeonCommand;
import id.naturalsmp.naturaldungeon.commands.PartyCommand;
import id.naturalsmp.naturaldungeon.commands.AdminCommand;
import id.naturalsmp.naturaldungeon.dungeon.DungeonGUI;
import id.naturalsmp.naturaldungeon.dungeon.BuffChoiceGUI;
import id.naturalsmp.naturaldungeon.dungeon.DungeonCompletionGUI;
import id.naturalsmp.naturaldungeon.leaderboard.LeaderboardGUI;
import id.naturalsmp.naturaldungeon.leaderboard.SQLiteStorage;
import id.naturalsmp.naturaldungeon.stats.PlayerStatsManager;
import id.naturalsmp.naturaldungeon.stats.StatsGUI;
import id.naturalsmp.naturaldungeon.editor.*;
import id.naturalsmp.naturaldungeon.mob.CustomMobManager;
import id.naturalsmp.naturaldungeon.mob.CustomMobSpawner;
import id.naturalsmp.naturaldungeon.skill.SkillRegistry;
import id.naturalsmp.naturaldungeon.skill.SkillExecutor;
import id.naturalsmp.naturaldungeon.player.AchievementManager;
import id.naturalsmp.naturaldungeon.queue.DungeonQueue;
import org.bukkit.plugin.java.JavaPlugin;

public final class NaturalDungeon extends JavaPlugin {

    private static NaturalDungeon instance;

    // Managers
    private PartyManager partyManager;
    private DungeonManager dungeonManager;
    private LootManager lootManager;
    private id.naturalsmp.naturaldungeon.leaderboard.LeaderboardManager leaderboardManager;
    private PlayerStatsManager playerStatsManager;
    private SQLiteStorage sqliteStorage;
    private StatsGUI statsGUI;
    private CustomMobManager customMobManager;
    private CustomMobSpawner customMobSpawner;
    private ChatInputHandler editorChatInput;
    private SkillRegistry skillRegistry;
    private SkillExecutor skillExecutor;
    private DungeonQueue dungeonQueue;
    private AchievementManager achievementManager;

    // Hooks
    private VaultHook vaultHook;
    private WorldGuardHook worldGuardHook;
    private MythicMobsHook mythicMobsHook;
    private MMOItemsHook mmoItemsHook;
    private AuraSkillsHook auraSkillsHook;
    private NaturalCoreHook naturalCoreHook;
    private AuraMobsHook auraMobsHook;
    private ModelEngineHook modelEngineHook;

    // VFX
    private id.naturalsmp.naturaldungeon.vfx.DamageNumberManager damageNumberManager;

    // Singleton GUI Listeners
    private DungeonGUI dungeonGUI;
    private BuffChoiceGUI buffChoiceGUI;
    private DungeonCompletionGUI dungeonCompletionGUI;
    private LeaderboardGUI leaderboardGUI;
    private PartyGUI partyGUI;

    private boolean maintenanceMode = false;

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
        this.playerStatsManager = new PlayerStatsManager(this);
        this.sqliteStorage = new SQLiteStorage(this);
        this.statsGUI = new StatsGUI(this);
        this.customMobManager = new CustomMobManager(this);
        this.customMobSpawner = new CustomMobSpawner(this);
        this.editorChatInput = new ChatInputHandler();
        this.skillRegistry = new SkillRegistry(this);
        this.skillExecutor = new SkillExecutor(this, skillRegistry);
        this.dungeonQueue = new DungeonQueue(this);
        // Player Managers
        this.achievementManager = new id.naturalsmp.naturaldungeon.player.AchievementManager(this);

        // 4. Register Commands
        registerCommands();

        // 5. Register Listeners
        registerListeners();
        getServer().getPluginManager().registerEvents(new id.naturalsmp.naturaldungeon.player.AchievementGUI(this),
                this);
        getServer().getPluginManager().registerEvents(new id.naturalsmp.naturaldungeon.loot.LootPreviewGUI(this), this);

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

    public boolean isMaintenanceMode() {
        return maintenanceMode;
    }

    public void setMaintenanceMode(boolean maintenanceMode) {
        this.maintenanceMode = maintenanceMode;
    }

    @Override
    public void onDisable() {
        if (dungeonManager != null) {
            dungeonManager.shutdown();
        }
        if (playerStatsManager != null) {
            playerStatsManager.saveAll();
        }
        if (sqliteStorage != null) {
            sqliteStorage.close();
        }
        if (customMobManager != null) {
            customMobManager.saveAll();
        }
        if (dungeonQueue != null) {
            dungeonQueue.clearAll();
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
        this.modelEngineHook = new ModelEngineHook(this);
        if (modelEngineHook.isEnabled()) {
            getLogger().info("ModelEngine hooked!");
        }

        // VFX
        this.damageNumberManager = new id.naturalsmp.naturaldungeon.vfx.DamageNumberManager(this);
    }

    private void registerCommands() {
        DungeonCommand dungeonCommand = new DungeonCommand(this);
        getCommand("dungeon").setExecutor(dungeonCommand);
        getCommand("dungeon").setTabCompleter(dungeonCommand);
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

        // Singleton GUI listeners (registered ONCE to prevent listener leak)
        this.dungeonGUI = new DungeonGUI(this);
        this.buffChoiceGUI = new BuffChoiceGUI(this);
        this.dungeonCompletionGUI = new DungeonCompletionGUI(this);
        this.leaderboardGUI = new LeaderboardGUI(this);
        this.partyGUI = new PartyGUI(this, partyManager);
        getServer().getPluginManager().registerEvents(dungeonGUI, this);
        getServer().getPluginManager().registerEvents(new DifficultyGUI(this), this);
        getServer().getPluginManager().registerEvents(buffChoiceGUI, this);
        getServer().getPluginManager().registerEvents(dungeonCompletionGUI, this);
        getServer().getPluginManager().registerEvents(leaderboardGUI, this);
        getServer().getPluginManager().registerEvents(partyGUI, this);
        getServer().getPluginManager().registerEvents(new PartyConfirmationGUI(this), this);
        getServer().getPluginManager().registerEvents(statsGUI, this);
        getServer().getPluginManager().registerEvents(playerStatsManager, this); // Cache eviction on quit

        // Editor GUI listeners
        getServer().getPluginManager().registerEvents(editorChatInput, this);
        getServer().getPluginManager().registerEvents(new DungeonListEditorGUI(this), this);
        getServer().getPluginManager().registerEvents(new DungeonMainEditorGUI(this), this);
        getServer().getPluginManager().registerEvents(new BasicInfoEditorGUI(this), this);
        getServer().getPluginManager().registerEvents(new DifficultyEditorGUI(this), this);
        getServer().getPluginManager().registerEvents(new DifficultyConfigGUI(this), this);
        getServer().getPluginManager().registerEvents(new StageEditorGUI(this), this);
        getServer().getPluginManager().registerEvents(new WaveEditorGUI(this), this);
        getServer().getPluginManager().registerEvents(new WaveConfigGUI(this), this);
        getServer().getPluginManager().registerEvents(new BossConfigGUI(this), this);
        getServer().getPluginManager().registerEvents(new RewardEditorGUI(this), this);
        getServer().getPluginManager().registerEvents(new LootEntryEditorGUI(this), this);
        getServer().getPluginManager().registerEvents(new MobEditorGUI(this), this);
        getServer().getPluginManager().registerEvents(new BossEditorGUI(this), this);
        getServer().getPluginManager().registerEvents(new MobSkillEditorGUI(this), this);

        // Admin GUIs
        getServer().getPluginManager().registerEvents(new id.naturalsmp.naturaldungeon.admin.AdminDashboardGUI(this),
                this);
        getServer().getPluginManager().registerEvents(new id.naturalsmp.naturaldungeon.admin.SetupWizardGUI(this),
                this);
        getServer().getPluginManager().registerEvents(new id.naturalsmp.naturaldungeon.admin.DiagnosticsGUI(this),
                this);
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
            // Must exactly match a dungeon world name, not substring
            boolean inDungeonWorld = dungeonManager.getDungeons().stream()
                    .anyMatch(d -> d.getWorld().equals(currentWorld));

            // Also verify the player is NOT in the main overworld (avoid false positive
            // if dungeon world has similar naming)
            boolean isMainWorld = currentWorld.equals(getServer().getWorlds().get(0).getName());

            if (inDungeonWorld && !isMainWorld) {
                getLogger().warning("Stuck player detected: " + p.getName() + " in " + currentWorld);
                p.teleport(getServer().getWorlds().get(0).getSpawnLocation());
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

    public ModelEngineHook getModelEngineHook() {
        return modelEngineHook;
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

    public boolean hasModelEngine() {
        return modelEngineHook != null && modelEngineHook.isEnabled();
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

    public DungeonGUI getDungeonGUI() {
        return dungeonGUI;
    }

    public BuffChoiceGUI getBuffChoiceGUI() {
        return buffChoiceGUI;
    }

    public DungeonCompletionGUI getDungeonCompletionGUI() {
        return dungeonCompletionGUI;
    }

    public LeaderboardGUI getLeaderboardGUI() {
        return leaderboardGUI;
    }

    public PartyGUI getPartyGUI() {
        return partyGUI;
    }

    public PlayerStatsManager getPlayerStatsManager() {
        return playerStatsManager;
    }

    public SQLiteStorage getSqliteStorage() {
        return sqliteStorage;
    }

    public StatsGUI getStatsGUI() {
        return statsGUI;
    }

    public CustomMobManager getCustomMobManager() {
        return customMobManager;
    }

    public CustomMobSpawner getCustomMobSpawner() {
        return customMobSpawner;
    }

    public ChatInputHandler getEditorChatInput() {
        return editorChatInput;
    }

    public SkillRegistry getSkillRegistry() {
        return skillRegistry;
    }

    public SkillExecutor getSkillExecutor() {
        return skillExecutor;
    }

    public DungeonQueue getDungeonQueue() {
        return dungeonQueue;
    }

    public id.naturalsmp.naturaldungeon.vfx.DamageNumberManager getDamageNumberManager() {
        return damageNumberManager;
    }

    public id.naturalsmp.naturaldungeon.player.AchievementManager getAchievementManager() {
        return achievementManager;
    }
}
