package net.justsomeswitches.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Switch Texture Screen with Raw JSON Variable System
 * CONSOLIDATED: Moved getTextureSprite() method from BlockTextureAnalyzer for better encapsulation
 */
public class SwitchTextureScreen extends AbstractContainerScreen<SwitchTextureMenu> {

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 176;

    // GUI background texture
    private static final ResourceLocation GUI_BACKGROUND = new ResourceLocation("justsomeswitches", "textures/gui/switch_texture_gui.png");

    // Central preview positioning
    private static final int PREVIEW_CENTER_X = 88;
    private static final int PREVIEW_CENTER_Y = 36;

    // Face dropdown positioning
    private static final int LEFT_FACE_X = 17;
    private static final int LEFT_FACE_Y = 53;
    private static final int RIGHT_FACE_X = 121;
    private static final int RIGHT_FACE_Y = 53;
    private static final int FACE_DROPDOWN_WIDTH = 48;
    private static final int FACE_DROPDOWN_HEIGHT = 12;

    // Texture preview positioning
    private static final int LEFT_PREVIEW_X = 17;
    private static final int LEFT_PREVIEW_Y = 67;
    private static final int RIGHT_PREVIEW_X = 121;
    private static final int RIGHT_PREVIEW_Y = 67;
    private static final int PREVIEW_SIZE = 18;

    // Inverted checkbox positioning
    private static final int INVERTED_X = 59;
    private static final int INVERTED_Y = 54;

    // Connection line positioning
    private static final int LEFT_LINE_START = 48;
    private static final int LEFT_LINE_END = 68;
    private static final int RIGHT_LINE_START = 108;
    private static final int RIGHT_LINE_END = 128;
    private static final int LINE_Y = 35;

    // Current dynamic state
    private FaceSelectionData.RawTextureSelection leftTextureSelection = FaceSelectionData.createDisabledSelection();
    private FaceSelectionData.RawTextureSelection rightTextureSelection = FaceSelectionData.createDisabledSelection();
    private boolean checkboxState = false;

    // Dropdown popup management
    private boolean showingLeftDropdown = false;
    private boolean showingRightDropdown = false;

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
     * Update UI state with raw texture selections
     */
    private void updateUIState() {
        // Get current state from menu
        FaceSelectionData.RawTextureSelection newLeftSelection = menu.getToggleTextureSelection();
        FaceSelectionData.RawTextureSelection newRightSelection = menu.getBaseTextureSelection();
        boolean newCheckboxState = menu.isInverted();

        // Update state
        leftTextureSelection = newLeftSelection;
        rightTextureSelection = newRightSelection;
        checkboxState = newCheckboxState;
    }

