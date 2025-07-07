package net.justsomeswitches.gui;

import net.justsomeswitches.util.DynamicBlockModelAnalyzer;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * FIXED: Face Selection Data System with corrected dropdown logic
 * ---
 * CRITICAL FIX: Eliminates "ALL" option for multi-texture blocks like oak logs
 */
public class FaceSelectionData {

    // Enable debug only for critical issues
    private static final boolean DEBUG_ENABLED = false;

    // Cache to prevent repeated dropdown creation debug spam
    private static final Map<String, DropdownState> dropdownStateCache = new HashMap<>();
    private static final Map<ItemStack, String> blockAnalysisCache = new HashMap<>();

    /**
     * Face option enum - keep original for backward compatibility, but add JSON variable support
     */
    public enum FaceOption {
        ALL("all", "All Faces", null),
        TOP("top", "Top", Direction.UP),
        BOTTOM("bottom", "Bottom", Direction.DOWN),
        NORTH("north", "North", Direction.NORTH),
        SOUTH("south", "South", Direction.SOUTH),
        EAST("east", "East", Direction.EAST),
        WEST("west", "West", Direction.WEST),

        // Add support for actual JSON variable names
        END("end", "Ends", Direction.UP),      // For logs - maps to top/bottom
        SIDE("side", "Sides", Direction.NORTH), // For logs - maps to sides
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
     * FIXED: Create dropdown state with corrected logic for multi-texture blocks
     */
    @Nonnull
    public static DropdownState createDropdownState(@Nonnull net.justsomeswitches.util.BlockTextureAnalyzer.BlockTextureInfo blockInfo,
                                                    @Nonnull FaceOption selectedOption) {
        // Create cache key
        String cacheKey = blockInfo.toString() + "_" + selectedOption.getSerializedName();

        // Check cache first
        DropdownState cached = dropdownStateCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        boolean hasMultipleFaces = blockInfo.hasMultipleFaceTextures();

        if (DEBUG_ENABLED) {
            System.out.println("DEBUG FaceSelection: Creating dropdown for block with multiple faces: " + hasMultipleFaces);
        }

        if (!hasMultipleFaces) {
            // Single texture blocks get disabled dropdown with ALL option
            DropdownState disabled = new DropdownState(false, List.of(FaceOption.ALL), FaceOption.ALL, null);
            dropdownStateCache.put(cacheKey, disabled);
            return disabled;
        }

        // FIXED: For multi-texture blocks, determine actual available options
        List<FaceOption> availableOptions = new ArrayList<>();

        // Get texture variables from block model
        List<String> textureVariables = getTextureVariables(blockInfo);

        if (DEBUG_ENABLED && !textureVariables.isEmpty()) {
            System.out.println("DEBUG FaceSelection: Found texture variables: " + textureVariables);
        }

        // Convert texture variables to face options
        for (String variable : textureVariables) {
            FaceOption faceOption = FaceOption.fromJsonVariable(variable);
            if (!availableOptions.contains(faceOption)) {
                availableOptions.add(faceOption);
                if (DEBUG_ENABLED) {
                    System.out.println("DEBUG FaceSelection: Added face option: " + faceOption + " for JSON variable: " + variable);
                }
            }
        }

        // FIXED: Only add ALL if no specific face options were found
        if (availableOptions.isEmpty()) {
            availableOptions.add(FaceOption.ALL);
        }

        // FIXED: For oak logs and similar blocks, set default to SIDE if selectedOption is ALL
        FaceOption validatedSelection = validateSelection(selectedOption, availableOptions);
        if (validatedSelection == FaceOption.ALL && availableOptions.contains(FaceOption.SIDE)) {
            validatedSelection = FaceOption.SIDE;
        }

        // Create dropdown state
        String previewTexture = getTextureForSelection(blockInfo, validatedSelection);
        DropdownState result = new DropdownState(true, availableOptions, validatedSelection, previewTexture);

        // Cache the result
        dropdownStateCache.put(cacheKey, result);

        if (DEBUG_ENABLED) {
            System.out.println("DEBUG FaceSelection: Final dropdown - Enabled: " + result.isEnabled() +
                    ", Options: " + availableOptions + ", Selected: " + validatedSelection);
        }

        return result;
    }

    /**
     * FIXED: Get texture variables with better log pattern detection
     */
    @Nonnull
    private static List<String> getTextureVariables(@Nonnull net.justsomeswitches.util.BlockTextureAnalyzer.BlockTextureInfo blockInfo) {
        // Check for common patterns first - FIXED: Use correct Direction->String map type
        Map<Direction, String> faceTextures = blockInfo.getFaceTextures();
        Set<String> uniqueTextures = new HashSet<>(faceTextures.values());

        List<String> variables = new ArrayList<>();

        // FIXED: Detect log pattern more accurately
        if (uniqueTextures.size() == 2) {
            boolean hasLogPattern = uniqueTextures.stream().anyMatch(tex ->
                    tex.contains("_log"));

            if (hasLogPattern) {
                variables.add("side");
                variables.add("end");
                if (DEBUG_ENABLED) {
                    System.out.println("DEBUG FaceSelection: Detected log pattern - added 'side' and 'end' variables");
                }
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

        if (DEBUG_ENABLED && !variables.isEmpty()) {
            System.out.println("DEBUG FaceSelection: Found actual texture variables: " + variables);
        }

        return variables;
    }

    /**
     * Extract variable name from texture path
     */
    @Nonnull
    private static String extractVariableFromTexture(@Nonnull String texturePath) {
        if (texturePath.contains("_top")) return "end";
        if (texturePath.contains("_log") && !texturePath.contains("_top")) return "side";
        if (texturePath.contains("_front")) return "front";
        if (texturePath.contains("_back")) return "back";
        if (texturePath.contains("_side")) return "side";
        return "side"; // Default for unrecognized patterns
    }

    /**
     * FIXED: Get texture path for a specific face selection - improved mapping
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
            // FIXED: For SIDE selection, get side texture specifically
            String sideTexture = blockInfo.getTextureForFace(Direction.NORTH);
            if (sideTexture != null && net.justsomeswitches.util.BlockTextureAnalyzer.isValidTexture(sideTexture)) {
                return sideTexture;
            }
        } else if (selection == FaceOption.END) {
            // FIXED: For END selection, get top texture specifically
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
     * FIXED: Validate face selection against available options with better defaults
     */
    @Nonnull
    public static FaceOption validateSelection(@Nonnull FaceOption selection,
                                               @Nonnull List<FaceOption> availableOptions) {
        if (availableOptions.contains(selection)) {
            return selection;
        }

        // FIXED: Better fallback logic
        if (availableOptions.contains(FaceOption.SIDE)) {
            return FaceOption.SIDE; // Prefer SIDE for logs
        }
        if (availableOptions.contains(FaceOption.ALL)) {
            return FaceOption.ALL;
        }

        // Return first available option
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