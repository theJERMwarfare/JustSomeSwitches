package net.justsomeswitches.gui;

import net.justsomeswitches.config.DebugConfig;
import net.justsomeswitches.util.DynamicBlockModelAnalyzer;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * DIAGNOSTIC: Face Selection Data System with MINIMAL logging
 */
public class FaceSelectionData {

    // Cache to prevent repeated dropdown creation
    private static final Map<String, DropdownState> dropdownStateCache = new HashMap<>();
    private static final Map<ItemStack, String> blockAnalysisCache = new HashMap<>();

    /**
     * Face option enum with JSON variable names exactly as they appear in model files
     */
    public enum FaceOption {
        ALL("all", "All Faces", null),

        // Standard face directions
        TOP("top", "top", Direction.UP),
        BOTTOM("bottom", "bottom", Direction.DOWN),
        NORTH("north", "north", Direction.NORTH),
        SOUTH("south", "south", Direction.SOUTH),
        EAST("east", "east", Direction.EAST),
        WEST("west", "west", Direction.WEST),

        // JSON variable names (exactly as they appear in model files)
        END("end", "end", Direction.UP),      // For logs - top/bottom faces
        SIDE("side", "side", Direction.NORTH), // For logs - side faces
        FRONT("front", "front", Direction.NORTH),
        BACK("back", "back", Direction.SOUTH);

        private final String serializedName;
        private final String displayName;
        private final Direction direction;

        FaceOption(String serializedName, String displayName, Direction direction) {
            this.serializedName = serializedName;
            this.displayName = displayName;
            this.direction = direction;
        }

        @Nonnull public String getSerializedName() { return serializedName; }
        @Nonnull public String getDisplayName() { return displayName; }
        @Nullable public Direction getDirection() { return direction; }
        public boolean isAll() { return this == ALL; }

        /**
         * Convert from serialized name (for NBT loading)
         */
        @Nonnull
        public static FaceOption fromSerializedName(@Nonnull String name) {
            for (FaceOption option : values()) {
                if (option.serializedName.equals(name)) {
                    return option;
                }
            }
            return ALL; // Fallback to ALL for invalid names
        }

        /**
         * Convert from direction
         */
        @Nonnull
        public static FaceOption fromDirection(@Nonnull Direction direction) {
            for (FaceOption option : values()) {
                if (option.direction == direction) {
                    return option;
                }
            }
            return ALL; // Fallback
        }

        /**
         * Create FaceOption from actual JSON variable name
         */
        @Nonnull
        public static FaceOption fromJsonVariable(@Nonnull String variableName) {
            // First try exact match
            for (FaceOption option : values()) {
                if (option.serializedName.equals(variableName)) {
                    return option;
                }
            }

            // If no exact match, create a fallback mapping
            return switch (variableName.toLowerCase()) {
                case "end" -> END;
                case "side" -> SIDE;
                case "front" -> FRONT;
                case "back" -> BACK;
                case "top" -> TOP;
                case "bottom" -> BOTTOM;
                case "north" -> NORTH;
                case "south" -> SOUTH;
                case "east" -> EAST;
                case "west" -> WEST;
                default -> ALL;
            };
        }
    }

    /**
     * Dropdown state information
     */
    public static class DropdownState {
        private final boolean enabled;
        private final List<FaceOption> availableOptions;
        private final FaceOption selectedOption;
        private final String previewTexture;

        public DropdownState(boolean enabled,
                             @Nonnull List<FaceOption> availableOptions,
                             @Nonnull FaceOption selectedOption,
                             @Nullable String previewTexture) {
            this.enabled = enabled;
            this.availableOptions = new ArrayList<>(availableOptions);
            this.selectedOption = selectedOption;
            this.previewTexture = previewTexture;
        }

        public boolean isEnabled() { return enabled; }
        @Nonnull public List<FaceOption> getAvailableOptions() { return new ArrayList<>(availableOptions); }
        @Nonnull public FaceOption getSelectedOption() { return selectedOption; }
        @Nullable public String getPreviewTexture() { return previewTexture; }

        public boolean hasPreview() { return previewTexture != null && !previewTexture.isEmpty(); }

