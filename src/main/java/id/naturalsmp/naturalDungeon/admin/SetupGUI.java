package id.naturalsmp.naturaldungeon.admin;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.dungeon.Dungeon;
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

public class SetupGUI implements Listener {

    private final NaturalDungeon plugin;
    private final SetupManager setupManager;

    public SetupGUI(NaturalDungeon plugin, SetupManager setupManager) {
        this.plugin = plugin;
        this.setupManager = setupManager;
    }

    public void open(Player player, Dungeon dungeon) {
        Inventory inv = Bukkit.createInventory(new SetupHolder(dungeon), 54,
                ChatUtils.colorize("&8Setup: &0" + dungeon.getDisplayName()));

        // Stages
        int slot = 10;
        int totalStages = dungeon.getTotalStages();
        // Allow creating up to existing + 1 (max 54 slots logic limits this but 10-16,
        // 19-25 etc pattern)
        // For simplicity, let's show existing stages + 1 "Add New" button if less than
        // max.

        // However, config structure is static in YAML usually.
        // For "Full Info", we iterate configured stages.

        for (Dungeon.Stage stage : dungeon.getStages()) {
            boolean isConfigured = !stage.getLocation(1).getSafeZone().isEmpty();
            // Note: We might need deeper check, but start with safezone presence.

            ItemStack icon = createItem(
                    isConfigured ? Material.LIME_STAINED_GLASS_PANE : Material.YELLOW_STAINED_GLASS_PANE,
                    "&aStage " + stage.getNumber(),
                    "",
                    "&7Safezone: " + (isConfigured ? "&aSet" : "&cNot Set"),
                    "&7Boss: " + (stage.hasBoss() ? "&aSet" : "&cNot Set"),
                    "",
                    "&eClick to Edit");
            inv.setItem(slot++, icon);
            if ((slot % 9) == 8)
                slot += 2; // Wrap around rows
        }

        // Add New Stage Button (Placeholder for now, or just rely on manual config for
        // adding stages)
        // Let's stick to EDITING existing stages for now to be safe with YAML
        // structure.

        // Back / Close
        inv.setItem(49, createItem(Material.BARRIER, "&cClose Setup"));

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof SetupHolder holder))
            return;
        e.setCancelled(true);
        if (e.getCurrentItem() == null)
            return;

        Player player = (Player) e.getWhoClicked();
        ItemStack item = e.getCurrentItem();

        if (item.getType() == Material.BARRIER) {
            setupManager.exitSetupMode(player);
            player.closeInventory();
        } else if (item.getType() == Material.LIME_STAINED_GLASS_PANE
                || item.getType() == Material.YELLOW_STAINED_GLASS_PANE) {
            String name = item.getItemMeta().getDisplayName();
            // Extract Stage Number from "&aStage 1"
            try {
                String numStr = ChatUtils.stripColor(name).replace("Stage ", "");
                int stageNum = Integer.parseInt(numStr);

                player.closeInventory();
                setupManager.enterStageEditor(player, holder.dungeon, stageNum);
            } catch (Exception ex) {
                player.sendMessage(ChatUtils.colorize("&cError parsing stage number."));
            }
        }
    }

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.colorize(name));
        List<String> loreList = new ArrayList<>();
        for (String l : lore)
            loreList.add(ChatUtils.colorize(l));
        meta.setLore(loreList);
        item.setItemMeta(meta);
        return item;
    }

    public static class SetupHolder implements InventoryHolder {
        private final Dungeon dungeon;

        public SetupHolder(Dungeon dungeon) {
            this.dungeon = dungeon;
        }

        public Dungeon getDungeon() {
            return dungeon;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
