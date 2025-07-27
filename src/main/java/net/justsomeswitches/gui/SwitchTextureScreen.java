package net.justsomeswitches.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

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

    // Central 3D preview positioning
    private static final int PREVIEW_CENTER_X = 88;
    private static final int PREVIEW_CENTER_Y = 35;
    private static final int PREVIEW_SCALE = 2; // 2x scale for visibility

    // Face dropdown positioning
    private static final int LEFT_FACE_X = 14;
    private static final int LEFT_FACE_Y = 47;
    private static final int RIGHT_FACE_X = 118;
    private static final int RIGHT_FACE_Y = 47;
    private static final int FACE_DROPDOWN_WIDTH = 44;
    private static final int FACE_DROPDOWN_HEIGHT = 12;

    // 2D texture preview positioning
    private static final int LEFT_PREVIEW_X = 27;
    private static final int LEFT_PREVIEW_Y = 64;
    private static final int RIGHT_PREVIEW_X = 131;
    private static final int RIGHT_PREVIEW_Y = 64;
    private static final int PREVIEW_SIZE = 18;

    // Current dynamic state
    private FaceSelectionData.RawTextureSelection leftTextureSelection = FaceSelectionData.RawTextureSelection.createDisabled();
    private FaceSelectionData.RawTextureSelection rightTextureSelection = FaceSelectionData.RawTextureSelection.createDisabled();

    // Dropdown popup management
    private boolean showingLeftDropdown = false;
    private boolean showingRightDropdown = false;

    // Previous selections for change detection
    private ItemStack previousLeftItem = ItemStack.EMPTY;
    private ItemStack previousRightItem = ItemStack.EMPTY;

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

    @Override
    protected void init() {
        super.init();
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

        // Handle dropdown selection clicks
        if (showingLeftDropdown && handleDropdownSelection(mouseX, mouseY, guiLeft, guiTop, true)) {
            return true;
        }

        if (showingRightDropdown && handleDropdownSelection(mouseX, mouseY, guiLeft, guiTop, false)) {
            return true;
        }

        // Close dropdowns if clicking elsewhere
        if (showingLeftDropdown || showingRightDropdown) {
            showingLeftDropdown = false;
            showingRightDropdown = false;
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
     * Toggles the left (toggle) dropdown state.
     */
    private void toggleLeftDropdown() {
        showingLeftDropdown = !showingLeftDropdown;
        showingRightDropdown = false;
    }

    /**
     * Toggles the right (base) dropdown state.
     */
    private void toggleRightDropdown() {
        showingRightDropdown = !showingRightDropdown;
        showingLeftDropdown = false;
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

    @Override
    protected void renderBg(@Nonnull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int guiLeft = (this.width - this.imageWidth) / 2;
        int guiTop = (this.height - this.imageHeight) / 2;

        // Draw GUI background
        graphics.blit(GUI_BACKGROUND, guiLeft, guiTop + 4, 0, 0, this.imageWidth, this.imageHeight, 256, 256);

        // Draw 3D switch preview
        drawBasic3DPreview(graphics, guiLeft, guiTop);

        // Draw face selection dropdowns
        drawCleanArchitectureDropdowns(graphics, guiLeft, guiTop);

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

            if (!isOpen) {
                // Down arrow (closed state)
                graphics.fill(arrowX + 2, arrowY, arrowX + 4, arrowY + 1, arrowColor);
                graphics.fill(arrowX + 1, arrowY + 1, arrowX + 5, arrowY + 2, arrowColor);
                graphics.fill(arrowX, arrowY + 2, arrowX + 6, arrowY + 3, arrowColor);
            } else {
                // Up arrow (open state)
                graphics.fill(arrowX, arrowY + 2, arrowX + 6, arrowY + 3, arrowColor);
                graphics.fill(arrowX + 1, arrowY + 1, arrowX + 5, arrowY + 2, arrowColor);
                graphics.fill(arrowX + 2, arrowY, arrowX + 4, arrowY + 1, arrowColor);
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
            // Safe access to sprite contents
            var contents = sprite.contents();
            if (contents != null && contents.name() != null) {
                return contents.name().toString();
            }
            return "missingno";
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

        // Draw popup border
        graphics.fill(x, y, x + FACE_DROPDOWN_WIDTH, y + 1, 0xFF000000);
        graphics.fill(x, y, x + 1, y + popupHeight, 0xFF000000);
        graphics.fill(x, y + popupHeight - 1, x + FACE_DROPDOWN_WIDTH, y + popupHeight, 0xFF000000);
        graphics.fill(x + FACE_DROPDOWN_WIDTH - 1, y, x + FACE_DROPDOWN_WIDTH, y + popupHeight, 0xFF000000);

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

    @Override
    protected void renderLabels(@Nonnull GuiGraphics graphics, int mouseX, int mouseY) {
        // Draw title
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);

        // Draw player inventory label
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }
}