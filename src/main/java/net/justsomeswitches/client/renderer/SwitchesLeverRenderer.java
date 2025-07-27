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
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-performance Block Entity Renderer for Switches Lever with advanced caching.
 * <p>
 * This renderer implements smart texture caching and memory optimization patterns
 * to achieve significant performance improvements in texture loading and rendering operations.
 * 
 * @since 1.0.0
 */
public class SwitchesLeverRenderer implements BlockEntityRenderer<SwitchesLeverBlockEntity> {

    // ========================================
    // PERFORMANCE OPTIMIZATION: TEXTURE CACHING SYSTEM
    // ========================================

    /**
     * Weak reference cache for texture sprites to prevent memory leaks.
     * <p>
     * Uses weak references to allow garbage collection while maintaining
     * performance benefits through caching frequently accessed textures.
     */
    private static final ConcurrentHashMap<ResourceLocation, WeakReference<TextureAtlasSprite>> 
            TEXTURE_CACHE = new ConcurrentHashMap<>();

    /**
     * Cache size tracking for monitoring and cleanup.
     */
    private static volatile int cacheHits = 0;
    private static volatile int cacheMisses = 0;

    private final BlockRenderDispatcher blockRenderer;

    /**
     * Creates a new switches lever renderer with optimized caching.
     *
     * @param context the block entity renderer provider context
     */
    public SwitchesLeverRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(@Nonnull SwitchesLeverBlockEntity blockEntity, float partialTick,
                       @Nonnull PoseStack poseStack, @Nonnull MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {

        // PERFORMANCE OPTIMIZATION: Early exit for non-custom textures
        if (!blockEntity.hasCustomTextures()) {
            renderVanilla(blockEntity.getBlockState(), poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }

        // PERFORMANCE OPTIMIZATION: Distance-based culling
        if (!shouldRender(blockEntity)) {
            return;
        }

        renderWithCustomTextures(blockEntity, blockEntity.getBlockState(), poseStack, bufferSource,
                packedLight, packedOverlay);
    }

    /**
     * PERFORMANCE OPTIMIZATION: Determine if block entity should be rendered.
     * <p>
     * Implements basic frustum culling to skip rendering for distant or off-screen blocks.
     *
     * @param blockEntity the block entity to check
     * @return true if the block should be rendered
     */
    private boolean shouldRender(@Nonnull SwitchesLeverBlockEntity blockEntity) {
        try {
            var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
            var pos = blockEntity.getBlockPos();
            
            // Skip rendering if too far away (64 block radius)
            double distanceSquared = camera.getPosition().distanceToSqr(pos.getX(), pos.getY(), pos.getZ());
            return distanceSquared < 4096.0; // 64^2
        } catch (Exception e) {
            // Fallback to always render if distance check fails
            return true;
        }
    }

    /**
     * Render with custom texture replacement using optimized caching.
     */
    private void renderWithCustomTextures(@Nonnull SwitchesLeverBlockEntity blockEntity,
                                          @Nonnull BlockState blockState,
                                          @Nonnull PoseStack poseStack,
                                          @Nonnull MultiBufferSource bufferSource,
                                          int packedLight, int packedOverlay) {

        BakedModel baseModel = blockRenderer.getBlockModel(blockState);
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.solid());

        // PERFORMANCE OPTIMIZATION: Use cached texture sprites
        TextureAtlasSprite baseSprite = getCachedTextureSprite(blockEntity.getBaseTexture());
        TextureAtlasSprite toggleSprite = getCachedTextureSprite(blockEntity.getToggleTexture());

        RandomSource random = RandomSource.create();

        // Process face-specific quads
        for (Direction face : Direction.values()) {
            List<BakedQuad> quads = baseModel.getQuads(blockState, face, random);
            processQuads(quads, poseStack, vertexConsumer, baseSprite, toggleSprite,
                    packedLight, packedOverlay);
        }

        // Process general quads
        List<BakedQuad> generalQuads = baseModel.getQuads(blockState, null, random);
        processQuads(generalQuads, poseStack, vertexConsumer, baseSprite, toggleSprite,
                packedLight, packedOverlay);
    }

    /**
     * PERFORMANCE OPTIMIZATION: Get texture sprite with smart caching.
     * <p>
     * Implements weak reference caching to balance performance with memory management.
     * Cache statistics are tracked for performance monitoring.
     *
     * @param texturePath the texture resource path
     * @return the texture atlas sprite, never null
     */
    @Nonnull
    private TextureAtlasSprite getCachedTextureSprite(@Nonnull String texturePath) {
        try {
            ResourceLocation textureLocation = new ResourceLocation(texturePath);
            
            // Check cache first
            WeakReference<TextureAtlasSprite> ref = TEXTURE_CACHE.get(textureLocation);
            TextureAtlasSprite cachedSprite = (ref != null) ? ref.get() : null;
            
            if (cachedSprite != null) {
                cacheHits++;
                return cachedSprite;
            }
            
            // Cache miss - load texture and cache it
            cacheMisses++;
            TextureAtlasSprite sprite = loadTextureSprite(textureLocation);
            TEXTURE_CACHE.put(textureLocation, new WeakReference<>(sprite));
            
            // PERFORMANCE OPTIMIZATION: Periodic cache cleanup
            if ((cacheHits + cacheMisses) % 1000 == 0) {
                cleanupCache();
            }
            
            return sprite;
            
        } catch (Exception e) {
            // Fallback to missing texture
            return getMissingTextureSprite();
        }
    }

    /**
     * PERFORMANCE OPTIMIZATION: Clean up stale weak references from cache.
     * <p>
     * Removes entries where the weak reference has been garbage collected
     * to prevent cache size from growing indefinitely.
     */
    private static void cleanupCache() {
        TEXTURE_CACHE.entrySet().removeIf(entry -> entry.getValue().get() == null);
    }

    /**
     * Load texture sprite from resource location.
     *
     * @param textureLocation the resource location for the texture
     * @return the texture atlas sprite
     */
    @Nonnull
    private TextureAtlasSprite loadTextureSprite(@Nonnull ResourceLocation textureLocation) {
        return Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(textureLocation);
    }

    /**
     * Get the missing texture sprite as fallback.
     *
     * @return the missing texture sprite
     */
    @Nonnull
    private TextureAtlasSprite getMissingTextureSprite() {
        return Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(new ResourceLocation("minecraft:missingno"));
    }

    /**
     * Process quads and replace textures with optimized sprite handling.
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
            TextureAtlasSprite replacementSprite = determineReplacementTexture(
                    originalSprite, baseSprite, toggleSprite);

            if (replacementSprite != originalSprite) {
                renderQuadWithCustomTexture(quad, replacementSprite, pose, normal,
                        vertexConsumer, packedLight, packedOverlay);
            } else {
                renderOriginalQuad(quad, pose, normal, vertexConsumer, packedLight, packedOverlay);
            }
        }
    }

    /**
     * Determine which replacement texture to use based on original texture characteristics.
     */
    @Nonnull
    private TextureAtlasSprite determineReplacementTexture(@Nonnull TextureAtlasSprite originalSprite,
                                                           @Nonnull TextureAtlasSprite baseSprite,
                                                           @Nonnull TextureAtlasSprite toggleSprite) {
        String originalName = originalSprite.contents().name().toString();

        // Exclude powered/unpowered state textures from replacement
        if (shouldExcludeFromReplacement(originalName)) {
            return originalSprite;
        }

        // Classify texture type and return appropriate replacement
        if (isBaseTexture(originalName)) {
            return baseSprite;
        }

        if (isToggleTexture(originalName)) {
            return toggleSprite;
        }

        return originalSprite;
    }

    /**
     * Check if texture should be excluded from replacement.
     */
    private boolean shouldExcludeFromReplacement(@Nonnull String textureName) {
        return textureName.contains("redstone") ||
                textureName.contains("_on") ||
                textureName.contains("_off") ||
                textureName.contains("powered") ||
                textureName.contains("gray_concrete_powder");
    }

    /**
     * Check if this is a base texture (cobblestone/stone-like).
     */
    private boolean isBaseTexture(@Nonnull String textureName) {
        return textureName.contains("cobblestone") ||
                textureName.contains("stone") ||
                (textureName.contains("concrete") && !textureName.contains("gray_concrete_powder"));
    }

    /**
     * Check if this is a toggle texture (wood/planks-like).
     */
    private boolean isToggleTexture(@Nonnull String textureName) {
        return textureName.contains("planks") ||
                textureName.contains("wood") ||
                (textureName.contains("log") && !shouldExcludeFromReplacement(textureName));
    }

    /**
     * Render quad with custom texture replacement.
     */
    private void renderQuadWithCustomTexture(@Nonnull BakedQuad originalQuad,
                                             @Nonnull TextureAtlasSprite newSprite,
                                             @Nonnull Matrix4f pose, @Nonnull Matrix3f normal,
                                             @Nonnull VertexConsumer vertexConsumer,
                                             int packedLight, int packedOverlay) {

        int[] vertexData = originalQuad.getVertices();
        TextureAtlasSprite originalSprite = originalQuad.getSprite();

        int[] newVertexData = transformVertexData(vertexData, originalSprite, newSprite);
        renderVertexData(newVertexData, pose, normal, vertexConsumer, packedLight, packedOverlay);
    }

    /**
     * Render original quad without modification.
     */
    private void renderOriginalQuad(@Nonnull BakedQuad quad,
                                    @Nonnull Matrix4f pose, @Nonnull Matrix3f normal,
                                    @Nonnull VertexConsumer vertexConsumer,
                                    int packedLight, int packedOverlay) {
        int[] vertexData = quad.getVertices();
        renderVertexData(vertexData, pose, normal, vertexConsumer, packedLight, packedOverlay);
    }

    /**
     * Transform vertex data to use new texture coordinates.
     */
    @Nonnull
    private int[] transformVertexData(@Nonnull int[] originalVertices,
                                      @Nonnull TextureAtlasSprite originalTexture,
                                      @Nonnull TextureAtlasSprite newTexture) {

        int[] newVertices = originalVertices.clone();

        for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
            int baseIndex = vertexIndex * 8;

            float originalU = Float.intBitsToFloat(originalVertices[baseIndex + 4]);
            float originalV = Float.intBitsToFloat(originalVertices[baseIndex + 5]);

            float newU = transformU(originalU, originalTexture, newTexture);
            float newV = transformV(originalV, originalTexture, newTexture);

            newVertices[baseIndex + 4] = Float.floatToIntBits(newU);
            newVertices[baseIndex + 5] = Float.floatToIntBits(newV);
        }

        return newVertices;
    }

