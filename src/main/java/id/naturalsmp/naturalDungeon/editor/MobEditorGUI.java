package id.naturalsmp.naturaldungeon.editor;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.mob.CustomMob;
import id.naturalsmp.naturaldungeon.mob.CustomMobManager;
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

import java.util.*;

/**
 * Custom mob manager GUI: list all custom mobs, create/edit/delete.
 */
public class MobEditorGUI implements Listener {

    private final NaturalDungeon plugin;

    public MobEditorGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String dungeonId) {
        CustomMobManager mobManager = plugin.getCustomMobManager();
        Inventory inv = Bukkit.createInventory(new MobEditorHolder(dungeonId), 54,
                ChatUtils.colorize("&d&lCUSTOM MOBS"));
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++)
            inv.setItem(i, filler);

        int slot = 10;
        for (CustomMob mob : mobManager.getAllMobs()) {
            if (slot > 43)
                break;
            inv.setItem(slot, createItem(Material.ZOMBIE_HEAD,
                    "&e" + mob.getName(),
                    "&7ID: &f" + mob.getId(),
                    "&7HP: &c" + mob.getHealth(),
                    "&7DMG: &c" + mob.getDamage(),
                    "&7Speed: &b" + mob.getSpeed(),
                    "&7Boss: " + (mob.isBoss() ? "&a✔" : "&c✗"),
                    "", "&aKlik untuk edit",
                    "&cShift+Klik untuk hapus"));
            slot++;
            if (slot % 9 == 8)
                slot += 2;
        }

        inv.setItem(49, createItem(Material.EMERALD, "&a&lBuat Custom Mob Baru"));
        inv.setItem(45, createItem(Material.ARROW, "&cKembali"));

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        InventoryHolder holder = e.getInventory().getHolder();
        if (holder instanceof MobConfigHolder) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null)
                return;
            handleConfigClick(e, (MobConfigHolder) holder);
        } else if (holder instanceof MobEditorHolder) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null)
                return;
            handleEditorClick(e, (MobEditorHolder) holder);
        }
    }

    private void handleEditorClick(InventoryClickEvent e, MobEditorHolder holder) {
        Player player = (Player) e.getWhoClicked();

        if (e.getSlot() == 45) {
            new DungeonMainEditorGUI(plugin).open(player, holder.dungeonId);
            return;
        }
        if (e.getSlot() == 49) {
            plugin.getEditorChatInput().requestInput(player,
                    ChatUtils.colorize("&eMasukkan ID mob baru (huruf kecil, tanpa spasi):"),
                    input -> {
                        String id = input.toLowerCase().replace(" ", "_");
                        plugin.getCustomMobManager().createMob(id);
                        player.sendMessage(ChatUtils.colorize("&aCustom mob &f" + id + " &adibuat!"));
                        openMobConfig(player, holder.dungeonId, id);
                    });
            return;
        }

        // Check if clicking a mob item
        ItemMeta meta = e.getCurrentItem().getItemMeta();
        if (meta != null && meta.hasLore()) {
            List<String> lore = meta.getLore();
            if (lore != null && lore.size() > 1) {
                String idLine = org.bukkit.ChatColor.stripColor(lore.get(0));
                if (idLine.startsWith("ID: ")) {
                    String mobId = idLine.substring(4);
                    if (e.isShiftClick()) {
                        plugin.getCustomMobManager().deleteMob(mobId);
                        player.sendMessage(ChatUtils.colorize("&cMob &f" + mobId + " &cdihapus!"));
                        open(player, holder.dungeonId);
                    } else {
                        openMobConfig(player, holder.dungeonId, mobId);
                    }
                }
            }
        }
    }

    private void handleConfigClick(InventoryClickEvent e, MobConfigHolder holder) {
        Player player = (Player) e.getWhoClicked();
        String mobId = holder.mobId;
        CustomMob mob = plugin.getCustomMobManager().getMob(mobId);
        if (mob == null) {
            player.sendMessage(ChatUtils.colorize("&cMob tidak ditemukan!"));
            player.closeInventory();
            return;
        }

        switch (e.getSlot()) {
            case 10: // Name
                plugin.getEditorChatInput().requestInput(player, "&eMasukkan nama baru:", input -> {
                    mob.setName(input);
                    plugin.getCustomMobManager().updateMob(mob);
                    player.sendMessage(ChatUtils.colorize("&aNama diubah!"));
                    openMobConfig(player, holder.dungeonId, mobId);
                });
                break;
            case 11: // HP
                plugin.getEditorChatInput().requestInput(player, "&eMasukkan HP baru:", input -> {
                    try {
                        double val = Double.parseDouble(input);
                        mob.setHealth(val);
                        plugin.getCustomMobManager().updateMob(mob);
                        openMobConfig(player, holder.dungeonId, mobId);
                    } catch (NumberFormatException ex) {
                        player.sendMessage(ChatUtils.colorize("&cAngka tidak valid"));
                    }
                });
                break;
            case 12: // Damage
                plugin.getEditorChatInput().requestInput(player, "&eMasukkan Damage baru:", input -> {
                    try {
                        double val = Double.parseDouble(input);
                        mob.setDamage(val);
                        plugin.getCustomMobManager().updateMob(mob);
                        openMobConfig(player, holder.dungeonId, mobId);
                    } catch (NumberFormatException ex) {
                        player.sendMessage(ChatUtils.colorize("&cAngka tidak valid"));
                    }
                });
                break;
            case 13: // Speed
                plugin.getEditorChatInput().requestInput(player, "&eMasukkan Speed baru (default 0.23):", input -> {
                    try {
                        double val = Double.parseDouble(input);
                        mob.setSpeed(val);
                        plugin.getCustomMobManager().updateMob(mob);
                        openMobConfig(player, holder.dungeonId, mobId);
                    } catch (NumberFormatException ex) {
                        player.sendMessage(ChatUtils.colorize("&cAngka tidak valid"));
                    }
                });
                break;
            case 14: // Entity Type
                plugin.getEditorChatInput().requestInput(player, "&eMasukkan EntityType (misal ZOMBIE, SKELETON):",
                        input -> {
                            try {
                                org.bukkit.entity.EntityType type = org.bukkit.entity.EntityType
                                        .valueOf(input.toUpperCase());
                                mob.setEntityType(type);
                                plugin.getCustomMobManager().updateMob(mob);
                                openMobConfig(player, holder.dungeonId, mobId);
                            } catch (IllegalArgumentException ex) {
                                player.sendMessage(ChatUtils.colorize("&cTipe entity tidak valid!"));
                            }
                        });
                break;
            case 15: // Boss Toggle
                mob.setBoss(!mob.isBoss());
                plugin.getCustomMobManager().updateMob(mob);
                openMobConfig(player, holder.dungeonId, mobId);
                break;
            case 16: // Skills
                // For now, simple message or placeholder. Boss Editor will have "more complex"
                // skill editor.
                player.sendMessage(
                        ChatUtils.colorize("&eSkill management will be available in Boss Editor or next update!"));
                break;
            case 22: // Back
                open(player, holder.dungeonId);
                break;
        }
    }

    public void openMobConfig(Player player, String dungeonId, String mobId) {
        CustomMob mob = plugin.getCustomMobManager().getMob(mobId);
        if (mob == null)
            return;
        Inventory inv = Bukkit.createInventory(new MobConfigHolder(dungeonId, mobId), 27,
                ChatUtils.colorize("&d&lMOB: &e" + mobId));
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++)
            inv.setItem(i, filler);

        inv.setItem(10,
                createItem(Material.NAME_TAG, "&e&lNama", "&7Saat ini: &f" + mob.getName(), "&aKlik untuk ubah"));
        inv.setItem(11, createItem(Material.IRON_SWORD, "&c&lHP", "&7Saat ini: &f" + mob.getHealth(), "&aKlik"));
        inv.setItem(12, createItem(Material.DIAMOND_SWORD, "&c&lDamage", "&7Saat ini: &f" + mob.getDamage(), "&aKlik"));
        inv.setItem(13, createItem(Material.FEATHER, "&b&lSpeed", "&7Saat ini: &f" + mob.getSpeed(), "&aKlik"));
        inv.setItem(14, createItem(Material.ZOMBIE_SPAWN_EGG, "&e&lEntity Type", "&7Saat ini: &f" + mob.getEntityType(),
                "&aKlik"));
        inv.setItem(15, createItem(Material.WITHER_SKELETON_SKULL, "&d&lBoss Toggle",
                "&7Saat ini: " + (mob.isBoss() ? "&a✔" : "&c✗"), "&aKlik untuk toggle"));
        inv.setItem(16, createItem(Material.BLAZE_ROD, "&6&lSkills", "&aKlik untuk lihat/assign skills"));

        inv.setItem(22, createItem(Material.ARROW, "&cKembali"));
        player.openInventory(inv);
    }

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.colorize(name));
        List<String> list = new ArrayList<>();
        for (String l : lore)
            list.add(ChatUtils.colorize(l));
        meta.setLore(list);
        item.setItemMeta(meta);
        return item;
    }

    public static class MobEditorHolder implements InventoryHolder {
        public final String dungeonId;

        public MobEditorHolder(String id) {
            this.dungeonId = id;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public static class MobConfigHolder implements InventoryHolder {
        public final String dungeonId, mobId;

        public MobConfigHolder(String d, String m) {
            this.dungeonId = d;
            this.mobId = m;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
