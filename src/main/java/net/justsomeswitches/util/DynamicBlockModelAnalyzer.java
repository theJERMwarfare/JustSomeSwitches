package net.justsomeswitches.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.resources.model.BakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * Extracts textures from block models using BakedModel API with 5-level fallback system.
 * Supports CTM/connected textures, animated textures, multi-layer models, and JSON variable name preservation.
 */
public class DynamicBlockModelAnalyzer {

    private static final Set<String> NON_FACE_TEXTURE_VARIABLES = Set.of(
            "particle", "overlay", "animation", "ctm", "connected",
            "north_overlay", "south_overlay", "east_overlay", "west_overlay",
            "up_overlay", "down_overlay", "layer0", "layer1", "layer2",
            "layer3", "layer4", "inside", "cross", "crop", "stem", "upper_stem"
    );

    private static final Map<TextureAtlasSprite, String> SPRITE_NAME_CACHE = new HashMap<>();

    /** Result of dynamic block analysis tracking unique textures and deduplicated face variables. */
    public static class DynamicBlockInfo {
        private final Map<String, String> textureVariables;
        private final List<String> availableVariables;

        public DynamicBlockInfo(@SuppressWarnings("unused") boolean hasMultipleTextures,
                                Map<String, String> textureVariables,
                                @SuppressWarnings("unused") @Nullable String primaryTexture) {
            this.textureVariables = new LinkedHashMap<>(textureVariables);
            Map<String, String> seenTextures = new LinkedHashMap<>();
            this.availableVariables = new ArrayList<>();
            for (Map.Entry<String, String> entry : textureVariables.entrySet()) {
                String variable = entry.getKey();
                String texture = entry.getValue();
                if (variable.equals("all")) {
                    continue;
                }
                if (NON_FACE_TEXTURE_VARIABLES.contains(variable.toLowerCase()) ||
                    variable.startsWith("#")) {
                    continue;
                }
                if (!seenTextures.containsKey(texture)) {
                    seenTextures.put(texture, variable);
                    availableVariables.add(variable);
                }
            }
        }
        /** Returns deduplicated variable names with exact JSON casing (each unique texture appears once). */
        public List<String> getAvailableVariables() { 
            return new ArrayList<>(availableVariables); 
        }

        @Nullable
        public String getTextureForVariable(String variable) {
            return textureVariables.get(variable);
        }
    }
    /** Analyzes ItemStack with 5-level fallback (BakedModel → particle → face → item model → default). */
    @Nonnull
    public static DynamicBlockInfo analyzeBlockDynamically(@Nonnull ItemStack itemStack) {
        if (itemStack.isEmpty() || !(itemStack.getItem() instanceof BlockItem blockItem)) {
            return new DynamicBlockInfo(false, Collections.emptyMap(), null);
        }

        Block block = blockItem.getBlock();
        BlockState state = block.defaultBlockState();

        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        BakedModel model = dispatcher.getBlockModel(state);

        Map<String, String> textureVariables = extractFromBakedModel(model, state);
        if (!textureVariables.isEmpty()) {
            Map<String, String> filteredVariables = filterNonFaceVariables(textureVariables);
            if (!filteredVariables.isEmpty()) {
                Set<String> uniqueTextures = new HashSet<>(filteredVariables.values());
                boolean hasMultipleTextures = uniqueTextures.size() > 1;
                String primaryTexture = getPrimaryTexture(filteredVariables);
                return new DynamicBlockInfo(hasMultipleTextures, filteredVariables, primaryTexture);
            }
        }
        String particleTexture = extractParticleTexture(model);
        if (particleTexture != null && !particleTexture.isEmpty()) {
            Map<String, String> particleFallback = new LinkedHashMap<>();
            particleFallback.put("all", particleTexture);
            return new DynamicBlockInfo(false, particleFallback, particleTexture);
        }
        String firstFaceTexture = extractFirstFaceTexture(model, state);
        if (firstFaceTexture != null && !firstFaceTexture.isEmpty()) {
            Map<String, String> faceFallback = new LinkedHashMap<>();
            faceFallback.put("all", firstFaceTexture);
            return new DynamicBlockInfo(false, faceFallback, firstFaceTexture);
        }
        String itemTexture = extractItemModelTexture(itemStack);
        if (itemTexture != null && !itemTexture.isEmpty()) {
            Map<String, String> itemFallback = new LinkedHashMap<>();
            itemFallback.put("all", itemTexture);
            return new DynamicBlockInfo(false, itemFallback, itemTexture);
        }
        return createDefaultFallbackInfo();
    }

