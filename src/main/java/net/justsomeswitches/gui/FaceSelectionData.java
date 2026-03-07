package net.justsomeswitches.gui;

import net.minecraft.world.item.ItemStack;
import net.justsomeswitches.util.DynamicBlockModelAnalyzer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

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

    /** Creates fallback selection for blocks that can't be analyzed. */
    @Nonnull
    private static RawTextureSelection createFallbackSelection(@Nonnull ItemStack itemStack,
                                                               @Nonnull String currentSelection) {
        var fallbackVariables = List.of("all");
        String selectedVariable = fallbackVariables.contains(currentSelection) ? currentSelection : "all";
        return new RawTextureSelection(false, fallbackVariables, selectedVariable, null, itemStack);
    }

}
