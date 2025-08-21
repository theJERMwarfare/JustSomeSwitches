package net.justsomeswitches.gui;

import net.justsomeswitches.blockentity.SwitchesLeverBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

/**
 * Switch Texture Screen for texture customization GUI.
 * <p>
 * Provides an intuitive interface for selecting and applying custom textures
 * to switch blocks with real-time preview, dropdown face selection, and
 * immediate texture application. Features professional resource management
 * and null safety for optimal stability.
 * 
 * @since 1.0.0
 */
public class SwitchTextureScreen extends AbstractContainerScreen<SwitchTextureMenu> {

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 176;

    // GUI background texture
    private static final ResourceLocation GUI_BACKGROUND = new ResourceLocation("justsomeswitches", "textures/gui/switch_texture_gui.png");

    // Central 3D preview positioning and scaling
    private static final int PREVIEW_CENTER_X = 82;  // Moved 1px closer to left edge (final positioning)
    private static final int PREVIEW_CENTER_Y = 34;  // Moved up 1px closer to top edge (final positioning)
    private static final float PREVIEW_SCALE = 2.0f; // 2x normal item size (32x32 pixels)

    // Face dropdown positioning
    private static final int LEFT_FACE_X = 14;
    private static final int LEFT_FACE_Y = 45;  // Moved up 2px closer to top edge
    private static final int RIGHT_FACE_X = 118;
    private static final int RIGHT_FACE_Y = 45;  // Moved up 2px closer to top edge
    private static final int FACE_DROPDOWN_WIDTH = 44;
    private static final int FACE_DROPDOWN_HEIGHT = 12;

    // 2D texture preview positioning
    private static final int LEFT_PREVIEW_X = 27;
    private static final int LEFT_PREVIEW_Y = 63;  // Moved up 1px closer to top edge (final positioning)
    private static final int RIGHT_PREVIEW_X = 131;
    private static final int RIGHT_PREVIEW_Y = 63;  // Moved up 1px closer to top edge (final positioning)
    private static final int PREVIEW_SIZE = 18;

    // Power category positioning
    private static final int POWER_DROPDOWN_X = 64;
    private static final int POWER_DROPDOWN_Y = 49;
    private static final int POWER_DROPDOWN_WIDTH = 48;
    private static final int POWER_DROPDOWN_HEIGHT = 12;
    
    // Power texture preview positioning (6x6 previews)
    private static final int UNPOWERED_PREVIEW_X = 57;
    private static final int UNPOWERED_PREVIEW_Y = 65;
    private static final int POWERED_PREVIEW_X = 63;
    private static final int POWERED_PREVIEW_Y = 76;
    private static final int POWER_PREVIEW_SIZE = 6;
    
    // Power label positioning
    private static final int UNPOWERED_LABEL_X = 68;
    private static final int UNPOWERED_LABEL_Y = 64;
    private static final int POWERED_LABEL_X = 74;
    private static final int POWERED_LABEL_Y = 75;

    // Current dynamic state
    private FaceSelectionData.RawTextureSelection leftTextureSelection = FaceSelectionData.RawTextureSelection.createDisabled();
    private FaceSelectionData.RawTextureSelection rightTextureSelection = FaceSelectionData.RawTextureSelection.createDisabled();

    // Dropdown popup management
    private boolean showingLeftDropdown = false;
    private boolean showingRightDropdown = false;
    private boolean showingPowerDropdown = false;

    // Previous selections for change detection
    private ItemStack previousLeftItem = ItemStack.EMPTY;
    private ItemStack previousRightItem = ItemStack.EMPTY;
    
    // Previous selections for live preview change detection
    private String previousBaseTexture = null;
    private String previousToggleTexture = null;
    private SwitchesLeverBlockEntity.PowerMode previousPowerMode = null;

    /**
     * Creates a new Switch Texture Screen.
     * 
     * @param menu the texture menu container
     * @param playerInventory the player's inventory
     * @param title the screen title component
     */
    public SwitchTextureScreen(@Nonnull SwitchTextureMenu menu, @Nonnull Inventory playerInventory, @Nonnull Component title) {
        super(menu, playerInventory, title);

        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;

        this.titleLabelX = 8;
        this.titleLabelY = 10;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 86;
    }
    
    /**
     * Get brightness multiplier for face-based shading.
     * Final brightness values for professional 3D shading effect.
     * 
     * @param face the block face direction (can be null for general quads)
     * @return brightness multiplier (1.0 = full bright, 0.4 = darker shadow)
     */
    private float getFaceBrightnessMultiplier(@Nullable Direction face) {
        if (face == null) {
            return 0.4f;  // General quads - 40%
        }
        
        return switch (face) {
            case UP -> 1.0f;      // Up - 100%
            case DOWN -> 0.4f;    // Down - 40%
            case NORTH -> 0.8f;   // North - 80%
            case SOUTH -> 0.4f;   // South - 40%
            case EAST -> 0.8f;    // East - 80%
            case WEST -> 0.4f;    // West - 40%
        };
    }
    
    /**
     * Render quad with custom texture and brightness shading.
     */
    private void renderQuadWithCustomTextureAndShading(@Nonnull BakedQuad originalQuad,
                                                        @Nonnull TextureAtlasSprite newSprite,
                                                        @Nonnull Matrix4f pose, @Nonnull Matrix3f normal,
                                                        @Nonnull VertexConsumer vertexConsumer,
                                                        int packedLight, int packedOverlay,
                                                        float brightnessMultiplier) {
        
        int[] vertexData = originalQuad.getVertices();
        TextureAtlasSprite originalSprite = originalQuad.getSprite();
        
        int[] newVertexData = transformVertexDataWithShading(vertexData, originalSprite, newSprite, brightnessMultiplier);
        renderVertexData(newVertexData, pose, normal, vertexConsumer, packedLight, packedOverlay);
    }
    
