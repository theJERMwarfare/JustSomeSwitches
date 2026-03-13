package net.justsomeswitches.client.model;

import net.justsomeswitches.blockentity.SwitchBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Bridge between ghost detection and model rendering with ModelData caching. */
public class GhostModelDataProvider {

    private static GhostModelDataProvider instance;
    private final Map<String, ModelData> ghostModelDataCache = new ConcurrentHashMap<>();
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

    /** Updates the ghost preview with new position, state, and orientation. */
    public void updateGhostPreview(@Nonnull BlockPos pos, @Nonnull BlockState state, @Nonnull String wallOrientation) {
        activeGhostPos = pos;
        activeGhostModelData = createGhostModelData(state, wallOrientation);
    }

    /** Clears the current ghost preview. */
    public void clearGhostPreview() {
        activeGhostPos = null;
        activeGhostModelData = null;
    }

    /** Gets the ModelData for ghost rendering at the specified position or null if no active ghost. */
    @Nullable
    public ModelData getGhostModelData(@Nonnull BlockPos pos) {
        if (activeGhostPos != null && activeGhostPos.equals(pos)) {
            return activeGhostModelData;
        }
        return null;
    }

    /** Creates ModelData for ghost preview rendering with default textures only. */
    @Nonnull
    private ModelData createGhostModelData(@Nonnull BlockState state, @Nonnull String wallOrientation) {
        String cacheKey = createCacheKey(state, wallOrientation);
        return ghostModelDataCache.computeIfAbsent(cacheKey, key -> {
            ModelData ghostData = ModelData.builder()
                .with(SwitchBlockEntity.GHOST_MODE, true)
                .with(SwitchBlockEntity.GHOST_ALPHA, 0.75f)
                .with(SwitchBlockEntity.GHOST_STATE, state)
                .with(SwitchBlockEntity.WALL_ORIENTATION, wallOrientation)
                .with(SwitchBlockEntity.TOGGLE_TEXTURE, SwitchBlockEntity.DEFAULT_TOGGLE_TEXTURE)
                .with(SwitchBlockEntity.BASE_TEXTURE, SwitchBlockEntity.DEFAULT_BASE_TEXTURE)
                .with(SwitchBlockEntity.FACE_SELECTION, "all,all")
                .with(SwitchBlockEntity.POWER_MODE, "DEFAULT")
                .with(SwitchBlockEntity.BASE_ROTATION, "NORMAL")
                .with(SwitchBlockEntity.TOGGLE_ROTATION, "NORMAL")
                .with(SwitchBlockEntity.INVERTED, false)
                .build();
            if (ghostModelDataCache.size() > 100) {
                cleanupCache();
            }
            return ghostData;
        });
    }

    /** Creates a cache key for ghost ModelData based on state and orientation. */
    @Nonnull
    private String createCacheKey(@Nonnull BlockState state, @Nonnull String wallOrientation) {
        return state + "_" + wallOrientation;
    }

    /** Cleans up the ModelData cache to prevent memory leaks. */
    private void cleanupCache() {
        if (ghostModelDataCache.size() > 50) {
            ghostModelDataCache.clear();
        }
    }
}
