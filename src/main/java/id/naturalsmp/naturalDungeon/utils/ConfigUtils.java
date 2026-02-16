package id.naturalsmp.naturaldungeon.utils;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class ConfigUtils {

    private static NaturalDungeon plugin;
    private static FileConfiguration messages;

    public static void init(NaturalDungeon instance) {
        plugin = instance;
        loadMessages();
    }

    public static void reload() {
        plugin.reloadConfig();
        loadMessages();
    }

    private static void loadMessages() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public static String getMessage(String path) {
        String message = messages.getString(path);
        if (message == null)
            return "Missing message: " + path;
        String prefix = messages.getString("prefix", "&c&l⚔ &7");
        return ChatUtils.colorize(message.replace("%prefix%", prefix));
    }

    public static String getMessage(String path, String... replacements) {
        String message = getMessage(path);
        for (int i = 0; i < replacements.length - 1; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }
        return message;
    }

    public static String getString(String path) {
        return plugin.getConfig().getString(path, "");
    }

    public static int getInt(String path) {
        return plugin.getConfig().getInt(path, 0);
    }

    public static int getInt(String path, int def) {
        return plugin.getConfig().getInt(path, def);
    }

    public static double getDouble(String path) {
        return plugin.getConfig().getDouble(path, 0.0);
    }

    public static boolean getBoolean(String path) {
        return plugin.getConfig().getBoolean(path, false);
    }
}
