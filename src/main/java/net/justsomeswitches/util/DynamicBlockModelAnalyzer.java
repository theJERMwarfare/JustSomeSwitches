package net.justsomeswitches.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.justsomeswitches.blockentity.tinting.FaceTintData;
import net.justsomeswitches.blockentity.tinting.OverlayLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
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

/** Dynamic block model analyzer that reads model JSON files at runtime and analyzes tinting properties. */
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

    /** Analyzes block model JSON, following parent chain when textures are not defined directly. */
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
                // Model JSON not found - block may use blockstate redirect (e.g. waxed copper).
                // Fall back to extracting textures from the BakedModel's quads.
                return extractTexturesFromBakedModel(blockId);
            }
            Map<String, String> textureVariables = extractDirectTextureVariables(modelJson, namespace);
            // If no textures found, follow parent chain (handles waxed copper, etc.)
            if (textureVariables.isEmpty() && modelJson.has("parent")) {
                textureVariables = resolveParentTextures(modelJson, namespace);
            }
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
    /** Follows parent model chain to find textures. Stops after 5 levels to prevent infinite loops. */
    @Nonnull
    private static Map<String, String> resolveParentTextures(@Nonnull JsonObject modelJson, @Nonnull String defaultNamespace) {
        Set<String> visited = new HashSet<>();
        JsonObject current = modelJson;
        for (int depth = 0; depth < 5; depth++) {
            if (!current.has("parent")) break;
            String parentRef = current.get("parent").getAsString();
            if (visited.contains(parentRef)) break;
            visited.add(parentRef);
            // Parse parent reference (e.g. "minecraft:block/copper_block" or "block/cube_all")
            String parentNamespace = defaultNamespace;
            String parentPath;
            if (parentRef.contains(":")) {
                String[] parentParts = parentRef.split(":", 2);
                parentNamespace = parentParts[0];
                parentPath = parentParts[1];
            } else {
                parentPath = parentRef;
            }
            // Only follow block model parents, not abstract parents like "cube_all"
            if (!parentPath.startsWith("block/")) break;
            String parentBlockName = parentPath.substring(6);
            JsonObject parentJson = loadBlockModelDirect(parentNamespace, parentBlockName);
            if (parentJson == null) break;
            Map<String, String> parentTextures = extractDirectTextureVariables(parentJson, parentNamespace);
            if (!parentTextures.isEmpty()) {
                return parentTextures;
            }
            current = parentJson;
        }
        return Collections.emptyMap();
    }

    /** Loads only the block's own model JSON file without parent resolution. */
    @Nullable
    private static JsonObject loadBlockModelDirect(@Nonnull String namespace, @Nonnull String blockName) {
        try {
            ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
            ResourceLocation modelLocation = ResourceLocation.fromNamespaceAndPath(namespace, "models/block/" + blockName + ".json");

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

    /**
     * Extracts textures from BakedModel quads when model JSON is unavailable.
     * Handles blocks with blockstate redirects (e.g. waxed copper → copper_block model).
     */
    @SuppressWarnings("resource") // SpriteContents owned by atlas, not our responsibility to close
    @Nonnull
    private static DynamicBlockInfo extractTexturesFromBakedModel(@Nonnull String blockId) {
        try {
            // Parse block registry name from blockId (format: "namespace:block/name")
            String[] parts = blockId.split(":");
            if (parts.length != 2 || !parts[1].startsWith("block/")) {
                return createFallbackInfo(blockId);
            }
            String registryName = parts[0] + ":" + parts[1].substring(6);
            ResourceLocation blockLoc = ResourceLocation.parse(registryName);
            // Registry.get() never returns null (returns air for unknown blocks)
            Block block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(blockLoc);
            BlockState blockState = block.defaultBlockState();
            BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(blockState);
            RandomSource random = RandomSource.create(42L);
            Map<String, String> textureVariables = new LinkedHashMap<>();
            Direction[] faceDirections = { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN };
            String[] variableNames = { "north", "south", "east", "west", "top", "bottom" };
            Set<String> uniqueTextures = new HashSet<>();
            for (int i = 0; i < faceDirections.length; i++) {
                List<BakedQuad> quads = model.getQuads(blockState, faceDirections[i], random, ModelData.EMPTY, null);
                if (!quads.isEmpty()) {
                    ResourceLocation spriteName = quads.getFirst().getSprite().contents().name();
                    Optional<String> ctmBase = ConnectedTextureHandler.getBaseTexture(spriteName);
                    String texturePath = ctmBase.orElse(spriteName.toString());
                    textureVariables.put(variableNames[i], texturePath);
                    uniqueTextures.add(texturePath);
                }
            }
            // If all faces use same texture, simplify to "all"
            if (uniqueTextures.size() == 1) {
                String singleTexture = uniqueTextures.iterator().next();
                textureVariables.clear();
                textureVariables.put("all", singleTexture);
            }
            if (textureVariables.isEmpty()) {
                return createFallbackInfo(blockId);
            }
            boolean hasMultiple = uniqueTextures.size() > 1;
            String primary = getPrimaryTexture(textureVariables);
            return new DynamicBlockInfo(hasMultiple, textureVariables, primary);
        } catch (Exception e) {
            return createFallbackInfo(blockId);
        }
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

    /**
     * Analyzes tinting data from block model quads using BakedModel API.
     * Extracts tintIndex from quads - works for ANY block (vanilla or modded).
     */
    @Nonnull
    public static FaceTintData analyzeTinting(@Nonnull BlockState blockState, @Nonnull Direction direction) {
        FaceTintData tintData = new FaceTintData();
        
        try {
            BakedModel model = Minecraft.getInstance()
                .getBlockRenderer()
                .getBlockModel(blockState);
            
            RandomSource random = RandomSource.create(42L);
            List<BakedQuad> quads = model.getQuads(blockState, direction, random, 
                                                  ModelData.EMPTY, null);
            
            if (!quads.isEmpty()) {
                BakedQuad quad = quads.getFirst();
                int tintIndex = quad.getTintIndex();
                tintData.setTintIndex(tintIndex);
            }
            
        } catch (Exception e) {
            // Silently handle errors - tinting is optional feature
        }
        
        return tintData;
    }

    /**
     * Analyzes overlay layers from block model quads.
     * Detects multiple quads per face - completely universal approach.
     */
    @SuppressWarnings("resource") // SpriteContents owned by atlas, not our responsibility to close
    @Nonnull
    public static List<OverlayLayer> analyzeOverlays(@Nonnull BlockState blockState, @Nonnull Direction direction) {
        List<OverlayLayer> layers = new ArrayList<>();
        
        try {
            BakedModel model = Minecraft.getInstance()
                .getBlockRenderer()
                .getBlockModel(blockState);
            
            RandomSource random = RandomSource.create(42L);
            List<BakedQuad> quads = model.getQuads(blockState, direction, random, 
                                                  ModelData.EMPTY, null);
            
            // Each quad becomes a layer
            for (int i = 0; i < quads.size(); i++) {
                BakedQuad quad = quads.get(i);
                ResourceLocation sprite = quad.getSprite().contents().name();
                int tintIndex = quad.getTintIndex();
                
                layers.add(new OverlayLayer(sprite, tintIndex, i));
            }
            
        } catch (Exception e) {
            // Silently handle errors - overlays are optional
        }
        
        return layers;
    }
}
