package net.justsomeswitches.gui;

import net.justsomeswitches.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Copy Texture Settings GUI with selective copying interface
 */
public class WrenchCopyScreen extends AbstractContainerScreen<WrenchCopyMenu> {
    
    // UI constants
    private static final ResourceLocation GUI_TEXTURE = 
        new ResourceLocation("justsomeswitches", "textures/gui/wrench_copy_gui.png");
    private static final int GUI_WIDTH = 187;
    private static final int GUI_HEIGHT = 240;
    private static final int CHECKBOX_SIZE = 16;
    private static final int PREVIEW_SIZE = 20;
    private static final int SMALL_PREVIEW_SIZE = 6;
    
    // Checkbox sprites
    private static final ResourceLocation CHECKBOX = new ResourceLocation("widget/checkbox");
    private static final ResourceLocation CHECKBOX_HIGHLIGHTED = new ResourceLocation("widget/checkbox_highlighted");
    private static final ResourceLocation CHECKBOX_SELECTED = new ResourceLocation("widget/checkbox_selected");
    private static final ResourceLocation CHECKBOX_SELECTED_HIGHLIGHTED = new ResourceLocation("widget/checkbox_selected_highlighted");
    
    // Checkbox and preview positions for each category
    private final CheckboxPosition[] checkboxPositions = {
        new CheckboxPosition(15, 25, 154, 23, "Toggle Block"),     // Toggle Block - 16x16 centered at previous 20x20 position
        new CheckboxPosition(15, 47, 154, 45, "Toggle Face"),      // Toggle Face - 16x16 centered at previous 20x20 position
        new CheckboxPosition(15, 69, 154, 68, "Toggle Rotation"),  // Toggle Rotation - 16x16 centered at previous 20x20 position
        new CheckboxPosition(15, 91, 161, 92, "Indicators"),       // Indicators - 16x16 centered at previous 20x20 position
        new CheckboxPosition(15, 113, 154, 111, "Base Block"),     // Base Block - 16x16 centered at previous 20x20 position
        new CheckboxPosition(15, 135, 154, 133, "Base Face"),      // Base Face - 16x16 centered at previous 20x20 position
        new CheckboxPosition(15, 157, 154, 156, "Base Rotation")   // Base Rotation - 16x16 centered at previous 20x20 position
    };
    
    // Button positions from user specifications
    private static final int SELECT_ALL_X = 12;
    private static final int SELECT_ALL_Y = 179;  // Moved up 6 pixels (185 - 6)
    private static final int CLEAR_ALL_X = 95;
    private static final int CLEAR_ALL_Y = 179;   // Moved up 6 pixels (185 - 6)
    private static final int COPY_SELECTED_X = 12;
    private static final int COPY_SELECTED_Y = 208;
    private static final int CANCEL_X = 95;
    private static final int CANCEL_Y = 208;
    private static final int BUTTON_WIDTH = 80;
    private static final int BUTTON_HEIGHT = 20;
    
    // Helper class for checkbox positioning
    private static class CheckboxPosition {
        final int checkboxX, checkboxY;
        final int previewX, previewY;
        final String label;
        
        CheckboxPosition(int checkboxX, int checkboxY, int previewX, int previewY, String label) {
            this.checkboxX = checkboxX;
            this.checkboxY = checkboxY;
            this.previewX = previewX;
            this.previewY = previewY;
            this.label = label;
        }
    }
    
    public WrenchCopyScreen(@Nonnull WrenchCopyMenu menu, @Nonnull Inventory playerInventory, @Nonnull Component title) {
        super(menu, playerInventory, Component.literal("Copy Texture Settings"));
        
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        
        // Hide labels as they are rendered in renderLabels() method
        this.titleLabelX = -1000;
        this.titleLabelY = -1000;
        this.inventoryLabelX = -1000;
        this.inventoryLabelY = -1000;
    }
    
