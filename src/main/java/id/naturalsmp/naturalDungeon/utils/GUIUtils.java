package id.naturalsmp.naturaldungeon.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * GUI utility class following NaturalCore aesthetic standards.
 * Uses Adventure Component API for modern text rendering.
 */
public class GUIUtils {

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();

    // ─── Inventory Creation ──────────────────────────────────────────

    /**
     * Create a GUI with a Component title (modern Adventure API).
     * Title should use small-caps Unicode + hex colors for premium feel.
     */
    public static Inventory createGUI(InventoryHolder holder, int size, String title) {
        Component titleComponent = toComponent(title);
        return Bukkit.createInventory(holder, size, titleComponent);
    }

    // ─── Item Creation ──────────────────────────────────────────────

    /**
     * Create an ItemStack with Component display name and lore.
     * Supports hex colors (&#RRGGBB) and legacy codes (&a, &l, etc.)
     */
    public static ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(toComponent(name));
            if (lore.length > 0) {
                meta.lore(Arrays.stream(lore)
                        .map(GUIUtils::toComponent)
                        .collect(Collectors.toList()));
            }
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES,
                    ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Create an ItemStack with a List of lore lines.
     */
    public static ItemStack createItem(Material material, String name, List<String> lore) {
        return createItem(material, name, lore.toArray(new String[0]));
    }

    // ─── Filler & Border ─────────────────────────────────────────────

    /**
     * Create a filler item (no name, decorative only).
     */
    public static ItemStack createFiller(Material material) {
        return createItem(material, " ");
    }

    /**
     * Fill all empty slots with filler items.
     */
    public static void fillEmpty(Inventory inventory, Material material) {
        ItemStack filler = createFiller(material);
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null)
                inventory.setItem(i, filler);
        }
    }

    /**
     * Fill all slots with filler items.
     */
    public static void fillAll(Inventory inventory, Material material) {
        ItemStack filler = createFiller(material);
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }
    }

    /**
     * Fill border of inventory (top, bottom, left, right columns).
     */
    public static void fillBorder(Inventory inventory, Material material) {
        ItemStack filler = createFiller(material);
        int size = inventory.getSize();
        int rows = size / 9;

        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, filler); // Top row
            inventory.setItem(size - 9 + i, filler); // Bottom row
        }
        for (int row = 1; row < rows - 1; row++) {
            inventory.setItem(row * 9, filler); // Left column
            inventory.setItem(row * 9 + 8, filler); // Right column
        }
    }

    // ─── Sound Effects ──────────────────────────────────────────────

    /**
     * Play GUI open sound.
     */
    public static void playOpenSound(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 0.5f, 1.2f);
    }

    /**
     * Play button click sound.
     */
    public static void playClickSound(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);
    }

    /**
     * Play success sound.
     */
    public static void playSuccessSound(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
    }

    /**
     * Play error sound.
     */
    public static void playErrorSound(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1f);
    }

    // ─── Text Conversion ────────────────────────────────────────────

    /**
     * Convert a legacy-formatted string (with &#RRGGBB and &codes) to a Component.
     */
    public static Component toComponent(String text) {
        if (text == null || text.isEmpty())
            return Component.empty();
        String colored = ChatUtils.colorize(text);
        return LEGACY_SERIALIZER.deserialize(colored);
    }

    // ─── Separator ──────────────────────────────────────────────────

    /**
     * Standard separator line for lore.
     */
    public static String separator() {
        return "&8&m                              ";
    }
}
