package id.naturalsmp.naturaldungeon.editor;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import id.naturalsmp.naturaldungeon.utils.GUIUtils;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.*;

/**
 * Multi-Phase Boss Designer — configure boss phases with HP thresholds and
 * skills.
 * Per vision_admin_experience.md §4 and vision_naturaldungeon.md §5
 *
 * Config path: stages.{X}.boss.phases.{N}.hp-threshold / skills
 */
public class BossDesignerGUI implements Listener {

    private final NaturalDungeon plugin;

    // Pre-built skill library
    private static final String[][] SKILL_LIBRARY = {
            { "MELEE_BOOST", "🗡 Melee Boost", "Bonus melee damage" },
            { "SUMMON_ADDS", "🧟 Summon Adds", "Summon minions periodically" },
            { "SHOCKWAVE", "💥 Shockwave", "Radial knockback + damage" },
            { "SHIELD_PHASE", "🛡 Shield Phase", "Immune + shield crystals" },
            { "FIRE_AURA", "🔥 Fire Aura", "DoT fire damage nearby" },
            { "LIGHTNING", "⚡ Lightning", "Strike random player" },
            { "LASER_BEAM", "🔴 Laser Beam", "Delayed line damage" },
            { "ARENA_SHRINK", "💀 Arena Shrink", "Shrink arena boundaries" },
            { "TELEPORT", "🌀 Teleport", "Random teleport nearby" },
            { "REGEN", "💚 Regeneration", "Heal over time" },
    };

