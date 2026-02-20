package id.naturalsmp.naturaldungeon.skill;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.skill.impl.*;

import java.util.*;

/**
 * Registry for all mob skills. Loads and provides access to all 110 skills.
 */
public class SkillRegistry {

    private final Map<String, MobSkill> skills = new LinkedHashMap<>();

    public SkillRegistry(NaturalDungeon plugin) {
        registerAll(plugin);
    }

    private void registerAll(NaturalDungeon plugin) {
        // ============ MELEE (15 skills) ============
        register(new GenericSkill("slash", "Slash", SkillCategory.MELEE, 40, 3, 6.0));
        register(new GenericSkill("heavy_strike", "Heavy Strike", SkillCategory.MELEE, 60, 3, 10.0));
        register(new GenericSkill("double_strike", "Double Strike", SkillCategory.MELEE, 50, 3, 5.0));
        register(new GenericSkill("backstab", "Backstab", SkillCategory.MELEE, 80, 3, 12.0));
        register(new GenericSkill("cleave", "Cleave", SkillCategory.MELEE, 60, 4, 8.0));
        register(new GenericSkill("uppercut", "Uppercut", SkillCategory.MELEE, 70, 3, 7.0));
        register(new GenericSkill("sweeping_slash", "Sweeping Slash", SkillCategory.MELEE, 80, 4, 9.0));
        register(new GenericSkill("bite", "Bite", SkillCategory.MELEE, 30, 2, 4.0));
        register(new GenericSkill("claw_swipe", "Claw Swipe", SkillCategory.MELEE, 40, 3, 6.0));
        register(new GenericSkill("tail_whip", "Tail Whip", SkillCategory.MELEE, 50, 4, 5.0));
        register(new GenericSkill("headbutt", "Headbutt", SkillCategory.MELEE, 60, 3, 8.0));
        register(new GenericSkill("shield_bash", "Shield Bash", SkillCategory.MELEE, 80, 3, 7.0));
        register(new GenericSkill("ground_pound", "Ground Pound", SkillCategory.MELEE, 100, 5, 10.0));
        register(new GenericSkill("frenzy", "Frenzy", SkillCategory.MELEE, 120, 3, 4.0));
        register(new GenericSkill("impale", "Impale", SkillCategory.MELEE, 90, 3, 14.0));

        // ============ RANGED (15 skills) ============
        register(new ProjectileSkill("fireball", "Fireball", SkillCategory.RANGED, 60, 20, 8.0));
        register(new ProjectileSkill("arrow_rain", "Arrow Rain", SkillCategory.RANGED, 100, 15, 5.0));
        register(new ProjectileSkill("ice_bolt", "Ice Bolt", SkillCategory.RANGED, 50, 18, 6.0));
        register(new ProjectileSkill("poison_spit", "Poison Spit", SkillCategory.RANGED, 60, 16, 4.0));
        register(new ProjectileSkill("shadow_bolt", "Shadow Bolt", SkillCategory.RANGED, 70, 20, 9.0));
        register(new ProjectileSkill("lightning_bolt", "Lightning Bolt", SkillCategory.RANGED, 80, 22, 12.0));
        register(new ProjectileSkill("web_shot", "Web Shot", SkillCategory.RANGED, 60, 15, 2.0));
        register(new ProjectileSkill("bone_throw", "Bone Throw", SkillCategory.RANGED, 40, 12, 5.0));
        register(new ProjectileSkill("acid_spray", "Acid Spray", SkillCategory.RANGED, 70, 10, 7.0));
        register(new ProjectileSkill("soul_arrow", "Soul Arrow", SkillCategory.RANGED, 90, 25, 10.0));
        register(new ProjectileSkill("fire_breath", "Fire Breath", SkillCategory.RANGED, 100, 8, 6.0));
        register(new ProjectileSkill("mud_ball", "Mud Ball", SkillCategory.RANGED, 45, 14, 3.0));
        register(new ProjectileSkill("spectral_lance", "Spectral Lance", SkillCategory.RANGED, 80, 20, 11.0));
        register(new ProjectileSkill("ender_pearl_throw", "Ender Pearl Throw", SkillCategory.RANGED, 120, 25, 0.0));
        register(new ProjectileSkill("wind_blast", "Wind Blast", SkillCategory.RANGED, 60, 15, 3.0));

        // ============ AOE (15 skills) ============
        register(new AoESkill("earthquake", "Earthquake", 100, 8, 10.0));
        register(new AoESkill("explosion", "Explosion", 120, 6, 15.0));
        register(new AoESkill("thunder_storm", "Thunder Storm", 140, 10, 8.0));
        register(new AoESkill("fire_nova", "Fire Nova", 80, 6, 12.0));
        register(new AoESkill("frost_ring", "Frost Ring", 90, 7, 8.0));
        register(new AoESkill("shockwave", "Shockwave", 70, 5, 6.0));
        register(new AoESkill("toxic_cloud", "Toxic Cloud", 100, 8, 4.0));
        register(new AoESkill("meteor_strike", "Meteor Strike", 200, 10, 20.0));
        register(new AoESkill("void_pulse", "Void Pulse", 100, 6, 9.0));
        register(new AoESkill("chain_lightning", "Chain Lightning", 80, 12, 7.0));
        register(new AoESkill("lava_pool", "Lava Pool", 120, 5, 6.0));
        register(new AoESkill("sand_storm", "Sand Storm", 100, 8, 5.0));
        register(new AoESkill("gravity_well", "Gravity Well", 150, 6, 3.0));
        register(new AoESkill("soul_drain_aoe", "Soul Drain AoE", 100, 7, 8.0));
        register(new AoESkill("blood_rain", "Blood Rain", 140, 10, 6.0));

        // ============ DEBUFF (12 skills) ============
        register(new DebuffSkill("slow", "Slow", 60, 10, 0, 100)); // Slowness
        register(new DebuffSkill("weakness", "Weakness", 80, 10, 18, 100)); // Weakness
        register(new DebuffSkill("blindness", "Blindness", 100, 10, 15, 60));
        register(new DebuffSkill("wither", "Wither", 80, 10, 20, 80));
        register(new DebuffSkill("poison", "Poison", 60, 10, 19, 100));
        register(new DebuffSkill("hunger", "Hunger", 80, 10, 17, 60));
        register(new DebuffSkill("mining_fatigue", "Mining Fatigue", 100, 10, 4, 80));
        register(new DebuffSkill("nausea", "Nausea", 120, 10, 9, 60));
        register(new DebuffSkill("levitation", "Levitation", 100, 10, 25, 40));
        register(new DebuffSkill("darkness", "Darkness", 100, 10, 33, 60));
        register(new DebuffSkill("silence", "Silence", 120, 10, 0, 60)); // custom
        register(new DebuffSkill("curse_of_decay", "Curse of Decay", 140, 12, 20, 120));

        // ============ BUFF (10 skills) ============
        register(new BuffSkill("speed_boost", "Speed Boost", 100, 1, 100));
        register(new BuffSkill("strength_boost", "Strength Boost", 120, 5, 100));
        register(new BuffSkill("resistance_boost", "Resistance Boost", 100, 11, 100));
        register(new BuffSkill("regeneration", "Regeneration", 140, 10, 80));
        register(new BuffSkill("fire_resistance_buff", "Fire Resistance", 120, 12, 100));
        register(new BuffSkill("absorption", "Absorption", 100, 22, 80));
        register(new BuffSkill("invisibility", "Invisibility", 200, 14, 100));
        register(new BuffSkill("jump_boost", "Jump Boost", 80, 8, 60));
        register(new BuffSkill("rage", "Rage", 160, 5, 120)); // extra strength
        register(new BuffSkill("iron_skin", "Iron Skin", 120, 11, 100));

        // ============ SUMMON (8 skills) ============
        register(new SummonSkill("summon_zombie", "Summon Zombie", 200, 12, "ZOMBIE", 3));
        register(new SummonSkill("summon_skeleton", "Summon Skeleton", 200, 12, "SKELETON", 2));
        register(new SummonSkill("summon_spider", "Summon Spider", 180, 12, "SPIDER", 3));
        register(new SummonSkill("summon_blaze", "Summon Blaze", 250, 12, "BLAZE", 2));
        register(new SummonSkill("summon_witch", "Summon Witch", 300, 12, "WITCH", 1));
        register(new SummonSkill("summon_phantom", "Summon Phantom", 240, 12, "PHANTOM", 2));
        register(new SummonSkill("summon_vex", "Summon Vex", 200, 12, "VEX", 3));
        register(new SummonSkill("summon_minion", "Summon Minion", 160, 12, "ZOMBIE", 5));

        // ============ MOVEMENT (10 skills) ============
        register(new GenericSkill("charge", "Charge", SkillCategory.MOVEMENT, 80, 15, 0.0));
        register(new GenericSkill("teleport", "Teleport", SkillCategory.MOVEMENT, 120, 12, 0.0));
        register(new GenericSkill("dash", "Dash", SkillCategory.MOVEMENT, 60, 8, 0.0));
        register(new GenericSkill("blink", "Blink", SkillCategory.MOVEMENT, 100, 10, 0.0));
        register(new GenericSkill("leap", "Leap", SkillCategory.MOVEMENT, 80, 10, 0.0));
        register(new GenericSkill("burrow", "Burrow", SkillCategory.MOVEMENT, 150, 12, 0.0));
        register(new GenericSkill("phase_shift", "Phase Shift", SkillCategory.MOVEMENT, 200, 15, 0.0));
        register(new GenericSkill("grapple", "Grapple", SkillCategory.MOVEMENT, 100, 12, 3.0));
        register(new GenericSkill("shadow_step", "Shadow Step", SkillCategory.MOVEMENT, 120, 10, 0.0));
        register(new GenericSkill("wind_walk", "Wind Walk", SkillCategory.MOVEMENT, 100, 8, 0.0));

        // ============ DEFENSE (10 skills) ============
        register(new GenericSkill("shield_wall", "Shield Wall", SkillCategory.DEFENSE, 120, 10, 0.0));
        register(new GenericSkill("thorns", "Thorns", SkillCategory.DEFENSE, 100, 10, 3.0));
        register(new GenericSkill("heal", "Heal", SkillCategory.DEFENSE, 140, 10, 0.0));
        register(new GenericSkill("damage_reflect", "Damage Reflect", SkillCategory.DEFENSE, 120, 10, 0.0));
        register(new GenericSkill("evasion", "Evasion", SkillCategory.DEFENSE, 100, 8, 0.0));
        register(new GenericSkill("parry", "Parry", SkillCategory.DEFENSE, 60, 5, 0.0));
        register(new GenericSkill("barrier", "Barrier", SkillCategory.DEFENSE, 150, 10, 0.0));
        register(new GenericSkill("cocoon", "Cocoon", SkillCategory.DEFENSE, 200, 10, 0.0));
        register(new GenericSkill("life_steal", "Life Steal", SkillCategory.DEFENSE, 80, 5, 5.0));
        register(new GenericSkill("undying", "Undying", SkillCategory.DEFENSE, 300, 15, 0.0));

        // ============ SPECIAL (10 skills) ============
        register(new GenericSkill("gravity_flip", "Gravity Flip", SkillCategory.SPECIAL, 120, 10, 0.0));
        register(new GenericSkill("time_freeze", "Time Freeze", SkillCategory.SPECIAL, 200, 12, 0.0));
        register(new GenericSkill("soul_swap", "Soul Swap", SkillCategory.SPECIAL, 150, 15, 0.0));
        register(new GenericSkill("clone", "Clone", SkillCategory.SPECIAL, 180, 12, 0.0));
        register(new GenericSkill("mind_control", "Mind Control", SkillCategory.SPECIAL, 250, 15, 0.0));
        register(new GenericSkill("curse_mark", "Curse Mark", SkillCategory.SPECIAL, 120, 10, 5.0));
        register(new GenericSkill("death_mark", "Death Mark", SkillCategory.SPECIAL, 200, 15, 10.0));
        register(new GenericSkill("enrage", "Enrage", SkillCategory.SPECIAL, 160, 10, 0.0));
        register(new GenericSkill("blood_sacrifice", "Blood Sacrifice", SkillCategory.SPECIAL, 180, 12, 0.0));
        register(new GenericSkill("soul_harvest", "Soul Harvest", SkillCategory.SPECIAL, 200, 15, 8.0));

        // ============ ELEMENTAL MASTERIES (4 skills) ============
        register(new ElementalMasterySkill("meteor", "Meteor", 300, 25, 25.0));
        register(new ElementalMasterySkill("tsunami", "Tsunami", 200, 15, 12.0));
        register(new ElementalMasterySkill("earth_spikes", "Earth Spikes", 150, 12, 10.0));
        register(new ElementalMasterySkill("thunder_strike", "Thunder Strike", 250, 20, 18.0));

        // ============ CROWD CONTROL (4 skills) ============
        register(new CrowdControlSkill("gravity_vortex", "Gravity Vortex", 400, 15, 0.0));
        register(new CrowdControlSkill("time_dilation", "Time Dilation", 500, 10, 0.0));
        register(new CrowdControlSkill("blind_fog", "Blind Fog", 300, 20, 0.0));
        register(new CrowdControlSkill("phantom_possession", "Phantom Possession", 600, 8, 4.0));

        // ============ BOSS (5 skills) ============
        register(new GenericSkill("arena_lockdown", "Arena Lockdown", SkillCategory.BOSS, 400, 20, 0.0));
        register(new GenericSkill("death_ray", "Death Ray", SkillCategory.BOSS, 300, 25, 25.0));
        register(new GenericSkill("world_ender", "World Ender", SkillCategory.BOSS, 500, 30, 20.0));
        register(new GenericSkill("soul_cage", "Soul Cage", SkillCategory.BOSS, 350, 20, 0.0));
        register(new GenericSkill("apocalypse", "Apocalypse", SkillCategory.BOSS, 600, 30, 30.0));
    }

