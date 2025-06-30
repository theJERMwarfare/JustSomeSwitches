package net.justsomeswitches.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.justsomeswitches.util.BlockTextureAnalyzer;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Enhanced client-side screen for the Switch Texture customization GUI
 * ---
 * Phase 4B: Silent operation and fixed face selection persistence
 */
public class SwitchTextureScreen extends AbstractContainerScreen<SwitchTextureMenu> {

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 176;

    // GUI background texture
    private static final ResourceLocation GUI_BACKGROUND = new ResourceLocation("justsomeswitches", "textures/gui/switch_texture_gui.png");

    // Central preview positioning
    private static final int PREVIEW_CENTER_X = 88;  // Center of 176px
    private static final int PREVIEW_CENTER_Y = 36;  // Same height as slots

    // Face dropdown positioning - directly under slots
    private static final int LEFT_FACE_X = 17;    // Centered under left slot
    private static final int LEFT_FACE_Y = 53;    // Directly under slots
    private static final int RIGHT_FACE_X = 121;  // Centered under right slot
    private static final int RIGHT_FACE_Y = 53;   // Directly under slots
    private static final int FACE_DROPDOWN_WIDTH = 48;  // Increased width for better text display
    private static final int FACE_DROPDOWN_HEIGHT = 12;

    // Texture preview positioning (18x18px under dropdowns)
    private static final int LEFT_PREVIEW_X = 17;
    private static final int LEFT_PREVIEW_Y = 67;  // Under left dropdown
    private static final int RIGHT_PREVIEW_X = 121;
    private static final int RIGHT_PREVIEW_Y = 67; // Under right dropdown
    private static final int PREVIEW_SIZE = 18;

    // Inverted checkbox positioning
    private static final int INVERTED_X = 59;     // Centered
    private static final int INVERTED_Y = 54;     // Same level as face dropdowns

    // Apply button positioning
    private static final int APPLY_BUTTON_X = 66;
    private static final int APPLY_BUTTON_Y = 68;  // Higher position
    private static final int BUTTON_WIDTH = 48;
    private static final int BUTTON_HEIGHT = 15;

    // Connection line positioning - FIXED: Shifted left by 20px
    private static final int LEFT_LINE_START = 48;   // Shifted left from 68
    private static final int LEFT_LINE_END = 68;     // Shifted left from 88
    private static final int RIGHT_LINE_START = 108; // Shifted left from 128
    private static final int RIGHT_LINE_END = 128;   // Shifted left from 148
    private static final int LINE_Y = 35;

    // GUI components
    private Button applyButton;

    // OPTIMIZED: Cache state to prevent unnecessary updates
    private FaceSelectionData.DropdownState lastLeftDropdownState = FaceSelectionData.createDisabledState();
    private FaceSelectionData.DropdownState lastRightDropdownState = FaceSelectionData.createDisabledState();
    private boolean lastCheckboxState = false;
    private boolean lastApplyButtonState = false;

    // Current dynamic state tracking
    private FaceSelectionData.DropdownState leftDropdownState = FaceSelectionData.createDisabledState();
    private FaceSelectionData.DropdownState rightDropdownState = FaceSelectionData.createDisabledState();
    private boolean checkboxState = false;

    // Dropdown popup management
    private boolean showingLeftDropdown = false;
    private boolean showingRightDropdown = false;

    public SwitchTextureScreen(@Nonnull SwitchTextureMenu menu, @Nonnull Inventory playerInventory, @Nonnull Component title) {
        super(menu, playerInventory, title);

        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;

        this.titleLabelX = 8;
        this.titleLabelY = 10;  // Moved down 4px (was 6)
        this.inventoryLabelX = 8;   // Standard position
        this.inventoryLabelY = 86;  // Moved down 1px from current (was 85)
    }

