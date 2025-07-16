package net.justsomeswitches.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.justsomeswitches.util.DynamicBlockModelAnalyzer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * Raw JSON Variable Face Selection System - CLEANED VERSION
 * Pure string-based system using exact JSON variable names
 * Universal compatibility with vanilla and modded blocks
 *
 * REMOVED: Unused deprecated method createDropdownState() (zero references found)
 */
public class FaceSelectionData {

    // Priority order for default selection (side-focused, universal)
    private static final String[] DEFAULT_PRIORITY = {
            "side", "north", "front", "top", "end", "south", "east", "west", "bottom", "back", "down", "up"
    };

    /**
     * Raw texture selection state using exact JSON variable names
     */
    public static class RawTextureSelection {
        private final boolean enabled;
        private final List<String> availableVariables;  // Raw JSON order preserved
        private final String selectedVariable;           // Raw JSON variable name
        private final String previewTexture;
        private final ItemStack sourceBlock;

        public RawTextureSelection(boolean enabled,
                                   @Nonnull List<String> availableVariables,
                                   @Nonnull String selectedVariable,
                                   @Nullable String previewTexture,
                                   @Nonnull ItemStack sourceBlock) {
            this.enabled = enabled;
            this.availableVariables = new ArrayList<>(availableVariables);
            this.selectedVariable = selectedVariable;
            this.previewTexture = previewTexture;
            this.sourceBlock = sourceBlock.copy();
        }

        public boolean isEnabled() { return enabled; }
        @Nonnull public List<String> getAvailableVariables() { return new ArrayList<>(availableVariables); }
        @Nonnull public String getSelectedVariable() { return selectedVariable; }
        @Nullable public String getPreviewTexture() { return previewTexture; }
        @Nonnull public ItemStack getSourceBlock() { return sourceBlock.copy(); }

        public boolean hasPreview() { return previewTexture != null && !previewTexture.isEmpty(); }

        /**
         * CRITICAL: Return raw JSON variable names as display options
         * NO conversion, NO capitalization, NO modification
         */
        @Nonnull
        public List<Component> getDisplayOptions() {
            return availableVariables.stream()
                    .map(variable -> (Component) Component.literal(variable))
                    .toList();
        }

        /**
         * Get index of currently selected variable for GUI dropdown
         */
        public int getSelectedIndex() {
            return Math.max(0, availableVariables.indexOf(selectedVariable));
        }

        /**
         * Get variable name by index for GUI dropdown selection
         */
        @Nonnull
        public String getVariableByIndex(int index) {
            if (index >= 0 && index < availableVariables.size()) {
                return availableVariables.get(index);
            }
            return availableVariables.isEmpty() ? "all" : availableVariables.get(0);
        }

        /**
         * Get texture path for the selected variable
         */
        @Nullable
        public String getTextureForVariable(@Nonnull String variable) {
            if (sourceBlock.isEmpty()) return null;

            try {
                DynamicBlockModelAnalyzer.DynamicBlockInfo blockInfo =
                        DynamicBlockModelAnalyzer.analyzeBlockDynamically(sourceBlock);
                return blockInfo.getTextureForVariable(variable);
            } catch (Exception e) {
                return null;
            }
        }
    }

    /**
     * Create raw texture selection from ItemStack - USED BY SwitchTextureMenu
     */
    @Nonnull
    public static RawTextureSelection createRawTextureSelection(@Nonnull ItemStack itemStack,
                                                                @Nonnull String currentSelection) {
        if (itemStack.isEmpty()) {
            return createDisabledSelection();
        }

        try {
            // Analyze block dynamically to get raw JSON variables
            DynamicBlockModelAnalyzer.DynamicBlockInfo blockInfo =
                    DynamicBlockModelAnalyzer.analyzeBlockDynamically(itemStack);

            // Get available variables
            List<String> availableVariables = blockInfo.getAvailableVariables();

            // If no variables found, provide fallback
            if (availableVariables.isEmpty()) {
                availableVariables = List.of("all");
            }

            // Determine if dropdown should be enabled based on variables
            boolean shouldEnable = availableVariables.size() > 1;

            // Determine best variable for new blocks
            String selectedVariable;
            if (currentSelection.equals("all") || !availableVariables.contains(currentSelection)) {
                // Use default selection for new blocks or invalid selections
                selectedVariable = getDefaultVariable(availableVariables);
            } else {
                // Keep current selection if valid
                selectedVariable = currentSelection;
            }

            // Get preview texture for selected variable
            String previewTexture = blockInfo.getTextureForVariable(selectedVariable);

            return new RawTextureSelection(shouldEnable, availableVariables, selectedVariable, previewTexture, itemStack);

        } catch (Exception e) {
            // Fallback for blocks that can't be analyzed
            return createFallbackSelection(itemStack, currentSelection);
        }
    }

