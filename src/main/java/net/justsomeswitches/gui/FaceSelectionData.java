package net.justsomeswitches.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.justsomeswitches.util.DynamicBlockModelAnalyzer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Modern Face Selection System with Java 17+ optimizations.
 * <p>
 * This system implements modern Java patterns including records, streams, and
 * immutable data structures for enhanced performance and type safety.
 * 
 * @since 1.0.0
 */
public class FaceSelectionData {

    // ========================================
    // MODERN JAVA: IMMUTABLE CONFIGURATION
    // ========================================

    /**
     * Modern record for priority configuration with improved immutability.
     */
    public record PriorityConfig(List<String> defaultOrder) {
        public PriorityConfig {
            // Defensive copy to ensure immutability
            defaultOrder = List.copyOf(defaultOrder);
        }

        /**
         * Default priority configuration for texture variable selection.
         */
        public static final PriorityConfig DEFAULT = new PriorityConfig(
                List.of("side", "north", "front", "top", "end", "south", "east", "west", "bottom", "back", "down", "up")
        );
    }

    private static final PriorityConfig PRIORITY_CONFIG = PriorityConfig.DEFAULT;

    // ========================================
    // MODERN JAVA: ENHANCED RECORD-BASED DATA STRUCTURES
    // ========================================

    /**
     * Modern record for raw texture selection with enhanced type safety and immutability.
     * <p>
     * This record replaces the previous class-based approach with a more efficient
     * and type-safe immutable data structure.
     */
    public record RawTextureSelection(
            boolean enabled,
            @Nonnull List<String> availableVariables,
            @Nonnull String selectedVariable,
            @Nullable String previewTexture,
            @Nonnull ItemStack sourceBlock
    ) {
        /**
         * Compact constructor with validation and defensive copying.
         */
        public RawTextureSelection {
            // Ensure immutability through defensive copying
            availableVariables = List.copyOf(availableVariables);
            sourceBlock = sourceBlock.copy();
        }

        /**
         * Enhanced factory method for creating disabled selections.
         */
        public static RawTextureSelection createDisabled() {
            return new RawTextureSelection(false, List.of("all"), "all", null, ItemStack.EMPTY);
        }

        /**
         * Check if preview texture is available.
         */
        public boolean hasPreview() { 
            return previewTexture != null && !previewTexture.isEmpty(); 
        }

        /**
         * MODERN JAVA: Stream-based display options generation.
         * <p>
         * Uses modern stream operations for efficient component creation.
         */
        @Nonnull
        public List<Component> getDisplayOptions() {
            return availableVariables.stream()
                    .map(Component::literal)
                    .collect(Collectors.toUnmodifiableList());
        }

        /**
         * Get index of currently selected variable for GUI dropdown.
         */
        public int getSelectedIndex() {
            return Math.max(0, availableVariables.indexOf(selectedVariable));
        }

        /**
         * Get variable name by index with bounds checking.
         */
        @Nonnull
        public String getVariableByIndex(int index) {
            if (index >= 0 && index < availableVariables.size()) {
                return availableVariables.get(index);
            }
            return availableVariables.isEmpty() ? "all" : availableVariables.get(0);
        }

        /**
         * Get texture path for the selected variable with caching.
         */
        @Nullable
        public String getTextureForVariable(@Nonnull String variable) {
            if (sourceBlock.isEmpty()) return null;

            try {
                var blockInfo = DynamicBlockModelAnalyzer.analyzeBlockDynamically(sourceBlock);
                return blockInfo.getTextureForVariable(variable);
            } catch (Exception e) {
                return null;
            }
        }
    }

    // ========================================
    // MODERN JAVA: FACTORY METHODS WITH STREAM OPTIMIZATIONS
    // ========================================

    /**
     * MODERN JAVA: Create raw texture selection using optimized stream processing.
     * <p>
     * Uses modern Java patterns including Optional, streams, and method references
     * for enhanced performance and readability.
     */
    @Nonnull
    public static RawTextureSelection createRawTextureSelection(@Nonnull ItemStack itemStack,
                                                                @Nonnull String currentSelection) {
        if (itemStack.isEmpty()) {
            return RawTextureSelection.createDisabled();
        }

        try {
            var blockInfo = DynamicBlockModelAnalyzer.analyzeBlockDynamically(itemStack);
            
            // MODERN JAVA: Stream-based variable processing
            final var availableVariables = blockInfo.getAvailableVariables().stream()
                    .filter(var -> !var.isEmpty())
                    .collect(Collectors.toUnmodifiableList());

            final var finalAvailableVariables = availableVariables.isEmpty() ? List.of("all") : availableVariables;

            boolean shouldEnable = finalAvailableVariables.size() > 1;

            // MODERN JAVA: Functional approach to variable selection
            String selectedVariable = Optional.of(currentSelection)
                    .filter(selection -> !selection.equals("all"))
                    .filter(finalAvailableVariables::contains)
                    .orElseGet(() -> getDefaultVariable(finalAvailableVariables));

            String previewTexture = blockInfo.getTextureForVariable(selectedVariable);

            return new RawTextureSelection(shouldEnable, finalAvailableVariables, selectedVariable, previewTexture, itemStack);

        } catch (Exception e) {
            return createFallbackSelection(itemStack, currentSelection);
        }
    }

