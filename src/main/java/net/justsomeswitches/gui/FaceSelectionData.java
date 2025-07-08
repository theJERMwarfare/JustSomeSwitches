package net.justsomeswitches.gui;

import net.justsomeswitches.config.DebugConfig;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * FRAMED BLOCKS APPROACH: Bulletproof face selection with enhanced serialization
 */
public class FaceSelectionData {

    // Minimal caching to prevent stale state
    private static final Map<String, DropdownState> dropdownStateCache = new HashMap<>();

    /**
     * BULLETPROOF: Enhanced face option enum with guaranteed serialization safety
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

        // JSON variable names (critical for logs and other blocks)
        END("end", "Ends", Direction.UP),
        SIDE("side", "Sides", Direction.NORTH),
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
         * BULLETPROOF: Enhanced serialization with comprehensive fallback handling
         */
        @Nonnull
        public static FaceOption fromSerializedName(@Nonnull String name) {
            if (name == null) {
                DebugConfig.logCritical("Null face selection name!");
                return ALL;
            }

            String cleanName = name.trim();
            if (cleanName.isEmpty()) {
                DebugConfig.logValidationFailure("Face selection name", "non-empty", "empty");
                return ALL;
            }

            // Exact match (primary path)
            for (FaceOption option : values()) {
                if (option.serializedName.equals(cleanName)) {
                    return option;
                }
            }

            // Case-insensitive fallback
            for (FaceOption option : values()) {
                if (option.serializedName.equalsIgnoreCase(cleanName)) {
                    DebugConfig.logValidationFailure("Face selection case", option.serializedName, cleanName);
                    return option;
                }
            }

            // Log unknown names for debugging
            DebugConfig.logValidationFailure("Face selection unknown", "known option", cleanName);
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
            if (variableName == null || variableName.trim().isEmpty()) {
                return ALL;
            }

            // Direct match first
            for (FaceOption option : values()) {
                if (option.serializedName.equals(variableName)) {
                    return option;
                }
            }

            // Fallback mapping
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
                default -> {
                    DebugConfig.logValidationFailure("JSON variable unknown", "known variable", variableName);
                    yield ALL;
                }
            };
        }

        /**
         * VALIDATION: Ensure option is not null and valid
         */
        @Nonnull
        public static FaceOption validate(@Nullable FaceOption option) {
            if (option == null) {
                DebugConfig.logCritical("Null FaceOption detected - using ALL as fallback");
                return ALL;
            }
            return option;
        }
    }

    /**
     * Dropdown state information with enhanced validation
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
            this.selectedOption = FaceOption.validate(selectedOption);
            this.previewTexture = previewTexture;

            // Validation
            if (!this.availableOptions.contains(this.selectedOption)) {
                DebugConfig.logValidationFailure("Dropdown selected option",
                        "option in available list", this.selectedOption.toString());
            }
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
     * BULLETPROOF: Enhanced dropdown state creation with absolute preservation guarantee
     */
    @Nonnull
    public static DropdownState createDropdownState(@Nonnull net.justsomeswitches.util.BlockTextureAnalyzer.BlockTextureInfo blockInfo,
                                                    @Nonnull FaceSelectionData.FaceOption currentSelectedOption) {

        // Validate inputs
        FaceOption safeSelection = FaceOption.validate(currentSelectedOption);

        boolean hasMultipleFaces = blockInfo.hasMultipleFaceTextures();

        if (!hasMultipleFaces) {
            // Single texture block - use ALL but preserve user choice if it makes sense
            return new DropdownState(false, List.of(FaceOption.ALL), FaceOption.ALL, blockInfo.getUniformTexture());
        }

        // Multi-texture blocks - determine available options
        List<FaceOption> availableOptions = new ArrayList<>();
        List<String> textureVariables = getTextureVariables(blockInfo);

        // Convert texture variables to face options
        for (String variable : textureVariables) {
            FaceOption faceOption = FaceOption.fromJsonVariable(variable);
            if (!availableOptions.contains(faceOption)) {
                availableOptions.add(faceOption);
            }
        }

        // Ensure we have at least one option
        if (availableOptions.isEmpty()) {
            availableOptions.add(FaceOption.ALL);
        }

        // CRITICAL: Absolutely preserve user's selection if possible
        FaceOption finalSelection = preserveUserSelection(safeSelection, availableOptions);

        String previewTexture = getTextureForSelection(blockInfo, finalSelection);

        return new DropdownState(true, availableOptions, finalSelection, previewTexture);
    }

    /**
     * BULLETPROOF: Absolute user selection preservation - never changes unless impossible
     */
    @Nonnull
    private static FaceOption preserveUserSelection(@Nonnull FaceOption userSelection, @Nonnull List<FaceOption> availableOptions) {
        // PRIORITY 1: If user's exact selection is available, ALWAYS use it
        if (availableOptions.contains(userSelection)) {
            DebugConfig.logSuccess("Preserved exact user selection: " + userSelection);
            return userSelection;
        }

        // PRIORITY 2: Try to find semantically similar option
        FaceOption similar = findSimilarOption(userSelection, availableOptions);
        if (similar != null) {
            DebugConfig.logValidationFailure("User selection unavailable", userSelection.toString(), similar.toString());
            return similar;
        }

        // PRIORITY 3: Fallback to best available option
        FaceOption fallback = availableOptions.contains(FaceOption.ALL) ?
                FaceOption.ALL : availableOptions.get(0);

        DebugConfig.logValidationFailure("No similar option found", userSelection.toString(), fallback.toString());
        return fallback;
    }

    /**
     * Enhanced texture variable detection
     */
    @Nonnull
    private static List<String> getTextureVariables(@Nonnull net.justsomeswitches.util.BlockTextureAnalyzer.BlockTextureInfo blockInfo) {
        Map<Direction, String> faceTextures = blockInfo.getFaceTextures();
        Set<String> uniqueTextures = new HashSet<>(faceTextures.values());

        List<String> variables = new ArrayList<>();

        // Log pattern detection (most important case)
        if (uniqueTextures.size() == 2) {
            boolean hasLogPattern = uniqueTextures.stream().anyMatch(tex ->
                    tex.contains("_log") || tex.contains("log"));

            if (hasLogPattern) {
                variables.add("side");  // Side faces
                variables.add("end");   // Top/bottom faces
                return variables;
            }
        }

        // Other multi-texture patterns
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
        String path = texturePath.toLowerCase();

        if (path.contains("_top")) return "end";
        if (path.contains("_log") && !path.contains("_top")) return "side";
        if (path.contains("_front")) return "front";
        if (path.contains("_back")) return "back";
        if (path.contains("_side")) return "side";
        if (path.contains("_end")) return "end";

        return "side";  // Default for unknown patterns
    }

    /**
     * Enhanced texture mapping for selections
     */
    @Nullable
    public static String getTextureForSelection(@Nonnull net.justsomeswitches.util.BlockTextureAnalyzer.BlockTextureInfo blockInfo,
                                                @Nonnull FaceOption selection) {
        FaceOption safeSelection = FaceOption.validate(selection);

        if (safeSelection.isAll()) {
            // For ALL, try uniform texture first
            if (blockInfo.getUniformTexture() != null) {
                return blockInfo.getUniformTexture();
            }

            // Try side texture for ALL
            String sideTexture = blockInfo.getTextureForFace(Direction.NORTH);
            if (sideTexture != null && net.justsomeswitches.util.BlockTextureAnalyzer.isValidTexture(sideTexture)) {
                return sideTexture;
            }

            // Fallback to any valid texture
            return blockInfo.getFaceTextures().values().stream()
                    .filter(net.justsomeswitches.util.BlockTextureAnalyzer::isValidTexture)
                    .findFirst()
                    .orElse(null);
        }

        // Handle specific face selections
        if (safeSelection == FaceOption.SIDE) {
            String sideTexture = blockInfo.getTextureForFace(Direction.NORTH);
            if (sideTexture != null && net.justsomeswitches.util.BlockTextureAnalyzer.isValidTexture(sideTexture)) {
                return sideTexture;
            }
        }

        if (safeSelection == FaceOption.END) {
            String topTexture = blockInfo.getTextureForFace(Direction.UP);
            if (topTexture != null && net.justsomeswitches.util.BlockTextureAnalyzer.isValidTexture(topTexture)) {
                return topTexture;
            }
        }

        if (safeSelection.getDirection() != null) {
            String faceTexture = blockInfo.getTextureForFace(safeSelection.getDirection());
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

    /**
     * Enhanced similarity matching
     */
    @Nullable
    private static FaceOption findSimilarOption(@Nonnull FaceOption userSelection, @Nonnull List<FaceOption> availableOptions) {
        return switch (userSelection) {
            case END -> {
                if (availableOptions.contains(FaceOption.TOP)) yield FaceOption.TOP;
                if (availableOptions.contains(FaceOption.BOTTOM)) yield FaceOption.BOTTOM;
                yield null;
            }
            case TOP, BOTTOM -> availableOptions.contains(FaceOption.END) ? FaceOption.END : null;
            case SIDE -> {
                if (availableOptions.contains(FaceOption.NORTH)) yield FaceOption.NORTH;
                if (availableOptions.contains(FaceOption.FRONT)) yield FaceOption.FRONT;
                yield null;
            }
            case FRONT -> {
                if (availableOptions.contains(FaceOption.SIDE)) yield FaceOption.SIDE;
                if (availableOptions.contains(FaceOption.NORTH)) yield FaceOption.NORTH;
                yield null;
            }
            case NORTH, SOUTH, EAST, WEST -> availableOptions.contains(FaceOption.SIDE) ? FaceOption.SIDE : null;
            default -> null;
        };
    }

    /**
     * Clear caches to prevent stale state
     */
    public static void clearCaches() {
        dropdownStateCache.clear();
    }
}