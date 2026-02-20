package id.naturalsmp.naturaldungeon.mob;

import org.bukkit.entity.EntityType;

import java.util.*;

/**
 * Data class representing a custom mob for dungeon usage.
 */
public class CustomMob {

    private final String id;
    private String name;
    private EntityType entityType;
    private double health;
    private double damage;
    private double speed;
    private boolean boss;
    private String modelId;
    private List<String> skillIds;
    private Map<String, Object> equipment; // slot -> material/item

    public CustomMob(String id) {
        this.id = id;
        this.name = id;
        this.entityType = EntityType.ZOMBIE;
        this.health = 20.0;
        this.damage = 5.0;
        this.speed = 0.23;
        this.boss = false;
        this.skillIds = new ArrayList<>();
        this.equipment = new HashMap<>();
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public double getHealth() {
        return health;
    }

    public double getDamage() {
        return damage;
    }

    public double getSpeed() {
        return speed;
    }

    public boolean isBoss() {
        return boss;
    }

    public List<String> getSkillIds() {
        return skillIds;
    }

    public Map<String, Object> getEquipment() {
        return equipment;
    }

    // Setters
    public void setName(String name) {
        this.name = name;
    }

    public void setEntityType(EntityType type) {
        this.entityType = type;
    }

    public void setHealth(double health) {
        this.health = health;
    }

    public void setDamage(double damage) {
        this.damage = damage;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public void setBoss(boolean boss) {
        this.boss = boss;
    }

    public void setSkillIds(List<String> ids) {
        this.skillIds = ids;
    }

    public void addSkill(String skillId) {
        if (!skillIds.contains(skillId))
            skillIds.add(skillId);
    }

    public void removeSkill(String skillId) {
        skillIds.remove(skillId);
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }
}
