package id.naturalsmp.naturaldungeon.integration;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import org.bukkit.inventory.ItemStack;

public class MMOItemsHook {

    private final NaturalDungeon plugin;

    public MMOItemsHook(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public ItemStack getItem(String typeName, String itemId) {
        try {
            Type type = MMOItems.plugin.getTypes().get(typeName);
            if (type == null) {
                plugin.getLogger().warning("MMOItems type not found: " + typeName);
                return null;
            }
            return MMOItems.plugin.getItem(type, itemId);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get MMOItems item: " + typeName + ":" + itemId);
            return null;
        }
    }

    public boolean hasItem(org.bukkit.entity.Player player, String type, String id, int amount) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && isMMOItem(item, type, id)) {
                count += item.getAmount();
            }
        }
        return count >= amount;
    }

    public void consumeItem(org.bukkit.entity.Player player, String type, String id, int amount) {
        int remaining = amount;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && isMMOItem(item, type, id)) {
                if (item.getAmount() > remaining) {
                    item.setAmount(item.getAmount() - remaining);
                    return;
                } else {
                    remaining -= item.getAmount();
                    player.getInventory().removeItem(item);
                }
            }
            if (remaining <= 0)
                break;
        }
    }

    private boolean isMMOItem(ItemStack item, String type, String id) {
        Type itemType = MMOItems.getType(item);
        if (itemType == null || !itemType.getId().equals(type))
            return false;
        String itemId = MMOItems.getID(item);
        return itemId != null && itemId.equals(id);
    }
}
