package net.justsomeswitches.client.model;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Unbaked geometry for switches with texture resolution and model preparation. */
public class SwitchesGeometry implements IUnbakedGeometry<SwitchesGeometry> {

    private final Map<String, String> baseTextures;
    private final Map<String, String> toggleTextures;
    private final Map<String, String> powerTextures;
    private final Map<String, SwitchesGeometryLoader.WallOrientationData> orientationTransforms;
    private final Map<String, String> jsonVariables;
    private final SwitchesGeometryLoader.PowerModeConfig powerModeConfig;
    private final String baseModelLocation;
    private final int toggleRotationCompensation;
    private final boolean isSlideModel;

    public SwitchesGeometry(@Nonnull Map<String, String> baseTextures,
                         @Nonnull Map<String, String> toggleTextures,
                         @Nonnull Map<String, String> powerTextures,
                         @Nonnull Map<String, SwitchesGeometryLoader.WallOrientationData> orientationTransforms,
                         @Nonnull Map<String, String> jsonVariables,
                         @Nonnull SwitchesGeometryLoader.PowerModeConfig powerModeConfig,
                         @Nonnull String baseModelLocation,
                         int toggleRotationCompensation,
                         boolean isSlideModel) {
        this.baseTextures = new HashMap<>(baseTextures);
        this.toggleTextures = new HashMap<>(toggleTextures);
        this.powerTextures = new HashMap<>(powerTextures);
        this.orientationTransforms = new HashMap<>(orientationTransforms);
        this.jsonVariables = new HashMap<>(jsonVariables);
        this.powerModeConfig = powerModeConfig;
        this.baseModelLocation = baseModelLocation;
        this.toggleRotationCompensation = toggleRotationCompensation;
        this.isSlideModel = isSlideModel;
    }

    @Override
    @Nonnull
    public BakedModel bake(@Nonnull IGeometryBakingContext context,
                          @Nonnull ModelBaker baker,
                          @Nonnull Function<Material, TextureAtlasSprite> spriteGetter,
                          @Nonnull ModelState modelState,
                          @Nonnull ItemOverrides overrides,
                          @Nonnull ResourceLocation modelLocation) {
        Map<String, TextureAtlasSprite> resolvedSprites = resolveAllTextures(spriteGetter);
        Map<String, Matrix4f> bakedTransforms = bakeOrientationTransforms();
        BakedModel baseCustomModel = getCustomBaseModel(baker, modelState, spriteGetter);
        return new SwitchDynamicModel(
                resolvedSprites,
                bakedTransforms,
                jsonVariables,
                powerModeConfig,
                baseCustomModel,
                overrides,
                toggleRotationCompensation,
                isSlideModel
        );
    }

    /** Resolves all texture sprites for caching. */
    @Nonnull
    private Map<String, TextureAtlasSprite> resolveAllTextures(
            @Nonnull Function<Material, TextureAtlasSprite> spriteGetter) {
        Map<String, TextureAtlasSprite> resolvedSprites = new HashMap<>();
        for (Map.Entry<String, String> entry : baseTextures.entrySet()) {
            String key = "base_" + entry.getKey();
            ResourceLocation textureLocation = new ResourceLocation(entry.getValue());
            Material material = new Material(
                    net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS,
                    textureLocation
            );
            resolvedSprites.put(key, spriteGetter.apply(material));
        }
        for (Map.Entry<String, String> entry : toggleTextures.entrySet()) {
            String key = "toggle_" + entry.getKey();
            ResourceLocation textureLocation = new ResourceLocation(entry.getValue());
            Material material = new Material(
                    net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS,
                    textureLocation
            );
            resolvedSprites.put(key, spriteGetter.apply(material));
        }
        for (Map.Entry<String, String> entry : powerTextures.entrySet()) {
            String key = "power_" + entry.getKey();
            ResourceLocation textureLocation = new ResourceLocation(entry.getValue());
            Material material = new Material(
                    net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS,
                    textureLocation
            );
            resolvedSprites.put(key, spriteGetter.apply(material));
        }
        ResourceLocation altUnpoweredLocation = new ResourceLocation(powerModeConfig.altUnpoweredTexture);
        ResourceLocation altPoweredLocation = new ResourceLocation(powerModeConfig.altPoweredTexture);
        Material altUnpoweredMaterial = new Material(
                net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS,
                altUnpoweredLocation
        );
        Material altPoweredMaterial = new Material(
                net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS,
                altPoweredLocation
        );
        resolvedSprites.put("alt_unpowered", spriteGetter.apply(altUnpoweredMaterial));
        resolvedSprites.put("alt_powered", spriteGetter.apply(altPoweredMaterial));
        return resolvedSprites;
    }

