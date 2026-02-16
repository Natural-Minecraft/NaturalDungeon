package id.naturalsmp.naturaldungeon.dungeon;

/**
 * Types of stages in a dungeon.
 */
public enum StageType {
    WAVE_DEFENSE,
    TRAP,
    PUZZLE,
    PARKOUR;

    public static StageType fromString(String str) {
        try {
            return valueOf(str.toUpperCase());
        } catch (IllegalArgumentException e) {
            return WAVE_DEFENSE; // default
        }
    }
}
