package id.naturalsmp.naturaldungeon.dungeon;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import id.naturalsmp.naturaldungeon.utils.GUIUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.*;

/**
 * Roguelike Branching Paths — after completing a stage, players vote on
 * which path to take next. Each path has different modifiers.
 *
 * Path Types:
 * - COMBAT: More mobs, more loot
 * - TREASURE: Less mobs, rare loot chest
 * - CHALLENGE: Special modifier (e.g., darkness, no regen), bonus XP
 * - SHORTCUT: Skip next stage but no loot from it
 * - GAUNTLET: Double mobs, double rewards
 */
public class BranchPathGUI implements Listener {

    private final NaturalDungeon plugin;
    private static final Random RNG = new Random();

    public enum PathType {
        COMBAT(Material.IRON_SWORD, "&#FF5555🗡 Jalur Pertempuran",
                "Lebih banyak mob, lebih banyak loot",
                "&cMob Count: &f+50%", "&eLoot: &f+25%"),
        TREASURE(Material.CHEST, "&#FFD700💎 Jalur Harta",
                "Mob sedikit, ada rare chest di akhir",
                "&aMob Count: &f-30%", "&6Rare Chest: &f✔"),
        CHALLENGE(Material.WITHER_SKELETON_SKULL, "&#AA44FF⚡ Jalur Tantangan",
                "Modifier spesial, bonus XP besar",
                "&5Modifier: &fRandom", "&bBonus XP: &f+100%"),
        SHORTCUT(Material.ENDER_PEARL, "&#55CCFF🌀 Jalan Pintas",
                "Skip stage berikutnya, tapi tanpa loot",
                "&aSkip: &f1 stage", "&cLoot: &fTidak ada"),
        GAUNTLET(Material.NETHERITE_SWORD, "&#FF0000💀 Penyiksaan",
                "Mob x2, reward x2, sangat sulit",
                "&cMob Count: &fx2", "&6Reward: &fx2");

        public final Material icon;
        public final String display;
        public final String description;
        public final String stat1;
        public final String stat2;

        PathType(Material icon, String display, String description, String stat1, String stat2) {
            this.icon = icon;
            this.display = display;
            this.description = description;
            this.stat1 = stat1;
            this.stat2 = stat2;
        }
    }

    // Track active votes per instance
    private final Map<Integer, PathVote> activeVotes = new HashMap<>();

    public BranchPathGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    /**
     * Open branch path selection for all players in a dungeon instance.
     * Randomly selects 3 path options.
     */
    public void openVote(DungeonInstance instance) {
        // Pick 3 random paths
        List<PathType> available = new ArrayList<>(Arrays.asList(PathType.values()));
        Collections.shuffle(available);
        List<PathType> options = available.subList(0, Math.min(3, available.size()));

        PathVote vote = new PathVote(options, instance);
        activeVotes.put(instance.getInstanceId(), vote);

        for (UUID pid : instance.getParticipants()) {
            Player p = org.bukkit.Bukkit.getPlayer(pid);
            if (p != null)
                openGUI(p, vote);
        }
    }