    public BossDesignerGUI(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String dungeonId, int stageIndex) {
        Inventory inv = GUIUtils.createGUI(new DesignerHolder(dungeonId, stageIndex), 54,
                "&#AA44FF🐉 ʙᴏꜱꜱ ᴅᴇꜱɪɢɴᴇʀ — Stage " + (stageIndex + 1));

        GUIUtils.fillBorder(inv, Material.BLACK_STAINED_GLASS_PANE);
        GUIUtils.fillEmpty(inv, Material.GRAY_STAINED_GLASS_PANE);

        String basePath = "stages." + (stageIndex + 1) + ".boss.";
        YamlConfiguration config = plugin.getDungeonManager().loadDungeonConfig(dungeonId);

        String bossId = config.getString(basePath + "mob", "Not Set");

        // ─── Basic Info (row 1) ───
        inv.setItem(2, GUIUtils.createItem(Material.WITHER_SKELETON_SKULL,
                "&#FF5555&l🐉 ʙᴏꜱꜱ: &f" + bossId,
                GUIUtils.separator(),
                "&7Klik untuk ganti boss mob.",
                "",
                "&#FFAA00&l➥ KLIK"));

        double hpMult = config.getDouble(basePath + "hp-multiplier", 1.0);
        inv.setItem(4, GUIUtils.createItem(Material.REDSTONE,
                "&#FF5555&l❤ HP: &f" + hpMult + "x",
                GUIUtils.separator(),
                "&7L-Click: +0.5x, R-Click: -0.5x"));

        inv.setItem(6, GUIUtils.createItem(Material.COMPASS,
                "&#55CCFF&l📍 ꜱᴘᴀᴡɴ ʟᴏᴄᴀᴛɪᴏɴ",
                GUIUtils.separator(),
                "&7Set spawn dari posisi kamu.",
                "",
                "&#FFAA00&l➥ KLIK"));

        // ─── Phase Timeline (row 2-3, slots 19-25) ───
        List<Map<?, ?>> phases = config.getMapList(basePath + "phases");
        if (phases.isEmpty()) {
            // Show empty + add phase
            inv.setItem(20, GUIUtils.createItem(Material.GRAY_DYE,
                    "&7&lTidak ada phase",
                    GUIUtils.separator(),
                    "&7Klik &f[+ Phase] &7di bawah."));
        } else {
            for (int p = 0; p < Math.min(phases.size(), 5); p++) {
                Map<?, ?> phase = phases.get(p);
                int threshold = phase.get("hp-threshold") != null ? ((Number) phase.get("hp-threshold")).intValue()
                        : 100;
                List<?> skills = phase.get("skills") instanceof List<?> sl ? sl : List.of();
                String phaseColor = p == 0 ? "&#55FF55" : p == phases.size() - 1 ? "&#FF0000" : "&#FFAA00";

                List<String> lore = new ArrayList<>();
                lore.add(GUIUtils.separator());
                lore.add("&7HP Threshold: " + phaseColor + threshold + "%");
                lore.add("&7Skills: &f" + skills.size());
                for (Object skill : skills) {
                    lore.add("  &7• &#AA44FF" + skill.toString());
                }
                lore.add("");
                lore.add("&#FF5555&lShift+Click → Hapus");
                lore.add("&#FFAA00&l➥ KLIK → Edit");

                inv.setItem(20 + p, GUIUtils.createItem(
                        p == 0 ? Material.LIME_CONCRETE
                                : p == phases.size() - 1 ? Material.RED_CONCRETE : Material.YELLOW_CONCRETE,
                        phaseColor + "&lPhase " + (p + 1) + " [" + threshold + "%]", lore));
            }
        }

        // ─── Skill Library (row 4, slots 37-43) ───
        for (int s = 0; s < Math.min(SKILL_LIBRARY.length, 7); s++) {
            inv.setItem(37 + s, GUIUtils.createItem(Material.ENCHANTED_BOOK,
                    "&#AA44FF&l" + SKILL_LIBRARY[s][1],
                    GUIUtils.separator(),
                    "&7" + SKILL_LIBRARY[s][2],
                    "",
                    "&7ID: &f" + SKILL_LIBRARY[s][0]));
        }

        // ─── Bottom Actions ───
        inv.setItem(45, GUIUtils.createItem(Material.ARROW, "&#FF5555&l← ᴋᴇᴍʙᴀʟɪ"));

        inv.setItem(49, GUIUtils.createItem(Material.EMERALD,
                "&#55FF55&l✚ ᴀᴅᴅ ᴘʜᴀꜱᴇ",
                GUIUtils.separator(),
                "&7Tambah phase baru.",
                "",
                "&#FFAA00&l➥ KLIK"));

        inv.setItem(53, GUIUtils.createItem(Material.ENDER_EYE,
                "&#55FF55&l▶ ᴛᴇꜱᴛ ʙᴏꜱꜱ",
                GUIUtils.separator(),
                "&7Spawn boss untuk preview.",
                "",
                "&#FFAA00&l➥ KLIK"));

        player.openInventory(inv);
        GUIUtils.playOpenSound(player);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof DesignerHolder holder))
            return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getView().getTopInventory())
            return;
        if (e.getCurrentItem() == null)
            return;

        Player player = (Player) e.getWhoClicked();
        GUIUtils.playClickSound(player);
        String basePath = "stages." + (holder.stageIndex + 1) + ".boss.";
        YamlConfiguration config = plugin.getDungeonManager().loadDungeonConfig(holder.dungeonId);

        switch (e.getSlot()) {
            case 2 -> {
                // Change boss mob — use MobPicker
                new MobPickerGUI(plugin).open(player, mobId -> {
                    plugin.getDungeonManager().setDungeonConfig(holder.dungeonId, basePath + "mob", mobId);
                    player.sendMessage(ChatUtils.colorize("&#55FF55✔ Boss: &f" + mobId));
                    GUIUtils.playSuccessSound(player);
                    open(player, holder.dungeonId, holder.stageIndex);
                });
            }
            case 4 -> {
                // HP multiplier +/- 0.5
                double hp = config.getDouble(basePath + "hp-multiplier", 1.0);
                hp = Math.max(0.5, hp + (e.isLeftClick() ? 0.5 : -0.5));
                plugin.getDungeonManager().setDungeonConfig(holder.dungeonId, basePath + "hp-multiplier", hp);
                open(player, holder.dungeonId, holder.stageIndex);
            }
            case 6 -> {
                // Set spawn
                player.closeInventory();
                org.bukkit.Location loc = player.getLocation();
                List<Double> locList = Arrays.asList(loc.getX(), loc.getY(), loc.getZ());
                plugin.getDungeonManager().setDungeonConfig(holder.dungeonId,
                        "stages." + (holder.stageIndex + 1) + ".boss-spawn", locList);
                player.sendMessage(ChatUtils.colorize("&#55FF55✔ Boss spawn: &f" +
                        loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ()));
                GUIUtils.playSuccessSound(player);
            }
            case 45 -> new WaveEditorGUI(plugin).open(player, holder.dungeonId, holder.stageIndex);
            case 49 -> {
                // Add phase
                List<Map<?, ?>> phases = config.getMapList(basePath + "phases");
                int threshold = phases.isEmpty() ? 100
                        : Math.max(0, ((Number) phases.get(phases.size() - 1).get("hp-threshold")).intValue() - 30);
                Map<String, Object> newPhase = new LinkedHashMap<>();
                newPhase.put("hp-threshold", threshold);
                newPhase.put("skills", new ArrayList<String>());
                phases.add(newPhase);
                plugin.getDungeonManager().setDungeonConfig(holder.dungeonId, basePath + "phases", phases);
                player.sendMessage(ChatUtils.colorize("&#55FF55✔ Phase ditambahkan! HP: " + threshold + "%"));
                GUIUtils.playSuccessSound(player);
                open(player, holder.dungeonId, holder.stageIndex);
            }
            case 53 -> {
                // Test boss — spawn it
                player.closeInventory();
                String bossId = config.getString(basePath + "mob");
                if (bossId == null || bossId.isEmpty()) {
                    player.sendMessage(ChatUtils.colorize("&#FF5555✖ &7Boss mob belum di-set!"));
                    GUIUtils.playErrorSound(player);
                    return;
                }
                player.performCommand("nd debug bossspawn " + bossId);
            }
            default -> {
                // Phase slots (20-24)
                if (e.getSlot() >= 20 && e.getSlot() <= 24) {
                    int phaseIdx = e.getSlot() - 20;
                    List<Map<?, ?>> phases = config.getMapList(basePath + "phases");
                    if (phaseIdx < phases.size()) {
                        if (e.isShiftClick()) {
                            // Delete phase
                            phases.remove(phaseIdx);
                            plugin.getDungeonManager().setDungeonConfig(holder.dungeonId, basePath + "phases", phases);
                            player.sendMessage(ChatUtils.colorize("&#FF5555✖ Phase " + (phaseIdx + 1) + " dihapus!"));
                            open(player, holder.dungeonId, holder.stageIndex);
                        } else {
                            // Add skill to phase
                            openSkillPicker(player, holder, phaseIdx);
                        }
                    }
                }
            }
        }
    }

    private void openSkillPicker(Player player, DesignerHolder holder, int phaseIdx) {
        player.closeInventory();
        StringBuilder sb = new StringBuilder();
        sb.append(ChatUtils.colorize("&#AA44FF&l⚔ Skill Library:\n"));
        for (String[] skill : SKILL_LIBRARY) {
            sb.append(ChatUtils.colorize("  &7• &#AA44FF" + skill[0] + " &7— " + skill[2] + "\n"));
        }
        sb.append(ChatUtils.colorize("&#FFAA00&l📝 &7Ketik skill ID:"));
        player.sendMessage(sb.toString());

        plugin.getEditorChatInput().requestInput(player,
                ChatUtils.colorize("&#AA44FF&l⚔ &7Ketik skill ID (e.g. SHOCKWAVE):"), input -> {
                    String basePath = "stages." + (holder.stageIndex + 1) + ".boss.";
                    YamlConfiguration config = plugin.getDungeonManager().loadDungeonConfig(holder.dungeonId);
                    List<Map<?, ?>> phases = config.getMapList(basePath + "phases");
                    if (phaseIdx < phases.size()) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> phase = (Map<String, Object>) phases.get(phaseIdx);
                        @SuppressWarnings("unchecked")
                        List<String> skills = phase.get("skills") instanceof List<?> sl
                                ? new ArrayList<>((List<String>) sl)
                                : new ArrayList<>();
                        skills.add(input.toUpperCase());
                        phase.put("skills", skills);
                        plugin.getDungeonManager().setDungeonConfig(holder.dungeonId, basePath + "phases", phases);
                        player.sendMessage(ChatUtils.colorize("&#55FF55✔ Skill &f" + input.toUpperCase() +
                                " &#55FF55ditambahkan ke Phase " + (phaseIdx + 1)));
                        GUIUtils.playSuccessSound(player);
                    }
                    open(player, holder.dungeonId, holder.stageIndex);
                });
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof DesignerHolder)
            e.setCancelled(true);
    }

    public static class DesignerHolder implements InventoryHolder {
        public final String dungeonId;
        public final int stageIndex;

        public DesignerHolder(String d, int s) {
            this.dungeonId = d;
            this.stageIndex = s;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
