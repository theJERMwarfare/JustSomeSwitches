package net.justsomeswitches.gui.components;

import net.justsomeswitches.blockentity.SwitchBlockEntity;
import net.justsomeswitches.gui.FaceSelectionData;
import net.justsomeswitches.gui.SwitchesTextureMenu;
import net.justsomeswitches.util.TextureRotation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders all texture previews in the GUI including 3D switch preview,
 * 2D texture previews with rotation support, and power state indicators.
 */
public class TexturePreviewRenderer {
    
    private static final int PREVIEW_SIZE = 18;
    private static final int POWER_PREVIEW_SIZE = 6;
    private static final float PREVIEW_SCALE = 2.0f;
    
    /** Pre-computed rotation for 3D preview X-axis. */
    private static final org.joml.Quaternionf ROTATION_X_10 = 
        new org.joml.Quaternionf().fromAxisAngleDeg(1, 0, 0, 10f);
    
    /** Pre-computed rotation for 3D preview Y-axis. */
    private static final org.joml.Quaternionf ROTATION_Y_NEG_215 = 
        new org.joml.Quaternionf().fromAxisAngleDeg(0, 1, 0, -215f);
    
    private final SwitchesTextureMenu menu;
    private final Font font;
    
    /** Cached ResourceLocation objects to avoid repeated creation. */
    private final Map<String, ResourceLocation> resourceLocationCache = new HashMap<>();
    
    /** Cached texture sprites for faster lookup. */
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection") // Updated during caching operations
    private final Map<String, TextureAtlasSprite> spriteCache = new HashMap<>();
    
    /** Cached sprite names to avoid repeated contents() calls. */
    private final Map<TextureAtlasSprite, String> spriteNameCache = new HashMap<>();
    
    /**
     * Creates a new TexturePreviewRenderer.
     * 
     * @param menu the texture menu providing data
     * @param font the font for rendering text
     */
    public TexturePreviewRenderer(@Nonnull SwitchesTextureMenu menu, @Nonnull Font font) {
        this.menu = menu;
        this.font = font;
    }
    
    /**
     * Clears all texture caches to free memory when screen closes.
     * Caches automatically repopulate on next use.
     */
    public void clearCaches() {
        resourceLocationCache.clear();
        spriteCache.clear();
        spriteNameCache.clear();
    }
    
    /**
     * Renders the live 3D preview in the GUI center using the Dynamic Model.
     * This ensures texture rotation is properly applied by leveraging the existing model system.
     * 
     * @param graphics the GUI graphics context
     * @param centerX the center X coordinate
     * @param centerY the center Y coordinate
     * @param leftSelection the toggle texture selection
     * @param rightSelection the base texture selection
     */
    public void renderLive3DPreview(@Nonnull GuiGraphics graphics, int centerX, int centerY,
                                     @Nonnull FaceSelectionData.RawTextureSelection leftSelection,
                                     @Nonnull FaceSelectionData.RawTextureSelection rightSelection) {
        try {
            BlockState switchState = getCurrentBlockState();
            
            PoseStack poseStack = graphics.pose();
            poseStack.pushPose();
            
            poseStack.translate(centerX, centerY, 100);
            poseStack.scale(PREVIEW_SCALE * 16, -PREVIEW_SCALE * 16, PREVIEW_SCALE * 16);
            
            poseStack.mulPose(ROTATION_X_10);
            poseStack.mulPose(ROTATION_Y_NEG_215);
            
            poseStack.translate(-0.5f, -0.5f, -0.5f);
            
            MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
            
            // Use the Dynamic Model system which handles texture rotation correctly
            renderBlockWithCustomTextures(switchState, poseStack, bufferSource, leftSelection, rightSelection);
            
            bufferSource.endBatch();
            
            poseStack.popPose();
            
        } catch (Exception e) {
            // Fallback to basic 3D preview without custom textures
            renderBasic3DPreview(graphics, centerX, centerY);
        }
    }
    
