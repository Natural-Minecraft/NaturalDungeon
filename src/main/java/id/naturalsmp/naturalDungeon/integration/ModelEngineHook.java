package id.naturalsmp.naturaldungeon.integration;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import id.naturalsmp.naturaldungeon.NaturalDungeon;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

/**
 * Hook for ModelEngine integration.
 */
public class ModelEngineHook {

    private final boolean enabled;

    public ModelEngineHook(NaturalDungeon plugin) {
        this.enabled = plugin.getServer().getPluginManager().isPluginEnabled("ModelEngine");
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Spawns a mob with a ModelEngine model.
     * Format: ME:model_id:entity_type
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

    public void applyModel(Entity entity, String modelId) {
        if (!enabled)
            return;

        ModeledEntity modeledEntity = ModelEngineAPI.createModeledEntity(entity);
        ActiveModel activeModel = ModelEngineAPI.createActiveModel(modelId);
        if (activeModel != null) {
            modeledEntity.addModel(activeModel, true);
        }
    }
}
