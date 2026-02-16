package id.naturalsmp.naturaldungeon.editor;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Utility for "type in chat" input — used by editor GUIs for names,
 * descriptions, numbers.
 */
public class ChatInputHandler implements Listener {

    private final Map<UUID, Consumer<String>> pending = new HashMap<>();

    public void requestInput(Player player, String prompt, Consumer<String> callback) {
        player.closeInventory();
        player.sendMessage(prompt);
        player.sendMessage(id.naturalsmp.naturaldungeon.utils.ChatUtils
                .colorize("&7Ketik di chat. Ketik &c'cancel' &7untuk membatalkan."));
        pending.put(player.getUniqueId(), callback);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Consumer<String> callback = pending.remove(e.getPlayer().getUniqueId());
        if (callback == null)
            return;
        e.setCancelled(true);
        String msg = e.getMessage().trim();
        if (msg.equalsIgnoreCase("cancel")) {
            e.getPlayer().sendMessage(id.naturalsmp.naturaldungeon.utils.ChatUtils.colorize("&cDibatalkan."));
            return;
        }
        // Run callback on main thread
        Bukkit.getScheduler().runTask(NaturalDungeon.getInstance(), () -> callback.accept(msg));
    }

    public boolean hasPending(UUID uuid) {
        return pending.containsKey(uuid);
    }
}