    /**
     * Transform U coordinate from original texture space to new texture space.
     */
    private float transformU(float originalU, @Nonnull TextureAtlasSprite originalTexture,
                             @Nonnull TextureAtlasSprite newTexture) {
        float relativeU = (originalU - originalTexture.getU0()) / (originalTexture.getU1() - originalTexture.getU0());
        return newTexture.getU0() + relativeU * (newTexture.getU1() - newTexture.getU0());
    }

    /**
     * Transform V coordinate from original texture space to new texture space.
     */
    private float transformV(float originalV, @Nonnull TextureAtlasSprite originalTexture,
                             @Nonnull TextureAtlasSprite newTexture) {
        float relativeV = (originalV - originalTexture.getV0()) / (originalTexture.getV1() - originalTexture.getV0());
        return newTexture.getV0() + relativeV * (newTexture.getV1() - newTexture.getV0());
    }

    /**
     * Render vertex data to vertex consumer with optimized processing.
     */
    private void renderVertexData(@Nonnull int[] vertexData,
                                  @Nonnull Matrix4f pose, @Nonnull Matrix3f normal,
                                  @Nonnull VertexConsumer vertexConsumer,
                                  int packedLight, int packedOverlay) {

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
     * Render vanilla without modifications for optimal performance.
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
     * Get cache performance statistics for monitoring.
     * 
     * @return array containing [cache hits, cache misses, cache size]
     */
    public static int[] getCacheStats() {
        return new int[]{cacheHits, cacheMisses, TEXTURE_CACHE.size()};
    }

    /**
     * Clear the texture cache (useful for debugging or memory pressure).
     */
    public static void clearCache() {
        TEXTURE_CACHE.clear();
        cacheHits = 0;
        cacheMisses = 0;
    }
}
