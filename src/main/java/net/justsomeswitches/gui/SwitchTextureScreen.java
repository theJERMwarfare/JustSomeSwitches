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
 * Phase 3B Enhancement: Perfectly aligned with vanilla Minecraft appearance
 * FIXED: Restored original working GUI rendering with new auto-apply functionality
 */
public class SwitchTextureScreen extends AbstractContainerScreen<SwitchTextureMenu> {

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 176;

    // Apply button positioning
    private static final int APPLY_BUTTON_X = 63;
    private static final int APPLY_BUTTON_Y = 58;
    private static final int BUTTON_WIDTH = 51;
    private static final int BUTTON_HEIGHT = 20;

    // GUI button
    private Button applyButton;

    public SwitchTextureScreen(@Nonnull SwitchTextureMenu menu, @Nonnull Inventory playerInventory, @Nonnull Component title) {
        super(menu, playerInventory, title);

        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;

        // Fixed label positioning
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
        // Update button state each tick to reflect current slot contents
        updateButtonState();
    }

    private void onApplyButtonClicked() {
        if (menu.hasValidBlockEntity()) {
            menu.applyTextures();
            updateButtonState();
            System.out.println("Phase 3C Debug: Apply button clicked - textures applied manually");
        }
    }

    private void updateButtonState() {
        if (applyButton != null) {
            // Button is always enabled when BlockEntity is valid
            applyButton.active = menu.hasValidBlockEntity();
        }
    }