    /**
     * Get default variable using priority order - USED BY SwitchTextureMenu
     */
    @Nonnull
    public static String getDefaultVariable(@Nonnull List<String> availableVariables) {
        if (availableVariables.isEmpty()) {
            return "all";
        }

        // Try priority order first
        for (String priority : DEFAULT_PRIORITY) {
            if (availableVariables.contains(priority)) {
                return priority;
            }
        }

        // Return first available if no priority matches
        return availableVariables.get(0);
    }

    /**
     * Create disabled selection for empty slots - USED BY SwitchTextureScreen
     */
    @Nonnull
    public static RawTextureSelection createDisabledSelection() {
        return new RawTextureSelection(false, List.of("all"), "all", null, ItemStack.EMPTY);
    }

    /**
     * Create fallback selection for blocks that can't be analyzed
     */
    @Nonnull
    private static RawTextureSelection createFallbackSelection(@Nonnull ItemStack itemStack,
                                                               @Nonnull String currentSelection) {
        List<String> fallbackVariables = List.of("all");
        String selectedVariable = fallbackVariables.contains(currentSelection) ? currentSelection : "all";

        return new RawTextureSelection(false, fallbackVariables, selectedVariable, null, itemStack);
    }

    /**
     * Get texture path for specific variable from ItemStack - USED BY SwitchTextureMenu
     */
    @Nullable
    public static String getTextureForVariable(@Nonnull ItemStack itemStack, @Nonnull String variable) {
        if (itemStack.isEmpty()) return null;

        try {
            DynamicBlockModelAnalyzer.DynamicBlockInfo blockInfo =
                    DynamicBlockModelAnalyzer.analyzeBlockDynamically(itemStack);
            return blockInfo.getTextureForVariable(variable);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get all available variables from ItemStack
     */
    @Nonnull
    public static List<String> getAvailableVariables(@Nonnull ItemStack itemStack) {
        if (itemStack.isEmpty()) return List.of("all");

        try {
            DynamicBlockModelAnalyzer.DynamicBlockInfo blockInfo =
                    DynamicBlockModelAnalyzer.analyzeBlockDynamically(itemStack);
            List<String> variables = blockInfo.getAvailableVariables();
            return variables.isEmpty() ? List.of("all") : variables;
        } catch (Exception e) {
            return List.of("all");
        }
    }

    // ========================================
    // LEGACY COMPATIBILITY (DEPRECATED BUT STILL REFERENCED)
    // ========================================

    /**
     * @deprecated Use RawTextureSelection instead
     */
    @Deprecated
    public static class DropdownState {
        private final RawTextureSelection rawSelection;

        public DropdownState(@Nonnull RawTextureSelection rawSelection) {
            this.rawSelection = rawSelection;
        }

        public boolean isEnabled() { return rawSelection.isEnabled(); }
        @Nonnull public List<Component> getDisplayOptions() { return rawSelection.getDisplayOptions(); }
        @Nullable public String getPreviewTexture() { return rawSelection.getPreviewTexture(); }
        public boolean hasPreview() { return rawSelection.hasPreview(); }
    }

    /**
     * @deprecated Use createDisabledSelection instead
     */
    @Deprecated
    @Nonnull
    public static DropdownState createDisabledState() {
        return new DropdownState(createDisabledSelection());
    }
}