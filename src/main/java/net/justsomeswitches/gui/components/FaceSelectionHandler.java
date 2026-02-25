package net.justsomeswitches.gui.components;

import net.justsomeswitches.blockentity.SwitchBlockEntity;
import net.justsomeswitches.gui.FaceSelectionData;
import net.justsomeswitches.gui.SwitchesTextureMenu;
import net.justsomeswitches.util.TextureRotation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Handles face selection state management including state tracking,
 * item change detection, block removal cleanup, and UI state updates.
 */
public class FaceSelectionHandler {
    private final SwitchesTextureMenu menu;
    private final DropdownManager dropdownManager;
    private FaceSelectionData.RawTextureSelection leftTextureSelection = FaceSelectionData.RawTextureSelection.createDisabled();
    private FaceSelectionData.RawTextureSelection rightTextureSelection = FaceSelectionData.RawTextureSelection.createDisabled();
    private ItemStack previousLeftItem = ItemStack.EMPTY;
    private ItemStack previousRightItem = ItemStack.EMPTY;
    private String previousBaseTexture = null;
    private String previousToggleTexture = null;
    private SwitchBlockEntity.PowerMode previousPowerMode = null;
    /**
     * Creates a new face selection handler.
     */
    public FaceSelectionHandler(@Nonnull SwitchesTextureMenu menu, @Nonnull DropdownManager dropdownManager) {
        this.menu = menu;
        this.dropdownManager = dropdownManager;
    }
    /**
     * Updates face selection state with change detection.
    * Detects block changes, updates selection state, triggers cleanup, and tracks texture changes.
    */
    public void updateUIState() {
        FaceSelectionData.RawTextureSelection newLeftSelection = menu.getToggleTextureSelection();
        FaceSelectionData.RawTextureSelection newRightSelection = menu.getBaseTextureSelection();
        ItemStack currentLeftItem = newLeftSelection.sourceBlock();
        ItemStack currentRightItem = newRightSelection.sourceBlock();
        if (!ItemStack.matches(previousLeftItem, currentLeftItem)) {
            if (currentLeftItem.isEmpty()) {
                handleBlockRemoval(true);
            }
            previousLeftItem = currentLeftItem.copy();
        }
        if (!ItemStack.matches(previousRightItem, currentRightItem)) {
            if (currentRightItem.isEmpty()) {
                handleBlockRemoval(false);
            }
            previousRightItem = currentRightItem.copy();
        }
        leftTextureSelection = newLeftSelection;
        rightTextureSelection = newRightSelection;
        if (!Objects.equals(previousBaseTexture, rightTextureSelection.previewTexture())) {
            previousBaseTexture = rightTextureSelection.previewTexture();
        }
        if (!Objects.equals(previousToggleTexture, leftTextureSelection.previewTexture())) {
            previousToggleTexture = leftTextureSelection.previewTexture();
        }
        if (previousPowerMode != menu.getPowerMode()) {
            previousPowerMode = menu.getPowerMode();
        }
    }
    /**
     * Handles cleanup when texture block is removed (closes dropdowns, resets selection).
     */
    private void handleBlockRemoval(boolean isLeft) {
        dropdownManager.closeAllDropdowns();
        if (isLeft) {
            menu.setToggleTextureVariable("all");
            menu.setToggleTextureRotation(TextureRotation.NORMAL);
        } else {
            menu.setBaseTextureVariable("all");
            menu.setBaseTextureRotation(TextureRotation.NORMAL);
        }
    }
    /** Gets the current left (toggle) texture selection. */
    @Nonnull
    public FaceSelectionData.RawTextureSelection getLeftTextureSelection() {
        return leftTextureSelection;
    }
    /** Gets the current right (base) texture selection. */
    @Nonnull
    public FaceSelectionData.RawTextureSelection getRightTextureSelection() {
        return rightTextureSelection;
    }
    /** Checks if the base texture slot has a block. */
    public boolean hasBaseTextureBlock() {
        return rightTextureSelection.hasPreview() && 
               !rightTextureSelection.sourceBlock().isEmpty();
    }
    /** Checks if the toggle texture slot has a block. */
    public boolean hasToggleTextureBlock() {
        return leftTextureSelection.hasPreview() && 
               !leftTextureSelection.sourceBlock().isEmpty();
    }
}
