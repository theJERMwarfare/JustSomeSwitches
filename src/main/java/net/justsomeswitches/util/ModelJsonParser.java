package net.justsomeswitches.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Parses model JSON files to extract exact texture variable names (e.g., "end"/"side" for logs).
 * BakedModel API loses original names during baking, so this reads JSON files directly.
 */
public class ModelJsonParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModelJsonParser.class);
    private static final Map<ResourceLocation, Map<String, String>> MODEL_VARIABLE_CACHE = new HashMap<>();
    /** Extracts texture variable names from block's model JSON file (returns variable → texture path map). */
    @Nonnull
    public static Map<String, String> getTextureVariables(@Nonnull ResourceLocation blockLocation) {
        if (MODEL_VARIABLE_CACHE.containsKey(blockLocation)) {
            return new LinkedHashMap<>(MODEL_VARIABLE_CACHE.get(blockLocation));
        }
        Map<String, String> variables = new LinkedHashMap<>();
        try {
            ResourceLocation modelLocation = new ResourceLocation(
                blockLocation.getNamespace(),
                "models/block/" + blockLocation.getPath() + ".json"
            );
            variables = parseModelJson(modelLocation);
            MODEL_VARIABLE_CACHE.put(blockLocation, new LinkedHashMap<>(variables));
        } catch (Exception e) {
            LOGGER.debug("Failed to parse model JSON for {}: {}", blockLocation, e.getMessage());
        }
        return variables;
    }
    /** Parses model JSON file recursively, resolving parent models and texture inheritance. */
    @Nonnull
    private static Map<String, String> parseModelJson(@Nonnull ResourceLocation modelLocation) {
        Map<String, String> variables = new LinkedHashMap<>();
        Set<ResourceLocation> visitedModels = new HashSet<>();
        parseModelJsonRecursive(modelLocation, variables, visitedModels);
        resolveTextureReferences(variables);
        return variables;
    }
    /** Recursively parses model JSON following parent chain (prevents infinite loops). */
    private static void parseModelJsonRecursive(@Nonnull ResourceLocation modelLocation,
                                                @Nonnull Map<String, String> variables,
                                                @Nonnull Set<ResourceLocation> visitedModels) {
        if (visitedModels.contains(modelLocation)) {
            return;
        }
        visitedModels.add(modelLocation);
        try {
            var resourceManager = Minecraft.getInstance().getResourceManager();
            Resource resource = resourceManager.getResourceOrThrow(modelLocation);
            try (InputStreamReader reader = new InputStreamReader(resource.open())) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                if (json.has("parent")) {
                    String parentPath = json.get("parent").getAsString();
                    ResourceLocation parentLocation = parseModelPath(parentPath);
                    parseModelJsonRecursive(parentLocation, variables, visitedModels);
                }
                if (json.has("textures")) {
                    JsonObject textures = json.getAsJsonObject("textures");
                    for (Map.Entry<String, JsonElement> entry : textures.entrySet()) {
                        String varName = entry.getKey();
                        String texturePath = entry.getValue().getAsString();
                        variables.put(varName, texturePath);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to load model resource {}: {}", modelLocation, e.getMessage());
        }
    }
    /** Resolves texture variable references (e.g., "#end" → actual texture paths). */
    private static void resolveTextureReferences(@Nonnull Map<String, String> variables) {
        boolean changed = true;
        int maxIterations = 10;
        int iterations = 0;
        while (changed && iterations < maxIterations) {
            changed = false;
            iterations++;
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                String texturePath = entry.getValue();
                if (texturePath.startsWith("#")) {
                    String refName = texturePath.substring(1);
                    if (variables.containsKey(refName)) {
                        String resolvedPath = variables.get(refName);
                        if (!resolvedPath.equals(texturePath)) {
                            entry.setValue(resolvedPath);
                            changed = true;
                        }
                    }
                }
            }
        }
    }
    /** Converts model path string to ResourceLocation (handles both full and short paths). */
    @Nonnull
    private static ResourceLocation parseModelPath(@Nonnull String path) {
        if (path.contains(":")) {
            return new ResourceLocation(path);
        } else {
            return new ResourceLocation("minecraft", "models/" + path + ".json");
        }
    }
}
