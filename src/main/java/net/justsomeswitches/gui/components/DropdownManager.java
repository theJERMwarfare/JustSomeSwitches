package net.justsomeswitches.gui.components;

import net.justsomeswitches.blockentity.SwitchBlockEntity;
import net.justsomeswitches.gui.FaceSelectionData;
import net.justsomeswitches.gui.SwitchesTextureMenu;
import net.justsomeswitches.util.TextureRotation;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Manages all dropdown UI elements including face selection, rotation,
 * power mode, state management, rendering, and interaction handling.
 */
public class DropdownManager {
    
    private static final int FACE_DROPDOWN_WIDTH = 46;
    private static final int FACE_DROPDOWN_HEIGHT = 12;
    private static final int ROTATION_DROPDOWN_WIDTH = 31;
    private static final int ROTATION_DROPDOWN_HEIGHT = 12;
    private static final int POWER_DROPDOWN_WIDTH = 46;
    private static final int POWER_DROPDOWN_HEIGHT = 12;
    private final SwitchesTextureMenu menu;
    private final Font font;
    private boolean showingLeftDropdown = false;
    private boolean showingRightDropdown = false;
    private boolean showingPowerDropdown = false;
    private boolean showingLeftRotationDropdown = false;
    private boolean showingRightRotationDropdown = false;
    
