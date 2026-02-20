package id.naturalsmp.naturaldungeon.vfx;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import id.naturalsmp.naturaldungeon.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

/**
 * Spawns floating damage numbers above mobs when hit.
 * Uses TextDisplay entities for modern 1.19.4+ visuals.
 */
public class DamageNumberManager {

    private final NaturalDungeon plugin;

    public DamageNumberManager(NaturalDungeon plugin) {
        this.plugin = plugin;
    }

    /**
     * Spawn a floating damage number at the given location.
     * 
     * @param location   Where to spawn the number
     * @param damage     Amount of damage dealt
     * @param isCritical Whether it was a critical hit
     */
    public void spawnDamageNumber(Location location, double damage, boolean isCritical) {
        if (location.getWorld() == null)
            return;

        // Offset slightly random to prevent overlap
        double offsetX = (Math.random() - 0.5) * 0.8;
        double offsetY = 1.5 + Math.random() * 0.5;
        double offsetZ = (Math.random() - 0.5) * 0.8;

        Location spawnLoc = location.clone().add(offsetX, offsetY, offsetZ);

        spawnLoc.getWorld().spawn(spawnLoc, TextDisplay.class, display -> {
            // Format text
            String text;
            if (isCritical) {
                text = ChatUtils.colorize("&#FFAA00&l⚡ CRIT -" + (int) damage);
            } else {
                text = ChatUtils.colorize(getDamageColor(damage) + "-" + (int) damage + " ❤");
            }

            display.setText(text);
            display.setBillboard(Display.Billboard.CENTER);
            display.setSeeThrough(false);
            display.setShadowed(true);
            display.setDefaultBackground(false);
            display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0)); // Transparent background

            // Scale based on damage (bigger numbers for bigger hits)
            float scale = isCritical ? 1.8f : Math.min(1.0f + (float) damage / 40f, 1.5f);
            display.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 0, 1),
                    new Vector3f(scale, scale, scale),
                    new AxisAngle4f(0, 0, 0, 1)));

            // Float upward animation via interpolation
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!display.isValid())
                    return;
                display.setInterpolationDelay(0);
                display.setInterpolationDuration(15); // 15 ticks = 0.75s
                display.setTransformation(new Transformation(
                        new Vector3f(0, 1.5f, 0), // Float up
                        new AxisAngle4f(0, 0, 0, 1),
                        new Vector3f(scale * 0.5f, scale * 0.5f, scale * 0.5f), // Shrink
                        new AxisAngle4f(0, 0, 0, 1)));
            }, 2L);

            // Remove after animation
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (display.isValid())
                    display.remove();
            }, 20L); // 1 second
        });
    }

    private String getDamageColor(double damage) {
        if (damage >= 15)
            return "&c"; // Red for high damage
        if (damage >= 8)
            return "&#FF6600"; // Orange
        if (damage >= 4)
            return "&e"; // Yellow
        return "&f"; // White for low
    }
}
