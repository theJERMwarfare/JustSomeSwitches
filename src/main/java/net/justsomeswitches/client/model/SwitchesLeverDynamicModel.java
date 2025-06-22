package net.justsomeswitches.client.model;

import net.justsomeswitches.blockentity.SwitchesLeverBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * Dynamic Block Model for Switches Lever that generates models with custom textures at runtime
 * ---
 * Phase 3C PIVOT: This approach generates block models dynamically based on BlockEntity NBT data
 * instead of using Block Entity Renderer overlay approach.
 * ---
 * This system:
 * - Reads custom textures from BlockEntity when block is rendered
 * - Generates new model quads with custom texture coordinates
 * - Returns modified model to Minecraft's rendering pipeline
 * - Integrates seamlessly with vanilla block rendering
 */
public class SwitchesLeverDynamicModel implements BakedModel {

    private final BakedModel baseModel;
    private final BakedModel onModel;

    public SwitchesLeverDynamicModel(@Nonnull BakedModel baseModel, @Nonnull BakedModel onModel) {
        this.baseModel = baseModel;
        this.onModel = onModel;
    }

    @Override
    @Nonnull
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, @Nonnull RandomSource random) {
        // This method is called when Minecraft renders the block
        // We need to determine if we should use custom textures

        // For now, return the base model quads - we'll enhance this
        boolean isPowered = state != null && state.getValue(BlockStateProperties.POWERED);
        BakedModel sourceModel = isPowered ? onModel : baseModel;

        return sourceModel.getQuads(state, direction, random);
    }

    /**
     * Enhanced getQuads that can access world context for BlockEntity data
     * This is called by our custom model loader when world context is available
     */
    @Nonnull
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction,
                                    @Nonnull RandomSource random, @Nullable BlockGetter level,
                                    @Nullable net.minecraft.core.BlockPos pos) {

        if (level == null || pos == null || state == null) {
            // Fallback to vanilla behavior
            return getQuads(state, direction, random);
        }

        // Try to get BlockEntity data for custom textures
        var blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof SwitchesLeverBlockEntity switchEntity)) {
            // No custom data available, use vanilla
            return getQuads(state, direction, random);
        }

        // Check if we have custom textures
        if (!switchEntity.hasCustomTextures()) {
            // No custom textures, use vanilla
            return getQuads(state, direction, random);
        }

        // Generate custom model with BlockEntity textures
        return generateCustomQuads(state, direction, random, switchEntity);
    }

    /**
     * Generates model quads with custom textures from BlockEntity data
     */
    @Nonnull
    private List<BakedQuad> generateCustomQuads(@Nonnull BlockState state, @Nullable Direction direction,
                                                @Nonnull RandomSource random, @Nonnull SwitchesLeverBlockEntity blockEntity) {

        try {
            // Get the appropriate base model
            boolean isPowered = state.getValue(BlockStateProperties.POWERED);
            BakedModel sourceModel = isPowered ? onModel : baseModel;

            // Get original quads
            List<BakedQuad> originalQuads = sourceModel.getQuads(state, direction, random);

            // Get custom texture sprites
            String baseTexture = blockEntity.getBaseTexture();
            String toggleTexture = blockEntity.getToggleTexture();

            TextureAtlasSprite baseSprite = getTextureSprite(baseTexture);
            TextureAtlasSprite toggleSprite = getTextureSprite(toggleTexture);

            // Process quads to replace textures
            return processQuadsWithCustomTextures(originalQuads, baseSprite, toggleSprite);

        } catch (Exception e) {
            System.out.println("Phase 3C Debug: Error generating custom quads - " + e.getMessage());
            // Fallback to vanilla on error
            return getQuads(state, direction, random);
        }
    }

    /**
     * Processes model quads to replace textures with custom ones
     */
    @Nonnull
    private List<BakedQuad> processQuadsWithCustomTextures(@Nonnull List<BakedQuad> originalQuads,
                                                           @Nonnull TextureAtlasSprite baseSprite,
                                                           @Nonnull TextureAtlasSprite toggleSprite) {

        // For Phase 3C Step 1: Return original quads with debug logging
        // This will be enhanced to actually modify the quads in the next step

        System.out.println("Phase 3C Debug: processQuadsWithCustomTextures - Processing " + originalQuads.size() +
                " quads with base: " + baseSprite.contents().name() +
                ", toggle: " + toggleSprite.contents().name());

        // TODO: Implement actual quad texture replacement
        // For now, return original quads to verify the system is working
        return originalQuads;
    }

    /**
     * Gets a texture sprite from the texture atlas
     */
    @Nonnull
    private TextureAtlasSprite getTextureSprite(@Nonnull String texturePath) {
        try {
            ResourceLocation textureLocation = new ResourceLocation(texturePath);
            return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(textureLocation);
        } catch (Exception e) {
            // Return fallback sprite
            return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(new ResourceLocation("minecraft:block/stone"));
        }
    }

    // ========================================
    // BAKEDMODEL INTERFACE IMPLEMENTATIONS
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
        return false; // We handle rendering through the normal pipeline
    }

    @Override
    @Nonnull
    public TextureAtlasSprite getParticleIcon() {
        return baseModel.getParticleIcon();
    }

    @Override
    @Nonnull
    public ItemTransforms getTransforms() {
        return baseModel.getTransforms();
    }

    @Override
    @Nonnull
    public ItemOverrides getOverrides() {
        return baseModel.getOverrides();
    }
}