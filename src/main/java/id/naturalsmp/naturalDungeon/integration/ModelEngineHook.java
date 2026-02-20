package id.naturalsmp.naturaldungeon.integration;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.lang.reflect.Method;

/**
 * Reflection-based hook for ModelEngine — no hard dependency required.
 * Supports ME:model_id:entity_type prefix in wave configs.
 */
public class ModelEngineHook {

    private final boolean enabled;
    private Method createModeledEntityMethod;
    private Method createActiveModelMethod;
    private Method addModelMethod;

    public ModelEngineHook(NaturalDungeon plugin) {
        boolean found = plugin.getServer().getPluginManager().isPluginEnabled("ModelEngine");
        if (found) {
            try {
                Class<?> apiClass = Class.forName("com.ticxo.modelengine.api.ModelEngineAPI");
                createModeledEntityMethod = apiClass.getMethod("createModeledEntity", Entity.class);
                createActiveModelMethod = apiClass.getMethod("createActiveModel", String.class);

                Class<?> modeledEntityClass = Class.forName("com.ticxo.modelengine.api.model.ModeledEntity");
                Class<?> activeModelClass = Class.forName("com.ticxo.modelengine.api.model.ActiveModel");
                addModelMethod = modeledEntityClass.getMethod("addModel", activeModelClass, boolean.class);

                plugin.getLogger().info("ModelEngine API loaded via reflection.");
            } catch (Exception e) {
                plugin.getLogger().warning("ModelEngine found but API reflection failed: " + e.getMessage());
                found = false;
            }
        }
        this.enabled = found;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Spawns a mob with a ModelEngine model.
     */
    public Entity spawnModelMob(String modelId, EntityType baseType, Location location) {
        if (!enabled)
            return null;

        Entity entity = location.getWorld().spawnEntity(location, baseType);
        if (entity instanceof LivingEntity) {
            applyModel(entity, modelId);
        }
        return entity;
    }

    /**
     * Apply a ModelEngine model to an existing entity via reflection.
     */
    public void applyModel(Entity entity, String modelId) {
        if (!enabled || createModeledEntityMethod == null)
            return;

        try {
            Object modeledEntity = createModeledEntityMethod.invoke(null, entity);
            Object activeModel = createActiveModelMethod.invoke(null, modelId);
            if (activeModel != null && modeledEntity != null) {
                addModelMethod.invoke(modeledEntity, activeModel, true);
            }
        } catch (Exception e) {
            // Silently fail — ModelEngine model might not exist
        }
    }
}
