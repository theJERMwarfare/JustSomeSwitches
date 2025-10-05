package net.justsomeswitches.gui.components;

import net.justsomeswitches.gui.WrenchCopyMenu;
import net.justsomeswitches.network.NetworkHandler;
import net.minecraft.core.BlockPos;

import javax.annotation.Nonnull;

/**
 * Handles copy action logic for the Wrench Copy GUI.
 */
public class CopyActionHandler {
    /** Handles Select All button click. */
    public void handleSelectAll(@Nonnull WrenchCopyMenu menu) {
        menu.setAllCopySelections(true);
    }
    /** Handles Clear All button click. */
    public void handleClearAll(@Nonnull WrenchCopyMenu menu) {
        menu.setAllCopySelections(false);
    }
    /** Handles Copy Selected button click. */
    public void handleCopySelected(@Nonnull WrenchCopyMenu menu, @Nonnull Runnable onClose) {
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
            NetworkHandler.sendWrenchCopySelection(
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
    /** Checks if at least one copy option is selected. */
    public boolean hasAnySelection(@Nonnull WrenchCopyMenu menu) {
        return menu.getCopyToggleBlock() || menu.getCopyToggleFace() || 
               menu.getCopyToggleRotation() || menu.getCopyIndicators() ||
               menu.getCopyBaseBlock() || menu.getCopyBaseFace() || 
               menu.getCopyBaseRotation();
    }
    /** Validates copy operation readiness. */
    @SuppressWarnings("unused") // May be used for future validation
    public boolean canProceedWithCopy(@Nonnull WrenchCopyMenu menu) {
        if (!hasAnySelection(menu)) {
            return false;
        }
        BlockPos blockPos = menu.getBlockPos();
        return blockPos != null;
    }
}