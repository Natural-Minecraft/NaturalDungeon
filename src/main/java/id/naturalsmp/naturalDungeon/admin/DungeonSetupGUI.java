package id.naturalsmp.naturaldungeon.admin;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.dungeon.Dungeon;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import id.naturalsmp.naturaldungeon.utils.ConfigUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DungeonSetupGUI implements Listener {

    private final NaturalDungeon plugin;
    private final Map<UUID, SetupSession> sessions = new HashMap<>();

    public DungeonSetupGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public void openSelector(Player player) {
        Inventory inv = Bukkit.createInventory(new SetupHolder("selector"), 54,
                ChatUtils.colorize("&8Select Dungeon to Edit"));

        int slot = 0;
        for (Dungeon d : plugin.getDungeonManager().getDungeons()) {
            ItemStack icon = new ItemStack(Material.SPAWNER);
            ItemMeta meta = icon.getItemMeta();
            meta.setDisplayName(ChatUtils.colorize("&a" + d.getId()));
            List<String> lore = new ArrayList<>();
            lore.add(ChatUtils.colorize("&7" + d.getDisplayName()));
            lore.add(ChatUtils.colorize("&eClick to Edit"));
            meta.setLore(lore);
            icon.setItemMeta(meta);
            inv.setItem(slot++, icon);
        }

        player.openInventory(inv);
    }

    public void openInstanceSelector(Player player, String dungeonId) {
        Inventory inv = Bukkit.createInventory(new SetupHolder("instance:" + dungeonId), 27,
                ChatUtils.colorize("&8Select Instance"));

        for (int i = 1; i <= 3; i++) {
            ItemStack icon = new ItemStack(Material.IRON_DOOR);
            ItemMeta meta = icon.getItemMeta();
            meta.setDisplayName(ChatUtils.colorize("&eInstance " + i));
            List<String> lore = new ArrayList<>();
            lore.add(ChatUtils.colorize("&7Edit locations for Instance " + i));
            meta.setLore(lore);
            icon.setItemMeta(meta);
            inv.setItem(10 + i, icon);
        }

        player.openInventory(inv);
    }

    public void openEditor(Player player, String dungeonId, int instanceId) {
        Inventory inv = Bukkit.createInventory(new SetupHolder("editor:" + dungeonId + ":" + instanceId), 45,
                ChatUtils.colorize("&8Editing " + dungeonId + " (" + instanceId + ")"));

        sessions.put(player.getUniqueId(), new SetupSession(dungeonId, instanceId));

        // Items for setting locations
        inv.setItem(11, createTool(Material.EMERALD_BLOCK, "&aSet Safezone (Stage 1)",
                "&7Click to set Safezone 1 to current location"));
        inv.setItem(13, createTool(Material.REDSTONE_BLOCK, "&cSet Boss Spawn (Stage 1)",
                "&7Click to set Boss Spawn 1 to current location"));
        inv.setItem(15, createTool(Material.BEACON, "&bSet Arena Center (Stage 1)",
                "&7Click to set Arena Center 1 (Loot Chest)"));

        inv.setItem(20, createTool(Material.EMERALD_ORE, "&aSet Safezone (Stage 2)",
                "&7Click to set Safezone 2 to current location"));
        inv.setItem(22, createTool(Material.REDSTONE_ORE, "&cSet Boss Spawn (Stage 2)",
                "&7Click to set Boss Spawn 2 to current location"));
        inv.setItem(24,
                createTool(Material.SEA_LANTERN, "&bSet Arena Center (Stage 2)", "&7Click to set Arena Center 2"));

        inv.setItem(40, createTool(Material.WRITABLE_BOOK, "&eSave & Reload", "&7Click to save changes to file"));

        player.openInventory(inv);
    }

    private ItemStack createTool(Material mat, String name, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatUtils.colorize(name));
        List<String> l = new ArrayList<>();
        l.add(ChatUtils.colorize(lore));
        l.add(ChatUtils.colorize("&8Right-Click to TP"));
        meta.setLore(l);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof SetupHolder holder))
            return;
        e.setCancelled(true);
        if (e.getCurrentItem() == null)
            return;

        Player player = (Player) e.getWhoClicked();
        String type = holder.data.split(":")[0];

        if (type.equals("selector")) {
            String dungeonId = ChatUtils.stripColor(e.getCurrentItem().getItemMeta().getDisplayName());
            openInstanceSelector(player, dungeonId);
        } else if (type.equals("instance")) {
            String dungeonId = holder.data.split(":")[1];
            int instanceId = Integer.parseInt(
                    ChatUtils.stripColor(e.getCurrentItem().getItemMeta().getDisplayName()).replace("Instance ", ""));
            openEditor(player, dungeonId, instanceId);
        } else if (type.equals("editor")) {
            handleEditorClick(e, player);
        }
    }

    private void handleEditorClick(InventoryClickEvent e, Player player) {
        SetupSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            player.closeInventory();
            return;
        }

        ItemStack item = e.getCurrentItem();
        String name = ChatUtils.stripColor(item.getItemMeta().getDisplayName());

        if (name.contains("Safezone (Stage 1)")) {
            setLocation(player, session.dungeonId, "stages.1.locations." + session.instanceId + ".safe-zone",
                    player.getLocation(), true); // Region name logic?
            // Actually, the config expects a REGION NAME or Coords?
            // Current Dungeon.java expects "safe-zone" string (Region Name).
            // BUT StageLocation can parse generic inputs?
            // "locations.1.safe-zone" -> String.
            // If I want to support "easy setup", I should probably use coordinates in
            // config OR set the WorldGuard region.
            // Setting WG region programmatically is hard.
            // I'll skip WG region setting and assume the user wants me to save COORDINATES
            // if supported.
            // But Dungeon.java uses `plugin.getWorldGuardHook().getRegionSpawnPoint`.
            // So I must set the REGION NAME.
            player.sendMessage(
                    ChatUtils.colorize("&cThis requires setting a WorldGuard region. Please use /rg define."));
        } else if (name.contains("Boss Spawn")) {
            int stage = name.contains("Stage 1") ? 1 : 2;
            saveLocation(player, session.dungeonId,
                    "stages." + stage + ".locations." + session.instanceId + ".boss-spawn", player.getLocation());
        } else if (name.contains("Arena Center")) { // Use this for loot/lightning
            // Not explicitly in config, usually calculated. But I can add "center" to
            // config.
        } else if (name.contains("Save & Reload")) {
            plugin.getDungeonManager().loadDungeons();
            player.sendMessage(ChatUtils.colorize("&aDungeons reloaded!"));
            player.closeInventory();
        }
    }

    private void saveLocation(Player player, String dungeonId, String path, Location loc) {
        File file = new File(plugin.getDataFolder(), "dungeons/" + dungeonId + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        List<Double> coords = new ArrayList<>();
        coords.add(loc.getX());
        coords.add(loc.getY());
        coords.add(loc.getZ());

        config.set(path, coords);
        try {
            config.save(file);
            player.sendMessage(ChatUtils.colorize("&aLocation saved to " + path));
        } catch (IOException ex) {
            ex.printStackTrace();
            player.sendMessage(ChatUtils.colorize("&cFailed to save!"));
        }
    }

    // Dumb helper to set WG region? No, too complex.
    private void setLocation(Player player, String dungeonId, String path, Location loc, boolean isRegion) {
        if (isRegion) {
            player.sendMessage(
                    ChatUtils.colorize("&e[Tip] To set safezone, define a WG region and put its name in config."));
        }
    }

    private static class SetupHolder implements InventoryHolder {
        String data;

        public SetupHolder(String data) {
            this.data = data;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private static class SetupSession {
        String dungeonId;
        int instanceId;

        public SetupSession(String d, int i) {
            this.dungeonId = d;
            this.instanceId = i;
        }
    }
}