    /**
     * Renders 2D texture previews with rotation support.
     * 
     * @param graphics the GUI graphics context
     * @param leftX the left preview X coordinate
     * @param leftY the left preview Y coordinate
     * @param rightX the right preview X coordinate
     * @param rightY the right preview Y coordinate
     * @param leftSelection the toggle texture selection
     * @param rightSelection the base texture selection
     */
    public void render2DTexturePreviews(@Nonnull GuiGraphics graphics, int leftX, int leftY,
                                        int rightX, int rightY,
                                        @Nonnull FaceSelectionData.RawTextureSelection leftSelection,
                                        @Nonnull FaceSelectionData.RawTextureSelection rightSelection) {
        // Draw left (toggle) texture preview with rotation support
        if (leftSelection.hasPreview()) {
            String leftPreviewTexture = leftSelection.previewTexture();
            if (leftPreviewTexture != null && !leftPreviewTexture.isEmpty()) {
                TextureRotation toggleRotation = menu.getToggleTextureRotation();
                renderTexturePreviewBox(graphics, leftX, leftY, leftPreviewTexture, toggleRotation);
            }
        }
        
        // Draw right (base) texture preview with rotation support
        if (rightSelection.hasPreview()) {
            String rightPreviewTexture = rightSelection.previewTexture();
            if (rightPreviewTexture != null && !rightPreviewTexture.isEmpty()) {
                TextureRotation baseRotation = menu.getBaseTextureRotation();
                renderTexturePreviewBox(graphics, rightX, rightY, rightPreviewTexture, baseRotation);
            }
        }
    }
    
    /**
     * Renders power texture previews (6x6 boxes).
     * 
     * @param graphics the GUI graphics context
     * @param unpoweredX the unpowered preview X coordinate
     * @param unpoweredY the unpowered preview Y coordinate
     * @param poweredX the powered preview X coordinate
     * @param poweredY the powered preview Y coordinate
     */
    public void renderPowerTexturePreviews(@Nonnull GuiGraphics graphics, int unpoweredX, int unpoweredY,
                                           int poweredX, int poweredY) {
        String unpoweredTexture = menu.getUnpoweredTexturePreview();
        String poweredTexture = menu.getPoweredTexturePreview();
        
        // Draw unpowered texture preview
        if (!unpoweredTexture.isEmpty()) {
            renderSmallTexturePreviewBox(graphics, unpoweredX, unpoweredY, unpoweredTexture);
        } else {
            renderEmptyPreviewBox(graphics, unpoweredX, unpoweredY);
        }
        
        // Draw powered texture preview
        if (!poweredTexture.isEmpty()) {
            renderSmallTexturePreviewBox(graphics, poweredX, poweredY, poweredTexture);
        } else {
            renderEmptyPreviewBox(graphics, poweredX, poweredY);
        }
    }
    /**
     * Gets the current block state from the world.
     */
    @Nonnull
    private BlockState getCurrentBlockState() {
        BlockState defaultState = net.justsomeswitches.init.JustSomeSwitchesModBlocks.SWITCHES_LEVER.get().defaultBlockState();
        
        boolean isPowered = false;
        if (menu.getBlockPos() != null && menu.getLevel() != null) {
            try {
                BlockState worldState = menu.getLevel().getBlockState(menu.getBlockPos());
                if (worldState.getBlock() == defaultState.getBlock()) {
                    isPowered = worldState.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED);
                }
            } catch (Exception e) {
                // Fall back to unpowered state
            }
        }
        
