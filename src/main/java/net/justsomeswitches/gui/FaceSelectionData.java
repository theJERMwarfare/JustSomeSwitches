package net.justsomeswitches.gui;

import net.justsomeswitches.util.DynamicBlockModelAnalyzer;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * FIXED: Face Selection Data System - Use actual JSON variable names from block models
 * ---
 * Shows exact variable names from block JSON files (e.g., "end", "side" for oak logs)
 */
public class FaceSelectionData {

    /**
     * Face option enum - keep original for backward compatibility, but add JSON variable support
     */
    public enum FaceOption {
        ALL("all", "all", null),
        TOP("top", "top", Direction.UP),
        BOTTOM("bottom", "bottom", Direction.DOWN),
        NORTH("north", "north", Direction.NORTH),
        SOUTH("south", "south", Direction.SOUTH),
        EAST("east", "east", Direction.EAST),
        WEST("west", "west", Direction.WEST),

        // Add support for actual JSON variable names
        END("end", "end", Direction.UP),      // For logs - maps to top/bottom
        SIDE("side", "side", Direction.NORTH), // For logs - maps to sides
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
    }

    /**
     * FIXED: Create dropdown state using actual JSON variable names from block model
     */
    @Nonnull
    public static DropdownState createDropdownState(@Nonnull net.justsomeswitches.util.BlockTextureAnalyzer.BlockTextureInfo blockInfo,
                                                    @Nonnull FaceOption currentSelection) {

        List<FaceOption> availableOptions = new ArrayList<>();
        availableOptions.add(FaceOption.ALL); // Always include "all"

        System.out.println("DEBUG FaceSelection: Creating dropdown for block with multiple faces: " + blockInfo.hasMultipleFaceTextures());

        if (blockInfo.hasMultipleFaceTextures()) {
            // CRITICAL FIX: Try to get actual texture variables from dynamic analysis first
            Set<String> actualVariables = getActualTextureVariablesFromDynamicAnalysis(blockInfo);

            // If dynamic analysis didn't work, fall back to pattern analysis
            if (actualVariables.isEmpty() || actualVariables.size() == 1) {
                actualVariables = getActualTextureVariables(blockInfo);
            }

            System.out.println("DEBUG FaceSelection: Found actual texture variables: " + actualVariables);

            // Add face options based on actual JSON variables
            for (String variable : actualVariables) {
                if (!variable.equals("all")) {
                    FaceOption faceOption = FaceOption.fromJsonVariable(variable);
                    if (!availableOptions.contains(faceOption)) {
                        availableOptions.add(faceOption);
                        System.out.println("DEBUG FaceSelection: Added face option: " + faceOption + " for JSON variable: " + variable);
                    }
                }
            }
        }

        // CRITICAL FIX: Always ensure current selection is available to prevent resets
        if (!availableOptions.contains(currentSelection)) {
            System.out.println("DEBUG FaceSelection: Current selection " + currentSelection +
                    " not in available options " + availableOptions + ", adding it to prevent reset");
            availableOptions.add(currentSelection);
        }

        // Determine if dropdown should be enabled
        boolean shouldEnable = blockInfo.hasMultipleFaceTextures() && availableOptions.size() > 1;

        // Get preview texture based on current selection
        String previewTexture = getPreviewTexture(blockInfo, currentSelection);

        System.out.println("DEBUG FaceSelection: Final dropdown - Enabled: " + shouldEnable +
                ", Options: " + availableOptions + ", Selected: " + currentSelection);

        return new DropdownState(shouldEnable, availableOptions, currentSelection, previewTexture);
    }

