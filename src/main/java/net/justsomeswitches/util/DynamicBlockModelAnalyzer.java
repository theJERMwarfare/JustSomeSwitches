package net.justsomeswitches.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Dynamic block model analyzer that reads model JSON files at runtime
 * ---
 * This system provides universal compatibility with vanilla and modded blocks
 * by dynamically parsing their model JSON files to extract texture information.
 */
public class DynamicBlockModelAnalyzer {

    // Texture variables to ignore (not actual block textures)
    private static final Set<String> IGNORED_TEXTURE_VARIABLES = Set.of(
            "particle", "overlay", "north_overlay", "south_overlay",
            "east_overlay", "west_overlay", "up_overlay", "down_overlay",
            "animation", "ctm", "connected"
    );

    /**
     * Result of dynamic block model analysis
     */
    public static class DynamicBlockInfo {
        private final boolean hasMultipleTextures;
        private final Map<String, String> textureVariables;
        private final List<String> availableVariables;
        private final String primaryTexture;

        public DynamicBlockInfo(boolean hasMultipleTextures,
                                Map<String, String> textureVariables,
                                @Nullable String primaryTexture) {
            this.hasMultipleTextures = hasMultipleTextures;
            this.textureVariables = new HashMap<>(textureVariables);
            this.primaryTexture = primaryTexture;

            // Filter available variables (exclude ignored ones)
            this.availableVariables = textureVariables.keySet().stream()
                    .filter(var -> !IGNORED_TEXTURE_VARIABLES.contains(var.toLowerCase()))
                    .sorted() // Sort alphabetically for consistency
                    .toList();
        }

        public boolean hasMultipleTextures() { return hasMultipleTextures; }
        public Map<String, String> getTextureVariables() { return new HashMap<>(textureVariables); }
        public List<String> getAvailableVariables() { return new ArrayList<>(availableVariables); }
        public boolean shouldEnableDropdown() { return hasMultipleTextures && availableVariables.size() > 1; }

        @Nullable
        public String getPrimaryTexture() { return primaryTexture; }

        @Nullable
        public String getTextureForVariable(String variable) {
            return textureVariables.get(variable);
        }

        /**
         * Get user-friendly display name for texture variable
         */
        @Nonnull
        public String getDisplayNameForVariable(@Nonnull String variable) {
            // Convert variable names to user-friendly display names
            return switch (variable.toLowerCase()) {
                case "all" -> "All Faces";
                case "top" -> "Top";
                case "bottom" -> "Bottom";
                case "side" -> "Sides";
                case "front" -> "Front";
                case "back" -> "Back";
                case "end" -> "Ends";
                case "north" -> "North";
                case "south" -> "South";
                case "east" -> "East";
                case "west" -> "West";
                case "up" -> "Top";
                case "down" -> "Bottom";
                default -> capitalizeFirst(variable);
            };
        }

        private String capitalizeFirst(String str) {
            if (str == null || str.isEmpty()) return str;
            return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
        }
    }

    /**
     * Analyze an ItemStack using dynamic model reading
     */
    @Nonnull
    public static DynamicBlockInfo analyzeBlockDynamically(@Nonnull ItemStack itemStack) {
        if (itemStack.isEmpty() || !(itemStack.getItem() instanceof BlockItem blockItem)) {
            return new DynamicBlockInfo(false, Collections.emptyMap(), null);
        }

        Block block = blockItem.getBlock();
        String blockId = getBlockId(block);

        return analyzeBlockModel(blockId);
    }

    /**
     * Analyze block model by reading JSON file dynamically
     */
    @Nonnull
    private static DynamicBlockInfo analyzeBlockModel(@Nonnull String blockId) {
        try {
            // Extract namespace and block name from blockId
            String[] parts = blockId.split(":");
            if (parts.length != 2) {
                return createFallbackInfo(blockId);
            }

            String namespace = parts[0];
            String fullPath = parts[1]; // "block/blockname"

            if (!fullPath.startsWith("block/")) {
                return createFallbackInfo(blockId);
            }

            String blockName = fullPath.substring(6); // Remove "block/" prefix

            // Load the block model JSON file
            JsonObject modelJson = loadBlockModel(namespace, blockName);
            if (modelJson == null) {
                return createFallbackInfo(blockId);
            }

            // Extract texture variables from model
            Map<String, String> textureVariables = extractTextureVariables(modelJson, namespace);

            // Filter out ignored variables
            Map<String, String> filteredVariables = textureVariables.entrySet().stream()
                    .filter(entry -> !IGNORED_TEXTURE_VARIABLES.contains(entry.getKey().toLowerCase()))
                    .collect(HashMap::new, (m, entry) -> m.put(entry.getKey(), entry.getValue()), HashMap::putAll);

            // Determine if block has multiple textures
            Set<String> uniqueTextures = new HashSet<>(filteredVariables.values());
            boolean hasMultipleTextures = uniqueTextures.size() > 1;

            // Determine primary texture
            String primaryTexture = getPrimaryTexture(filteredVariables);

            return new DynamicBlockInfo(hasMultipleTextures, filteredVariables, primaryTexture);

        } catch (Exception e) {
            return createFallbackInfo(blockId);
        }
    }

    /**
     * Load block model JSON file from resource packs
     */
    @Nullable
    private static JsonObject loadBlockModel(@Nonnull String namespace, @Nonnull String blockName) {
        try {
            ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();

            // Try to load the block model JSON file
            ResourceLocation modelLocation = new ResourceLocation(namespace, "models/block/" + blockName + ".json");

            Optional<Resource> resourceOpt = resourceManager.getResource(modelLocation);
            if (resourceOpt.isPresent()) {
                Resource resource = resourceOpt.get();

                try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                    JsonElement jsonElement = JsonParser.parseReader(reader);
                    if (jsonElement.isJsonObject()) {
                        JsonObject modelJson = jsonElement.getAsJsonObject();
                        return resolveParentModels(modelJson, namespace, resourceManager);
                    }
                }
            }

        } catch (IOException e) {
            // Silent failure - this is expected for many blocks
        }

