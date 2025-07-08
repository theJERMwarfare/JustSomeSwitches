package net.justsomeswitches.gui;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * FRAMED BLOCKS SOLUTION: Enhanced Face Selection Data System with robust persistence
 */
public class FaceSelectionData {

    // Cache to prevent repeated dropdown creation
    private static final Map<String, DropdownState> dropdownStateCache = new HashMap<>();
    private static final Map<ItemStack, String> blockAnalysisCache = new HashMap<>();

    /**
     * FRAMED BLOCKS PATTERN: Enhanced face option enum with robust serialization
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
         * FRAMED BLOCKS PATTERN: Enhanced serialization with robust error handling and debugging
         */
        @Nonnull
        public static FaceOption fromSerializedName(@Nonnull String name) {
            System.out.println("FRAMED SOLUTION: Converting serialized name: '" + name + "'");

            // Enhanced validation for empty/null names
            if (name == null || name.trim().isEmpty()) {
                System.out.println("FRAMED SOLUTION: Empty/null name, defaulting to ALL");
                return ALL;
            }

            // Enhanced matching with case-insensitive fallback
            for (FaceOption option : values()) {
                if (option.serializedName.equals(name)) {
                    System.out.println("FRAMED SOLUTION: Exact match found: " + option);
                    return option;
                }
            }

            // Case-insensitive fallback for robustness
            for (FaceOption option : values()) {
                if (option.serializedName.equalsIgnoreCase(name)) {
                    System.out.println("FRAMED SOLUTION: Case-insensitive match found: " + option + " for '" + name + "'");
                    return option;
                }
            }

            System.out.println("FRAMED SOLUTION: No match found for '" + name + "', defaulting to ALL");
            return ALL; // Robust fallback to ALL for invalid names
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
     * FRAMED BLOCKS PATTERN: Enhanced dropdown state creation with robust user selection preservation
     */
    @Nonnull
    public static DropdownState createDropdownState(@Nonnull net.justsomeswitches.util.BlockTextureAnalyzer.BlockTextureInfo blockInfo,
                                                    @Nonnull FaceSelectionData.FaceOption currentSelectedOption) {

        System.out.println("FRAMED SOLUTION: Creating dropdown state for selection: " + currentSelectedOption);

        boolean hasMultipleFaces = blockInfo.hasMultipleFaceTextures();

        if (!hasMultipleFaces) {
            System.out.println("FRAMED SOLUTION: Single texture block - disabled dropdown");
            return new DropdownState(false, List.of(FaceOption.ALL), FaceOption.ALL, null);
        }

        // For multi-texture blocks, determine actual available options
        List<FaceOption> availableOptions = new ArrayList<>();

        // Get texture variables from block model
        List<String> textureVariables = getTextureVariables(blockInfo);
        System.out.println("FRAMED SOLUTION: Found texture variables: " + textureVariables);

        // Convert texture variables to face options (preserve exact JSON variable names)
        for (String variable : textureVariables) {
            FaceOption faceOption = FaceOption.fromJsonVariable(variable);
            if (!availableOptions.contains(faceOption)) {
                availableOptions.add(faceOption);
                System.out.println("FRAMED SOLUTION: Added option: " + faceOption + " from variable: " + variable);
            }
        }

        // Only add ALL if no specific face options were found
        if (availableOptions.isEmpty()) {
            availableOptions.add(FaceOption.ALL);
            System.out.println("FRAMED SOLUTION: No specific options found - added ALL");
        }

        // CRITICAL: Preserve user's current selection using robust validation
        FaceOption validatedSelection = validateSelection(currentSelectedOption, availableOptions);
        System.out.println("FRAMED SOLUTION: Validated selection: " + currentSelectedOption + " → " + validatedSelection);

        // Create dropdown state with preserved user selection
        String previewTexture = getTextureForSelection(blockInfo, validatedSelection);
        System.out.println("FRAMED SOLUTION: Preview texture for " + validatedSelection + ": " + previewTexture);

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
     * FRAMED BLOCKS PATTERN: Enhanced validation that strongly preserves user selections
     */
    @Nonnull
    public static FaceOption validateSelection(@Nonnull FaceOption selection,
                                               @Nonnull List<FaceOption> availableOptions) {
        System.out.println("FRAMED SOLUTION: Validating selection '" + selection + "' against options: " +
                availableOptions.stream().map(FaceOption::getSerializedName).toList());

        // PRIORITY 1: Preserve user's exact selection if available
        if (availableOptions.contains(selection)) {
            System.out.println("FRAMED SOLUTION: User selection '" + selection + "' is valid - preserving");
            return selection;
        }

        // PRIORITY 2: If user had a specific face but options changed, try to find similar option
        if (selection != FaceOption.ALL) {
            // Try to find a semantically similar option
            FaceOption similar = findSimilarOption(selection, availableOptions);
            if (similar != null) {
                System.out.println("FRAMED SOLUTION: Found similar option '" + similar + "' for user selection '" + selection + "'");
                return similar;
            }
        }

        // PRIORITY 3: Fallback to ALL if available (most common fallback)
        if (availableOptions.contains(FaceOption.ALL)) {
            System.out.println("FRAMED SOLUTION: Falling back to ALL for invalid selection '" + selection + "'");
            return FaceOption.ALL;
        }

        // PRIORITY 4: Last resort - return first available option
        FaceOption fallback = availableOptions.isEmpty() ? FaceOption.ALL : availableOptions.get(0);
        System.out.println("FRAMED SOLUTION: Last resort fallback to '" + fallback + "' for selection '" + selection + "'");
        return fallback;
    }

    /**
     * FRAMED BLOCKS PATTERN: Find semantically similar face option for better user experience
     */
    @Nullable
    private static FaceOption findSimilarOption(@Nonnull FaceOption userSelection, @Nonnull List<FaceOption> availableOptions) {
        // Map similar concepts
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

    /**
     * Clear caches (for memory management)
     */
    public static void clearCaches() {
        dropdownStateCache.clear();
        blockAnalysisCache.clear();
    }
}