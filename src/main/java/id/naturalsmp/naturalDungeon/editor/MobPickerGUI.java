package id.naturalsmp.naturaldungeon.editor;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.mob.CustomMob;
import id.naturalsmp.naturaldungeon.utils.GUIUtils;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.*;
import java.util.function.Consumer;

/**
 * Mob Picker sub-GUI — visual mob selection from Vanilla, MythicMobs, or Custom
 * mobs.
 * Per vision_admin_experience.md §3: Mob Picker Sub-GUI
 */
public class MobPickerGUI implements Listener {

    private final NaturalDungeon plugin;
    private final Map<UUID, PickerSession> sessions = new HashMap<>();

    // Commonly used hostile mobs for dungeon scenarios
    private static final EntityType[] VANILLA_MOBS = {
            EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER,
            EntityType.CREEPER, EntityType.ENDERMAN, EntityType.WITCH,
            EntityType.BLAZE, EntityType.WITHER_SKELETON, EntityType.PILLAGER,
            EntityType.VINDICATOR, EntityType.EVOKER, EntityType.RAVAGER,
            EntityType.HOGLIN, EntityType.PIGLIN_BRUTE, EntityType.DROWNED,
            EntityType.HUSK, EntityType.STRAY, EntityType.PHANTOM,
            EntityType.GUARDIAN, EntityType.ELDER_GUARDIAN, EntityType.SHULKER,
            EntityType.VEX, EntityType.CAVE_SPIDER, EntityType.SILVERFISH,
            EntityType.SLIME, EntityType.MAGMA_CUBE, EntityType.GHAST, EntityType.WARDEN
    };

    public MobPickerGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    /**
     * Opens the mob picker. When a mob is selected, the callback is invoked with
     * the mob ID string.
     */
    public void open(Player player, Consumer<String> onSelect) {
        sessions.put(player.getUniqueId(), new PickerSession(onSelect, "ALL"));
        openPage(player, "ALL");
    }

    private void openPage(Player player, String filter) {
        PickerSession session = sessions.get(player.getUniqueId());
        if (session == null)
            return;
        session.filter = filter;

        Inventory inv = GUIUtils.createGUI(new PickerHolder(), 54,
                "&#AA44FF🧟 ᴍᴏʙ ᴘɪᴄᴋᴇʀ");

        GUIUtils.fillBorder(inv, Material.BLACK_STAINED_GLASS_PANE);

        // ─── Filter Tabs (top row) ───
        inv.setItem(2, createTab("ALL", "&#FFD700&l📋 ᴀʟʟ", filter.equals("ALL")));
        inv.setItem(3, createTab("VANILLA", "&#55FF55&l🧟 ᴠᴀɴɪʟʟᴀ", filter.equals("VANILLA")));
        inv.setItem(4, createTab("MYTHIC", "&#AA44FF&l📦 ᴍʏᴛʜɪᴄᴍᴏʙꜱ", filter.equals("MYTHIC")));
        inv.setItem(5, createTab("CUSTOM", "&#FFAA00&l⭐ ᴄᴜꜱᴛᴏᴍ", filter.equals("CUSTOM")));

        // ─── Mob Grid (slots 19-43) ───
        List<MobEntry> mobs = getMobs(filter);
        int[] slots = { 19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43 };

        for (int i = 0; i < Math.min(mobs.size(), slots.length); i++) {
            MobEntry mob = mobs.get(i);
            inv.setItem(slots[i], GUIUtils.createItem(mob.icon,
                    mob.color + "&l" + mob.displayName,
                    GUIUtils.separator(),
                    "&7ID: &f" + mob.id,
                    "&7Type: &f" + mob.source,
                    "",
                    "&#FFAA00&l➥ KLIK untuk pilih"));
        }

        // Close
        inv.setItem(49, GUIUtils.createItem(Material.BARRIER, "&#FF5555&l✕ ʙᴀᴛᴀʟ"));

        player.openInventory(inv);
        GUIUtils.playOpenSound(player);
    }

    private org.bukkit.inventory.ItemStack createTab(String id, String name, boolean active) {
        return GUIUtils.createItem(
                active ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE,
                name, active ? "&7▼ Sedang dipilih" : "&7Klik untuk filter");
    }