        return null;
    }

    /**
     * Resolve parent models recursively to get complete texture information
     */
    @Nonnull
    private static JsonObject resolveParentModels(@Nonnull JsonObject modelJson, @Nonnull String namespace, @Nonnull ResourceManager resourceManager) {
        JsonObject resolvedModel = modelJson.deepCopy();

        // Check if this model has a parent
        if (modelJson.has("parent")) {
            String parentPath = modelJson.get("parent").getAsString();

            try {
                // Parse parent path
                String[] parentParts = parentPath.split(":");
                String parentNamespace = parentParts.length > 1 ? parentParts[0] : namespace;
                String parentModel = parentParts.length > 1 ? parentParts[1] : parentPath;

                // Load parent model
                ResourceLocation parentLocation = new ResourceLocation(parentNamespace, "models/" + parentModel + ".json");
                Optional<Resource> parentResourceOpt = resourceManager.getResource(parentLocation);

                if (parentResourceOpt.isPresent()) {
                    Resource parentResource = parentResourceOpt.get();

                    try (InputStreamReader reader = new InputStreamReader(parentResource.open(), StandardCharsets.UTF_8)) {
                        JsonElement parentElement = JsonParser.parseReader(reader);
                        if (parentElement.isJsonObject()) {
                            JsonObject parentJson = parentElement.getAsJsonObject();

                            // Recursively resolve parent's parents
                            JsonObject resolvedParent = resolveParentModels(parentJson, parentNamespace, resourceManager);

                            // Merge parent textures with child textures (child overrides parent)
                            if (resolvedParent.has("textures")) {
                                JsonObject parentTextures = resolvedParent.getAsJsonObject("textures");
                                JsonObject childTextures = resolvedModel.has("textures") ?
                                        resolvedModel.getAsJsonObject("textures") : new JsonObject();

                                // Merge textures (child overrides parent)
                                JsonObject mergedTextures = new JsonObject();
                                for (Map.Entry<String, JsonElement> entry : parentTextures.entrySet()) {
                                    mergedTextures.add(entry.getKey(), entry.getValue());
                                }
                                for (Map.Entry<String, JsonElement> entry : childTextures.entrySet()) {
                                    mergedTextures.add(entry.getKey(), entry.getValue());
                                }

                                resolvedModel.add("textures", mergedTextures);
                            }
                        }
                    }
                }

            } catch (Exception e) {
                // Silent failure - continue without parent resolution
            }
        }

        return resolvedModel;
    }

    /**
     * Extract texture variables from model JSON
     */
    @Nonnull
    private static Map<String, String> extractTextureVariables(@Nonnull JsonObject modelJson, @Nonnull String namespace) {
        Map<String, String> textureVariables = new HashMap<>();

        if (modelJson.has("textures")) {
            JsonObject textures = modelJson.getAsJsonObject("textures");

            for (Map.Entry<String, JsonElement> entry : textures.entrySet()) {
                String variable = entry.getKey();
                String texturePath = entry.getValue().getAsString();

                // Resolve texture path references (e.g., "#side" -> actual texture path)
                String resolvedPath = resolveTextureReference(texturePath, textures, namespace);

                textureVariables.put(variable, resolvedPath);
            }
        }

        return textureVariables;
    }

    /**
     * Resolve texture references (e.g., "#side" references)
     */
    @Nonnull
    private static String resolveTextureReference(@Nonnull String texturePath, @Nonnull JsonObject allTextures, @Nonnull String namespace) {
        if (texturePath.startsWith("#")) {
            // This is a reference to another texture variable
            String referencedVariable = texturePath.substring(1);
            if (allTextures.has(referencedVariable)) {
                String referencedPath = allTextures.get(referencedVariable).getAsString();
                return resolveTextureReference(referencedPath, allTextures, namespace); // Recursive resolution
            }
        }

        // If path doesn't contain namespace, add default namespace
        if (!texturePath.contains(":")) {
            return namespace + ":" + texturePath;
        }

        return texturePath;
    }

    /**
     * Get primary texture from texture variables
     */
    @Nullable
    private static String getPrimaryTexture(@Nonnull Map<String, String> textureVariables) {
        // Priority order for primary texture selection
        String[] priorityOrder = {"all", "side", "top", "front", "north"};

        for (String priority : priorityOrder) {
            if (textureVariables.containsKey(priority)) {
                return textureVariables.get(priority);
            }
        }

        // Return any available texture as fallback
        return textureVariables.values().stream().findFirst().orElse(null);
    }

    /**
     * Create fallback info for blocks that can't be analyzed dynamically
     */
    @Nonnull
    private static DynamicBlockInfo createFallbackInfo(@Nonnull String blockId) {
        Map<String, String> fallbackTextures = Map.of("all", blockId);
        return new DynamicBlockInfo(false, fallbackTextures, blockId);
    }

    /**
     * Get block ID from Block instance
     */
    @Nonnull
    private static String getBlockId(@Nonnull Block block) {
        try {
            ResourceLocation blockRegistryName = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
            if (blockRegistryName != null) {
                return blockRegistryName.getNamespace() + ":block/" + blockRegistryName.getPath();
            }
        } catch (Exception e) {
            // Silent failure
        }

        return "minecraft:block/stone";
    }

    /**
     * Check if a texture path is valid
     */
    public static boolean isValidTexture(@Nonnull String texturePath) {
        return BlockTextureAnalyzer.isValidTexture(texturePath);
    }
}