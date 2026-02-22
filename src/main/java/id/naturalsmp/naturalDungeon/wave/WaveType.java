package id.naturalsmp.naturaldungeon.wave;

public enum WaveType {
    KILL_ALL,
    DEFEND_TARGET,
    CAPTURE_ZONE,
    HUNT_TARGET,
    // Phase 3 wave types
    SURVIVAL, // Survive for X seconds against endless respawns
    ESCORT, // Protect a moving NPC to destination
    DESTRUCTION, // Destroy target entities/blocks within time
    MINI_BOSS // Single tough mob — must kill to proceed
}