    /** Extracts textures from BakedModel using NeoForge API with CTM detection and JSON variable name preservation. */
    @Nonnull
    private static Map<String, String> extractFromBakedModel(@Nonnull BakedModel model, 
                                                             @Nonnull BlockState state) {
        Map<String, String> textureVariables = new LinkedHashMap<>();
        
        // Try to get exact variable names from model JSON
        Block block = state.getBlock();
        ResourceLocation blockLocation = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
        Map<String, String> jsonVariables = ModelJsonParser.getTextureVariables(blockLocation);
        
        // If we have JSON variables, use those exact names
        if (!jsonVariables.isEmpty()) {
            // JSON variables have exact names but may have texture references
            // We need to resolve them to actual texture paths
            for (Map.Entry<String, String> entry : jsonVariables.entrySet()) {
                String varName = entry.getKey();
                String texturePath = entry.getValue();
                
                // Make sure path doesn't start with # (should be resolved already)
                if (!texturePath.startsWith("#")) {
                    textureVariables.put(varName, texturePath);
                }
            }
            
            return textureVariables;
        }
        

        TextureAtlasSprite particleSprite = model.getParticleIcon(ModelData.EMPTY);
        String particleTexture = getSpriteResourceLocation(particleSprite);

        ResourceLocation particleLocation = net.justsomeswitches.client.model.TextureAtlasHandler.getSpriteLocation(particleSprite);
        Optional<String> ctmBaseTexture = ConnectedTextureHandler.getBaseTexture(particleLocation);
        
        if (ctmBaseTexture.isPresent()) {
            // CTM block detected - use standalone base texture
            particleTexture = ctmBaseTexture.get();
        }

        RandomSource random = RandomSource.create();
        Map<String, String> faceTextures = new LinkedHashMap<>();
        
        for (Direction direction : Direction.values()) {
            // CRITICAL: NeoForge requires ModelData and RenderType parameters
            List<BakedQuad> quads = model.getQuads(state, direction, random, 
                                                    ModelData.EMPTY, null);
            if (!quads.isEmpty()) {
                BakedQuad quad = quads.get(0);
                String faceTexture = getSpriteResourceLocation(quad.getSprite());
                faceTextures.put(direction.getName(), faceTexture);
            }
        }


        Set<String> uniqueTextures = new HashSet<>(faceTextures.values());
        
        if (uniqueTextures.size() == 1 && !faceTextures.isEmpty()) {
            // Single texture block - return just "all" pointing to that texture
            Map<String, String> singleTextureMap = new LinkedHashMap<>();
            singleTextureMap.put("all", faceTextures.values().iterator().next());
            return singleTextureMap;
        } else if (faceTextures.isEmpty()) {
            // No face textures - use particle texture
            Map<String, String> singleTextureMap = new LinkedHashMap<>();
            singleTextureMap.put("all", particleTexture);
            return singleTextureMap;
        }

        textureVariables.put("all", particleTexture);
        textureVariables.putAll(faceTextures);

        return textureVariables;
    }
    /** Gets ResourceLocation string from TextureAtlasSprite using reflection-free access (cached). */
    @Nonnull
    private static String getSpriteResourceLocation(@Nonnull TextureAtlasSprite sprite) {
        if (SPRITE_NAME_CACHE.containsKey(sprite)) {
            return SPRITE_NAME_CACHE.get(sprite);
        }
        ResourceLocation location = net.justsomeswitches.client.model.TextureAtlasHandler.getSpriteLocation(sprite);
        String spriteName = location.toString();
        SPRITE_NAME_CACHE.put(sprite, spriteName);
        return spriteName;
    }

    /** Gets primary texture from variables using priority order (all, side, top, front, north). */
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
    /** Filters out non-face texture variables (overlay, particle, animation, layer0-4, etc). */
    @Nonnull
    private static Map<String, String> filterNonFaceVariables(@Nonnull Map<String, String> textureVariables) {
        Map<String, String> filteredVariables = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : textureVariables.entrySet()) {
            String variable = entry.getKey();
            if (!NON_FACE_TEXTURE_VARIABLES.contains(variable.toLowerCase()) &&
                    !variable.startsWith("#")) {
                filteredVariables.put(variable, entry.getValue());
            }
        }
        return filteredVariables;
    }
    
    /** Extracts particle texture from BakedModel (Level 2 fallback for complex renderers). */
    @Nullable
    private static String extractParticleTexture(@Nonnull BakedModel model) {
        try {
            TextureAtlasSprite particleSprite = model.getParticleIcon(ModelData.EMPTY);
            return getSpriteResourceLocation(particleSprite);
        } catch (Exception e) {
            return null;
        }
    }
    
    /** Extracts first available face texture from BakedModel (Level 3 fallback when particle unavailable). */
    @Nullable
    private static String extractFirstFaceTexture(@Nonnull BakedModel model, @Nonnull BlockState state) {
        try {
            RandomSource random = RandomSource.create();
            for (Direction direction : Direction.values()) {
                List<BakedQuad> quads = model.getQuads(state, direction, random, ModelData.EMPTY, null);
                if (!quads.isEmpty()) {
                    BakedQuad quad = quads.get(0);
                    return getSpriteResourceLocation(quad.getSprite());
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }
    
    /** Extracts texture from item model (Level 4 fallback for corrupted block models). */
    @Nullable
    private static String extractItemModelTexture(@Nonnull ItemStack itemStack) {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            BakedModel itemModel = minecraft.getItemRenderer().getModel(itemStack, null, null, 0);
            TextureAtlasSprite itemSprite = itemModel.getParticleIcon(ModelData.EMPTY);
            return getSpriteResourceLocation(itemSprite);
        } catch (Exception e) {
            return null;
        }
    }
    
    /** Creates default fallback info with empty texture override (Level 5 prevents missing texture placeholders). */
    @Nonnull
    private static DynamicBlockInfo createDefaultFallbackInfo() {
        return new DynamicBlockInfo(false, Collections.emptyMap(), null);
    }
}
