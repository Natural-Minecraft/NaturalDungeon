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
            Class<?> apiClass = Class.forName("dev.aurelium.auramobs.api.AuraMobsAPI");
            Method getMethod = apiClass.getMethod("get");
            this.auraMobsApi = getMethod.invoke(null);

            // Attempt to find setLevel or setMobLevel
            try {
                this.setLevelMethod = apiClass.getMethod("setMobLevel", Entity.class, int.class);
            } catch (NoSuchMethodException e) {
                this.setLevelMethod = apiClass.getMethod("setLevel", Entity.class, int.class);
            }

            this.enabled = true;
            plugin.getLogger().info("AuraMobs hooked for advanced mob leveling!");
        } catch (Exception e) {
            plugin.getLogger().warning("AuraMobs found but failed to hook API: " + e.getMessage());
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
