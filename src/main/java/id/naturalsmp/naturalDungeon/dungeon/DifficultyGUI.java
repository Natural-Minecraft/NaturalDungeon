package id.naturalsmp.naturaldungeon.dungeon;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.party.PartyConfirmationGUI;
import id.naturalsmp.naturaldungeon.utils.ConfigUtils;
import id.naturalsmp.naturaldungeon.utils.GUIUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class DifficultyGUI implements Listener {

    private final NaturalDungeon plugin;
    private final NamespacedKey diffKey;

    public DifficultyGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
        this.diffKey = new NamespacedKey(plugin, "difficulty_id");
    }

    public void open(Player player, String dungeonId) {
        Dungeon dungeon = plugin.getDungeonManager().getDungeon(dungeonId);
        if (dungeon == null)
            return;

        Inventory inv = GUIUtils.createGUI(new DifficultyHolder(dungeonId), 27,
                "&#FFAA00⚙ ᴘɪʟɪʜ ᴅɪꜰꜰɪᴄᴜʟᴛʏ ⚙");

        GUIUtils.fillAll(inv, Material.BLACK_STAINED_GLASS_PANE);

        // Centered slots for up to 5 difficulties
        int[] slots = { 10, 11, 13, 15, 16 };
        int count = dungeon.getDifficulties().size();
        int[] usedSlots = centerSlots(count);

        int idx = 0;
        for (DungeonDifficulty diff : dungeon.getDifficulties()) {
            if (idx >= usedSlots.length)
                break;

            String keyDisplay = (diff.getKeyReq() != null && !diff.getKeyReq().equalsIgnoreCase("none"))
                    ? "&#FF5555" + diff.getKeyReq()
                    : "&#55FF55Tidak ada";

            Material mat = getDifficultyMaterial(diff.getId());
            String color = getDifficultyColor(diff.getId());

            List<String> lore = new ArrayList<>();
            lore.add(GUIUtils.separator());
            lore.add("&7Min Tier: " + color + diff.getMinTier() + "+");
            lore.add("&7Key: " + keyDisplay);
            lore.add("&7Max Deaths: &#FF5555" + diff.getMaxDeaths());
            lore.add("&7Reward: &#FFAA00" + diff.getRewardMultiplier() + "x");
            lore.add("");
            lore.add("&#FFAA00&l⚔ Kiri-Klik &7→ Main");
            lore.add("&#55CCFF&l👁 Kanan-Klik &7→ Preview Loot");

            ItemStack item = GUIUtils.createItem(mat, color + "&l" + diff.getDisplayName(), lore);
            ItemMeta meta = item.getItemMeta();
            meta.getPersistentDataContainer().set(diffKey, PersistentDataType.STRING, diff.getId());
            item.setItemMeta(meta);

            inv.setItem(usedSlots[idx++], item);
        }

        // Back button (slot 22)
        inv.setItem(22, GUIUtils.createItem(Material.ARROW,
                "&#FF5555&l← ᴋᴇᴍʙᴀʟɪ",
                "&7Kembali ke daftar dungeon."));

        player.openInventory(inv);
        GUIUtils.playOpenSound(player);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof DifficultyHolder holder))
            return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getView().getTopInventory())
            return;
        if (e.getCurrentItem() == null)
            return;

        Player player = (Player) e.getWhoClicked();

        // Back button
        if (e.getCurrentItem().getType() == Material.ARROW) {
            GUIUtils.playClickSound(player);
            plugin.getDungeonManager().openDungeonGUI(player);
            return;
        }

        ItemMeta meta = e.getCurrentItem().getItemMeta();
        if (meta == null || !meta.getPersistentDataContainer().has(diffKey, PersistentDataType.STRING))
            return;

        String diffId = meta.getPersistentDataContainer().get(diffKey, PersistentDataType.STRING);
        Dungeon dungeon = plugin.getDungeonManager().getDungeon(holder.dungeonId);

        if (dungeon != null) {
            DungeonDifficulty diff = dungeon.getDifficulty(diffId);
            if (diff != null) {
                if (e.isRightClick()) {
                    // Open Loot Preview
                    GUIUtils.playClickSound(player);
                    new id.naturalsmp.naturaldungeon.loot.LootPreviewGUI(plugin).open(player, dungeon, diff);
                    return;
                }

                // Validate Requirements
                if (!checkRequirements(player, diff)) {
                    GUIUtils.playErrorSound(player);
                    player.closeInventory();
                    return;
                }

                // Open Confirmation
                GUIUtils.playClickSound(player);
                new PartyConfirmationGUI(plugin).startConfirmation(player, dungeon, diff);
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof DifficultyHolder)
            e.setCancelled(true);
    }

    private boolean checkRequirements(Player player, DungeonDifficulty diff) {
        if (plugin.getNaturalCoreHook().isEnabled()) {
            int playerTier = plugin.getNaturalCoreHook().getPlayerTier(player);
            if (playerTier < diff.getMinTier()) {
                player.sendMessage(
                        ConfigUtils.getMessage("dungeon.tier-too-low", "%tier%", String.valueOf(diff.getMinTier())));
                return false;
            }
        }
        return true;
    }

    private int[] centerSlots(int count) {
        return switch (count) {
            case 1 -> new int[] { 13 };
            case 2 -> new int[] { 12, 14 };
            case 3 -> new int[] { 11, 13, 15 };
            case 4 -> new int[] { 10, 12, 14, 16 };
            default -> new int[] { 10, 11, 13, 15, 16 };
        };
    }

    private Material getDifficultyMaterial(String id) {
        return switch (id.toLowerCase()) {
            case "easy" -> Material.LIME_BANNER;
            case "normal" -> Material.YELLOW_BANNER;
            case "hard" -> Material.ORANGE_BANNER;
            case "hell", "nightmare" -> Material.RED_BANNER;
            case "extreme", "inferno" -> Material.BLACK_BANNER;
            default -> Material.WHITE_BANNER;
        };
    }

    private String getDifficultyColor(String id) {
        return switch (id.toLowerCase()) {
            case "easy" -> "&#55FF55";
            case "normal" -> "&#FFFF55";
            case "hard" -> "&#FFAA00";
            case "hell", "nightmare" -> "&#FF5555";
            case "extreme", "inferno" -> "&#AA00AA";
            default -> "&#FFFFFF";
        };
    }

    public static class DifficultyHolder implements InventoryHolder {
        public final String dungeonId;

        public DifficultyHolder(String dungeonId) {
            this.dungeonId = dungeonId;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
