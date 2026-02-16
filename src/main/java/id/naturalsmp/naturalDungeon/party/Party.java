package id.naturalsmp.naturaldungeon.party;

import id.naturalsmp.naturaldungeon.utils.ConfigUtils;

import java.util.*;

public class Party {

    private UUID leader;
    private final List<UUID> members = new ArrayList<>();
    private final Set<UUID> pendingInvites = new HashSet<>();
    private int tier = 1;

    public Party(UUID leader) {
        this.leader = leader;
        this.members.add(leader);
    }

    public UUID getLeader() {
        return leader;
    }

    /**
     * Transfer leadership to another member.
     * 
     * @return true if transfer was successful
     */
    public boolean transferLeader(UUID newLeader) {
        if (!members.contains(newLeader) || newLeader.equals(leader))
            return false;
        this.leader = newLeader;
        return true;
    }

    public List<UUID> getMembers() {
        return new ArrayList<>(members);
    }

    public int getTier() {
        return tier;
    }

    public int getMaxPlayers() {
        // Read from config if available, fallback to hardcoded
        int configMax = ConfigUtils.getInt("party.upgrades.tier" + tier + ".max-size", -1);
        if (configMax > 0)
            return configMax;
        return switch (tier) {
            case 2 -> 4;
            case 3 -> 8;
            case 4 -> 16;
            case 5 -> 24;
            default -> 2;
        };
    }

    public boolean isFull() {
        return members.size() >= getMaxPlayers();
    }

    public boolean isLeader(UUID uuid) {
        return leader.equals(uuid);
    }

    public boolean isMember(UUID uuid) {
        return members.contains(uuid);
    }

    public boolean addMember(UUID uuid) {
        if (isFull() || members.contains(uuid))
            return false;
        members.add(uuid);
        pendingInvites.remove(uuid);
        return true;
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }

    public void addInvite(UUID uuid) {
        pendingInvites.add(uuid);
    }

    public void removeInvite(UUID uuid) {
        pendingInvites.remove(uuid);
    }

    public boolean hasInvite(UUID uuid) {
        return pendingInvites.contains(uuid);
    }

    public Set<UUID> getPendingInvites() {
        return new HashSet<>(pendingInvites);
    }

    public boolean upgradeTier() {
        if (tier >= 5)
            return false;
        tier++;
        return true;
    }

    public int getUpgradeCost() {
        // Read from config if available, fallback to hardcoded
        int configCost = ConfigUtils.getInt("party.upgrades.tier" + (tier + 1) + ".cost", -1);
        if (configCost >= 0)
            return configCost;
        return switch (tier) {
            case 1 -> 15000;
            case 2 -> 50000;
            case 3 -> 150000;
            case 4 -> 500000;
            default -> 0;
        };
    }
}