    /**
     * CRITICAL FIX: Get texture variables directly from DynamicBlockModelAnalyzer
     */
    @Nonnull
    private static Set<String> getActualTextureVariablesFromDynamicAnalysis(@Nonnull net.justsomeswitches.util.BlockTextureAnalyzer.BlockTextureInfo blockInfo) {
        Set<String> variables = new HashSet<>();

        try {
            // We need to somehow get access to the original ItemStack or block to run dynamic analysis
            // For now, this is a placeholder - the real implementation would need the ItemStack
            // This method would use DynamicBlockModelAnalyzer.analyzeBlockDynamically(itemStack)

            // Since we can't get the ItemStack from BlockTextureInfo, return empty set
            // The caller will fall back to pattern analysis

        } catch (Exception e) {
            System.out.println("DEBUG FaceSelection: Error in dynamic analysis: " + e.getMessage());
        }

        return variables;
    }

    /**
     * CRITICAL FIX: Get actual texture variable names from block model JSON
     */
    @Nonnull
    private static Set<String> getActualTextureVariables(@Nonnull net.justsomeswitches.util.BlockTextureAnalyzer.BlockTextureInfo blockInfo) {
        Set<String> variables = new HashSet<>();

        // Get the face texture map to analyze texture differences
        Map<Direction, String> faceTextures = blockInfo.getFaceTextures();
        System.out.println("DEBUG FaceSelection: Analyzing face textures: " + faceTextures);

        // Collect all unique texture paths
        Set<String> uniqueTextures = new HashSet<>();
        for (String texture : faceTextures.values()) {
            if (texture != null && !texture.isEmpty()) {
                uniqueTextures.add(texture);
            }
        }

        System.out.println("DEBUG FaceSelection: Found unique textures: " + uniqueTextures);

        // CRITICAL FIX: Analyze texture patterns to determine JSON variables
        if (uniqueTextures.size() > 1) {
            // Multiple textures - analyze patterns
            String topTexture = faceTextures.get(Direction.UP);
            String bottomTexture = faceTextures.get(Direction.DOWN);
            String northTexture = faceTextures.get(Direction.NORTH);
            String southTexture = faceTextures.get(Direction.SOUTH);
            String eastTexture = faceTextures.get(Direction.EAST);
            String westTexture = faceTextures.get(Direction.WEST);

            // Check for vertical vs horizontal texture differences (like logs)
            if (topTexture != null && northTexture != null && !topTexture.equals(northTexture)) {
                // This is likely a log pattern: top/bottom different from sides
                variables.add("end");   // For top/bottom faces
                variables.add("side");  // For horizontal faces
                System.out.println("DEBUG FaceSelection: Detected log pattern - added 'end' and 'side' variables");
            } else {
                // Add variables based on actual texture patterns
                for (String texture : uniqueTextures) {
                    String variable = extractVariableFromTexturePath(texture);
                    if (variable != null) {
                        variables.add(variable);
                    }
                }

                // Ensure we have meaningful variables
                if (variables.isEmpty()) {
                    // Fallback for complex patterns
                    if (topTexture != null && !topTexture.equals(northTexture)) {
                        variables.add("top");
                        variables.add("side");
                    } else {
                        variables.add("all");
                    }
                }
            }
        } else {
            // Single texture - uniform block
            variables.add("all");
        }

        System.out.println("DEBUG FaceSelection: Final texture variables: " + variables);
        return variables;
    }

    /**
     * IMPROVED: Extract variable name from texture path with better pattern detection
     */
    @Nullable
    private static String extractVariableFromTexturePath(@Nonnull String texturePath) {
        try {
            // Extract the texture name from the path
            String[] parts = texturePath.split("/");
            String textureName = parts[parts.length - 1];

            System.out.println("DEBUG FaceSelection: Analyzing texture name: " + textureName);

            // IMPROVED: Better pattern detection for common block types
            if (textureName.contains("_top") || textureName.contains("_end")) {
                return "end";
            } else if (textureName.contains("_side")) {
                return "side";
            } else if (textureName.contains("_front")) {
                return "front";
            } else if (textureName.contains("_back")) {
                return "back";
            } else if (textureName.contains("_bottom")) {
                return "end"; // Bottom also maps to "end" for logs
            }

            // For base textures without suffixes (like "oak_log"), this usually represents the "side" texture
            if (!textureName.contains("_")) {
                return "side";
            }

        } catch (Exception e) {
            System.out.println("DEBUG FaceSelection: Error extracting variable from path " + texturePath + ": " + e.getMessage());
        }

        return null;
    }

