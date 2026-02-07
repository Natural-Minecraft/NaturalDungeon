package id.naturalsmp.naturaldungeon.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.List;
import java.util.UUID;

/**
 * Fired when a dungeon instance is completed successfully.
 */
public class DungeonCompleteEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String dungeonId;
    private final String difficulty;
    private final List<UUID> playerUuids;

    public DungeonCompleteEvent(String dungeonId, String difficulty, List<UUID> playerUuids) {
        this.dungeonId = dungeonId;
        this.difficulty = difficulty;
        this.playerUuids = playerUuids;
    }

    public String getDungeonId() {
        return dungeonId;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public List<UUID> getPlayerUuids() {
        return playerUuids;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
