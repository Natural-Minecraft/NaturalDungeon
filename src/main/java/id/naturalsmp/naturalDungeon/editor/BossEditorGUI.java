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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class BossEditorGUI implements Listener {

    private final NaturalDungeon plugin;

    public BossEditorGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String dungeonId) {
        CustomMobManager mobManager = plugin.getCustomMobManager();
        Inventory inv = GUIUtils.createGUI(new BossListHolder(dungeonId), 54,
                "&#AA44FF🐉 ʙᴏꜱꜱ ᴇᴅɪᴛᴏʀ");

        GUIUtils.fillBorder(inv, Material.BLACK_STAINED_GLASS_PANE);
        GUIUtils.fillEmpty(inv, Material.GRAY_STAINED_GLASS_PANE);

        int slot = 10;
        for (CustomMob mob : mobManager.getAllMobs()) {
            if (!mob.isBoss())
                continue;
            if (slot > 43)
                break;

            inv.setItem(slot, GUIUtils.createItem(Material.WITHER_SKELETON_SKULL,
                    "&#FF5555&l" + mob.getName(),
                    GUIUtils.separator(),
                    "&7ID: &f" + mob.getId(),
                    "&7HP: &#FF5555" + mob.getHealth(),
                    "&7DMG: &#FFAA00" + mob.getDamage(),
                    "",
                    "&#FFAA00&l⚔ Klik &7→ Edit",
                    "&#FF5555&l✖ Shift+Klik &7→ Hapus"));
            slot++;
            if (slot % 9 == 8)
                slot += 2;
        }

        inv.setItem(49, GUIUtils.createItem(Material.NETHER_STAR, "&#55FF55&l✚ ʙᴜᴀᴛ ʙᴏꜱꜱ ʙᴀʀᴜ"));
        inv.setItem(45, GUIUtils.createItem(Material.ARROW, "&#FF5555&l← ᴋᴇᴍʙᴀʟɪ"));

        player.openInventory(inv);
        GUIUtils.playOpenSound(player);
    }

    public void openBossConfig(Player player, String dungeonId, String mobId) {
        CustomMob mob = plugin.getCustomMobManager().getMob(mobId);
        if (mob == null)
            return;

        Inventory inv = GUIUtils.createGUI(new BossConfigHolder(dungeonId, mobId), 54,
                "&#AA44FF🐉 ʙᴏꜱꜱ: &f" + mobId);

        GUIUtils.fillBorder(inv, Material.BLACK_STAINED_GLASS_PANE);
        GUIUtils.fillEmpty(inv, Material.GRAY_STAINED_GLASS_PANE);

        // Stats
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

        // Equipment
        inv.setItem(28, GUIUtils.createItem(Material.DIAMOND_HELMET, "&#55CCFF&lHelmet", "&7Pegang item lalu klik."));
        inv.setItem(29,
                GUIUtils.createItem(Material.DIAMOND_CHESTPLATE, "&#55CCFF&lChestplate", "&7Pegang item lalu klik."));
        inv.setItem(30,
                GUIUtils.createItem(Material.DIAMOND_LEGGINGS, "&#55CCFF&lLeggings", "&7Pegang item lalu klik."));
        inv.setItem(31, GUIUtils.createItem(Material.DIAMOND_BOOTS, "&#55CCFF&lBoots", "&7Pegang item lalu klik."));
        inv.setItem(33, GUIUtils.createItem(Material.DIAMOND_SWORD, "&#FF5555&lMain Hand", "&7Pegang item lalu klik."));
        inv.setItem(34, GUIUtils.createItem(Material.SHIELD, "&#FFAA00&lOff Hand", "&7Pegang item lalu klik."));

        // Skills
        inv.setItem(16, GUIUtils.createItem(Material.ENCHANTED_BOOK, "&#AA44FF&l🔮 ꜱᴋɪʟʟꜱ",
                GUIUtils.separator(), "&7Skills: &f" + mob.getSkillIds().size(), "", "&#FFAA00&l➥ KLIK"));

        inv.setItem(49, GUIUtils.createItem(Material.ARROW, "&#FF5555&l← ᴋᴇᴍʙᴀʟɪ"));

        player.openInventory(inv);
        GUIUtils.playOpenSound(player);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        InventoryHolder h = e.getInventory().getHolder();
        if (e.getCurrentItem() == null)
            return;

        if (h instanceof BossListHolder holder) {
            e.setCancelled(true);
            if (e.getClickedInventory() != e.getView().getTopInventory())
                return;
            GUIUtils.playClickSound((Player) e.getWhoClicked());
            handleListClick(e, holder);
        } else if (h instanceof BossConfigHolder holder) {
            e.setCancelled(true);
            if (e.getClickedInventory() != e.getView().getTopInventory())
                return;
            GUIUtils.playClickSound((Player) e.getWhoClicked());
            handleConfigClick(e, holder);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        InventoryHolder h = e.getInventory().getHolder();
        if (h instanceof BossListHolder || h instanceof BossConfigHolder)
            e.setCancelled(true);
    }

    private void handleListClick(InventoryClickEvent e, BossListHolder holder) {
        Player player = (Player) e.getWhoClicked();
        if (e.getSlot() == 45) {
            new DungeonMainEditorGUI(plugin).open(player, holder.dungeonId);
        } else if (e.getSlot() == 49) {
            plugin.getEditorChatInput().requestInput(player, "&eMasukkan ID Boss Baru:", input -> {
                String id = input.toLowerCase().replace(" ", "_");
                plugin.getCustomMobManager().createMob(id);
                CustomMob mob = plugin.getCustomMobManager().getMob(id);
                mob.setBoss(true); // Auto set boss
                mob.setHealth(100.0); // Default boss HP
                mob.setDamage(10.0);
                plugin.getCustomMobManager().updateMob(mob);
                openBossConfig(player, holder.dungeonId, id);
            });
        } else {
            ItemMeta meta = e.getCurrentItem().getItemMeta();
            if (meta != null && meta.hasLore()) {
                String idLine = org.bukkit.ChatColor.stripColor(meta.getLore().get(0));
                if (idLine.startsWith("ID: ")) {
                    String mobId = idLine.substring(4);
                    if (e.isShiftClick()) {
                        plugin.getCustomMobManager().deleteMob(mobId);
                        open(player, holder.dungeonId);
                    } else {
                        openBossConfig(player, holder.dungeonId, mobId);
                    }
                }
            }
        }
    }

    private void handleConfigClick(InventoryClickEvent e, BossConfigHolder holder) {
        Player player = (Player) e.getWhoClicked();
        CustomMob mob = plugin.getCustomMobManager().getMob(holder.mobId);
        if (mob == null) {
            player.closeInventory();
            return;
        }

        switch (e.getSlot()) {
            case 10: // Name
                plugin.getEditorChatInput().requestInput(player, "Nama Boss:", input -> {
                    mob.setName(input);
                    plugin.getCustomMobManager().updateMob(mob);
                    openBossConfig(player, holder.dungeonId, holder.mobId);
                });
                break;
            case 11: // HP
                plugin.getEditorChatInput().requestInput(player, "HP Boss:", input -> {
                    try {
                        mob.setHealth(Double.parseDouble(input));
                        plugin.getCustomMobManager().updateMob(mob);
                    } catch (Exception ex) {
                    }
                    openBossConfig(player, holder.dungeonId, holder.mobId);
                });
                break;
            case 12: // Damage
                plugin.getEditorChatInput().requestInput(player, "Damage Boss:", input -> {
                    try {
                        mob.setDamage(Double.parseDouble(input));
                        plugin.getCustomMobManager().updateMob(mob);
                    } catch (Exception ex) {
                    }
                    openBossConfig(player, holder.dungeonId, holder.mobId);
                });
                break;
            case 13: // Speed
                plugin.getEditorChatInput().requestInput(player, "Speed Boss:", input -> {
                    try {
                        mob.setSpeed(Double.parseDouble(input));
                        plugin.getCustomMobManager().updateMob(mob);
                    } catch (Exception ex) {
                    }
                    openBossConfig(player, holder.dungeonId, holder.mobId);
                });
                break;
            case 14: // Type
                plugin.getEditorChatInput().requestInput(player, "Entity Type:", input -> {
                    try {
                        mob.setEntityType(org.bukkit.entity.EntityType.valueOf(input.toUpperCase()));
                        plugin.getCustomMobManager().updateMob(mob);
                    } catch (Exception ex) {
                    }
                    openBossConfig(player, holder.dungeonId, holder.mobId);
                });
                break;
            case 16: // Skills
                // Open Skill Editor
                new MobSkillEditorGUI(plugin).open(player, holder.dungeonId, holder.mobId, true);
                break;
            case 28: // Helmet
            case 29: // Chest
            case 30: // Leggings
            case 31: // Boots
            case 33: // Main Hand
            case 34: // Off Hand
                // Simple implementation: prompt user to hold item and type "set"
                player.sendMessage(ChatUtils.colorize("&ePegang item yang diinginkan, lalu ketik 'set' di chat."));
                plugin.getEditorChatInput().requestInput(player, "&eKetik 'set' untuk set item dari tangan:", input -> {
                    if (input.equalsIgnoreCase("set")) {
                        ItemStack item = player.getInventory().getItemInMainHand();
                        // For now we just store material or simple logic.
                        // To be truly complex, we'd need to serialize the ItemStack.
                        // Assuming CustomMob stores Material or serialized item.
                        // CustomMob.equipment is Map<String, Object>.
                        // We will store the Material name for now as per simple design,
                        // unless user asks for full ItemStack NBT serialization.
                        // Let's assume standard Material for now.
                        if (item.getType() != Material.AIR) {
                            String slotName = switch (e.getSlot()) {
                                case 28 -> "HELMET";
                                case 29 -> "CHESTPLATE";
                                case 30 -> "LEGGINGS";
                                case 31 -> "BOOTS";
                                case 33 -> "HAND";
                                case 34 -> "OFFHAND";
                                default -> "HAND";
                            };
                            mob.getEquipment().put(slotName, item.getType().name());
                            plugin.getCustomMobManager().updateMob(mob);
                            player.sendMessage(ChatUtils.colorize("&aEquipment updated!"));
                        }
                    }
                    openBossConfig(player, holder.dungeonId, holder.mobId);
                });
                break;
            case 49:
                open(player, holder.dungeonId);
                break;
        }
    }

    // Using GUIUtils.createItem() instead of local createItem

    public static class BossListHolder implements InventoryHolder {
        public final String dungeonId;

        public BossListHolder(String d) {
            this.dungeonId = d;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public static class BossConfigHolder implements InventoryHolder {
        public final String dungeonId, mobId;

        public BossConfigHolder(String d, String m) {
            this.dungeonId = d;
            this.mobId = m;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
