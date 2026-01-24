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

        plugin.getDungeonManager().openDungeonGUI(player);
        return true;
    }
}