    @Override
    protected void init() {
        super.init();

        int guiLeft = (this.width - this.imageWidth) / 2;
        int guiTop = (this.height - this.imageHeight) / 2;

        // Create Apply button - positioned with updated coordinates
        applyButton = Button.builder(
                        Component.literal("Apply"),
                        button -> onApplyButtonClicked()
                )
                .bounds(guiLeft + APPLY_BUTTON_X, guiTop + APPLY_BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();

        addRenderableWidget(applyButton);
        updateUIState();
    }

    @Override
    public void containerTick() {
        super.containerTick();
        updateUIState();
    }

    /**
     * OPTIMIZED: UI state update with change detection to prevent spam
     */
    private void updateUIState() {
        // Get current state
        FaceSelectionData.DropdownState newLeftState = menu.getToggleDropdownState();
        FaceSelectionData.DropdownState newRightState = menu.getBaseDropdownState();
        boolean newCheckboxState = menu.isInverted();
        boolean newApplyButtonState = menu.hasValidBlockEntity();

        // Only update if something actually changed
        boolean stateChanged = false;

        if (!dropdownStatesEqual(leftDropdownState, newLeftState)) {
            leftDropdownState = newLeftState;
            lastLeftDropdownState = newLeftState;
            stateChanged = true;
        }

        if (!dropdownStatesEqual(rightDropdownState, newRightState)) {
            rightDropdownState = newRightState;
            lastRightDropdownState = newRightState;
            stateChanged = true;
        }

        if (checkboxState != newCheckboxState) {
            checkboxState = newCheckboxState;
            lastCheckboxState = newCheckboxState;
            stateChanged = true;
        }

        if (applyButton != null && applyButton.active != newApplyButtonState) {
            applyButton.active = newApplyButtonState;
            lastApplyButtonState = newApplyButtonState;
            stateChanged = true;
        }

        // Only log if debug is needed and state actually changed
        // (Removed debug output for silent operation)
    }

    /**
     * Helper method to compare dropdown states
     */
    private boolean dropdownStatesEqual(FaceSelectionData.DropdownState state1, FaceSelectionData.DropdownState state2) {
        if (state1 == null || state2 == null) return state1 == state2;

        return state1.isEnabled() == state2.isEnabled() &&
                state1.getSelectedOption() == state2.getSelectedOption() &&
                state1.getAvailableOptions().equals(state2.getAvailableOptions());
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
            if (leftDropdownState.isEnabled()) {
                toggleLeftDropdown();
                return true;
            }
        }

        if (isWithinBounds(mouseX, mouseY, guiLeft + RIGHT_FACE_X, guiTop + RIGHT_FACE_Y,
                FACE_DROPDOWN_WIDTH, FACE_DROPDOWN_HEIGHT)) {
            if (rightDropdownState.isEnabled()) {
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
        showingRightDropdown = false; // Close other dropdown
    }

    /**
     * Toggle right (base) dropdown state
     */
    private void toggleRightDropdown() {
        showingRightDropdown = !showingRightDropdown;
        showingLeftDropdown = false; // Close other dropdown
    }

    /**
     * Handle dropdown selection clicks
     */
    private boolean handleDropdownSelection(double mouseX, double mouseY, int guiLeft, int guiTop, boolean isLeft) {
        FaceSelectionData.DropdownState dropdownState = isLeft ? leftDropdownState : rightDropdownState;
        List<FaceSelectionData.FaceOption> options = dropdownState.getAvailableOptions();

        int dropdownX = isLeft ? guiLeft + LEFT_FACE_X : guiLeft + RIGHT_FACE_X;
        int dropdownY = (isLeft ? guiTop + LEFT_FACE_Y : guiTop + RIGHT_FACE_Y) + FACE_DROPDOWN_HEIGHT;

        for (int i = 0; i < options.size(); i++) {
            int optionY = dropdownY + (i * 12);
            if (isWithinBounds(mouseX, mouseY, dropdownX, optionY, FACE_DROPDOWN_WIDTH, 12)) {
                // Selection made
                FaceSelectionData.FaceOption selectedOption = options.get(i);

                if (isLeft) {
                    menu.setToggleFaceSelection(selectedOption);
                    showingLeftDropdown = false;
                } else {
                    menu.setBaseFaceSelection(selectedOption);
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

    private void onApplyButtonClicked() {
        if (menu.hasValidBlockEntity()) {
            menu.applyTextures();
            updateUIState();
        }
    }

    @Override
    protected void renderBg(@Nonnull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int guiLeft = (this.width - this.imageWidth) / 2;
        int guiTop = (this.height - this.imageHeight) / 2;

        // Draw the GUI background image moved down 4px
        graphics.blit(GUI_BACKGROUND, guiLeft, guiTop + 4, 0, 0, this.imageWidth, this.imageHeight, 256, 256);

        // Draw central preview area placeholder text
        drawPreviewPlaceholder(graphics, guiLeft, guiTop);

        // Draw connection lines
        drawConnectionLines(graphics, guiLeft, guiTop);

        // Draw enhanced face selection dropdowns
        drawEnhancedFaceDropdowns(graphics, guiLeft, guiTop);

        // Draw texture previews
        drawTexturePreview(graphics, guiLeft, guiTop);

        // Draw enhanced inversion checkbox
        drawEnhancedInversionCheckbox(graphics, guiLeft, guiTop);
    }

    /**
     * Draw preview placeholder text (slot background now in image)
     */
    private void drawPreviewPlaceholder(@Nonnull GuiGraphics graphics, int guiLeft, int guiTop) {
        int centerX = guiLeft + PREVIEW_CENTER_X;
        int centerY = guiTop + PREVIEW_CENTER_Y;

        // Just draw placeholder text - slot background is in the image
        Component previewText = Component.literal("Preview");
        int textWidth = this.font.width(previewText);
        int textX = centerX - textWidth / 2;
        int textY = centerY - 4;
        graphics.drawString(this.font, previewText, textX, textY, 0x404040, false);
    }

    /**
     * Draw connection lines between slots and preview
     */
    private void drawConnectionLines(@Nonnull GuiGraphics graphics, int guiLeft, int guiTop) {
        // Line Y position - moved down 4px from current position
        int lineY = guiTop + LINE_Y;

        // Left connection line
        graphics.fill(guiLeft + LEFT_LINE_START, lineY, guiLeft + LEFT_LINE_END, lineY + 1, 0xFF999999);

        // Right connection line
        graphics.fill(guiLeft + RIGHT_LINE_START, lineY, guiLeft + RIGHT_LINE_END, lineY + 1, 0xFF999999);
    }

    /**
     * Draw enhanced face selection dropdowns with dynamic states
     */
    private void drawEnhancedFaceDropdowns(@Nonnull GuiGraphics graphics, int guiLeft, int guiTop) {
        // Left (toggle) face dropdown
        drawEnhancedDropdownButton(graphics, guiLeft + LEFT_FACE_X, guiTop + LEFT_FACE_Y,
                leftDropdownState, showingLeftDropdown);

        // Right (base) face dropdown
        drawEnhancedDropdownButton(graphics, guiLeft + RIGHT_FACE_X, guiTop + RIGHT_FACE_Y,
                rightDropdownState, showingRightDropdown);
    }

    /**
     * Draw enhanced dropdown button with dynamic state
     */
    private void drawEnhancedDropdownButton(@Nonnull GuiGraphics graphics, int x, int y,
                                            @Nonnull FaceSelectionData.DropdownState state, boolean isOpen) {
        // Determine colors based on state
        int bgColor = state.isEnabled() ? 0xFFC6C6C6 : 0xFF888888;  // Grayed out if disabled
        int textColor = state.isEnabled() ? 0xFF404040 : 0xFF666666; // Dimmed text if disabled

        // Draw dropdown background
        graphics.fill(x, y, x + FACE_DROPDOWN_WIDTH, y + FACE_DROPDOWN_HEIGHT, bgColor);

        // Draw dropdown border (raised/lowered appearance based on state)
        if (state.isEnabled()) {
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
        if (state.isEnabled()) {
            int arrowColor = 0xFF000000;
            int arrowX = x + FACE_DROPDOWN_WIDTH - 10;
            int arrowY = y + 4;

            if (!isOpen) {
                // Down arrow (closed state)
                graphics.fill(arrowX + 2, arrowY, arrowX + 4, arrowY + 1, arrowColor);     // Top line
                graphics.fill(arrowX + 1, arrowY + 1, arrowX + 5, arrowY + 2, arrowColor); // Middle line
                graphics.fill(arrowX, arrowY + 2, arrowX + 6, arrowY + 3, arrowColor);     // Bottom line
            } else {
                // Up arrow (open state)
                graphics.fill(arrowX, arrowY + 2, arrowX + 6, arrowY + 3, arrowColor);     // Bottom line
                graphics.fill(arrowX + 1, arrowY + 1, arrowX + 5, arrowY + 2, arrowColor); // Middle line
                graphics.fill(arrowX + 2, arrowY, arrowX + 4, arrowY + 1, arrowColor);     // Top line
            }
        }

        // Draw current selection or "Face" label
        String displayText = state.isEnabled() ?
                state.getSelectedOption().getDisplayName() : "Face";

        // Truncate text if too long
        if (displayText.length() > 6) {
            displayText = displayText.substring(0, 6);
        }

        graphics.drawString(this.font, displayText, x + 2, y + 2, textColor, false);
    }

    /**
     * Draw 18x18px texture previews under dropdowns
     */
    private void drawTexturePreview(@Nonnull GuiGraphics graphics, int guiLeft, int guiTop) {
        // Draw left (toggle) texture preview
        if (leftDropdownState.hasPreview()) {
            drawTexturePreviewBox(graphics, guiLeft + LEFT_PREVIEW_X, guiTop + LEFT_PREVIEW_Y,
                    leftDropdownState.getPreviewTexture());
        }

        // Draw right (base) texture preview
        if (rightDropdownState.hasPreview()) {
            drawTexturePreviewBox(graphics, guiLeft + RIGHT_PREVIEW_X, guiTop + RIGHT_PREVIEW_Y,
                    rightDropdownState.getPreviewTexture());
        }
    }

    /**
     * Draw individual 18x18px texture preview box
     */
    private void drawTexturePreviewBox(@Nonnull GuiGraphics graphics, int x, int y, @Nonnull String texturePath) {
        try {
            // Get texture sprite
            TextureAtlasSprite sprite = BlockTextureAnalyzer.getTextureSprite(texturePath);

            if (sprite != null && !sprite.contents().name().toString().contains("missingno")) {
                // Draw preview background
                graphics.fill(x - 1, y - 1, x + PREVIEW_SIZE + 1, y + PREVIEW_SIZE + 1, 0xFF000000);
                graphics.fill(x, y, x + PREVIEW_SIZE, y + PREVIEW_SIZE, 0xFFFFFFFF);

                // Draw texture sprite scaled to 18x18
                graphics.blit(x, y, 0, PREVIEW_SIZE, PREVIEW_SIZE, sprite);
            } else {
                // Draw error/missing texture indicator
                graphics.fill(x, y, x + PREVIEW_SIZE, y + PREVIEW_SIZE, 0xFFFF00FF); // Magenta for missing
            }
        } catch (Exception e) {
            // Draw error indicator
            graphics.fill(x, y, x + PREVIEW_SIZE, y + PREVIEW_SIZE, 0xFFFF0000); // Red for error
        }
    }

    /**
     * Draw enhanced inversion checkbox with proper state
     */
    private void drawEnhancedInversionCheckbox(@Nonnull GuiGraphics graphics, int guiLeft, int guiTop) {
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
            // Simple checkmark pattern
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

        // Render dropdown popups AFTER everything else for proper z-order
        int guiLeft = (this.width - this.imageWidth) / 2;
        int guiTop = (this.height - this.imageHeight) / 2;

        if (showingLeftDropdown) {
            drawDropdownPopup(graphics, guiLeft + LEFT_FACE_X, guiTop + LEFT_FACE_Y + FACE_DROPDOWN_HEIGHT, leftDropdownState);
        }

        if (showingRightDropdown) {
            drawDropdownPopup(graphics, guiLeft + RIGHT_FACE_X, guiTop + RIGHT_FACE_Y + FACE_DROPDOWN_HEIGHT, rightDropdownState);
        }
    }

    /**
     * Draw dropdown popup menu
     */
    private void drawDropdownPopup(@Nonnull GuiGraphics graphics, int x, int y,
                                   @Nonnull FaceSelectionData.DropdownState state) {
        List<FaceSelectionData.FaceOption> options = state.getAvailableOptions();
        int popupHeight = options.size() * 12;

        // Draw popup background
        graphics.fill(x, y, x + FACE_DROPDOWN_WIDTH, y + popupHeight, 0xFFC6C6C6);

        // Draw popup border
        graphics.fill(x, y, x + FACE_DROPDOWN_WIDTH, y + 1, 0xFF000000);
        graphics.fill(x, y, x + 1, y + popupHeight, 0xFF000000);
        graphics.fill(x, y + popupHeight - 1, x + FACE_DROPDOWN_WIDTH, y + popupHeight, 0xFF000000);
        graphics.fill(x + FACE_DROPDOWN_WIDTH - 1, y, x + FACE_DROPDOWN_WIDTH, y + popupHeight, 0xFF000000);

        // Draw options
        for (int i = 0; i < options.size(); i++) {
            FaceSelectionData.FaceOption option = options.get(i);
            int optionY = y + (i * 12);

            // Highlight selected option
            if (option == state.getSelectedOption()) {
                graphics.fill(x + 1, optionY, x + FACE_DROPDOWN_WIDTH - 1, optionY + 12, 0xFF8888FF);
            }

            // Draw option text
            String optionText = option.getDisplayName();
            if (optionText.length() > 6) {
                optionText = optionText.substring(0, 6);
            }
            graphics.drawString(this.font, optionText, x + 2, optionY + 2, 0xFF000000, false);
        }
    }

    @Override
    protected void renderLabels(@Nonnull GuiGraphics graphics, int mouseX, int mouseY) {
        // Draw title
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);

        // Draw the player inventory label
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }
}