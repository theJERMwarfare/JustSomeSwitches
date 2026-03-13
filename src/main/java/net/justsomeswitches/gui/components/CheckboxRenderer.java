package net.justsomeswitches.gui.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.justsomeswitches.gui.WrenchCopyMenu;

import javax.annotation.Nonnull;

/**
 * Handles vanilla Minecraft checkbox rendering and interaction for the Wrench Copy GUI.
 */
public class CheckboxRenderer {
    private static final int CHECKBOX_SIZE = 16;
    private static final int SPRITE_SIZE = 20;
    private static final int TEXTURE_SIZE = 64;
    private static final ResourceLocation CHECKBOX_TEXTURE = new ResourceLocation("textures/gui/checkbox.png");
    private final CheckboxPosition[] checkboxPositions = {
        new CheckboxPosition(15, 25, 154, 23, "Toggle Block"),     // Toggle Block
        new CheckboxPosition(15, 47, 154, 45, "Toggle Face"),      // Toggle Face
        new CheckboxPosition(15, 69, 154, 68, "Toggle Rotation"),  // Toggle Rotation
        new CheckboxPosition(15, 91, 161, 92, "Indicators"),       // Indicators
        new CheckboxPosition(15, 113, 154, 111, "Base Block"),     // Base Block
        new CheckboxPosition(15, 135, 154, 133, "Base Face"),      // Base Face
        new CheckboxPosition(15, 157, 154, 156, "Base Rotation")   // Base Rotation
    };
    /** Helper class for checkbox positioning. */
    public static class CheckboxPosition {
        public final int checkboxX, checkboxY;
        public final int previewX, previewY;
        public final String label;
        
        public CheckboxPosition(int checkboxX, int checkboxY, int previewX, int previewY, String label) {
            this.checkboxX = checkboxX;
            this.checkboxY = checkboxY;
            this.previewX = previewX;
            this.previewY = previewY;
            this.label = label;
        }
    }
    /** Gets all checkbox positions. */
    public CheckboxPosition[] getCheckboxPositions() {
        return checkboxPositions;
    }
    /** Draws vanilla Minecraft checkbox with current selection state. */
    public void drawVanillaCheckbox(@Nonnull GuiGraphics graphics, int x, int y, int index, 
                                   int mouseX, int mouseY, @Nonnull WrenchCopyMenu menu) {
        boolean isChecked = getCheckboxState(index, menu);
        boolean isHovered = mouseX >= x && mouseX < x + CHECKBOX_SIZE &&
                           mouseY >= y && mouseY < y + CHECKBOX_SIZE;
        float u = isHovered ? SPRITE_SIZE : 0;
        float v = isChecked ? SPRITE_SIZE : 0;
        graphics.blit(CHECKBOX_TEXTURE, x, y, CHECKBOX_SIZE, CHECKBOX_SIZE,
                u, v, SPRITE_SIZE, SPRITE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE);
    }
    /** Gets checkbox state for given index. */
    public boolean getCheckboxState(int index, @Nonnull WrenchCopyMenu menu) {
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
    /** Toggles checkbox state for given index. */
    public void toggleCheckbox(int index, @Nonnull WrenchCopyMenu menu) {
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
    /** Handles mouse click events for checkbox interactions. */
    public boolean handleCheckboxClick(double mouseX, double mouseY, int guiLeft, int guiTop, 
                                      @Nonnull WrenchCopyMenu menu) {
        for (int i = 0; i < checkboxPositions.length; i++) {
            CheckboxPosition pos = checkboxPositions[i];
            int checkboxX = guiLeft + pos.checkboxX;
            int checkboxY = guiTop + pos.checkboxY;
            
            if (mouseX >= checkboxX && mouseX < checkboxX + CHECKBOX_SIZE &&
                mouseY >= checkboxY && mouseY < checkboxY + CHECKBOX_SIZE) {
                toggleCheckbox(i, menu);
                return true;
            }
        }
        return false;
    }
    /** Gets the checkbox size constant. */
    public static int getCheckboxSize() {
        return CHECKBOX_SIZE;
    }
}