package net.justsomeswitches.client.model;

import net.justsomeswitches.blockentity.SwitchesLeverBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bridge between ghost detection and model rendering.*
 * 
 * This class handles:
 * - Creating ModelData for ghost rendering
 * - Including GHOST_MODE=true, GHOST_ALPHA=0.75f
 * - Including calculated BlockState and wall orientation
 * - Bypassing texture customization for ghost preview (shows default lever only)
 * - Minimizing ModelData creation overhead with caching
 */
public class GhostModelDataProvider {
    
    private static GhostModelDataProvider instance;
    
    // Cache for ghost ModelData to minimize creation overhead
    private final Map<String, ModelData> ghostModelDataCache = new ConcurrentHashMap<>();
    
    // Current active ghost preview data
    private BlockPos activeGhostPos = null;
    private ModelData activeGhostModelData = null;
    
    private GhostModelDataProvider() {}
    
    @Nonnull
    public static GhostModelDataProvider getInstance() {
        if (instance == null) {
            instance = new GhostModelDataProvider();
        }
        return instance;
    }
    
    /**
     * Updates the ghost preview with new position, state, and orientation.
     */
    public void updateGhostPreview(@Nonnull BlockPos pos, @Nonnull BlockState state, @Nonnull String wallOrientation) {
        activeGhostPos = pos;
        activeGhostModelData = createGhostModelData(state, wallOrientation);
    }
    
    /**
     * Clears the current ghost preview.
     */
    public void clearGhostPreview() {
        activeGhostPos = null;
        activeGhostModelData = null;
    }
    
    /**
     * Gets the ModelData for ghost rendering at the specified position.
     * Returns null if no ghost preview is active at that position.
     */
    @Nullable
    public ModelData getGhostModelData(@Nonnull BlockPos pos) {
        if (activeGhostPos != null && activeGhostPos.equals(pos)) {
            return activeGhostModelData;
        }
        return null;
    }
    
    /**
     * Creates ModelData specifically for ghost preview rendering.
     * Ghost preview always shows default lever appearance with no texture customization.
     */
    @Nonnull
    private ModelData createGhostModelData(@Nonnull BlockState state, @Nonnull String wallOrientation) {
        String cacheKey = createCacheKey(state, wallOrientation);
        
        return ghostModelDataCache.computeIfAbsent(cacheKey, key -> {
            ModelData ghostData = ModelData.builder()
                // Ghost preview properties
                .with(SwitchesLeverBlockEntity.GHOST_MODE, true)
                .with(SwitchesLeverBlockEntity.GHOST_ALPHA, 0.75f)
                .with(SwitchesLeverBlockEntity.GHOST_STATE, state)
                .with(SwitchesLeverBlockEntity.WALL_ORIENTATION, wallOrientation)
                
                // Default textures only (no customization for ghost preview)
                .with(SwitchesLeverBlockEntity.TOGGLE_TEXTURE, SwitchesLeverBlockEntity.DEFAULT_TOGGLE_TEXTURE)
                .with(SwitchesLeverBlockEntity.BASE_TEXTURE, SwitchesLeverBlockEntity.DEFAULT_BASE_TEXTURE)
                .with(SwitchesLeverBlockEntity.FACE_SELECTION, "all,all")
                .with(SwitchesLeverBlockEntity.POWER_MODE, "DEFAULT")
                .with(SwitchesLeverBlockEntity.BASE_ROTATION, "NORMAL")
                .with(SwitchesLeverBlockEntity.TOGGLE_ROTATION, "NORMAL")
                .with(SwitchesLeverBlockEntity.INVERTED, false)
                .build();
            
            // Automatic cache cleanup when size exceeds limit
            if (ghostModelDataCache.size() > 100) {
                cleanupCache();
            }
            
            return ghostData;
        });
    }
    
    /**
     * Creates a cache key for ghost ModelData based on state and orientation.
     */
    @Nonnull
    private String createCacheKey(@Nonnull BlockState state, @Nonnull String wallOrientation) {
        // Use state's toString for reliable hashing, combined with wall orientation
        return state + "_" + wallOrientation;
    }
    
    /**
     * Cleans up the ModelData cache to prevent memory leaks.
     */
    private void cleanupCache() {
        if (ghostModelDataCache.size() > 50) {
            ghostModelDataCache.clear(); // Simple cleanup - clear cache when it gets too large
        }
    }
    
    
}