    /**
     * Render original quad with brightness shading.
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

    @Override
    protected void init() {
        super.init();
        
        // Complete menu initialization now that GUI is fully rendered
        // This enables slot analysis for user interactions while preventing
        // premature analysis during GUI setup that would overwrite persisted values
        menu.completeInitialization();
        
        updateUIState();
    }

    @Override
    public void containerTick() {
        super.containerTick();
        updateUIState();
    }

    /**
     * Updates UI state with intelligent change detection.
     * <p>
     * Monitors slot changes and manages dropdown states while preserving
     * user selections and providing appropriate cleanup when blocks are removed.
     */
    private void updateUIState() {
        // Get current state from menu
        FaceSelectionData.RawTextureSelection newLeftSelection = menu.getToggleTextureSelection();
        FaceSelectionData.RawTextureSelection newRightSelection = menu.getBaseTextureSelection();

        // Detect slot changes for cleanup only (auto-apply handled in menu)
        ItemStack currentLeftItem = newLeftSelection.sourceBlock();
        ItemStack currentRightItem = newRightSelection.sourceBlock();

        // Handle left (toggle) slot changes
        if (!ItemStack.matches(previousLeftItem, currentLeftItem)) {
            if (currentLeftItem.isEmpty()) {
                handleBlockRemoval(true);
            }
            previousLeftItem = currentLeftItem.copy();
        }

        // Handle right (base) slot changes
        if (!ItemStack.matches(previousRightItem, currentRightItem)) {
            if (currentRightItem.isEmpty()) {
                handleBlockRemoval(false);
            }
            previousRightItem = currentRightItem.copy();
        }

        // Update state
        leftTextureSelection = newLeftSelection;
        rightTextureSelection = newRightSelection;
        
        // Check for texture/power changes that should update preview
        boolean previewNeedsUpdate = false;
        
        // Detect base texture changes
        if (!Objects.equals(previousBaseTexture, rightTextureSelection.previewTexture())) {
            previewNeedsUpdate = true;
            previousBaseTexture = rightTextureSelection.previewTexture();
        }
        
        // Detect toggle texture changes
        if (!Objects.equals(previousToggleTexture, leftTextureSelection.previewTexture())) {
            previewNeedsUpdate = true;
            previousToggleTexture = leftTextureSelection.previewTexture();
        }
        
        // Detect power mode changes
        if (previousPowerMode != menu.getPowerMode()) {
            previewNeedsUpdate = true;
            previousPowerMode = menu.getPowerMode();
        }
        
        // Preview will update automatically on next renderBg() call if needed
    }

    /**
     * Renders the live 3D preview in the GUI center using direct block rendering.
     * This approach applies custom textures directly to the block model for accurate preview.
     * 
     * @param graphics the GUI graphics context
     * @param guiLeft the GUI left offset
     * @param guiTop the GUI top offset
     */
    private void drawLive3DPreview(@Nonnull GuiGraphics graphics, int guiLeft, int guiTop) {
        try {
            // Get the actual switch block state from the world
            BlockState switchState = getCurrentBlockState();
            
            // Calculate center position for 2x scaled preview
            int centerX = guiLeft + PREVIEW_CENTER_X;
            int centerY = guiTop + PREVIEW_CENTER_Y;
            
            // Set up pose stack for 3D rendering with proper isometric view
            PoseStack poseStack = graphics.pose();
            poseStack.pushPose();
            
            // Position and scale the preview for 2x normal item size
            poseStack.translate(centerX, centerY, 100);
            poseStack.scale(PREVIEW_SCALE * 16, -PREVIEW_SCALE * 16, PREVIEW_SCALE * 16);
            
            // Apply inventory-style rotations and lighting
            // Final angle refinements: X=10° for optimal vertical angle, Y=-215° for perfect front view
            // Position moved for better GUI centering
            poseStack.mulPose(new org.joml.Quaternionf().fromAxisAngleDeg(1, 0, 0, 10f));    // X: Refined vertical angle
            poseStack.mulPose(new org.joml.Quaternionf().fromAxisAngleDeg(0, 1, 0, -215f));  // Y: Perfect front top-right corner
            
            // Center the block
            poseStack.translate(-0.5f, -0.5f, -0.5f);
            
            // Get buffer source for rendering with inventory-style lighting
            MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
            
            // Render the block with custom textures and inventory-style lighting
            renderBlockWithCustomTextures(switchState, poseStack, bufferSource);
            
            // Finish rendering
            bufferSource.endBatch();
            
            poseStack.popPose();
            
        } catch (Exception e) {
            // Graceful fallback to current static preview
            drawBasic3DPreview(graphics, guiLeft, guiTop);
        }
    }
    
    /**
     * Gets the current block state from the world, including power state.
     * FIXED: Force consistent GUI preview orientation regardless of world placement.
     * 
     * @return the current block state with proper powered/unpowered state and consistent GUI orientation
     */
    @Nonnull
    private BlockState getCurrentBlockState() {
        // Start with default block state
        BlockState defaultState = net.justsomeswitches.init.JustSomeSwitchesModBlocks.SWITCHES_LEVER.get().defaultBlockState();
        
        // Get the powered state from the actual world block if available
        boolean isPowered = false;
        if (menu.getBlockPos() != null && menu.getLevel() != null) {
            try {
                BlockState worldState = menu.getLevel().getBlockState(menu.getBlockPos());
                if (worldState.getBlock() == defaultState.getBlock()) {
                    // Extract only the powered state, ignore placement orientation
                    isPowered = worldState.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED);
                }
            } catch (Exception e) {
                // Fall back to unpowered state if world access fails
            }
        }
        
