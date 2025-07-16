package net.justsomeswitches.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.justsomeswitches.util.DynamicBlockModelAnalyzer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * Raw JSON Variable Face Selection System - CLEAN VERSION
 * REWRITTEN: Pure string-based system using exact JSON variable names
 * Universal compatibility with vanilla and modded blocks
 *
 * FIXES APPLIED:
 * - Auto-apply default texture when blocks placed
 * - EXACT JSON variable extraction (no filtering, no additions, no modifications)
 * - Improved face selection preservation
 * - Removed excessive debug logging
 */
public class FaceSelectionData {

    // Non-face texture variables to exclude from face selection
    private static final Set<String> IGNORED_VARIABLES = Set.of(
            "particle", "overlay", "animation", "ctm", "connected",
            "north_overlay", "south_overlay", "east_overlay", "west_overlay",
            "up_overlay", "down_overlay"
    );

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
     * Create raw texture selection from ItemStack - FIXED VERSION
     * FIXES: Auto-apply default texture, exact variable filtering
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

            // Get available variables with EXACT filtering
            List<String> availableVariables = getExactFilteredVariables(blockInfo);

            // CRITICAL FIX: Determine if dropdown should be enabled based on EXACT variables
            boolean shouldEnable = availableVariables.size() > 1;

            // FIXED: Always determine best default variable for new blocks
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
     * CRITICAL FIX: Get EXACT texture variables from block's JSON file
     * NO filtering, NO additions, NO modifications - only exclude non-face textures like "particle"
     */
    @Nonnull
    private static List<String> getExactFilteredVariables(@Nonnull DynamicBlockModelAnalyzer.DynamicBlockInfo blockInfo) {
        // SIMPLE: Just use the availableVariables from DynamicBlockModelAnalyzer
        // It already excludes non-face textures and preserves JSON order
        List<String> availableVariables = blockInfo.getAvailableVariables();

        // If no variables found, provide fallback
        if (availableVariables.isEmpty()) {
            return List.of("all");
        }

        return availableVariables;
    }

    /**
     * Determine selected variable using priority-based logic
     */
    @Nonnull
    private static String determineSelectedVariable(@Nonnull List<String> availableVariables,
                                                    @Nonnull String currentSelection) {
        // If current selection is available, use it
        if (availableVariables.contains(currentSelection)) {
            return currentSelection;
        }

        // Use priority-based default selection
        return getDefaultVariable(availableVariables);
    }

    /**
     * Get default variable using priority order (side-focused, universal)
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
     * Create disabled selection for empty slots
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
     * Validate raw variable name
     */
    public static boolean isValidVariable(@Nonnull String variable) {
        return !variable.isEmpty() && !IGNORED_VARIABLES.contains(variable.toLowerCase());
    }

    /**
     * Get texture path for specific variable from ItemStack
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
     * Get all available variables from ItemStack - FIXED VERSION
     */
    @Nonnull
    public static List<String> getAvailableVariables(@Nonnull ItemStack itemStack) {
        if (itemStack.isEmpty()) return List.of("all");

        try {
            DynamicBlockModelAnalyzer.DynamicBlockInfo blockInfo =
                    DynamicBlockModelAnalyzer.analyzeBlockDynamically(itemStack);
            return getExactFilteredVariables(blockInfo);
        } catch (Exception e) {
            return List.of("all");
        }
    }

    // ========================================
    // LEGACY COMPATIBILITY (DEPRECATED)
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
     * @deprecated Use createRawTextureSelection instead
     */
    @Deprecated
    @Nonnull
    public static DropdownState createDropdownState(@Nonnull net.justsomeswitches.util.BlockTextureAnalyzer.BlockTextureInfo blockInfo,
                                                    @Nonnull Object currentSelection) {
        // Convert legacy call to new system
        String variable = (currentSelection != null) ? currentSelection.toString() : "all";
        RawTextureSelection rawSelection = createFallbackSelection(ItemStack.EMPTY, variable);
        return new DropdownState(rawSelection);
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