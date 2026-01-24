package id.naturalsmp.naturaldungeon.integration;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.registry.NamespacedId;
import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.api.user.SkillsUser;
import org.bukkit.entity.Player;

public class AuraSkillsHook {

    private final NaturalDungeon plugin;
    private AuraSkillsApi api;

    public AuraSkillsHook(NaturalDungeon plugin) {
        this.plugin = plugin;
        try {
            this.api = AuraSkillsApi.get();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to hook AuraSkills API: " + e.getMessage());
        }
    }

    public void addXp(Player player, String skillName, double amount) {
        if (api == null)
            return;
        try {
            SkillsUser user = api.getUser(player.getUniqueId());
            NamespacedId nsId = NamespacedId.fromDefault(skillName.toLowerCase());
            Skill skill = api.getGlobalRegistry().getSkill(nsId);
            if (skill != null && user != null) {
                user.addSkillXp(skill, amount);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to add XP: " + e.getMessage());
        }
    }

    public int getLevel(Player player, String skillName) {
        if (api == null)
            return 0;
        try {
            SkillsUser user = api.getUser(player.getUniqueId());
            NamespacedId nsId = NamespacedId.fromDefault(skillName.toLowerCase());
            Skill skill = api.getGlobalRegistry().getSkill(nsId);
            if (skill != null && user != null) {
                return user.getSkillLevel(skill);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get level: " + e.getMessage());
        }
        return 0;
    }
}
