package net.justsomeswitches.gui;

import net.justsomeswitches.blockentity.SwitchesLeverBlockEntity;
import net.justsomeswitches.util.TextureRotation;
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
 * Switch texture screen for customization GUI.
 */
public class SwitchesTextureScreen extends AbstractContainerScreen<SwitchesTextureMenu> {

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 176;


    private static final ResourceLocation GUI_BACKGROUND = new ResourceLocation("justsomeswitches", "textures/gui/switch_texture_gui.png");


    // 3D Preview - no change
    private static final int PREVIEW_CENTER_X = 81;
    private static final int PREVIEW_CENTER_Y = 39;  // was 40, y -1 (NO CHANGE)
    private static final float PREVIEW_SCALE = 2.0f;

    // Face selection dropdowns - additional adjustments
    private static final int LEFT_FACE_X = 11;
    private static final int LEFT_FACE_Y = 50;        // was 45, y +5, then -1, then +1
    private static final int RIGHT_FACE_X = 119;
    private static final int RIGHT_FACE_Y = 50;       // was 45, y +5, then -1, then +1
    private static final int FACE_DROPDOWN_WIDTH = 46;
    private static final int FACE_DROPDOWN_HEIGHT = 12;

    // Rotation dropdowns - additional adjustments
    private static final int LEFT_ROTATION_X = 26;
    private static final int LEFT_ROTATION_Y = 65;    // was 59, y +5, then -1, then +2
    private static final int RIGHT_ROTATION_X = 119;
    private static final int RIGHT_ROTATION_Y = 65;   // was 59, y +5, then -1, then +2
    private static final int ROTATION_DROPDOWN_WIDTH = 31;
    private static final int ROTATION_DROPDOWN_HEIGHT = 12;

    // Texture previews - additional adjustments
    private static final int LEFT_PREVIEW_X = 36;     // was 34, x +2
    private static final int LEFT_PREVIEW_Y = 28;     // was 21, y +6, then +1
    private static final int RIGHT_PREVIEW_X = 122;   // was 120, x +2
    private static final int RIGHT_PREVIEW_Y = 28;    // was 21, y +6, then +1
    private static final int PREVIEW_SIZE = 18;

    // Power dropdown - additional adjustments
    private static final int POWER_DROPDOWN_X = 65;
    private static final int POWER_DROPDOWN_Y = 50;   // was 45, y +5, then -1, then +1
    private static final int POWER_DROPDOWN_WIDTH = 46;
    private static final int POWER_DROPDOWN_HEIGHT = 12;

    // Power previews - additional adjustments
    private static final int UNPOWERED_PREVIEW_X = 64; // was 63, x +1
    private static final int UNPOWERED_PREVIEW_Y = 64; // was 58, y +6, then +1, then -2, then +1
    private static final int POWERED_PREVIEW_X = 69;   // was 68, x +1
    private static final int POWERED_PREVIEW_Y = 73;   // was 67, y +6, then +1, then -2, then +1
    private static final int POWER_PREVIEW_SIZE = 6;

    // Power labels - final adjustments
    private static final int UNPOWERED_LABEL_X = 73;   // reverted back to 73
    private static final int UNPOWERED_LABEL_Y = 64;   // was 65, y -1  
    private static final int POWERED_LABEL_X = 78;     // no change
    private static final int POWERED_LABEL_Y = 73;     // was 74, y -1




    private FaceSelectionData.RawTextureSelection leftTextureSelection = FaceSelectionData.RawTextureSelection.createDisabled();
    private FaceSelectionData.RawTextureSelection rightTextureSelection = FaceSelectionData.RawTextureSelection.createDisabled();


    private boolean showingLeftDropdown = false;
    private boolean showingRightDropdown = false;
    private boolean showingPowerDropdown = false;
    
    // NEW: Rotation dropdown states
    private boolean showingLeftRotationDropdown = false;
    private boolean showingRightRotationDropdown = false;


    private ItemStack previousLeftItem = ItemStack.EMPTY;
    private ItemStack previousRightItem = ItemStack.EMPTY;

    private String previousBaseTexture = null;
    private String previousToggleTexture = null;
    private SwitchesLeverBlockEntity.PowerMode previousPowerMode = null;


    public SwitchesTextureScreen(@Nonnull SwitchesTextureMenu menu, @Nonnull Inventory playerInventory, @Nonnull Component title) {
        super(menu, playerInventory, title);

        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;

        this.titleLabelX = 8;
        this.titleLabelY = 10;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 86;
    }
    
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
     * Transform vertex data to use new texture coordinates.
     */
    @Nonnull
    private int[] transformVertexDataWithShading(@Nonnull int[] originalVertices,
                                                  @Nonnull TextureAtlasSprite originalTexture,
                                                  @Nonnull TextureAtlasSprite newTexture,
                                                  float brightnessMultiplier) {
        
        int[] newVertices = originalVertices.clone();
        
        for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
            int baseIndex = vertexIndex * 8;

            float originalU = Float.intBitsToFloat(originalVertices[baseIndex + 4]);
            float originalV = Float.intBitsToFloat(originalVertices[baseIndex + 5]);
            
            float newU = transformU(originalU, originalTexture, newTexture);
            float newV = transformV(originalV, originalTexture, newTexture);
            
            newVertices[baseIndex + 4] = Float.floatToIntBits(newU);
            newVertices[baseIndex + 5] = Float.floatToIntBits(newV);

            int originalColor = originalVertices[baseIndex + 3];
            int shadedColor = applyBrightnessShading(originalColor, brightnessMultiplier);
            newVertices[baseIndex + 3] = shadedColor;
        }
        