    /**
     * Constructs a new dropdown manager.
     * 
     * @param menu the texture menu
     * @param font the font renderer
     */
    public DropdownManager(@Nonnull SwitchesTextureMenu menu, @Nonnull Font font) {
        this.menu = menu;
        this.font = font;
    }
    /**
     * Checks if any dropdown is currently open.
     */
    public boolean isAnyDropdownOpen() {
        return showingLeftDropdown || showingRightDropdown || showingPowerDropdown ||
               showingLeftRotationDropdown || showingRightRotationDropdown;
    }
    /**
     * Closes all open dropdowns.
     */
    public void closeAllDropdowns() {
        showingLeftDropdown = false;
        showingRightDropdown = false;
        showingPowerDropdown = false;
        showingLeftRotationDropdown = false;
        showingRightRotationDropdown = false;
    }
    /** Checks if left face dropdown is open. */
    public boolean isLeftDropdownOpen() {
        return showingLeftDropdown;
    }
    /** Checks if right face dropdown is open. */
    public boolean isRightDropdownOpen() {
        return showingRightDropdown;
    }
    /** Checks if power dropdown is open. */
    public boolean isPowerDropdownOpen() {
        return showingPowerDropdown;
    }
    /** Checks if left rotation dropdown is open. */
    public boolean isLeftRotationDropdownOpen() {
        return showingLeftRotationDropdown;
    }
    /** Checks if right rotation dropdown is open. */
    public boolean isRightRotationDropdownOpen() {
        return showingRightRotationDropdown;
    }
    /** Toggles the left (toggle) face dropdown state. */
    public void toggleLeftDropdown() {
        showingLeftDropdown = !showingLeftDropdown;
        showingRightDropdown = false;
        showingPowerDropdown = false;
        showingLeftRotationDropdown = false;
        showingRightRotationDropdown = false;
    }
    /** Toggles the right (base) face dropdown state. */
    public void toggleRightDropdown() {
        showingRightDropdown = !showingRightDropdown;
        showingLeftDropdown = false;
        showingPowerDropdown = false;
        showingLeftRotationDropdown = false;
        showingRightRotationDropdown = false;
    }
    /** Toggles the power dropdown state. */
    public void togglePowerDropdown() {
        showingPowerDropdown = !showingPowerDropdown;
        showingLeftDropdown = false;
        showingRightDropdown = false;
        showingLeftRotationDropdown = false;
        showingRightRotationDropdown = false;
    }
    /** Toggles the toggle rotation dropdown state. */
    public void toggleToggleRotationDropdown() {
        showingLeftRotationDropdown = !showingLeftRotationDropdown;
        showingLeftDropdown = false;
        showingRightDropdown = false;
        showingPowerDropdown = false;
        showingRightRotationDropdown = false;
    }
    /** Toggles the base rotation dropdown state. */
    public void toggleBaseRotationDropdown() {
        showingRightRotationDropdown = !showingRightRotationDropdown;
        showingLeftDropdown = false;
        showingRightDropdown = false;
        showingPowerDropdown = false;
        showingLeftRotationDropdown = false;
    }
    /** Checks if coordinates are within face dropdown bounds. */
    public boolean isWithinFaceDropdownBounds(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + FACE_DROPDOWN_WIDTH && 
               mouseY >= y && mouseY < y + FACE_DROPDOWN_HEIGHT;
    }
    /** Checks if coordinates are within power dropdown bounds. */
    public boolean isWithinPowerDropdownBounds(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + POWER_DROPDOWN_WIDTH && 
               mouseY >= y && mouseY < y + POWER_DROPDOWN_HEIGHT;
    }
    /** Checks if coordinates are within rotation dropdown bounds. */
    public boolean isWithinRotationDropdownBounds(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + ROTATION_DROPDOWN_WIDTH && 
               mouseY >= y && mouseY < y + ROTATION_DROPDOWN_HEIGHT;
    }
    /** Handles face dropdown option selection. */
    public boolean handleFaceDropdownSelection(double mouseX, double mouseY, int dropdownX, int dropdownY,
                                               @Nonnull FaceSelectionData.RawTextureSelection selection, boolean isLeft) {
        List<String> variables = selection.availableVariables();
        int dropdownHeight = variables.size() * 12;
        
        
        if (mouseX >= dropdownX && mouseX < dropdownX + FACE_DROPDOWN_WIDTH && 
            mouseY >= dropdownY && mouseY < dropdownY + dropdownHeight) {

            for (int i = 0; i < variables.size(); i++) {
                int optionY = dropdownY + (i * 12);
                
                if (mouseY >= optionY && mouseY < optionY + 12) {
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

            return true;
        }
        
        return false;
    }
    /** Handles power dropdown option selection. */
    public boolean handlePowerDropdownSelection(double mouseX, double mouseY, int dropdownX, int dropdownY) {
        SwitchBlockEntity.PowerMode[] modes = SwitchBlockEntity.PowerMode.values();
        int dropdownHeight = modes.length * 12;
        
        // Check if click is in dropdown popup area
        if (mouseX >= dropdownX && mouseX < dropdownX + POWER_DROPDOWN_WIDTH && 
            mouseY >= dropdownY && mouseY < dropdownY + dropdownHeight) {
            
            // Check which option was clicked
            for (int i = 0; i < modes.length; i++) {
                int optionY = dropdownY + (i * 12);
                
                if (mouseY >= optionY && mouseY < optionY + 12) {
                    menu.setPowerMode(modes[i]);
                    showingPowerDropdown = false;
                    return true;
                }
            }
            
            // Click in dropdown area but not on option - still consume event
            return true;
        }
        
        return false;
    }
    /** Handles toggle rotation dropdown option selection. */
    public boolean handleToggleRotationDropdownSelection(double mouseX, double mouseY, int dropdownX, int dropdownY) {
        TextureRotation[] rotations = TextureRotation.values();
        int dropdownHeight = rotations.length * 12;
        
        // Check if click is in dropdown popup area
        if (mouseX >= dropdownX && mouseX < dropdownX + ROTATION_DROPDOWN_WIDTH && 
            mouseY >= dropdownY && mouseY < dropdownY + dropdownHeight) {
            
            // Check which option was clicked
            for (int i = 0; i < rotations.length; i++) {
                int optionY = dropdownY + (i * 12);
                
                if (mouseY >= optionY && mouseY < optionY + 12) {
                    menu.setToggleTextureRotation(rotations[i]);
                    showingLeftRotationDropdown = false;
                    return true;
                }
            }
            
            // Click in dropdown area but not on option - still consume event
            return true;
        }
        
        return false;
    }
    /** Handles base rotation dropdown option selection. */
    public boolean handleBaseRotationDropdownSelection(double mouseX, double mouseY, int dropdownX, int dropdownY) {
        TextureRotation[] rotations = TextureRotation.values();
        int dropdownHeight = rotations.length * 12;
        
        // Check if click is in dropdown popup area
        if (mouseX >= dropdownX && mouseX < dropdownX + ROTATION_DROPDOWN_WIDTH && 
            mouseY >= dropdownY && mouseY < dropdownY + dropdownHeight) {
            
            // Check which option was clicked
            for (int i = 0; i < rotations.length; i++) {
                int optionY = dropdownY + (i * 12);
                
                if (mouseY >= optionY && mouseY < optionY + 12) {
                    menu.setBaseTextureRotation(rotations[i]);
                    showingRightRotationDropdown = false;
                    return true;
                }
            }
            
            // Click in dropdown area but not on option - still consume event
            return true;
        }
        
        return false;
    }
    /** Renders a face selection dropdown button. */
    public void renderFaceDropdownButton(@Nonnull GuiGraphics graphics, int x, int y,
                                        @Nonnull FaceSelectionData.RawTextureSelection selection, boolean isOpen) {

        int bgColor = selection.enabled() ? 0xFFC6C6C6 : 0xFF888888;
        int textColor = selection.enabled() ? 0xFF404040 : 0xFF666666;

        graphics.fill(x, y, x + FACE_DROPDOWN_WIDTH, y + FACE_DROPDOWN_HEIGHT, bgColor);

        if (selection.enabled()) {
            int lightColor = isOpen ? 0xFF555555 : 0xFFFFFFFF;
            int darkColor = isOpen ? 0xFFFFFFFF : 0xFF555555;
            
            graphics.fill(x, y, x + FACE_DROPDOWN_WIDTH, y + 1, lightColor);
            graphics.fill(x, y, x + 1, y + FACE_DROPDOWN_HEIGHT, lightColor);
            graphics.fill(x, y + FACE_DROPDOWN_HEIGHT - 1, x + FACE_DROPDOWN_WIDTH, y + FACE_DROPDOWN_HEIGHT, darkColor);
            graphics.fill(x + FACE_DROPDOWN_WIDTH - 1, y, x + FACE_DROPDOWN_WIDTH, y + FACE_DROPDOWN_HEIGHT, darkColor);
        } else {
            graphics.fill(x, y, x + FACE_DROPDOWN_WIDTH, y + 1, 0xFF666666);
            graphics.fill(x, y, x + 1, y + FACE_DROPDOWN_HEIGHT, 0xFF666666);
            graphics.fill(x, y + FACE_DROPDOWN_HEIGHT - 1, x + FACE_DROPDOWN_WIDTH, y + FACE_DROPDOWN_HEIGHT, 0xFF999999);
            graphics.fill(x + FACE_DROPDOWN_WIDTH - 1, y, x + FACE_DROPDOWN_WIDTH, y + FACE_DROPDOWN_HEIGHT, 0xFF999999);
        }

        if (selection.enabled()) {
            renderDropdownArrow(graphics, x + FACE_DROPDOWN_WIDTH - 10, y + 4, 6, isOpen, 0xFF000000);
        }

        if (selection.enabled()) {
            String displayText = selection.selectedVariable();
            if (displayText.length() > 7) {
                displayText = displayText.substring(0, 7);
            }
            
            graphics.pose().pushPose();
            graphics.pose().scale(0.8f, 0.8f, 1.0f);
            int fontHeight = this.font.lineHeight;
            int centeredY = (int)((y + (FACE_DROPDOWN_HEIGHT - fontHeight) / 2.0 + 2) / 0.8f);
            graphics.drawString(this.font, displayText, (int)((x + 3) / 0.8f), centeredY, textColor, false);
            graphics.pose().popPose();
        }
    }
    /** Renders a rotation dropdown button. */
    public void renderRotationDropdownButton(@Nonnull GuiGraphics graphics, int x, int y,
                                            TextureRotation rotation, boolean isOpen, boolean isEnabled) {
        if (!isEnabled) {
            renderDisabledRotationDropdown(graphics, x, y);
            return;
        }

        graphics.fill(x, y, x + ROTATION_DROPDOWN_WIDTH, y + ROTATION_DROPDOWN_HEIGHT, 0xFFC6C6C6);

        int lightColor = isOpen ? 0xFF555555 : 0xFFFFFFFF;
        int darkColor = isOpen ? 0xFFFFFFFF : 0xFF555555;
        
        graphics.fill(x, y, x + ROTATION_DROPDOWN_WIDTH, y + 1, lightColor);
        graphics.fill(x, y, x + 1, y + ROTATION_DROPDOWN_HEIGHT, lightColor);
        graphics.fill(x, y + ROTATION_DROPDOWN_HEIGHT - 1, x + ROTATION_DROPDOWN_WIDTH, y + ROTATION_DROPDOWN_HEIGHT, darkColor);
        graphics.fill(x + ROTATION_DROPDOWN_WIDTH - 1, y, x + ROTATION_DROPDOWN_WIDTH, y + ROTATION_DROPDOWN_HEIGHT, darkColor);

        renderDropdownArrow(graphics, x + ROTATION_DROPDOWN_WIDTH - 8, y + 4, 4, isOpen, 0xFF000000);

        String displayText = rotation.getDisplayName();
        if (displayText.length() > 6) {
            displayText = displayText.substring(0, 6);
        }
        
        graphics.pose().pushPose();
        graphics.pose().scale(0.70f, 0.70f, 1.0f);
        int fontHeight = this.font.lineHeight;
        int centeredY = (int)((y + (ROTATION_DROPDOWN_HEIGHT - fontHeight) / 2.0 + 2) / 0.70f);
        graphics.drawString(this.font, displayText, (int)((x + 3) / 0.70f), centeredY, 0xFF404040, false);
        graphics.pose().popPose();
    }
    /** Renders a disabled rotation dropdown. */
    private void renderDisabledRotationDropdown(@Nonnull GuiGraphics graphics, int x, int y) {

        graphics.fill(x, y, x + ROTATION_DROPDOWN_WIDTH, y + ROTATION_DROPDOWN_HEIGHT, 0xFF888888);

        graphics.fill(x, y, x + ROTATION_DROPDOWN_WIDTH, y + 1, 0xFF666666);
        graphics.fill(x, y, x + 1, y + ROTATION_DROPDOWN_HEIGHT, 0xFF666666);
        graphics.fill(x, y + ROTATION_DROPDOWN_HEIGHT - 1, x + ROTATION_DROPDOWN_WIDTH, y + ROTATION_DROPDOWN_HEIGHT, 0xFF999999);
        graphics.fill(x + ROTATION_DROPDOWN_WIDTH - 1, y, x + ROTATION_DROPDOWN_WIDTH, y + ROTATION_DROPDOWN_HEIGHT, 0xFF999999);

        renderDropdownArrow(graphics, x + ROTATION_DROPDOWN_WIDTH - 8, y + 4, 4, false, 0xFF666666);
    }
    /** Renders a power mode dropdown button. */
    public void renderPowerDropdownButton(@Nonnull GuiGraphics graphics, int x, int y,
                                         SwitchBlockEntity.PowerMode powerMode, boolean isOpen) {
        // Draw background
        graphics.fill(x, y, x + POWER_DROPDOWN_WIDTH, y + POWER_DROPDOWN_HEIGHT, 0xFFC6C6C6);

        int lightColor = isOpen ? 0xFF555555 : 0xFFFFFFFF;
        int darkColor = isOpen ? 0xFFFFFFFF : 0xFF555555;
        
        graphics.fill(x, y, x + POWER_DROPDOWN_WIDTH, y + 1, lightColor);
        graphics.fill(x, y, x + 1, y + POWER_DROPDOWN_HEIGHT, lightColor);
        graphics.fill(x, y + POWER_DROPDOWN_HEIGHT - 1, x + POWER_DROPDOWN_WIDTH, y + POWER_DROPDOWN_HEIGHT, darkColor);
        graphics.fill(x + POWER_DROPDOWN_WIDTH - 1, y, x + POWER_DROPDOWN_WIDTH, y + POWER_DROPDOWN_HEIGHT, darkColor);
        
        // Draw arrow
        renderDropdownArrow(graphics, x + POWER_DROPDOWN_WIDTH - 10, y + 4, 6, isOpen, 0xFF000000);
        
        // Draw power mode text
        String displayText = formatPowerModeText(powerMode.name());
        graphics.pose().pushPose();
        graphics.pose().scale(0.8f, 0.8f, 1.0f);
        int fontHeight = this.font.lineHeight;
        int centeredY = (int)((y + (POWER_DROPDOWN_HEIGHT - fontHeight) / 2.0 + 2) / 0.8f);
        graphics.drawString(this.font, displayText, (int)((x + 3) / 0.8f), centeredY, 0xFF404040, false);
        graphics.pose().popPose();
    }
    /** Renders a dropdown arrow. */
    private void renderDropdownArrow(@Nonnull GuiGraphics graphics, int x, int y, int width, boolean isOpen, int color) {
        if (isOpen) {
            if (width == 6) {
                graphics.fill(x + 2, y, x + 4, y + 1, color);
                graphics.fill(x + 1, y + 1, x + 5, y + 2, color);
                graphics.fill(x, y + 2, x + 6, y + 3, color);
            } else { // width == 4
                graphics.fill(x + 1, y, x + 3, y + 1, color);
                graphics.fill(x, y + 1, x + 4, y + 2, color);
                graphics.fill(x, y + 2, x + 4, y + 3, color);
            }
        } else {
            if (width == 6) {
                graphics.fill(x, y, x + 6, y + 1, color);
                graphics.fill(x + 1, y + 1, x + 5, y + 2, color);
                graphics.fill(x + 2, y + 2, x + 4, y + 3, color);
            } else { // width == 4
                graphics.fill(x, y, x + 4, y + 1, color);
                graphics.fill(x, y + 1, x + 4, y + 2, color);
                graphics.fill(x + 1, y + 2, x + 3, y + 3, color);
            }
        }
    }
    /** Renders a face selection dropdown popup. */
    public void renderFaceDropdownPopup(@Nonnull GuiGraphics graphics, int x, int y,
                                       @Nonnull FaceSelectionData.RawTextureSelection selection,
                                       int mouseX, int mouseY) {
        List<String> variables = selection.availableVariables();
        int popupHeight = variables.size() * 12;

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 400);

        graphics.fill(x, y, x + FACE_DROPDOWN_WIDTH, y + popupHeight, 0xFFC6C6C6);
        
        // Draw border
        graphics.fill(x, y, x + FACE_DROPDOWN_WIDTH, y + 1, 0xFF000000);
        graphics.fill(x, y, x + 1, y + popupHeight, 0xFF000000);
        graphics.fill(x + FACE_DROPDOWN_WIDTH - 1, y, x + FACE_DROPDOWN_WIDTH, y + popupHeight, 0xFF000000);
        graphics.fill(x, y + popupHeight, x + FACE_DROPDOWN_WIDTH, y + popupHeight + 1, 0xFF000000);

        for (int i = 0; i < variables.size(); i++) {
            String variable = variables.get(i);
            int optionY = y + (i * 12);

            boolean isHovered = mouseX >= x && mouseX < x + FACE_DROPDOWN_WIDTH && 
                               mouseY >= optionY && mouseY < optionY + 12;
            
            if (isHovered) {
                graphics.fill(x + 1, optionY, x + FACE_DROPDOWN_WIDTH - 1, optionY + 12, 0xFFAAAAFF);
            } else if (variable.equals(selection.selectedVariable())) {
                graphics.fill(x + 1, optionY, x + FACE_DROPDOWN_WIDTH - 1, optionY + 12, 0xFF8888FF);
            }

            String displayText = variable;
            if (displayText.length() > 7) {
                displayText = displayText.substring(0, 7);
            }
            
            graphics.pose().pushPose();
            graphics.pose().scale(0.8f, 0.8f, 1.0f);
            int fontHeight = this.font.lineHeight;
            int centeredY = (int)((optionY + (12 - fontHeight) / 2.0 + 2) / 0.8f);
            graphics.drawString(this.font, displayText, (int)((x + 3) / 0.8f), centeredY, 0xFF000000, false);
            graphics.pose().popPose();
        }

        graphics.pose().popPose();
    }
    /** Renders a rotation dropdown popup. */
    public void renderRotationDropdownPopup(@Nonnull GuiGraphics graphics, int x, int y,
                                           TextureRotation currentRotation, int mouseX, int mouseY) {
        TextureRotation[] rotations = TextureRotation.values();
        int popupHeight = rotations.length * 12;
        
        // Elevate z-order
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 400);
        
        // Draw background
        graphics.fill(x, y, x + ROTATION_DROPDOWN_WIDTH, y + popupHeight, 0xFFC6C6C6);
        
        // Draw border
        graphics.fill(x, y, x + ROTATION_DROPDOWN_WIDTH, y + 1, 0xFF000000);
        graphics.fill(x, y, x + 1, y + popupHeight, 0xFF000000);
        graphics.fill(x + ROTATION_DROPDOWN_WIDTH - 1, y, x + ROTATION_DROPDOWN_WIDTH, y + popupHeight, 0xFF000000);
        graphics.fill(x, y + popupHeight, x + ROTATION_DROPDOWN_WIDTH, y + popupHeight + 1, 0xFF000000);
        
        // Draw options
        for (int i = 0; i < rotations.length; i++) {
            TextureRotation rotation = rotations[i];
            int optionY = y + (i * 12);
            
            // Highlight on hover or selection
            boolean isHovered = mouseX >= x && mouseX < x + ROTATION_DROPDOWN_WIDTH && 
                               mouseY >= optionY && mouseY < optionY + 12;
            
            if (isHovered) {
                graphics.fill(x + 1, optionY, x + ROTATION_DROPDOWN_WIDTH - 1, optionY + 12, 0xFFAAAAFF);
            } else if (rotation == currentRotation) {
                graphics.fill(x + 1, optionY, x + ROTATION_DROPDOWN_WIDTH - 1, optionY + 12, 0xFF8888FF);
            }
            
            // Draw text
            String displayText = rotation.getDisplayName();
            if (displayText.length() > 6) {
                displayText = displayText.substring(0, 6);
            }
            
            graphics.pose().pushPose();
            graphics.pose().scale(0.70f, 0.70f, 1.0f);
            int fontHeight = this.font.lineHeight;
            int centeredY = (int)((optionY + (12 - fontHeight) / 2.0 + 2) / 0.70f);
            graphics.drawString(this.font, displayText, (int)((x + 3) / 0.70f), centeredY, 0xFF000000, false);
            graphics.pose().popPose();
        }
        
        // Restore z-order
        graphics.pose().popPose();
    }
    /** Renders a power mode dropdown popup. */
    public void renderPowerDropdownPopup(@Nonnull GuiGraphics graphics, int x, int y,
                                        SwitchBlockEntity.PowerMode currentMode,
                                        int mouseX, int mouseY) {
        SwitchBlockEntity.PowerMode[] modes = SwitchBlockEntity.PowerMode.values();
        int popupHeight = modes.length * 12;
        
        // Elevate z-order
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 400);
        
