package net.justsomeswitches.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import javax.annotation.Nonnull;

/**
 * Client-side screen for the Switch Texture customization GUI
 * ---
 * Phase 4A: Layout Matching User Design Image
 * Positioned to exactly match the uploaded design layout
 */
public class SwitchTextureScreen extends AbstractContainerScreen<SwitchTextureMenu> {

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 176;

    // Phase 4A: Positioning to match user design image
    private static final int LEFT_SLOT_X = 43;   // Positioned inward from edges
    private static final int LEFT_SLOT_Y = 35;   // Aligned with preview
    private static final int RIGHT_SLOT_X = 115; // Positioned inward from edges
    private static final int RIGHT_SLOT_Y = 35;  // Aligned with preview

    // Preview positioning (centered, same height as slots)
    private static final int PREVIEW_CENTER_X = 88; // Exact center
    private static final int PREVIEW_CENTER_Y = 35; // Same as slots
    private static final int PREVIEW_SIZE = 32;

    // Apply button positioning (centered, compact)
    private static final int APPLY_BUTTON_X = 66;
    private static final int APPLY_BUTTON_Y = 75;
    private static final int BUTTON_WIDTH = 44;
    private static final int BUTTON_HEIGHT = 16;

    // Player inventory positioning (original)
    private static final int PLAYER_INV_X = 9;
    private static final int PLAYER_INV_Y = 95;
    private static final int HOTBAR_X = 9;
    private static final int HOTBAR_Y = 153;

    // GUI button
    private Button applyButton;

    public SwitchTextureScreen(@Nonnull SwitchTextureMenu menu, @Nonnull Inventory playerInventory, @Nonnull Component title) {
        super(menu, playerInventory, title);

        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;

        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 82;
    }

