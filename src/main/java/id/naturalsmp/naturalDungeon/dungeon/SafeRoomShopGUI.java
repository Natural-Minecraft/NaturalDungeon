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
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Safe Room Shop — purchasable buffs and items during safe room breaks.
 * Uses dungeon currency (collected coins/XP during the run).
 */
public class SafeRoomShopGUI implements Listener {

    private final NaturalDungeon plugin;

    private static final ShopItem[] ITEMS = {
            new ShopItem(Material.GOLDEN_APPLE, "&6🍎 Golden Apple",
                    "Restores 4 hearts + Absorption", 30, ShopAction.GIVE_ITEM),
            new ShopItem(Material.SPLASH_POTION, "&c❤ Healing Potion",
                    "Instant Health II splash", 20, ShopAction.HEAL),
            new ShopItem(Material.IRON_CHESTPLATE, "&f🛡 Iron Armor Set",
                    "Full iron armor set", 80, ShopAction.GIVE_ARMOR),
            new ShopItem(Material.IRON_SWORD, "&f🗡 Iron Sword",
                    "Iron Sword with Sharpness I", 50, ShopAction.GIVE_WEAPON),
            new ShopItem(Material.ARROW, "&7🏹 Arrow Bundle (16)",
                    "16 arrows for ranged combat", 15, ShopAction.GIVE_ARROWS),
            new ShopItem(Material.BLAZE_POWDER, "&6⚡ Strength Boost",
                    "Strength I for 60 seconds", 40, ShopAction.BUFF_STRENGTH),
            new ShopItem(Material.SUGAR, "&b⚡ Speed Boost",
                    "Speed II for 60 seconds", 35, ShopAction.BUFF_SPEED),
            new ShopItem(Material.GHAST_TEAR, "&a💚 Regen Boost",
                    "Regeneration II for 30 seconds", 45, ShopAction.BUFF_REGEN),
            new ShopItem(Material.SHIELD, "&b🛡 Resistance",
                    "Resistance I for 60 seconds", 50, ShopAction.BUFF_RESISTANCE),
            new ShopItem(Material.ENDER_PEARL, "&5🌀 Ender Pearl",
                    "1 Ender Pearl for escape", 25, ShopAction.GIVE_PEARL),
    };

