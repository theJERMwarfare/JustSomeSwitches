package net.justsomeswitches.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.justsomeswitches.blockentity.SwitchesLeverBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Block Entity Renderer for Switches Lever that renders complete models with custom textures
 * ---
 * Phase 3C FINAL: Complete model rendering with texture replacement
 * This renderer now replaces vanilla block rendering entirely when custom textures are applied,
 * providing seamless integration with the texture customization system.
 * ---
 * Compatible with NeoForge 1.20.4 using correct Matrix imports and rendering patterns
 */
public class SwitchesLeverBlockEntityRenderer implements BlockEntityRenderer<SwitchesLeverBlockEntity> {

    // Model resource locations for different switch states
    private static final ResourceLocation LEVER_MODEL = new ResourceLocation("justsomeswitches", "block/switches_lever");
    private static final ResourceLocation LEVER_ON_MODEL = new ResourceLocation("justsomeswitches", "block/switches_lever_on");

    // Default texture fallbacks (same as model defaults)
    private static final String DEFAULT_BASE_TEXTURE = "minecraft:block/cobblestone";
    private static final String DEFAULT_TOGGLE_TEXTURE = "minecraft:block/oak_planks";
    private static final String UNPOWERED_TEXTURE = "minecraft:block/gray_concrete_powder";
    private static final String POWERED_TEXTURE = "minecraft:block/redstone_block";

