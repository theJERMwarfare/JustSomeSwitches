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
 *
 * SAFE CLEANUP: Only returns EXACT texture variables from block's OWN JSON file
 * No parent model inheritance, no filtering, no additions - just raw variables
 * Universal compatibility with vanilla and modded blocks
 */
public class DynamicBlockModelAnalyzer {

    // ONLY non-face texture variables to exclude (not actual block face textures)
    private static final Set<String> NON_FACE_TEXTURE_VARIABLES = Set.of(
            "particle", "overlay", "animation", "ctm", "connected",
            "north_overlay", "south_overlay", "east_overlay", "west_overlay",
            "up_overlay", "down_overlay", "layer0", "layer1", "layer2",
            "layer3", "layer4", "inside", "cross", "crop", "stem", "upper_stem"
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
            this.textureVariables = new LinkedHashMap<>(textureVariables); // Preserve order
            this.primaryTexture = primaryTexture;

            // CRITICAL: ONLY exclude non-face textures, preserve EXACT JSON variables
            this.availableVariables = new ArrayList<>();
            for (String variable : textureVariables.keySet()) {
                // ONLY exclude non-face textures like "particle" - include EVERYTHING else from JSON
                if (!NON_FACE_TEXTURE_VARIABLES.contains(variable.toLowerCase()) &&
                        !variable.startsWith("#")) { // Exclude texture references
                    this.availableVariables.add(variable);
                }
            }
        }

        public boolean hasMultipleTextures() { return hasMultipleTextures; }
        public Map<String, String> getTextureVariables() { return new LinkedHashMap<>(textureVariables); }
        public List<String> getAvailableVariables() { return new ArrayList<>(availableVariables); }
        public boolean shouldEnableDropdown() { return hasMultipleTextures && availableVariables.size() > 1; }

        @Nullable
        public String getPrimaryTexture() { return primaryTexture; }

        @Nullable
        public String getTextureForVariable(String variable) {
            return textureVariables.get(variable);
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
     * CRITICAL FIX: Analyze ONLY the block's own JSON file - NO parent model inheritance
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

            // Load ONLY the block's own model JSON file - NO parent resolution
            JsonObject modelJson = loadBlockModelDirect(namespace, blockName);
            if (modelJson == null) {
                return createFallbackInfo(blockId);
            }

            // Extract ONLY the texture variables from THIS block's JSON
            Map<String, String> textureVariables = extractDirectTextureVariables(modelJson, namespace);

            // SIMPLE FILTERING: Only exclude non-face textures, keep everything else from JSON
            Map<String, String> filteredVariables = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : textureVariables.entrySet()) {
                String variable = entry.getKey();
                // ONLY exclude non-face textures like "particle" - keep EVERYTHING else from JSON
                if (!NON_FACE_TEXTURE_VARIABLES.contains(variable.toLowerCase()) &&
                        !variable.startsWith("#")) {
                    filteredVariables.put(variable, entry.getValue());
                }
            }

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
     * Load ONLY the block's own model JSON file - NO parent resolution
     */
    @Nullable
    private static JsonObject loadBlockModelDirect(@Nonnull String namespace, @Nonnull String blockName) {
        try {
            ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();

            // Load only the block model JSON file - no parent resolution
            ResourceLocation modelLocation = new ResourceLocation(namespace, "models/block/" + blockName + ".json");

            Optional<Resource> resourceOpt = resourceManager.getResource(modelLocation);
            if (resourceOpt.isPresent()) {
                Resource resource = resourceOpt.get();

                try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                    JsonElement jsonElement = JsonParser.parseReader(reader);
                    if (jsonElement.isJsonObject()) {
                        return jsonElement.getAsJsonObject();
                    }
                }
            }

        } catch (IOException e) {
            // Silent failure - this is expected for many blocks
        }

        return null;
    }

    /**
     * Extract ONLY the direct texture variables from THIS block's JSON
     * NO parent model resolution - just what's in this file
     */
    @Nonnull
    private static Map<String, String> extractDirectTextureVariables(@Nonnull JsonObject modelJson, @Nonnull String namespace) {
        Map<String, String> textureVariables = new LinkedHashMap<>(); // Preserve order

        if (modelJson.has("textures")) {
            JsonObject textures = modelJson.getAsJsonObject("textures");

            // Only iterate over what's actually in THIS block's JSON file
            for (Map.Entry<String, JsonElement> entry : textures.entrySet()) {
                String variable = entry.getKey();
                String texturePath = entry.getValue().getAsString();

                // Resolve only simple texture path references within THIS file
                String resolvedPath = resolveSimpleTextureReference(texturePath, textures, namespace);

                // Only add if resolved path is valid and not empty
                if (!resolvedPath.isEmpty() && !resolvedPath.startsWith("#")) {
                    textureVariables.put(variable, resolvedPath);
                }
            }
        }

        return textureVariables;
    }

    /**
     * Resolve simple texture references ONLY within the same file
     */
    @Nonnull
    private static String resolveSimpleTextureReference(@Nonnull String texturePath, @Nonnull JsonObject allTextures, @Nonnull String namespace) {
        // Track resolution chain to prevent infinite loops
        Set<String> visited = new HashSet<>();
        return resolveSimpleTextureReferenceInternal(texturePath, allTextures, namespace, visited);
    }

    @Nonnull
    private static String resolveSimpleTextureReferenceInternal(@Nonnull String texturePath, @Nonnull JsonObject allTextures, @Nonnull String namespace, @Nonnull Set<String> visited) {
        if (texturePath.startsWith("#")) {
            // This is a reference to another texture variable in THIS file
            String referencedVariable = texturePath.substring(1);

            // Prevent infinite loops
            if (visited.contains(referencedVariable)) {
                return ""; // Return empty for circular references
            }

            visited.add(referencedVariable);

            if (allTextures.has(referencedVariable)) {
                String referencedPath = allTextures.get(referencedVariable).getAsString();
                return resolveSimpleTextureReferenceInternal(referencedPath, allTextures, namespace, visited);
            }

            // Reference not found in THIS file
            return "";
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
        Map<String, String> fallbackTextures = new LinkedHashMap<>();
        fallbackTextures.put("all", blockId);
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

}