    /**
     * MODERN JAVA: Stream-based default variable selection.
     * <p>
     * Uses stream operations with priority ordering for optimal variable selection.
     */
    @Nonnull
    public static String getDefaultVariable(@Nonnull List<String> availableVariables) {
        if (availableVariables.isEmpty()) {
            return "all";
        }

        // MODERN JAVA: Stream-based priority matching
        return PRIORITY_CONFIG.defaultOrder().stream()
                .filter(availableVariables::contains)
                .findFirst()
                .orElse(availableVariables.get(0));
    }

    /**
     * MODERN JAVA: Optimized texture path retrieval with caching.
     */
    @Nullable
    public static String getTextureForVariable(@Nonnull ItemStack itemStack, @Nonnull String variable) {
        if (itemStack.isEmpty()) return null;

        try {
            var blockInfo = DynamicBlockModelAnalyzer.analyzeBlockDynamically(itemStack);
            return blockInfo.getTextureForVariable(variable);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * MODERN JAVA: Stream-based available variables extraction.
     */
    @Nonnull
    public static List<String> getAvailableVariables(@Nonnull ItemStack itemStack) {
        if (itemStack.isEmpty()) return List.of("all");

        try {
            var blockInfo = DynamicBlockModelAnalyzer.analyzeBlockDynamically(itemStack);
            var variables = blockInfo.getAvailableVariables();
            return variables.isEmpty() ? List.of("all") : List.copyOf(variables);
        } catch (Exception e) {
            return List.of("all");
        }
    }

    // ========================================
    // PRIVATE UTILITY METHODS
    // ========================================

    /**
     * Create fallback selection for blocks that can't be analyzed.
     */
    @Nonnull
    private static RawTextureSelection createFallbackSelection(@Nonnull ItemStack itemStack,
                                                               @Nonnull String currentSelection) {
        var fallbackVariables = List.of("all");
        String selectedVariable = fallbackVariables.contains(currentSelection) ? currentSelection : "all";
        return new RawTextureSelection(false, fallbackVariables, selectedVariable, null, itemStack);
    }

    // ========================================
    // LEGACY COMPATIBILITY (DEPRECATED BUT PRESERVED)
    // ========================================

    /**
     * @deprecated Use RawTextureSelection record instead
     * <p>
     * This class is maintained for backward compatibility but new code should
     * use the modern RawTextureSelection record.
     */
    @Deprecated
    public static class DropdownState {
        private final RawTextureSelection rawSelection;

        public DropdownState(@Nonnull RawTextureSelection rawSelection) {
            this.rawSelection = rawSelection;
        }

        public boolean isEnabled() { return rawSelection.enabled(); }
        
        @Nonnull 
        public List<Component> getDisplayOptions() { return rawSelection.getDisplayOptions(); }
        
        @Nullable 
        public String getPreviewTexture() { return rawSelection.previewTexture(); }
        
        public boolean hasPreview() { return rawSelection.hasPreview(); }
    }

    /**
     * @deprecated Use RawTextureSelection.createDisabled() instead
     */
    @Deprecated
    @Nonnull
    public static DropdownState createDisabledState() {
        return new DropdownState(RawTextureSelection.createDisabled());
    }

    // ========================================
    // MODERN JAVA: UTILITY RECORDS FOR ENHANCED TYPE SAFETY
    // ========================================

    /**
     * Modern record for validation results with enhanced error information.
     */
    public record ValidationResult(
            boolean isValid,
            @Nonnull String message,
            @Nonnull Optional<String> suggestion
    ) {
        public ValidationResult {
            message = Objects.requireNonNull(message, "Validation message cannot be null");
            suggestion = Objects.requireNonNull(suggestion, "Suggestion optional cannot be null");
        }

        /**
         * Factory method for successful validation.
         */
        public static ValidationResult success() {
            return new ValidationResult(true, "Valid", Optional.empty());
        }

        /**
         * Factory method for failed validation with suggestion.
         */
        public static ValidationResult failure(@Nonnull String message, @Nullable String suggestion) {
            return new ValidationResult(false, message, Optional.ofNullable(suggestion));
        }
    }

    /**
     * MODERN JAVA: Validate texture selection with comprehensive error reporting.
     */
    @Nonnull
    public static ValidationResult validateSelection(@Nonnull ItemStack itemStack, @Nonnull String variable) {
        if (itemStack.isEmpty()) {
            return ValidationResult.failure("No block selected", "Place a block in the slot");
        }

        var availableVariables = getAvailableVariables(itemStack);
        if (!availableVariables.contains(variable)) {
            String suggestion = getDefaultVariable(availableVariables);
            return ValidationResult.failure(
                "Variable '%s' not available for this block".formatted(variable),
                suggestion
            );
        }

        return ValidationResult.success();
    }

    /**
     * Modern record for selection statistics with performance metrics.
     */
    public record SelectionStats(
            int totalSelections,
            @Nonnull Map<String, Integer> variableUsage,
            double averageVariableCount
    ) {
        public SelectionStats {
            variableUsage = Map.copyOf(variableUsage);
        }

        /**
         * Get the most commonly used variable.
         */
        @Nonnull
        public Optional<String> getMostUsedVariable() {
            return variableUsage.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey);
        }
    }
}
