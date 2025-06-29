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
 * Phase 4B: Advanced face selection with dynamic options based on block analysis
 */
public class FaceSelectionData {

    /**
     * Face selection options - includes ALL plus specific faces
     */
    public enum FaceOption {
        ALL("All Faces", "all"),
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
     * Create dropdown state based on block analysis
     */
    @Nonnull
    public static DropdownState createDropdownState(@Nonnull net.justsomeswitches.util.BlockTextureAnalyzer.BlockTextureInfo blockInfo,
                                                    @Nonnull FaceOption currentSelection) {

        if (!blockInfo.shouldEnableDropdown()) {
            // Dropdown should be disabled - no multiple face textures
            System.out.println("Phase 4B Debug: Dropdown disabled - no multiple face textures");
            return new DropdownState(false, List.of(FaceOption.ALL), FaceOption.ALL, blockInfo.getUniformTexture());
        }

        // Build available options based on block analysis
        List<FaceOption> availableOptions = new ArrayList<>();
        availableOptions.add(FaceOption.ALL); // Always include "All"

        // Add specific faces that have different textures
        for (Direction face : blockInfo.getAvailableFaces()) {
            FaceOption faceOption = FaceOption.fromDirection(face);
            if (faceOption != FaceOption.ALL) {
                availableOptions.add(faceOption);
            }
        }

        // Determine preview texture based on current selection
        String previewTexture = getPreviewTexture(blockInfo, currentSelection);

        System.out.println("Phase 4B Debug: Dropdown enabled with " + availableOptions.size() +
                " options, preview: " + previewTexture);

        return new DropdownState(true, availableOptions, currentSelection, previewTexture);
    }

    /**
     * Get preview texture based on current selection and block info
     */
    @Nullable
    private static String getPreviewTexture(@Nonnull net.justsomeswitches.util.BlockTextureAnalyzer.BlockTextureInfo blockInfo,
                                            @Nonnull FaceOption selection) {
        if (selection.isAll()) {
            // For "All", use the most common texture or first available
            return blockInfo.getUniformTexture() != null ?
                    blockInfo.getUniformTexture() :
                    blockInfo.getFaceTextures().values().stream().findFirst().orElse(null);
        } else if (selection.getDirection() != null) {
            // For specific face, get that face's texture
            return blockInfo.getTextureForFace(selection.getDirection());
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
     * Get texture path for a specific face selection
     */
    @Nullable
    public static String getTextureForSelection(@Nonnull net.justsomeswitches.util.BlockTextureAnalyzer.BlockTextureInfo blockInfo,
                                                @Nonnull FaceOption selection) {
        if (selection.isAll()) {
            // For "All", return uniform texture or fallback
            return blockInfo.getUniformTexture();
        } else if (selection.getDirection() != null) {
            // For specific face, return that face's texture
            return blockInfo.getTextureForFace(selection.getDirection());
        }

        return null;
    }
}