    @Override
    protected void init() {
        super.init();

        int guiLeft = (this.width - this.imageWidth) / 2;
        int guiTop = (this.height - this.imageHeight) / 2;

        // Create Apply button
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

        // Draw authentic vanilla-style background
        drawVanillaBackground(graphics, guiLeft, guiTop, this.imageWidth, this.imageHeight);

        // Draw texture slots positioned to match design
        int leftSlotX = guiLeft + LEFT_SLOT_X - 1;
        int leftSlotY = guiTop + LEFT_SLOT_Y - 1;
        drawVanillaSlot(graphics, leftSlotX, leftSlotY);

        int rightSlotX = guiLeft + RIGHT_SLOT_X - 1;
        int rightSlotY = guiTop + RIGHT_SLOT_Y - 1;
        drawVanillaSlot(graphics, rightSlotX, rightSlotY);

        // Draw central preview placeholder (will be updated when preview is implemented)
        drawPreviewPlaceholder(graphics, guiLeft, guiTop);

        // Draw connection lines (may change when preview is implemented)
        drawConnectionLines(graphics, guiLeft, guiTop);

        // Draw face selection dropdown placeholders
        drawFaceDropdownPlaceholders(graphics, guiLeft, guiTop);

        // Draw inversion checkbox placeholder
        drawInversionCheckboxPlaceholder(graphics, guiLeft, guiTop);

        // Draw inventory panel
        drawInventoryPanel(graphics, guiLeft + PLAYER_INV_X - 1, guiTop + PLAYER_INV_Y - 1);

        // Draw hotbar panel
        drawHotbarPanel(graphics, guiLeft + HOTBAR_X - 1, guiTop + HOTBAR_Y - 1);

        // Draw individual player inventory slots
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int x = guiLeft + PLAYER_INV_X + col * 18 - 1;
                int y = guiTop + PLAYER_INV_Y + row * 18 - 1;
                drawVanillaSlot(graphics, x, y);
            }
        }

        // Draw individual hotbar slots
        for (int col = 0; col < 9; col++) {
            int x = guiLeft + HOTBAR_X + col * 18 - 1;
            int y = guiTop + HOTBAR_Y - 1;
            drawVanillaSlot(graphics, x, y);
        }
    }

    /**
     * Draw central preview placeholder (currently with slot background as shown in design)
     * Note: Will be updated to remove slot background when preview is implemented
     */
    private void drawPreviewPlaceholder(@Nonnull GuiGraphics graphics, int guiLeft, int guiTop) {
        int centerX = guiLeft + PREVIEW_CENTER_X - PREVIEW_SIZE / 2;
        int centerY = guiTop + PREVIEW_CENTER_Y - PREVIEW_SIZE / 2;

        // Draw preview background area (currently with slot background as in design)
        graphics.fill(centerX, centerY, centerX + PREVIEW_SIZE, centerY + PREVIEW_SIZE, 0xFF8B8B8B);

        // Draw preview border (recessed like slots)
        graphics.fill(centerX, centerY, centerX + PREVIEW_SIZE, centerY + 1, 0xFF373737);
        graphics.fill(centerX, centerY, centerX + 1, centerY + PREVIEW_SIZE, 0xFF373737);
        graphics.fill(centerX, centerY + PREVIEW_SIZE - 1, centerX + PREVIEW_SIZE, centerY + PREVIEW_SIZE, 0xFFFFFFFF);
        graphics.fill(centerX + PREVIEW_SIZE - 1, centerY, centerX + PREVIEW_SIZE, centerY + PREVIEW_SIZE, 0xFFFFFFFF);

        // Placeholder text
        Component previewText = Component.literal("Preview");
        int textWidth = this.font.width(previewText);
        int textX = centerX + (PREVIEW_SIZE - textWidth) / 2;
        int textY = centerY + PREVIEW_SIZE / 2 - 4;
        graphics.drawString(this.font, previewText, textX, textY, 0x404040, false);
    }

    /**
     * Draw connection lines (may change when preview is implemented)
     */
    private void drawConnectionLines(@Nonnull GuiGraphics graphics, int guiLeft, int guiTop) {
        // Calculate connection points with 3px gaps
        int leftSlotRightEdge = guiLeft + LEFT_SLOT_X + 18;
        int rightSlotLeftEdge = guiLeft + RIGHT_SLOT_X;
        int previewLeftEdge = guiLeft + PREVIEW_CENTER_X - PREVIEW_SIZE / 2;
        int previewRightEdge = guiLeft + PREVIEW_CENTER_X + PREVIEW_SIZE / 2;
        int lineY = guiTop + LEFT_SLOT_Y + 9; // Center of slots

        // Left connection line
        int leftLineStart = leftSlotRightEdge + 3;
        int leftLineEnd = previewLeftEdge - 3;
        if (leftLineEnd > leftLineStart) {
            graphics.fill(leftLineStart, lineY, leftLineEnd, lineY + 1, 0xFF999999);
        }

        // Right connection line
        int rightLineStart = previewRightEdge + 3;
        int rightLineEnd = rightSlotLeftEdge - 3;
        if (rightLineEnd > rightLineStart) {
            graphics.fill(rightLineStart, lineY, rightLineEnd, lineY + 1, 0xFF999999);
        }
    }

    /**
     * Draw face selection dropdown placeholders
     */
    private void drawFaceDropdownPlaceholders(@Nonnull GuiGraphics graphics, int guiLeft, int guiTop) {
        // Left face dropdown (centered under left slot)
        int leftDropdownX = guiLeft + LEFT_SLOT_X + (18 - 38) / 2;
        int leftDropdownY = guiTop + LEFT_SLOT_Y + 22;
        drawDropdownPlaceholder(graphics, leftDropdownX, leftDropdownY, "Face");

        // Right face dropdown (centered under right slot)
        int rightDropdownX = guiLeft + RIGHT_SLOT_X + (18 - 38) / 2;
        int rightDropdownY = guiTop + RIGHT_SLOT_Y + 22;
        drawDropdownPlaceholder(graphics, rightDropdownX, rightDropdownY, "Face");
    }

    /**
     * Draw individual dropdown placeholder
     */
    private void drawDropdownPlaceholder(@Nonnull GuiGraphics graphics, int x, int y, @Nonnull String label) {
        int width = 38;
        int height = 12;

        // Draw dropdown background
        graphics.fill(x, y, x + width, y + height, 0xFFC6C6C6);

        // Draw dropdown border
        graphics.fill(x, y, x + width, y + 1, 0xFF555555);
        graphics.fill(x, y, x + 1, y + height, 0xFF555555);
        graphics.fill(x, y + height - 1, x + width, y + height, 0xFFFFFFFF);
        graphics.fill(x + width - 1, y, x + width, y + height, 0xFFFFFFFF);

        // Draw dropdown arrow
        graphics.fill(x + width - 8, y + 4, x + width - 6, y + 5, 0xFF000000);
        graphics.fill(x + width - 9, y + 5, x + width - 5, y + 6, 0xFF000000);
        graphics.fill(x + width - 10, y + 6, x + width - 4, y + 7, 0xFF000000);

        // Draw label text
        graphics.drawString(this.font, label, x + 2, y + 2, 0x404040, false);
    }

    /**
     * Draw inversion checkbox placeholder (centered)
     */
    private void drawInversionCheckboxPlaceholder(@Nonnull GuiGraphics graphics, int guiLeft, int guiTop) {
        int checkboxX = guiLeft + PREVIEW_CENTER_X - 25;  // Centered under preview
        int checkboxY = guiTop + PREVIEW_CENTER_Y + 22;   // Same distance as face dropdowns

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

    /**
     * Authentic vanilla-style GUI background with precise corner rounding
     */
    private void drawVanillaBackground(@Nonnull GuiGraphics graphics, int x, int y, int width, int height) {
        // Main background fill
        graphics.fill(x, y, x + width, y + height, 0xFFC6C6C6);

        // Corner rounding for main background BEFORE outline
        // Top left corner: 2px rounding
        graphics.fill(x, y, x + 2, y + 2, 0x00000000);
        // Top right corner: 3px rounding
        graphics.fill(x + width - 3, y, x + width, y + 3, 0x00000000);
        // Bottom right corner: 2px rounding
        graphics.fill(x + width - 2, y + height - 2, x + width, y + height, 0x00000000);
        // Bottom left corner: 3px rounding
        graphics.fill(x, y + height - 3, x + 3, y + height, 0x00000000);

        // 1px solid black outline (with corner rounding)
        // Top edge (with corner rounding)
        graphics.fill(x + 2, y, x + width - 3, y + 1, 0xFF000000);
        // Left edge (with corner rounding)
        graphics.fill(x, y + 2, x + 1, y + height - 3, 0xFF000000);
        // Bottom edge (with corner rounding)
        graphics.fill(x + 3, y + height - 1, x + width - 2, y + height, 0xFF000000);
        // Right edge (with corner rounding)
        graphics.fill(x + width - 1, y + 3, x + width, y + height - 2, 0xFF000000);

        // Corner pixels for black outline
        // Top left corner (2px rounding)
        graphics.fill(x + 1, y + 1, x + 2, y + 2, 0xFF000000);
        // Top right corner (3px rounding)
        graphics.fill(x + width - 2, y + 1, x + width - 1, y + 2, 0xFF000000);
        graphics.fill(x + width - 3, y + 2, x + width - 2, y + 3, 0xFF000000);
        // Bottom right corner (2px rounding)
        graphics.fill(x + width - 2, y + height - 2, x + width - 1, y + height - 1, 0xFF000000);
        // Bottom left corner (3px rounding)
        graphics.fill(x + 1, y + height - 2, x + 2, y + height - 1, 0xFF000000);
        graphics.fill(x + 2, y + height - 3, x + 3, y + height - 2, 0xFF000000);

        // 2px white highlight on top and left edges
        graphics.fill(x + 2, y + 1, x + width - 3, y + 3, 0xFFFFFFFF);
        graphics.fill(x + 1, y + 2, x + 3, y + height - 3, 0xFFFFFFFF);

        // White highlight corner rounding (1px for top-left)
        graphics.fill(x + 1, y + 1, x + 3, y + 2, 0xFFFFFFFF);

        // 2px gray shadow (RGB 85,85,85) on right and bottom edges
        graphics.fill(x + 3, y + height - 3, x + width - 2, y + height - 1, 0xFF555555);
        graphics.fill(x + width - 3, y + 3, x + width - 1, y + height - 2, 0xFF555555);

        // Gray shadow corner rounding (1px for bottom-right)
        graphics.fill(x + width - 3, y + height - 2, x + width - 1, y + height - 1, 0xFF555555);
    }

    /**
     * Draws inventory panel with square bottom corners and rounded top corners
     */
    private void drawInventoryPanel(@Nonnull GuiGraphics graphics, int x, int y) {
        int width = 162;
        int height = 54;

        // Panel background
        graphics.fill(x, y, x + width, y + height, 0xFFC6C6C6);

        // Rounded top corners only
        graphics.fill(x, y, x + 1, y + 1, 0xFFF0F0F0);
        graphics.fill(x + width - 1, y, x + width, y + 1, 0xFFF0F0F0);

        // Borders
        graphics.fill(x + 1, y, x + width - 1, y + 1, 0xFF555555);
        graphics.fill(x, y + 1, x + 1, y + height, 0xFF555555);
        graphics.fill(x, y + height - 1, x + width, y + height, 0xFFFFFFFF);
        graphics.fill(x + width - 1, y + 1, x + width, y + height, 0xFFFFFFFF);
    }

    /**
     * Draws hotbar panel with square top corners and rounded bottom corners
     */
    private void drawHotbarPanel(@Nonnull GuiGraphics graphics, int x, int y) {
        int width = 162;
        int height = 18;

        // Panel background
        graphics.fill(x, y, x + width, y + height, 0xFFC6C6C6);

        // Round bottom corners only
        graphics.fill(x, y + height - 1, x + 1, y + height, 0xFFF0F0F0);
        graphics.fill(x + width - 1, y + height - 1, x + width, y + height, 0xFFF0F0F0);

        // Borders
        graphics.fill(x, y, x + width, y + 1, 0xFF555555);
        graphics.fill(x, y, x + 1, y + height - 1, 0xFF555555);
        graphics.fill(x + 1, y + height - 1, x + width - 1, y + height, 0xFFFFFFFF);
        graphics.fill(x + width - 1, y, x + width, y + height - 1, 0xFFFFFFFF);
    }

    /**
     * Draws a single slot with vanilla-style appearance
     */
    private void drawVanillaSlot(@Nonnull GuiGraphics graphics, int x, int y) {
        // Slot background
        graphics.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);

        // Slot borders (recessed look)
        graphics.fill(x, y, x + 18, y + 1, 0xFF373737);
        graphics.fill(x, y, x + 1, y + 18, 0xFF373737);
        graphics.fill(x, y + 17, x + 18, y + 18, 0xFFFFFFFF);
        graphics.fill(x + 17, y, x + 18, y + 18, 0xFFFFFFFF);
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