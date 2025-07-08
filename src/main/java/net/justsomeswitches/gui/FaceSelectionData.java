package net.justsomeswitches.gui;

import net.justsomeswitches.config.DebugConfig;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * FIXED: Enhanced Face Selection with strong user choice preservation
 */
public class FaceSelectionData {

    // Reduced caching to prevent stale state issues
    private static final Map<String, DropdownState> dropdownStateCache = new HashMap<>();

    /**
     * FIXED: Enhanced face option enum with robust serialization
     */
    public enum FaceOption {
        ALL("all", "All Faces", null),

        // Standard face directions
        TOP("top", "Top", Direction.UP),
        BOTTOM("bottom", "Bottom", Direction.DOWN),
        NORTH("north", "North", Direction.NORTH),
        SOUTH("south", "South", Direction.SOUTH),
        EAST("east", "East", Direction.EAST),
        WEST("west", "West", Direction.WEST),

        // JSON variable names (exactly as they appear in model files)
        END("end", "Ends", Direction.UP),      // For logs - top/bottom faces
        SIDE("side", "Sides", Direction.NORTH), // For logs - side faces
        FRONT("front", "Front", Direction.NORTH),
        BACK("back", "Back", Direction.SOUTH);

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
         * FIXED: Enhanced serialization with guaranteed preservation
         */
        @Nonnull
        public static FaceOption fromSerializedName(@Nonnull String name) {
            // Quick validation
            if (name == null || name.trim().isEmpty()) {
                return ALL;
            }

            // Exact match (most common case)
            for (FaceOption option : values()) {
                if (option.serializedName.equals(name)) {
                    return option;
                }
            }

            // Case-insensitive fallback for robustness
            for (FaceOption option : values()) {
                if (option.serializedName.equalsIgnoreCase(name)) {
                    return option;
                }
            }

            // Ultimate fallback
            return ALL;
        }

        @Nonnull
        public static FaceOption fromDirection(@Nonnull Direction direction) {
            for (FaceOption option : values()) {
                if (option.direction == direction) {
                    return option;
                }
            }
            return ALL;
        }