        return newVertices;
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
     * Transform U coordinate from original to new texture space.
     */
    private float transformU(float originalU, @Nonnull TextureAtlasSprite originalTexture,
                             @Nonnull TextureAtlasSprite newTexture) {
        float relativeU = (originalU - originalTexture.getU0()) / (originalTexture.getU1() - originalTexture.getU0());
        return newTexture.getU0() + relativeU * (newTexture.getU1() - newTexture.getU0());
    }
    
    /**
     * Transform V coordinate from original to new texture space.
     */
    private float transformV(float originalV, @Nonnull TextureAtlasSprite originalTexture,
                             @Nonnull TextureAtlasSprite newTexture) {
        float relativeV = (originalV - originalTexture.getV0()) / (originalTexture.getV1() - originalTexture.getV0());
        return newTexture.getV0() + relativeV * (newTexture.getV1() - newTexture.getV0());
    }

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
    
    // Note: determineReplacementTexture method removed - texture processing now handled by Dynamic Model
    
    // Note: texture identification methods removed - texture processing now handled by Dynamic Model
    
    // Note: renderQuadWithCustomTextureAndShading method removed - texture processing now handled by Dynamic Model
    
    // Note: custom texture sprite methods removed - texture processing now handled by Dynamic Model
    
    // Note: processCustomQuads method removed - texture processing now handled by Dynamic Model
    
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
                                               @Nonnull MultiBufferSource bufferSource) {
        
        BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
        BakedModel baseModel = blockRenderer.getBlockModel(blockState);
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.translucent());

        // Create comprehensive ModelData with all current GUI state including rotation
        net.neoforged.neoforge.client.model.data.ModelData.Builder modelDataBuilder = 
            net.neoforged.neoforge.client.model.data.ModelData.builder();
        
        // Add texture rotation states
        modelDataBuilder.with(net.justsomeswitches.blockentity.SwitchesLeverBlockEntity.BASE_ROTATION, 
                             menu.getBaseTextureRotation().name());
        modelDataBuilder.with(net.justsomeswitches.blockentity.SwitchesLeverBlockEntity.TOGGLE_ROTATION, 
                             menu.getToggleTextureRotation().name());
        
        // Add current texture paths from GUI selections
        FaceSelectionData.RawTextureSelection baseSelection = rightTextureSelection;
        FaceSelectionData.RawTextureSelection toggleSelection = leftTextureSelection;
        
        if (baseSelection.hasPreview() && baseSelection.previewTexture() != null) {
            modelDataBuilder.with(net.justsomeswitches.blockentity.SwitchesLeverBlockEntity.BASE_TEXTURE, 
                                 baseSelection.previewTexture());
        }
        
        if (toggleSelection.hasPreview() && toggleSelection.previewTexture() != null) {
            modelDataBuilder.with(net.justsomeswitches.blockentity.SwitchesLeverBlockEntity.TOGGLE_TEXTURE, 
                                 toggleSelection.previewTexture());
        }
        
        // Add power mode state
        modelDataBuilder.with(net.justsomeswitches.blockentity.SwitchesLeverBlockEntity.POWER_MODE, 
                             menu.getPowerMode().name());
        
        // Add wall orientation (center for GUI preview)
        modelDataBuilder.with(net.justsomeswitches.blockentity.SwitchesLeverBlockEntity.WALL_ORIENTATION, "center");
        
        // Add face selection state
        String faceSelection = baseSelection.selectedVariable() + "," + toggleSelection.selectedVariable();
        modelDataBuilder.with(net.justsomeswitches.blockentity.SwitchesLeverBlockEntity.FACE_SELECTION, faceSelection);
        
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
     * Renders quads directly from the Dynamic Model without additional texture processing.
     * The Dynamic Model has already applied all texture replacements and rotations.
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

    @Override
    protected void init() {
        super.init();

        menu.completeInitialization();
        
        updateUIState();
    }

    @Override
    public void containerTick() {
        super.containerTick();
        updateUIState();
    }

    /**
     * Updates UI state with change detection.
     */
    private void updateUIState() {

        FaceSelectionData.RawTextureSelection newLeftSelection = menu.getToggleTextureSelection();
        FaceSelectionData.RawTextureSelection newRightSelection = menu.getBaseTextureSelection();


        ItemStack currentLeftItem = newLeftSelection.sourceBlock();
        ItemStack currentRightItem = newRightSelection.sourceBlock();


        if (!ItemStack.matches(previousLeftItem, currentLeftItem)) {
            if (currentLeftItem.isEmpty()) {
                handleBlockRemoval(true);
            }
            previousLeftItem = currentLeftItem.copy();
        }


        if (!ItemStack.matches(previousRightItem, currentRightItem)) {
            if (currentRightItem.isEmpty()) {
                handleBlockRemoval(false);
            }
            previousRightItem = currentRightItem.copy();
        }


        leftTextureSelection = newLeftSelection;
        rightTextureSelection = newRightSelection;

        @SuppressWarnings("unused")
        boolean previewNeedsUpdate = false;

        if (!Objects.equals(previousBaseTexture, rightTextureSelection.previewTexture())) {
            previousBaseTexture = rightTextureSelection.previewTexture();
        }

        if (!Objects.equals(previousToggleTexture, leftTextureSelection.previewTexture())) {
            previousToggleTexture = leftTextureSelection.previewTexture();
        }

        if (previousPowerMode != menu.getPowerMode()) {
            previousPowerMode = menu.getPowerMode();
        }

    }

    /**
     * Renders the live 3D preview in the GUI center using the Dynamic Model.
     * This ensures texture rotation is properly applied by leveraging the existing model system.
     */
    private void drawLive3DPreview(@Nonnull GuiGraphics graphics, int guiLeft, int guiTop) {
        try {
            BlockState switchState = getCurrentBlockState();

            int centerX = guiLeft + PREVIEW_CENTER_X;
            int centerY = guiTop + PREVIEW_CENTER_Y;

            PoseStack poseStack = graphics.pose();
            poseStack.pushPose();

            poseStack.translate(centerX, centerY, 100);
            poseStack.scale(PREVIEW_SCALE * 16, -PREVIEW_SCALE * 16, PREVIEW_SCALE * 16);

            poseStack.mulPose(new org.joml.Quaternionf().fromAxisAngleDeg(1, 0, 0, 10f));
            poseStack.mulPose(new org.joml.Quaternionf().fromAxisAngleDeg(0, 1, 0, -215f));

            poseStack.translate(-0.5f, -0.5f, -0.5f);

            MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();

            // Use the Dynamic Model system which handles texture rotation correctly
            renderBlockWithCustomTextures(switchState, poseStack, bufferSource);

            bufferSource.endBatch();
            
            poseStack.popPose();
            
        } catch (Exception e) {
            // Fallback to basic 3D preview without custom textures
            drawBasic3DPreview(graphics, guiLeft, guiTop);
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
        showingLeftRotationDropdown = false;
        showingRightRotationDropdown = false;

        // Reset selection to defaults (handled by menu auto-apply)
        if (isLeft) {
            menu.setToggleTextureVariable("all");
            menu.setToggleTextureRotation(TextureRotation.NORMAL); // Reset rotation to 0°
        } else {
            menu.setBaseTextureVariable("all");
            menu.setBaseTextureRotation(TextureRotation.NORMAL); // Reset rotation to 0°
        }
    }

    /**
     * Handles mouse click events for dropdown interactions.
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int guiLeft = (this.width - this.imageWidth) / 2;
        int guiTop = (this.height - this.imageHeight) / 2;

        // Handle dropdown selection clicks FIRST to prioritize popup interactions
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

        // Handle base rotation dropdown selection clicks
        if (showingRightRotationDropdown && handleBaseRotationDropdownSelection(mouseX, mouseY, guiLeft, guiTop)) {
            return true;
        }

        // Handle toggle rotation dropdown selection clicks
        if (showingLeftRotationDropdown && handleToggleRotationDropdownSelection(mouseX, mouseY, guiLeft, guiTop)) {
            return true;
        }



        // Handle dropdown button clicks AFTER popup selections to avoid conflicts
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

        // Handle toggle rotation dropdown click (only when any block in toggle texture slot)
        if (isWithinRotationDropdownBounds(mouseX, mouseY, guiLeft + LEFT_ROTATION_X, guiTop + LEFT_ROTATION_Y)) {
            if (hasToggleTextureBlock()) {
                toggleToggleRotationDropdown();
                return true;
            }
        }

        // Handle base rotation dropdown click (only when any block in base texture slot)
        if (isWithinRotationDropdownBounds(mouseX, mouseY, guiLeft + RIGHT_ROTATION_X, guiTop + RIGHT_ROTATION_Y)) {
            if (hasBaseTextureBlock()) {
                toggleBaseRotationDropdown();
                return true;
            }
        }



        // Close dropdowns if clicking elsewhere
        if (showingLeftDropdown || showingRightDropdown || showingPowerDropdown || 
            showingLeftRotationDropdown || showingRightRotationDropdown) {
            showingLeftDropdown = false;
            showingRightDropdown = false;
            showingPowerDropdown = false;
            showingLeftRotationDropdown = false;
            showingRightRotationDropdown = false;
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * Bounds checking for dropdown areas.
     */
    private boolean isWithinDropdownBounds(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + FACE_DROPDOWN_WIDTH && mouseY >= y && mouseY < y + FACE_DROPDOWN_HEIGHT;
    }

    private boolean isWithinPowerDropdownBounds(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + POWER_DROPDOWN_WIDTH && mouseY >= y && mouseY < y + POWER_DROPDOWN_HEIGHT;
    }

    private boolean isWithinRotationDropdownBounds(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + ROTATION_DROPDOWN_WIDTH && mouseY >= y && mouseY < y + ROTATION_DROPDOWN_HEIGHT;
    }



    /**
     * Toggles the left (toggle) dropdown state.
     */
    private void toggleLeftDropdown() {
        showingLeftDropdown = !showingLeftDropdown;
        showingRightDropdown = false;
        showingPowerDropdown = false;
        showingLeftRotationDropdown = false;
        showingRightRotationDropdown = false;
    }

    /**
     * Toggles the right (base) dropdown state.
     */
    private void toggleRightDropdown() {
        showingRightDropdown = !showingRightDropdown;
        showingLeftDropdown = false;
        showingPowerDropdown = false;
        showingLeftRotationDropdown = false;
        showingRightRotationDropdown = false;
    }

    /**
     * Toggles the power dropdown state.
     */
    private void togglePowerDropdown() {
        showingPowerDropdown = !showingPowerDropdown;
        showingLeftDropdown = false;
        showingRightDropdown = false;
        showingLeftRotationDropdown = false;
        showingRightRotationDropdown = false;
    }

    /**
     * Toggles the toggle rotation dropdown state.
     */
    private void toggleToggleRotationDropdown() {
        showingLeftRotationDropdown = !showingLeftRotationDropdown;
        showingLeftDropdown = false;
        showingRightDropdown = false;
        showingPowerDropdown = false;
        showingRightRotationDropdown = false;
    }

    /**
     * Toggles the base rotation dropdown state.
     */
    private void toggleBaseRotationDropdown() {
        showingRightRotationDropdown = !showingRightRotationDropdown;
        showingLeftDropdown = false;
        showingRightDropdown = false;
        showingPowerDropdown = false;
        showingLeftRotationDropdown = false;
    }



    /**
     * Handles dropdown option selection.
     */
    private boolean handleDropdownSelection(double mouseX, double mouseY, int guiLeft, int guiTop, boolean isLeft) {
        FaceSelectionData.RawTextureSelection selection = isLeft ? leftTextureSelection : rightTextureSelection;
        List<String> variables = selection.availableVariables();

        int dropdownX = isLeft ? guiLeft + LEFT_FACE_X : guiLeft + RIGHT_FACE_X;
        int dropdownY = (isLeft ? guiTop + LEFT_FACE_Y : guiTop + RIGHT_FACE_Y) + FACE_DROPDOWN_HEIGHT;

        for (int i = 0; i < variables.size(); i++) {
            int optionY = dropdownY + (i * 12);
            if (isWithinDropdownBounds(mouseX, mouseY, dropdownX, optionY)) {
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
     * Handles power dropdown option selection.
     */
    private boolean handlePowerDropdownSelection(double mouseX, double mouseY, int guiLeft, int guiTop) {
        SwitchesLeverBlockEntity.PowerMode[] modes = SwitchesLeverBlockEntity.PowerMode.values();
        
        int dropdownX = guiLeft + POWER_DROPDOWN_X;
        int dropdownY = guiTop + POWER_DROPDOWN_Y + POWER_DROPDOWN_HEIGHT;
        
        for (int i = 0; i < modes.length; i++) {
            int optionY = dropdownY + (i * 12);
            if (isWithinPowerDropdownBounds(mouseX, mouseY, dropdownX, optionY)) {
                menu.setPowerMode(modes[i]);
                showingPowerDropdown = false;
                return true;
            }
        }
        
        return false;
    }

    /**
     * Handles toggle rotation dropdown option selection.
     */
    private boolean handleToggleRotationDropdownSelection(double mouseX, double mouseY, int guiLeft, int guiTop) {
        TextureRotation[] rotations = TextureRotation.values();
        
        int dropdownX = guiLeft + LEFT_ROTATION_X;
        int dropdownY = guiTop + LEFT_ROTATION_Y + ROTATION_DROPDOWN_HEIGHT;
        
        for (int i = 0; i < rotations.length; i++) {
            int optionY = dropdownY + (i * 12);
            if (isWithinRotationDropdownBounds(mouseX, mouseY, dropdownX, optionY)) {
                menu.setToggleTextureRotation(rotations[i]);
                showingLeftRotationDropdown = false;
                return true;
            }
        }
        
        return false;
    }



    /**
     * Handles base rotation dropdown option selection.
     */
    private boolean handleBaseRotationDropdownSelection(double mouseX, double mouseY, int guiLeft, int guiTop) {
        TextureRotation[] rotations = TextureRotation.values();
        
        int dropdownX = guiLeft + RIGHT_ROTATION_X;
        int dropdownY = guiTop + RIGHT_ROTATION_Y + ROTATION_DROPDOWN_HEIGHT;
        
        for (int i = 0; i < rotations.length; i++) {
            int optionY = dropdownY + (i * 12);
            if (isWithinRotationDropdownBounds(mouseX, mouseY, dropdownX, optionY)) {
                menu.setBaseTextureRotation(rotations[i]);
                showingRightRotationDropdown = false;
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
     * Renders a basic 3D switch preview.
     */
    private void drawBasic3DPreview(@Nonnull GuiGraphics graphics, int guiLeft, int guiTop) {
        try {
            ItemStack switchStack = new ItemStack(
                    net.justsomeswitches.init.JustSomeSwitchesModBlocks.SWITCHES_LEVER_ITEM.get());

            int centerX = guiLeft + PREVIEW_CENTER_X;
            int centerY = guiTop + PREVIEW_CENTER_Y;

            graphics.pose().pushPose();
            graphics.pose().translate(centerX, centerY, 100);
            graphics.pose().scale(PREVIEW_SCALE, PREVIEW_SCALE, 1.0f);
            graphics.pose().translate(-8, -8, 0);
            graphics.renderItem(switchStack, 0, 0);
            graphics.pose().popPose();
        } catch (Exception e) {
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
     * Draws dropdown buttons for face selection and rotation.
     * 
     * @param graphics the GUI graphics context
     * @param guiLeft the GUI left offset
     * @param guiTop the GUI top offset
     */
    private void drawCleanArchitectureDropdowns(@Nonnull GuiGraphics graphics, int guiLeft, int guiTop) {
        // Left (toggle) face selection dropdown
        drawCleanDropdownButton(graphics, guiLeft + LEFT_FACE_X, guiTop + LEFT_FACE_Y,
                leftTextureSelection, showingLeftDropdown);

        // Right (base) face selection dropdown
        drawCleanDropdownButton(graphics, guiLeft + RIGHT_FACE_X, guiTop + RIGHT_FACE_Y,
                rightTextureSelection, showingRightDropdown);

        // NEW: Left (toggle) rotation dropdown - blank placeholder
        drawRotationDropdownButton(graphics, guiLeft + LEFT_ROTATION_X, guiTop + LEFT_ROTATION_Y, 
                showingLeftRotationDropdown, true);

        // NEW: Right (base) rotation dropdown - blank placeholder
        drawRotationDropdownButton(graphics, guiLeft + RIGHT_ROTATION_X, guiTop + RIGHT_ROTATION_Y, 
                showingRightRotationDropdown, false);


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
            if (displayText.length() > 7) {
                displayText = displayText.substring(0, 7);
            }
        }

        // Only draw text if not empty
        if (!displayText.isEmpty()) {
            // Use same text scaling and positioning as power dropdown
            graphics.pose().pushPose();
            graphics.pose().scale(0.8f, 0.8f, 1.0f);
            // Calculate vertically centered position with same formula as power dropdown
            int fontHeight = this.font.lineHeight;
            int centeredY = (int)((y + (FACE_DROPDOWN_HEIGHT - fontHeight) / 2.0 + 2) / 0.8f);
            graphics.drawString(this.font, displayText, (int)((x + 3) / 0.8f), centeredY, textColor, false);
            graphics.pose().popPose();
        }
    }

    /**
     * Checks if base texture slot has a block (regardless of face selection availability).
     */
    private boolean hasBaseTextureBlock() {
        return rightTextureSelection.hasPreview() && 
               !rightTextureSelection.sourceBlock().isEmpty();
    }

    /**
     * Checks if toggle texture slot has a block (regardless of face selection availability).
     */
    private boolean hasToggleTextureBlock() {
        return leftTextureSelection.hasPreview() && 
               !leftTextureSelection.sourceBlock().isEmpty();
    }

    /**
     * Draws a rotation dropdown button.
     * 
     * @param graphics the GUI graphics context
     * @param x the button X position
     * @param y the button Y position
     * @param isOpen whether the dropdown is currently open
     * @param isLeft true for left (toggle) rotation, false for right (base) rotation
     */
    private void drawRotationDropdownButton(@Nonnull GuiGraphics graphics, int x, int y, boolean isOpen, boolean isLeft) {
        if (isLeft) {
            // Left (toggle) rotation dropdown - functional when any block in toggle texture slot
            if (hasToggleTextureBlock()) {
                drawToggleRotationDropdown(graphics, x, y, isOpen);
            } else {
                drawDisabledRotationDropdown(graphics, x, y);
            }
        } else {
            // Right (base) rotation dropdown - functional when any block in base texture slot
            if (hasBaseTextureBlock()) {
                drawBaseRotationDropdown(graphics, x, y, isOpen);
            } else {
                drawDisabledRotationDropdown(graphics, x, y);
            }
        }
    }
    
    /**
     * Draws the disabled toggle rotation dropdown (placeholder).
     */
    private void drawDisabledRotationDropdown(@Nonnull GuiGraphics graphics, int x, int y) {
        // Draw dropdown background (disabled/grayed out as placeholder)
        int bgColor = 0xFF888888;

        // Draw dropdown background
        graphics.fill(x, y, x + ROTATION_DROPDOWN_WIDTH, y + ROTATION_DROPDOWN_HEIGHT, bgColor);

        // Draw disabled border
        graphics.fill(x, y, x + ROTATION_DROPDOWN_WIDTH, y + 1, 0xFF666666);
        graphics.fill(x, y, x + 1, y + ROTATION_DROPDOWN_HEIGHT, 0xFF666666);
        graphics.fill(x, y + ROTATION_DROPDOWN_HEIGHT - 1, x + ROTATION_DROPDOWN_WIDTH, y + ROTATION_DROPDOWN_HEIGHT, 0xFF999999);
        graphics.fill(x + ROTATION_DROPDOWN_WIDTH - 1, y, x + ROTATION_DROPDOWN_WIDTH, y + ROTATION_DROPDOWN_HEIGHT, 0xFF999999);

        // Draw dropdown arrow (disabled)
        int arrowColor = 0xFF666666;
        int arrowX = x + ROTATION_DROPDOWN_WIDTH - 8;
        int arrowY = y + 4;

        // Down arrow (closed state)
        graphics.fill(arrowX, arrowY, arrowX + 4, arrowY + 1, arrowColor);          // Top: wide
        graphics.fill(arrowX + 1, arrowY + 1, arrowX + 3, arrowY + 2, arrowColor);  // Middle
        graphics.fill(arrowX + 2, arrowY + 2, arrowX + 2, arrowY + 3, arrowColor);  // Bottom: narrow

        // Leave text area blank (placeholder)
    }
    
    /**
     * Draws the functional toggle rotation dropdown.
     */
    private void drawToggleRotationDropdown(@Nonnull GuiGraphics graphics, int x, int y, boolean isOpen) {
        TextureRotation currentRotation = menu.getToggleTextureRotation();
        
        // Draw dropdown background (enabled)
        int bgColor = 0xFFC6C6C6;
        graphics.fill(x, y, x + ROTATION_DROPDOWN_WIDTH, y + ROTATION_DROPDOWN_HEIGHT, bgColor);
        
        // Draw dropdown border
        int lightColor = isOpen ? 0xFF555555 : 0xFFFFFFFF;
        int darkColor = isOpen ? 0xFFFFFFFF : 0xFF555555;
        
        graphics.fill(x, y, x + ROTATION_DROPDOWN_WIDTH, y + 1, lightColor);
        graphics.fill(x, y, x + 1, y + ROTATION_DROPDOWN_HEIGHT, lightColor);
        graphics.fill(x, y + ROTATION_DROPDOWN_HEIGHT - 1, x + ROTATION_DROPDOWN_WIDTH, y + ROTATION_DROPDOWN_HEIGHT, darkColor);
        graphics.fill(x + ROTATION_DROPDOWN_WIDTH - 1, y, x + ROTATION_DROPDOWN_WIDTH, y + ROTATION_DROPDOWN_HEIGHT, darkColor);
        
        // Draw dropdown arrow
        int arrowColor = 0xFF000000;
        int arrowX = x + ROTATION_DROPDOWN_WIDTH - 8;
        int arrowY = y + 4;
        
        if (isOpen) {
            // Up arrow (open state)
            graphics.fill(arrowX + 1, arrowY, arrowX + 3, arrowY + 1, arrowColor);
            graphics.fill(arrowX, arrowY + 1, arrowX + 4, arrowY + 2, arrowColor);
            graphics.fill(arrowX, arrowY + 2, arrowX + 4, arrowY + 3, arrowColor);
        } else {
            // Down arrow (closed state)
            graphics.fill(arrowX, arrowY, arrowX + 4, arrowY + 1, arrowColor);
            graphics.fill(arrowX, arrowY + 1, arrowX + 4, arrowY + 2, arrowColor);
            graphics.fill(arrowX + 1, arrowY + 2, arrowX + 3, arrowY + 3, arrowColor);
        }
        
        // Draw current rotation text (70% scale, 6 characters max)
        String displayText = currentRotation.getDisplayName();
        if (displayText.length() > 6) {
            displayText = displayText.substring(0, 6);
        }
        
        graphics.pose().pushPose();
        graphics.pose().scale(0.70f, 0.70f, 1.0f);
        // Calculate vertically centered position
        int fontHeight = this.font.lineHeight;
        int centeredY = (int)((y + (ROTATION_DROPDOWN_HEIGHT - fontHeight) / 2.0 + 2) / 0.70f);
        graphics.drawString(this.font, displayText, (int)((x + 3) / 0.70f), centeredY, 0xFF404040, false);
        graphics.pose().popPose();
    }

    /**
     * Draws the functional base rotation dropdown.
     */
    private void drawBaseRotationDropdown(@Nonnull GuiGraphics graphics, int x, int y, boolean isOpen) {
        TextureRotation currentRotation = menu.getBaseTextureRotation();
        
        // Draw dropdown background (enabled)
        int bgColor = 0xFFC6C6C6;
        graphics.fill(x, y, x + ROTATION_DROPDOWN_WIDTH, y + ROTATION_DROPDOWN_HEIGHT, bgColor);
        
        // Draw dropdown border
        int lightColor = isOpen ? 0xFF555555 : 0xFFFFFFFF;
        int darkColor = isOpen ? 0xFFFFFFFF : 0xFF555555;
        
        graphics.fill(x, y, x + ROTATION_DROPDOWN_WIDTH, y + 1, lightColor);
        graphics.fill(x, y, x + 1, y + ROTATION_DROPDOWN_HEIGHT, lightColor);
        graphics.fill(x, y + ROTATION_DROPDOWN_HEIGHT - 1, x + ROTATION_DROPDOWN_WIDTH, y + ROTATION_DROPDOWN_HEIGHT, darkColor);
        graphics.fill(x + ROTATION_DROPDOWN_WIDTH - 1, y, x + ROTATION_DROPDOWN_WIDTH, y + ROTATION_DROPDOWN_HEIGHT, darkColor);
        
        // Draw dropdown arrow
        int arrowColor = 0xFF000000;
        int arrowX = x + ROTATION_DROPDOWN_WIDTH - 8;
        int arrowY = y + 4;
        
        if (isOpen) {
            // Up arrow (open state)
            graphics.fill(arrowX + 1, arrowY, arrowX + 3, arrowY + 1, arrowColor);
            graphics.fill(arrowX, arrowY + 1, arrowX + 4, arrowY + 2, arrowColor);
            graphics.fill(arrowX, arrowY + 2, arrowX + 4, arrowY + 3, arrowColor);
        } else {
            // Down arrow (closed state)
            graphics.fill(arrowX, arrowY, arrowX + 4, arrowY + 1, arrowColor);
            graphics.fill(arrowX, arrowY + 1, arrowX + 4, arrowY + 2, arrowColor);
            graphics.fill(arrowX + 1, arrowY + 2, arrowX + 3, arrowY + 3, arrowColor);
        }
        
        // Draw current rotation text (70% scale, 6 characters max)
        String displayText = currentRotation.getDisplayName();
        if (displayText.length() > 6) {
            displayText = displayText.substring(0, 6);
        }
        
        graphics.pose().pushPose();
        graphics.pose().scale(0.70f, 0.70f, 1.0f);
        // Calculate vertically centered position
        int fontHeight = this.font.lineHeight;
        int centeredY = (int)((y + (ROTATION_DROPDOWN_HEIGHT - fontHeight) / 2.0 + 2) / 0.70f);
        graphics.drawString(this.font, displayText, (int)((x + 3) / 0.70f), centeredY, 0xFF404040, false);
        graphics.pose().popPose();
    }



    /**
     * Draws 2D texture previews with null safety.
     */
    private void drawSafeTexturePreview(@Nonnull GuiGraphics graphics, int guiLeft, int guiTop) {
        // Draw left (toggle) texture preview with rotation support
        if (leftTextureSelection.hasPreview()) {
            String leftPreviewTexture = leftTextureSelection.previewTexture();
            if (leftPreviewTexture != null && !leftPreviewTexture.isEmpty()) {
                TextureRotation toggleRotation = menu.getToggleTextureRotation();
                drawTexturePreviewBox(graphics, guiLeft + LEFT_PREVIEW_X, guiTop + LEFT_PREVIEW_Y, leftPreviewTexture, toggleRotation);
            }
        }

        // Draw right (base) texture preview with rotation support
        if (rightTextureSelection.hasPreview()) {
            String rightPreviewTexture = rightTextureSelection.previewTexture();
            if (rightPreviewTexture != null && !rightPreviewTexture.isEmpty()) {
                TextureRotation baseRotation = menu.getBaseTextureRotation();
                drawTexturePreviewBox(graphics, guiLeft + RIGHT_PREVIEW_X, guiTop + RIGHT_PREVIEW_Y, rightPreviewTexture, baseRotation);
            }
        }
    }

    /**
     * Draws a 2D texture preview box.
     */
    private void drawTexturePreviewBox(@Nonnull GuiGraphics graphics, int x, int y, @Nonnull String texturePath, @Nullable TextureRotation rotation) {
        try {
            TextureAtlasSprite sprite = getTextureSprite(texturePath);
            if (sprite != null && !getSafeSpriteName(sprite).contains("missingno")) {
                graphics.fill(x, y, x + PREVIEW_SIZE, y + PREVIEW_SIZE, 0xFFFFFFFF);
                if (rotation != null && rotation != TextureRotation.NORMAL) {
                    drawRotatedTexturePreview(graphics, x, y, sprite, rotation);
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
    private void drawRotatedTexturePreview(@Nonnull GuiGraphics graphics, int x, int y, @Nonnull TextureAtlasSprite sprite, @Nonnull TextureRotation rotation) {
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
     * Gets texture sprite for 2D preview rendering.
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

    /**
     * Safely retrieves sprite name.
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

    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Use standard container screen background for lighter darkening (matches vanilla GUIs)
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

        if (showingLeftRotationDropdown) {
            drawToggleRotationDropdownPopup(graphics, guiLeft + LEFT_ROTATION_X, guiTop + LEFT_ROTATION_Y + ROTATION_DROPDOWN_HEIGHT);
        }

        if (showingRightRotationDropdown) {
            drawBaseRotationDropdownPopup(graphics, guiLeft + RIGHT_ROTATION_X, guiTop + RIGHT_ROTATION_Y + ROTATION_DROPDOWN_HEIGHT);
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

            String displayText = variable;
            if (displayText.length() > 7) {
                displayText = displayText.substring(0, 7);
            }
            graphics.pose().pushPose();
            graphics.pose().scale(0.8f, 0.8f, 1.0f);
            // Calculate vertically centered position for popup text with same positioning as power dropdown
            int fontHeight = this.font.lineHeight;
            int centeredY = (int)((optionY + (12 - fontHeight) / 2.0 + 2) / 0.8f);
            graphics.drawString(this.font, displayText, (int)((x + 3) / 0.8f), centeredY, 0xFF000000, false);
            graphics.pose().popPose();
        }

        // Restore z-order
        graphics.pose().popPose();
    }

    /**
     * Draws toggle rotation dropdown popup menu.
     */
    private void drawToggleRotationDropdownPopup(@Nonnull GuiGraphics graphics, int x, int y) {
        TextureRotation[] rotations = TextureRotation.values();
        TextureRotation currentRotation = menu.getToggleTextureRotation();
        int popupHeight = rotations.length * 12;
        
        // Elevate z-order to render above other elements
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 400);
        
        // Draw popup background
        graphics.fill(x, y, x + ROTATION_DROPDOWN_WIDTH, y + popupHeight, 0xFFC6C6C6);
        
        // Draw popup border
        graphics.fill(x, y, x + ROTATION_DROPDOWN_WIDTH, y + 1, 0xFF000000);
        graphics.fill(x, y, x + 1, y + popupHeight, 0xFF000000);
        graphics.fill(x + ROTATION_DROPDOWN_WIDTH - 1, y, x + ROTATION_DROPDOWN_WIDTH, y + popupHeight, 0xFF000000);
        graphics.fill(x, y + popupHeight, x + ROTATION_DROPDOWN_WIDTH, y + popupHeight + 1, 0xFF000000);
        
        // Draw rotation options
        for (int i = 0; i < rotations.length; i++) {
            TextureRotation rotation = rotations[i];
            int optionY = y + (i * 12);
            
            // Highlight selected rotation
            if (rotation == currentRotation) {
                graphics.fill(x + 1, optionY, x + ROTATION_DROPDOWN_WIDTH - 1, optionY + 12, 0xFF8888FF);
            }
            
            // Draw rotation name (6 characters max, 70% scale like dropdown button)
            String rotationText = rotation.getDisplayName();
            if (rotationText.length() > 6) {
                rotationText = rotationText.substring(0, 6);
            }
            
            graphics.pose().pushPose();
            graphics.pose().scale(0.70f, 0.70f, 1.0f);
            int fontHeight = this.font.lineHeight;
            int centeredY = (int)((optionY + (12 - fontHeight) / 2.0 + 2) / 0.70f);
            graphics.drawString(this.font, rotationText, (int)((x + 3) / 0.70f), centeredY, 0xFF000000, false);
            graphics.pose().popPose();
        }
        
        // Restore z-order
        graphics.pose().popPose();
    }

    /**
     * Draws base rotation dropdown popup menu.
     */
    private void drawBaseRotationDropdownPopup(@Nonnull GuiGraphics graphics, int x, int y) {
        TextureRotation[] rotations = TextureRotation.values();
        TextureRotation currentRotation = menu.getBaseTextureRotation();
        int popupHeight = rotations.length * 12;
        
        // Elevate z-order to render above other elements
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 400);
        
        // Draw popup background
        graphics.fill(x, y, x + ROTATION_DROPDOWN_WIDTH, y + popupHeight, 0xFFC6C6C6);
        
        // Draw popup border
        graphics.fill(x, y, x + ROTATION_DROPDOWN_WIDTH, y + 1, 0xFF000000);
        graphics.fill(x, y, x + 1, y + popupHeight, 0xFF000000);
        graphics.fill(x + ROTATION_DROPDOWN_WIDTH - 1, y, x + ROTATION_DROPDOWN_WIDTH, y + popupHeight, 0xFF000000);
        graphics.fill(x, y + popupHeight, x + ROTATION_DROPDOWN_WIDTH, y + popupHeight + 1, 0xFF000000);
        
        // Draw rotation options
        for (int i = 0; i < rotations.length; i++) {
            TextureRotation rotation = rotations[i];
            int optionY = y + (i * 12);
            
            // Highlight selected rotation
            if (rotation == currentRotation) {
                graphics.fill(x + 1, optionY, x + ROTATION_DROPDOWN_WIDTH - 1, optionY + 12, 0xFF8888FF);
            }
            
            // Draw rotation name (6 characters max, 70% scale like dropdown button)
            String rotationText = rotation.getDisplayName();
            if (rotationText.length() > 6) {
                rotationText = rotationText.substring(0, 6);
            }
            
            graphics.pose().pushPose();
            graphics.pose().scale(0.70f, 0.70f, 1.0f);
            int fontHeight = this.font.lineHeight;
            int centeredY = (int)((optionY + (12 - fontHeight) / 2.0 + 2) / 0.70f);
            graphics.drawString(this.font, rotationText, (int)((x + 3) / 0.70f), centeredY, 0xFF000000, false);
            graphics.pose().popPose();
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
            // Up arrow (open state) - narrow at top, wide at bottom
            graphics.fill(arrowX + 2, arrowY, arrowX + 4, arrowY + 1, arrowColor);
            graphics.fill(arrowX + 1, arrowY + 1, arrowX + 5, arrowY + 2, arrowColor);
            graphics.fill(arrowX, arrowY + 2, arrowX + 6, arrowY + 3, arrowColor);
        } else {
            // Down arrow (closed state) - wide at top, narrow at bottom
            graphics.fill(arrowX, arrowY, arrowX + 6, arrowY + 1, arrowColor);
            graphics.fill(arrowX + 1, arrowY + 1, arrowX + 5, arrowY + 2, arrowColor);
            graphics.fill(arrowX + 2, arrowY + 2, arrowX + 4, arrowY + 3, arrowColor);
        }
        
        // Draw current power mode text (lowercase as specified)
        String displayText = formatPowerModeText(currentMode.name());
        graphics.pose().pushPose();
        graphics.pose().scale(0.8f, 0.8f, 1.0f);
        // Calculate vertically centered position, move 1 pixel right, and move down 1 additional pixel
        int fontHeight = this.font.lineHeight;
        int centeredY = (int)((y + (POWER_DROPDOWN_HEIGHT - fontHeight) / 2.0 + 2) / 0.8f);
        graphics.drawString(this.font, displayText, (int)((x + 3) / 0.8f), centeredY, 0xFF404040, false);
        graphics.pose().popPose();
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
            graphics.pose().pushPose();
            graphics.pose().scale(0.8f, 0.8f, 1.0f);
            // Calculate vertically centered position for popup text, move 1 pixel right, and move down 1 additional pixel
            int fontHeight = this.font.lineHeight;
            int centeredY = (int)((optionY + (12 - fontHeight) / 2.0 + 2) / 0.8f);
            graphics.drawString(this.font, modeText, (int)((x + 3) / 0.8f), centeredY, 0xFF000000, false);
            graphics.pose().popPose();
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
                    // Draw preview background (no black border)
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
            // Draw preview background (no black border)
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
        // Draw white fill (no black border)
        graphics.fill(x, y, x + POWER_PREVIEW_SIZE, y + POWER_PREVIEW_SIZE, 0xFFFFFFFF);
    }

    /**
     * Draws power category labels.
     */
    private void drawPowerLabels(@Nonnull GuiGraphics graphics, int guiLeft, int guiTop) {
        // Draw "Unpowered" label with 75% scale
        graphics.pose().pushPose();
        graphics.pose().scale(0.75f, 0.75f, 1.0f);
        graphics.drawString(this.font, "Unpowered", (int)((guiLeft + UNPOWERED_LABEL_X) / 0.75f), (int)((guiTop + UNPOWERED_LABEL_Y) / 0.75f), 0xFF404040, false);
        graphics.pose().popPose();
        
        // Draw "Powered" label with 75% scale
        graphics.pose().pushPose();
        graphics.pose().scale(0.75f, 0.75f, 1.0f);
        graphics.drawString(this.font, "Powered", (int)((guiLeft + POWERED_LABEL_X) / 0.75f), (int)((guiTop + POWERED_LABEL_Y) / 0.75f), 0xFF404040, false);
        graphics.pose().popPose();
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