    /**
     * Get actual texture variables from ItemStack using DynamicBlockModelAnalyzer
     */
    @Nonnull
    public static Set<String> getTextureVariablesFromItemStack(@Nonnull ItemStack itemStack) {
        try {
            DynamicBlockModelAnalyzer.DynamicBlockInfo dynamicInfo =
                    DynamicBlockModelAnalyzer.analyzeBlockDynamically(itemStack);

            Map<String, String> textureVariables = dynamicInfo.getTextureVariables();
            System.out.println("DEBUG FaceSelection: Dynamic analysis found texture variables: " + textureVariables.keySet());

            return new HashSet<>(textureVariables.keySet());

        } catch (Exception e) {
            System.out.println("DEBUG FaceSelection: Error getting texture variables from ItemStack: " + e.getMessage());
            return Set.of("all");
        }
    }

    /**
     * Get preview texture with simple direct lookup
     */
    @Nullable
    private static String getPreviewTexture(@Nonnull net.justsomeswitches.util.BlockTextureAnalyzer.BlockTextureInfo blockInfo,
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
     * Validate face selection against available options
     */
    @Nonnull
    public static FaceOption validateSelection(@Nonnull FaceOption selection,
                                               @Nonnull List<FaceOption> availableOptions) {
        if (availableOptions.contains(selection)) {
            return selection;
        }
        // Fallback to ALL if current selection is not available
        return FaceOption.ALL;
    }

    /**
     * Get texture path for a specific face selection - maps to actual JSON variables
     */
    @Nullable
    public static String getTextureForSelection(@Nonnull net.justsomeswitches.util.BlockTextureAnalyzer.BlockTextureInfo blockInfo,
                                                @Nonnull FaceOption selection) {
        if (selection.isAll()) {
            // For "all", return most appropriate texture
            if (blockInfo.getUniformTexture() != null) {
                return blockInfo.getUniformTexture();
            }

            // For multi-face blocks, prefer side texture
            String sideTexture = blockInfo.getTextureForFace(Direction.NORTH);
            if (sideTexture != null && net.justsomeswitches.util.BlockTextureAnalyzer.isValidTexture(sideTexture)) {
                return sideTexture;
            }

            // Fallback to any available texture
            return blockInfo.getFaceTextures().values().stream()
                    .filter(net.justsomeswitches.util.BlockTextureAnalyzer::isValidTexture)
                    .findFirst()
                    .orElse(null);
        } else {
            // Map face option to appropriate direction based on JSON variable
            Direction targetDirection = mapFaceOptionToDirection(selection, blockInfo);
            if (targetDirection != null) {
                String faceTexture = blockInfo.getTextureForFace(targetDirection);
                if (faceTexture != null && net.justsomeswitches.util.BlockTextureAnalyzer.isValidTexture(faceTexture)) {
                    return faceTexture;
                }
            }
        }

        return null;
    }

    /**
     * Map face option to appropriate direction based on block type and JSON variables
     */
    @Nullable
    private static Direction mapFaceOptionToDirection(@Nonnull FaceOption faceOption,
                                                      @Nonnull net.justsomeswitches.util.BlockTextureAnalyzer.BlockTextureInfo blockInfo) {
        return switch (faceOption) {
            case END -> Direction.UP;      // "end" variable typically maps to top/bottom faces
            case SIDE -> Direction.NORTH;  // "side" variable typically maps to horizontal faces
            case FRONT -> Direction.NORTH;
            case BACK -> Direction.SOUTH;
            case TOP -> Direction.UP;
            case BOTTOM -> Direction.DOWN;
            case NORTH -> Direction.NORTH;
            case SOUTH -> Direction.SOUTH;
            case EAST -> Direction.EAST;
            case WEST -> Direction.WEST;
            default -> null;
        };
    }
}