        @Nonnull
        public static FaceOption fromJsonVariable(@Nonnull String variableName) {
            for (FaceOption option : values()) {
                if (option.serializedName.equals(variableName)) {
                    return option;
                }
            }

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
     * FIXED: Enhanced dropdown state creation with STRONG user selection preservation
     */
    @Nonnull
    public static DropdownState createDropdownState(@Nonnull net.justsomeswitches.util.BlockTextureAnalyzer.BlockTextureInfo blockInfo,
                                                    @Nonnull FaceSelectionData.FaceOption currentSelectedOption) {

        DebugConfig.logUserAction("Creating dropdown for preserved selection: " + currentSelectedOption);

        boolean hasMultipleFaces = blockInfo.hasMultipleFaceTextures();

        if (!hasMultipleFaces) {
            // Single texture block - use ALL but preserve the user's choice if possible
            return new DropdownState(false, List.of(FaceOption.ALL), FaceOption.ALL, blockInfo.getUniformTexture());
        }

        // For multi-texture blocks, determine available options
        List<FaceOption> availableOptions = new ArrayList<>();

        // Get texture variables from block model
        List<String> textureVariables = getTextureVariables(blockInfo);

        // Convert texture variables to face options
        for (String variable : textureVariables) {
            FaceOption faceOption = FaceOption.fromJsonVariable(variable);
            if (!availableOptions.contains(faceOption)) {
                availableOptions.add(faceOption);
            }
        }

        // Add ALL if no specific options found
        if (availableOptions.isEmpty()) {
            availableOptions.add(FaceOption.ALL);
        }

        // CRITICAL FIX: STRONGLY preserve user's selection
        FaceOption finalSelection = preserveUserSelection(currentSelectedOption, availableOptions);

        String previewTexture = getTextureForSelection(blockInfo, finalSelection);

        return new DropdownState(true, availableOptions, finalSelection, previewTexture);
    }

    /**
     * FIXED: STRONG user selection preservation - prioritizes exact preservation
     */
    @Nonnull
    private static FaceOption preserveUserSelection(@Nonnull FaceOption userSelection, @Nonnull List<FaceOption> availableOptions) {
        // PRIORITY 1: If user's selection is available, ALWAYS preserve it
        if (availableOptions.contains(userSelection)) {
            DebugConfig.logUserAction("Preserving exact user selection: " + userSelection);
            return userSelection;
        }

        // PRIORITY 2: Only if user's selection is not available, find similar
        FaceOption similar = findSimilarOption(userSelection, availableOptions);
        if (similar != null) {
            DebugConfig.logUserAction("User selection " + userSelection + " → similar: " + similar);
            return similar;
        }

        // PRIORITY 3: Fallback to ALL or first available
        FaceOption fallback = availableOptions.contains(FaceOption.ALL) ? FaceOption.ALL : availableOptions.get(0);
        DebugConfig.logUserAction("User selection " + userSelection + " → fallback: " + fallback);
        return fallback;
    }

    /**
     * Get texture variables with better pattern detection
     */
    @Nonnull
    private static List<String> getTextureVariables(@Nonnull net.justsomeswitches.util.BlockTextureAnalyzer.BlockTextureInfo blockInfo) {
        Map<Direction, String> faceTextures = blockInfo.getFaceTextures();
        Set<String> uniqueTextures = new HashSet<>(faceTextures.values());

        List<String> variables = new ArrayList<>();

        // Detect log pattern (most common case)
        if (uniqueTextures.size() == 2) {
            boolean hasLogPattern = uniqueTextures.stream().anyMatch(tex -> tex.contains("_log"));

            if (hasLogPattern) {
                variables.add("side");  // JSON variable name
                variables.add("end");   // JSON variable name
                return variables;
            }
        }

        // For other multi-texture blocks
        if (uniqueTextures.size() > 1) {
            for (String texture : uniqueTextures) {
                String variable = extractVariableFromTexture(texture);
                if (!variable.equals("all") && !variables.contains(variable)) {
                    variables.add(variable);
                }
            }
        }

        return variables;
    }

    @Nonnull
    private static String extractVariableFromTexture(@Nonnull String texturePath) {
        if (texturePath.contains("_top")) return "end";
        if (texturePath.contains("_log") && !texturePath.contains("_top")) return "side";
        if (texturePath.contains("_front")) return "front";
        if (texturePath.contains("_back")) return "back";
        if (texturePath.contains("_side")) return "side";
        return "side";
    }

    /**
     * Get texture path for selection - enhanced mapping
     */
    @Nullable
    public static String getTextureForSelection(@Nonnull net.justsomeswitches.util.BlockTextureAnalyzer.BlockTextureInfo blockInfo,
                                                @Nonnull FaceOption selection) {
        if (selection.isAll()) {
            if (blockInfo.getUniformTexture() != null) {
                return blockInfo.getUniformTexture();
            }

            String sideTexture = blockInfo.getTextureForFace(Direction.NORTH);
            if (sideTexture != null && net.justsomeswitches.util.BlockTextureAnalyzer.isValidTexture(sideTexture)) {
                return sideTexture;
            }

            return blockInfo.getFaceTextures().values().stream()
                    .filter(net.justsomeswitches.util.BlockTextureAnalyzer::isValidTexture)
                    .findFirst()
                    .orElse(null);
        } else if (selection == FaceOption.SIDE) {
            String sideTexture = blockInfo.getTextureForFace(Direction.NORTH);
            if (sideTexture != null && net.justsomeswitches.util.BlockTextureAnalyzer.isValidTexture(sideTexture)) {
                return sideTexture;
            }
        } else if (selection == FaceOption.END) {
            String topTexture = blockInfo.getTextureForFace(Direction.UP);
            if (topTexture != null && net.justsomeswitches.util.BlockTextureAnalyzer.isValidTexture(topTexture)) {
                return topTexture;
            }
        } else if (selection.getDirection() != null) {
            String faceTexture = blockInfo.getTextureForFace(selection.getDirection());
            if (faceTexture != null && net.justsomeswitches.util.BlockTextureAnalyzer.isValidTexture(faceTexture)) {
                return faceTexture;
            }
        }

        return null;
    }

    @Nonnull
    public static DropdownState createDisabledState() {
        return new DropdownState(false, List.of(FaceOption.ALL), FaceOption.ALL, null);
    }

    @Nullable
    private static FaceOption findSimilarOption(@Nonnull FaceOption userSelection, @Nonnull List<FaceOption> availableOptions) {
        return switch (userSelection) {
            case END -> availableOptions.contains(FaceOption.TOP) ? FaceOption.TOP :
                    availableOptions.contains(FaceOption.BOTTOM) ? FaceOption.BOTTOM : null;
            case TOP -> availableOptions.contains(FaceOption.END) ? FaceOption.END : null;
            case BOTTOM -> availableOptions.contains(FaceOption.END) ? FaceOption.END : null;
            case SIDE -> availableOptions.contains(FaceOption.NORTH) ? FaceOption.NORTH :
                    availableOptions.contains(FaceOption.FRONT) ? FaceOption.FRONT : null;
            case FRONT -> availableOptions.contains(FaceOption.SIDE) ? FaceOption.SIDE :
                    availableOptions.contains(FaceOption.NORTH) ? FaceOption.NORTH : null;
            case NORTH, SOUTH, EAST, WEST -> availableOptions.contains(FaceOption.SIDE) ? FaceOption.SIDE : null;
            default -> null;
        };
    }

    public static void clearCaches() {
        dropdownStateCache.clear();
    }
}