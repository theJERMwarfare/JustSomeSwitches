package net.justsomeswitches.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.justsomeswitches.blockentity.SwitchesLeverBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Block Entity Renderer for Switches Lever with complete texture replacement
 * ---
 * FUNCTIONAL: Applies custom textures through rendering pipeline
 */
public class SwitchesLeverRenderer implements BlockEntityRenderer<SwitchesLeverBlockEntity> {

    private final BlockRenderDispatcher blockRenderer;

    public SwitchesLeverRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
        System.out.println("DEBUG Renderer: SwitchesLeverRenderer created");
    }

    @Override
    public void render(@Nonnull SwitchesLeverBlockEntity blockEntity, float partialTick,
                       @Nonnull PoseStack poseStack, @Nonnull MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {

        // Debug output for texture state
        System.out.println("DEBUG Renderer: Rendering switch with custom textures - " +
                "Base: " + blockEntity.getBaseFaceSelection() +
                ", Toggle: " + blockEntity.getToggleFaceSelection());
        System.out.println("DEBUG Renderer: Block Entity Renderer is working!");

        BlockState blockState = blockEntity.getBlockState();

        if (blockEntity.hasCustomTextures()) {
            System.out.println("DEBUG Renderer: Has custom textures - applying modifications");
            System.out.println("DEBUG Renderer: Base texture: " + blockEntity.getBaseTexture());
            System.out.println("DEBUG Renderer: Toggle texture: " + blockEntity.getToggleTexture());

            renderWithCustomTextures(blockEntity, blockState, poseStack, bufferSource,
                    packedLight, packedOverlay);
        } else {
            System.out.println("DEBUG Renderer: No custom textures - using vanilla rendering");
            renderVanilla(blockState, poseStack, bufferSource, packedLight, packedOverlay);
        }
    }

    /**
     * Render with custom texture replacement
     */
    private void renderWithCustomTextures(@Nonnull SwitchesLeverBlockEntity blockEntity,
                                          @Nonnull BlockState blockState,
                                          @Nonnull PoseStack poseStack,
                                          @Nonnull MultiBufferSource bufferSource,
                                          int packedLight, int packedOverlay) {

        System.out.println("DEBUG Renderer: Applying custom textures - Base: " +
                blockEntity.getBaseTexture() + ", Toggle: " + blockEntity.getToggleTexture());

        // Get the base model
        BakedModel baseModel = blockRenderer.getBlockModel(blockState);

        // Get vertex consumer for solid rendering
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.solid());

        // Get base and toggle textures
        TextureAtlasSprite baseSprite = getTextureSprite(blockEntity.getBaseTexture());
        TextureAtlasSprite toggleSprite = getTextureSprite(blockEntity.getToggleTexture());

        System.out.println("DEBUG Renderer: Loaded sprites - Base: " + baseSprite.contents().name() +
                ", Toggle: " + toggleSprite.contents().name());

        // Process all quads for all faces (including null for general quads)
        RandomSource random = RandomSource.create();

        for (Direction face : Direction.values()) {
            List<BakedQuad> quads = baseModel.getQuads(blockState, face, random);
            processQuads(quads, poseStack, vertexConsumer, baseSprite, toggleSprite,
                    packedLight, packedOverlay);
        }

        // Process general quads (not face-specific)
        List<BakedQuad> generalQuads = baseModel.getQuads(blockState, null, random);
        processQuads(generalQuads, poseStack, vertexConsumer, baseSprite, toggleSprite,
                packedLight, packedOverlay);

        System.out.println("DEBUG Renderer: Custom texture rendering completed");
    }

    /**
     * Process quads and replace textures
     */
    private void processQuads(@Nonnull List<BakedQuad> quads, @Nonnull PoseStack poseStack,
                              @Nonnull VertexConsumer vertexConsumer,
                              @Nonnull TextureAtlasSprite baseSprite,
                              @Nonnull TextureAtlasSprite toggleSprite,
                              int packedLight, int packedOverlay) {

        var lastPose = poseStack.last();
        Matrix4f pose = lastPose.pose();
        Matrix3f normal = lastPose.normal();

        for (BakedQuad quad : quads) {
            TextureAtlasSprite originalSprite = quad.getSprite();
            String originalTextureName = originalSprite.contents().name().toString();

            System.out.println("DEBUG Renderer: Processing quad with original texture: " + originalTextureName);

            // Determine replacement texture
            TextureAtlasSprite replacementSprite = determineReplacementTexture(
                    originalSprite, baseSprite, toggleSprite);

            if (replacementSprite != originalSprite) {
                System.out.println("DEBUG Renderer: Replacing " + originalTextureName +
                        " with " + replacementSprite.contents().name());

                // Create modified quad with new texture
                renderQuadWithCustomTexture(quad, replacementSprite, pose, normal,
                        vertexConsumer, packedLight, packedOverlay);
            } else {
                System.out.println("DEBUG Renderer: No replacement for " + originalTextureName);
                // Render original quad
                renderOriginalQuad(quad, pose, normal, vertexConsumer, packedLight, packedOverlay);
            }
        }
    }

    /**
     * Determine which replacement texture to use
     */
    @Nonnull
    private TextureAtlasSprite determineReplacementTexture(@Nonnull TextureAtlasSprite originalSprite,
                                                           @Nonnull TextureAtlasSprite baseSprite,
                                                           @Nonnull TextureAtlasSprite toggleSprite) {
        String originalName = originalSprite.contents().name().toString();

        // Exclude powered/unpowered state textures
        if (shouldExcludeFromReplacement(originalName)) {
            return originalSprite;
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
        return originalSprite;
    }

    /**
     * Check if texture should be excluded from replacement
     */
    private boolean shouldExcludeFromReplacement(@Nonnull String textureName) {
        return textureName.contains("redstone") ||
                textureName.contains("_on") ||
                textureName.contains("_off") ||
                textureName.contains("powered") ||
                textureName.contains("gray_concrete_powder");
    }

    /**
     * Check if this is a base texture
     */
    private boolean isBaseTexture(@Nonnull String textureName) {
        return textureName.contains("cobblestone") ||
                textureName.contains("stone") ||
                (textureName.contains("concrete") && !textureName.contains("gray_concrete_powder"));
    }

    /**
     * Check if this is a toggle texture
     */
    private boolean isToggleTexture(@Nonnull String textureName) {
        return textureName.contains("planks") ||
                textureName.contains("wood") ||
                (textureName.contains("log") && !shouldExcludeFromReplacement(textureName));
    }

    /**
     * Render quad with custom texture replacement
     */
    private void renderQuadWithCustomTexture(@Nonnull BakedQuad originalQuad,
                                             @Nonnull TextureAtlasSprite newSprite,
                                             @Nonnull Matrix4f pose, @Nonnull Matrix3f normal,
                                             @Nonnull VertexConsumer vertexConsumer,
                                             int packedLight, int packedOverlay) {

        // Get original vertex data
        int[] vertexData = originalQuad.getVertices();
        TextureAtlasSprite originalSprite = originalQuad.getSprite();

        // Transform vertex data to use new texture coordinates
        int[] newVertexData = transformVertexData(vertexData, originalSprite, newSprite);

        // Render the modified quad
        renderVertexData(newVertexData, pose, normal, vertexConsumer, packedLight, packedOverlay);
    }

    /**
     * Render original quad without modification
     */
    private void renderOriginalQuad(@Nonnull BakedQuad quad,
                                    @Nonnull Matrix4f pose, @Nonnull Matrix3f normal,
                                    @Nonnull VertexConsumer vertexConsumer,
                                    int packedLight, int packedOverlay) {
        int[] vertexData = quad.getVertices();
        renderVertexData(vertexData, pose, normal, vertexConsumer, packedLight, packedOverlay);
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
     * Transform U coordinate
     */
    private float transformU(float originalU, @Nonnull TextureAtlasSprite originalTexture,
                             @Nonnull TextureAtlasSprite newTexture) {
        float relativeU = (originalU - originalTexture.getU0()) / (originalTexture.getU1() - originalTexture.getU0());
        return newTexture.getU0() + relativeU * (newTexture.getU1() - newTexture.getU0());
    }

    /**
     * Transform V coordinate
     */
    private float transformV(float originalV, @Nonnull TextureAtlasSprite originalTexture,
                             @Nonnull TextureAtlasSprite newTexture) {
        float relativeV = (originalV - originalTexture.getV0()) / (originalTexture.getV1() - originalTexture.getV0());
        return newTexture.getV0() + relativeV * (newTexture.getV1() - newTexture.getV0());
    }

    /**
     * Render vertex data to vertex consumer
     */
    private void renderVertexData(@Nonnull int[] vertexData,
                                  @Nonnull Matrix4f pose, @Nonnull Matrix3f normal,
                                  @Nonnull VertexConsumer vertexConsumer,
                                  int packedLight, int packedOverlay) {

        // Render 4 vertices per quad
        for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
            int baseIndex = vertexIndex * 8;

            float x = Float.intBitsToFloat(vertexData[baseIndex]);
            float y = Float.intBitsToFloat(vertexData[baseIndex + 1]);
            float z = Float.intBitsToFloat(vertexData[baseIndex + 2]);

            int color = vertexData[baseIndex + 3];
            float u = Float.intBitsToFloat(vertexData[baseIndex + 4]);
            float v = Float.intBitsToFloat(vertexData[baseIndex + 5]);

            int normalData = vertexData[baseIndex + 6];
            float nx = ((normalData >> 0) & 0xFF) / 127.5f - 1.0f;
            float ny = ((normalData >> 8) & 0xFF) / 127.5f - 1.0f;
            float nz = ((normalData >> 16) & 0xFF) / 127.5f - 1.0f;

            // Extract color components
            int red = (color >> 16) & 0xFF;
            int green = (color >> 8) & 0xFF;
            int blue = color & 0xFF;
            int alpha = (color >> 24) & 0xFF;

            vertexConsumer.vertex(pose, x, y, z)
                    .color(red, green, blue, alpha)
                    .uv(u, v)
                    .overlayCoords(packedOverlay)
                    .uv2(packedLight)
                    .normal(normal, nx, ny, nz)
                    .endVertex();
        }
    }

    /**
     * Render vanilla without modifications
     */
    private void renderVanilla(@Nonnull BlockState blockState, @Nonnull PoseStack poseStack,
                               @Nonnull MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        BakedModel model = blockRenderer.getBlockModel(blockState);
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.solid());

        blockRenderer.getModelRenderer().renderModel(
                poseStack.last(), vertexConsumer, blockState, model, 1.0f, 1.0f, 1.0f,
                packedLight, packedOverlay);
    }

    /**
     * Get texture sprite from path
     */
    @Nonnull
    private TextureAtlasSprite getTextureSprite(@Nonnull String texturePath) {
        try {
            ResourceLocation textureLocation = new ResourceLocation(texturePath);
            return Minecraft.getInstance()
                    .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                    .apply(textureLocation);
        } catch (Exception e) {
            System.out.println("DEBUG Renderer: Error loading texture " + texturePath + " - " + e.getMessage());
            // Fallback to missing texture
            return Minecraft.getInstance()
                    .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                    .apply(new ResourceLocation("minecraft:missingno"));
        }
    }
}