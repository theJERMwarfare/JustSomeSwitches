package net.justsomeswitches.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import javax.annotation.Nonnull;

/**
 * Client-side screen for the Switch Texture customization GUI
 * ---
 * Phase 4A: Layout Matching User Design Image
 * Pixel-perfect positioning measured from uploaded image
 */
public class SwitchTextureScreen extends AbstractContainerScreen<SwitchTextureMenu> {

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 176;

    // GUI background texture
    private static final ResourceLocation GUI_BACKGROUND = new ResourceLocation("justsomeswitches", "textures/gui/switch_texture_gui.png");

    // Texture slot positioning - measured from uploaded image
    private static final int LEFT_SLOT_X = 12;    // Left texture slot
    private static final int LEFT_SLOT_Y = 28;    // CORRECTED: Higher position
    private static final int RIGHT_SLOT_X = 145;  // Right texture slot
    private static final int RIGHT_SLOT_Y = 28;   // CORRECTED: Higher position

    // Central preview positioning
    private static final int PREVIEW_CENTER_X = 88;  // Center of 176px
    private static final int PREVIEW_CENTER_Y = 36;  // CORRECTED: Same height as slots
    private static final int PREVIEW_SIZE = 32;

    // Face dropdown positioning - directly under slots
    private static final int LEFT_FACE_X = 17;    // Centered under left slot
    private static final int LEFT_FACE_Y = 53;    // CORRECTED: Directly under slots
    private static final int RIGHT_FACE_X = 121;  // Centered under right slot
    private static final int RIGHT_FACE_Y = 53;   // CORRECTED: Directly under slots
    private static final int FACE_DROPDOWN_WIDTH = 38;
    private static final int FACE_DROPDOWN_HEIGHT = 12;

    // Inverted checkbox positioning - CORRECTED: Properly centered
    private static final int INVERTED_X = 59;     // CORRECTED: Properly centered
    private static final int INVERTED_Y = 54;     // Same level as face dropdowns

    // Apply button positioning - CORRECTED: Much higher
    private static final int APPLY_BUTTON_X = 66;
    private static final int APPLY_BUTTON_Y = 68;  // CORRECTED: Much higher position
    private static final int BUTTON_WIDTH = 48;
    private static final int BUTTON_HEIGHT = 15;

    // Player inventory positioning (standard for 176px)
    private static final int PLAYER_INV_X = 8;    // Standard position
    private static final int PLAYER_INV_Y = 98;
    private static final int HOTBAR_X = 8;        // Standard position
    private static final int HOTBAR_Y = 156;