        return defaultState
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.ATTACH_FACE,
                        net.minecraft.world.level.block.state.properties.AttachFace.WALL)
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING,
                        net.minecraft.core.Direction.NORTH)
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED, isPowered);
    }
    /**
     * Renders the block using the Dynamic Model with proper texture rotation support.
     */
    private void renderBlockWithCustomTextures(@Nonnull BlockState blockState,
                                               @Nonnull PoseStack poseStack,
                                               @Nonnull MultiBufferSource bufferSource,
                                               @Nonnull FaceSelectionData.RawTextureSelection toggleSelection,
                                               @Nonnull FaceSelectionData.RawTextureSelection baseSelection) {
        
        BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
        BakedModel baseModel = blockRenderer.getBlockModel(blockState);
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.translucent());
        
        // Create comprehensive ModelData with all current GUI state including rotation
        net.neoforged.neoforge.client.model.data.ModelData.Builder modelDataBuilder =
                net.neoforged.neoforge.client.model.data.ModelData.builder();
        
        // Add texture rotation states
        modelDataBuilder.with(SwitchBlockEntity.BASE_ROTATION,
                menu.getBaseTextureRotation().name());
        modelDataBuilder.with(SwitchBlockEntity.TOGGLE_ROTATION,
                menu.getToggleTextureRotation().name());
        
        // Add toggle slot state for conditional rotation compensation
        modelDataBuilder.with(SwitchBlockEntity.HAS_TOGGLE_BLOCK,
                !toggleSelection.sourceBlock().isEmpty());
        
        // Add current texture paths from GUI selections
        if (baseSelection.hasPreview() && baseSelection.previewTexture() != null) {
            modelDataBuilder.with(SwitchBlockEntity.BASE_TEXTURE,
                    baseSelection.previewTexture());
        }
        
        if (toggleSelection.hasPreview() && toggleSelection.previewTexture() != null) {
            modelDataBuilder.with(SwitchBlockEntity.TOGGLE_TEXTURE,
                    toggleSelection.previewTexture());
        }
        
        // Add power mode state
        modelDataBuilder.with(SwitchBlockEntity.POWER_MODE,
                menu.getPowerMode().name());
        
        // Add wall orientation (center for GUI preview)
        modelDataBuilder.with(SwitchBlockEntity.WALL_ORIENTATION, "center");
        
        // Add face selection state
        String faceSelection = baseSelection.selectedVariable() + "," + toggleSelection.selectedVariable();
        modelDataBuilder.with(SwitchBlockEntity.FACE_SELECTION, faceSelection);
        
        net.neoforged.neoforge.client.model.data.ModelData modelData = modelDataBuilder.build();
        
        RandomSource random = RandomSource.create();
        int packedLight = net.minecraft.client.renderer.LightTexture.pack(15, 15);
        int packedOverlay = 655360;
        
        // Let the Dynamic Model handle all texture processing including rotation
        for (Direction face : Direction.values()) {
            List<BakedQuad> quads = baseModel.getQuads(blockState, face, random, modelData, null);
            renderQuadsDirectly(quads, poseStack, vertexConsumer, packedLight, packedOverlay);
        }
        
        List<BakedQuad> generalQuads = baseModel.getQuads(blockState, null, random, modelData, null);
        renderQuadsDirectly(generalQuads, poseStack, vertexConsumer, packedLight, packedOverlay);
    }
    /**
     * Renders quads directly from the Dynamic Model.
     * Model has already applied all texture replacements and rotations.
     */
    private void renderQuadsDirectly(@Nonnull List<BakedQuad> quads, @Nonnull PoseStack poseStack,
                                     @Nonnull VertexConsumer vertexConsumer,
                                     int packedLight, int packedOverlay) {
        
        var lastPose = poseStack.last();
        Matrix4f pose = lastPose.pose();
        Matrix3f normal = lastPose.normal();
        
        for (BakedQuad quad : quads) {
            float brightnessMultiplier = getFaceBrightnessMultiplier(quad.getDirection());
            renderOriginalQuadWithShading(quad, pose, normal, vertexConsumer,
                    packedLight, packedOverlay, brightnessMultiplier);
        }
    }
    /**
     * Renders a quad with shading applied.
     */
    private void renderOriginalQuadWithShading(@Nonnull BakedQuad quad,
                                                @Nonnull Matrix4f pose, @Nonnull Matrix3f normal,
                                                @Nonnull VertexConsumer vertexConsumer,
                                                int packedLight, int packedOverlay,
                                                float brightnessMultiplier) {
        int[] vertexData = quad.getVertices();
        int[] shadedVertexData = applyShadingToVertexData(vertexData, brightnessMultiplier);
        renderVertexData(shadedVertexData, pose, normal, vertexConsumer, packedLight, packedOverlay);
    }
    /**
     * Apply brightness shading to vertex data.
     */
    @Nonnull
    private int[] applyShadingToVertexData(@Nonnull int[] originalVertices, float brightnessMultiplier) {
        int[] newVertices = originalVertices.clone();
        
        for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
            int baseIndex = vertexIndex * 8;
            
            int originalColor = originalVertices[baseIndex + 3];
            int shadedColor = applyBrightnessShading(originalColor, brightnessMultiplier);
            newVertices[baseIndex + 3] = shadedColor;
        }
        
        return newVertices;
    }
    /**
     * Apply brightness shading to color.
     */
    private int applyBrightnessShading(int originalColor, float brightnessMultiplier) {
        int alpha = (originalColor >> 24) & 0xFF;
        int red = (int)(((originalColor >> 16) & 0xFF) * brightnessMultiplier);
        int green = (int)(((originalColor >> 8) & 0xFF) * brightnessMultiplier);
        int blue = (int)((originalColor & 0xFF) * brightnessMultiplier);
        
        red = Math.max(0, Math.min(255, red));
        green = Math.max(0, Math.min(255, green));
        blue = Math.max(0, Math.min(255, blue));
        
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }
    /**
     * Renders vertex data to the vertex consumer.
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
            float nx = (normalData & 0xFF) / 127.5f - 1.0f;
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
     * Gets brightness multiplier for face-based shading.
     */
    private float getFaceBrightnessMultiplier(@Nullable Direction face) {
        if (face == null) {
            return 0.4f;  // General quads - 40%
        }
        
        return switch (face) {
            case UP -> 1.0f;
            case DOWN -> 0.4f;
            case NORTH -> 0.8f;
            case SOUTH -> 0.4f;
            case EAST -> 0.8f;
            case WEST -> 0.4f;
        };
    }
    /**
     * Renders a basic 3D switch preview as fallback.
     */
    private void renderBasic3DPreview(@Nonnull GuiGraphics graphics, int centerX, int centerY) {
        try {
            ItemStack switchStack = new ItemStack(
                    net.justsomeswitches.init.JustSomeSwitchesModBlocks.SWITCHES_LEVER_ITEM.get());
            
            graphics.pose().pushPose();
            graphics.pose().translate(centerX, centerY, 100);
            graphics.pose().scale(PREVIEW_SCALE, PREVIEW_SCALE, 1.0f);
            graphics.pose().translate(-8, -8, 0);
            graphics.renderItem(switchStack, 0, 0);
            graphics.pose().popPose();
        } catch (Exception e) {
            renderFallbackText(graphics, centerX, centerY);
        }
    }
    /**
     * Draws fallback text when 3D preview rendering fails.
     */
    private void renderFallbackText(@Nonnull GuiGraphics graphics, int centerX, int centerY) {
        Component previewText = Component.literal("Switch");
        int textWidth = this.font.width(previewText);
        int textX = centerX - textWidth / 2;
        int textY = centerY - 4;
        graphics.drawString(this.font, previewText, textX, textY, 0x404040, false);
    }
    /**
     * Draws a 2D texture preview box.
     */
    private void renderTexturePreviewBox(@Nonnull GuiGraphics graphics, int x, int y,
                                         @Nonnull String texturePath, @Nullable TextureRotation rotation) {
        try {
            TextureAtlasSprite sprite = getTextureSprite(texturePath);
            if (sprite != null && !getSafeSpriteName(sprite).contains("missingno")) {
                graphics.fill(x, y, x + PREVIEW_SIZE, y + PREVIEW_SIZE, 0xFFFFFFFF);
                if (rotation != null && rotation != TextureRotation.NORMAL) {
                    renderRotatedTexturePreview(graphics, x, y, sprite, rotation);
                } else {
                    graphics.blit(x, y, 0, PREVIEW_SIZE, PREVIEW_SIZE, sprite);
                }
            } else {
                graphics.fill(x, y, x + PREVIEW_SIZE, y + PREVIEW_SIZE, 0xFFFF00FF);
            }
        } catch (Exception e) {
            graphics.fill(x, y, x + PREVIEW_SIZE, y + PREVIEW_SIZE, 0xFFFF0000);
        }
    }
    /**
     * Draws a rotated texture preview using transformation matrices.
     */
    private void renderRotatedTexturePreview(@Nonnull GuiGraphics graphics, int x, int y,
                                             @Nonnull TextureAtlasSprite sprite, @Nonnull TextureRotation rotation) {
        graphics.pose().pushPose();
        
        // Translate to center of preview area
        graphics.pose().translate(x + PREVIEW_SIZE / 2f, y + PREVIEW_SIZE / 2f, 0);
        
        // Apply rotation
        graphics.pose().mulPose(new org.joml.Quaternionf().fromAxisAngleDeg(0, 0, 1, rotation.getDegrees()));
        
        // Translate back and draw
        graphics.pose().translate(-PREVIEW_SIZE / 2f, -PREVIEW_SIZE / 2f, 0);
        graphics.blit(0, 0, 0, PREVIEW_SIZE, PREVIEW_SIZE, sprite);
        
        graphics.pose().popPose();
    }
    /**
     * Returns cached ResourceLocation, creating if needed.
     */
    @Nonnull
    private ResourceLocation getCachedResourceLocation(@Nonnull String path) {
        return resourceLocationCache.computeIfAbsent(path, ResourceLocation::new);
    }
    /**
     * Returns cached sprite name, caching if needed.
     */
    @Nonnull
    private String getCachedSpriteName(@Nonnull TextureAtlasSprite sprite) {
        return spriteNameCache.computeIfAbsent(sprite, this::computeSpriteName);
    }
    /**
     * Computes sprite name by accessing sprite contents.
     * CRITICAL: Never close sprite contents - sprites manage their own lifecycle.
     * Premature closure crashes animated textures.
     */
    @Nonnull
    @SuppressWarnings("resource") // Sprite contents must NOT be closed - managed by Minecraft
    private String computeSpriteName(@Nonnull TextureAtlasSprite sprite) {
        try {
            var contents = sprite.contents();
            return contents.name().toString();
        } catch (Exception e) {
            return "missingno";
        }
    }
    /**
     * Gets texture sprite for 2D preview rendering.
     */
    @Nullable
    private TextureAtlasSprite getTextureSprite(@Nonnull String texturePath) {
        try {
            ResourceLocation textureLocation = getCachedResourceLocation(texturePath);
            TextureAtlasSprite sprite = Minecraft.getInstance()
                    .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                    .apply(textureLocation);
            
            if (sprite != null) {
                String spriteName = getCachedSpriteName(sprite);
                if (!spriteName.contains("missingno")) {
                    return sprite;
                }
            }
            // Try fallback patterns for face-specific textures
            if (texturePath.contains("_top") || texturePath.contains("_side") || texturePath.contains("_front")) {
                String basePath = texturePath.replaceAll("_(top|side|front)$", "");
                ResourceLocation fallbackLocation = getCachedResourceLocation(basePath);
                TextureAtlasSprite fallbackSprite = Minecraft.getInstance()
                        .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                        .apply(fallbackLocation);
                
                if (fallbackSprite != null) {
                    String fallbackSpriteName = getCachedSpriteName(fallbackSprite);
                    if (!fallbackSpriteName.contains("missingno")) {
                        return fallbackSprite;
                    }
                }
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    /**
     * Safely retrieves sprite name using cache.
     */
    @Nonnull
    private String getSafeSpriteName(@Nonnull TextureAtlasSprite sprite) {
        return getCachedSpriteName(sprite);
    }
    /**
     * Draws a small 6x6 texture preview showing the specific 2x2 UV area rendered on the model.
     * Provides exact preview of what appears on the switch.
     */
    private void renderSmallTexturePreviewBox(@Nonnull GuiGraphics graphics, int x, int y, @Nonnull String texturePath) {
        try {
            TextureAtlasSprite sprite = getTextureSprite(texturePath);
            if (sprite != null) {
                String spriteName = getCachedSpriteName(sprite);
                if (!spriteName.contains("missingno")) {
                    graphics.fill(x, y, x + POWER_PREVIEW_SIZE, y + POWER_PREVIEW_SIZE, 0xFFFFFFFF);
                    renderUVSpecificPreview(graphics, x, y, sprite);
                } else {
                    renderEmptyPreviewBox(graphics, x, y);
                }
            } else {
                renderEmptyPreviewBox(graphics, x, y);
            }
        } catch (Exception e) {
            renderEmptyPreviewBox(graphics, x, y);
        }
    }
    /**
     * Renders UV-specific power texture preview using scissor clipping.
     * UV (2,6) with 2x2 area → Texture pixels 2-3 (width), 6-7 (height).
     * GUI pixels: 6-11 (width), 18-23 (height) = 6x6 preview area.
     */
    private void renderUVSpecificPreview(@Nonnull GuiGraphics graphics, int x, int y, @Nonnull TextureAtlasSprite sprite) {
        try {
            graphics.fill(x, y, x + POWER_PREVIEW_SIZE, y + POWER_PREVIEW_SIZE, 0xFFFFFFFF);
            int offsetX = 6;   // UV X coordinate 2 * 3 (scale factor)
            int offsetY = 18;  // UV Y coordinate 6 * 3 (scale factor)
            // Calculate texture position so target UV area appears in preview
            int textureX = x - offsetX;
            int textureY = y - offsetY;
            graphics.enableScissor(x, y, x + POWER_PREVIEW_SIZE, y + POWER_PREVIEW_SIZE);
            graphics.blit(textureX, textureY, 0, 48, 48, sprite);
            graphics.disableScissor();
            
        } catch (Exception e) {
            // Fallback to a solid color if rendering fails
            graphics.fill(x, y, x + POWER_PREVIEW_SIZE, y + POWER_PREVIEW_SIZE, 0xFFCCCCCC);
        }
    }
    /**
     * Draws an empty preview box.
     */
    private void renderEmptyPreviewBox(@Nonnull GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + POWER_PREVIEW_SIZE, y + POWER_PREVIEW_SIZE, 0xFFFFFFFF);
    }
}
