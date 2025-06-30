package net.justsomeswitches.client.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Custom model for SwitchesLever that handles dynamic texture replacement
 * OPTIMIZED: Silent operation for performance
 */
public class SwitchesLeverModel implements BakedModel {

    private final BakedModel baseModel;
    private final BlockState representativeState;

    // Texture caching for performance
    private TextureAtlasSprite cachedBaseTexture;
    private TextureAtlasSprite cachedToggleTexture;
    private String cachedBaseTexturePath;
    private String cachedToggleTexturePath;

    public SwitchesLeverModel(@Nonnull BlockState state, @Nonnull BakedModel baseModel) {
        this.representativeState = state;
        this.baseModel = baseModel;
    }

    // ========================================
    // OPTIMIZED: SILENT TEXTURE REPLACEMENT
    // ========================================

    @Override
    @Nonnull
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull RandomSource rand, @Nonnull ModelData extraData, @Nullable RenderType renderType) {
        return processQuads(state, side, rand, extraData, renderType);
    }

    @Override
    @Nonnull
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull RandomSource rand) {
        // For item rendering, use default textures without ModelData
        return processQuads(state, side, rand, ModelData.EMPTY, null);
    }

    /**
     * OPTIMIZED: Centralized quad processing with silent operation
     */
    @Nonnull
    private List<BakedQuad> processQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull RandomSource rand, @Nonnull ModelData extraData, @Nullable RenderType renderType) {
        // Get base model quads
        List<BakedQuad> baseQuads = baseModel.getQuads(state, side, rand, extraData, renderType);

        // Check if we have texture data
        net.justsomeswitches.blockentity.SwitchesLeverBlockEntity.SwitchTextureData textureData =
                extraData.get(net.justsomeswitches.blockentity.SwitchesLeverBlockEntity.TEXTURE_PROPERTY);

        if (textureData != null && textureData.hasCustomTextures()) {
            return replaceTexturesInQuads(baseQuads, textureData);
        }

        return baseQuads;
    }

    /**
     * Replace textures in the model quads based on texture data
     */
    @Nonnull
    private List<BakedQuad> replaceTexturesInQuads(@Nonnull List<BakedQuad> baseQuads, @Nonnull net.justsomeswitches.blockentity.SwitchesLeverBlockEntity.SwitchTextureData textureData) {
        // Load texture sprites (with caching)
        TextureAtlasSprite baseSprite = getTextureSprite(textureData.getBaseTexture(), true);
        TextureAtlasSprite toggleSprite = getTextureSprite(textureData.getToggleTexture(), false);

        // Transform quads with new textures
        return baseQuads.stream()
                .map(quad -> replaceQuadTexture(quad, baseSprite, toggleSprite))
                .toList();
    }

    /**
     * Replace texture in a single quad
     */
    @Nonnull
    private BakedQuad replaceQuadTexture(@Nonnull BakedQuad originalQuad,
                                         @Nonnull TextureAtlasSprite baseSprite,
                                         @Nonnull TextureAtlasSprite toggleSprite) {

        // Get original texture from quad
        TextureAtlasSprite originalTexture = originalQuad.getSprite();

        // Determine which replacement texture to use based on original texture
        TextureAtlasSprite replacementSprite = determineReplacementTexture(originalTexture, baseSprite, toggleSprite);

        // If no replacement needed, return original
        if (replacementSprite == originalTexture) {
            return originalQuad;
        }

        // Create new quad with replaced texture
        return new BakedQuad(
                transformVertexData(originalQuad.getVertices(), originalTexture, replacementSprite),
                originalQuad.getTintIndex(),
                originalQuad.getDirection(),
                replacementSprite,
                originalQuad.isShade()
        );
    }

    /**
     * Determine which replacement texture to use for the original texture
     */
    @Nonnull
    private TextureAtlasSprite determineReplacementTexture(@Nonnull TextureAtlasSprite originalTexture,
                                                           @Nonnull TextureAtlasSprite baseSprite,
                                                           @Nonnull TextureAtlasSprite toggleSprite) {

        String originalName = originalTexture.contents().name().toString();

        // Exclude powered/unpowered state textures
        if (shouldExcludeFromReplacement(originalName)) {
            return originalTexture;
        }

        // Check if this is a base texture (cobblestone-like)
        if (isBaseTexture(originalName)) {
            return baseSprite;
        }

        // Check if this is a toggle texture (wood-like)
        if (isToggleTexture(originalName)) {
            return toggleSprite;
        }

        // Default: no replacement
        return originalTexture;
    }

    /**
     * Check if texture should be excluded from replacement (powered/unpowered states)
     */
    private boolean shouldExcludeFromReplacement(@Nonnull String textureName) {
        // Exclude redstone-related textures that represent powered states
        return textureName.contains("redstone") ||
                textureName.contains("_on") ||
                textureName.contains("_off") ||
                textureName.contains("powered") ||
                textureName.contains("gray_concrete_powder"); // Switch mechanism parts
    }

    /**
     * Check if this is a base texture (should be replaced with base texture from GUI)
     */
    private boolean isBaseTexture(@Nonnull String textureName) {
        // Cobblestone and stone-like textures are base textures
        return textureName.contains("cobblestone") ||
                textureName.contains("stone") ||
                textureName.contains("concrete") && !textureName.contains("gray_concrete_powder");
    }

    /**
     * Check if this is a toggle texture (should be replaced with toggle texture from GUI)
     */
    private boolean isToggleTexture(@Nonnull String textureName) {
        // Wood-like textures are toggle textures
        return textureName.contains("planks") ||
                textureName.contains("wood") ||
                textureName.contains("log") &&
                        // Make sure it's not a powered/unpowered state texture
                        !shouldExcludeFromReplacement(textureName);
    }

    /**
     * Transform vertex data to use new texture coordinates
     */
    @Nonnull
    private int[] transformVertexData(@Nonnull int[] originalVertices,
                                      @Nonnull TextureAtlasSprite originalTexture,
                                      @Nonnull TextureAtlasSprite newTexture) {

        int[] newVertices = originalVertices.clone();

        // Transform UV coordinates for each vertex (4 vertices per quad)
        for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
            int baseIndex = vertexIndex * 8; // 8 integers per vertex

            // Get original UV coordinates
            float originalU = Float.intBitsToFloat(originalVertices[baseIndex + 4]);
            float originalV = Float.intBitsToFloat(originalVertices[baseIndex + 5]);

            // Transform UV coordinates to new texture
            float newU = transformU(originalU, originalTexture, newTexture);
            float newV = transformV(originalV, originalTexture, newTexture);

            // Update vertex data
            newVertices[baseIndex + 4] = Float.floatToIntBits(newU);
            newVertices[baseIndex + 5] = Float.floatToIntBits(newV);
        }

        return newVertices;
    }

    /**
     * Transform U coordinate from original texture to new texture
     */
    private float transformU(float originalU, @Nonnull TextureAtlasSprite originalTexture, @Nonnull TextureAtlasSprite newTexture) {
        // Calculate relative position within original texture
        float relativeU = (originalU - originalTexture.getU0()) / (originalTexture.getU1() - originalTexture.getU0());

        // Map to new texture coordinates
        return newTexture.getU0() + relativeU * (newTexture.getU1() - newTexture.getU0());
    }

    /**
     * Transform V coordinate from original texture to new texture
     */
    private float transformV(float originalV, @Nonnull TextureAtlasSprite originalTexture, @Nonnull TextureAtlasSprite newTexture) {
        // Calculate relative position within original texture
        float relativeV = (originalV - originalTexture.getV0()) / (originalTexture.getV1() - originalTexture.getV0());

        // Map to new texture coordinates
        return newTexture.getV0() + relativeV * (newTexture.getV1() - newTexture.getV0());
    }

    /**
     * Get texture sprite with caching for performance
     */
    @Nonnull
    private TextureAtlasSprite getTextureSprite(@Nonnull String texturePath, boolean isBase) {
        // Check cache first
        if (isBase && texturePath.equals(cachedBaseTexturePath) && cachedBaseTexture != null) {
            return cachedBaseTexture;
        }
        if (!isBase && texturePath.equals(cachedToggleTexturePath) && cachedToggleTexture != null) {
            return cachedToggleTexture;
        }

        // Load new texture sprite
        TextureAtlasSprite sprite = loadTextureSprite(texturePath);

        // Cache for future use
        if (isBase) {
            cachedBaseTexture = sprite;
            cachedBaseTexturePath = texturePath;
        } else {
            cachedToggleTexture = sprite;
            cachedToggleTexturePath = texturePath;
        }

        return sprite;
    }

    /**
     * Load texture sprite from resource location
     */
    @Nonnull
    private TextureAtlasSprite loadTextureSprite(@Nonnull String texturePath) {
        try {
            ResourceLocation textureLocation = new ResourceLocation(texturePath);
            return Minecraft.getInstance()
                    .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                    .apply(textureLocation);
        } catch (Exception e) {
            // Fallback to missing texture
            return Minecraft.getInstance()
                    .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                    .apply(new ResourceLocation("minecraft:missingno"));
        }
    }

    // ========================================
    // DELEGATED METHODS TO BASE MODEL
    // ========================================

    @Override
    public boolean useAmbientOcclusion() {
        return baseModel.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return baseModel.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return baseModel.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return false; // We handle rendering through quads
    }

    @Override
    @Nonnull
    public TextureAtlasSprite getParticleIcon() {
        return baseModel.getParticleIcon();
    }

    @Override
    @Nonnull
    public BakedModel applyTransform(@Nonnull ItemDisplayContext displayContext,
                                     @Nonnull com.mojang.blaze3d.vertex.PoseStack poseStack,
                                     boolean applyLeftHandTransform) {
        return baseModel.applyTransform(displayContext, poseStack, applyLeftHandTransform);
    }

    @Override
    @NotNull
    public ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
        return baseModel.getRenderTypes(state, rand, data);
    }

    @Override
    @NotNull
    public ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData) {
        // Get BlockEntity and request its ModelData
        if (level.getBlockEntity(pos) instanceof net.justsomeswitches.blockentity.SwitchesLeverBlockEntity blockEntity) {
            ModelData entityData = blockEntity.getModelData();

            // Combine base model data with BlockEntity data
            return ModelData.builder()
                    .with(net.justsomeswitches.blockentity.SwitchesLeverBlockEntity.TEXTURE_PROPERTY,
                            entityData.get(net.justsomeswitches.blockentity.SwitchesLeverBlockEntity.TEXTURE_PROPERTY))
                    .build();
        }

        // Fallback to base model
        return baseModel.getModelData(level, pos, state, modelData);
    }

    @Override
    @Nonnull
    public ItemOverrides getOverrides() {
        return baseModel.getOverrides();
    }
}