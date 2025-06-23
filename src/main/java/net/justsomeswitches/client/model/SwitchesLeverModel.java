package net.justsomeswitches.client.model;

import net.justsomeswitches.blockentity.SwitchesLeverBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
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
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Custom Model for Switches Lever with Dynamic Texture Replacement
 * ---
 * Phase 3C: Simplified Model-Based Texture System
 * This model integrates with vanilla rendering to replace textures dynamically
 * based on BlockEntity data without causing z-fighting issues.
 */
public class SwitchesLeverModel implements BakedModel {

    private final BakedModel baseModel;
    private final BlockState modelState;

    // Texture cache for performance
    private TextureAtlasSprite cachedBaseTexture = null;
    private TextureAtlasSprite cachedToggleTexture = null;
    private String cachedBaseTexturePath = "";
    private String cachedToggleTexturePath = "";

    public SwitchesLeverModel(BlockState state, BakedModel baseModel) {
        this.modelState = state;
        this.baseModel = baseModel;
    }

    @Override
    @Nonnull
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, @Nonnull RandomSource random) {
        return getQuads(state, direction, random, ModelData.EMPTY, null);
    }

    @Override
    @Nonnull
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, @Nonnull RandomSource random,
                                    @Nonnull ModelData modelData, @Nullable RenderType renderType) {

        // Get texture data from ModelData
        net.justsomeswitches.blockentity.SwitchesLeverBlockEntity.SwitchTextureData textureData =
                modelData.get(net.justsomeswitches.blockentity.SwitchesLeverBlockEntity.TEXTURE_PROPERTY);

        // Debug output
        if (textureData != null) {
            System.out.println("Phase 3C Debug: Custom model getQuads - Has texture data: Base=" +
                    textureData.getBaseTexture() + ", Toggle=" + textureData.getToggleTexture() +
                    ", HasCustom=" + textureData.hasCustomTextures());
        } else {
            System.out.println("Phase 3C Debug: Custom model getQuads - No texture data in ModelData");
        }

        // Get base model quads
        List<BakedQuad> baseQuads = baseModel.getQuads(state, direction, random, modelData, renderType);
        System.out.println("Phase 3C Debug: Base model returned " + baseQuads.size() + " quads");

        // If no custom textures, use base model
        if (textureData == null || !textureData.hasCustomTextures()) {
            return baseQuads;
        }

        // Replace textures in quads
        List<BakedQuad> modifiedQuads = replaceTexturesInQuads(baseQuads, textureData);
        System.out.println("Phase 3C Debug: Texture replacement completed, returning " + modifiedQuads.size() + " modified quads");

        return modifiedQuads;
    }

    /**
     * Replace textures in the model quads based on texture data
     */
    @Nonnull
    private List<BakedQuad> replaceTexturesInQuads(@Nonnull List<BakedQuad> baseQuads, @Nonnull net.justsomeswitches.blockentity.SwitchesLeverBlockEntity.SwitchTextureData textureData) {
        System.out.println("Phase 3C Debug: Starting texture replacement - " + baseQuads.size() + " input quads");

        // Load texture sprites (with caching)
        TextureAtlasSprite baseSprite = getTextureSprite(textureData.getBaseTexture(), true);
        TextureAtlasSprite toggleSprite = getTextureSprite(textureData.getToggleTexture(), false);

        System.out.println("Phase 3C Debug: Loaded sprites - Base: " + baseSprite.contents().name() +
                ", Toggle: " + toggleSprite.contents().name());

        // Transform quads with new textures
        List<BakedQuad> result = baseQuads.stream()
                .map(quad -> replaceQuadTexture(quad, baseSprite, toggleSprite))
                .toList();

        System.out.println("Phase 3C Debug: Texture replacement completed - " + result.size() + " output quads");
        return result;
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

        // Debug output for original texture
        String originalName = originalTexture.contents().name().toString();
        System.out.println("Phase 3C Debug: Processing quad with texture: " + originalName);

        // Determine which replacement texture to use based on original texture
        TextureAtlasSprite replacementSprite = determineReplacementTexture(originalTexture, baseSprite, toggleSprite);

        // If no replacement needed, return original
        if (replacementSprite == originalTexture) {
            System.out.println("Phase 3C Debug: No replacement needed for texture: " + originalName);
            return originalQuad;
        }

        System.out.println("Phase 3C Debug: Replacing texture " + originalName + " with " + replacementSprite.contents().name());

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

        // Check if this is a base texture (cobblestone-like)
        String originalName = originalTexture.contents().name().toString();
        System.out.println("Phase 3C Debug: Determining replacement for: " + originalName);

        // More comprehensive texture matching
        if (originalName.contains("cobblestone") || originalName.contains("stone") ||
                originalName.contains("base") || originalName.contains("bottom")) {
            System.out.println("Phase 3C Debug: Identified as base texture -> replacing with " + baseSprite.contents().name());
            return baseSprite;
        }

        // Check if this is a toggle texture (wood-like)
        if (originalName.contains("oak") || originalName.contains("planks") || originalName.contains("wood") ||
                originalName.contains("toggle") || originalName.contains("lever") || originalName.contains("top")) {
            System.out.println("Phase 3C Debug: Identified as toggle texture -> replacing with " + toggleSprite.contents().name());
            return toggleSprite;
        }

        // Try to match by typical switch texture patterns
        if (originalName.contains("switches") || originalName.contains("lever")) {
            // If it contains switch/lever, try to determine if it's base or toggle part
            if (originalName.contains("base") || originalName.contains("bottom")) {
                System.out.println("Phase 3C Debug: Switch base texture -> replacing with " + baseSprite.contents().name());
                return baseSprite;
            } else {
                System.out.println("Phase 3C Debug: Switch toggle texture -> replacing with " + toggleSprite.contents().name());
                return toggleSprite;
            }
        }

        // Default: no replacement
        System.out.println("Phase 3C Debug: No pattern match - keeping original texture");
        return originalTexture;
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
            System.out.println("Phase 3C Debug: Custom model requesting ModelData from BlockEntity at " + pos);

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
    public net.minecraft.client.renderer.block.model.ItemOverrides getOverrides() {
        return baseModel.getOverrides();
    }
}