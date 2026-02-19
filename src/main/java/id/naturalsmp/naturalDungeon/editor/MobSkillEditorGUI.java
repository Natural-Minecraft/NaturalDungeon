package id.naturalsmp.naturaldungeon.editor;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.mob.CustomMob;
import id.naturalsmp.naturaldungeon.skill.MobSkill;
import id.naturalsmp.naturaldungeon.skill.SkillCategory;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class MobSkillEditorGUI implements Listener {

    private final NaturalDungeon plugin;

    public MobSkillEditorGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String dungeonId, String mobId, boolean isBossEditor) {
        Inventory inv = Bukkit.createInventory(new SkillCategoryHolder(dungeonId, mobId, isBossEditor), 27,
                ChatUtils.colorize("&d&lSKILL CATEGORIES"));

        int[] slots = { 10, 11, 12, 13, 14, 15, 16, 19, 20, 21 };
        SkillCategory[] cats = SkillCategory.values();

        for (int i = 0; i < cats.length; i++) {
            if (i >= slots.length)
                break;
            SkillCategory cat = cats[i];
            inv.setItem(slots[i], createItem(Material.BOOK, "&e" + cat.getDisplayName(), "&7" + cat.getDescription()));
        }

        inv.setItem(22, createItem(Material.ARROW, "&cKembali"));
        player.openInventory(inv);
    }

    public void openCategory(Player player, String dungeonId, String mobId, SkillCategory category,
            boolean isBossEditor) {
        Inventory inv = Bukkit.createInventory(new SkillListHolder(dungeonId, mobId, category, isBossEditor), 54,
                ChatUtils.colorize("&8" + category.getDisplayName() + " Skills"));

        CustomMob mob = plugin.getCustomMobManager().getMob(mobId);
        List<MobSkill> skills = plugin.getSkillRegistry().getSkillsByCategory(category);

        int slot = 0;
        for (MobSkill skill : skills) {
            boolean hasSkill = mob.getSkillIds().contains(skill.getId());
            inv.setItem(slot, createItem(hasSkill ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE,
                    (hasSkill ? "&a" : "&c") + skill.getDisplayName(),
                    "&7ID: &f" + skill.getId(),
                    "&7Cooldown: &f" + skill.getCooldownTicks() + " ticks",
                    "",
                    hasSkill ? "&aTERPASANG (Klik untuk hapus)" : "&cTIDAK TERPASANG (Klik untuk pasang)"));
            slot++;
        }

        inv.setItem(49, createItem(Material.ARROW, "&cKembali"));
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        InventoryHolder h = e.getInventory().getHolder();
        if (e.getCurrentItem() == null)
            return;

        if (h instanceof SkillCategoryHolder holder) {
            e.setCancelled(true);
            handleCategoryClick(e, holder);
        } else if (h instanceof SkillListHolder holder) {
            e.setCancelled(true);
            handleListClick(e, holder);
        }
    }

    private void handleCategoryClick(InventoryClickEvent e, SkillCategoryHolder holder) {
        Player player = (Player) e.getWhoClicked();
        if (e.getSlot() == 22) {
            if (holder.isBossEditor) {
                new BossEditorGUI(plugin).openBossConfig(player, holder.dungeonId, holder.mobId); // Need to expose this
                                                                                                  // or use
                                                                                                  // reflection/public??
                // BossEditorGUI.openBossConfig is private in my previous step...
                // Mistake! I need to make it public or access it differently.
                // For now, I'll update BossEditorGUI to make it public or use a different entry
                // point.
                // Or I can just open the list if I can't go back to config directly easily
                // without an instance.
                // Wait, I can instantiate BossEditorGUI. I need to make the method public.
                // For MobEditorGUI, logic is similar.
                // Let's assume I will make openBossConfig public in next step.
                // Actually, I can just open the main editor for now if that fails, but I should
                // fix it.
                // I will update BossEditorGUI to make openBossConfig public.
            } else {
                // Open Mob Config (private too? Check MobEditorGUI)
                // MobEditorGUI.openMobConfig is private.
                // I need to fix visibility for both.
                new MobEditorGUI(plugin).open(player, holder.dungeonId); // Fallback to list
            }
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
                    // player.sendMessage(ChatUtils.colorize("&cSkill removed!"));
                } else {
                    mob.addSkill(skillId);
                    // player.sendMessage(ChatUtils.colorize("&aSkill added!"));
                }
                plugin.getCustomMobManager().updateMob(mob);
                openCategory(player, holder.dungeonId, holder.mobId, holder.category, holder.isBossEditor);
            }
        }
    }

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatUtils.colorize(name));
            List<String> list = new ArrayList<>();
            for (String l : lore)
                list.add(ChatUtils.colorize(l));
            meta.setLore(list);
            item.setItemMeta(meta);
        }
        return item;
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