        @Nonnull
        public List<Component> getDisplayOptions() {
            return availableOptions.stream()
                    .map(option -> (Component) Component.literal(option.getDisplayName()))
                    .toList();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof DropdownState other)) return false;
            return enabled == other.enabled &&
                    selectedOption == other.selectedOption &&
                    availableOptions.equals(other.availableOptions) &&
                    Objects.equals(previewTexture, other.previewTexture);
        }

        @Override
        public int hashCode() {
            return Objects.hash(enabled, availableOptions, selectedOption, previewTexture);
        }
    }

    /**
     * DIAGNOSTIC: Create dropdown state with minimal logging
     */
    @Nonnull
    public static DropdownState createDropdownState(@Nonnull net.justsomeswitches.util.BlockTextureAnalyzer.BlockTextureInfo blockInfo,
                                                    @Nonnull FaceOption currentSelectedOption) {

        boolean hasMultipleFaces = blockInfo.hasMultipleFaceTextures();

        if (!hasMultipleFaces) {
            return new DropdownState(false, List.of(FaceOption.ALL), FaceOption.ALL, null);
        }

        // For multi-texture blocks, determine actual available options
        List<FaceOption> availableOptions = new ArrayList<>();

        // Get texture variables from block model
        List<String> textureVariables = getTextureVariables(blockInfo);

        // Convert texture variables to face options (preserve exact JSON variable names)
        for (String variable : textureVariables) {
            FaceOption faceOption = FaceOption.fromJsonVariable(variable);
            if (!availableOptions.contains(faceOption)) {
                availableOptions.add(faceOption);
            }
        }

        // Only add ALL if no specific face options were found
        if (availableOptions.isEmpty()) {
            availableOptions.add(FaceOption.ALL);
        }

        // CRITICAL: Preserve user's current selection - NO AUTO-SETTING
        FaceOption validatedSelection = validateSelection(currentSelectedOption, availableOptions);

        DebugConfig.logStateChange("DROPDOWN", "Created - Options:" + availableOptions + " Selected:" + validatedSelection);

        // Create dropdown state with user's selection preserved
        String previewTexture = getTextureForSelection(blockInfo, validatedSelection);
        return new DropdownState(true, availableOptions, validatedSelection, previewTexture);
    }

    /**
     * Get texture variables with better log pattern detection
     */
    @Nonnull
    private static List<String> getTextureVariables(@Nonnull net.justsomeswitches.util.BlockTextureAnalyzer.BlockTextureInfo blockInfo) {
        // Check for common patterns first
        Map<Direction, String> faceTextures = blockInfo.getFaceTextures();
        Set<String> uniqueTextures = new HashSet<>(faceTextures.values());

        List<String> variables = new ArrayList<>();

        // Detect log pattern more accurately
        if (uniqueTextures.size() == 2) {
            boolean hasLogPattern = uniqueTextures.stream().anyMatch(tex ->
                    tex.contains("_log"));

            if (hasLogPattern) {
                variables.add("side");  // ← JSON variable name exactly as in model
                variables.add("end");   // ← JSON variable name exactly as in model
                return variables;
            }
        }

        // Add standard face options for multi-texture blocks
        if (uniqueTextures.size() > 1) {
            // Add actual texture variables found
            for (String texture : uniqueTextures) {
                String variable = extractVariableFromTexture(texture);
                if (!variable.equals("all") && !variables.contains(variable)) {
                    variables.add(variable);
                }
            }
        }

        return variables;
    }

    /**
     * Extract variable name from texture path (use exact JSON variable names)
     */
    @Nonnull
    private static String extractVariableFromTexture(@Nonnull String texturePath) {
        if (texturePath.contains("_top")) return "end";  // ← JSON variable name
        if (texturePath.contains("_log") && !texturePath.contains("_top")) return "side";  // ← JSON variable name
        if (texturePath.contains("_front")) return "front";
        if (texturePath.contains("_back")) return "back";
        if (texturePath.contains("_side")) return "side";
        return "side"; // Default for unrecognized patterns
    }

    /**
     * Get texture path for a specific face selection - improved mapping
     */
    @Nullable
    public static String getTextureForSelection(@Nonnull net.justsomeswitches.util.BlockTextureAnalyzer.BlockTextureInfo blockInfo,
                                                @Nonnull FaceOption selection) {
        if (selection.isAll()) {
            // For "All", return the most appropriate texture
            if (blockInfo.getUniformTexture() != null) {
                return blockInfo.getUniformTexture();
            }

            // For multi-face blocks, prefer side texture for preview
            String sideTexture = blockInfo.getTextureForFace(Direction.NORTH);
            if (sideTexture != null && net.justsomeswitches.util.BlockTextureAnalyzer.isValidTexture(sideTexture)) {
                return sideTexture;
            }

            // Fallback to any available texture
            return blockInfo.getFaceTextures().values().stream()
                    .filter(net.justsomeswitches.util.BlockTextureAnalyzer::isValidTexture)
                    .findFirst()
                    .orElse(null);
        } else if (selection == FaceOption.SIDE) {
            // For SIDE selection, get side texture specifically
            String sideTexture = blockInfo.getTextureForFace(Direction.NORTH);
            if (sideTexture != null && net.justsomeswitches.util.BlockTextureAnalyzer.isValidTexture(sideTexture)) {
                return sideTexture;
            }
        } else if (selection == FaceOption.END) {
            // For END selection, get top texture specifically
            String topTexture = blockInfo.getTextureForFace(Direction.UP);
            if (topTexture != null && net.justsomeswitches.util.BlockTextureAnalyzer.isValidTexture(topTexture)) {
                return topTexture;
            }
        } else if (selection.getDirection() != null) {
            // For specific face, get that face's texture
            String faceTexture = blockInfo.getTextureForFace(selection.getDirection());
            if (faceTexture != null && net.justsomeswitches.util.BlockTextureAnalyzer.isValidTexture(faceTexture)) {
                return faceTexture;
            }
        }

        return null;
    }

    /**
     * Create disabled dropdown state (for empty slots)
     */
    @Nonnull
    public static DropdownState createDisabledState() {
        return new DropdownState(false, List.of(FaceOption.ALL), FaceOption.ALL, null);
    }

    /**
     * DIAGNOSTIC: Validate face selection - PRESERVE user's choice, no auto-defaults
     */
    @Nonnull
    public static FaceOption validateSelection(@Nonnull FaceOption selection,
                                               @Nonnull List<FaceOption> availableOptions) {
        if (availableOptions.contains(selection)) {
            return selection;
        }

        // Only fallback if the user's selection is truly invalid
        if (availableOptions.contains(FaceOption.ALL)) {
            return FaceOption.ALL;
        }

        // Return first available option only as last resort
        return availableOptions.isEmpty() ? FaceOption.ALL : availableOptions.get(0);
    }

    /**
     * Clear caches (for memory management)
     */
    public static void clearCaches() {
        dropdownStateCache.clear();
        blockAnalysisCache.clear();
    }
}