    public void register(MobSkill skill) {
        skills.put(skill.getId(), skill);
    }

    public MobSkill getSkill(String id) {
        return skills.get(id);
    }

    public Collection<MobSkill> getAllSkills() {
        return skills.values();
    }

    public List<MobSkill> getSkillsByCategory(SkillCategory category) {
        List<MobSkill> result = new ArrayList<>();
        for (MobSkill skill : skills.values()) {
            if (skill.getCategory() == category)
                result.add(skill);
        }
        return result;
    }

    public int getSkillCount() {
        return skills.size();
    }

    /**
     * Apply skills to a living entity by setting metadata tags.
     * The SkillExecutor tick loop reads these metadata to execute skills at
     * runtime.
     */
    public void applySkills(org.bukkit.entity.LivingEntity entity, java.util.List<String> skillIds) {
        if (skillIds == null || skillIds.isEmpty())
            return;

        // Mark as dungeon mob
        entity.setMetadata("dungeon_mob",
                new org.bukkit.metadata.FixedMetadataValue(
                        org.bukkit.Bukkit.getPluginManager().getPlugin("NaturalDungeon"), true));

        // Set skill IDs as comma-separated metadata
        String joined = String.join(",", skillIds);
        entity.setMetadata("mob_skills",
                new org.bukkit.metadata.FixedMetadataValue(
                        org.bukkit.Bukkit.getPluginManager().getPlugin("NaturalDungeon"), joined));
    }
}