        // Draw background
        graphics.fill(x, y, x + POWER_DROPDOWN_WIDTH, y + popupHeight, 0xFFC6C6C6);
        
        // Draw border
        graphics.fill(x, y, x + POWER_DROPDOWN_WIDTH, y + 1, 0xFF000000);
        graphics.fill(x, y, x + 1, y + popupHeight, 0xFF000000);
        graphics.fill(x + POWER_DROPDOWN_WIDTH - 1, y, x + POWER_DROPDOWN_WIDTH, y + popupHeight, 0xFF000000);
        graphics.fill(x, y + popupHeight, x + POWER_DROPDOWN_WIDTH, y + popupHeight + 1, 0xFF000000);
        
        // Draw options
        for (int i = 0; i < modes.length; i++) {
            SwitchBlockEntity.PowerMode mode = modes[i];
            int optionY = y + (i * 12);
            
            // Highlight on hover or selection
            boolean isHovered = mouseX >= x && mouseX < x + POWER_DROPDOWN_WIDTH && 
                               mouseY >= optionY && mouseY < optionY + 12;
            
            if (isHovered) {
                graphics.fill(x + 1, optionY, x + POWER_DROPDOWN_WIDTH - 1, optionY + 12, 0xFFAAAAFF);
            } else if (mode == currentMode) {
                graphics.fill(x + 1, optionY, x + POWER_DROPDOWN_WIDTH - 1, optionY + 12, 0xFF8888FF);
            }
            
            // Draw text
            String displayText = formatPowerModeText(mode.name());
            graphics.pose().pushPose();
            graphics.pose().scale(0.8f, 0.8f, 1.0f);
            int fontHeight = this.font.lineHeight;
            int centeredY = (int)((optionY + (12 - fontHeight) / 2.0 + 2) / 0.8f);
            graphics.drawString(this.font, displayText, (int)((x + 3) / 0.8f), centeredY, 0xFF000000, false);
            graphics.pose().popPose();
        }
        
        // Restore z-order
        graphics.pose().popPose();
    }
    /** Formats power mode text for display (lowercase with capital first letter). */
    private String formatPowerModeText(String modeText) {
        if (modeText == null || modeText.isEmpty()) {
            return "default";
        }
        
        String lowercase = modeText.toLowerCase();
        return Character.toUpperCase(lowercase.charAt(0)) + lowercase.substring(1);
    }
}