    public SafeRoomShopGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, DungeonInstance instance) {
        int playerCoins = instance.getPlayerCoins(player.getUniqueId());

        Inventory inv = GUIUtils.createGUI(new ShopHolder(instance), 54,
                "&#FFD700🛒 ꜱᴀꜰᴇ ʀᴏᴏᴍ ꜱʜᴏᴘ");

        GUIUtils.fillBorder(inv, Material.BLACK_STAINED_GLASS_PANE);
        GUIUtils.fillEmpty(inv, Material.GRAY_STAINED_GLASS_PANE);

        // Show items in grid (slots 10-16, 19-25)
        int[] slots = { 10, 11, 12, 13, 14, 15, 16, 19, 20, 21 };
        for (int i = 0; i < Math.min(ITEMS.length, slots.length); i++) {
            ShopItem item = ITEMS[i];
            boolean canAfford = playerCoins >= item.cost;
            String costColor = canAfford ? "&#55FF55" : "&#FF5555";

            inv.setItem(slots[i], GUIUtils.createItem(item.icon,
                    item.name,
                    GUIUtils.separator(),
                    "&7" + item.description,
                    "",
                    costColor + "💰 " + item.cost + " coins",
                    "&7Saldo: &f" + playerCoins,
                    "",
                    canAfford ? "&#55FF55&l➥ KLIK untuk beli" : "&#FF5555&lSaldo tidak cukup!"));
        }

        // Player balance display
        inv.setItem(40, GUIUtils.createItem(Material.GOLD_INGOT,
                "&#FFD700&l💰 Saldo: &f" + playerCoins,
                GUIUtils.separator(),
                "&7Koin didapat dari kill mobs",
                "&7dan menyelesaikan wave."));

        // Close button
        inv.setItem(49, GUIUtils.createItem(Material.BARRIER,
                "&#FF5555&l✖ ᴛᴜᴛᴜᴘ"));

        player.openInventory(inv);
        GUIUtils.playOpenSound(player);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof ShopHolder holder))
            return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getView().getTopInventory())
            return;
        if (e.getCurrentItem() == null)
            return;

        Player player = (Player) e.getWhoClicked();
        GUIUtils.playClickSound(player);

        if (e.getSlot() == 49) {
            player.closeInventory();
            return;
        }

        // Find which item was clicked
        int[] slots = { 10, 11, 12, 13, 14, 15, 16, 19, 20, 21 };
        int itemIdx = -1;
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == e.getSlot()) {
                itemIdx = i;
                break;
            }
        }

        if (itemIdx < 0 || itemIdx >= ITEMS.length)
            return;

        ShopItem item = ITEMS[itemIdx];
        DungeonInstance instance = holder.instance;
        int coins = instance.getPlayerCoins(player.getUniqueId());

        if (coins < item.cost) {
            player.sendMessage(
                    ChatUtils.colorize("&#FF5555✖ &7Koin tidak cukup! Butuh &f" + item.cost + " &7punya &f" + coins));
            GUIUtils.playErrorSound(player);
            return;
        }

        // Deduct coins
        instance.addPlayerCoins(player.getUniqueId(), -item.cost);

        // Execute action
        switch (item.action) {
            case GIVE_ITEM -> player.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 1));
            case HEAL -> player.setHealth(Math.min(player.getHealth() + 8, player.getAttribute(
                    org.bukkit.attribute.Attribute.MAX_HEALTH).getValue()));
            case GIVE_ARMOR -> {
                player.getInventory().addItem(
                        new ItemStack(Material.IRON_HELMET),
                        new ItemStack(Material.IRON_CHESTPLATE),
                        new ItemStack(Material.IRON_LEGGINGS),
                        new ItemStack(Material.IRON_BOOTS));
            }
            case GIVE_WEAPON -> {
                ItemStack sword = new ItemStack(Material.IRON_SWORD);
                sword.addEnchantment(org.bukkit.enchantments.Enchantment.SHARPNESS, 1);
                player.getInventory().addItem(sword);
            }
            case GIVE_ARROWS -> player.getInventory().addItem(new ItemStack(Material.ARROW, 16));
            case BUFF_STRENGTH -> player.addPotionEffect(
                    new PotionEffect(PotionEffectType.STRENGTH, 1200, 0, false, true));
            case BUFF_SPEED -> player.addPotionEffect(
                    new PotionEffect(PotionEffectType.SPEED, 1200, 1, false, true));
            case BUFF_REGEN -> player.addPotionEffect(
                    new PotionEffect(PotionEffectType.REGENERATION, 600, 1, false, true));
            case BUFF_RESISTANCE -> player.addPotionEffect(
                    new PotionEffect(PotionEffectType.RESISTANCE, 1200, 0, false, true));
            case GIVE_PEARL -> player.getInventory().addItem(new ItemStack(Material.ENDER_PEARL, 1));
        }

        player.sendMessage(ChatUtils.colorize("&#55FF55✔ &7Membeli " + ChatUtils.colorize(item.name) + "!"));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);

        // Re-open with updated balance
        open(player, instance);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof ShopHolder)
            e.setCancelled(true);
    }

    // ─── Inner Types ──────────────────────────────────────────────

    enum ShopAction {
        GIVE_ITEM, HEAL, GIVE_ARMOR, GIVE_WEAPON, GIVE_ARROWS,
        BUFF_STRENGTH, BUFF_SPEED, BUFF_REGEN, BUFF_RESISTANCE, GIVE_PEARL
    }

    static class ShopItem {
        final Material icon;
        final String name;
        final String description;
        final int cost;
        final ShopAction action;

        ShopItem(Material icon, String name, String description, int cost, ShopAction action) {
            this.icon = icon;
            this.name = name;
            this.description = description;
            this.cost = cost;
            this.action = action;
        }
    }

    public static class ShopHolder implements InventoryHolder {
        public final DungeonInstance instance;

        public ShopHolder(DungeonInstance instance) {
            this.instance = instance;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