        // FIXED: Always use wall,facing=north orientation for consistent GUI preview
        // This ensures the GUI 3D preview always looks the same regardless of actual placement
        return defaultState
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.ATTACH_FACE, 
                         net.minecraft.world.level.block.state.properties.AttachFace.WALL)
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, 
                         net.minecraft.core.Direction.NORTH)
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED, isPowered);
    }
    
    /**
     * Renders the block with custom texture replacement applied.
     * Uses similar logic to the BlockEntityRenderer for consistency.
     */
    private void renderBlockWithCustomTextures(@Nonnull BlockState blockState, 
                                               @Nonnull PoseStack poseStack,
                                               @Nonnull MultiBufferSource bufferSource) {
        
        BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
        BakedModel baseModel = blockRenderer.getBlockModel(blockState);
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.translucent());  // Try translucent for better lighting
        
        // Get current texture selections
        TextureAtlasSprite baseSprite = getCustomTextureSprite(rightTextureSelection);
        TextureAtlasSprite toggleSprite = getCustomTextureSprite(leftTextureSelection);
        
        // Get power category textures based on current mode
        TextureAtlasSprite unpoweredSprite = getPowerTextureSprite(false);
        TextureAtlasSprite poweredSprite = getPowerTextureSprite(true);
        
        RandomSource random = RandomSource.create();
        // Back to original bright lighting for good visibility
        int packedLight = net.minecraft.client.renderer.LightTexture.pack(15, 15);  // Original bright setting
        int packedOverlay = 655360;  // No overlay
        
        // Process face-specific quads with brightness shading
        for (Direction face : Direction.values()) {
            List<BakedQuad> quads = baseModel.getQuads(blockState, face, random, 
                net.neoforged.neoforge.client.model.data.ModelData.EMPTY, null);
            
            processCustomQuads(quads, poseStack, vertexConsumer, baseSprite, toggleSprite,
                    unpoweredSprite, poweredSprite, packedLight, packedOverlay);
        }
        
        // Process general quads with brightness shading
        List<BakedQuad> generalQuads = baseModel.getQuads(blockState, null, random, 
            net.neoforged.neoforge.client.model.data.ModelData.EMPTY, null);
        processCustomQuads(generalQuads, poseStack, vertexConsumer, baseSprite, toggleSprite,
                unpoweredSprite, poweredSprite, packedLight, packedOverlay);
    }
    
    /**
     * Gets custom texture sprite for current selection.
     */
    @Nullable
    private TextureAtlasSprite getCustomTextureSprite(@Nonnull FaceSelectionData.RawTextureSelection selection) {
        if (!selection.hasPreview()) {
            return null;
        }
        
        String texturePath = selection.previewTexture();
        if (texturePath == null || texturePath.isEmpty()) {
            return null;
        }
        
        return getTextureSprite(texturePath);
    }
    
    /**
     * Gets power category texture sprite based on power state.
     */
    @Nullable
    private TextureAtlasSprite getPowerTextureSprite(boolean powered) {
        String texturePath = powered ? menu.getPoweredTexturePreview() : menu.getUnpoweredTexturePreview();
        
        if (texturePath == null || texturePath.isEmpty()) {
            return null;
        }
        
        return getTextureSprite(texturePath);
    }
    
    /**
     * Process quads and apply custom texture replacement with face-based color shading.
     */
    private void processCustomQuads(@Nonnull List<BakedQuad> quads, @Nonnull PoseStack poseStack,
                                    @Nonnull VertexConsumer vertexConsumer,
                                    @Nullable TextureAtlasSprite baseSprite,
                                    @Nullable TextureAtlasSprite toggleSprite,
                                    @Nullable TextureAtlasSprite unpoweredSprite,
                                    @Nullable TextureAtlasSprite poweredSprite,
                                    int packedLight, int packedOverlay) {
        
        var lastPose = poseStack.last();
        Matrix4f pose = lastPose.pose();
        Matrix3f normal = lastPose.normal();
        
        for (BakedQuad quad : quads) {
            TextureAtlasSprite originalSprite = quad.getSprite();
            
            TextureAtlasSprite replacementSprite = determineReplacementTexture(
                    originalSprite, baseSprite, toggleSprite, unpoweredSprite, poweredSprite);
            
            // Get face-based brightness multiplier for shading
            float brightnessMultiplier = getFaceBrightnessMultiplier(quad.getDirection());
            
            if (replacementSprite != null && replacementSprite != originalSprite) {
                renderQuadWithCustomTextureAndShading(quad, replacementSprite, pose, normal,
                        vertexConsumer, packedLight, packedOverlay, brightnessMultiplier);
            } else {
                renderOriginalQuadWithShading(quad, pose, normal, vertexConsumer, 
                        packedLight, packedOverlay, brightnessMultiplier);
            }
        }
    }
    
    /**
     * Determine which replacement texture to use based on original texture.
     */
    @Nullable
    private TextureAtlasSprite determineReplacementTexture(@Nonnull TextureAtlasSprite originalSprite,
                                                           @Nullable TextureAtlasSprite baseSprite,
                                                           @Nullable TextureAtlasSprite toggleSprite,
                                                           @Nullable TextureAtlasSprite unpoweredSprite,
                                                           @Nullable TextureAtlasSprite poweredSprite) {
        
        String originalName = getSafeSpriteName(originalSprite);
        
        // Power category textures take priority
        if (isPoweredTexture(originalName) && poweredSprite != null) {
            return poweredSprite;
        }
        
        if (isUnpoweredTexture(originalName) && unpoweredSprite != null) {
            return unpoweredSprite;
        }
        
        // Skip textures that shouldn't be replaced
        if (shouldExcludeFromReplacement(originalName)) {
            return null;
        }
        
        // Apply base/toggle texture replacements
        if (isBaseTexture(originalName) && baseSprite != null) {
            return baseSprite;
        }
        
        if (isToggleTexture(originalName) && toggleSprite != null) {
            return toggleSprite;
        }
        
        return null;
    }
    
    /**
     * Check if texture should be excluded from replacement.
     */
    private boolean shouldExcludeFromReplacement(@Nonnull String textureName) {
        return textureName.contains("redstone") ||
                textureName.contains("_on") ||
                textureName.contains("_off") ||
                textureName.contains("gray_concrete_powder");
    }
    
    /**
     * Check if this is a powered texture that should be replaced by power category.
     */
    private boolean isPoweredTexture(@Nonnull String textureName) {
        return textureName.contains("redstone_block") ||
                textureName.contains("switches_lever_powered") ||
                textureName.contains("powered") ||
                (textureName.contains("lever") && textureName.contains("on"));
    }
    
    /**
     * Check if this is an unpowered texture that should be replaced by power category.
     */
    private boolean isUnpoweredTexture(@Nonnull String textureName) {
        return textureName.contains("gray_concrete_powder") ||
                textureName.contains("switches_lever_unpowered") ||
                textureName.contains("unpowered") ||
                (textureName.contains("lever") && textureName.contains("off"));
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
     * Transform vertex data to use new texture coordinates with brightness shading.
     */
    @Nonnull
    private int[] transformVertexDataWithShading(@Nonnull int[] originalVertices,
                                                  @Nonnull TextureAtlasSprite originalTexture,
                                                  @Nonnull TextureAtlasSprite newTexture,
                                                  float brightnessMultiplier) {
        
        int[] newVertices = originalVertices.clone();
        
        for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
            int baseIndex = vertexIndex * 8;
            
            // Transform texture coordinates
            float originalU = Float.intBitsToFloat(originalVertices[baseIndex + 4]);
            float originalV = Float.intBitsToFloat(originalVertices[baseIndex + 5]);
            
            float newU = transformU(originalU, originalTexture, newTexture);
            float newV = transformV(originalV, originalTexture, newTexture);
            
            newVertices[baseIndex + 4] = Float.floatToIntBits(newU);
            newVertices[baseIndex + 5] = Float.floatToIntBits(newV);
            
            // Apply brightness shading
            int originalColor = originalVertices[baseIndex + 3];
            int shadedColor = applyBrightnessShading(originalColor, brightnessMultiplier);
            newVertices[baseIndex + 3] = shadedColor;
        }
        
        return newVertices;
    }
    
    /**
     * Apply brightness shading to vertex data without texture changes.
     */
    @Nonnull
    private int[] applyShadingToVertexData(@Nonnull int[] originalVertices, float brightnessMultiplier) {
        int[] newVertices = originalVertices.clone();
        
        for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
            int baseIndex = vertexIndex * 8;
            
            // Apply brightness shading
            int originalColor = originalVertices[baseIndex + 3];
            int shadedColor = applyBrightnessShading(originalColor, brightnessMultiplier);
            newVertices[baseIndex + 3] = shadedColor;
        }
        
        return newVertices;
    }
    
    /**
     * Apply brightness shading to color.
     * Multiplies RGB channels by brightness multiplier for realistic 3D shading.
     */
    private int applyBrightnessShading(int originalColor, float brightnessMultiplier) {
        int alpha = (originalColor >> 24) & 0xFF;
        int red = (int)(((originalColor >> 16) & 0xFF) * brightnessMultiplier);
        int green = (int)(((originalColor >> 8) & 0xFF) * brightnessMultiplier);
        int blue = (int)((originalColor & 0xFF) * brightnessMultiplier);
        
        // Clamp values to valid range
        red = Math.max(0, Math.min(255, red));
        green = Math.max(0, Math.min(255, green));
        blue = Math.max(0, Math.min(255, blue));
        
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
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
     * Render vertex data to vertex consumer.
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
     * Handles cleanup when a texture block is removed from a slot.
     * 
     * @param isLeft true if the left (toggle) slot was cleared, false for right (base) slot
     */
    private void handleBlockRemoval(boolean isLeft) {
        // Close any open dropdowns
        showingLeftDropdown = false;
        showingRightDropdown = false;

        // Reset selection to defaults (handled by menu auto-apply)
        if (isLeft) {
            menu.setToggleTextureVariable("all");
        } else {
            menu.setBaseTextureVariable("all");
        }
    }

    /**
     * Enhanced mouse click handling for dropdown interactions.
     * <p>
     * Manages dropdown opening/closing and option selection with proper
     * bounds checking and immediate texture application.
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int guiLeft = (this.width - this.imageWidth) / 2;
        int guiTop = (this.height - this.imageHeight) / 2;

        // Handle dropdown clicks with proper bounds checking
        if (isWithinDropdownBounds(mouseX, mouseY, guiLeft + LEFT_FACE_X, guiTop + LEFT_FACE_Y)) {
            if (leftTextureSelection.enabled()) {
                toggleLeftDropdown();
                return true;
            }
        }

        if (isWithinDropdownBounds(mouseX, mouseY, guiLeft + RIGHT_FACE_X, guiTop + RIGHT_FACE_Y)) {
            if (rightTextureSelection.enabled()) {
                toggleRightDropdown();
                return true;
            }
        }

        // Handle power dropdown click
        if (isWithinPowerDropdownBounds(mouseX, mouseY, guiLeft + POWER_DROPDOWN_X, guiTop + POWER_DROPDOWN_Y)) {
            togglePowerDropdown();
            return true;
        }

        // Handle dropdown selection clicks
        if (showingLeftDropdown && handleDropdownSelection(mouseX, mouseY, guiLeft, guiTop, true)) {
            return true;
        }

        if (showingRightDropdown && handleDropdownSelection(mouseX, mouseY, guiLeft, guiTop, false)) {
            return true;
        }

        // Handle power dropdown selection clicks
        if (showingPowerDropdown && handlePowerDropdownSelection(mouseX, mouseY, guiLeft, guiTop)) {
            return true;
        }

        // Close dropdowns if clicking elsewhere
        if (showingLeftDropdown || showingRightDropdown || showingPowerDropdown) {
            showingLeftDropdown = false;
            showingRightDropdown = false;
            showingPowerDropdown = false;
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * Optimized bounds checking for dropdown areas.
     * <p>
     * Uses constants directly to eliminate redundant parameter warnings.
     * 
     * @param mouseX the mouse X coordinate
     * @param mouseY the mouse Y coordinate
     * @param x the dropdown X position
     * @param y the dropdown Y position
     * @return true if mouse is within dropdown bounds
     */
    private boolean isWithinDropdownBounds(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + FACE_DROPDOWN_WIDTH && mouseY >= y && mouseY < y + FACE_DROPDOWN_HEIGHT;
    }

    /**
     * Optimized bounds checking for power dropdown area.
     */
    private boolean isWithinPowerDropdownBounds(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + POWER_DROPDOWN_WIDTH && mouseY >= y && mouseY < y + POWER_DROPDOWN_HEIGHT;
    }

    /**
     * Toggles the left (toggle) dropdown state.
     */
    private void toggleLeftDropdown() {
        showingLeftDropdown = !showingLeftDropdown;
        showingRightDropdown = false;
        showingPowerDropdown = false;
    }

    /**
     * Toggles the right (base) dropdown state.
     */
    private void toggleRightDropdown() {
        showingRightDropdown = !showingRightDropdown;
        showingLeftDropdown = false;
        showingPowerDropdown = false;
    }

    /**
     * Toggles the power dropdown state.
     */
    private void togglePowerDropdown() {
        showingPowerDropdown = !showingPowerDropdown;
        showingLeftDropdown = false;
        showingRightDropdown = false;
    }

    /**
     * Handles dropdown option selection with immediate texture application.
     * 
     * @param mouseX the mouse X coordinate
     * @param mouseY the mouse Y coordinate
     * @param guiLeft the GUI left offset
     * @param guiTop the GUI top offset
     * @param isLeft true for left dropdown, false for right dropdown
     * @return true if a selection was made
     */
    private boolean handleDropdownSelection(double mouseX, double mouseY, int guiLeft, int guiTop, boolean isLeft) {
        FaceSelectionData.RawTextureSelection selection = isLeft ? leftTextureSelection : rightTextureSelection;
        List<String> variables = selection.availableVariables();

        int dropdownX = isLeft ? guiLeft + LEFT_FACE_X : guiLeft + RIGHT_FACE_X;
        int dropdownY = (isLeft ? guiTop + LEFT_FACE_Y : guiTop + RIGHT_FACE_Y) + FACE_DROPDOWN_HEIGHT;

        for (int i = 0; i < variables.size(); i++) {
            int optionY = dropdownY + (i * 12);
            if (isWithinDropdownBounds(mouseX, mouseY, dropdownX, optionY)) {
                // Selection made - triggers immediate apply in menu
                String selectedVariable = variables.get(i);

                if (isLeft) {
                    menu.setToggleTextureVariable(selectedVariable);
                    showingLeftDropdown = false;
                } else {
                    menu.setBaseTextureVariable(selectedVariable);
                    showingRightDropdown = false;
                }

                return true;
            }
        }

        return false;
    }

    /**
     * Handles power dropdown option selection with immediate power mode application.
     */
    private boolean handlePowerDropdownSelection(double mouseX, double mouseY, int guiLeft, int guiTop) {
        SwitchesLeverBlockEntity.PowerMode[] modes = SwitchesLeverBlockEntity.PowerMode.values();
        
        int dropdownX = guiLeft + POWER_DROPDOWN_X;
        int dropdownY = guiTop + POWER_DROPDOWN_Y + POWER_DROPDOWN_HEIGHT;
        
        for (int i = 0; i < modes.length; i++) {
            int optionY = dropdownY + (i * 12);
            if (isWithinPowerDropdownBounds(mouseX, mouseY, dropdownX, optionY)) {
                // Selection made - triggers immediate apply in menu
                SwitchesLeverBlockEntity.PowerMode selectedMode = modes[i];
                menu.setPowerMode(selectedMode);
                showingPowerDropdown = false;
                return true;
            }
        }
        
        return false;
    }

    @Override
    protected void renderBg(@Nonnull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int guiLeft = (this.width - this.imageWidth) / 2;
        int guiTop = (this.height - this.imageHeight) / 2;

        // Draw GUI background
        graphics.blit(GUI_BACKGROUND, guiLeft, guiTop + 4, 0, 0, this.imageWidth, this.imageHeight, 256, 256);

        // Draw live 3D switch preview with current textures
        drawLive3DPreview(graphics, guiLeft, guiTop);

        // Draw face selection dropdowns
        drawCleanArchitectureDropdowns(graphics, guiLeft, guiTop);

        // Draw power category dropdown and previews
        drawPowerCategoryElements(graphics, guiLeft, guiTop);

        // Draw working 2D texture previews with null safety
        drawSafeTexturePreview(graphics, guiLeft, guiTop);
    }

    /**
     * Renders a basic 3D switch preview in the center of the GUI.
     * <p>
     * Shows the default switch ItemStack at 2x scale with proper error handling
     * and fallback to text display if 3D rendering fails.
     * 
     * @param graphics the GUI graphics context
     * @param guiLeft the GUI left offset
     * @param guiTop the GUI top offset
     */
    private void drawBasic3DPreview(@Nonnull GuiGraphics graphics, int guiLeft, int guiTop) {
        try {
            // Create basic switch ItemStack (no custom textures, no NBT)
            ItemStack switchStack = new ItemStack(
                    net.justsomeswitches.init.JustSomeSwitchesModBlocks.SWITCHES_LEVER_ITEM.get()
            );

            // Calculate center position for 2x scaled item
            int centerX = guiLeft + PREVIEW_CENTER_X;
            int centerY = guiTop + PREVIEW_CENTER_Y;

            // Render 3D switch preview at 2x scale
            graphics.pose().pushPose();
            graphics.pose().translate(centerX, centerY, 100);
            graphics.pose().scale(PREVIEW_SCALE, PREVIEW_SCALE, 1.0f);
            graphics.pose().translate(-8, -8, 0); // Center the 16x16 item

            // Render the basic switch ItemStack (default textures)
            graphics.renderItem(switchStack, 0, 0);

            graphics.pose().popPose();

        } catch (Exception e) {
            // Fallback to text if 3D rendering fails
            drawFallbackText(graphics, guiLeft, guiTop);
        }
    }

    /**
     * Draws fallback text when 3D preview rendering fails.
     * 
     * @param graphics the GUI graphics context
     * @param guiLeft the GUI left offset
     * @param guiTop the GUI top offset
     */
    private void drawFallbackText(@Nonnull GuiGraphics graphics, int guiLeft, int guiTop) {
        int centerX = guiLeft + PREVIEW_CENTER_X;
        int centerY = guiTop + PREVIEW_CENTER_Y;

        Component previewText = Component.literal("Switch");
        int textWidth = this.font.width(previewText);
        int textX = centerX - textWidth / 2;
        int textY = centerY - 4;
        graphics.drawString(this.font, previewText, textX, textY, 0x404040, false);
    }

    /**
     * Draws dropdown buttons for face selection.
     * 
     * @param graphics the GUI graphics context
     * @param guiLeft the GUI left offset
     * @param guiTop the GUI top offset
     */
    private void drawCleanArchitectureDropdowns(@Nonnull GuiGraphics graphics, int guiLeft, int guiTop) {
        // Left (toggle) variable dropdown
        drawCleanDropdownButton(graphics, guiLeft + LEFT_FACE_X, guiTop + LEFT_FACE_Y,
                leftTextureSelection, showingLeftDropdown);

        // Right (base) variable dropdown
        drawCleanDropdownButton(graphics, guiLeft + RIGHT_FACE_X, guiTop + RIGHT_FACE_Y,
                rightTextureSelection, showingRightDropdown);
    }

    /**
     * Draws a dropdown button with proper state visualization.
     * 
     * @param graphics the GUI graphics context
     * @param x the button X position
     * @param y the button Y position
     * @param selection the texture selection state
     * @param isOpen whether the dropdown is currently open
     */
    private void drawCleanDropdownButton(@Nonnull GuiGraphics graphics, int x, int y,
                                         @Nonnull FaceSelectionData.RawTextureSelection selection, boolean isOpen) {
        // Determine colors based on state
        int bgColor = selection.enabled() ? 0xFFC6C6C6 : 0xFF888888;
        int textColor = selection.enabled() ? 0xFF404040 : 0xFF666666;

        // Draw dropdown background
        graphics.fill(x, y, x + FACE_DROPDOWN_WIDTH, y + FACE_DROPDOWN_HEIGHT, bgColor);

        // Draw dropdown border
        if (selection.enabled()) {
            int lightColor = isOpen ? 0xFF555555 : 0xFFFFFFFF;
            int darkColor = isOpen ? 0xFFFFFFFF : 0xFF555555;

            graphics.fill(x, y, x + FACE_DROPDOWN_WIDTH, y + 1, lightColor);
            graphics.fill(x, y, x + 1, y + FACE_DROPDOWN_HEIGHT, lightColor);
            graphics.fill(x, y + FACE_DROPDOWN_HEIGHT - 1, x + FACE_DROPDOWN_WIDTH, y + FACE_DROPDOWN_HEIGHT, darkColor);
            graphics.fill(x + FACE_DROPDOWN_WIDTH - 1, y, x + FACE_DROPDOWN_WIDTH, y + FACE_DROPDOWN_HEIGHT, darkColor);
        } else {
            // Disabled border
            graphics.fill(x, y, x + FACE_DROPDOWN_WIDTH, y + 1, 0xFF666666);
            graphics.fill(x, y, x + 1, y + FACE_DROPDOWN_HEIGHT, 0xFF666666);
            graphics.fill(x, y + FACE_DROPDOWN_HEIGHT - 1, x + FACE_DROPDOWN_WIDTH, y + FACE_DROPDOWN_HEIGHT, 0xFF999999);
            graphics.fill(x + FACE_DROPDOWN_WIDTH - 1, y, x + FACE_DROPDOWN_WIDTH, y + FACE_DROPDOWN_HEIGHT, 0xFF999999);
        }

        // Draw dropdown arrow
        if (selection.enabled()) {
            int arrowColor = 0xFF000000;
            int arrowX = x + FACE_DROPDOWN_WIDTH - 10;
            int arrowY = y + 4;

            if (isOpen) {
                // Up arrow (open state) - indicates "click to collapse"
                graphics.fill(arrowX + 2, arrowY, arrowX + 4, arrowY + 1, arrowColor);      // Top: narrow
                graphics.fill(arrowX + 1, arrowY + 1, arrowX + 5, arrowY + 2, arrowColor);  // Middle
                graphics.fill(arrowX, arrowY + 2, arrowX + 6, arrowY + 3, arrowColor);      // Bottom: wide
            } else {
                // Down arrow (closed state) - indicates "click to expand"
                graphics.fill(arrowX, arrowY, arrowX + 6, arrowY + 1, arrowColor);          // Top: wide
                graphics.fill(arrowX + 1, arrowY + 1, arrowX + 5, arrowY + 2, arrowColor);  // Middle
                graphics.fill(arrowX + 2, arrowY + 2, arrowX + 4, arrowY + 3, arrowColor);  // Bottom: narrow
            }
        }

        // Draw current selection or blank text
        String displayText;
        if (!selection.enabled()) {
            // Grayed out with completely empty text when inactive
            displayText = "";
        } else {
            // Show current selection (raw JSON variable name)
            displayText = selection.selectedVariable();

            // Truncate text if too long for dropdown width
            if (displayText.length() > 5) {
                displayText = displayText.substring(0, 5);
            }
        }

        // Only draw text if not empty
        if (!displayText.isEmpty()) {
            graphics.drawString(this.font, displayText, x + 2, y + 2, textColor, false);
        }
    }

    /**
     * Draws 2D texture previews with comprehensive null safety.
     * <p>
     * Enhanced version with proper null checking to prevent argument
     * warnings and ensure stable preview rendering.
     * 
     * @param graphics the GUI graphics context
     * @param guiLeft the GUI left offset
     * @param guiTop the GUI top offset
     */
    private void drawSafeTexturePreview(@Nonnull GuiGraphics graphics, int guiLeft, int guiTop) {
        // Draw left (toggle) texture preview with null safety
        if (leftTextureSelection.hasPreview()) {
            String leftPreviewTexture = leftTextureSelection.previewTexture();
            if (leftPreviewTexture != null && !leftPreviewTexture.isEmpty()) {
                drawTexturePreviewBox(graphics, guiLeft + LEFT_PREVIEW_X, guiTop + LEFT_PREVIEW_Y, leftPreviewTexture);
            }
        }

        // Draw right (base) texture preview with null safety
        if (rightTextureSelection.hasPreview()) {
            String rightPreviewTexture = rightTextureSelection.previewTexture();
            if (rightPreviewTexture != null && !rightPreviewTexture.isEmpty()) {
                drawTexturePreviewBox(graphics, guiLeft + RIGHT_PREVIEW_X, guiTop + RIGHT_PREVIEW_Y, rightPreviewTexture);
            }
        }
    }

    /**
     * Draws an individual 2D texture preview box with proper resource management.
     * <p>
     * Enhanced with proper resource handling to prevent SpriteContents
     * resource leaks and ensure safe texture access.
     * 
     * @param graphics the GUI graphics context
     * @param x the preview box X position
     * @param y the preview box Y position
     * @param texturePath the texture path to render (guaranteed non-null)
     */
    private void drawTexturePreviewBox(@Nonnull GuiGraphics graphics, int x, int y, @Nonnull String texturePath) {
        try {
            // Get texture sprite with proper resource management
            TextureAtlasSprite sprite = getTextureSprite(texturePath);

            if (sprite != null) {
                // Safe sprite contents access with proper resource handling
                String spriteName = getSafeSpriteName(sprite);
                
                if (!spriteName.contains("missingno")) {
                    // Draw preview background
                    graphics.fill(x - 1, y - 1, x + PREVIEW_SIZE + 1, y + PREVIEW_SIZE + 1, 0xFF000000);
                    graphics.fill(x, y, x + PREVIEW_SIZE, y + PREVIEW_SIZE, 0xFFFFFFFF);

                    // Draw texture sprite
                    graphics.blit(x, y, 0, PREVIEW_SIZE, PREVIEW_SIZE, sprite);
                } else {
                    // Draw missing texture indicator
                    graphics.fill(x, y, x + PREVIEW_SIZE, y + PREVIEW_SIZE, 0xFFFF00FF);
                }
            } else {
                // Draw missing texture indicator
                graphics.fill(x, y, x + PREVIEW_SIZE, y + PREVIEW_SIZE, 0xFFFF00FF);
            }
        } catch (Exception e) {
            // Draw error indicator
            graphics.fill(x, y, x + PREVIEW_SIZE, y + PREVIEW_SIZE, 0xFFFF0000);
        }
    }

    /**
     * Safely retrieves sprite name with proper resource management.
     * <p>
     * Eliminates SpriteContents resource warnings by ensuring proper
     * access patterns and exception handling.
     * 
     * @param sprite the texture atlas sprite
     * @return the sprite name, or "missingno" if access fails
     */
    @Nonnull
    private String getSafeSpriteName(@Nonnull TextureAtlasSprite sprite) {
        try {
            // Safe access to sprite contents with proper resource management
            try (var contents = sprite.contents()) {
                return contents.name().toString();
            }
        } catch (Exception e) {
            // Return safe fallback on any resource access error
            return "missingno";
        }
    }

    /**
     * Gets texture sprite for 2D preview rendering with proper resource management.
     * <p>
     * Enhanced with comprehensive fallback patterns and proper exception
     * handling to ensure stable texture loading.
     * 
     * @param texturePath the texture path to load
     * @return the texture sprite, or null if unavailable
     */
    @Nullable
    private TextureAtlasSprite getTextureSprite(@Nonnull String texturePath) {
        try {
            ResourceLocation textureLocation = new ResourceLocation(texturePath);
            TextureAtlasSprite sprite = Minecraft.getInstance()
                    .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                    .apply(textureLocation);

            if (sprite != null) {
                String spriteName = getSafeSpriteName(sprite);
                if (!spriteName.contains("missingno")) {
                    return sprite;
                }
            }

            // Try fallback patterns for face-specific textures
            if (texturePath.contains("_top") || texturePath.contains("_side") || texturePath.contains("_front")) {
                String basePath = texturePath.replaceAll("_(top|side|front)$", "");
                ResourceLocation fallbackLocation = new ResourceLocation(basePath);
                TextureAtlasSprite fallbackSprite = Minecraft.getInstance()
                        .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                        .apply(fallbackLocation);

                if (fallbackSprite != null) {
                    String fallbackSpriteName = getSafeSpriteName(fallbackSprite);
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

    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        // Render dropdown popups for proper z-order
        int guiLeft = (this.width - this.imageWidth) / 2;
        int guiTop = (this.height - this.imageHeight) / 2;

        if (showingLeftDropdown) {
            drawCleanDropdownPopup(graphics, guiLeft + LEFT_FACE_X, guiTop + LEFT_FACE_Y + FACE_DROPDOWN_HEIGHT, leftTextureSelection);
        }

        if (showingRightDropdown) {
            drawCleanDropdownPopup(graphics, guiLeft + RIGHT_FACE_X, guiTop + RIGHT_FACE_Y + FACE_DROPDOWN_HEIGHT, rightTextureSelection);
        }

        if (showingPowerDropdown) {
            drawPowerDropdownPopup(graphics, guiLeft + POWER_DROPDOWN_X, guiTop + POWER_DROPDOWN_Y + POWER_DROPDOWN_HEIGHT);
        }
    }

    /**
     * Draws dropdown popup menu with elevated z-order.
     * 
     * @param graphics the GUI graphics context
     * @param x the popup X position
     * @param y the popup Y position
     * @param selection the texture selection state
     */
    private void drawCleanDropdownPopup(@Nonnull GuiGraphics graphics, int x, int y,
                                        @Nonnull FaceSelectionData.RawTextureSelection selection) {
        List<String> variables = selection.availableVariables();
        int popupHeight = variables.size() * 12;

        // Elevate z-order to render above inventory items
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 400);

        // Draw popup background
        graphics.fill(x, y, x + FACE_DROPDOWN_WIDTH, y + popupHeight, 0xFFC6C6C6);

        // Draw popup border - ensure all borders are properly rendered regardless of option count
        // Top border (1 pixel high)
        graphics.fill(x, y, x + FACE_DROPDOWN_WIDTH, y + 1, 0xFF000000);
        // Left border (full height)
        graphics.fill(x, y, x + 1, y + popupHeight, 0xFF000000);
        // Right border (full height) 
        graphics.fill(x + FACE_DROPDOWN_WIDTH - 1, y, x + FACE_DROPDOWN_WIDTH, y + popupHeight, 0xFF000000);
        // Bottom border (1 pixel high) - draw outside the main area to ensure visibility
        graphics.fill(x, y + popupHeight, x + FACE_DROPDOWN_WIDTH, y + popupHeight + 1, 0xFF000000);

        // Draw raw variable options
        for (int i = 0; i < variables.size(); i++) {
            String variable = variables.get(i);
            int optionY = y + (i * 12);

            // Highlight selected variable
            if (variable.equals(selection.selectedVariable())) {
                graphics.fill(x + 1, optionY, x + FACE_DROPDOWN_WIDTH - 1, optionY + 12, 0xFF8888FF);
            }

            // Draw variable name (raw JSON variable, no modification)
            String displayText = variable;
            if (displayText.length() > 5) {
                displayText = displayText.substring(0, 5);
            }
            graphics.drawString(this.font, displayText, x + 2, optionY + 2, 0xFF000000, false);
        }

        // Restore z-order
        graphics.pose().popPose();
    }

    // ========================================
    // POWER CATEGORY GUI ELEMENTS
    // ========================================

    /**
     * Draws power category dropdown and texture previews.
     */
    private void drawPowerCategoryElements(@Nonnull GuiGraphics graphics, int guiLeft, int guiTop) {
        // Draw power mode dropdown
        drawPowerDropdownButton(graphics, guiLeft + POWER_DROPDOWN_X, guiTop + POWER_DROPDOWN_Y);
        
        // Draw power texture previews
        drawPowerTexturePreview(graphics, guiLeft, guiTop);
        
        // Draw power labels
        drawPowerLabels(graphics, guiLeft, guiTop);
    }

    /**
     * Draws the power mode dropdown button.
     */
    private void drawPowerDropdownButton(@Nonnull GuiGraphics graphics, int x, int y) {
        SwitchesLeverBlockEntity.PowerMode currentMode = menu.getPowerMode();
        
        // Draw dropdown background
        graphics.fill(x, y, x + POWER_DROPDOWN_WIDTH, y + POWER_DROPDOWN_HEIGHT, 0xFFC6C6C6);
        
        // Draw dropdown border
        int lightColor = showingPowerDropdown ? 0xFF555555 : 0xFFFFFFFF;
        int darkColor = showingPowerDropdown ? 0xFFFFFFFF : 0xFF555555;
        
        graphics.fill(x, y, x + POWER_DROPDOWN_WIDTH, y + 1, lightColor);
        graphics.fill(x, y, x + 1, y + POWER_DROPDOWN_HEIGHT, lightColor);
        graphics.fill(x, y + POWER_DROPDOWN_HEIGHT - 1, x + POWER_DROPDOWN_WIDTH, y + POWER_DROPDOWN_HEIGHT, darkColor);
        graphics.fill(x + POWER_DROPDOWN_WIDTH - 1, y, x + POWER_DROPDOWN_WIDTH, y + POWER_DROPDOWN_HEIGHT, darkColor);
        
        // Draw dropdown arrow
        int arrowColor = 0xFF000000;
        int arrowX = x + POWER_DROPDOWN_WIDTH - 10;
        int arrowY = y + 4;
        
        if (showingPowerDropdown) {
            // Up arrow (open state) - wide at top, narrow at bottom
            graphics.fill(arrowX, arrowY, arrowX + 6, arrowY + 1, arrowColor);
            graphics.fill(arrowX + 1, arrowY + 1, arrowX + 5, arrowY + 2, arrowColor);
            graphics.fill(arrowX + 2, arrowY + 2, arrowX + 4, arrowY + 3, arrowColor);
        } else {
            // Down arrow (closed state) - narrow at top, wide at bottom
            graphics.fill(arrowX + 2, arrowY, arrowX + 4, arrowY + 1, arrowColor);
            graphics.fill(arrowX + 1, arrowY + 1, arrowX + 5, arrowY + 2, arrowColor);
            graphics.fill(arrowX, arrowY + 2, arrowX + 6, arrowY + 3, arrowColor);
        }
        
        // Draw current power mode text (lowercase as specified)
        String displayText = formatPowerModeText(currentMode.name());
        graphics.drawString(this.font, displayText, x + 2, y + 2, 0xFF404040, false);
    }

    /**
     * Draws power dropdown popup menu.
     */
    private void drawPowerDropdownPopup(@Nonnull GuiGraphics graphics, int x, int y) {
        SwitchesLeverBlockEntity.PowerMode[] modes = SwitchesLeverBlockEntity.PowerMode.values();
        SwitchesLeverBlockEntity.PowerMode currentMode = menu.getPowerMode();
        int popupHeight = modes.length * 12;
        
        // Elevate z-order to render above other elements
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 400);
        
        // Draw popup background
        graphics.fill(x, y, x + POWER_DROPDOWN_WIDTH, y + popupHeight, 0xFFC6C6C6);
        
        // Draw popup border - ensure all borders are properly rendered regardless of option count
        // Top border (1 pixel high)
        graphics.fill(x, y, x + POWER_DROPDOWN_WIDTH, y + 1, 0xFF000000);
        // Left border (full height)
        graphics.fill(x, y, x + 1, y + popupHeight, 0xFF000000);
        // Right border (full height)
        graphics.fill(x + POWER_DROPDOWN_WIDTH - 1, y, x + POWER_DROPDOWN_WIDTH, y + popupHeight, 0xFF000000);
        // Bottom border (1 pixel high) - draw outside the main area to ensure visibility
        graphics.fill(x, y + popupHeight, x + POWER_DROPDOWN_WIDTH, y + popupHeight + 1, 0xFF000000);
        
        // Draw power mode options
        for (int i = 0; i < modes.length; i++) {
            SwitchesLeverBlockEntity.PowerMode mode = modes[i];
            int optionY = y + (i * 12);
            
            // Highlight selected mode
            if (mode == currentMode) {
                graphics.fill(x + 1, optionY, x + POWER_DROPDOWN_WIDTH - 1, optionY + 12, 0xFF8888FF);
            }
            
            // Draw mode name (lowercase as specified)
            String modeText = formatPowerModeText(mode.name());
            graphics.drawString(this.font, modeText, x + 2, optionY + 2, 0xFF000000, false);
        }
        
        // Restore z-order
        graphics.pose().popPose();
    }

    /**
     * Draws power texture previews (6x6 boxes).
     */
    private void drawPowerTexturePreview(@Nonnull GuiGraphics graphics, int guiLeft, int guiTop) {
        String unpoweredTexture = menu.getUnpoweredTexturePreview();
        String poweredTexture = menu.getPoweredTexturePreview();
        
        // Draw unpowered texture preview
        if (!unpoweredTexture.isEmpty()) {
            drawSmallTexturePreviewBox(graphics, guiLeft + UNPOWERED_PREVIEW_X, guiTop + UNPOWERED_PREVIEW_Y, unpoweredTexture);
        } else {
            // Draw empty preview box
            drawEmptyPreviewBox(graphics, guiLeft + UNPOWERED_PREVIEW_X, guiTop + UNPOWERED_PREVIEW_Y);
        }
        
        // Draw powered texture preview
        if (!poweredTexture.isEmpty()) {
            drawSmallTexturePreviewBox(graphics, guiLeft + POWERED_PREVIEW_X, guiTop + POWERED_PREVIEW_Y, poweredTexture);
        } else {
            // Draw empty preview box
            drawEmptyPreviewBox(graphics, guiLeft + POWERED_PREVIEW_X, guiTop + POWERED_PREVIEW_Y);
        }
    }

    /**
     * Draws a small 6x6 texture preview box showing the specific 2x2 UV area that will be rendered on the model.
     * This provides an exact preview of what the user will see on the switch, rather than a scaled-down full texture.
     */
    private void drawSmallTexturePreviewBox(@Nonnull GuiGraphics graphics, int x, int y, @Nonnull String texturePath) {
        try {
            TextureAtlasSprite sprite = getTextureSprite(texturePath);
            if (sprite != null) {
                String spriteName = getSafeSpriteName(sprite);
                if (!spriteName.contains("missingno")) {
                    // Draw preview background (black border, white fill)
                    graphics.fill(x - 1, y - 1, x + POWER_PREVIEW_SIZE + 1, y + POWER_PREVIEW_SIZE + 1, 0xFF000000);
                    graphics.fill(x, y, x + POWER_PREVIEW_SIZE, y + POWER_PREVIEW_SIZE, 0xFFFFFFFF);
                    
                    // Draw the specific 2x2 UV area scaled to fill the 6x6 preview
                    drawUVSpecificPreview(graphics, x, y, sprite);
                } else {
                    drawEmptyPreviewBox(graphics, x, y);
                }
            } else {
                drawEmptyPreviewBox(graphics, x, y);
            }
        } catch (Exception e) {
            drawEmptyPreviewBox(graphics, x, y);
        }
    }
    
    /**
     * Draws UV-specific power texture preview using scissor clipping approach.
     * ---
     * Updated for simplified UV coordinates where both powered and unpowered states
     * use the same location on the texture:
     * - 2 pixels from the left edge of the texture
     * - 6 pixels from the top edge of the texture
     * ---
     * Mathematical mapping with 3x scaling (16x16 → 48x48):
     * - Texture UV (2,6) with 2x2 area → Texture pixels 2-3 (width), 6-7 (height)
     * - GUI pixels: 6-11 (width), 18-23 (height) = 6x6 preview area
     * ---
     * @param graphics the GUI graphics context
     * @param x the preview box X position  
     * @param y the preview box Y position
     * @param sprite the texture sprite to display
     */
    private void drawUVSpecificPreview(@Nonnull GuiGraphics graphics, int x, int y, @Nonnull TextureAtlasSprite sprite) {
        try {
            // Draw preview background
            graphics.fill(x - 1, y - 1, x + POWER_PREVIEW_SIZE + 1, y + POWER_PREVIEW_SIZE + 1, 0xFF000000);
            graphics.fill(x, y, x + POWER_PREVIEW_SIZE, y + POWER_PREVIEW_SIZE, 0xFFFFFFFF);

            // Simplified UV coordinate mapping - both powered and unpowered use same location:
            // UV (2,6) with 2x2 area → GUI pixels 6-11 (width), 18-23 (height)
            
            int offsetX = 6;   // UV X coordinate 2 * 3 (scale factor) = 6
            int offsetY = 18;  // UV Y coordinate 6 * 3 (scale factor) = 18
            
            // Calculate texture position so target UV area appears in preview location
            int textureX = x - offsetX;
            int textureY = y - offsetY;
            
            // 1. Enable scissor clipping to 6x6 preview area (crop to show only target UV)
            graphics.enableScissor(x, y, x + POWER_PREVIEW_SIZE, y + POWER_PREVIEW_SIZE);
            
            // 2. Render 48x48 texture positioned so target UV area appears in preview
            //    (48x48 = 3x scale factor from 16x16 original)
            graphics.blit(textureX, textureY, 0, 48, 48, sprite);
            
            // 3. Disable scissor clipping
            graphics.disableScissor();
            
        } catch (Exception e) {
            // Fallback to a solid color if rendering fails
            graphics.fill(x, y, x + POWER_PREVIEW_SIZE, y + POWER_PREVIEW_SIZE, 0xFFCCCCCC);
        }
    }

    /**
     * Draws an empty preview box.
     */
    private void drawEmptyPreviewBox(@Nonnull GuiGraphics graphics, int x, int y) {
        // Draw black border, white fill
        graphics.fill(x - 1, y - 1, x + POWER_PREVIEW_SIZE + 1, y + POWER_PREVIEW_SIZE + 1, 0xFF000000);
        graphics.fill(x, y, x + POWER_PREVIEW_SIZE, y + POWER_PREVIEW_SIZE, 0xFFFFFFFF);
    }

    /**
     * Draws power category labels.
     */
    private void drawPowerLabels(@Nonnull GuiGraphics graphics, int guiLeft, int guiTop) {
        // Draw "Unpowered" label
        graphics.drawString(this.font, "Unpowered", guiLeft + UNPOWERED_LABEL_X, guiTop + UNPOWERED_LABEL_Y, 0xFF404040, false);
        
        // Draw "Powered" label
        graphics.drawString(this.font, "Powered", guiLeft + POWERED_LABEL_X, guiTop + POWERED_LABEL_Y, 0xFF404040, false);
    }

    /**
     * Formats power mode text for display (lowercase with capital first letter).
     */
    private String formatPowerModeText(String modeText) {
        if (modeText == null || modeText.isEmpty()) {
            return "default";
        }
        
        String lowercase = modeText.toLowerCase();
        return Character.toUpperCase(lowercase.charAt(0)) + lowercase.substring(1);
    }

    @Override
    protected void renderLabels(@Nonnull GuiGraphics graphics, int mouseX, int mouseY) {
        // Draw title
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);

        // Draw player inventory label
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }
}