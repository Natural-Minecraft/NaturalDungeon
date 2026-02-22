package id.naturalsmp.naturaldungeon.player;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class AchievementManager {

    private final NaturalDungeon plugin;
    private File file;
    private FileConfiguration config;

    private final List<Achievement> registeredAchievements = new ArrayList<>();

    public AchievementManager(NaturalDungeon plugin) {
        this.plugin = plugin;
        setupFile();
        registerDefaultAchievements();
    }

    private void setupFile() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }
        file = new File(plugin.getDataFolder(), "achievements.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create achievements.yml!");
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save achievements.yml!");
        }
    }

    private void registerDefaultAchievements() {
        registeredAchievements.add(new Achievement("first_blood", "First Blood", "Kill your first dungeon mob.", 1000));
        registeredAchievements.add(new Achievement("novice", "Dungeon Novice", "Clear your first dungeon.", 5000));
        registeredAchievements.add(new Achievement("expert", "Dungeon Expert", "Clear 10 dungeons.", 25000));
        registeredAchievements.add(new Achievement("master", "Dungeon Master", "Clear 100 dungeons.", 100000));
        registeredAchievements
                .add(new Achievement("flawless", "Flawless Victory", "Clear a dungeon with 0 deaths.", 10000));
        registeredAchievements.add(new Achievement("mvp", "Star Player", "Get MVP in a dungeon party.", 5000));
        // Phase 3 Milestones
        registeredAchievements
                .add(new Achievement("speed_runner", "Speed Runner", "Clear a dungeon in under 5 minutes.", 15000));
        registeredAchievements.add(new Achievement("solo_warrior", "Solo Warrior", "Clear a dungeon alone.", 20000));
        registeredAchievements
                .add(new Achievement("party_player", "Party Player", "Clear a dungeon with 4 players.", 10000));
        registeredAchievements.add(new Achievement("boss_slayer", "Boss Slayer", "Kill 10 bosses total.", 30000));
        registeredAchievements.add(new Achievement("mastery_5", "Apprentice", "Reach mastery level 5.", 15000));
        registeredAchievements.add(new Achievement("mastery_20", "Legend", "Reach max mastery level 20.", 100000));
    }

    public List<Achievement> getAchievements() {
        return registeredAchievements;
    }

    public boolean hasAchievement(UUID uuid, String id) {
        return config.getBoolean(uuid.toString() + "." + id + ".unlocked", false);
    }

    public void unlockAchievement(Player player, String id) {
        if (hasAchievement(player.getUniqueId(), id))
            return; // Already unlocked

        Achievement achievement = registeredAchievements.stream().filter(a -> a.getId().equals(id)).findFirst()
                .orElse(null);
        if (achievement == null)
            return;

        config.set(player.getUniqueId().toString() + "." + id + ".unlocked", true);
        config.set(player.getUniqueId().toString() + "." + id + ".date", System.currentTimeMillis());
        save();

        // Award Money via NaturalDungeon VaultHook if available
        if (plugin.getVaultHook() != null && plugin.getVaultHook().isEnabled()) {
            plugin.getVaultHook().deposit(player, achievement.getRewardMoney());
        }

        // Send visual effects
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        player.sendTitle(ChatUtils.colorize("&#FFFF00&lACHIEVEMENT UNLOCKED"),
                ChatUtils.colorize("&f" + achievement.getName()), 10, 60, 20);
        player.sendMessage(ChatUtils.colorize(" "));
        player.sendMessage(ChatUtils.colorize("&#FFFF00&lACHIEVEMENT UNLOCKED &8» &f" + achievement.getName()));
        player.sendMessage(ChatUtils.colorize("&7" + achievement.getDescription()));
        player.sendMessage(ChatUtils.colorize("&a+ Rp " + achievement.getRewardMoney()));
        player.sendMessage(ChatUtils.colorize(" "));

        // Broadcast to server if it's a major achievement (expert, master, flawless)
        if (id.equals("expert") || id.equals("master") || id.equals("flawless")) {
            Bukkit.broadcastMessage(ChatUtils.colorize("&6&lSERVER &8» &e" + player.getName()
                    + " &7baru saja mendapatkan achievement &f&l" + achievement.getName() + "&7!"));
        }
    }

    // Progression Tracking
    public void incrementStat(UUID uuid, String statKey) {
        int current = config.getInt(uuid.toString() + ".stats." + statKey, 0);
        config.set(uuid.toString() + ".stats." + statKey, current + 1);
        save();
    }

    public int getStat(UUID uuid, String statKey) {
        return config.getInt(uuid.toString() + ".stats." + statKey, 0);
    }

    public static class Achievement {
        private final String id;
        private final String name;
        private final String description;
        private final double rewardMoney;

        public Achievement(String id, String name, String description, double rewardMoney) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.rewardMoney = rewardMoney;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public double getRewardMoney() {
            return rewardMoney;
        }
    }
}
