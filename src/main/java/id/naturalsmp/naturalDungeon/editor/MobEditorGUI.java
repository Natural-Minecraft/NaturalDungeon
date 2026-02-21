package id.naturalsmp.naturaldungeon.editor;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.mob.CustomMob;
import id.naturalsmp.naturaldungeon.mob.CustomMobManager;
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
        Inventory inv = GUIUtils.createGUI(new MobEditorHolder(dungeonId), 54,
                "&#FF69B4👹 ᴄᴜꜱᴛᴏᴍ ᴍᴏʙꜱ");

        GUIUtils.fillBorder(inv, Material.BLACK_STAINED_GLASS_PANE);
        GUIUtils.fillEmpty(inv, Material.GRAY_STAINED_GLASS_PANE);

        int slot = 10;
        for (CustomMob mob : mobManager.getAllMobs()) {
            if (slot > 43)
                break;
            inv.setItem(slot, GUIUtils.createItem(Material.ZOMBIE_HEAD,
                    "&#FFAA00&l" + mob.getName(),
                    GUIUtils.separator(),
                    "&7ID: &f" + mob.getId(),
                    "&7HP: &#FF5555" + mob.getHealth(),
                    "&7DMG: &#FFAA00" + mob.getDamage(),
                    "&7Speed: &#55CCFF" + mob.getSpeed(),
                    "&7Boss: " + (mob.isBoss() ? "&#55FF55✔" : "&#FF5555✗"),
                    "",
                    "&#FFAA00&l⚔ Klik &7→ Edit",
                    "&#FF5555&l✖ Shift+Klik &7→ Hapus"));
            slot++;
            if (slot % 9 == 8)
                slot += 2;
        }

        inv.setItem(49, GUIUtils.createItem(Material.EMERALD,
                "&#55FF55&l✚ ʙᴜᴀᴛ ᴍᴏʙ ʙᴀʀᴜ",
                GUIUtils.separator(),
                "&7Buat custom mob baru.",
                "",
                "&#FFAA00&l➥ KLIK"));

        inv.setItem(45, GUIUtils.createItem(Material.ARROW,
                "&#FF5555&l← ᴋᴇᴍʙᴀʟɪ"));

        player.openInventory(inv);
        GUIUtils.playOpenSound(player);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        InventoryHolder holder = e.getInventory().getHolder();
        if (holder instanceof MobConfigHolder) {
            e.setCancelled(true);
            if (e.getClickedInventory() != e.getView().getTopInventory())
                return;
            if (e.getCurrentItem() == null)
                return;
            GUIUtils.playClickSound((Player) e.getWhoClicked());
            handleConfigClick(e, (MobConfigHolder) holder);
        } else if (holder instanceof MobEditorHolder) {
            e.setCancelled(true);
            if (e.getClickedInventory() != e.getView().getTopInventory())
                return;
            if (e.getCurrentItem() == null)
                return;
            GUIUtils.playClickSound((Player) e.getWhoClicked());
            handleEditorClick(e, (MobEditorHolder) holder);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        InventoryHolder h = e.getInventory().getHolder();
        if (h instanceof MobEditorHolder || h instanceof MobConfigHolder)
            e.setCancelled(true);
    }

    private void handleEditorClick(InventoryClickEvent e, MobEditorHolder holder) {
        Player player = (Player) e.getWhoClicked();

        if (e.getSlot() == 45) {
            new DungeonMainEditorGUI(plugin).open(player, holder.dungeonId);
            return;
        }
        if (e.getSlot() == 49) {
            player.closeInventory();
            plugin.getEditorChatInput().requestInput(player,
                    ChatUtils.colorize("&#55FF55&l✚ &7Masukkan ID mob baru (huruf kecil, tanpa spasi):"),
                    input -> {
                        String id = input.toLowerCase().replace(" ", "_");
                        plugin.getCustomMobManager().createMob(id);
                        player.sendMessage(ChatUtils.colorize("&#55FF55✔ Custom mob &f" + id + " &#55FF55dibuat!"));
                        GUIUtils.playSuccessSound(player);
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
                        player.sendMessage(ChatUtils.colorize("&#FF5555✖ Mob &f" + mobId + " &#FF5555dihapus!"));
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
            player.sendMessage(ChatUtils.colorize("&#FF5555✖ Mob tidak ditemukan!"));
            GUIUtils.playErrorSound(player);
            player.closeInventory();
            return;
        }

        switch (e.getSlot()) {
            case 10 -> {
                player.closeInventory();
                plugin.getEditorChatInput().requestInput(player,
                        ChatUtils.colorize("&#FFD700&l✏ &7Masukkan nama baru:"), input -> {
                            mob.setName(input);
                            plugin.getCustomMobManager().updateMob(mob);
                            player.sendMessage(ChatUtils.colorize("&#55FF55✔ Nama diubah!"));
                            GUIUtils.playSuccessSound(player);
                            openMobConfig(player, holder.dungeonId, mobId);
                        });
            }
            case 11 -> {
                player.closeInventory();
                plugin.getEditorChatInput().requestInput(player,
                        ChatUtils.colorize("&#FF5555&l❤ &7Masukkan HP baru:"), input -> {
                            try {
                                mob.setHealth(Double.parseDouble(input));
                                plugin.getCustomMobManager().updateMob(mob);
                                GUIUtils.playSuccessSound(player);
                            } catch (NumberFormatException ex) {
                                player.sendMessage(ChatUtils.colorize("&#FF5555✖ Angka tidak valid"));
                                GUIUtils.playErrorSound(player);
                            }
                            openMobConfig(player, holder.dungeonId, mobId);
                        });
            }
            case 12 -> {
                player.closeInventory();
                plugin.getEditorChatInput().requestInput(player,
                        ChatUtils.colorize("&#FFAA00&l⚔ &7Masukkan Damage baru:"), input -> {
                            try {
                                mob.setDamage(Double.parseDouble(input));
                                plugin.getCustomMobManager().updateMob(mob);
                                GUIUtils.playSuccessSound(player);
                            } catch (NumberFormatException ex) {
                                player.sendMessage(ChatUtils.colorize("&#FF5555✖ Angka tidak valid"));
                                GUIUtils.playErrorSound(player);
                            }
                            openMobConfig(player, holder.dungeonId, mobId);
                        });
            }
            case 13 -> {
                player.closeInventory();
                plugin.getEditorChatInput().requestInput(player,
                        ChatUtils.colorize("&#55CCFF&l💨 &7Masukkan Speed (default 0.23):"), input -> {
                            try {
                                mob.setSpeed(Double.parseDouble(input));
                                plugin.getCustomMobManager().updateMob(mob);
                                GUIUtils.playSuccessSound(player);
                            } catch (NumberFormatException ex) {
                                player.sendMessage(ChatUtils.colorize("&#FF5555✖ Angka tidak valid"));
                                GUIUtils.playErrorSound(player);
                            }
                            openMobConfig(player, holder.dungeonId, mobId);
                        });
            }
            case 14 -> {
                player.closeInventory();
                plugin.getEditorChatInput().requestInput(player,
                        ChatUtils.colorize("&#55FF55&l🐾 &7Masukkan EntityType (ZOMBIE, SKELETON, dll):"), input -> {
                            try {
                                mob.setEntityType(org.bukkit.entity.EntityType.valueOf(input.toUpperCase()));
                                plugin.getCustomMobManager().updateMob(mob);
                                GUIUtils.playSuccessSound(player);
                            } catch (IllegalArgumentException ex) {
                                player.sendMessage(ChatUtils.colorize("&#FF5555✖ Tipe entity tidak valid!"));
                                GUIUtils.playErrorSound(player);
                            }
                            openMobConfig(player, holder.dungeonId, mobId);
                        });
            }
            case 15 -> {
                mob.setBoss(!mob.isBoss());
                plugin.getCustomMobManager().updateMob(mob);
                player.sendMessage(
                        ChatUtils.colorize("&#55FF55✔ Boss: " + (mob.isBoss() ? "&#55FF55ON" : "&#FF5555OFF")));
                openMobConfig(player, holder.dungeonId, mobId);
            }
            case 16 -> {
                new MobSkillEditorGUI(plugin).open(player, holder.dungeonId, mobId, mob.isBoss());
            }
            case 22 -> open(player, holder.dungeonId);
        }
    }

    public void openMobConfig(Player player, String dungeonId, String mobId) {
        CustomMob mob = plugin.getCustomMobManager().getMob(mobId);
        if (mob == null)
            return;

        Inventory inv = GUIUtils.createGUI(new MobConfigHolder(dungeonId, mobId), 27,
                "&#FF69B4👹 ᴍᴏʙ: &f" + mobId);

        GUIUtils.fillAll(inv, Material.BLACK_STAINED_GLASS_PANE);

        inv.setItem(10, GUIUtils.createItem(Material.NAME_TAG, "&#FFD700&l✏ ɴᴀᴍᴀ", GUIUtils.separator(),
                "&7Saat ini: &f" + mob.getName(), "", "&#FFAA00&l➥ KLIK"));
        inv.setItem(11, GUIUtils.createItem(Material.IRON_SWORD, "&#FF5555&l❤ ʜᴘ", GUIUtils.separator(),
                "&7Saat ini: &f" + mob.getHealth(), "", "&#FFAA00&l➥ KLIK"));
        inv.setItem(12, GUIUtils.createItem(Material.DIAMOND_SWORD, "&#FFAA00&l⚔ ᴅᴀᴍᴀɢᴇ", GUIUtils.separator(),
                "&7Saat ini: &f" + mob.getDamage(), "", "&#FFAA00&l➥ KLIK"));
        inv.setItem(13, GUIUtils.createItem(Material.FEATHER, "&#55CCFF&l💨 ꜱᴘᴇᴇᴅ", GUIUtils.separator(),
                "&7Saat ini: &f" + mob.getSpeed(), "", "&#FFAA00&l➥ KLIK"));
        inv.setItem(14, GUIUtils.createItem(Material.ZOMBIE_SPAWN_EGG, "&#55FF55&l🐾 ᴇɴᴛɪᴛʏ ᴛʏᴘᴇ", GUIUtils.separator(),
                "&7Saat ini: &f" + mob.getEntityType(), "", "&#FFAA00&l➥ KLIK"));
        inv.setItem(15, GUIUtils.createItem(Material.WITHER_SKELETON_SKULL, "&#AA44FF&l🐉 ʙᴏꜱꜱ ᴛᴏɢɢʟᴇ",
                GUIUtils.separator(), "&7Saat ini: " + (mob.isBoss() ? "&#55FF55✔ ON" : "&#FF5555✗ OFF"), "",
                "&#FFAA00&l➥ KLIK"));
        inv.setItem(16, GUIUtils.createItem(Material.BLAZE_ROD, "&#FFAA00&l🔮 ꜱᴋɪʟʟꜱ",
                GUIUtils.separator(), "&7Skills: &f" + mob.getSkillIds().size(), "", "&#FFAA00&l➥ KLIK"));

        inv.setItem(22, GUIUtils.createItem(Material.ARROW, "&#FF5555&l← ᴋᴇᴍʙᴀʟɪ"));

        player.openInventory(inv);
        GUIUtils.playOpenSound(player);
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