    /**
     * Enhanced mouse click handling for dropdowns and checkbox
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int guiLeft = (this.width - this.imageWidth) / 2;
        int guiTop = (this.height - this.imageHeight) / 2;

        // Handle dropdown clicks
        if (isWithinBounds(mouseX, mouseY, guiLeft + LEFT_FACE_X, guiTop + LEFT_FACE_Y,
                FACE_DROPDOWN_WIDTH, FACE_DROPDOWN_HEIGHT)) {
            if (leftTextureSelection.isEnabled()) {
                toggleLeftDropdown();
                return true;
            }
        }

        if (isWithinBounds(mouseX, mouseY, guiLeft + RIGHT_FACE_X, guiTop + RIGHT_FACE_Y,
                FACE_DROPDOWN_WIDTH, FACE_DROPDOWN_HEIGHT)) {
            if (rightTextureSelection.isEnabled()) {
                toggleRightDropdown();
                return true;
            }
        }

        // Handle checkbox click
        if (isWithinBounds(mouseX, mouseY, guiLeft + INVERTED_X, guiTop + INVERTED_Y, 10, 10)) {
            toggleInversionState();
            return true;
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
     * Check if coordinates are within specified bounds
     */
    private boolean isWithinBounds(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    /**
     * Toggle left (toggle) dropdown state
     */
    private void toggleLeftDropdown() {
        showingLeftDropdown = !showingLeftDropdown;
        showingRightDropdown = false;
    }

    /**
     * Toggle right (base) dropdown state
     */
    private void toggleRightDropdown() {
        showingRightDropdown = !showingRightDropdown;
        showingLeftDropdown = false;
    }

    /**
     * Handle dropdown selection clicks with raw variables
     */
    private boolean handleDropdownSelection(double mouseX, double mouseY, int guiLeft, int guiTop, boolean isLeft) {
        FaceSelectionData.RawTextureSelection selection = isLeft ? leftTextureSelection : rightTextureSelection;
        List<String> variables = selection.getAvailableVariables();

        int dropdownX = isLeft ? guiLeft + LEFT_FACE_X : guiLeft + RIGHT_FACE_X;
        int dropdownY = (isLeft ? guiTop + LEFT_FACE_Y : guiTop + RIGHT_FACE_Y) + FACE_DROPDOWN_HEIGHT;

        for (int i = 0; i < variables.size(); i++) {
            int optionY = dropdownY + (i * 12);
            if (isWithinBounds(mouseX, mouseY, dropdownX, optionY, FACE_DROPDOWN_WIDTH, 12)) {
                // Selection made - triggers auto-apply
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
     * Toggle inversion checkbox state
     */
    private void toggleInversionState() {
        checkboxState = !checkboxState;
        menu.setInverted(checkboxState);
    }

    @Override
    protected void renderBg(@Nonnull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int guiLeft = (this.width - this.imageWidth) / 2;
        int guiTop = (this.height - this.imageHeight) / 2;

        // Draw GUI background
        graphics.blit(GUI_BACKGROUND, guiLeft, guiTop + 4, 0, 0, this.imageWidth, this.imageHeight, 256, 256);

        // Draw central preview area
        drawPreviewPlaceholder(graphics, guiLeft, guiTop);

        // Draw connection lines
        drawConnectionLines(graphics, guiLeft, guiTop);

        // Draw face selection dropdowns
        drawRawVariableDropdowns(graphics, guiLeft, guiTop);

        // Draw texture previews
        drawTexturePreview(graphics, guiLeft, guiTop);

        // Draw inversion checkbox
        drawInversionCheckbox(graphics, guiLeft, guiTop);
    }

    /**
     * Draw preview placeholder text
     */
    private void drawPreviewPlaceholder(@Nonnull GuiGraphics graphics, int guiLeft, int guiTop) {
        int centerX = guiLeft + PREVIEW_CENTER_X;
        int centerY = guiTop + PREVIEW_CENTER_Y;

        Component previewText = Component.literal("Auto-Apply");
        int textWidth = this.font.width(previewText);
        int textX = centerX - textWidth / 2;
        int textY = centerY - 4;
        graphics.drawString(this.font, previewText, textX, textY, 0x404040, false);
    }

    /**
     * Draw connection lines between slots and preview
     */
    private void drawConnectionLines(@Nonnull GuiGraphics graphics, int guiLeft, int guiTop) {
        int lineY = guiTop + LINE_Y;

        // Left connection line
        graphics.fill(guiLeft + LEFT_LINE_START, lineY, guiLeft + LEFT_LINE_END, lineY + 1, 0xFF999999);

        // Right connection line
        graphics.fill(guiLeft + RIGHT_LINE_START, lineY, guiLeft + RIGHT_LINE_END, lineY + 1, 0xFF999999);
    }

    /**
     * Draw raw variable dropdowns with dynamic states
     */
    private void drawRawVariableDropdowns(@Nonnull GuiGraphics graphics, int guiLeft, int guiTop) {
        // Left (toggle) variable dropdown
        drawRawVariableDropdownButton(graphics, guiLeft + LEFT_FACE_X, guiTop + LEFT_FACE_Y,
                leftTextureSelection, showingLeftDropdown);

        // Right (base) variable dropdown
        drawRawVariableDropdownButton(graphics, guiLeft + RIGHT_FACE_X, guiTop + RIGHT_FACE_Y,
                rightTextureSelection, showingRightDropdown);
    }

    /**
     * Draw raw variable dropdown button
     */
    private void drawRawVariableDropdownButton(@Nonnull GuiGraphics graphics, int x, int y,
                                               @Nonnull FaceSelectionData.RawTextureSelection selection, boolean isOpen) {
        // Determine colors based on state
        int bgColor = selection.isEnabled() ? 0xFFC6C6C6 : 0xFF888888;
        int textColor = selection.isEnabled() ? 0xFF404040 : 0xFF666666;

        // Draw dropdown background
        graphics.fill(x, y, x + FACE_DROPDOWN_WIDTH, y + FACE_DROPDOWN_HEIGHT, bgColor);

        // Draw dropdown border
        if (selection.isEnabled()) {
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
        if (selection.isEnabled()) {
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

        // Draw current selection (raw JSON variable name)
        String displayText = selection.isEnabled() ? selection.getSelectedVariable() : "Variable";

        // Truncate text if too long
        if (displayText.length() > 6) {
            displayText = displayText.substring(0, 6);
        }

        graphics.drawString(this.font, displayText, x + 2, y + 2, textColor, false);
    }

    /**
     * Draw texture previews under dropdowns
     */
    private void drawTexturePreview(@Nonnull GuiGraphics graphics, int guiLeft, int guiTop) {
        // Draw left (toggle) texture preview
        if (leftTextureSelection.hasPreview()) {
            drawTexturePreviewBox(graphics, guiLeft + LEFT_PREVIEW_X, guiTop + LEFT_PREVIEW_Y,
                    leftTextureSelection.getPreviewTexture());
        }

        // Draw right (base) texture preview
        if (rightTextureSelection.hasPreview()) {
            drawTexturePreviewBox(graphics, guiLeft + RIGHT_PREVIEW_X, guiTop + RIGHT_PREVIEW_Y,
                    rightTextureSelection.getPreviewTexture());
        }
    }

    /**
     * Draw individual texture preview box
     */
    private void drawTexturePreviewBox(@Nonnull GuiGraphics graphics, int x, int y, @Nonnull String texturePath) {
        try {
            // Get texture sprite (moved from BlockTextureAnalyzer)
            TextureAtlasSprite sprite = getTextureSprite(texturePath);

            if (sprite != null && !sprite.contents().name().toString().contains("missingno")) {
                // Draw preview background
                graphics.fill(x - 1, y - 1, x + PREVIEW_SIZE + 1, y + PREVIEW_SIZE + 1, 0xFF000000);
                graphics.fill(x, y, x + PREVIEW_SIZE, y + PREVIEW_SIZE, 0xFFFFFFFF);

                // Draw texture sprite
                graphics.blit(x, y, 0, PREVIEW_SIZE, PREVIEW_SIZE, sprite);
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
     * Get texture sprite for preview rendering (moved from BlockTextureAnalyzer)
     * Provides texture sprite loading with fallback patterns for GUI preview system.
     */
    @Nullable
    private TextureAtlasSprite getTextureSprite(@Nonnull String texturePath) {
        try {
            ResourceLocation textureLocation = new ResourceLocation(texturePath);
            TextureAtlasSprite sprite = Minecraft.getInstance()
                    .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                    .apply(textureLocation);

            if (sprite != null && !sprite.contents().name().toString().contains("missingno")) {
                return sprite;
            }

            // Try fallback patterns for face-specific textures
            if (texturePath.contains("_top") || texturePath.contains("_side") || texturePath.contains("_front")) {
                String basePath = texturePath.replaceAll("_(top|side|front)$", "");
                ResourceLocation fallbackLocation = new ResourceLocation(basePath);
                TextureAtlasSprite fallbackSprite = Minecraft.getInstance()
                        .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                        .apply(fallbackLocation);

                if (fallbackSprite != null && !fallbackSprite.contents().name().toString().contains("missingno")) {
                    return fallbackSprite;
                }
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Draw inversion checkbox
     */
    private void drawInversionCheckbox(@Nonnull GuiGraphics graphics, int guiLeft, int guiTop) {
        int checkboxX = guiLeft + INVERTED_X;
        int checkboxY = guiTop + INVERTED_Y;

        // Draw checkbox background
        graphics.fill(checkboxX, checkboxY, checkboxX + 10, checkboxY + 10, 0xFFFFFFFF);

        // Draw checkbox border
        graphics.fill(checkboxX, checkboxY, checkboxX + 10, checkboxY + 1, 0xFF555555);
        graphics.fill(checkboxX, checkboxY, checkboxX + 1, checkboxY + 10, 0xFF555555);
        graphics.fill(checkboxX, checkboxY + 9, checkboxX + 10, checkboxY + 10, 0xFFDDDDDD);
        graphics.fill(checkboxX + 9, checkboxY, checkboxX + 10, checkboxY + 10, 0xFFDDDDDD);

        // Draw checkmark if checked
        if (checkboxState) {
            graphics.fill(checkboxX + 2, checkboxY + 5, checkboxX + 3, checkboxY + 6, 0xFF000000);
            graphics.fill(checkboxX + 3, checkboxY + 6, checkboxX + 4, checkboxY + 7, 0xFF000000);
            graphics.fill(checkboxX + 4, checkboxY + 4, checkboxX + 5, checkboxY + 5, 0xFF000000);
            graphics.fill(checkboxX + 5, checkboxY + 3, checkboxX + 6, checkboxY + 4, 0xFF000000);
            graphics.fill(checkboxX + 6, checkboxY + 2, checkboxX + 7, checkboxY + 3, 0xFF000000);
            graphics.fill(checkboxX + 7, checkboxY + 1, checkboxX + 8, checkboxY + 2, 0xFF000000);
        }

        // Draw "Inverted" label
        Component label = Component.literal("Inverted");
        graphics.drawString(this.font, label, checkboxX + 15, checkboxY + 1, 0x404040, false);
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
            drawRawVariableDropdownPopup(graphics, guiLeft + LEFT_FACE_X, guiTop + LEFT_FACE_Y + FACE_DROPDOWN_HEIGHT, leftTextureSelection);
        }

        if (showingRightDropdown) {
            drawRawVariableDropdownPopup(graphics, guiLeft + RIGHT_FACE_X, guiTop + RIGHT_FACE_Y + FACE_DROPDOWN_HEIGHT, rightTextureSelection);
        }
    }

    /**
     * Draw raw variable dropdown popup menu
     */
    private void drawRawVariableDropdownPopup(@Nonnull GuiGraphics graphics, int x, int y,
                                              @Nonnull FaceSelectionData.RawTextureSelection selection) {
        List<String> variables = selection.getAvailableVariables();
        int popupHeight = variables.size() * 12;

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
            if (variable.equals(selection.getSelectedVariable())) {
                graphics.fill(x + 1, optionY, x + FACE_DROPDOWN_WIDTH - 1, optionY + 12, 0xFF8888FF);
            }

            // Draw variable name (raw JSON variable, no modification)
            String displayText = variable;
            if (displayText.length() > 6) {
                displayText = displayText.substring(0, 6);
            }
            graphics.drawString(this.font, displayText, x + 2, optionY + 2, 0xFF000000, false);
        }
    }

    @Override
    protected void renderLabels(@Nonnull GuiGraphics graphics, int mouseX, int mouseY) {
        // Draw title
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);

        // Draw player inventory label
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }
}