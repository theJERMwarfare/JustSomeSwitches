package net.justsomeswitches.gui;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Face selection data management for dynamic dropdown system
 * ---
 * Phase 4B: Enhanced face selection with silent operation for performance
 */
public class FaceSelectionData {

    /**
     * Face selection options - includes ALL plus specific faces
     */
    public enum FaceOption {
        ALL("All", "all"),
        TOP("Top", "top", Direction.UP),
        BOTTOM("Bottom", "bottom", Direction.DOWN),
        NORTH("North", "north", Direction.NORTH),
        SOUTH("South", "south", Direction.SOUTH),
        EAST("East", "east", Direction.EAST),
        WEST("West", "west", Direction.WEST);

        private final String displayName;
        private final String serializedName;
        private final Direction direction;

        FaceOption(String displayName, String serializedName) {
            this.displayName = displayName;
            this.serializedName = serializedName;
            this.direction = null;
        }

        FaceOption(String displayName, String serializedName, Direction direction) {
            this.displayName = displayName;
            this.serializedName = serializedName;
            this.direction = direction;
        }

        @Nonnull
        public String getDisplayName() { return displayName; }

        @Nonnull
        public String getSerializedName() { return serializedName; }

        @Nullable
        public Direction getDirection() { return direction; }

        public boolean isAll() { return this == ALL; }

        @Nonnull
        public static FaceOption fromSerializedName(@Nonnull String name) {
            for (FaceOption option : values()) {
                if (option.serializedName.equals(name)) {
                    return option;
                }
            }
            return ALL; // Default fallback
        }

        @Nonnull
        public static FaceOption fromDirection(@Nonnull Direction direction) {
            for (FaceOption option : values()) {
                if (option.direction == direction) {
                    return option;
                }
            }
            return ALL; // Fallback
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
     * Create dropdown state based on block analysis - OPTIMIZED with silent operation
     */
    @Nonnull
    public static DropdownState createDropdownState(@Nonnull net.justsomeswitches.util.BlockTextureAnalyzer.BlockTextureInfo blockInfo,
                                                    @Nonnull FaceOption currentSelection) {

        // Always include ALL option as first choice
        List<FaceOption> availableOptions = new ArrayList<>();
        availableOptions.add(FaceOption.ALL); // Always include "All"

        if (blockInfo.hasMultipleFaceTextures()) {
            // Add specific faces that have different textures
            for (Direction face : blockInfo.getAvailableFaces()) {
                FaceOption faceOption = FaceOption.fromDirection(face);
                if (faceOption != FaceOption.ALL && !availableOptions.contains(faceOption)) {
                    availableOptions.add(faceOption);
                }
            }

            // If we only have one face available, add common options for multi-face blocks
            if (availableOptions.size() == 1) {
                // For blocks like logs, add both Top and Side options
                if (hasTopBottomDifference(blockInfo)) {
                    availableOptions.add(FaceOption.TOP);
                    availableOptions.add(FaceOption.NORTH); // Represent side faces
                }
            }
        }

        // Determine if dropdown should be enabled
        boolean shouldEnable = blockInfo.hasMultipleFaceTextures() && availableOptions.size() > 1;

        // Determine preview texture based on current selection
        String previewTexture = getPreviewTexture(blockInfo, currentSelection);

        return new DropdownState(shouldEnable, availableOptions, currentSelection, previewTexture);
    }

    /**
     * Check if block has top/bottom texture differences (like logs)
     */
    private static boolean hasTopBottomDifference(@Nonnull net.justsomeswitches.util.BlockTextureAnalyzer.BlockTextureInfo blockInfo) {
        String topTexture = blockInfo.getTextureForFace(Direction.UP);
        String sideTexture = blockInfo.getTextureForFace(Direction.NORTH);

        return topTexture != null && sideTexture != null && !topTexture.equals(sideTexture);
    }

    /**
     * Get preview texture based on current selection and block info - OPTIMIZED
     */
    @Nullable
    private static String getPreviewTexture(@Nonnull net.justsomeswitches.util.BlockTextureAnalyzer.BlockTextureInfo blockInfo,
                                            @Nonnull FaceOption selection) {
        if (selection.isAll()) {
            // For "All", try to use the most appropriate texture
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
     * Get texture path for a specific face selection - OPTIMIZED
     */
    @Nullable
    public static String getTextureForSelection(@Nonnull net.justsomeswitches.util.BlockTextureAnalyzer.BlockTextureInfo blockInfo,
                                                @Nonnull FaceOption selection) {
        if (selection.isAll()) {
            // For "All", return most appropriate texture
            if (blockInfo.getUniformTexture() != null) {
                return blockInfo.getUniformTexture();
            }

            // For multi-face blocks, prefer side texture
            String sideTexture = blockInfo.getTextureForFace(Direction.NORTH);
            if (sideTexture != null && net.justsomeswitches.util.BlockTextureAnalyzer.isValidTexture(sideTexture)) {
                return sideTexture;
            }

            // Fallback to any valid texture
            return blockInfo.getFaceTextures().values().stream()
                    .filter(net.justsomeswitches.util.BlockTextureAnalyzer::isValidTexture)
                    .findFirst()
                    .orElse(null);
        } else if (selection.getDirection() != null) {
            // For specific face, return that face's texture
            String faceTexture = blockInfo.getTextureForFace(selection.getDirection());
            if (faceTexture != null && net.justsomeswitches.util.BlockTextureAnalyzer.isValidTexture(faceTexture)) {
                return faceTexture;
            }
        }

        return null;
    }

    /**
     * Get effective texture path for model application
     */
    @Nonnull
    public static String getEffectiveTexturePath(@Nonnull String baseTexturePath, @Nonnull FaceOption faceSelection) {
        if (faceSelection.isAll() || faceSelection.getDirection() == null) {
            return baseTexturePath;
        }

        // For specific face selections, try to construct face-specific texture path
        Direction face = faceSelection.getDirection();

        // Handle common face texture patterns
        if (face == Direction.UP && !baseTexturePath.contains("_top")) {
            String topTexture = baseTexturePath + "_top";
            if (net.justsomeswitches.util.BlockTextureAnalyzer.isValidTexture(topTexture)) {
                return topTexture;
            }
        }

        if ((face == Direction.NORTH || face == Direction.SOUTH || face == Direction.EAST || face == Direction.WEST)
                && !baseTexturePath.contains("_side")) {
            String sideTexture = baseTexturePath + "_side";
            if (net.justsomeswitches.util.BlockTextureAnalyzer.isValidTexture(sideTexture)) {
                return sideTexture;
            }
        }

        // Return base texture if no face-specific variant exists
        return baseTexturePath;
    }
}