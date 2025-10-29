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

/** Dynamic block model analyzer that reads model JSON files at runtime. */
public class DynamicBlockModelAnalyzer {

    private static final Set<String> NON_FACE_TEXTURE_VARIABLES = Set.of(
            "particle", "overlay", "animation", "ctm", "connected",
            "north_overlay", "south_overlay", "east_overlay", "west_overlay",
            "up_overlay", "down_overlay", "layer0", "layer1", "layer2",
            "layer3", "layer4", "inside", "cross", "crop", "stem", "upper_stem"
    );



    /** Result of dynamic block model analysis. */
    public static class DynamicBlockInfo {
        private final Map<String, String> textureVariables;
        private final List<String> availableVariables;

        public DynamicBlockInfo(@SuppressWarnings("unused") boolean hasMultipleTextures,
                                Map<String, String> textureVariables,
                                @SuppressWarnings("unused") @Nullable String primaryTexture) {
            this.textureVariables = new LinkedHashMap<>(textureVariables); // Preserve order

            this.availableVariables = new ArrayList<>();
            for (String variable : textureVariables.keySet()) {
                if (!NON_FACE_TEXTURE_VARIABLES.contains(variable.toLowerCase()) &&
                        !variable.startsWith("#")) {
                    this.availableVariables.add(variable);
                }
            }
        }

        public List<String> getAvailableVariables() { return new ArrayList<>(availableVariables); }

        @Nullable
        public String getTextureForVariable(String variable) {
            return textureVariables.get(variable);
        }
    }

    /** Analyzes an ItemStack using dynamic model reading. */
    @Nonnull
    public static DynamicBlockInfo analyzeBlockDynamically(@Nonnull ItemStack itemStack) {
        if (itemStack.isEmpty() || !(itemStack.getItem() instanceof BlockItem blockItem)) {
            return new DynamicBlockInfo(false, Collections.emptyMap(), null);
        }

        Block block = blockItem.getBlock();
        String blockId = getBlockId(block);

        return analyzeBlockModel(blockId);
    }

    /** Analyzes only the block's own JSON file without parent model inheritance. */
    @Nonnull
    private static DynamicBlockInfo analyzeBlockModel(@Nonnull String blockId) {
        try {
            String[] parts = blockId.split(":");
            if (parts.length != 2) {
                return createFallbackInfo(blockId);
            }

            String namespace = parts[0];
            String fullPath = parts[1];

            if (!fullPath.startsWith("block/")) {
                return createFallbackInfo(blockId);
            }

            String blockName = fullPath.substring(6);


            JsonObject modelJson = loadBlockModelDirect(namespace, blockName);
            if (modelJson == null) {
                return createFallbackInfo(blockId);
            }


            Map<String, String> textureVariables = extractDirectTextureVariables(modelJson, namespace);

            Map<String, String> filteredVariables = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : textureVariables.entrySet()) {
                String variable = entry.getKey();
                if (!NON_FACE_TEXTURE_VARIABLES.contains(variable.toLowerCase()) &&
                        !variable.startsWith("#")) {
                    filteredVariables.put(variable, entry.getValue());
                }
            }


            Set<String> uniqueTextures = new HashSet<>(filteredVariables.values());
            boolean hasMultipleTextures = uniqueTextures.size() > 1;
            String primaryTexture = getPrimaryTexture(filteredVariables);

            return new DynamicBlockInfo(hasMultipleTextures, filteredVariables, primaryTexture);

        } catch (Exception e) {
            return createFallbackInfo(blockId);
        }
    }

    /** Loads only the block's own model JSON file without parent resolution. */
    @Nullable
    private static JsonObject loadBlockModelDirect(@Nonnull String namespace, @Nonnull String blockName) {
        try {
            ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
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
            // Intentionally ignore - return null for missing model files
        }

        return null;
    }

    /** Extracts direct texture variables from the block's JSON file. */
    @Nonnull
    private static Map<String, String> extractDirectTextureVariables(@Nonnull JsonObject modelJson, @Nonnull String namespace) {
        Map<String, String> textureVariables = new LinkedHashMap<>();

        if (modelJson.has("textures")) {
            JsonObject textures = modelJson.getAsJsonObject("textures");


            for (Map.Entry<String, JsonElement> entry : textures.entrySet()) {
                String variable = entry.getKey();
                String texturePath = entry.getValue().getAsString();


                String resolvedPath = resolveSimpleTextureReference(texturePath, textures, namespace);


                if (!resolvedPath.isEmpty() && !resolvedPath.startsWith("#")) {
                    textureVariables.put(variable, resolvedPath);
                }
            }
        }

        return textureVariables;
    }

    /** Resolves simple texture references within the same file. */
    @Nonnull
    private static String resolveSimpleTextureReference(@Nonnull String texturePath, @Nonnull JsonObject allTextures, @Nonnull String namespace) {

        Set<String> visited = new HashSet<>();
        return resolveSimpleTextureReferenceInternal(texturePath, allTextures, namespace, visited);
    }

    @Nonnull
    private static String resolveSimpleTextureReferenceInternal(@Nonnull String texturePath, @Nonnull JsonObject allTextures, @Nonnull String namespace, @Nonnull Set<String> visited) {
        if (texturePath.startsWith("#")) {

            String referencedVariable = texturePath.substring(1);

            if (visited.contains(referencedVariable)) {
                return "";
            }

            visited.add(referencedVariable);

            if (allTextures.has(referencedVariable)) {
                String referencedPath = allTextures.get(referencedVariable).getAsString();
                return resolveSimpleTextureReferenceInternal(referencedPath, allTextures, namespace, visited);
            }


            return "";
        }


        if (!texturePath.contains(":")) {
            return namespace + ":" + texturePath;
        }

        return texturePath;
    }

    /** Gets primary texture from texture variables. */
    @Nullable
    private static String getPrimaryTexture(@Nonnull Map<String, String> textureVariables) {

        String[] priorityOrder = {"all", "side", "top", "front", "north"};

        for (String priority : priorityOrder) {
            if (textureVariables.containsKey(priority)) {
                return textureVariables.get(priority);
            }
        }


        return textureVariables.values().stream().findFirst().orElse(null);
    }

    /** Creates fallback info for blocks that can't be analyzed dynamically. */
    @Nonnull
    private static DynamicBlockInfo createFallbackInfo(@Nonnull String blockId) {
        Map<String, String> fallbackTextures = new LinkedHashMap<>();
        fallbackTextures.put("all", blockId);
        return new DynamicBlockInfo(false, fallbackTextures, blockId);
    }

    /** Gets block ID from Block instance. */
    @Nonnull
    private static String getBlockId(@Nonnull Block block) {
        try {
            ResourceLocation blockRegistryName = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
            return blockRegistryName.getNamespace() + ":block/" + blockRegistryName.getPath();
        } catch (Exception e) {
            // Intentionally ignore - return safe default
            return "minecraft:block/stone";
        }
    }


}
