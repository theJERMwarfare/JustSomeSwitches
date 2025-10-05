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
import java.util.List;

/** World rendering system for ghost block previews during translucent stage. */
@Mod.EventBusSubscriber(modid = "justsomeswitches", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class GhostWorldRenderer {

    /** Renders a quad with manual alpha transparency using white color with ghost alpha. */
    private static void renderQuadWithAlpha(@Nonnull VertexConsumer buffer,
                                           @Nonnull PoseStack poseStack,
                                           @Nonnull BakedQuad quad,
                                           float alpha,
                                           int packedLight,
                                           int packedOverlay) {
        int[] vertices = quad.getVertices();
        int ghostAlpha = (int)(alpha * 255);
        for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
            int baseIndex = vertexIndex * 8;
            float x = Float.intBitsToFloat(vertices[baseIndex]);
            float y = Float.intBitsToFloat(vertices[baseIndex + 1]);
            float z = Float.intBitsToFloat(vertices[baseIndex + 2]);
            float u = Float.intBitsToFloat(vertices[baseIndex + 4]);
            float v = Float.intBitsToFloat(vertices[baseIndex + 5]);
            int normalData = vertices[baseIndex + 6];
            buffer.vertex(poseStack.last().pose(), x, y, z)
                  .color(255, 255, 255, ghostAlpha)
                  .uv(u, v)
                  .overlayCoords(packedOverlay)
                  .uv2(packedLight)
                  .normal(poseStack.last().normal(),
                         (normalData & 0xFF) / 127.0f - 1.0f,
                         ((normalData >> 8) & 0xFF) / 127.0f - 1.0f,
                         ((normalData >> 16) & 0xFF) / 127.0f - 1.0f)
                  .endVertex();
        }
    }

    /** Renders ghost previews during the translucent rendering stage. */
    @SubscribeEvent
    public static void onRenderLevelStage(@Nonnull RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        renderActiveGhostPreviews(event);
    }

    /** Renders all currently active ghost previews. */
    private static void renderActiveGhostPreviews(@Nonnull RenderLevelStageEvent event) {
        GhostBlockDetector detector = GhostBlockDetector.getInstance();
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
        bufferSource.endBatch();
    }

    /** Renders a single ghost block at the specified position. */
    private static void renderGhostBlock(@Nonnull BlockPos pos,
                                        @Nonnull BlockState state,
                                        @Nonnull String wallOrientation,
                                        @Nonnull PoseStack poseStack,
                                        @Nonnull MultiBufferSource bufferSource,
                                        @Nonnull BlockRenderDispatcher blockRenderer,
                                        @SuppressWarnings("unused") float partialTick) {
        poseStack.pushPose();
        double cameraX = Minecraft.getInstance().getEntityRenderDispatcher().camera.getPosition().x;
        double cameraY = Minecraft.getInstance().getEntityRenderDispatcher().camera.getPosition().y;
        double cameraZ = Minecraft.getInstance().getEntityRenderDispatcher().camera.getPosition().z;
        poseStack.translate(
            pos.getX() - cameraX,
            pos.getY() - cameraY,
            pos.getZ() - cameraZ
        );
        try {
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            com.mojang.blaze3d.systems.RenderSystem.blendFuncSeparate(
                com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.SourceFactor.ONE,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ZERO
            );
            com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
            com.mojang.blaze3d.systems.RenderSystem.depthMask(false);
            ModelData ghostModelData = GhostModelDataProvider.getInstance().getGhostModelData(pos);
            if (ghostModelData == null) {
                ghostModelData = createGhostModelData(state, wallOrientation);
            }
            RenderType renderType = RenderType.translucent();
            BakedModel model = blockRenderer.getBlockModel(state);
            if (model instanceof SwitchesLeverDynamicModel dynamicModel) {
                renderWithDynamicModel(dynamicModel, state, pos, ghostModelData, renderType, poseStack, bufferSource);
            } else {
                renderWithStandardModel(model, state, pos, ghostModelData, renderType, poseStack, bufferSource);
            }
        } catch (Exception e) {
            // Intentionally ignore rendering errors - ghost preview is optional visual feature
        } finally {
            com.mojang.blaze3d.systems.RenderSystem.disableBlend();
            com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
            com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
            poseStack.popPose();
        }
    }

    /** Renders ghost block using manual alpha transparency with texture-based filtering. */
    private static void renderWithDynamicModel(@Nonnull SwitchesLeverDynamicModel dynamicModel,
                                              @Nonnull BlockState state,
                                              @Nonnull BlockPos pos,
                                              @Nonnull ModelData modelData,
                                              @Nonnull RenderType renderType,
                                              @Nonnull PoseStack poseStack,
                                              @Nonnull MultiBufferSource bufferSource) {
        var buffer = bufferSource.getBuffer(renderType);
        RandomSource random = RandomSource.create();
        random.setSeed(state.getSeed(pos));
        int packedLight;
        try {
            var level = Minecraft.getInstance().level;
            if (level == null) throw new IllegalStateException("Level is null");
            int fullLight = net.minecraft.client.renderer.LevelRenderer.getLightColor(level, pos);
            int skyLight = (fullLight >> 20) & 15;
            int blockLight = (fullLight >> 4) & 15;
            skyLight = Math.max(0, (int)(skyLight * 0.7f));
            blockLight = Math.max(0, (int)(blockLight * 0.7f));
            packedLight = (skyLight << 20) | (blockLight << 4);
        } catch (Exception e) {
            packedLight = 0;
        }
        int packedOverlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
        float alpha = 0.75f;
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

    /** Determines if a quad should be rendered for ghost preview using texture-based filtering. */
    private static boolean isQuadVisible(@Nonnull BakedQuad quad) {
        return !isPoweredTextureFace(quad);
    }

    /** Checks if a quad uses the "powered" texture that should be hidden in ghost preview. */
    private static boolean isPoweredTextureFace(@Nonnull BakedQuad quad) {
        try {
            var sprite = quad.getSprite();
            String textureName = getTextureName(sprite);
            if (textureName.contains("powered")) {
                return true;
            }
            if (textureName.contains("redstone_block") ||
                textureName.contains("switches_lever_powered") ||
                textureName.contains("lever_on") ||
                (textureName.contains("lever") && textureName.contains("on"))) {
                return true;
            }
            if (hasPoweredUVMapping(quad)) {
                return true;
            }
        } catch (Exception e) {
            // Intentionally ignore UV mapping errors - safe default is false
        }
        return false;
    }

    /** Checks if quad has UV mapping that suggests powered texture. */
    private static boolean hasPoweredUVMapping(@Nonnull BakedQuad quad) {
        try {
            int[] vertices = quad.getVertices();
            for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
                int baseIndex = vertexIndex * 8;
                float u = Float.intBitsToFloat(vertices[baseIndex + 4]);
                float v = Float.intBitsToFloat(vertices[baseIndex + 5]);
                if (isPoweredUVRange(u, v)) {
                    return true;
                }
            }
        } catch (Exception e) {
        }
        return false;
    }

    /** Determines if UV coordinates are in powered texture range. */
    private static boolean isPoweredUVRange(@SuppressWarnings("unused") float u, @SuppressWarnings("unused") float v) {
        return false;
    }

    /** Extracts texture name from sprite for analysis. */
    @Nonnull
    private static String getTextureName(@Nonnull TextureAtlasSprite sprite) {
        try {
            try (var contents = sprite.contents()) {
                return contents.name().toString().toLowerCase();
            }
        } catch (Exception e) {
            // Intentionally ignore texture name errors - return safe default
            return "unknown";
        }
    }

    /** Fallback rendering for standard models with manual alpha transparency. */
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
        int packedLight;
        try {
            var level = Minecraft.getInstance().level;
            if (level == null) throw new IllegalStateException("Level is null");
            int fullLight = net.minecraft.client.renderer.LevelRenderer.getLightColor(level, pos);
            int skyLight = (fullLight >> 20) & 15;
            int blockLight = (fullLight >> 4) & 15;
            skyLight = Math.max(0, (int)(skyLight * 0.7f));
            blockLight = Math.max(0, (int)(blockLight * 0.7f));
            packedLight = (skyLight << 20) | (blockLight << 4);
        } catch (Exception e) {
            packedLight = 0;
        }
        int packedOverlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
        float alpha = 0.75f;
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

    /** Creates ghost ModelData for rendering. */
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