    @Override
    protected void renderBg(@Nonnull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int guiLeft = (this.width - this.imageWidth) / 2;
        int guiTop = (this.height - this.imageHeight) / 2;

        // Draw vanilla-style background with proper 3-pixel rounded corners
        drawVanillaBackground(graphics, guiLeft, guiTop, this.imageWidth, this.imageHeight);

        // Draw texture slot backgrounds - CORRECTED: moved 1 pixel right and 1 pixel down
        int toggleSlotX = guiLeft + 62;  // Was 61, moved 1 pixel right
        int toggleSlotY = guiTop + 35;   // Was 34, moved 1 pixel down
        drawVanillaSlot(graphics, toggleSlotX, toggleSlotY);

        int baseSlotX = guiLeft + 98;    // Was 97, moved 1 pixel right
        int baseSlotY = guiTop + 35;     // Was 34, moved 1 pixel down
        drawVanillaSlot(graphics, baseSlotX, baseSlotY);

        // Draw inventory panel (3 rows) with square bottom corners - FIXED: moved right 1 and down 1
        int invX = guiLeft + 8;   // Was 7, moved 1 pixel right
        int invY = guiTop + 94;   // Was 93, moved 1 pixel down
        int invWidth = 162;
        int invHeight = 54;
        drawInventoryPanel(graphics, invX, invY, invWidth, invHeight);

        // Draw hotbar panel (1 row) with square top corners - FIXED: moved right 1 and down 1
        int hotbarX = guiLeft + 8;    // Was 7, moved 1 pixel right
        int hotbarY = guiTop + 152;   // Was 151, moved 1 pixel down
        int hotbarWidth = 162;
        int hotbarHeight = 18;
        drawHotbarPanel(graphics, hotbarX, hotbarY, hotbarWidth, hotbarHeight);

        // Draw individual player inventory slots - CORRECTED: moved 1 pixel right and 1 pixel down
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int x = guiLeft + 8 + col * 18;  // Was 7, moved 1 pixel right
                int y = guiTop + 94 + row * 18;  // Was 93, moved 1 pixel down
                drawVanillaSlot(graphics, x, y);
            }
        }

        // Draw individual hotbar slots - CORRECTED: moved 1 pixel right and 1 pixel down
        for (int col = 0; col < 9; col++) {
            int x = guiLeft + 8 + col * 18;   // Was 7, moved 1 pixel right
            int y = guiTop + 152;             // Was 151, moved 1 pixel down
            drawVanillaSlot(graphics, x, y);
        }
    }

    /**
     * Draws vanilla-style GUI background with 3-pixel rounded corners
     */
    private void drawVanillaBackground(@Nonnull GuiGraphics graphics, int x, int y, int width, int height) {
        // Main background fill
        graphics.fill(x, y, x + width, y + height, 0xFFF0F0F0);

        // 3-pixel rounded corners - make corners transparent
        graphics.fill(x, y, x + 3, y + 3, 0xFFC6C6C6);                        // Top-left
        graphics.fill(x + width - 3, y, x + width, y + 3, 0xFFC6C6C6);        // Top-right
        graphics.fill(x, y + height - 3, x + 3, y + height, 0xFFC6C6C6);      // Bottom-left
        graphics.fill(x + width - 3, y + height - 3, x + width, y + height, 0xFFC6C6C6); // Bottom-right

        // Border - light on top/left, dark on bottom/right
        graphics.fill(x + 3, y, x + width - 3, y + 1, 0xFFFFFFFF);            // Top border (light)
        graphics.fill(x, y + 3, x + 1, y + height - 3, 0xFFFFFFFF);           // Left border (light)
        graphics.fill(x + 3, y + height - 1, x + width - 3, y + height, 0xFF555555); // Bottom border (dark)
        graphics.fill(x + width - 1, y + 3, x + width, y + height - 3, 0xFF555555);  // Right border (dark)

        // Corner borders
        graphics.fill(x + 1, y + 1, x + 3, y + 2, 0xFFFFFFFF);                // Top-left light
        graphics.fill(x + 1, y + 1, x + 2, y + 3, 0xFFFFFFFF);
        graphics.fill(x + width - 3, y + 1, x + width - 1, y + 2, 0xFFFFFFFF); // Top-right light
        graphics.fill(x + width - 2, y + 1, x + width - 1, y + 3, 0xFF555555);
        graphics.fill(x + 1, y + height - 2, x + 3, y + height - 1, 0xFFFFFFFF); // Bottom-left light
        graphics.fill(x + 1, y + height - 3, x + 2, y + height - 1, 0xFFFFFFFF);
        graphics.fill(x + width - 3, y + height - 2, x + width - 1, y + height - 1, 0xFF555555); // Bottom-right dark
        graphics.fill(x + width - 2, y + height - 3, x + width - 1, y + height - 1, 0xFF555555);
    }

    /**
     * Draws inventory panel with square bottom corners and rounded top corners
     */
    private void drawInventoryPanel(@Nonnull GuiGraphics graphics, int x, int y, int width, int height) {
        // Panel background
        graphics.fill(x, y, x + width, y + height, 0xFFC6C6C6);

        // Rounded top corners only - make corners transparent
        graphics.fill(x, y, x + 1, y + 1, 0xFFF0F0F0);              // Top-left transparent
        graphics.fill(x + width - 1, y, x + width, y + 1, 0xFFF0F0F0); // Top-right transparent

        // Borders - dark on top/left, light on bottom/right
        graphics.fill(x + 1, y, x + width - 1, y + 1, 0xFF555555);   // Top border (dark)
        graphics.fill(x, y + 1, x + 1, y + height, 0xFF555555);     // Left border (dark)
        graphics.fill(x, y + height - 1, x + width, y + height, 0xFFFFFFFF); // Bottom border (light)
        graphics.fill(x + width - 1, y + 1, x + width, y + height, 0xFFFFFFFF); // Right border (light)
    }

    /**
     * Draws hotbar panel with square top corners and rounded bottom corners
     */
    private void drawHotbarPanel(@Nonnull GuiGraphics graphics, int x, int y, int width, int height) {
        // Panel background
        graphics.fill(x, y, x + width, y + height, 0xFFC6C6C6);

        // Round bottom corners only (top corners stay square)
        graphics.fill(x, y + height - 1, x + 1, y + height, 0xFFF0F0F0);        // Bottom-left transparent
        graphics.fill(x + width - 1, y + height - 1, x + width, y + height, 0xFFF0F0F0); // Bottom-right transparent

        // Borders - dark on top/left, light on bottom/right
        graphics.fill(x, y, x + width, y + 1, 0xFF555555);          // Top border (dark)
        graphics.fill(x, y, x + 1, y + height - 1, 0xFF555555);    // Left border (dark)
        graphics.fill(x + 1, y + height - 1, x + width - 1, y + height, 0xFFFFFFFF); // Bottom border (light)
        graphics.fill(x + width - 1, y, x + width, y + height - 1, 0xFFFFFFFF); // Right border (light)
    }

    /**
     * Draws a single slot with vanilla-style appearance
     */
    private void drawVanillaSlot(@Nonnull GuiGraphics graphics, int x, int y) {
        // Slot background
        graphics.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);

        // Slot borders (recessed look)
        graphics.fill(x, y, x + 18, y + 1, 0xFF373737);              // Top dark
        graphics.fill(x, y, x + 1, y + 18, 0xFF373737);              // Left dark
        graphics.fill(x, y + 17, x + 18, y + 18, 0xFFFFFFFF);        // Bottom light
        graphics.fill(x + 17, y, x + 18, y + 18, 0xFFFFFFFF);        // Right light
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

        // Draw texture slot labels - FIXED: moved up 2 pixels
        Component toggleLabel = Component.literal("Toggle:");
        int toggleLabelWidth = this.font.width(toggleLabel);
        int toggleLabelX = 62 + (18 - toggleLabelWidth) / 2;
        graphics.drawString(this.font, toggleLabel, toggleLabelX, 23, 0x404040, false);  // Was 25, moved up 2 pixels

        Component baseLabel = Component.literal("Base:");
        int baseLabelWidth = this.font.width(baseLabel);
        int baseLabelX = 98 + (18 - baseLabelWidth) / 2;
        graphics.drawString(this.font, baseLabel, baseLabelX, 23, 0x404040, false);      // Was 25, moved up 2 pixels

        // Draw the player inventory label
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }
}