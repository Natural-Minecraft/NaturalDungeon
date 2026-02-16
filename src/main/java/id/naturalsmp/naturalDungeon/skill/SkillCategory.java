package id.naturalsmp.naturaldungeon.skill;

/**
 * Categories for mob skills, used for organization and filtering.
 */
public enum SkillCategory {
    MELEE("Melee", "Serangan jarak dekat"),
    RANGED("Ranged", "Serangan jarak jauh"),
    AOE("AoE", "Serangan area"),
    DEBUFF("Debuff", "Efek negatif ke pemain"),
    BUFF("Buff", "Efek positif untuk mob"),
    SUMMON("Summon", "Memanggil bantuan"),
    MOVEMENT("Movement", "Gerakan khusus"),
    DEFENSE("Defense", "Pertahanan"),
    SPECIAL("Special", "Skill unik"),
    BOSS("Boss", "Skill eksklusif boss");

    private final String displayName;
    private final String description;

    SkillCategory(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