    @Override
    protected void init() {
        super.init();
        
        int guiLeft = (this.width - this.imageWidth) / 2;
        int guiTop = (this.height - this.imageHeight) / 2;
        
        // Add buttons using exact coordinates from user specifications
        addRenderableWidget(Button.builder(
            Component.literal("Select All"),
            button -> handleSelectAll()
        ).bounds(guiLeft + SELECT_ALL_X, guiTop + SELECT_ALL_Y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        
        addRenderableWidget(Button.builder(
            Component.literal("Clear All"),
            button -> handleClearAll()
        ).bounds(guiLeft + CLEAR_ALL_X, guiTop + CLEAR_ALL_Y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        
        addRenderableWidget(Button.builder(
            Component.literal("Copy Selected"),
            button -> handleCopySelected()
        ).bounds(guiLeft + COPY_SELECTED_X, guiTop + COPY_SELECTED_Y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        
        addRenderableWidget(Button.builder(
            Component.literal("Cancel"),
            button -> handleCancel()
        ).bounds(guiLeft + CANCEL_X, guiTop + CANCEL_Y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }
    
    @Override
    protected void renderBg(@Nonnull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int guiLeft = (this.width - this.imageWidth) / 2;
        int guiTop = (this.height - this.imageHeight) / 2;
        
        // Draw GUI background image
        graphics.blit(GUI_TEXTURE, guiLeft, guiTop, 0, 0, this.imageWidth, this.imageHeight);
        
        // Draw checkboxes and previews using exact positions
        int index = 0;
        for (CheckboxPosition pos : checkboxPositions) {
            drawVanillaCheckbox(graphics, guiLeft + pos.checkboxX, guiTop + pos.checkboxY, index, mouseX, mouseY);
            drawPreview(graphics, guiLeft + pos.previewX, guiTop + pos.previewY, index);
            index++;
        }
    }
    
    @Override
    protected void renderLabels(@Nonnull GuiGraphics graphics, int mouseX, int mouseY) {
        // Draw title like default GUI headers
        graphics.drawString(this.font, this.title, 8, 6, 0x404040, false);
        
        // Draw checkbox labels with button text formatting and drop shadow
        for (int i = 0; i < checkboxPositions.length; i++) {
            CheckboxPosition pos = checkboxPositions[i];
            int labelX = pos.checkboxX + CHECKBOX_SIZE + 5; // 5px spacing after checkbox
            int labelY = (int)(pos.checkboxY + 4.5f); // Center text vertically with checkbox (moved up 1.5 pixels total)
            
            // Use same color as button text with drop shadow
            graphics.drawString(this.font, pos.label, labelX, labelY, 0xFFFFFF, true);
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Left click
            int guiLeft = (this.width - this.imageWidth) / 2;
            int guiTop = (this.height - this.imageHeight) / 2;
            
            // Check if click is on any checkbox using exact positions
            for (int i = 0; i < checkboxPositions.length; i++) {
                CheckboxPosition pos = checkboxPositions[i];
                int checkboxX = guiLeft + pos.checkboxX;
                int checkboxY = guiTop + pos.checkboxY;
                
                if (mouseX >= checkboxX && mouseX < checkboxX + CHECKBOX_SIZE &&
                    mouseY >= checkboxY && mouseY < checkboxY + CHECKBOX_SIZE) {
                    toggleCheckbox(i);
                    return true;
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    /**
     * Draws vanilla Minecraft checkbox with current selection state
     */
    private void drawVanillaCheckbox(@Nonnull GuiGraphics graphics, int x, int y, int index, int mouseX, int mouseY) {
        boolean isChecked = getCheckboxState(index);
        boolean isHovered = mouseX >= x && mouseX < x + CHECKBOX_SIZE && 
                           mouseY >= y && mouseY < y + CHECKBOX_SIZE;
        
        // Select appropriate checkbox sprite based on state
        ResourceLocation checkboxSprite;
        if (isChecked && isHovered) {
            checkboxSprite = CHECKBOX_SELECTED_HIGHLIGHTED;
        } else if (isChecked) {
            checkboxSprite = CHECKBOX_SELECTED;
        } else if (isHovered) {
            checkboxSprite = CHECKBOX_HIGHLIGHTED;
        } else {
            checkboxSprite = CHECKBOX;
        }
        
        // Draw the vanilla checkbox sprite
        graphics.blitSprite(checkboxSprite, x, y, CHECKBOX_SIZE, CHECKBOX_SIZE);
    }
    
    /**
     * Draws preview for the setting (matching texture customization GUI exactly)
     */
    private void drawPreview(@Nonnull GuiGraphics graphics, int x, int y, int index) {
        switch (index) {
            case 0: // Toggle Block
                drawBlockItemPreview(graphics, x, y, menu.getToggleBlockItemStack());
                break;
                
            case 1: // Toggle Face
                drawFaceTexturePreview(graphics, x + 1, y + 1, true); // 18x18 centered in 20x20 space
                break;
                
            case 2: // Toggle Rotation
                drawRotationPreview(graphics, x, y, index);
                break;
                
            case 3: // Indicators
                drawIndicatorsPreview(graphics, x, y);
                break;
                
            case 4: // Base Block
                drawBlockItemPreview(graphics, x, y, menu.getBaseBlockItemStack());
                break;
                
            case 5: // Base Face
                drawFaceTexturePreview(graphics, x + 1, y + 1, false); // 18x18 centered in 20x20 space
                break;
                
            case 6: // Base Rotation
                drawRotationPreview(graphics, x, y, index);
                break;
        }
    }
    
    private void drawBlockItemPreview(@Nonnull GuiGraphics graphics, int x, int y, @Nonnull ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            drawCenteredTextInBox(graphics, "Default", x, y, PREVIEW_SIZE, PREVIEW_SIZE);
        } else {
            graphics.pose().pushPose();
            graphics.pose().translate(x + 10f, y + 10f, 0);
            graphics.pose().scale(1.125f, 1.125f, 1.0f);
            graphics.pose().translate(-8f, -8f, 0);
            graphics.renderItem(itemStack, 0, 0);
            graphics.pose().popPose();
        }
    }
    
    /**
     * Draws face texture preview (matching SwitchTextureScreen style)
     */
    private void drawFaceTexturePreview(@Nonnull GuiGraphics graphics, int x, int y, boolean isToggle) {
        final int size = 18; // Always 18x18 for face previews
        try {
            String texturePath = isToggle ? menu.getToggleTexturePathForPreview() : menu.getBaseTexturePathForPreview();
            
            if (texturePath != null && !texturePath.isEmpty() && !"Default".equals(texturePath)) {
                // Get rotation if rotation copying is enabled
                net.justsomeswitches.util.TextureRotation rotation = null;
                if ((isToggle && menu.getCopyToggleRotation()) || (!isToggle && menu.getCopyBaseRotation())) {
                    rotation = isToggle ? menu.getToggleTextureRotation() : menu.getBaseTextureRotation();
                }
                
                // Draw texture preview box like SwitchTextureScreen
                drawTexturePreviewBox(graphics, x, y, size, texturePath, rotation);
            } else {
                // Draw "Default" text centered in the preview box
                String faceVariable = getPreviewText(isToggle ? 1 : 5);
                String displayText = (!faceVariable.trim().isEmpty()) ? faceVariable : "Default";
                // Change "all" to "Default" for consistency
                if ("all".equals(displayText)) {
                    displayText = "Default";
                }
                
                drawCenteredTextInBox(graphics, displayText, x, y, size, size);
            }
        } catch (Exception e) {
            // Error fallback
            graphics.fill(x, y, x + size, y + size, 0xFFFF0000);
        }
    }
    
    /**
     * Draws texture preview box
     */
    private void drawTexturePreviewBox(@Nonnull GuiGraphics graphics, int x, int y, int size, @Nonnull String texturePath, @Nullable net.justsomeswitches.util.TextureRotation rotation) {
        try {
            TextureAtlasSprite sprite = getTextureSprite(texturePath);
            if (sprite != null && !getSafeSpriteName(sprite).contains("missingno")) {
                graphics.fill(x, y, x + size, y + size, 0xFFFFFFFF);
                if (rotation != null && rotation != net.justsomeswitches.util.TextureRotation.NORMAL) {
                    drawRotatedTexturePreview(graphics, x, y, size, sprite, rotation);
                } else {
                    graphics.blit(x, y, 0, size, size, sprite);
                }
            } else {
                graphics.fill(x, y, x + size, y + size, 0xFFFF00FF);
            }
        } catch (Exception e) {
            graphics.fill(x, y, x + size, y + size, 0xFFFF0000);
        }
    }
    
    /**
     * Draws rotated texture preview
     */
    private void drawRotatedTexturePreview(@Nonnull GuiGraphics graphics, int x, int y, int size, @Nonnull TextureAtlasSprite sprite, @Nonnull net.justsomeswitches.util.TextureRotation rotation) {
        graphics.pose().pushPose();
        
        // Translate to center of preview area
        float halfSize = size / 2f;
        graphics.pose().translate(x + halfSize, y + halfSize, 0);
        
        // Apply rotation
        graphics.pose().mulPose(new org.joml.Quaternionf().fromAxisAngleDeg(0, 0, 1, rotation.getDegrees()));
        
        // Translate back and draw at specified size
        graphics.pose().translate(-halfSize, -halfSize, 0);
        graphics.blit(0, 0, 0, size, size, sprite);
        
        graphics.pose().popPose();
    }
    
    /**
     * Draws rotation preview (text-based)
     */
    private void drawRotationPreview(@Nonnull GuiGraphics graphics, int x, int y, int index) {
        String previewText = getPreviewText(index);
        
        // Draw text centered in 20x20 box
        drawCenteredTextInBox(graphics, previewText, x, y, 20, 20);
    }
    
    /**
     * Draws indicators preview (same as SwitchTextureScreen power previews)
     * Using exact coordinates: unpowered at (161,92), powered at (161,100)
     */
    private void drawIndicatorsPreview(@Nonnull GuiGraphics graphics, int x, int y) {
        // Get current power mode textures
        String unpoweredTexture = getUnpoweredTexturePreview();
        String poweredTexture = getPoweredTexturePreview();
        
        // Draw unpowered preview (top box at y offset 0)
        if (!unpoweredTexture.isEmpty()) {
            drawSmallTexturePreviewBox(graphics, x, y, unpoweredTexture);
        } else {
            graphics.fill(x, y, x + SMALL_PREVIEW_SIZE, y + SMALL_PREVIEW_SIZE, 0xFFCCCCCC);
        }
        
        // Draw powered preview (bottom box at y offset 8)
        if (!poweredTexture.isEmpty()) {
            drawSmallTexturePreviewBox(graphics, x, y + 8, poweredTexture);
        } else {
            graphics.fill(x, y + 8, x + SMALL_PREVIEW_SIZE, y + 8 + SMALL_PREVIEW_SIZE, 0xFFFFAAAA);
        }
    }
    
    /**
     * Draws small texture preview box (6x6, same as SwitchTextureScreen)
     */
    private void drawSmallTexturePreviewBox(@Nonnull GuiGraphics graphics, int x, int y, @Nonnull String texturePath) {
        try {
            TextureAtlasSprite sprite = getTextureSprite(texturePath);
            if (sprite != null) {
                String spriteName = getSafeSpriteName(sprite);
                if (!spriteName.contains("missingno")) {
                    // Draw preview background
                    graphics.fill(x, y, x + SMALL_PREVIEW_SIZE, y + SMALL_PREVIEW_SIZE, 0xFFFFFFFF);
                    
                    // Use same UV-specific preview logic as SwitchTextureScreen
                    drawUVSpecificPreview(graphics, x, y, sprite);
                    return;
                }
            }
            // Fallback
            graphics.fill(x, y, x + SMALL_PREVIEW_SIZE, y + SMALL_PREVIEW_SIZE, 0xFFCCCCCC);
        } catch (Exception e) {
            // Error fallback
            graphics.fill(x, y, x + SMALL_PREVIEW_SIZE, y + SMALL_PREVIEW_SIZE, 0xFFCCCCCC);
        }
    }
    
    /**
     * UV-specific preview for power indicators (copied from SwitchTextureScreen)
     */
    private void drawUVSpecificPreview(@Nonnull GuiGraphics graphics, int x, int y, @Nonnull TextureAtlasSprite sprite) {
        try {
            // UV coordinates for power indicators: (2,6) with 2x2 area
            int offsetX = 6;   // UV X coordinate 2 * 3 (scale factor) = 6
            int offsetY = 18;  // UV Y coordinate 6 * 3 (scale factor) = 18
            
            // Calculate texture position so target UV area appears in preview location
            int textureX = x - offsetX;
            int textureY = y - offsetY;
            
            // Enable scissor clipping to 6x6 preview area
            graphics.enableScissor(x, y, x + SMALL_PREVIEW_SIZE, y + SMALL_PREVIEW_SIZE);
            
            // Render 48x48 texture positioned so target UV area appears in preview
            graphics.blit(textureX, textureY, 0, 48, 48, sprite);
            
            // Disable scissor clipping
            graphics.disableScissor();
            
        } catch (Exception e) {
            // Fallback to solid color
            graphics.fill(x, y, x + SMALL_PREVIEW_SIZE, y + SMALL_PREVIEW_SIZE, 0xFFCCCCCC);
        }
    }
    
    /**
     * Gets unpowered texture preview path
     */
    private String getUnpoweredTexturePreview() {
        try {
            return menu.getUnpoweredTexture();
        } catch (Exception e) {
            return "";
        }
    }
    
    /**
     * Gets powered texture preview path
     */
    private String getPoweredTexturePreview() {
        try {
            return menu.getPoweredTexture();
        } catch (Exception e) {
            return "";
        }
    }
    
    /**
     * Draws text centered both horizontally and vertically in a specified box
     */
    private void drawCenteredTextInBox(@Nonnull GuiGraphics graphics, @Nonnull String text, int boxX, int boxY, int boxWidth, int boxHeight) {
        // Calculate appropriate scale to fit text in box
        int textWidth = this.font.width(text);
        int textHeight = this.font.lineHeight;
        
        // Calculate scale factors for both dimensions
        float scaleX = (boxWidth - 2) / (float)textWidth; // -2 for some padding
        float scaleY = (boxHeight - 2) / (float)textHeight; // -2 for some padding
        
        // Use the smaller scale to ensure text fits in both dimensions
        float scale = Math.min(Math.min(scaleX, scaleY), 1.0f); // Don't scale up, only down
        
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0f);
        
        // Calculate centered position after scaling
        int scaledTextWidth = (int)(textWidth * scale);
        int scaledTextHeight = (int)(textHeight * scale);
        
        int centeredX = (int)((boxX + (boxWidth - scaledTextWidth) / 2.0f) / scale);
        int centeredY = (int)((boxY + (boxHeight - scaledTextHeight) / 2.0f) / scale);
        
        // Special vertical adjustment for rotation text values (any degree text)
        if (text.endsWith("°")) {
            centeredY -= (int)(0.5f / scale); // Move up by 0.5 pixel (scaled)
        }
        
        // Draw text with drop shadow
        graphics.drawString(this.font, text, centeredX, centeredY, 0xFFFFFF, true);
        
        graphics.pose().popPose();
    }
    
    /**
     * Gets texture sprite (same as SwitchTextureScreen)
     */
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
     * Safely retrieves sprite name (same as SwitchTextureScreen)
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
     * Gets checkbox state for given index
     */
    private boolean getCheckboxState(int index) {
        return switch (index) {
            case 0 -> menu.getCopyToggleBlock();
            case 1 -> menu.getCopyToggleFace();
            case 2 -> menu.getCopyToggleRotation();
            case 3 -> menu.getCopyIndicators();
            case 4 -> menu.getCopyBaseBlock();
            case 5 -> menu.getCopyBaseFace();
            case 6 -> menu.getCopyBaseRotation();
            default -> false;
        };
    }
    
    /**
     * Gets preview text for given index
     */
    @Nonnull
    private String getPreviewText(int index) {
        try {
            String result = switch (index) {
                case 0 -> menu.getToggleBlockDisplay();
                case 1 -> menu.getToggleFaceDisplay();
                case 2 -> menu.getToggleRotationDisplay();
                case 3 -> menu.getIndicatorsDisplay();
                case 4 -> menu.getBaseBlockDisplay();
                case 5 -> menu.getBaseFaceDisplay();
                case 6 -> menu.getBaseRotationDisplay();
                default -> "Default";
            };
            
            // Ensure we always return a non-null, non-empty string for proper preview rendering
            if (result == null || result.trim().isEmpty()) {
                result = "Default";
            }
            
            return result;
        } catch (Exception e) {
            return "Default";
        }
    }
    
    /**
     * Toggles checkbox state for given index
     */
    private void toggleCheckbox(int index) {
        switch (index) {
            case 0 -> menu.setCopyToggleBlock(!menu.getCopyToggleBlock());
            case 1 -> menu.setCopyToggleFace(!menu.getCopyToggleFace());
            case 2 -> menu.setCopyToggleRotation(!menu.getCopyToggleRotation());
            case 3 -> menu.setCopyIndicators(!menu.getCopyIndicators());
            case 4 -> menu.setCopyBaseBlock(!menu.getCopyBaseBlock());
            case 5 -> menu.setCopyBaseFace(!menu.getCopyBaseFace());
            case 6 -> menu.setCopyBaseRotation(!menu.getCopyBaseRotation());
        }
    }
    
    /**
     * Handles Select All button click
     */
    private void handleSelectAll() {
        menu.setAllCopySelections(true);
    }
    
    /**
     * Handles Clear All button click
     */
    private void handleClearAll() {
        menu.setAllCopySelections(false);
    }
    
    /**
     * Handles Copy Selected button click
     */
    private void handleCopySelected() {
        // Check if at least one option is selected
        boolean hasSelection = menu.getCopyToggleBlock() || menu.getCopyToggleFace() || 
                              menu.getCopyToggleRotation() || menu.getCopyIndicators() ||
                              menu.getCopyBaseBlock() || menu.getCopyBaseFace() || 
                              menu.getCopyBaseRotation();
        
        if (!hasSelection) {
            // TODO: Show "No settings selected" message
            return;
        }
        
        try {
            // Get block position safely
            BlockPos blockPos = menu.getBlockPos();
            if (blockPos == null) {
                this.onClose();
                return;
            }
            
            // Send copy selection to server
            NetworkHandler.sendWrenchCopySelection(
                blockPos,
                menu.getCopyToggleBlock(),
                menu.getCopyToggleFace(),
                menu.getCopyToggleRotation(),
                menu.getCopyIndicators(),
                menu.getCopyBaseBlock(),
                menu.getCopyBaseFace(),
                menu.getCopyBaseRotation()
            );
        } catch (Exception e) {
            // Handle any network or data errors gracefully
            // Just close the GUI if something goes wrong
        }
        
        // Close GUI
        this.onClose();
    }
    
    /**
     * Handles Cancel button click
     */
    private void handleCancel() {
        // Close GUI without performing copy
        this.onClose();
    }
}
