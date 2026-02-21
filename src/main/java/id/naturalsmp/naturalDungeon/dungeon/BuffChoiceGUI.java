package id.naturalsmp.naturaldungeon.dungeon;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class BuffChoiceGUI implements Listener {

    private final NaturalDungeon plugin;
    private final Map<UUID, PotionEffectType> chosenBuffs = new HashMap<>();

    public BuffChoiceGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, DungeonInstance instance) {
        Inventory inv = GUIUtils.createGUI(new BuffHolder(instance), 27,
                "&#AA44FF⚡ ᴘɪʟɪʜ ʙᴜꜰꜰ ᴅᴜɴɢᴇᴏɴ ⚡");

        GUIUtils.fillAll(inv, Material.GRAY_STAINED_GLASS_PANE);

        // ─── Buff Options ───

        // Strength (slot 11)
        inv.setItem(11, GUIUtils.createItem(Material.IRON_SWORD,
                "&#FF4444&l⚔ ꜱᴛʀᴇɴɢᴛʜ",
                GUIUtils.separator(),
                "&7Meningkatkan damage serangan",
                "&7sebesar &c+30%&7.",
                "",
                "&7Durasi: &f1 Stage",
                "",
                "&#FFAA00&l➥ KLIK UNTUK PILIH"));

        // Regeneration (slot 13)
        inv.setItem(13, GUIUtils.createItem(Material.GOLDEN_APPLE,
                "&#FF69B4&l💚 ʀᴇɢᴇɴᴇʀᴀᴛɪᴏɴ",
                GUIUtils.separator(),
                "&7Regenerasi HP otomatis",
                "&7setiap &a3 detik&7.",
                "",
                "&7Durasi: &f1 Stage",
                "",
                "&#FFAA00&l➥ KLIK UNTUK PILIH"));

        // Speed (slot 15)
        inv.setItem(15, GUIUtils.createItem(Material.SUGAR,
                "&#55CCFF&l💨 ꜱᴘᴇᴇᴅ",
                GUIUtils.separator(),
                "&7Meningkatkan kecepatan gerak",
                "&7sebesar &b+20%&7.",
                "",
                "&7Durasi: &f1 Stage",
                "",
                "&#FFAA00&l➥ KLIK UNTUK PILIH"));

        player.openInventory(inv);
        GUIUtils.playOpenSound(player);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof BuffHolder holder))
            return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getView().getTopInventory())
            return;

        ItemStack item = e.getCurrentItem();
        if (item == null || item.getType() == Material.GRAY_STAINED_GLASS_PANE)
            return;

        Player player = (Player) e.getWhoClicked();
        GUIUtils.playClickSound(player);

        PotionEffectType buff = switch (e.getSlot()) {
            case 11 -> PotionEffectType.STRENGTH;
            case 13 -> PotionEffectType.REGENERATION;
            case 15 -> PotionEffectType.SPEED;
            default -> null;
        };

        if (buff != null) {
            chosenBuffs.put(player.getUniqueId(), buff);
            applyBuff(player, buff);
            player.closeInventory();
            GUIUtils.playSuccessSound(player);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof BuffHolder)
            e.setCancelled(true);
    }

    public void applyBuff(Player player, PotionEffectType type) {
        int amplifier = type == PotionEffectType.STRENGTH ? 0 : 0;
        player.addPotionEffect(new PotionEffect(type, 20 * 60 * 5, amplifier, true, true));
    }

    public void reapplyBuff(Player player) {
        PotionEffectType type = chosenBuffs.get(player.getUniqueId());
        if (type != null) {
            applyBuff(player, type);
        }
    }

    public void clearBuffs(List<UUID> players) {
        for (UUID uuid : players) {
            chosenBuffs.remove(uuid);
        }
    }

    public static class BuffHolder implements InventoryHolder {
        private final DungeonInstance instance;

        public BuffHolder(DungeonInstance instance) {
            this.instance = instance;
        }

        public DungeonInstance getInstance() {
            return instance;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
