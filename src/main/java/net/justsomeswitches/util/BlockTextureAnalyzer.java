package net.justsomeswitches.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * Block texture analyzer using dynamic model reading system
 * ---
 * Phase 4B: Streamlined system using DynamicBlockModelAnalyzer for universal compatibility
 */
public class BlockTextureAnalyzer {

    /**
     * Result of block texture analysis (converted from dynamic system)
     */
    public static class BlockTextureInfo {
        private final boolean hasMultipleFaceTextures;
        private final Map<Direction, String> faceTextures;
        private final List<Direction> availableFaces;
        private final String uniformTexture;

        public BlockTextureInfo(boolean hasMultipleFaceTextures,
                                Map<Direction, String> faceTextures,
                                @Nullable String uniformTexture) {
            this.hasMultipleFaceTextures = hasMultipleFaceTextures;
            this.faceTextures = new HashMap<>(faceTextures);
            this.uniformTexture = uniformTexture;
            this.availableFaces = new ArrayList<>();

            // Create available faces list for compatibility
            if (hasMultipleFaceTextures) {
                Set<String> uniqueTextures = new HashSet<>(faceTextures.values());
                if (uniqueTextures.size() > 1) {
                    // Add representative faces
                    faceTextures.entrySet().stream()
                            .collect(HashMap<String, List<Direction>>::new,
                                    (m, e) -> m.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey()),
                                    (m1, m2) -> m2.forEach((k, v) -> m1.merge(k, v, (v1, v2) -> { v1.addAll(v2); return v1; })))
                            .values()
                            .forEach(faces -> {
                                if (!availableFaces.contains(faces.get(0))) {
                                    availableFaces.add(faces.get(0));
                                }
                            });
                }
            }
        }

        public boolean hasMultipleFaceTextures() { return hasMultipleFaceTextures; }
        public Map<Direction, String> getFaceTextures() { return new HashMap<>(faceTextures); }
        public List<Direction> getAvailableFaces() { return new ArrayList<>(availableFaces); }
        public boolean shouldEnableDropdown() { return hasMultipleFaceTextures && !availableFaces.isEmpty(); }

        @Nullable
        public String getUniformTexture() { return uniformTexture; }

        @Nullable
        public String getTextureForFace(Direction face) {
            return faceTextures.get(face);
        }
    }

    /**
     * Analyze an ItemStack using the dynamic model reading system
     */
    @Nonnull
    public static BlockTextureInfo analyzeBlock(@Nonnull ItemStack itemStack) {
        if (itemStack.isEmpty() || !(itemStack.getItem() instanceof BlockItem blockItem)) {
            return new BlockTextureInfo(false, Collections.emptyMap(), null);
        }

        // Use dynamic model analyzer for universal compatibility
        DynamicBlockModelAnalyzer.DynamicBlockInfo dynamicInfo =
                DynamicBlockModelAnalyzer.analyzeBlockDynamically(itemStack);

        // Convert dynamic info to legacy format for compatibility
        return convertDynamicToLegacy(dynamicInfo);
    }

    /**
     * Convert dynamic analysis result to legacy BlockTextureInfo format
     */
    @Nonnull
    private static BlockTextureInfo convertDynamicToLegacy(@Nonnull DynamicBlockModelAnalyzer.DynamicBlockInfo dynamicInfo) {
        Map<Direction, String> faceTextures = new HashMap<>();
        Map<String, String> textureVariables = dynamicInfo.getTextureVariables();

        // Map texture variables to face directions
        for (Direction face : Direction.values()) {
            String texturePath = getTextureForFace(textureVariables, face);
            faceTextures.put(face, texturePath);
        }

        boolean hasMultiple = dynamicInfo.hasMultipleTextures();
        String uniform = hasMultiple ? null : dynamicInfo.getPrimaryTexture();

        return new BlockTextureInfo(hasMultiple, faceTextures, uniform);
    }

    /**
     * Map texture variables to face directions
     */
    @Nonnull
    private static String getTextureForFace(@Nonnull Map<String, String> textureVariables, @Nonnull Direction face) {
        // Priority mapping for face directions
        String texture = switch (face) {
            case UP -> getTextureByPriority(textureVariables, "top", "up", "end", "all");
            case DOWN -> getTextureByPriority(textureVariables, "bottom", "down", "end", "all");
            case NORTH, SOUTH, EAST, WEST -> getTextureByPriority(textureVariables, "side", "front", "all");
        };

        return texture != null ? texture : textureVariables.values().stream().findFirst().orElse("minecraft:block/stone");
    }

    /**
     * Get texture by priority order
     */
    @Nullable
    private static String getTextureByPriority(@Nonnull Map<String, String> textureVariables, @Nonnull String... priorities) {
        for (String priority : priorities) {
            if (textureVariables.containsKey(priority)) {
                return textureVariables.get(priority);
            }
        }
        return null;
    }

    /**
     * Get texture sprite for preview rendering
     */
    @Nullable
    public static TextureAtlasSprite getTextureSprite(@Nonnull String texturePath) {
        try {
            ResourceLocation textureLocation = new ResourceLocation(texturePath);
            TextureAtlasSprite sprite = Minecraft.getInstance()
                    .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                    .apply(textureLocation);

            if (sprite != null && !sprite.contents().name().toString().contains("missingno")) {
                return sprite;
            }

            // Try fallback patterns for face-specific textures
            if (texturePath.contains("_top") || texturePath.contains("_side") || texturePath.contains("_front")) {
                String basePath = texturePath.replaceAll("_(top|side|front)$", "");
                ResourceLocation fallbackLocation = new ResourceLocation(basePath);
                TextureAtlasSprite fallbackSprite = Minecraft.getInstance()
                        .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                        .apply(fallbackLocation);

                if (fallbackSprite != null && !fallbackSprite.contents().name().toString().contains("missingno")) {
                    return fallbackSprite;
                }
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Check if a texture exists and is valid
     */
    public static boolean isValidTexture(@Nonnull String texturePath) {
        TextureAtlasSprite sprite = getTextureSprite(texturePath);
        return sprite != null && !sprite.contents().name().toString().contains("missingno");
    }

    /**
     * Get display name for a face direction
     */
    @Nonnull
    public static String getFaceDisplayName(@Nonnull Direction face) {
        return switch (face) {
            case UP -> "Top";
            case DOWN -> "Bottom";
            case NORTH -> "North";
            case SOUTH -> "South";
            case EAST -> "East";
            case WEST -> "West";
        };
    }
}