    // GUI button
    private Button applyButton;

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
        updateButtonState();
    }

    @Override
    public void containerTick() {
        super.containerTick();
        updateButtonState();
    }

    private void onApplyButtonClicked() {
        if (menu.hasValidBlockEntity()) {
            menu.applyTextures();
            updateButtonState();
            System.out.println("Phase 4A Debug: Apply button clicked - textures applied manually");
        }
    }

    private void updateButtonState() {
        if (applyButton != null) {
            applyButton.active = menu.hasValidBlockEntity();
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

        // Draw face selection dropdowns
        drawFaceDropdowns(graphics, guiLeft, guiTop);

        // Draw inversion checkbox
        drawInversionCheckbox(graphics, guiLeft, guiTop);
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
     * Draw connection lines between slots and preview - ADJUSTED from current locations
     */
    private void drawConnectionLines(@Nonnull GuiGraphics graphics, int guiLeft, int guiTop) {
        // Line Y position - moved down 4px from current position
        int lineY = guiTop + 35; // Down 4px from previous position

        // Left connection line - moved right 3px from current, 20px long
        int leftLineStart = guiLeft + LEFT_SLOT_X + 18 + 18; // Right 3px from current position
        int leftLineEnd = leftLineStart + 20; // 20px long
        graphics.fill(leftLineStart, lineY, leftLineEnd, lineY + 1, 0xFF999999);

        // Right connection line - moved left 3px from current, 20px long
        int rightLineEnd = guiLeft + RIGHT_SLOT_X - 17; // Left 3px from current position
        int rightLineStart = rightLineEnd - 20; // 20px long
        graphics.fill(rightLineStart, lineY, rightLineEnd, lineY + 1, 0xFF999999);
    }

    /**
     * Draw face selection dropdowns
     */
    private void drawFaceDropdowns(@Nonnull GuiGraphics graphics, int guiLeft, int guiTop) {
        // Left face dropdown
        drawDropdownButton(graphics, guiLeft + LEFT_FACE_X, guiTop + LEFT_FACE_Y, "Face");

        // Right face dropdown
        drawDropdownButton(graphics, guiLeft + RIGHT_FACE_X, guiTop + RIGHT_FACE_Y, "Face");
    }

    /**
     * Draw individual dropdown button
     */
    private void drawDropdownButton(@Nonnull GuiGraphics graphics, int x, int y, @Nonnull String label) {
        // Draw dropdown background
        graphics.fill(x, y, x + FACE_DROPDOWN_WIDTH, y + FACE_DROPDOWN_HEIGHT, 0xFFC6C6C6);

        // Draw dropdown border (raised appearance)
        graphics.fill(x, y, x + FACE_DROPDOWN_WIDTH, y + 1, 0xFFFFFFFF);
        graphics.fill(x, y, x + 1, y + FACE_DROPDOWN_HEIGHT, 0xFFFFFFFF);
        graphics.fill(x, y + FACE_DROPDOWN_HEIGHT - 1, x + FACE_DROPDOWN_WIDTH, y + FACE_DROPDOWN_HEIGHT, 0xFF555555);
        graphics.fill(x + FACE_DROPDOWN_WIDTH - 1, y, x + FACE_DROPDOWN_WIDTH, y + FACE_DROPDOWN_HEIGHT, 0xFF555555);

        // Draw dropdown arrow
        graphics.fill(x + FACE_DROPDOWN_WIDTH - 8, y + 4, x + FACE_DROPDOWN_WIDTH - 6, y + 5, 0xFF000000);
        graphics.fill(x + FACE_DROPDOWN_WIDTH - 9, y + 5, x + FACE_DROPDOWN_WIDTH - 5, y + 6, 0xFF000000);
        graphics.fill(x + FACE_DROPDOWN_WIDTH - 10, y + 6, x + FACE_DROPDOWN_WIDTH - 4, y + 7, 0xFF000000);

        // Draw label text
        graphics.drawString(this.font, label, x + 2, y + 2, 0x404040, false);
    }

    /**
     * Draw inversion checkbox - CORRECTED: Properly centered
     */
    private void drawInversionCheckbox(@Nonnull GuiGraphics graphics, int guiLeft, int guiTop) {
        int checkboxX = guiLeft + INVERTED_X;
        int checkboxY = guiTop + INVERTED_Y;

        // Draw checkbox
        graphics.fill(checkboxX, checkboxY, checkboxX + 10, checkboxY + 10, 0xFFFFFFFF);
        graphics.fill(checkboxX, checkboxY, checkboxX + 10, checkboxY + 1, 0xFF555555);
        graphics.fill(checkboxX, checkboxY, checkboxX + 1, checkboxY + 10, 0xFF555555);
        graphics.fill(checkboxX, checkboxY + 9, checkboxX + 10, checkboxY + 10, 0xFFDDDDDD);
        graphics.fill(checkboxX + 9, checkboxY, checkboxX + 10, checkboxY + 10, 0xFFDDDDDD);

        // Draw "Inverted" label
        Component label = Component.literal("Inverted");
        graphics.drawString(this.font, label, checkboxX + 15, checkboxY + 1, 0x404040, false);
    }

    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(@Nonnull GuiGraphics graphics, int mouseX, int mouseY) {
        // Draw title
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);

        // Draw the player inventory label
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }
}