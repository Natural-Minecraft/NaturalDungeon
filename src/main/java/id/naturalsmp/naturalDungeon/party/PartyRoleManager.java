package id.naturalsmp.naturaldungeon.party;

import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * Party Role System — Tank / DPS / Healer.
 *
 * Each role provides passive buffs during dungeon runs:
 * - TANK: +4 HP, Resistance I, aggro boost (glowing)
 * - DPS: Strength I, Speed I, 10% bonus damage
 * - HEALER: Regen I, nearby allies get Regen particles every 5s
 * - NONE: No role selected (default)
 */
public class PartyRoleManager {

    public enum Role {
        NONE("&7Tidak Ada", "Belum pilih role"),
        TANK("&b🛡 Tank", "Pertahanan + HP tinggi, menarik agresi mob"),
        DPS("&c🗡 DPS", "Serangan kuat + kecepatan tinggi"),
        HEALER("&a💚 Healer", "Regenerasi sendiri + membantu penyembuhan tim");

        public final String display;
        public final String description;

        Role(String display, String description) {
            this.display = display;
            this.description = description;
        }
    }

    private final Map<UUID, Role> roles = new HashMap<>();

    public Role getRole(UUID playerId) {
        return roles.getOrDefault(playerId, Role.NONE);
    }

    public void setRole(UUID playerId, Role role) {
        roles.put(playerId, role);
    }

    public void clearRole(UUID playerId) {
        roles.remove(playerId);
    }

    public void clearAll(Collection<UUID> participants) {
        for (UUID uuid : participants)
            roles.remove(uuid);
    }

    /**
     * Apply role-specific buffs to a player. Called when dungeon starts.
     */
    public void applyRoleBuffs(Player player) {
        Role role = getRole(player.getUniqueId());
        if (role == Role.NONE)
            return;

        switch (role) {
            case TANK -> {
                // +4 HP (2 hearts)
                org.bukkit.attribute.AttributeInstance maxHp = player.getAttribute(
                        org.bukkit.attribute.Attribute.MAX_HEALTH);
                if (maxHp != null) {
                    maxHp.setBaseValue(maxHp.getBaseValue() + 4);
                    player.setHealth(Math.min(player.getHealth() + 4, maxHp.getValue()));
                }
                player.addPotionEffect(
                        new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 0, false, false));
                player.sendMessage(ChatUtils.colorize("&b🛡 &7Role &bTank &7aktif! &8(+HP, Resistance I)"));
            }
            case DPS -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 0, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, false, false));
                player.sendMessage(ChatUtils.colorize("&c🗡 &7Role &cDPS &7aktif! &8(Strength I, Speed I)"));
            }
            case HEALER -> {
                player.addPotionEffect(
                        new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 0, false, false));
                player.sendMessage(ChatUtils.colorize("&a💚 &7Role &aHealer &7aktif! &8(Regen I + Aura)"));
            }
        }
    }

    /**
     * Remove role buffs from a player. Called when dungeon ends.
     */
    public void removeRoleBuffs(Player player) {
        Role role = getRole(player.getUniqueId());
        if (role == Role.NONE)
            return;

        switch (role) {
            case TANK -> {
                org.bukkit.attribute.AttributeInstance maxHp = player.getAttribute(
                        org.bukkit.attribute.Attribute.MAX_HEALTH);
                if (maxHp != null) {
                    maxHp.setBaseValue(20.0); // Reset to default
                }
                player.removePotionEffect(PotionEffectType.RESISTANCE);
            }
            case DPS -> {
                player.removePotionEffect(PotionEffectType.STRENGTH);
                player.removePotionEffect(PotionEffectType.SPEED);
            }
            case HEALER -> {
                player.removePotionEffect(PotionEffectType.REGENERATION);
            }
        }
    }

    /**
     * Healer aura tick — heals nearby allies.
     * Call this every ~5 seconds during dungeon.
     */
    public void tickHealerAura(Collection<UUID> participants) {
        for (UUID uuid : participants) {
            if (getRole(uuid) != Role.HEALER)
                continue;
            Player healer = org.bukkit.Bukkit.getPlayer(uuid);
            if (healer == null || healer.isDead())
                continue;

            // Heal nearby allies within 8 blocks
            for (UUID allyId : participants) {
                if (allyId.equals(uuid))
                    continue;
                Player ally = org.bukkit.Bukkit.getPlayer(allyId);
                if (ally == null || ally.isDead())
                    continue;
                if (ally.getLocation().distanceSquared(healer.getLocation()) <= 64) { // 8 blocks
                    double maxHp = ally.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
                    if (ally.getHealth() < maxHp) {
                        ally.setHealth(Math.min(ally.getHealth() + 2, maxHp)); // +1 heart
                        ally.getWorld().spawnParticle(org.bukkit.Particle.HEART,
                                ally.getLocation().add(0, 2, 0), 3, 0.3, 0.3, 0.3, 0);
                    }
                }
            }
        }
    }

    /**
     * Get a summary of party role distribution.
     */
    public Map<Role, Integer> getRoleDistribution(Collection<UUID> participants) {
        Map<Role, Integer> dist = new EnumMap<>(Role.class);
        for (UUID uuid : participants) {
            Role r = getRole(uuid);
            dist.merge(r, 1, Integer::sum);
        }
        return dist;
    }
}
