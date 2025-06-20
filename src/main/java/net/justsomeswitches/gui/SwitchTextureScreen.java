package net.justsomeswitches.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import javax.annotation.Nonnull;

/**
 * Client-side screen for the Switch Texture customization GUI
 */
public class SwitchTextureScreen extends AbstractContainerScreen<SwitchTextureMenu> {

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 140;  // Slightly taller for better spacing

    public SwitchTextureScreen(@Nonnull SwitchTextureMenu menu, @Nonnull Inventory playerInventory, @Nonnull Component title) {
        super(menu, playerInventory, title);

        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;

        // Improved label positioning with proper spacing
        this.titleLabelX = 8;
        this.titleLabelY = 6;          // Title at top
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 47;     // Space for texture area above
    }

    @Override
    protected void renderBg(@Nonnull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int guiLeft = (this.width - this.imageWidth) / 2;
        int guiTop = (this.height - this.imageHeight) / 2;

        // Draw main background
        graphics.fill(guiLeft, guiTop, guiLeft + this.imageWidth, guiTop + this.imageHeight, 0xFFC6C6C6);

        // Draw border
        graphics.fill(guiLeft, guiTop, guiLeft + this.imageWidth, guiTop + 1, 0xFF000000); // Top
        graphics.fill(guiLeft, guiTop + this.imageHeight - 1, guiLeft + this.imageWidth, guiTop + this.imageHeight, 0xFF000000); // Bottom
        graphics.fill(guiLeft, guiTop, guiLeft + 1, guiTop + this.imageHeight, 0xFF000000); // Left
        graphics.fill(guiLeft + this.imageWidth - 1, guiTop, guiLeft + this.imageWidth, guiTop + this.imageHeight, 0xFF000000); // Right

        // Draw texture slots backgrounds with better positioning
        // Toggle slot - centered better
        int toggleSlotX = guiLeft + 62;
        int toggleSlotY = guiTop + 26;  // Moved down for space after label
        drawSlotBackground(graphics, toggleSlotX, toggleSlotY);

        // Base slot - centered better
        int baseSlotX = guiLeft + 98;
        int baseSlotY = guiTop + 26;    // Moved down for space after label
        drawSlotBackground(graphics, baseSlotX, baseSlotY);

        // Draw player inventory area background
        graphics.fill(guiLeft + 7, guiTop + 57, guiLeft + 169, guiTop + 129, 0xFF8B8B8B);

        // Draw individual player inventory slots
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int x = guiLeft + 8 + col * 18;
                int y = guiTop + 58 + row * 18;  // Adjusted for new height
                drawSlotBackground(graphics, x, y);
            }
        }

        // Draw hotbar slots
        for (int col = 0; col < 9; col++) {
            int x = guiLeft + 8 + col * 18;
            int y = guiTop + 116;  // Adjusted for new height
            drawSlotBackground(graphics, x, y);
        }
    }

    private void drawSlotBackground(@Nonnull GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
        graphics.fill(x, y, x + 18, y + 1, 0xFF373737);
        graphics.fill(x, y, x + 1, y + 18, 0xFF373737);
        graphics.fill(x + 17, y + 1, x + 18, y + 18, 0xFFFFFFFF);
        graphics.fill(x + 1, y + 17, x + 18, y + 18, 0xFFFFFFFF);
    }

    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(@Nonnull GuiGraphics graphics, int mouseX, int mouseY) {
        // Draw title with proper spacing
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);

        // Draw texture slot labels - centered over slots with spacing
        // Toggle label centered over toggle slot
        Component toggleLabel = Component.literal("Toggle:");
        int toggleLabelWidth = this.font.width(toggleLabel);
        int toggleLabelX = 62 + (18 - toggleLabelWidth) / 2;  // Center over 18px slot
        graphics.drawString(this.font, toggleLabel, toggleLabelX, 17, 0x404040, false);

        // Base label centered over base slot
        Component baseLabel = Component.literal("Base:");
        int baseLabelWidth = this.font.width(baseLabel);
        int baseLabelX = 98 + (18 - baseLabelWidth) / 2;  // Center over 18px slot
        graphics.drawString(this.font, baseLabel, baseLabelX, 17, 0x404040, false);

        // Draw the player inventory label
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }
}