    private void openGUI(Player player, PathVote vote) {
        Inventory inv = GUIUtils.createGUI(
                new BranchHolder(vote.instance.getInstanceId()), 27,
                "&#AA44FF🔀 ᴘɪʟɪʜ ᴊᴀʟᴜʀ");

        GUIUtils.fillBorder(inv, Material.BLACK_STAINED_GLASS_PANE);
        GUIUtils.fillEmpty(inv, Material.PURPLE_STAINED_GLASS_PANE);

        // 3 path options in slots 11, 13, 15
        int[] slots = { 11, 13, 15 };
        for (int i = 0; i < vote.options.size(); i++) {
            PathType path = vote.options.get(i);
            int votes = vote.getVotes(path);
            int total = vote.totalVotes();

            String voteBar = total > 0 ? " &8(" + votes + "/" + total + ")" : "";

            inv.setItem(slots[i], GUIUtils.createItem(path.icon,
                    path.display + voteBar,
                    GUIUtils.separator(),
                    "&7" + path.description,
                    "",
                    path.stat1,
                    path.stat2,
                    "",
                    "&#FFAA00&l➥ KLIK untuk vote"));
        }

        player.openInventory(inv);
        GUIUtils.playOpenSound(player);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof BranchHolder holder))
            return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getView().getTopInventory())
            return;
        if (e.getCurrentItem() == null)
            return;

        Player player = (Player) e.getWhoClicked();
        GUIUtils.playClickSound(player);

        PathVote vote = activeVotes.get(holder.instanceId);
        if (vote == null)
            return;

        // Check which slot was clicked
        int[] slots = { 11, 13, 15 };
        int pathIdx = -1;
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == e.getSlot()) {
                pathIdx = i;
                break;
            }
        }

        if (pathIdx < 0 || pathIdx >= vote.options.size())
            return;

        PathType chosen = vote.options.get(pathIdx);

        // Record vote
        if (vote.hasVoted(player.getUniqueId())) {
            player.sendMessage(ChatUtils.colorize("&#FFAA00⚠ &7Kamu sudah vote! Vote di-update."));
        }
        vote.castVote(player.getUniqueId(), chosen);
        player.sendMessage(ChatUtils.colorize("&#55FF55✔ &7Vote untuk " +
                ChatUtils.colorize(chosen.display) + "!"));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);

        // Update GUI for all voters
        for (UUID pid : vote.instance.getParticipants()) {
            Player p = org.bukkit.Bukkit.getPlayer(pid);
            if (p != null && p.getOpenInventory().getTopInventory().getHolder() instanceof BranchHolder) {
                openGUI(p, vote);
            }
        }

        // Check if all players voted
        if (vote.allVoted()) {
            resolveVote(vote);
        }
    }

    private void resolveVote(PathVote vote) {
        PathType winner = vote.getWinner();
        activeVotes.remove(vote.instance.getInstanceId());

        // Close all GUIs
        for (UUID pid : vote.instance.getParticipants()) {
            Player p = org.bukkit.Bukkit.getPlayer(pid);
            if (p != null) {
                p.closeInventory();
                p.sendMessage(ChatUtils.colorize(
                        "&#AA44FF&l🔀 &7Jalur terpilih: " + ChatUtils.colorize(winner.display)));
            }
        }

        // Apply path modifiers to the instance
        applyPathModifier(vote.instance, winner);
    }

    private void applyPathModifier(DungeonInstance instance, PathType path) {
        switch (path) {
            case COMBAT -> {
                // +50% mob count, +25% loot — stored as metadata
                instance.setPathModifier("mob_multiplier", 1.5);
                instance.setPathModifier("loot_multiplier", 1.25);
            }
            case TREASURE -> {
                instance.setPathModifier("mob_multiplier", 0.7);
                instance.setPathModifier("treasure_chest", 1.0);
            }
            case CHALLENGE -> {
                instance.setPathModifier("mob_multiplier", 1.0);
                instance.setPathModifier("xp_multiplier", 2.0);
                // Apply random modifier
                String[] modifiers = { "DARKNESS", "NO_REGEN", "SPEED_MOBS", "FIRE_FLOOR" };
                String mod = modifiers[RNG.nextInt(modifiers.length)];
                instance.setPathModifier("special_modifier", mod);
                instance.broadcastTitle("&#AA44FF&l⚡ " + mod,
                        "&7Tantangan spesial aktif!", 10, 40, 10);
            }
            case SHORTCUT -> {
                instance.setPathModifier("skip_stage", 1.0);
                instance.setPathModifier("loot_multiplier", 0.0);
            }
            case GAUNTLET -> {
                instance.setPathModifier("mob_multiplier", 2.0);
                instance.setPathModifier("loot_multiplier", 2.0);
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof BranchHolder)
            e.setCancelled(true);
    }

    // ─── Inner Classes ──────────────────────────────────────────────

    static class PathVote {
        final List<PathType> options;
        final DungeonInstance instance;
        final Map<UUID, PathType> votes = new HashMap<>();

        PathVote(List<PathType> options, DungeonInstance instance) {
            this.options = options;
            this.instance = instance;
        }

        void castVote(UUID player, PathType path) {
            votes.put(player, path);
        }

        boolean hasVoted(UUID player) {
            return votes.containsKey(player);
        }

        int getVotes(PathType path) {
            return (int) votes.values().stream().filter(p -> p == path).count();
        }

        int totalVotes() {
            return votes.size();
        }

        boolean allVoted() {
            for (UUID pid : instance.getParticipants()) {
                if (!votes.containsKey(pid))
                    return false;
            }
            return true;
        }

        PathType getWinner() {
            Map<PathType, Integer> counts = new EnumMap<>(PathType.class);
            for (PathType p : votes.values())
                counts.merge(p, 1, Integer::sum);
            return counts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(options.get(0));
        }
    }

    public static class BranchHolder implements InventoryHolder {
        public final int instanceId;

        public BranchHolder(int id) {
            this.instanceId = id;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
