package id.naturalsmp.naturaldungeon.progression;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.UUID;

/**
 * Weekly Challenge System: e.g. "Complete 10 dungeons this week"
 * Gives a reward upon completion.
 */
public class WeeklyChallengeManager {

    private final NaturalDungeon plugin;
    private final File file;
    private YamlConfiguration config;

    private int currentWeek;
    private int currentYear;

    private final int TARGET_CLEARS = 10;

    public WeeklyChallengeManager(NaturalDungeon plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "weekly_challenges.yml");
        loadConfig();
        checkWeekReset();
    }

    private void loadConfig() {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to create weekly_challenges.yml");
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    private void saveConfig() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save weekly_challenges.yml");
        }
    }

    private void checkWeekReset() {
        LocalDate now = LocalDate.now();
        int week = now.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int year = now.get(IsoFields.WEEK_BASED_YEAR);

        int savedWeek = config.getInt("metadata.week", -1);
        int savedYear = config.getInt("metadata.year", -1);

        if (savedWeek != week || savedYear != year) {
            // New week! Reset everything
            config.set("players", null);
            config.set("metadata.week", week);
            config.set("metadata.year", year);
            saveConfig();

            this.currentWeek = week;
            this.currentYear = year;
            plugin.getLogger().info("Started new Weekly Challenge: Week " + week + ", " + year);
        } else {
            this.currentWeek = savedWeek;
            this.currentYear = savedYear;
        }
    }

    /**
     * Add progress to the player's weekly challenge. Called on dungeon clear.
     */
    public void addProgress(UUID uuid) {
        checkWeekReset();

        String path = "players." + uuid.toString();
        if (config.getBoolean(path + ".completed", false)) {
            return; // Already did it this week
        }

        int currentClears = config.getInt(path + ".clears", 0);
        currentClears++;
        config.set(path + ".clears", currentClears);

        Player p = Bukkit.getPlayer(uuid);

        if (currentClears >= TARGET_CLEARS) {
            // Completed!
            config.set(path + ".completed", true);
            if (p != null) {
                p.sendMessage(ChatUtils.colorize("&#FFD700&l🏆 WEEKLY CHALLENGE COMPLETE!"));
                p.sendMessage(
                        ChatUtils.colorize("&7Kamu telah menyelesaikan " + TARGET_CLEARS + " dungeon minggu ini."));
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);

                // Reward: e.g. 5000 economy money or Vault hook
                if (plugin.getVaultHook() != null && plugin.getVaultHook().isEconomyEnabled()) {
                    plugin.getVaultHook().giveMoney(p, 5000);
                    p.sendMessage(ChatUtils.colorize("&#55FF55&l+ $5,000"));
                } else {
                    // Fallback command or NaturalCoin
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "eco give " + p.getName() + " 5000");
                }
            }
        } else {
            if (p != null) {
                p.sendMessage(ChatUtils.colorize(
                        "&eWeekly Challenge: &f" + currentClears + "&7/&f" + TARGET_CLEARS + " &7dungeons cleared."));
            }
        }

        saveConfig();
    }

    public int getProgress(UUID uuid) {
        return config.getInt("players." + uuid.toString() + ".clears", 0);
    }

    public boolean isCompleted(UUID uuid) {
        return config.getBoolean("players." + uuid.toString() + ".completed", false);
    }
}
