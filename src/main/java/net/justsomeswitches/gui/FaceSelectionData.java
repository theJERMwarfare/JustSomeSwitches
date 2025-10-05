package net.justsomeswitches.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.justsomeswitches.util.DynamicBlockModelAnalyzer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/** Modern face selection system using Java 17+ records and streams. */
public class FaceSelectionData {

    /** Record for priority configuration with immutability. */
    public record PriorityConfig(List<String> defaultOrder) {
        public PriorityConfig {
            defaultOrder = List.copyOf(defaultOrder);
        }

        /** Default priority configuration for texture variable selection. */
        public static final PriorityConfig DEFAULT = new PriorityConfig(
                List.of("side", "north", "front", "top", "end", "south", "east", "west", "bottom", "back", "down", "up")
        );
    }

    private static final PriorityConfig PRIORITY_CONFIG = PriorityConfig.DEFAULT;

    /** Record for raw texture selection with immutability and type safety. */
    public record RawTextureSelection(
            boolean enabled,
            @Nonnull List<String> availableVariables,
            @Nonnull String selectedVariable,
            @Nullable String previewTexture,
            @Nonnull ItemStack sourceBlock
    ) {
        /** Compact constructor with defensive copying. */
        public RawTextureSelection {
            availableVariables = List.copyOf(availableVariables);
            sourceBlock = sourceBlock.copy();
        }

        /** Creates disabled selection. */
        public static RawTextureSelection createDisabled() {
            return new RawTextureSelection(false, List.of("all"), "all", null, ItemStack.EMPTY);
        }

        /** Checks if preview texture is available. */
        public boolean hasPreview() { 
            return previewTexture != null && !previewTexture.isEmpty(); 
        }

        /** Returns display options using stream operations. */
        @Nonnull
        public List<Component> getDisplayOptions() {
            return availableVariables.stream()
                    .map(Component::literal)
                    .collect(Collectors.toUnmodifiableList());
        }

        /** Returns index of currently selected variable. */
        @SuppressWarnings("unused") // Public API method
        public int getSelectedIndex() {
            return Math.max(0, availableVariables.indexOf(selectedVariable));
        }

        /** Returns variable name by index with bounds checking. */
        @SuppressWarnings("unused") // Public API method
        @Nonnull
        public String getVariableByIndex(int index) {
            if (index >= 0 && index < availableVariables.size()) {
                return availableVariables.get(index);
            }
            return availableVariables.isEmpty() ? "all" : availableVariables.get(0);
        }

        /** Returns texture path for the selected variable. */
        @SuppressWarnings("unused") // Public API method
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

    /** Creates raw texture selection using stream processing. */
    @Nonnull
    public static RawTextureSelection createRawTextureSelection(@Nonnull ItemStack itemStack,
                                                                @Nonnull String currentSelection) {
        if (itemStack.isEmpty()) {
            return RawTextureSelection.createDisabled();
        }

        try {
            var blockInfo = DynamicBlockModelAnalyzer.analyzeBlockDynamically(itemStack);
            
            final var availableVariables = blockInfo.getAvailableVariables().stream()
                    .filter(var -> !var.isEmpty())
                    .toList();

            final var finalAvailableVariables = availableVariables.isEmpty() ? List.of("all") : availableVariables;

            boolean shouldEnable = finalAvailableVariables.size() > 1;

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

    /** Returns default variable using priority ordering. */
    @Nonnull
    public static String getDefaultVariable(@Nonnull List<String> availableVariables) {
        if (availableVariables.isEmpty()) {
            return "all";
        }

        return PRIORITY_CONFIG.defaultOrder().stream()
                .filter(availableVariables::contains)
                .findFirst()
                .orElse(availableVariables.get(0));
    }

    /** Returns texture path for variable with caching. */
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

    /** Returns available variables for ItemStack. */
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

    /** Creates fallback selection for blocks that can't be analyzed. */
    @Nonnull
    private static RawTextureSelection createFallbackSelection(@Nonnull ItemStack itemStack,
                                                               @Nonnull String currentSelection) {
        var fallbackVariables = List.of("all");
        String selectedVariable = fallbackVariables.contains(currentSelection) ? currentSelection : "all";
        return new RawTextureSelection(false, fallbackVariables, selectedVariable, null, itemStack);
    }

    /** @deprecated Use RawTextureSelection record instead. */
    @Deprecated
    @SuppressWarnings("DeprecatedIsStillUsed") // Legacy compatibility
    public static class DropdownState {
        private final RawTextureSelection rawSelection;

        public DropdownState(@Nonnull RawTextureSelection rawSelection) {
            this.rawSelection = rawSelection;
        }

        @SuppressWarnings("unused") // Legacy API
        public boolean isEnabled() { return rawSelection.enabled(); }
        
        @SuppressWarnings("unused") // Legacy API
        @Nonnull 
        public List<Component> getDisplayOptions() { return rawSelection.getDisplayOptions(); }
        
        @SuppressWarnings("unused") // Legacy API
        @Nullable 
        public String getPreviewTexture() { return rawSelection.previewTexture(); }
        
        @SuppressWarnings("unused") // Legacy API
        public boolean hasPreview() { return rawSelection.hasPreview(); }
    }

    /** @deprecated Use RawTextureSelection.createDisabled() instead. */
    @Deprecated
    @Nonnull
    public static DropdownState createDisabledState() {
        return new DropdownState(RawTextureSelection.createDisabled());
    }

    /** Record for validation results with error information. */
    public record ValidationResult(
            boolean isValid,
            @Nonnull String message,
            @Nonnull Optional<String> suggestion
    ) {
        public ValidationResult {
            message = Objects.requireNonNull(message, "Validation message cannot be null");
            suggestion = Objects.requireNonNull(suggestion, "Suggestion optional cannot be null");
        }

        /** Creates successful validation result. */
        public static ValidationResult success() {
            return new ValidationResult(true, "Valid", Optional.empty());
        }

        /** Creates failed validation result with suggestion. */
        public static ValidationResult failure(@Nonnull String message, @Nullable String suggestion) {
            return new ValidationResult(false, message, Optional.ofNullable(suggestion));
        }
    }

    /** Validates texture selection with comprehensive error reporting. */
    @SuppressWarnings("unused") // Public API method
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

    /** Record for selection statistics with performance metrics. */
    @SuppressWarnings("unused") // Future analytics feature
    public record SelectionStats(
            int totalSelections,
            @Nonnull Map<String, Integer> variableUsage,
            double averageVariableCount
    ) {
        public SelectionStats {
            variableUsage = Map.copyOf(variableUsage);
        }

        /** Returns most commonly used variable. */
        @SuppressWarnings("unused") // Future analytics feature
        @Nonnull
        public Optional<String> getMostUsedVariable() {
            return variableUsage.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey);
        }
    }
}