    public SwitchesLeverBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        // Constructor for renderer context
    }

    @Override
    public void render(@Nonnull SwitchesLeverBlockEntity blockEntity, float partialTick,
                       @Nonnull PoseStack poseStack, @Nonnull MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {

        // Verify we have valid data
        if (blockEntity.getLevel() == null) {
            return;
        }

        BlockState blockState = blockEntity.getBlockState();

        // ALWAYS render - we'll handle both custom and default cases to completely override vanilla
        String baseTexture = blockEntity.hasCustomTextures() ? blockEntity.getBaseTexture() : DEFAULT_BASE_TEXTURE;
        String toggleTexture = blockEntity.hasCustomTextures() ? blockEntity.getToggleTexture() : DEFAULT_TOGGLE_TEXTURE;

        System.out.println("Phase 3C FINAL: Completely overriding vanilla rendering - Custom: " + blockEntity.hasCustomTextures() +
                ", Base: " + baseTexture + ", Toggle: " + toggleTexture);

        try {
            // Determine which model to use based on powered state
            boolean isPowered = blockState.getValue(BlockStateProperties.POWERED);
            ResourceLocation modelLocation = isPowered ? LEVER_ON_MODEL : LEVER_MODEL;

            // Get the model from the model manager
            Minecraft mc = Minecraft.getInstance();
            BakedModel model = mc.getModelManager().getModel(modelLocation);

            if (model == null) {
                System.out.println("Phase 3C FINAL: Could not load model " + modelLocation);
                return;
            }

            // Get texture sprites for our textures (custom or default)
            TextureAtlasSprite baseSprite = getTextureSprite(baseTexture);
            TextureAtlasSprite toggleSprite = getTextureSprite(toggleTexture);
            TextureAtlasSprite unpoweredSprite = getTextureSprite(UNPOWERED_TEXTURE);
            TextureAtlasSprite poweredSprite = getTextureSprite(POWERED_TEXTURE);

            // Create texture mapping
            Map<String, TextureAtlasSprite> textureMap = createTextureMapping(baseSprite, toggleSprite, unpoweredSprite, poweredSprite);

            // Setup pose stack
            poseStack.pushPose();

            // Get vertex consumer for solid block rendering
            VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.cutout());

            // Render the complete model with textures (custom or default)
            renderCompleteModelWithCustomTextures(model, blockState, poseStack, vertexConsumer, packedLight, packedOverlay, textureMap);

            poseStack.popPose();

        } catch (Exception e) {
            System.out.println("Phase 3C FINAL: Rendering error - " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Renders the complete switch model with custom texture replacement
     * ---
     * Phase 3C FINAL: This replaces the vanilla model entirely with custom textures
     */
    private void renderCompleteModelWithCustomTextures(@Nonnull BakedModel model, @Nonnull BlockState blockState,
                                                       @Nonnull PoseStack poseStack, @Nonnull VertexConsumer vertexConsumer,
                                                       int packedLight, int packedOverlay,
                                                       @Nonnull Map<String, TextureAtlasSprite> textureMap) {
        try {
            // Get transformation matrices
            var lastPose = poseStack.last();
            var pose = lastPose.pose();
            var normal = lastPose.normal();

            // Get random source for model rendering
            RandomSource random = RandomSource.create();

            // Process model quads for all faces (null face = general quads)
            List<BakedQuad> generalQuads = model.getQuads(blockState, null, random);
            for (BakedQuad quad : generalQuads) {
                renderQuadWithCustomTexture(quad, vertexConsumer, pose, normal, packedLight, packedOverlay, textureMap);
            }

            // Process direction-specific quads for all faces
            for (Direction direction : Direction.values()) {
                List<BakedQuad> directionQuads = model.getQuads(blockState, direction, random);
                for (BakedQuad quad : directionQuads) {
                    renderQuadWithCustomTexture(quad, vertexConsumer, pose, normal, packedLight, packedOverlay, textureMap);
                }
            }

            System.out.println("Phase 3C FINAL: Successfully rendered complete model with " +
                    (generalQuads.size() + getAllDirectionQuadCount(model, blockState, random)) + " quads");

        } catch (Exception e) {
            System.out.println("Phase 3C FINAL: Error in complete model rendering - " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Counts total quads for all directions (for debug logging)
     */
    private int getAllDirectionQuadCount(@Nonnull BakedModel model, @Nonnull BlockState blockState, @Nonnull RandomSource random) {
        int count = 0;
        for (Direction direction : Direction.values()) {
            count += model.getQuads(blockState, direction, random).size();
        }
        return count;
    }

    /**
     * Renders a single quad with custom texture replacement
     * ---
     * Phase 3C FINAL: Core texture replacement logic for individual model quads
     */
    private void renderQuadWithCustomTexture(@Nonnull BakedQuad quad, @Nonnull VertexConsumer vertexConsumer,
                                             @Nonnull Matrix4f pose, @Nonnull Matrix3f normal, int packedLight, int packedOverlay,
                                             @Nonnull Map<String, TextureAtlasSprite> textureMap) {

        // Get the original texture sprite for this quad
        TextureAtlasSprite originalSprite = quad.getSprite();
        String originalTextureName = originalSprite.contents().name().toString();

        // Determine which custom texture to use based on the original texture
        TextureAtlasSprite customSprite = determineCustomTexture(originalTextureName, textureMap, originalSprite);

        // Get vertex data from the quad
        int[] vertexData = quad.getVertices();

        // Process each vertex in the quad (4 vertices per quad)
        for (int i = 0; i < 4; i++) {
            int vertexIndex = i * 8; // 8 integers per vertex in the vertex format

            // Extract vertex data
            float x = Float.intBitsToFloat(vertexData[vertexIndex]);
            float y = Float.intBitsToFloat(vertexData[vertexIndex + 1]);
            float z = Float.intBitsToFloat(vertexData[vertexIndex + 2]);

            // Extract color (if present)
            int color = vertexData[vertexIndex + 3];
            float r = ((color >> 16) & 0xFF) / 255.0f;
            float g = ((color >> 8) & 0xFF) / 255.0f;
            float b = (color & 0xFF) / 255.0f;
            float a = ((color >> 24) & 0xFF) / 255.0f;

            // Handle default white color if no color specified
            if (color == 0) {
                r = g = b = a = 1.0f;
            }

            // Extract original UV coordinates
            float originalU = Float.intBitsToFloat(vertexData[vertexIndex + 4]);
            float originalV = Float.intBitsToFloat(vertexData[vertexIndex + 5]);

            // Map original UV to custom texture UV
            float customU = mapUVCoordinate(originalU, originalSprite, customSprite, true);
            float customV = mapUVCoordinate(originalV, originalSprite, customSprite, false);

            // Extract normal data
            int normalData = vertexData[vertexIndex + 6];
            float nx = ((byte)(normalData & 0xFF)) / 127.0f;
            float ny = ((byte)((normalData >> 8) & 0xFF)) / 127.0f;
            float nz = ((byte)((normalData >> 16) & 0xFF)) / 127.0f;

            // Add vertex to the vertex consumer with custom texture coordinates
            vertexConsumer.vertex(pose, x, y, z)
                    .color(r, g, b, a)
                    .uv(customU, customV)
                    .overlayCoords(packedOverlay)
                    .uv2(packedLight)
                    .normal(normal, nx, ny, nz)
                    .endVertex();
        }
    }

    /**
     * Determines which custom texture to use based on the original texture name
     * ---
     * Phase 3C FINAL: Enhanced texture mapping with better detection
     */
    @Nonnull
    private TextureAtlasSprite determineCustomTexture(@Nonnull String originalTextureName,
                                                      @Nonnull Map<String, TextureAtlasSprite> textureMap,
                                                      @Nonnull TextureAtlasSprite fallback) {

        // Check if this texture matches one of our UV mappings
        // The model uses references like "minecraft:block/cobblestone" for base, etc.

        if (originalTextureName.contains("cobblestone")) {
            System.out.println("Phase 3C FINAL: Replacing cobblestone with custom base texture");
            return textureMap.getOrDefault("base", fallback);
        } else if (originalTextureName.contains("oak_planks")) {
            System.out.println("Phase 3C FINAL: Replacing oak_planks with custom toggle texture");
            return textureMap.getOrDefault("toggle", fallback);
        } else if (originalTextureName.contains("gray_concrete_powder")) {
            // Keep unpowered texture as is
            return textureMap.getOrDefault("unpowered", fallback);
        } else if (originalTextureName.contains("redstone_block")) {
            // Keep powered texture as is
            return textureMap.getOrDefault("powered", fallback);
        }

        // If no match found, return the original sprite
        return fallback;
    }

    /**
     * Maps UV coordinates from original texture to custom texture
     * ---
     * Phase 3C FINAL: Proper UV coordinate transformation between different texture sprites
     */
    private float mapUVCoordinate(float originalCoord, @Nonnull TextureAtlasSprite originalSprite,
                                  @Nonnull TextureAtlasSprite customSprite, boolean isU) {

        // Convert from original sprite's UV space to custom sprite's UV space
        if (isU) {
            // Map U coordinate
            float originalMin = originalSprite.getU0();
            float originalMax = originalSprite.getU1();
            float customMin = customSprite.getU0();
            float customMax = customSprite.getU1();

            // Normalize to 0-1 range relative to original sprite
            float normalized = (originalCoord - originalMin) / (originalMax - originalMin);

            // Map to custom sprite's UV range
            return customMin + normalized * (customMax - customMin);
        } else {
            // Map V coordinate
            float originalMin = originalSprite.getV0();
            float originalMax = originalSprite.getV1();
            float customMin = customSprite.getV0();
            float customMax = customSprite.getV1();

            // Normalize to 0-1 range relative to original sprite
            float normalized = (originalCoord - originalMin) / (originalMax - originalMin);

            // Map to custom sprite's UV range
            return customMin + normalized * (customMax - customMin);
        }
    }

    /**
     * Gets a texture sprite from the texture atlas
     */
    @Nonnull
    private TextureAtlasSprite getTextureSprite(@Nonnull String texturePath) {
        try {
            ResourceLocation textureLocation = new ResourceLocation(texturePath);
            TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(textureLocation);

            System.out.println("Phase 3C FINAL: Loaded texture sprite - Path: " + texturePath +
                    " -> Sprite: " + sprite.contents().name() +
                    " (Missing: " + sprite.contents().name().toString().contains("missingno") + ")");

            return sprite;
        } catch (Exception e) {
            System.out.println("Phase 3C FINAL: Error loading texture " + texturePath + " - " + e.getMessage());
            // Return a fallback sprite
            return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(new ResourceLocation("minecraft:block/stone"));
        }
    }

    /**
     * Creates a texture mapping from UV names to texture sprites
     */
    @Nonnull
    private Map<String, TextureAtlasSprite> createTextureMapping(@Nonnull TextureAtlasSprite baseSprite,
                                                                 @Nonnull TextureAtlasSprite toggleSprite,
                                                                 @Nonnull TextureAtlasSprite unpoweredSprite,
                                                                 @Nonnull TextureAtlasSprite poweredSprite) {
        Map<String, TextureAtlasSprite> textureMap = new HashMap<>();

        textureMap.put("base", baseSprite);
        textureMap.put("toggle", toggleSprite);
        textureMap.put("unpowered", unpoweredSprite);
        textureMap.put("powered", poweredSprite);

        System.out.println("Phase 3C FINAL: Created texture mapping - " + textureMap.size() + " textures registered");

        return textureMap;
    }

    @Override
    public int getViewDistance() {
        return 16;
    }

    @Override
    public boolean shouldRenderOffScreen(@Nonnull SwitchesLeverBlockEntity blockEntity) {
        return false;
    }
}