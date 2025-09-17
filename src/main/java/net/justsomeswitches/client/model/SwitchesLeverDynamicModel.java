package net.justsomeswitches.client.model;

import net.justsomeswitches.blockentity.SwitchesLeverBlockEntity;
import net.justsomeswitches.util.TextureRotation;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dynamic model for lever blocks with custom texture support.
 */
public class SwitchesLeverDynamicModel implements IDynamicBakedModel {
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
     * Creates rotation matrix for wall orientation.
     */
    @Nonnull
    private Matrix4f createWallOrientationMatrix(@Nonnull String wallOrientation, 
                                                 @Nonnull net.minecraft.core.Direction wallFace) {
        
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
    private final Map<ModelCacheKey, List<BakedQuad>> instanceCache = new HashMap<>();


    private static final Map<ModelCacheKey, List<BakedQuad>> GLOBAL_CACHE = new ConcurrentHashMap<>();


    private final Map<String, TextureAtlasSprite> textureSprites;
    private final BakedModel vanillaLeverModel;
    private final ItemOverrides itemOverrides;

    // Cache performance tracking
    private static final java.util.concurrent.atomic.AtomicInteger cacheOperations = 
            new java.util.concurrent.atomic.AtomicInteger(0);


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

        // Periodic cache cleanup
        if (cacheOperations.incrementAndGet() % 1000 == 0) {
            cleanupGlobalCache();
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

        List<BakedQuad> cached = instanceCache.get(key);
        if (cached != null) {
            return cached;
        }



        cached = GLOBAL_CACHE.get(key);
        if (cached != null) {
            instanceCache.put(key, cached);
            return cached;
        }

        return null;
    }


    private void cacheGeneratedQuads(@Nonnull ModelCacheKey key, @Nonnull List<BakedQuad> quads) {

        instanceCache.put(key, quads);


        if (isCommonConfiguration(key)) {
            GLOBAL_CACHE.put(key, quads);
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
            texturedQuads = applyCustomTextures(baseQuads, toggleTexture, baseTexture, faceSelection, powerMode, state, extraData);
        }

        if (state != null && isWallPlacement(state) && extraData != ModelData.EMPTY) {
            texturedQuads = applyWallOrientationRotations(texturedQuads, state, extraData);
        }

        return texturedQuads;
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
     * Applies custom texture replacement to quads.
     */
    @Nonnull
    private List<BakedQuad> applyCustomTextures(@Nonnull List<BakedQuad> baseQuads, 
                                               @Nullable String toggleTexture,
                                               @Nullable String baseTexture,
                                               @Nullable String faceSelection,
                                               @Nullable String powerMode,
                                               @Nullable BlockState state,
                                               @Nonnull ModelData extraData) {
        
        List<BakedQuad> texturedQuads = new ArrayList<>();
        for (BakedQuad quad : baseQuads) {
            BakedQuad processedQuad = processQuadWithCustomTextures(quad, toggleTexture, baseTexture, 
                                                                  faceSelection, powerMode, state, extraData);
            texturedQuads.add(processedQuad);
        }
        return texturedQuads;
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
                                                   @Nonnull ModelData extraData) {

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
        boolean isToggleTexturePart = isLeverMovingPart(originalTextureName);
        if (isToggleTexturePart && toggleTexture != null) {
            String rotationString = extraData.get(SwitchesLeverBlockEntity.TOGGLE_ROTATION);
            if (rotationString != null) {
                try {
                    rotation = TextureRotation.valueOf(rotationString);
                } catch (IllegalArgumentException e) {
                    rotation = TextureRotation.NORMAL;
                }
            }
        }
        
        // Apply processing if we need texture replacement OR rotation
        boolean needsTextureReplacement = (replacementSprite != originalSprite);
        boolean needsRotation = (rotation != null && rotation != TextureRotation.NORMAL);
        
        if (!needsTextureReplacement && !needsRotation) {
            return originalQuad;
        }
        
        // Use replacement sprite if available, otherwise use original sprite
        TextureAtlasSprite finalSprite = needsTextureReplacement ? replacementSprite : originalSprite;
        
        return replaceQuadTexture(originalQuad, finalSprite, rotation);
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
                                        @Nullable TextureRotation rotation) {
        
        int[] originalVertices = originalQuad.getVertices();
        TextureAtlasSprite originalSprite = originalQuad.getSprite();


        int[] newVertices = transformVertexData(originalVertices, originalSprite, newSprite, rotation);


        return new BakedQuad(
                newVertices,
                originalQuad.getTintIndex(),
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



    @Nonnull
    private String getTextureName(@Nonnull TextureAtlasSprite sprite) {
        try (var contents = sprite.contents()) {
            return contents.name().toString();
        } catch (Exception e) {
            return "unknown";
        }
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
    /**
     * Gets texture sprite from path string.
     */
    @Nullable
    private TextureAtlasSprite getTextureSprite(@Nullable String texturePath) {
        if (texturePath == null || texturePath.isEmpty()) {
            return null;
        }
        
        try {

            ResourceLocation textureLocation = new ResourceLocation(texturePath);

            for (TextureAtlasSprite sprite : textureSprites.values()) {
                try (var contents = sprite.contents()) {
                    if (contents.name().equals(textureLocation)) {
                        return sprite;
                    }
                } catch (Exception e) {
                    // Continue checking other sprites
                }
            }

            TextureAtlasSprite directSprite = textureSprites.get(texturePath);
            if (directSprite != null) {
                return directSprite;
            }

            String simplifiedKey = textureLocation.getPath().replace("/", "_");
            TextureAtlasSprite simplifiedSprite = textureSprites.get(simplifiedKey);
            if (simplifiedSprite != null) {
                return simplifiedSprite;
            }

            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            try {
                TextureAtlasSprite atlasSprite = minecraft.getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS)
                    .apply(textureLocation);
                if (atlasSprite != null) {
                    return atlasSprite;
                }
            } catch (Exception atlasException) {
                // Ignore exception
            }
            
        } catch (Exception e) {
            // Ignore exception
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
                renderType
        );
    }


    private static void cleanupGlobalCache() {
        if (GLOBAL_CACHE.size() > 10000) {

            int removeCount = GLOBAL_CACHE.size() / 5;
            GLOBAL_CACHE.entrySet().stream()
                    .limit(removeCount)
                    .map(Map.Entry::getKey)
                    .forEach(GLOBAL_CACHE::remove);
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

    /**
     * Cache key for memory efficiency and fast comparison.
     */
    private static class ModelCacheKey {
        private final String blockStateString;
        private final Direction side;
        private final String toggleTexture;
        private final String baseTexture;
        private final String powerMode;
        private final String wallOrientation;
        private final String baseRotation;
        private final String toggleRotation;
        private final Boolean ghostMode;
        private final Float ghostOpacity;

        private final RenderType renderType;
        private final int hashCode;

        public ModelCacheKey(@Nonnull String blockStateString, @Nullable Direction side,
                            @Nullable String toggleTexture, @Nullable String baseTexture,
                            @Nullable String powerMode, @Nullable String wallOrientation,
                            @Nullable String baseRotation, @Nullable String toggleRotation,
                            @Nullable Boolean ghostMode, @Nullable Float ghostOpacity,
                            @Nullable RenderType renderType) {
            this.blockStateString = blockStateString;
            this.side = side;
            this.toggleTexture = toggleTexture;
            this.baseTexture = baseTexture;
            this.powerMode = powerMode;
            this.wallOrientation = wallOrientation;
            this.baseRotation = baseRotation;
            this.toggleRotation = toggleRotation;
            this.ghostMode = ghostMode;
            this.ghostOpacity = ghostOpacity;

            this.renderType = renderType;

            this.hashCode = Objects.hash(blockStateString, side, toggleTexture, baseTexture, powerMode, wallOrientation, baseRotation, toggleRotation, ghostMode, ghostOpacity, renderType);
        }

        public boolean isDefaultTextures() {
            return (toggleTexture == null || toggleTexture.equals(SwitchesLeverBlockEntity.DEFAULT_TOGGLE_TEXTURE)) &&
                   (baseTexture == null || baseTexture.equals(SwitchesLeverBlockEntity.DEFAULT_BASE_TEXTURE)) &&
                   (powerMode == null || powerMode.equals("DEFAULT")) &&
                   (baseRotation == null || baseRotation.equals("NORMAL")) &&
                   (toggleRotation == null || toggleRotation.equals("NORMAL"));
        }

        @Nullable
        public String getWallOrientation() {
            return wallOrientation != null ? wallOrientation : "center";
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof ModelCacheKey other)) return false;
            return Objects.equals(blockStateString, other.blockStateString) &&
                   Objects.equals(side, other.side) &&
                   Objects.equals(toggleTexture, other.toggleTexture) &&
                   Objects.equals(baseTexture, other.baseTexture) &&
                   Objects.equals(powerMode, other.powerMode) &&
                   Objects.equals(wallOrientation, other.wallOrientation) &&
                   Objects.equals(baseRotation, other.baseRotation) &&
                   Objects.equals(toggleRotation, other.toggleRotation) &&
                   Objects.equals(ghostMode, other.ghostMode) &&
                   Objects.equals(ghostOpacity, other.ghostOpacity) &&
                   Objects.equals(renderType, other.renderType);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }
}