    private List<MobEntry> getMobs(String filter) {
        List<MobEntry> list = new ArrayList<>();

        // Vanilla mobs
        if (filter.equals("ALL") || filter.equals("VANILLA")) {
            for (EntityType type : VANILLA_MOBS) {
                String name = type.name().replace("_", " ");
                name = name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
                list.add(new MobEntry(type.name(), name, "Vanilla", "&#55FF55",
                        getMobIcon(type)));
            }
        }

        // MythicMobs
        if (filter.equals("ALL") || filter.equals("MYTHIC")) {
            if (plugin.getServer().getPluginManager().getPlugin("MythicMobs") != null) {
                try {
                    for (String mm : io.lumine.mythic.bukkit.MythicBukkit.inst().getMobManager().getMobNames()) {
                        list.add(new MobEntry("MM:" + mm, mm, "MythicMobs", "&#AA44FF",
                                Material.SPAWNER));
                    }
                } catch (Exception ignored) {
                }
            }
        }

        // Custom mobs
        if (filter.equals("ALL") || filter.equals("CUSTOM")) {
            for (CustomMob mob : plugin.getCustomMobManager().getAllMobs()) {
                list.add(new MobEntry("CUSTOM:" + mob.getId(), mob.getId(), "Custom", "&#FFAA00",
                        Material.PLAYER_HEAD));
            }
        }

        return list;
    }

    private Material getMobIcon(EntityType type) {
        return switch (type) {
            case ZOMBIE, HUSK, DROWNED -> Material.ZOMBIE_HEAD;
            case SKELETON, STRAY -> Material.SKELETON_SKULL;
            case CREEPER -> Material.CREEPER_HEAD;
            case SPIDER, CAVE_SPIDER -> Material.FERMENTED_SPIDER_EYE;
            case ENDERMAN -> Material.ENDER_PEARL;
            case WITCH -> Material.SPLASH_POTION;
            case BLAZE -> Material.BLAZE_ROD;
            case WITHER_SKELETON -> Material.WITHER_SKELETON_SKULL;
            case PILLAGER -> Material.CROSSBOW;
            case VINDICATOR -> Material.IRON_AXE;
            case EVOKER -> Material.TOTEM_OF_UNDYING;
            case RAVAGER -> Material.LEATHER_HORSE_ARMOR;
            case SLIME, MAGMA_CUBE -> Material.SLIME_BALL;
            case GHAST -> Material.GHAST_TEAR;
            case PHANTOM -> Material.PHANTOM_MEMBRANE;
            case GUARDIAN, ELDER_GUARDIAN -> Material.PRISMARINE_SHARD;
            case SHULKER -> Material.SHULKER_SHELL;
            case VEX -> Material.IRON_SWORD;
            case SILVERFISH -> Material.STONE;
            case PIGLIN_BRUTE -> Material.GOLDEN_AXE;
            case HOGLIN -> Material.LEATHER;
            case WARDEN -> Material.SCULK_SHRIEKER;
            default -> Material.SPAWNER;
        };
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof PickerHolder))
            return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getView().getTopInventory())
            return;
        if (e.getCurrentItem() == null)
            return;

        Player player = (Player) e.getWhoClicked();
        PickerSession session = sessions.get(player.getUniqueId());
        if (session == null)
            return;

        GUIUtils.playClickSound(player);

        // Filter tabs
        switch (e.getSlot()) {
            case 2 -> {
                openPage(player, "ALL");
                return;
            }
            case 3 -> {
                openPage(player, "VANILLA");
                return;
            }
            case 4 -> {
                openPage(player, "MYTHIC");
                return;
            }
            case 5 -> {
                openPage(player, "CUSTOM");
                return;
            }
            case 49 -> {
                sessions.remove(player.getUniqueId());
                player.closeInventory();
                return;
            }
        }

        // Mob selection
        int[] slots = { 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43 };
        int clickedIdx = -1;
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == e.getSlot()) {
                clickedIdx = i;
                break;
            }
        }

        if (clickedIdx >= 0) {
            List<MobEntry> mobs = getMobs(session.filter);
            if (clickedIdx < mobs.size()) {
                MobEntry mob = mobs.get(clickedIdx);
                sessions.remove(player.getUniqueId());
                player.closeInventory();
                GUIUtils.playSuccessSound(player);
                session.onSelect.accept(mob.id);
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof PickerHolder)
            e.setCancelled(true);
    }

    // ─── Data Classes ───────────────────────────────────────────────

    public static class PickerHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private static class PickerSession {
        final Consumer<String> onSelect;
        String filter;

        PickerSession(Consumer<String> onSelect, String filter) {
            this.onSelect = onSelect;
            this.filter = filter;
        }
    }

    private static class MobEntry {
        final String id;
        final String displayName;
        final String source;
        final String color;
        final Material icon;

        MobEntry(String id, String displayName, String source, String color, Material icon) {
            this.id = id;
            this.displayName = displayName;
            this.source = source;
            this.color = color;
            this.icon = icon;
        }
    }
}
