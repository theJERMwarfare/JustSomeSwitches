package net.justsomeswitches.gui.components;

import net.justsomeswitches.gui.BrushCopyMenu;
import net.justsomeswitches.network.NetworkHandler;
import net.minecraft.core.BlockPos;

import javax.annotation.Nonnull;

/**
 * Handles copy action logic for the Brush Copy GUI.
 */
public class CopyActionHandler {
    /** Handles Select All button click. */
    public void handleSelectAll(@Nonnull BrushCopyMenu menu) {
        menu.setAllCopySelections(true);
    }
    /** Handles Clear All button click. */
    public void handleClearAll(@Nonnull BrushCopyMenu menu) {
        menu.setAllCopySelections(false);
    }
    /** Handles Copy Selected button click. */
    public void handleCopySelected(@Nonnull BrushCopyMenu menu, @Nonnull Runnable onClose) {
        boolean hasSelection = menu.getCopyToggleBlock() || menu.getCopyToggleFace() || 
                              menu.getCopyToggleRotation() || menu.getCopyIndicators() ||
                              menu.getCopyBaseBlock() || menu.getCopyBaseFace() || 
                              menu.getCopyBaseRotation();
        if (!hasSelection) {
            return;
        }
        try {
            BlockPos blockPos = menu.getBlockPos();
            if (blockPos == null) {
                onClose.run();
                return;
            }
            NetworkHandler.sendBrushCopySelection(
                blockPos,
                menu.getCopyToggleBlock(),
                menu.getCopyToggleFace(),
                menu.getCopyToggleRotation(),
                menu.getCopyIndicators(),
                menu.getCopyBaseBlock(),
                menu.getCopyBaseFace(),
                menu.getCopyBaseRotation()
            );
        } catch (Exception e) {
            // Intentionally ignore network errors - close GUI regardless
        }
        onClose.run();
    }
    /** Handles Cancel button click. */
    public void handleCancel(@Nonnull Runnable onClose) {
        onClose.run();
    }
}