    /** Pre-calculates orientation transforms. */
    @Nonnull
    private Map<String, Matrix4f> bakeOrientationTransforms() {
        Map<String, Matrix4f> bakedTransforms = new HashMap<>();
        for (Map.Entry<String, SwitchesGeometryLoader.WallOrientationData> entry : orientationTransforms.entrySet()) {
            String orientationName = entry.getKey();
            SwitchesGeometryLoader.WallOrientationData data = entry.getValue();
            Matrix4f transform = new Matrix4f();
            transform.identity();
            transform.translate(data.translationX, data.translationY, data.translationZ);
            if (data.rotationX != 0) {
                transform.rotateX((float) Math.toRadians(data.rotationX));
            }
            if (data.rotationY != 0) {
                transform.rotateY((float) Math.toRadians(data.rotationY));
            }
            if (data.rotationZ != 0) {
                transform.rotateZ((float) Math.toRadians(data.rotationZ));
            }
            bakedTransforms.put(orientationName, transform);
        }
        return bakedTransforms;
    }

    /** Gets the custom base model for geometry. */
    @Nonnull
    private BakedModel getCustomBaseModel(@Nonnull ModelBaker baker,
                                        @Nonnull ModelState modelState,
                                        @Nonnull Function<Material, TextureAtlasSprite> spriteGetter) {
        try {
            ResourceLocation customModelLocation = new ResourceLocation(baseModelLocation);
            BakedModel result = baker.bake(customModelLocation, modelState, spriteGetter);
            if (result != null) {
                return result;
            }
        } catch (Exception e) {
            // Intentionally fall through to vanilla model if custom model fails
        }
        try {
            ResourceLocation vanillaLeverLocation = new ResourceLocation("minecraft:block/lever");
            BakedModel vanillaResult = baker.bake(vanillaLeverLocation, modelState, spriteGetter);
            if (vanillaResult != null) {
                return vanillaResult;
            }
        } catch (Exception fallbackException) {
            // Intentionally fall through to minimal model if all else fails
        }
        return createMinimalModel();
    }

    /** Creates a minimal fallback model. */
    @Nonnull
    private BakedModel createMinimalModel() {
        return new BakedModel() {
            @Override
            @Nonnull
            public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull RandomSource rand) {
                return List.of();
            }

            @Override
            @Nonnull
            public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull RandomSource rand, @Nonnull ModelData extraData, @Nullable net.minecraft.client.renderer.RenderType renderType) {
                return List.of();
            }

            @Override
            public boolean useAmbientOcclusion() { return true; }

            @Override
            public boolean isGui3d() { return true; }

            @Override
            public boolean usesBlockLight() { return true; }

            @Override
            public boolean isCustomRenderer() { return false; }

            @Override
            @Nonnull
            public TextureAtlasSprite getParticleIcon() {
                return net.minecraft.client.Minecraft.getInstance()
                    .getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS)
                    .apply(new ResourceLocation("minecraft:missingno"));
            }

            @Override
            @Nonnull
            public ItemOverrides getOverrides() { return ItemOverrides.EMPTY; }
        };
    }
}
