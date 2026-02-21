package id.naturalsmp.naturaldungeon.queue;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Queue system for players waiting to enter a dungeon.
 */
public class DungeonQueue {

    private final NaturalDungeon plugin;
    // dungeonId -> queue of player UUIDs
    private final Map<String, Queue<UUID>> queues = new HashMap<>();

    public DungeonQueue(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    /**
     * Add a player to the queue for a dungeon.
     */
    public boolean joinQueue(Player player, String dungeonId) {
        UUID uuid = player.getUniqueId();

        // Check already in a queue
        for (Map.Entry<String, Queue<UUID>> entry : queues.entrySet()) {
            if (entry.getValue().contains(uuid)) {
                player.sendMessage(ChatUtils.colorize("&cKamu sudah ada di antrian &f" + entry.getKey() + "&c!"));
                return false;
            }
        }

        Queue<UUID> queue = queues.computeIfAbsent(dungeonId, k -> new ConcurrentLinkedQueue<>());
        queue.add(uuid);

        player.sendMessage(
                ChatUtils.colorize("&aKamu masuk antrian untuk &f" + dungeonId + "&a! Posisi: &f#" + queue.size()));

        // Check if queue is ready to start
        int minPlayers = plugin.getConfig().getInt("queue.min-players", 1);
        if (queue.size() >= minPlayers) {
            startFromQueue(dungeonId);
        }

        return true;
    }

    /**
     * Remove a player from all queues.
     */
    public void leaveQueue(Player player) {
        UUID uuid = player.getUniqueId();
        for (Map.Entry<String, Queue<UUID>> entry : queues.entrySet()) {
            if (entry.getValue().remove(uuid)) {
                player.sendMessage(ChatUtils.colorize("&cKamu keluar dari antrian &f" + entry.getKey() + "&c."));
                return;
            }
        }
        player.sendMessage(ChatUtils.colorize("&cKamu tidak ada di antrian manapun."));
    }

    /**
     * Get queue position for a player.
     */
    public int getPosition(Player player, String dungeonId) {
        Queue<UUID> queue = queues.get(dungeonId);
        if (queue == null)
            return -1;
        int pos = 0;
        for (UUID uuid : queue) {
            pos++;
            if (uuid.equals(player.getUniqueId()))
                return pos;
        }
        return -1;
    }

    /**
     * Get queue size for a dungeon.
     */
    public int getQueueSize(String dungeonId) {
        Queue<UUID> queue = queues.get(dungeonId);
        return queue == null ? 0 : queue.size();
    }

    /**
     * Start a dungeon run from the queue.
     */
    private void startFromQueue(String dungeonId) {
        Queue<UUID> queue = queues.get(dungeonId);
        if (queue == null || queue.isEmpty())
            return;

        int maxPlayers = plugin.getConfig().getInt("queue.max-players", 4);
        List<Player> partyFromQueue = new ArrayList<>();

        while (!queue.isEmpty() && partyFromQueue.size() < maxPlayers) {
            UUID uuid = queue.poll();
            Player player = org.bukkit.Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                partyFromQueue.add(player);
            }
        }

        if (partyFromQueue.isEmpty())
            return;

        // Notify players
        for (Player p : partyFromQueue) {
            p.sendMessage(ChatUtils.colorize("&a&lAntrian siap! &fMemulai dungeon &e" + dungeonId + "&f..."));
        }

        // Start the dungeon with queued players
        id.naturalsmp.naturaldungeon.dungeon.Dungeon dungeon = plugin.getDungeonManager().getDungeon(dungeonId);
        if (dungeon == null)
            return;

        List<UUID> uuids = partyFromQueue.stream().map(Player::getUniqueId)
                .collect(java.util.stream.Collectors.toList());
        plugin.getDungeonManager().startDungeon(uuids, dungeon, dungeon.getDifficulty("normal"));
    }

    /**
     * Clear all queues (used on disable).
     */
    public void clearAll() {
        queues.clear();
    }
}
