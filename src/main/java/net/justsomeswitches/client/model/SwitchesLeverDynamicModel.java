package net.justsomeswitches.client.model;

import net.justsomeswitches.blockentity.SwitchesLeverBlockEntity;
import net.justsomeswitches.blockentity.tinting.OverlayLayer;
import net.justsomeswitches.util.TextureRotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** Dynamic model for lever blocks with custom texture support. */
public class SwitchesLeverDynamicModel implements IDynamicBakedModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(SwitchesLeverDynamicModel.class);
    
    /** Pre-computed wall orientation matrices for common rotations. Keys are "orientation_direction" like "top_north". */
    private static final Map<String, Matrix4f> WALL_ROTATION_CACHE = new HashMap<>();
    
    static {
        // Pre-compute rotation matrices for all wall orientations and directions
        for (String orientation : new String[]{"top", "left", "right", "bottom"}) {
            for (Direction direction : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                int degrees;
                switch (orientation) {
                    case "top": degrees = 180; break;
                    case "left": degrees = 90; break;
                    case "right": degrees = -90; break;
                    default: continue; // Skip bottom and center
                }
                
                float radians = (float) Math.toRadians(degrees);
                Matrix4f matrix = new Matrix4f().identity();
                
                switch (direction) {
                    case NORTH:
                    case SOUTH:
                        matrix.rotateZ(radians);
                        break;
                    case EAST:
                    case WEST:
                        matrix.rotateX(-radians);
                        break;
                }
                
                String key = orientation + "_" + direction.getName();
                WALL_ROTATION_CACHE.put(key, matrix);
            }
        }
    }
    
    /**
     * Checks if block is wall-mounted.
     */
    private boolean isWallPlacement(@Nonnull BlockState state) {
        try {
            net.minecraft.world.level.block.state.properties.AttachFace attachFace = 
                state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.ATTACH_FACE);
            return attachFace == net.minecraft.world.level.block.state.properties.AttachFace.WALL;
        } catch (Exception e) {
            return false;
        }
    }
    /**
     * Applies wall orientation rotations.
     */
    @Nonnull
    private List<BakedQuad> applyWallOrientationRotations(@Nonnull List<BakedQuad> baseQuads, 
                                                          @Nonnull BlockState state, 
                                                          @Nonnull ModelData extraData) {
        
        String wallOrientation = getWallOrientationFromBlockEntity(extraData);
        
        if ("center".equals(wallOrientation)) {
            return baseQuads;
        }
        
        net.minecraft.core.Direction wallFace;
        try {
            wallFace = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
        } catch (Exception e) {
            return baseQuads;
        }
        
        Matrix4f wallOrientationMatrix = createWallOrientationMatrix(wallOrientation, wallFace);
        
        if (wallOrientationMatrix.equals(new Matrix4f().identity())) {
            return baseQuads;
        }
        
        List<BakedQuad> rotatedQuads = new ArrayList<>();
        for (BakedQuad quad : baseQuads) {
            BakedQuad rotatedQuad = transformQuadWithMatrix(quad, wallOrientationMatrix);
            rotatedQuads.add(rotatedQuad);
        }
        
        return rotatedQuads;
    }
    /**
     * Gets wall orientation from model data.
     */
    @Nonnull
    private String getWallOrientationFromBlockEntity(@Nonnull ModelData extraData) {
        try {
            String wallOrientation = extraData.get(SwitchesLeverBlockEntity.WALL_ORIENTATION);
            if (wallOrientation != null && !wallOrientation.isEmpty()) {
                return wallOrientation;
            }
        } catch (Exception e) {
            // Silently handle errors
        }
        
        return "center";
    }
    /**
     * Creates rotation matrix for wall orientation. Uses pre-computed cache for common orientations.
     */
    @Nonnull
    private Matrix4f createWallOrientationMatrix(@Nonnull String wallOrientation, 
                                                 @Nonnull net.minecraft.core.Direction wallFace) {
        
        // Check cache for pre-computed matrices
        String cacheKey = wallOrientation + "_" + wallFace.getName();
        Matrix4f cached = WALL_ROTATION_CACHE.get(cacheKey);
        if (cached != null) {
            return new Matrix4f(cached); // Return copy to avoid mutation
        }
        
        // Fallback for center and bottom orientations (identity matrix)
        Matrix4f matrix = new Matrix4f().identity();

        int degrees;
        switch (wallOrientation) {
            case "top":
                degrees = 180;
                break;
            case "left":
                degrees = 90;
                break;
            case "right":
                degrees = -90;
                break;
            case "bottom":
            case "center":
            default:
                return matrix;
        }
        
        float radians = (float) Math.toRadians(degrees);

        switch (wallFace) {
            case NORTH:
            case SOUTH:

                matrix.rotateZ(radians);
                break;
            case EAST:
            case WEST:

                matrix.rotateX(-radians);
                break;
        }
        
        return matrix;
    }
    /**
     * Transforms quad using rotation matrix.
     */
    @Nonnull
    private BakedQuad transformQuadWithMatrix(@Nonnull BakedQuad originalQuad, @Nonnull Matrix4f matrix) {
        int[] originalVertices = originalQuad.getVertices();
        int[] transformedVertices = new int[originalVertices.length];
        System.arraycopy(originalVertices, 0, transformedVertices, 0, originalVertices.length);

        for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
            int baseIndex = vertexIndex * 8;
            float x = Float.intBitsToFloat(originalVertices[baseIndex]);
            float y = Float.intBitsToFloat(originalVertices[baseIndex + 1]);
            float z = Float.intBitsToFloat(originalVertices[baseIndex + 2]);
            
            org.joml.Vector3f vertex = new org.joml.Vector3f(x, y, z);
            vertex.sub(0.5f, 0.5f, 0.5f);
            matrix.transformPosition(vertex);
            vertex.add(0.5f, 0.5f, 0.5f);
            transformedVertices[baseIndex] = Float.floatToIntBits(vertex.x);
            transformedVertices[baseIndex + 1] = Float.floatToIntBits(vertex.y);
            transformedVertices[baseIndex + 2] = Float.floatToIntBits(vertex.z);
        }
        
        return new BakedQuad(
                transformedVertices,
                originalQuad.getTintIndex(),
                originalQuad.getDirection(),
                originalQuad.getSprite(),
                originalQuad.isShade()
        );
    }
    // LRU cache implementation with configurable size (optimized for modpack environments)
    private static final int DEFAULT_CACHE_SIZE = 2000; // Optimized from 5000 for memory efficiency
    private static final int MAX_CACHE_SIZE = getConfiguredCacheSize();
    private static final Map<ModelCacheKey, CacheEntry> GLOBAL_CACHE = new ConcurrentHashMap<>();
    
    // Texture ID mapping for memory-efficient cache keys
    private static final Map<String, Integer> TEXTURE_ID_MAP = new ConcurrentHashMap<>();
    private static final java.util.concurrent.atomic.AtomicInteger NEXT_TEXTURE_ID = new java.util.concurrent.atomic.AtomicInteger(0);
    
    // BlockState string to ID mapping for cache key compression
    private static final Map<String, Integer> BLOCKSTATE_ID_MAP = new ConcurrentHashMap<>();
    private static final java.util.concurrent.atomic.AtomicInteger NEXT_BLOCKSTATE_ID = new java.util.concurrent.atomic.AtomicInteger(0);
    
    // Cache statistics tracking
    private static final java.util.concurrent.atomic.AtomicLong cacheHits = new java.util.concurrent.atomic.AtomicLong(0);
    private static final java.util.concurrent.atomic.AtomicLong cacheMisses = new java.util.concurrent.atomic.AtomicLong(0);
    private static final java.util.concurrent.atomic.AtomicLong cacheEvictions = new java.util.concurrent.atomic.AtomicLong(0);


    private final Map<String, TextureAtlasSprite> textureSprites;
    private final BakedModel vanillaLeverModel;
    private final ItemOverrides itemOverrides;
    
    /** Cached sprite names to avoid repeated contents() calls. Thread-safe for concurrent rendering. */
    private final Map<TextureAtlasSprite, String> spriteNameCache = new ConcurrentHashMap<>();
    /** Cached ResourceLocation objects to reduce allocations. Thread-safe for concurrent rendering. */
    private final Map<String, ResourceLocation> resourceLocationCache = new ConcurrentHashMap<>();
    /** Tracks overlay diagnostic logging to avoid log spam (one message per unique texture). */
    private final java.util.Set<String> overlayDiagnosticLogged = ConcurrentHashMap.newKeySet();

    // Cache performance tracking
    private static final java.util.concurrent.atomic.AtomicInteger cacheOperations = 
            new java.util.concurrent.atomic.AtomicInteger(0);
    
    /**
     * Gets or creates texture ID for memory-efficient cache keys.
     */
    private static int getTextureId(@Nullable String texturePath) {
        if (texturePath == null) return 0;
        return TEXTURE_ID_MAP.computeIfAbsent(texturePath, 
            path -> NEXT_TEXTURE_ID.incrementAndGet());
    }
    
    /**
     * Gets or creates BlockState ID for cache key compression.
     */
    private static int getBlockStateId(@Nonnull String blockStateString) {
        return BLOCKSTATE_ID_MAP.computeIfAbsent(blockStateString,
            state -> NEXT_BLOCKSTATE_ID.incrementAndGet());
    }
    
    /**
     * Gets configured cache size from system property or uses default.
     * Can be configured via: -Djustsomeswitches.cache.size=2000
     */
    private static int getConfiguredCacheSize() {
        String sizeProperty = System.getProperty("justsomeswitches.cache.size");
        if (sizeProperty != null) {
            try {
                int size = Integer.parseInt(sizeProperty);
                if (size > 0 && size <= 20000) { // Max 20k for safety
                    LOGGER.info("Cache size configured to: {} entries", size);
                    return size;
                }
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid cache size property: {}", sizeProperty);
            }
        }
        return DEFAULT_CACHE_SIZE;
    }
    
    /**
     * Cache entry with weighted access tracking for optimized LRU eviction.
     * Uses a combined score of access frequency and recency for better cache retention.
     */
    private static class CacheEntry {
        private final List<BakedQuad> quads;
        private volatile long lastAccessTime;
        private final java.util.concurrent.atomic.AtomicInteger accessCount;
        public CacheEntry(List<BakedQuad> quads) {
            this.quads = quads;
            this.lastAccessTime = System.currentTimeMillis();
            this.accessCount = new java.util.concurrent.atomic.AtomicInteger(1);
        }
        public List<BakedQuad> getQuads() {
            this.lastAccessTime = System.currentTimeMillis();
            this.accessCount.incrementAndGet();
            return quads;
        }
        public int getAccessCount() {
            return accessCount.get();
        }
        /** Weighted eviction score: higher = higher priority to keep. */
        public long getEvictionScore() {
            long ageMs = System.currentTimeMillis() - lastAccessTime;
            return (accessCount.get() * 1000L) - ageMs;
        }
    }


    public SwitchesLeverDynamicModel(@Nonnull Map<String, TextureAtlasSprite> textureSprites,
                             @SuppressWarnings("unused") @Nonnull Map<String, Matrix4f> orientationTransforms,
                             @SuppressWarnings("unused") @Nonnull Map<String, String> jsonVariables,
                             @SuppressWarnings("unused") @Nonnull SwitchesGeometryLoader.PowerModeConfig powerModeConfig,
                             @Nonnull BakedModel vanillaLeverModel,
                             @Nonnull ItemOverrides itemOverrides) {
        this.textureSprites = new HashMap<>(textureSprites);
        this.vanillaLeverModel = vanillaLeverModel;
        this.itemOverrides = itemOverrides;
    }
    /**
     * Calculates Z-offset for overlay layer to prevent z-fighting.
     */
    private static float calculateZOffset(int order) {
        return order * 0.0001f;
    }
    
    /**
     * Gets TextureAtlasSprite for a ResourceLocation.
     */
    @SuppressWarnings("resource") // SpriteContents owned by atlas, not our responsibility to close
    @Nonnull
    private TextureAtlasSprite getAtlasSprite(@Nonnull ResourceLocation spriteLocation) {
        net.minecraft.client.renderer.texture.TextureAtlas atlas = 
            net.minecraft.client.Minecraft.getInstance()
                .getModelManager()
                .getAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS);
        TextureAtlasSprite sprite = atlas.getSprite(spriteLocation);
        // atlas.getSprite() never returns null; check for missing texture placeholder
        if (sprite.contents().name().equals(
                net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation())) {
            sprite = atlas.getSprite(
                net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation());
        }
        return sprite;
    }
    
    /**
     * Validates ModelData before rendering.
     */
    private boolean hasValidModelData(@Nonnull ModelData extraData) {
        if (extraData == ModelData.EMPTY) {
            return false;
        }
        
        // Ghost preview mode is always valid
        Boolean ghostMode = extraData.get(SwitchesLeverBlockEntity.GHOST_MODE);
        if (ghostMode != null && ghostMode) {
            return true;
        }
        
        // Check for any texture customization or power mode
        return extraData.get(SwitchesLeverBlockEntity.TOGGLE_TEXTURE) != null ||
               extraData.get(SwitchesLeverBlockEntity.BASE_TEXTURE) != null ||
               extraData.get(SwitchesLeverBlockEntity.POWER_MODE) != null;
    }

    @Override
    @Nonnull
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                   @Nonnull RandomSource rand, @Nonnull ModelData extraData, 
                                   @Nullable RenderType renderType) {

        if (!hasValidModelData(extraData)) {
            return vanillaLeverModel.getQuads(state, side, rand, ModelData.EMPTY, renderType);
        }


        ModelCacheKey cacheKey = createCacheKey(state, side, extraData, renderType);
        List<BakedQuad> cachedQuads = getCachedQuads(cacheKey);
        if (cachedQuads != null) {
            return cachedQuads;
        }

        List<BakedQuad> generatedQuads = generateSwitchQuads(state, side, extraData, rand, renderType);
        cacheGeneratedQuads(cacheKey, generatedQuads);

        // Periodic cache cleanup and statistics logging
        int ops = cacheOperations.incrementAndGet();
        if (ops % 1000 == 0) {
            cleanupGlobalCache();
            if (ops % 10000 == 0) {
                logCacheStatistics();
            }
        }

        return generatedQuads;
    }

    @Override
    @Nonnull
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull RandomSource rand) {
        return getQuads(state, side, rand, ModelData.EMPTY, null);
    }

    /**
     * Multi-level cache lookup.
     */
    @Nullable
    private List<BakedQuad> getCachedQuads(@Nonnull ModelCacheKey key) {
        CacheEntry entry = GLOBAL_CACHE.get(key);
        if (entry != null) {
            cacheHits.incrementAndGet();
            return entry.getQuads();
        }
        cacheMisses.incrementAndGet();
        return null;
    }


    private void cacheGeneratedQuads(@Nonnull ModelCacheKey key, @Nonnull List<BakedQuad> quads) {
        if (isCommonConfiguration(key)) {
            // Check cache size before adding
            if (GLOBAL_CACHE.size() >= MAX_CACHE_SIZE) {
                performLRUEviction();
            }
            GLOBAL_CACHE.put(key, new CacheEntry(quads));
        }
    }


    private boolean isCommonConfiguration(@Nonnull ModelCacheKey key) {

        return key.isDefaultTextures() || "center".equals(key.getWallOrientation());
    }

    /**
     * Generates switch quads based on current configuration.
     */
    @Nonnull
    private List<BakedQuad> generateSwitchQuads(@Nullable BlockState state, @Nullable Direction side,
                                               @Nonnull ModelData extraData, @Nonnull RandomSource rand,
                                               @Nullable RenderType renderType) {

        // Check for ghost preview mode first (simplified processing)
        if (isGhostPreviewMode(extraData)) {
            return generateGhostPreviewQuads(state, side, extraData, rand, renderType);
        }

        // Regular texture customization processing for placed blocks
        boolean hasTextureData = false;
        String toggleTexture = null;
        String baseTexture = null;
        String faceSelection = null;
        String powerMode = null;
        
        if (extraData != ModelData.EMPTY) {
            toggleTexture = extraData.get(SwitchesLeverBlockEntity.TOGGLE_TEXTURE);
            baseTexture = extraData.get(SwitchesLeverBlockEntity.BASE_TEXTURE);
            faceSelection = extraData.get(SwitchesLeverBlockEntity.FACE_SELECTION);
            powerMode = extraData.get(SwitchesLeverBlockEntity.POWER_MODE);
            
            // Check for texture replacement or power mode
            boolean hasTextureReplacement = (toggleTexture != null && !toggleTexture.equals(SwitchesLeverBlockEntity.DEFAULT_TOGGLE_TEXTURE)) ||
                           (baseTexture != null && !baseTexture.equals(SwitchesLeverBlockEntity.DEFAULT_BASE_TEXTURE)) ||
                           (powerMode != null);
            
            // Check for texture rotation
            String baseRotation = extraData.get(SwitchesLeverBlockEntity.BASE_ROTATION);
            String toggleRotation = extraData.get(SwitchesLeverBlockEntity.TOGGLE_ROTATION);
            boolean hasTextureRotation = (baseRotation != null && !baseRotation.equals("NORMAL") && baseTexture != null) ||
                                        (toggleRotation != null && !toggleRotation.equals("NORMAL") && toggleTexture != null);
            
            hasTextureData = hasTextureReplacement || hasTextureRotation;
        }


        List<BakedQuad> baseQuads = vanillaLeverModel.getQuads(state, side, rand, 
                net.neoforged.neoforge.client.model.data.ModelData.EMPTY, renderType);
        
        if (baseQuads.isEmpty()) {
            return baseQuads;
        }

        List<BakedQuad> texturedQuads = baseQuads;
        if (hasTextureData) {
            // Get overlay data for texture application
            Map<Direction, List<OverlayLayer>> overlayData = extraData.get(SwitchesLeverBlockEntity.OVERLAY_DATA);
            texturedQuads = applyCustomTextures(baseQuads, toggleTexture, baseTexture, faceSelection, powerMode, state, extraData, overlayData);
        }

        if (state != null && isWallPlacement(state) && extraData != ModelData.EMPTY) {
            texturedQuads = applyWallOrientationRotations(texturedQuads, state, extraData);
        }

        return texturedQuads;
    }
    
    /**
     * Builds a BakedQuad with Z-offset applied to prevent z-fighting.
     */
    @Nonnull
    private BakedQuad buildQuadWithZOffset(@Nonnull BakedQuad templateQuad,
                                          @Nonnull TextureAtlasSprite sprite,
                                          int tintIndex,
                                          float zOffset) {
        int[] originalVertices = templateQuad.getVertices();
        int[] newVertices = new int[originalVertices.length];
        System.arraycopy(originalVertices, 0, newVertices, 0, originalVertices.length);
        
        Direction face = templateQuad.getDirection();
        // Apply Z-offset along face normal
        for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
            int baseIndex = vertexIndex * 8;
            float x = Float.intBitsToFloat(originalVertices[baseIndex]);
            float y = Float.intBitsToFloat(originalVertices[baseIndex + 1]);
            float z = Float.intBitsToFloat(originalVertices[baseIndex + 2]);
            
            // Apply offset along face normal
            switch (face) {
                case UP -> y += zOffset;
                case DOWN -> y -= zOffset;
                case NORTH -> z -= zOffset;
                case SOUTH -> z += zOffset;
                case WEST -> x -= zOffset;
                case EAST -> x += zOffset;
            }
            
            newVertices[baseIndex] = Float.floatToIntBits(x);
            newVertices[baseIndex + 1] = Float.floatToIntBits(y);
            newVertices[baseIndex + 2] = Float.floatToIntBits(z);
        }
        
        // Update texture coordinates for new sprite
        TextureAtlasSprite originalSprite = templateQuad.getSprite();
        for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
            int baseIndex = vertexIndex * 8;
            
            float originalU = Float.intBitsToFloat(originalVertices[baseIndex + 4]);
            float originalV = Float.intBitsToFloat(originalVertices[baseIndex + 5]);
            
            // Convert to relative coordinates
            float relativeU = (originalU - originalSprite.getU0()) / (originalSprite.getU1() - originalSprite.getU0());
            float relativeV = (originalV - originalSprite.getV0()) / (originalSprite.getV1() - originalSprite.getV0());
            
            // Convert to new texture space
            float newU = sprite.getU0() + relativeU * (sprite.getU1() - sprite.getU0());
            float newV = sprite.getV0() + relativeV * (sprite.getV1() - sprite.getV0());
            
            newVertices[baseIndex + 4] = Float.floatToIntBits(newU);
            newVertices[baseIndex + 5] = Float.floatToIntBits(newV);
        }
        
        return new BakedQuad(
            newVertices,
            tintIndex,
            face,
            sprite,
            templateQuad.isShade()
        );
    }
    
    /**
     * Generates quads specifically for ghost preview (default lever with correct orientation).
     */
    @Nonnull
    private List<BakedQuad> generateGhostPreviewQuads(@Nullable BlockState state, @Nullable Direction side,
                                                     @Nonnull ModelData extraData, @Nonnull RandomSource rand,
                                                     @Nullable RenderType renderType) {
        
        // Get base vanilla lever quads (no texture customization)
        List<BakedQuad> baseQuads = vanillaLeverModel.getQuads(state, side, rand, 
                net.neoforged.neoforge.client.model.data.ModelData.EMPTY, renderType);
        
        if (baseQuads.isEmpty()) {
            return baseQuads;
        }

        // Apply wall orientation if needed (ghost preview shows correct orientation)
        if (state != null && isWallPlacement(state)) {
            return applyWallOrientationRotations(baseQuads, state, extraData);
        }
        
        return baseQuads;
    }
    
    /**
     * Checks if ghost preview mode is enabled in ModelData.
     */
    private boolean isGhostPreviewMode(@Nonnull ModelData extraData) {
        Boolean ghostMode = extraData.get(SwitchesLeverBlockEntity.GHOST_MODE);
        return ghostMode != null && ghostMode;
    }



    /**
     * Applies custom texture replacement to quads with overlay support.
     */
    @Nonnull
    private List<BakedQuad> applyCustomTextures(@Nonnull List<BakedQuad> baseQuads, 
                                               @Nullable String toggleTexture,
                                               @Nullable String baseTexture,
                                               @Nullable String faceSelection,
                                               @Nullable String powerMode,
                                               @Nullable BlockState state,
                                               @Nonnull ModelData extraData,
                                               @Nullable Map<Direction, List<OverlayLayer>> overlayData) {
        
        // Get tintIndex from ModelData (same tintIndex for both toggle and base parts)
        Integer tintIndexObj = extraData.get(SwitchesLeverBlockEntity.TINT_INDEX);
        int tintIndex = (tintIndexObj != null) ? tintIndexObj : -1;
        List<BakedQuad> texturedQuads = new ArrayList<>();
        for (BakedQuad quad : baseQuads) {
            // Process quad with overlay support
            List<BakedQuad> processedQuads = processQuadWithOverlaySupport(
                quad, toggleTexture, baseTexture, faceSelection, powerMode, state, extraData,
                tintIndex, tintIndex, overlayData
            );
            texturedQuads.addAll(processedQuads);
        }
        return texturedQuads;
    }
    /**
     * Processes quad with overlay support - returns list of quads (multiple if overlays exist).
     */
    @Nonnull
    private List<BakedQuad> processQuadWithOverlaySupport(@Nonnull BakedQuad originalQuad,
                                                         @Nullable String toggleTexture,
                                                         @Nullable String baseTexture,
                                                         @Nullable String faceSelection,
                                                         @Nullable String powerMode,
                                                         @Nullable BlockState state,
                                                         @Nonnull ModelData extraData,
                                                         int toggleTintIndex,
                                                         int baseTintIndex,
                                                         @Nullable Map<Direction, List<OverlayLayer>> overlayData) {
        
        TextureAtlasSprite originalSprite = originalQuad.getSprite();
        String originalTextureName = getTextureName(originalSprite);
        
        // Determine if this quad uses toggle or base texture
        boolean isTogglePart = isLeverMovingPart(originalTextureName);
        boolean isBasePart = isLeverBasePart(originalTextureName);
        
        // Check if we have overlays for this texture
        List<OverlayLayer> overlayLayers = null;
        if (overlayData != null && !overlayData.isEmpty() &&
                ((isTogglePart && toggleTexture != null) || (isBasePart && baseTexture != null))) {
            // Find overlay data with multiple layers (e.g. grass block sides have dirt + overlay)
            for (Direction dir : Direction.values()) {
                List<OverlayLayer> layers = overlayData.get(dir);
                if (layers != null && layers.size() > 1) {
                    overlayLayers = layers;
                    break;
                }
            }
        }
        // Diagnostic: log overlay rendering decisions (once per texture to avoid log spam)
        if (overlayDiagnosticLogged.add(originalTextureName)) {
            if (overlayData != null && !overlayData.isEmpty() && overlayLayers == null) {
                LOGGER.info("Overlay data present but no multi-layer faces found for quad '{}' (toggle={}, base={})",
                    originalTextureName, isTogglePart, isBasePart);
            }
            if (overlayLayers != null) {
                LOGGER.info("Rendering {} overlay layers for quad '{}'", overlayLayers.size(), originalTextureName);
            }
        }
        
        // If we have overlay layers, generate multiple quads
        if (overlayLayers != null && overlayLayers.size() > 1) {
            List<BakedQuad> result = new ArrayList<>();
            for (OverlayLayer layer : overlayLayers) {
                float zOffset = calculateZOffset(layer.getOrder());
                TextureAtlasSprite layerSprite = getAtlasSprite(layer.getSprite());
                int layerTintIndex = layer.getTintIndex();
                
                BakedQuad overlayQuad = buildQuadWithZOffset(
                    originalQuad,
                    layerSprite,
                    layerTintIndex,
                    zOffset
                );
                result.add(overlayQuad);
            }
            return result;
        }
        
        // No overlays or single layer - use normal processing
        BakedQuad processedQuad = processQuadWithCustomTextures(
            originalQuad, toggleTexture, baseTexture, faceSelection, powerMode, state, extraData,
            toggleTintIndex, baseTintIndex
        );
        return Collections.singletonList(processedQuad);
    }
    
    /**
     * Processes individual quad with custom texture replacement.
     */
    @Nonnull
    private BakedQuad processQuadWithCustomTextures(@Nonnull BakedQuad originalQuad,
                                                   @Nullable String toggleTexture,
                                                   @Nullable String baseTexture,
                                                   @SuppressWarnings("unused") @Nullable String faceSelection,
                                                   @Nullable String powerMode,
                                                   @SuppressWarnings("unused") @Nullable BlockState state,
                                                   @Nonnull ModelData extraData,
                                                   int toggleTintIndex,
                                                   int baseTintIndex) {

        TextureAtlasSprite originalSprite = originalQuad.getSprite();
        String originalTextureName = getTextureName(originalSprite);

        // Determine replacement texture
        TextureAtlasSprite replacementSprite = determineReplacementTexture(
                originalSprite, originalTextureName, toggleTexture, baseTexture, 
                powerMode);

        // Check for texture rotations (regardless of whether texture is being replaced)
        TextureRotation rotation = null;
        
        // Check if this is a base texture part that needs rotation
        boolean isBaseTexturePart = isLeverBasePart(originalTextureName);
        if (isBaseTexturePart && baseTexture != null) {
            String rotationString = extraData.get(SwitchesLeverBlockEntity.BASE_ROTATION);
            if (rotationString != null) {
                try {
                    rotation = TextureRotation.valueOf(rotationString);
                } catch (IllegalArgumentException e) {
                    rotation = TextureRotation.NORMAL;
                }
            }
        }
        
        // Check if this is a toggle texture part that needs rotation
        // IMPORTANT: Toggle texture faces in the vanilla model have built-in 270° rotation,
        // so we need to apply a +90° offset to compensate and give users intuitive control
        // HOWEVER: Only apply rotation compensation when there is a block in the toggle slot
        boolean isToggleTexturePart = isLeverMovingPart(originalTextureName);
        if (isToggleTexturePart) {
            // Check if toggle slot has a block
            Boolean hasToggleBlock = extraData.get(SwitchesLeverBlockEntity.HAS_TOGGLE_BLOCK);
            boolean toggleSlotHasBlock = hasToggleBlock != null && hasToggleBlock;
            
            if (toggleSlotHasBlock) {
                // Toggle slot has block - apply rotation with compensation
                String rotationString = extraData.get(SwitchesLeverBlockEntity.TOGGLE_ROTATION);
                if (rotationString != null) {
                    try {
                        TextureRotation userRotation = TextureRotation.valueOf(rotationString);
                        // Apply +90° offset to compensate for model's built-in 270° rotation
                        rotation = compensateToggleRotation(userRotation);
                    } catch (IllegalArgumentException e) {
                        rotation = TextureRotation.RIGHT; // Compensated NORMAL = RIGHT
                    }
                }
            }
            // else: Toggle slot is empty - no rotation applied (rotation stays null, becomes NORMAL)
        }
        
        // Apply processing if we need texture replacement OR rotation
        boolean needsTextureReplacement = (replacementSprite != originalSprite);
        boolean needsRotation = (rotation != null && rotation != TextureRotation.NORMAL);
        
        if (!needsTextureReplacement && !needsRotation) {
            return originalQuad;
        }
        
        // Use replacement sprite if available, otherwise use original sprite
        TextureAtlasSprite finalSprite = needsTextureReplacement ? replacementSprite : originalSprite;
        
        // Determine tintIndex for this quad
        int quadTintIndex = originalQuad.getTintIndex(); // Default: preserve original
        if (needsTextureReplacement) {
            // Applying custom texture - use appropriate tintIndex from source block
            if (isToggleTexturePart && toggleTexture != null) {
                quadTintIndex = toggleTintIndex;
            } else if (isBaseTexturePart && baseTexture != null) {
                quadTintIndex = baseTintIndex;
            }
        }
        
        return replaceQuadTexture(originalQuad, finalSprite, rotation, quadTintIndex);
    }

    /**
     * Determines replacement texture based on part type.
     */
    @Nonnull
    private TextureAtlasSprite determineReplacementTexture(@Nonnull TextureAtlasSprite originalSprite,
                                                         @Nonnull String originalTextureName,
                                                         @Nullable String toggleTexture,
                                                         @Nullable String baseTexture,
                                                         @Nullable String powerMode) {

        // Handle power textures (including DEFAULT mode)
        if (powerMode != null) {
            if (isPoweredTexture(originalTextureName)) {
                return getPoweredReplacementTexture(powerMode, toggleTexture);
            }
            if (isUnpoweredTexture(originalTextureName)) {
                return getUnpoweredReplacementTexture(powerMode, toggleTexture);
            }
        }

        // Handle toggle texture replacement
        if (isLeverMovingPart(originalTextureName) && toggleTexture != null) {
            TextureAtlasSprite toggleSprite = getTextureSprite(toggleTexture);
            if (toggleSprite != null) {
                return toggleSprite;
            }
        }

        // Handle base texture replacement
        if (isLeverBasePart(originalTextureName) && baseTexture != null) {
            TextureAtlasSprite baseSprite = getTextureSprite(baseTexture);
            if (baseSprite != null) {
                return baseSprite;
            }
        }

        return originalSprite;
    }

    /**
     * Gets powered texture replacement based on power mode.
     */
    @Nonnull
    private TextureAtlasSprite getPoweredReplacementTexture(@Nullable String powerMode, @Nullable String toggleTexture) {
        if ("ALT".equals(powerMode)) {
            // ALT mode uses lime concrete powder for powered state
            TextureAtlasSprite altSprite = getTextureSprite("minecraft:block/lime_concrete_powder");
            if (altSprite != null) return altSprite;
        } else if ("NONE".equals(powerMode)) {
            // NONE mode uses toggle texture (or default toggle if not set)
            String effectiveToggleTexture = toggleTexture != null ? toggleTexture : SwitchesLeverBlockEntity.DEFAULT_TOGGLE_TEXTURE;
            TextureAtlasSprite toggleSprite = getTextureSprite(effectiveToggleTexture);
            if (toggleSprite != null) return toggleSprite;
        }
        
        // DEFAULT mode and fallback: use redstone block
        TextureAtlasSprite fallback = getTextureSprite("minecraft:block/redstone_block");
        return fallback != null ? fallback : textureSprites.values().iterator().next();
    }

    /**
     * Gets unpowered texture replacement based on power mode.
     */
    @Nonnull
    private TextureAtlasSprite getUnpoweredReplacementTexture(@Nullable String powerMode, @Nullable String toggleTexture) {
        if ("ALT".equals(powerMode)) {
            // ALT mode uses redstone block for unpowered state
            TextureAtlasSprite altSprite = getTextureSprite("minecraft:block/redstone_block");
            if (altSprite != null) return altSprite;
        } else if ("NONE".equals(powerMode)) {
            // NONE mode uses toggle texture (or default toggle if not set)
            String effectiveToggleTexture = toggleTexture != null ? toggleTexture : SwitchesLeverBlockEntity.DEFAULT_TOGGLE_TEXTURE;
            TextureAtlasSprite toggleSprite = getTextureSprite(effectiveToggleTexture);
            if (toggleSprite != null) return toggleSprite;
        }
        
        // DEFAULT mode and fallback: use gray concrete powder
        TextureAtlasSprite fallback = getTextureSprite("minecraft:block/gray_concrete_powder");
        return fallback != null ? fallback : textureSprites.values().iterator().next();
    }


    @Nonnull
    private BakedQuad replaceQuadTexture(@Nonnull BakedQuad originalQuad, 
                                        @Nonnull TextureAtlasSprite newSprite,
                                        @Nullable TextureRotation rotation,
                                        int tintIndex) {
        
        int[] originalVertices = originalQuad.getVertices();
        TextureAtlasSprite originalSprite = originalQuad.getSprite();


        int[] newVertices = transformVertexData(originalVertices, originalSprite, newSprite, rotation);


        return new BakedQuad(
                newVertices,
                tintIndex,  // Use provided tintIndex from source block
                originalQuad.getDirection(),
                newSprite,
                originalQuad.isShade()
        );
    }


    @Nonnull
    private int[] transformVertexData(@Nonnull int[] originalVertices,
                                     @Nonnull TextureAtlasSprite originalTexture,
                                     @Nonnull TextureAtlasSprite newTexture,
                                     @Nullable TextureRotation rotation) {

        int[] newVertices = originalVertices.clone();

        for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
            int baseIndex = vertexIndex * 8;

            float originalU = Float.intBitsToFloat(originalVertices[baseIndex + 4]);
            float originalV = Float.intBitsToFloat(originalVertices[baseIndex + 5]);

            // Convert to relative coordinates (0.0 to 1.0)
            float relativeU = (originalU - originalTexture.getU0()) / (originalTexture.getU1() - originalTexture.getU0());
            float relativeV = (originalV - originalTexture.getV0()) / (originalTexture.getV1() - originalTexture.getV0());
            
            // Apply rotation if specified
            if (rotation != null && rotation != TextureRotation.NORMAL) {
                float[] rotated = rotation.rotateUV(relativeU, relativeV);
                relativeU = rotated[0];
                relativeV = rotated[1];
            }
            
            // Convert to new texture space
            float newU = newTexture.getU0() + relativeU * (newTexture.getU1() - newTexture.getU0());
            float newV = newTexture.getV0() + relativeV * (newTexture.getV1() - newTexture.getV0());

            newVertices[baseIndex + 4] = Float.floatToIntBits(newU);
            newVertices[baseIndex + 5] = Float.floatToIntBits(newV);
        }

        return newVertices;
    }




    /**
     * Applies +90° rotation offset to compensate for toggle texture faces' built-in 270° rotation in the model JSON.
     * This ensures users get intuitive rotation control where NORMAL appears as expected.
     * Mapping:
     * - User selects NORMAL (0°) → Apply RIGHT (90°) → Total: 270° + 90° = 360° = 0° (appears normal)
     * - User selects RIGHT (90°) → Apply INVERT (180°) → Total: 270° + 180° = 450° = 90°
     * - User selects INVERT (180°) → Apply LEFT (-90°) → Total: 270° - 90° = 180°  
     * - User selects LEFT (-90°) → Apply NORMAL (0°) → Total: 270° + 0° = 270° = -90°
     */
    @Nonnull
    private TextureRotation compensateToggleRotation(@Nonnull TextureRotation userRotation) {
        return switch (userRotation) {
            case NORMAL -> TextureRotation.RIGHT;   // 0° + 90° = 90° compensation
            case RIGHT -> TextureRotation.INVERT;   // 90° + 90° = 180° compensation
            case INVERT -> TextureRotation.LEFT;    // 180° + 90° = 270° = -90° compensation
            case LEFT -> TextureRotation.NORMAL;    // -90° + 90° = 0° compensation
        };
    }

    /**
     * Identifies lever moving parts for toggle texture.
     */
    private boolean isLeverMovingPart(@Nonnull String originalTextureName) {

        if (isPoweredTexture(originalTextureName) || isUnpoweredTexture(originalTextureName)) {
            return false;
        }

        return originalTextureName.contains("planks") ||
               originalTextureName.contains("wood") ||
               originalTextureName.contains("oak") ||
               originalTextureName.contains("lever") ||

               (!originalTextureName.contains("cobblestone") && 
                !originalTextureName.contains("stone") &&
                !originalTextureName.contains("iron"));
    }

    /**
     * Identifies lever base parts for base texture.
     */
    private boolean isLeverBasePart(@Nonnull String originalTextureName) {

        if (isPoweredTexture(originalTextureName) || isUnpoweredTexture(originalTextureName)) {
            return false;
        }

        return originalTextureName.contains("cobblestone") ||
               originalTextureName.contains("stone") ||
               originalTextureName.contains("bricks") ||
               originalTextureName.contains("iron");
    }



    /** Returns cached sprite name to avoid repeated contents() calls. */
    @Nonnull
    private String getCachedSpriteName(@Nonnull TextureAtlasSprite sprite) {
        return spriteNameCache.computeIfAbsent(sprite, this::computeSpriteName);
    }
    
    /** Computes sprite name from sprite contents. Does NOT close contents - they are owned by the atlas. */
    @Nonnull
    private String computeSpriteName(@Nonnull TextureAtlasSprite sprite) {
        try {
            return sprite.contents().name().toString();
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    @Nonnull
    private String getTextureName(@Nonnull TextureAtlasSprite sprite) {
        return getCachedSpriteName(sprite);
    }

    private boolean isPoweredTexture(@Nonnull String textureName) {
        return textureName.contains("redstone_block") ||
               textureName.contains("switches_lever_powered") ||
               textureName.contains("powered") ||
               (textureName.contains("lever") && textureName.contains("on"));
    }

    private boolean isUnpoweredTexture(@Nonnull String textureName) {
        return textureName.contains("gray_concrete_powder") ||
               textureName.contains("switches_lever_unpowered") ||
               textureName.contains("unpowered") ||
               (textureName.contains("lever") && textureName.contains("off"));
    }
    /** Returns cached ResourceLocation to reduce object allocations. */
    @Nonnull
    private ResourceLocation getCachedResourceLocation(@Nonnull String path) {
        return resourceLocationCache.computeIfAbsent(path, ResourceLocation::new);
    }
    
    /**
     * Gets texture sprite from path string.
     */
    @SuppressWarnings("resource") // SpriteContents owned by atlas, not our responsibility to close
    @Nullable
    private TextureAtlasSprite getTextureSprite(@Nullable String texturePath) {
        if (texturePath == null || texturePath.isEmpty()) {
            return null;
        }
        try {
            ResourceLocation textureLocation = getCachedResourceLocation(texturePath);
            // Check model's own texture sprites first (fast path)
            for (TextureAtlasSprite sprite : textureSprites.values()) {
                try {
                    if (sprite.contents().name().equals(textureLocation)) {
                        return sprite;
                    }
                } catch (Exception e) {
                    // Continue checking other sprites
                }
            }
            // Direct key lookup in model sprites
            TextureAtlasSprite directSprite = textureSprites.get(texturePath);
            if (directSprite != null) {
                return directSprite;
            }
            // Simplified key lookup
            String simplifiedKey = textureLocation.getPath().replace("/", "_");
            TextureAtlasSprite simplifiedSprite = textureSprites.get(simplifiedKey);
            if (simplifiedSprite != null) {
                return simplifiedSprite;
            }
            // Atlas lookup using model manager (getAtlasSprite never returns null)
            TextureAtlasSprite atlasSprite = getAtlasSprite(textureLocation);
            // Check that we got a real sprite, not the missing texture
            if (!atlasSprite.contents().name().equals(
                    net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation())) {
                return atlasSprite;
            }
            LOGGER.warn("Failed to find texture sprite for: {}", texturePath);
        } catch (Exception e) {
            LOGGER.warn("Error looking up texture sprite for: {}", texturePath, e);
        }
        return null;
    }

    /**
     * Creates cache key from rendering parameters.
     */
    @Nonnull
    private ModelCacheKey createCacheKey(@Nullable BlockState state, @Nullable Direction side,
                                        @Nonnull ModelData extraData, @Nullable RenderType renderType) {

        // Simplified cache key for ghost preview (no texture customization)
        Boolean ghostMode = extraData.get(SwitchesLeverBlockEntity.GHOST_MODE);
        if (ghostMode != null && ghostMode) {
            Float ghostOpacity = extraData.get(SwitchesLeverBlockEntity.GHOST_ALPHA);
            String wallOrientation = extraData.get(SwitchesLeverBlockEntity.WALL_ORIENTATION);
            
            return new ModelCacheKey(
                state != null ? state.toString() : "null",
                side,
                null, null, null, // No texture parameters for ghost preview
                wallOrientation,
                null, null, // No rotation parameters for ghost preview
                ghostMode,
                ghostOpacity,
                null, // No toggle block for ghost preview
                renderType
            );
        }
        
        // Full cache key for regular texture customization
        String toggleTexture = extraData.get(SwitchesLeverBlockEntity.TOGGLE_TEXTURE);
        String baseTexture = extraData.get(SwitchesLeverBlockEntity.BASE_TEXTURE);
        String powerMode = extraData.get(SwitchesLeverBlockEntity.POWER_MODE);
        String wallOrientation = extraData.get(SwitchesLeverBlockEntity.WALL_ORIENTATION);
        String baseRotation = extraData.get(SwitchesLeverBlockEntity.BASE_ROTATION);
        String toggleRotation = extraData.get(SwitchesLeverBlockEntity.TOGGLE_ROTATION);
        Boolean hasToggleBlock = extraData.get(SwitchesLeverBlockEntity.HAS_TOGGLE_BLOCK);

        return new ModelCacheKey(
                state != null ? state.toString() : "null",
                side,
                toggleTexture,
                baseTexture,
                powerMode,
                wallOrientation,
                baseRotation,
                toggleRotation,
                false, // Not ghost mode
                null, // No ghost opacity
                hasToggleBlock,
                renderType
        );
    }


    /**
     * Performs LRU-based cache cleanup when cache exceeds size limit.
     */
    private static void cleanupGlobalCache() {
        if (GLOBAL_CACHE.size() > MAX_CACHE_SIZE) {
            performLRUEviction();
        }
    }
    
    /**
     * Performs weighted LRU eviction to reduce cache to 80% of max size.
     * Uses eviction score (access frequency + recency) to retain frequently-used entries.
     * Higher eviction score = higher priority to keep in cache.
     */
    private static void performLRUEviction() {
        int targetSize = (int) (MAX_CACHE_SIZE * 0.8); // Reduce to 80% capacity
        int toRemove = GLOBAL_CACHE.size() - targetSize;
        
        if (toRemove <= 0) {
            return;
        }
        
        // Sort entries by eviction score (LOWEST score evicted first)
        // Score = (accessCount * 1000) - age_ms
        // This keeps frequently-accessed AND recently-used entries
        GLOBAL_CACHE.entrySet().stream()
            .sorted(java.util.Comparator.comparingLong(e -> e.getValue().getEvictionScore()))
            .limit(toRemove)
            .map(Map.Entry::getKey)
            .forEach(key -> {
                GLOBAL_CACHE.remove(key);
                cacheEvictions.incrementAndGet();
            });
    }
    
    /**
     * Estimates cache memory usage in bytes.
     * Provides approximate memory footprint for monitoring.
     */
    private static long estimateCacheMemoryUsage() {
        long totalBytes = 0;
        
        // Cache key overhead (integer IDs + packed flags)
        // ModelCacheKey: ~48 bytes per entry (compressed from ~200+ bytes with strings)
        totalBytes += GLOBAL_CACHE.size() * 48L;
        
        // Quad data overhead
        for (CacheEntry entry : GLOBAL_CACHE.values()) {
            List<BakedQuad> quads = entry.getQuads();
            // BakedQuad: ~200 bytes per quad (vertices + metadata)
            totalBytes += quads.size() * 200L;
        }
        
        // ID mapping overhead
        totalBytes += TEXTURE_ID_MAP.size() * 64L; // String + Integer
        totalBytes += BLOCKSTATE_ID_MAP.size() * 64L; // String + Integer
        
        return totalBytes;
    }
    
    /**
     * Logs cache statistics for performance monitoring.
     */
    private static void logCacheStatistics() {
        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        long evictions = cacheEvictions.get();
        long total = hits + misses;
        
        if (total > 0) {
            double hitRate = (hits * 100.0) / total;
            long memoryBytes = estimateCacheMemoryUsage();
            double memoryMB = memoryBytes / (1024.0 * 1024.0);
            
            LOGGER.info(
                "Cache Statistics - Size: {}/{}, Memory: {}MB, Hits: {}, Misses: {}, Hit Rate: {}%, Evictions: {}",
                GLOBAL_CACHE.size(), MAX_CACHE_SIZE, String.format("%.2f", memoryMB),
                hits, misses, String.format("%.2f", hitRate), evictions
            );
            LOGGER.info(
                "  ID Maps - Textures: {}, BlockStates: {}",
                TEXTURE_ID_MAP.size(), BLOCKSTATE_ID_MAP.size()
            );
            
            // Log most accessed entries (top 5)
            GLOBAL_CACHE.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(
                    e2.getValue().getAccessCount(),
                    e1.getValue().getAccessCount()))
                .limit(5)
                .forEach(entry -> LOGGER.debug(
                    "  Top cached config: {} (accessed {} times)",
                    entry.getKey().toString(), entry.getValue().getAccessCount()
                ));
        }
    }



    @Override
    public boolean useAmbientOcclusion() {
        return vanillaLeverModel.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return vanillaLeverModel.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return vanillaLeverModel.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    @Nonnull
    @SuppressWarnings("deprecation")
    public TextureAtlasSprite getParticleIcon() {

        TextureAtlasSprite stoneTexture = textureSprites.get("base_default");
        if (stoneTexture != null) {
            return stoneTexture;
        }

        return vanillaLeverModel.getParticleIcon();
    }

    @Override
    @Nonnull
    public ItemOverrides getOverrides() {
        return itemOverrides;
    }
    /** Returns render types including cutoutMipped for overlay textures with alpha transparency. */
    @Override
    @Nonnull
    public net.neoforged.neoforge.client.ChunkRenderTypeSet getRenderTypes(
            @Nonnull BlockState state, @Nonnull RandomSource rand, @Nonnull ModelData data) {
        return net.neoforged.neoforge.client.ChunkRenderTypeSet.of(
            RenderType.solid(), RenderType.cutoutMipped());
    }
    /**
     * Optimized cache key using integer IDs and bitpacked flags for memory efficiency.
     * Reduces memory footprint from ~200+ bytes to ~48 bytes per key.
     */
    private static class ModelCacheKey {
        private final int blockStateId;
        private final Direction side;
        private final int toggleTextureId;
        private final int baseTextureId;
        private final int stateFlags; // Bitpacked: powerMode(2 bits) + wallOrientation(3 bits) + rotations(6 bits) + ghost(1 bit)
        private final Float ghostOpacity;
        private final RenderType renderType;
        private final int hashCode;
        
        // Bit positions for state flags
        private static final int POWER_MODE_SHIFT = 0;    // Bits 0-1
        private static final int WALL_ORIENT_SHIFT = 2;   // Bits 2-4
        private static final int BASE_ROTATION_SHIFT = 5; // Bits 5-7
        private static final int TOGGLE_ROTATION_SHIFT = 8; // Bits 8-10
        private static final int GHOST_MODE_SHIFT = 11;   // Bit 11
        private static final int HAS_TOGGLE_BLOCK_SHIFT = 12; // Bit 12

        public ModelCacheKey(@Nonnull String blockStateString, @Nullable Direction side,
                            @Nullable String toggleTexture, @Nullable String baseTexture,
                            @Nullable String powerMode, @Nullable String wallOrientation,
                            @Nullable String baseRotation, @Nullable String toggleRotation,
                            @Nullable Boolean ghostMode, @Nullable Float ghostOpacity,
                            @Nullable Boolean hasToggleBlock,
                            @Nullable RenderType renderType) {
            // Convert strings to integer IDs for memory efficiency
            this.blockStateId = getBlockStateId(blockStateString);
            this.side = side;
            this.toggleTextureId = getTextureId(toggleTexture);
            this.baseTextureId = getTextureId(baseTexture);
            this.ghostOpacity = ghostOpacity;
            this.renderType = renderType;
            
            // Pack all state flags into a single integer
            int flags = 0;
            flags |= encodePowerMode(powerMode) << POWER_MODE_SHIFT;
            flags |= encodeWallOrientation(wallOrientation) << WALL_ORIENT_SHIFT;
            flags |= encodeRotation(baseRotation) << BASE_ROTATION_SHIFT;
            flags |= encodeRotation(toggleRotation) << TOGGLE_ROTATION_SHIFT;
            flags |= (ghostMode != null && ghostMode ? 1 : 0) << GHOST_MODE_SHIFT;
            flags |= (hasToggleBlock != null && hasToggleBlock ? 1 : 0) << HAS_TOGGLE_BLOCK_SHIFT;
            this.stateFlags = flags;

            this.hashCode = Objects.hash(blockStateId, side, toggleTextureId, baseTextureId, stateFlags, ghostOpacity, renderType);
        }
        
        // Encoding helpers for bitpacking
        private static int encodePowerMode(@Nullable String mode) {
            if (mode == null) return 0;
            return switch (mode) {
                case "DEFAULT" -> 0;
                case "ALT" -> 1;
                case "NONE" -> 2;
                default -> 0;
            };
        }
        
        private static int encodeWallOrientation(@Nullable String orientation) {
            if (orientation == null || "center".equals(orientation)) return 0;
            return switch (orientation) {
                case "top" -> 1;
                case "bottom" -> 2;
                case "left" -> 3;
                case "right" -> 4;
                default -> 0;
            };
        }
        
        private static int encodeRotation(@Nullable String rotation) {
            if (rotation == null || "NORMAL".equals(rotation)) return 0;
            return switch (rotation) {
                case "RIGHT" -> 1;      // 90° clockwise
                case "INVERT" -> 2;     // 180°
                case "LEFT" -> 3;       // -90° counterclockwise
                default -> 0;
            };
        }
        
        // Decoding helpers for toString and getWallOrientation
        private String decodeWallOrientation() {
            int encoded = (stateFlags >> WALL_ORIENT_SHIFT) & 0b111;
            return switch (encoded) {
                case 1 -> "top";
                case 2 -> "bottom";
                case 3 -> "left";
                case 4 -> "right";
                default -> "center";
            };
        }
        
        private boolean isGhostMode() {
            return ((stateFlags >> GHOST_MODE_SHIFT) & 1) == 1;
        }

        public boolean isDefaultTextures() {
            // Check if using default texture IDs (0 = null/default)
            boolean defaultTextures = (toggleTextureId == 0 || toggleTextureId == getTextureId(SwitchesLeverBlockEntity.DEFAULT_TOGGLE_TEXTURE)) &&
                                     (baseTextureId == 0 || baseTextureId == getTextureId(SwitchesLeverBlockEntity.DEFAULT_BASE_TEXTURE));
            
            // Check if using default flags (all 0 = defaults)
            int powerMode = (stateFlags >> POWER_MODE_SHIFT) & 0b11;
            int baseRotation = (stateFlags >> BASE_ROTATION_SHIFT) & 0b111;
            int toggleRotation = (stateFlags >> TOGGLE_ROTATION_SHIFT) & 0b111;
            
            return defaultTextures && powerMode == 0 && baseRotation == 0 && toggleRotation == 0;
        }

        @Nullable
        public String getWallOrientation() {
            return decodeWallOrientation();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof ModelCacheKey other)) return false;
            return blockStateId == other.blockStateId &&
                   Objects.equals(side, other.side) &&
                   toggleTextureId == other.toggleTextureId &&
                   baseTextureId == other.baseTextureId &&
                   stateFlags == other.stateFlags &&
                   Objects.equals(ghostOpacity, other.ghostOpacity) &&
                   Objects.equals(renderType, other.renderType);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("CacheKey[");
            if (isGhostMode()) {
                sb.append("ghost,opacity=").append(ghostOpacity);
            } else {
                // Decode texture IDs for display
                if (toggleTextureId != 0 && toggleTextureId != getTextureId(SwitchesLeverBlockEntity.DEFAULT_TOGGLE_TEXTURE)) {
                    sb.append("toggleId=").append(toggleTextureId).append(",");
                }
                if (baseTextureId != 0 && baseTextureId != getTextureId(SwitchesLeverBlockEntity.DEFAULT_BASE_TEXTURE)) {
                    sb.append("baseId=").append(baseTextureId).append(",");
                }
                
                // Decode power mode
                int powerMode = (stateFlags >> POWER_MODE_SHIFT) & 0b11;
                if (powerMode != 0) {
                    String mode = switch (powerMode) {
                        case 1 -> "ALT";
                        case 2 -> "NONE";
                        default -> "DEFAULT";
                    };
                    sb.append("power=").append(mode).append(",");
                }
            }
            
            // Decode wall orientation
            String wallOrient = decodeWallOrientation();
            if (!"center".equals(wallOrient)) {
                sb.append("wall=").append(wallOrient).append(",");
            }
            
            if (side != null) {
                sb.append("side=").append(side.getName());
            }
            
            sb.append(",stateId=").append(blockStateId);
            return sb.append("]").toString();
        }
    }
}
