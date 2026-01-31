package id.naturalsmp.naturaldungeon.integration;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

public class AuraMobsHook {

    private final NaturalDungeon plugin;
    private boolean enabled = false;
    private Object auraMobsApi;
    private Method setLevelMethod;

    public AuraMobsHook(NaturalDungeon plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        Plugin auraMobs = Bukkit.getPluginManager().getPlugin("AuraMobs");
        if (auraMobs == null)
            return;

        try {
            Class<?> apiClass;
            try {
                apiClass = Class.forName("dev.aurelium.auramobs.api.AuraMobsAPI");
            } catch (ClassNotFoundException e) {
                apiClass = Class.forName("dev.aurelium.auramobs.AuraMobsAPI");
            }

            // Try to find the singleton instance
            Method getMethod;
            try {
                getMethod = apiClass.getMethod("get");
            } catch (NoSuchMethodException e) {
                try {
                    getMethod = apiClass.getMethod("getInstance");
                } catch (NoSuchMethodException e2) {
                    getMethod = apiClass.getMethod("instance");
                }
            }
            this.auraMobsApi = getMethod.invoke(null);

            // Attempt to find setLevel or setMobLevel
            try {
                this.setLevelMethod = apiClass.getMethod("setMobLevel", Entity.class, int.class);
            } catch (NoSuchMethodException e) {
                try {
                    this.setLevelMethod = apiClass.getMethod("setLevel", Entity.class, int.class);
                } catch (NoSuchMethodException e2) {
                    // Try to find any method with (Entity, int) signature
                    for (Method m : apiClass.getMethods()) {
                        if (m.getParameterCount() == 2 && m.getParameterTypes()[0].equals(Entity.class)
                                && m.getParameterTypes()[1].equals(int.class)) {
                            this.setLevelMethod = m;
                            break;
                        }
                    }
                }
            }

            if (this.auraMobsApi != null && this.setLevelMethod != null) {
                this.enabled = true;
                plugin.getLogger().info("AuraMobs hooked for advanced mob leveling!");
            } else {
                plugin.getLogger().warning("AuraMobs found but API methods not resolved correctly.");
            }
        } catch (Exception e) {
            plugin.getLogger().warning(
                    "AuraMobs found but failed to hook API: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setLevel(Entity entity, int level) {
        if (!enabled || auraMobsApi == null || setLevelMethod == null)
            return;
        try {
            setLevelMethod.invoke(auraMobsApi, entity, level);
        } catch (Exception e) {
            // Silently fail or log once
        }
    }
}
