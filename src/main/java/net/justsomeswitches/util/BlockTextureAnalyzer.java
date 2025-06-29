package net.justsomeswitches.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * Utility class for analyzing block textures and face-specific texture information
 * ---
 * Phase 4B: Sophisticated block analysis for dynamic face selection system
 */
public class BlockTextureAnalyzer {

    /**
     * Result of block texture analysis
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

            // Determine available faces (faces with different textures)
            this.availableFaces = new ArrayList<>();
            if (hasMultipleFaceTextures) {
                Set<String> uniqueTextures = new HashSet<>(faceTextures.values());
                if (uniqueTextures.size() > 1) {
                    // Only include faces that have unique textures
                    Map<String, List<Direction>> textureToFaces = new HashMap<>();
                    faceTextures.forEach((face, texture) ->
                            textureToFaces.computeIfAbsent(texture, k -> new ArrayList<>()).add(face));

                    // Add faces that have textures different from others
                    textureToFaces.forEach((texture, faces) -> {
                        if (faces.size() == 1 || uniqueTextures.size() > 2) {
                            availableFaces.addAll(faces);
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
     * Analyze an ItemStack to determine its face texture properties
     */
    @Nonnull
    public static BlockTextureInfo analyzeBlock(@Nonnull ItemStack itemStack) {
        if (itemStack.isEmpty() || !(itemStack.getItem() instanceof BlockItem blockItem)) {
            return new BlockTextureInfo(false, Collections.emptyMap(), null);
        }

        Block block = blockItem.getBlock();
        BlockState defaultState = block.defaultBlockState();

        System.out.println("Phase 4B Debug: Analyzing block: " + block.getDescriptionId());

        return analyzeBlockState(block, defaultState);
    }

    /**
     * Analyze a block state to determine face texture information
     */
    @Nonnull
    private static BlockTextureInfo analyzeBlockState(@Nonnull Block block, @Nonnull BlockState state) {
        try {
            String blockId = getBlockId(block);

            // Get textures for each face
            Map<Direction, String> faceTextures = new HashMap<>();

            for (Direction face : Direction.values()) {
                String faceTexture = getFaceTexture(blockId, face);
                if (faceTexture != null) {
                    faceTextures.put(face, faceTexture);
                }
            }

            // Analyze if block has multiple face textures
            Set<String> uniqueTextures = new HashSet<>(faceTextures.values());
            boolean hasMultipleFaceTextures = uniqueTextures.size() > 1;

            String uniformTexture = uniqueTextures.size() == 1 ?
                    uniqueTextures.iterator().next() : null;

            System.out.println("Phase 4B Debug: Block analysis - Multiple faces: " +
                    hasMultipleFaceTextures + ", Unique textures: " + uniqueTextures.size());

            return new BlockTextureInfo(hasMultipleFaceTextures, faceTextures, uniformTexture);

        } catch (Exception e) {
            System.err.println("Phase 4B Error: Failed to analyze block " + block.getDescriptionId() + " - " + e.getMessage());
            return new BlockTextureInfo(false, Collections.emptyMap(), null);
        }
    }

    /**
     * Get texture path for a specific face of a block
     */
    @Nullable
    private static String getFaceTexture(@Nonnull String blockId, @Nonnull Direction face) {
        try {
            // For most blocks, determine face-specific texture patterns

            // Logs have different textures for top/bottom vs sides
            if (blockId.contains("log") || blockId.contains("wood")) {
                if (face == Direction.UP || face == Direction.DOWN) {
                    return blockId.replace("_log", "_log_top").replace("_wood", "_log_top");
                } else {
                    return blockId; // Side texture
                }
            }

            // Grass block has different textures
            if (blockId.equals("minecraft:block/grass_block")) {
                return switch (face) {
                    case UP -> "minecraft:block/grass_block_top";
                    case DOWN -> "minecraft:block/dirt";
                    default -> "minecraft:block/grass_block_side";
                };
            }

            // Dirt path has different top texture
            if (blockId.equals("minecraft:block/dirt_path")) {
                return face == Direction.UP ? "minecraft:block/dirt_path_top" : "minecraft:block/dirt_path_side";
            }

            // Farmland has different top texture
            if (blockId.equals("minecraft:block/farmland")) {
                return face == Direction.UP ? "minecraft:block/farmland" : "minecraft:block/dirt";
            }

            // Furnaces and similar blocks
            if (blockId.contains("furnace")) {
                return switch (face) {
                    case NORTH -> blockId + "_front"; // Assuming north is front
                    case UP, DOWN -> blockId + "_top";
                    default -> blockId + "_side";
                };
            }

            // For most other blocks, assume uniform texture
            return blockId;

        } catch (Exception e) {
            System.err.println("Phase 4B Error: Failed to get face texture for " + blockId + " face " + face + " - " + e.getMessage());
            return blockId; // Fallback to base texture
        }
    }

    /**
     * Get the block ID for texture analysis
     */
    @Nonnull
    private static String getBlockId(@Nonnull Block block) {
        try {
            ResourceLocation blockRegistryName = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
            if (blockRegistryName != null) {
                return blockRegistryName.getNamespace() + ":block/" + blockRegistryName.getPath();
            }
        } catch (Exception e) {
            System.err.println("Phase 4B Error: Failed to get block registry name for " + block.getDescriptionId() + " - " + e.getMessage());
        }

        // Fallback
        return "minecraft:block/stone";
    }

    /**
     * Get texture sprite for preview rendering
     */
    @Nullable
    public static TextureAtlasSprite getTextureSprite(@Nonnull String texturePath) {
        try {
            ResourceLocation textureLocation = new ResourceLocation(texturePath);
            return Minecraft.getInstance()
                    .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                    .apply(textureLocation);
        } catch (Exception e) {
            System.err.println("Phase 4B Error: Failed to load texture sprite for " + texturePath + " - " + e.getMessage());
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