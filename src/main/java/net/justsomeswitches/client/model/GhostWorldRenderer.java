package net.justsomeswitches.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.justsomeswitches.blockentity.SwitchesLeverBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * World rendering system for ghost block previews.
 *
 * This class handles:
 * - Rendering ghost blocks during the translucent rendering stage
 * - Using existing SwitchesLeverDynamicModel for consistent appearance
 * - Proper transparency handling with OpenGL state management
 * - Integration with ghost detection and ModelData systems
 */
@Mod.EventBusSubscriber(modid = "justsomeswitches", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class GhostWorldRenderer {
    
    /**
     * Renders a quad with manual alpha transparency using a simplified approach.
     * Uses white color with ghost alpha for reliable transparency.
     */
    private static void renderQuadWithAlpha(@Nonnull VertexConsumer buffer,
                                           @Nonnull PoseStack poseStack,
                                           @Nonnull BakedQuad quad,
                                           float alpha,
                                           int packedLight,
                                           int packedOverlay) {
        
        // Get vertex data from the quad
        int[] vertices = quad.getVertices();
        
        // Ghost alpha (75% opacity)
        int ghostAlpha = (int)(alpha * 255);
        
        // Manual vertex rendering with alpha transparency
        for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
            int baseIndex = vertexIndex * 8; // 8 integers per vertex
            
            // Extract vertex position
            float x = Float.intBitsToFloat(vertices[baseIndex]);
            float y = Float.intBitsToFloat(vertices[baseIndex + 1]);
            float z = Float.intBitsToFloat(vertices[baseIndex + 2]);
            
            // Extract texture coordinates
            float u = Float.intBitsToFloat(vertices[baseIndex + 4]);
            float v = Float.intBitsToFloat(vertices[baseIndex + 5]);
            
            // Extract normal data
            int normalData = vertices[baseIndex + 6];
            
            // Use white color with ghost alpha for transparency
            buffer.vertex(poseStack.last().pose(), x, y, z)
                  .color(255, 255, 255, ghostAlpha) // White with ghost alpha
                  .uv(u, v)
                  .overlayCoords(packedOverlay)
                  .uv2(packedLight)
                  .normal(poseStack.last().normal(),
                         (normalData & 0xFF) / 127.0f - 1.0f,  // X normal
                         ((normalData >> 8) & 0xFF) / 127.0f - 1.0f,  // Y normal  
                         ((normalData >> 16) & 0xFF) / 127.0f - 1.0f) // Z normal
                  .endVertex();
        }
    }
    
    /**
     * Renders ghost previews during the translucent rendering stage.
     */
    @SubscribeEvent
    public static void onRenderLevelStage(@Nonnull RenderLevelStageEvent event) {
        // Render during translucent stage for proper transparency
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        
        renderActiveGhostPreviews(event);
    }
    
    /**
     * Renders all currently active ghost previews.
     */
    private static void renderActiveGhostPreviews(@Nonnull RenderLevelStageEvent event) {
        GhostBlockDetector detector = GhostBlockDetector.getInstance();
        
        // Check if ghost preview is active
        if (!detector.isGhostActive()) {
            return;
        }
        
        BlockPos ghostPos = detector.getCurrentGhostPos();
        BlockState ghostState = detector.getCurrentGhostState();
        
        if (ghostPos == null || ghostState == null) {
            return;
        }
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        
        // Get rendering components
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        BlockRenderDispatcher blockRenderer = mc.getBlockRenderer();
        
        renderGhostBlock(
            ghostPos,
            ghostState,
            detector.getCurrentWallOrientation(),
            poseStack,
            bufferSource,
            blockRenderer,
            event.getPartialTick()
        );
        
        // Ensure buffers are flushed
        bufferSource.endBatch();
    }
    
    /**
     * Renders a single ghost block at the specified position.
     */
    private static void renderGhostBlock(@Nonnull BlockPos pos,
                                        @Nonnull BlockState state,
                                        @Nonnull String wallOrientation,
                                        @Nonnull PoseStack poseStack,
                                        @Nonnull MultiBufferSource bufferSource,
                                        @Nonnull BlockRenderDispatcher blockRenderer,
                                        @SuppressWarnings("unused") float partialTick) {
        
        // Set up pose stack for this position
        poseStack.pushPose();
        
        // Calculate camera offset for proper positioning
        double cameraX = Minecraft.getInstance().getEntityRenderDispatcher().camera.getPosition().x;
        double cameraY = Minecraft.getInstance().getEntityRenderDispatcher().camera.getPosition().y;
        double cameraZ = Minecraft.getInstance().getEntityRenderDispatcher().camera.getPosition().z;
        
        poseStack.translate(
            pos.getX() - cameraX,
            pos.getY() - cameraY,
            pos.getZ() - cameraZ
        );
        
        try {
            // Enhanced OpenGL state setup for proper transparency
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            com.mojang.blaze3d.systems.RenderSystem.blendFuncSeparate(
                com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.SourceFactor.ONE,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ZERO
            );
            com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
            com.mojang.blaze3d.systems.RenderSystem.depthMask(false); // Don't write to depth buffer for transparency
            
            // Get ghost ModelData from provider (contains correct wall orientation)
            ModelData ghostModelData = GhostModelDataProvider.getInstance().getGhostModelData(pos);
            if (ghostModelData == null) {
                // Create ghost ModelData if not available
                ghostModelData = createGhostModelData(state, wallOrientation);
            }
            
            // Use translucent render type for proper alpha blending support
            // Manual alpha only works with translucent buffers
            RenderType renderType = RenderType.translucent();
            
            // Get the custom model for switches lever
            BakedModel model = blockRenderer.getBlockModel(state);
            
            if (model instanceof SwitchesLeverDynamicModel dynamicModel) {
                renderWithDynamicModel(dynamicModel, state, pos, ghostModelData, renderType, poseStack, bufferSource);
            } else {
                renderWithStandardModel(model, state, pos, ghostModelData, renderType, poseStack, bufferSource);
            }
            
        } catch (Exception e) {
            // Handle rendering errors gracefully
        } finally {
            // Restore OpenGL state
            com.mojang.blaze3d.systems.RenderSystem.disableBlend();
            com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
            com.mojang.blaze3d.systems.RenderSystem.depthMask(true); // Restore depth writes
            poseStack.popPose();
        }
    }
    
    /**
     * Renders ghost block using manual alpha transparency with texture-based filtering.
     * Filters specific textures (like powered states) instead of geometric analysis.
     */
    private static void renderWithDynamicModel(@Nonnull SwitchesLeverDynamicModel dynamicModel,
                                              @Nonnull BlockState state,
                                              @Nonnull BlockPos pos,
                                              @Nonnull ModelData modelData,
                                              @Nonnull RenderType renderType,
                                              @Nonnull PoseStack poseStack,
                                              @Nonnull MultiBufferSource bufferSource) {
        
        // Use same code path as placed blocks with manual transparency and face culling
        var buffer = bufferSource.getBuffer(renderType);
        RandomSource random = RandomSource.create();
        random.setSeed(state.getSeed(pos));
        
        // Use dimmed lighting for ghost preview (less bright than placed blocks)
        int packedLight;
        try {
            var level = Minecraft.getInstance().level;
            if (level == null) throw new IllegalStateException("Level is null");
            int fullLight = net.minecraft.client.renderer.LevelRenderer.getLightColor(level, pos);
            // Dim the lighting for ghost preview (reduce by ~30%)
            int skyLight = (fullLight >> 20) & 15;
            int blockLight = (fullLight >> 4) & 15;
            skyLight = Math.max(0, (int)(skyLight * 0.7f));
            blockLight = Math.max(0, (int)(blockLight * 0.7f));
            packedLight = (skyLight << 20) | (blockLight << 4);
        } catch (Exception e) {
            packedLight = 0; // Conservative fallback
        }
        int packedOverlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
        
        // Render with manual transparency (75% opacity)
        float alpha = 0.75f;
        
        // Render all quads (directional and general) with texture-based filtering
        for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
            List<BakedQuad> quads = dynamicModel.getQuads(state, direction, random, modelData, renderType);
            for (BakedQuad quad : quads) {
                if (isQuadVisible(quad)) {
                    renderQuadWithAlpha(buffer, poseStack, quad, alpha, packedLight, packedOverlay);
                }
            }
        }
        
        List<BakedQuad> generalQuads = dynamicModel.getQuads(state, null, random, modelData, renderType);
        for (BakedQuad quad : generalQuads) {
            if (isQuadVisible(quad)) {
                renderQuadWithAlpha(buffer, poseStack, quad, alpha, packedLight, packedOverlay);
            }
        }
    }
    
    /**
     * Determines if a quad should be rendered for ghost preview.
     * Uses texture-based filtering to hide powered texture faces.
     */
    private static boolean isQuadVisible(@Nonnull BakedQuad quad) {
        return !isPoweredTextureFace(quad);
    }
    
    /**
     * Checks if a quad uses the "powered" texture that should be hidden in ghost preview.
     * Specifically targets the powered UV texture face as mentioned by user.
     */
    private static boolean isPoweredTextureFace(@Nonnull BakedQuad quad) {
        try {
            var sprite = quad.getSprite();
            
            // Get texture name from sprite
            String textureName = getTextureName(sprite);
            
            // Specific targeting of "powered" UV texture as requested
            // Check for any variation of powered texture names
            if (textureName.contains("powered")) {
                return true; // Hide any face with "powered" in texture name
            }
            
            // Also check for redstone-related textures that indicate powered state
            if (textureName.contains("redstone_block") || 
                textureName.contains("switches_lever_powered") ||
                textureName.contains("lever_on") ||
                (textureName.contains("lever") && textureName.contains("on"))) {
                return true;
            }
            
            // Check UV coordinates for powered texture mapping
            // Powered textures often use specific UV regions
            if (hasPoweredUVMapping(quad)) {
                return true;
            }
                   
        } catch (Exception e) {
            // If we can't determine texture, default to showing it (conservative)
        }
        
        return false; // Default: show the face
    }
    

    
    /**
     * Checks if quad has UV mapping that suggests powered texture.
     * Powered textures often use specific regions of the texture atlas.
     */
    private static boolean hasPoweredUVMapping(@Nonnull BakedQuad quad) {
        try {
            int[] vertices = quad.getVertices();
            
            // Check UV coordinates of all vertices for powered texture patterns
            for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
                int baseIndex = vertexIndex * 8;
                
                // Extract UV coordinates (at offset 4 and 5)
                float u = Float.intBitsToFloat(vertices[baseIndex + 4]);
                float v = Float.intBitsToFloat(vertices[baseIndex + 5]);
                
                // Check for specific UV ranges that indicate powered texture
                // These are example ranges - adjust based on your texture atlas
                if (isPoweredUVRange(u, v)) {
                    return true;
                }
            }
            
        } catch (Exception e) {
            // Ignore UV analysis errors
        }
        
        return false;
    }
    
    /**
     * Determines if UV coordinates are in powered texture range.
     * Adjust these ranges based on your texture atlas layout.
     */
    private static boolean isPoweredUVRange(@SuppressWarnings("unused") float u, @SuppressWarnings("unused") float v) {
        // Example powered texture UV ranges (adjust for your atlas)
        // Check if UV falls in known powered texture regions
        
        // Common powered texture patterns:
        // - Bottom half of texture (v > 0.5)
        // - Right half of texture (u > 0.5) 
        // - Specific corners or regions
        
        // Conservative approach: focus on texture name filtering
        return false;
    }
    
    /**
     * Extracts texture name from sprite for analysis.
     */
    @Nonnull
    private static String getTextureName(@Nonnull TextureAtlasSprite sprite) {
        try {
            try (var contents = sprite.contents()) {
                return contents.name().toString().toLowerCase();
            }
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    /**
     * Fallback rendering for standard models with manual alpha transparency and texture-based filtering.
     */
    private static void renderWithStandardModel(@Nonnull BakedModel model,
                                               @Nonnull BlockState state,
                                               @Nonnull BlockPos pos,
                                               @Nonnull ModelData modelData,
                                               @Nonnull RenderType renderType,
                                               @Nonnull PoseStack poseStack,
                                               @Nonnull MultiBufferSource bufferSource) {
        
        var buffer = bufferSource.getBuffer(renderType);
        RandomSource random = RandomSource.create();
        random.setSeed(state.getSeed(pos));
        
        // Use dimmed lighting for ghost preview (less bright than placed blocks)
        int packedLight;
        try {
            var level = Minecraft.getInstance().level;
            if (level == null) throw new IllegalStateException("Level is null");
            int fullLight = net.minecraft.client.renderer.LevelRenderer.getLightColor(level, pos);
            // Dim the lighting for ghost preview (reduce by ~30%)
            int skyLight = (fullLight >> 20) & 15;
            int blockLight = (fullLight >> 4) & 15;
            skyLight = Math.max(0, (int)(skyLight * 0.7f));
            blockLight = Math.max(0, (int)(blockLight * 0.7f));
            packedLight = (skyLight << 20) | (blockLight << 4);
        } catch (Exception e) {
            packedLight = 0;
        }
        int packedOverlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
        
        // Manual transparency (75% opacity)
        float alpha = 0.75f;
        
        // Standard model rendering with manual transparency and texture-based filtering
        for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
            List<BakedQuad> quads = model.getQuads(state, direction, random, modelData, renderType);
            for (BakedQuad quad : quads) {
                if (isQuadVisible(quad)) {
                    renderQuadWithAlpha(buffer, poseStack, quad, alpha, packedLight, packedOverlay);
                }
            }
        }
        
        List<BakedQuad> generalQuads = model.getQuads(state, null, random, modelData, renderType);
        for (BakedQuad quad : generalQuads) {
            if (isQuadVisible(quad)) {
                renderQuadWithAlpha(buffer, poseStack, quad, alpha, packedLight, packedOverlay);
            }
        }
    }
    
    /**
     * Creates ghost ModelData for rendering.
     */
    @Nonnull
    private static ModelData createGhostModelData(@Nonnull BlockState state, @Nonnull String wallOrientation) {
        return ModelData.builder()
                .with(SwitchesLeverBlockEntity.GHOST_MODE, true)
                .with(SwitchesLeverBlockEntity.GHOST_ALPHA, 0.75f)
                .with(SwitchesLeverBlockEntity.GHOST_STATE, state)
                .with(SwitchesLeverBlockEntity.WALL_ORIENTATION, wallOrientation)
                .build();
    }
}
