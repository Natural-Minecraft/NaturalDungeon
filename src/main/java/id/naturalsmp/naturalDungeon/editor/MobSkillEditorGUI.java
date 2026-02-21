package id.naturalsmp.naturaldungeon.editor;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.mob.CustomMob;
import id.naturalsmp.naturaldungeon.skill.MobSkill;
import id.naturalsmp.naturaldungeon.skill.SkillCategory;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import id.naturalsmp.naturaldungeon.utils.GUIUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class MobSkillEditorGUI implements Listener {

    private final NaturalDungeon plugin;

    public MobSkillEditorGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String dungeonId, String mobId, boolean isBossEditor) {
        Inventory inv = GUIUtils.createGUI(new SkillCategoryHolder(dungeonId, mobId, isBossEditor), 27,
                "&#AA44FF🔮 ꜱᴋɪʟʟ ᴄᴀᴛᴇɢᴏʀɪᴇꜱ");

        GUIUtils.fillAll(inv, Material.BLACK_STAINED_GLASS_PANE);

        int[] slots = { 10, 11, 12, 13, 14, 15, 16, 19, 20, 21 };
        SkillCategory[] cats = SkillCategory.values();

        for (int i = 0; i < cats.length; i++) {
            if (i >= slots.length)
                break;
            SkillCategory cat = cats[i];
            inv.setItem(slots[i], GUIUtils.createItem(Material.BOOK,
                    "&#FFAA00&l" + cat.getDisplayName(),
                    GUIUtils.separator(),
                    "&7" + cat.getDescription(),
                    "",
                    "&#FFAA00&l➥ KLIK"));
        }

        // ─── Skill Presets ───
        inv.setItem(0, GUIUtils.createItem(Material.DIAMOND_CHESTPLATE,
                "&#FF6666&lᴛᴀɴᴋʏ ʙᴏꜱꜱ",
                GUIUtils.separator(),
                "&7Shield Wall, Life Drain,",
                "&7Ground Slam, Gravity Pull",
                "",
                "&#55FF55&l➥ APPLY PRESET"));

        inv.setItem(1, GUIUtils.createItem(Material.BLAZE_POWDER,
                "&#FF8800&lᴀᴏᴇ ᴄᴀꜱᴛᴇʀ",
                GUIUtils.separator(),
                "&7Meteor Shower, Thunderstorm,",
                "&7Earth Spikes, Tsunami Wave",
                "",
                "&#55FF55&l➥ APPLY PRESET"));

        inv.setItem(2, GUIUtils.createItem(Material.GOLDEN_APPLE,
                "&#55FF55&lꜱᴜᴘᴘᴏʀᴛ ᴍᴏʙ",
                GUIUtils.separator(),
                "&7Heal Aura, Shield Allies,",
                "&7Summon Minions, Phantom Phase",
                "",
                "&#55FF55&l➥ APPLY PRESET"));

        inv.setItem(3, GUIUtils.createItem(Material.IRON_SWORD,
                "&#AA55FF&lᴀꜱꜱᴀꜱꜱɪɴ",
                GUIUtils.separator(),
                "&7Shadow Step, Soul Tether,",
                "&7Blindness Fog, Time Dilation",
                "",
                "&#55FF55&l➥ APPLY PRESET"));

        inv.setItem(22, GUIUtils.createItem(Material.ARROW, "&#FF5555&l← ᴋᴇᴍʙᴀʟɪ"));

        player.openInventory(inv);
        GUIUtils.playOpenSound(player);
    }

    public void openCategory(Player player, String dungeonId, String mobId, SkillCategory category,
            boolean isBossEditor) {
        Inventory inv = GUIUtils.createGUI(new SkillListHolder(dungeonId, mobId, category, isBossEditor), 54,
                "&#AA44FF🔮 " + category.getDisplayName() + " ꜱᴋɪʟʟꜱ");

        CustomMob mob = plugin.getCustomMobManager().getMob(mobId);
        List<MobSkill> skills = plugin.getSkillRegistry().getSkillsByCategory(category);

        int slot = 0;
        for (MobSkill skill : skills) {
            boolean hasSkill = mob.getSkillIds().contains(skill.getId());
            inv.setItem(slot, GUIUtils.createItem(
                    hasSkill ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE,
                    (hasSkill ? "&#55FF55" : "&#FF5555") + "&l" + skill.getDisplayName(),
                    GUIUtils.separator(),
                    "&7ID: &f" + skill.getId(),
                    "&7Cooldown: &f" + skill.getCooldownTicks() + " ticks",
                    "",
                    hasSkill ? "&#55FF55✔ TERPASANG &7— Klik hapus" : "&#FF5555✗ TIDAK &7— Klik pasang"));
            slot++;
        }

        inv.setItem(49, GUIUtils.createItem(Material.ARROW, "&#FF5555&l← ᴋᴇᴍʙᴀʟɪ"));

        player.openInventory(inv);
        GUIUtils.playOpenSound(player);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        InventoryHolder h = e.getInventory().getHolder();
        if (e.getCurrentItem() == null)
            return;

        if (h instanceof SkillCategoryHolder holder) {
            e.setCancelled(true);
            if (e.getClickedInventory() != e.getView().getTopInventory())
                return;
            GUIUtils.playClickSound((Player) e.getWhoClicked());
            handleCategoryClick(e, holder);
        } else if (h instanceof SkillListHolder holder) {
            e.setCancelled(true);
            if (e.getClickedInventory() != e.getView().getTopInventory())
                return;
            GUIUtils.playClickSound((Player) e.getWhoClicked());
            handleListClick(e, holder);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        InventoryHolder h = e.getInventory().getHolder();
        if (h instanceof SkillCategoryHolder || h instanceof SkillListHolder)
            e.setCancelled(true);
    }

    private void handleCategoryClick(InventoryClickEvent e, SkillCategoryHolder holder) {
        Player player = (Player) e.getWhoClicked();
        if (e.getSlot() == 22) {
            if (holder.isBossEditor) {
                new BossEditorGUI(plugin).openBossConfig(player, holder.dungeonId, holder.mobId);
            } else {
                new MobEditorGUI(plugin).open(player, holder.dungeonId);
            }
            return;
        }

        // Handle Preset Clicks (slots 0-3)
        if (e.getSlot() >= 0 && e.getSlot() <= 3) {
            CustomMob mob = plugin.getCustomMobManager().getMob(holder.mobId);
            if (mob == null)
                return;

            List<String> preset = switch (e.getSlot()) {
                case 0 -> List.of("shield_wall", "life_drain", "ground_slam", "gravity_pull");
                case 1 -> List.of("meteor_shower", "thunderstorm", "earth_spikes", "tsunami_wave");
                case 2 -> List.of("heal_aura", "shield_allies", "summon_minions", "phantom_phase");
                case 3 -> List.of("shadow_step", "soul_tether", "blindness_fog", "time_dilation");
                default -> List.of();
            };

            mob.getSkillIds().clear();
            for (String skillId : preset) {
                if (plugin.getSkillRegistry().getSkill(skillId) != null) {
                    mob.addSkill(skillId);
                }
            }
            plugin.getCustomMobManager().updateMob(mob);
            player.sendMessage(ChatUtils.colorize("&#55FF55✔ Preset applied! " + preset.size() + " skills."));
            GUIUtils.playSuccessSound(player);
            open(player, holder.dungeonId, holder.mobId, holder.isBossEditor);
            return;
        }

        ItemMeta meta = e.getCurrentItem().getItemMeta();
        if (meta != null) {
            String name = org.bukkit.ChatColor.stripColor(meta.getDisplayName());
            for (SkillCategory cat : SkillCategory.values()) {
                if (cat.getDisplayName().equalsIgnoreCase(name)) {
                    openCategory(player, holder.dungeonId, holder.mobId, cat, holder.isBossEditor);
                    return;
                }
            }
        }
    }

    private void handleListClick(InventoryClickEvent e, SkillListHolder holder) {
        Player player = (Player) e.getWhoClicked();
        if (e.getSlot() == 49) {
            open(player, holder.dungeonId, holder.mobId, holder.isBossEditor);
            return;
        }

        CustomMob mob = plugin.getCustomMobManager().getMob(holder.mobId);
        ItemMeta meta = e.getCurrentItem().getItemMeta();
        if (meta != null && meta.hasLore()) {
            String idLine = org.bukkit.ChatColor.stripColor(meta.getLore().get(0));
            if (idLine.startsWith("ID: ")) {
                String skillId = idLine.substring(4);
                if (mob.getSkillIds().contains(skillId)) {
                    mob.removeSkill(skillId);
                    player.sendMessage(ChatUtils.colorize("&#FF5555✖ Skill dihapus!"));
                } else {
                    mob.addSkill(skillId);
                    player.sendMessage(ChatUtils.colorize("&#55FF55✔ Skill dipasang!"));
                }
                plugin.getCustomMobManager().updateMob(mob);
                openCategory(player, holder.dungeonId, holder.mobId, holder.category, holder.isBossEditor);
            }
        }
    }

    public static class SkillCategoryHolder implements InventoryHolder {
        public final String dungeonId, mobId;
        public final boolean isBossEditor;

        public SkillCategoryHolder(String d, String m, boolean b) {
            this.dungeonId = d;
            this.mobId = m;
            this.isBossEditor = b;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public static class SkillListHolder implements InventoryHolder {
        public final String dungeonId, mobId;
        public final SkillCategory category;
        public final boolean isBossEditor;

        public SkillListHolder(String d, String m, SkillCategory c, boolean b) {
            this.dungeonId = d;
            this.mobId = m;
            this.category = c;
            this.isBossEditor = b;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
