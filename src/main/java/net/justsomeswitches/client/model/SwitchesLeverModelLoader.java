package net.justsomeswitches.client.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

/**
 * Model Loader for Switches Lever Dynamic Models
 * ---
 * Phase 3C PIVOT: Dynamic Model System
 * This loader registers our dynamic model system with Minecraft's model pipeline
 * allowing us to generate models with custom textures at runtime.
 * ---
 * This approach replaces the Block Entity Renderer approach with proper model integration.
 */
public class SwitchesLeverModelLoader implements net.neoforged.neoforge.client.model.geometry.IGeometryLoader<SwitchesLeverModelLoader.Geometry> {

    public static final ResourceLocation LOADER_ID = new ResourceLocation("justsomeswitches", "switches_lever");

    @Override
    @Nonnull
    public Geometry read(@Nonnull JsonObject jsonObject, @Nonnull JsonDeserializationContext context) {
        // Read configuration from model JSON if needed
        return new Geometry();
    }

    /**
     * Geometry class that handles the actual model creation
     */
    public static class Geometry implements net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry<Geometry> {

        @Override
        @Nonnull
        public BakedModel bake(@Nonnull net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext context,
                               @Nonnull ModelBaker baker, @Nonnull Function<Material, TextureAtlasSprite> spriteGetter,
                               @Nonnull ModelState modelState, @Nonnull ItemOverrides overrides,
                               @Nonnull ResourceLocation modelLocation) {

            // Load the base vanilla models for switches lever
            ResourceLocation baseModelLocation = new ResourceLocation("justsomeswitches", "block/switches_lever");
            ResourceLocation onModelLocation = new ResourceLocation("justsomeswitches", "block/switches_lever_on");

            try {
                // Get the vanilla models
                UnbakedModel baseUnbakedModel = baker.getModel(baseModelLocation);
                UnbakedModel onUnbakedModel = baker.getModel(onModelLocation);

                // Bake them
                BakedModel baseModel = baseUnbakedModel.bake(baker, spriteGetter, modelState, modelLocation);
                BakedModel onModel = onUnbakedModel.bake(baker, spriteGetter, modelState, modelLocation);

                // Create our dynamic model wrapper
                return new SwitchesLeverDynamicModel(baseModel, onModel);

            } catch (Exception e) {
                System.out.println("Phase 3C Debug: Error baking dynamic model - " + e.getMessage());
                e.printStackTrace();

                // Fallback: create a simple model
                return createFallbackModel(spriteGetter);
            }
        }

        @Override
        @Nonnull
        public Collection<Material> getMaterials(@Nonnull net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext context,
                                                 @Nonnull Function<ResourceLocation, UnbakedModel> modelGetter,
                                                 @Nonnull java.util.Set<com.mojang.datafixers.util.Pair<String, String>> missingTextureErrors) {

            // Return materials needed for our model
            return Collections.emptyList(); // We'll use the vanilla model's materials
        }

        /**
         * Creates a fallback model in case of errors
         */
        @Nonnull
        private BakedModel createFallbackModel(@Nonnull Function<Material, TextureAtlasSprite> spriteGetter) {
            // Create a simple fallback model
            Material stoneMaterial = new Material(InventoryMenu.BLOCK_ATLAS, new ResourceLocation("minecraft:block/stone"));
            TextureAtlasSprite stoneSprite = spriteGetter.apply(stoneMaterial);

            // For now, this is a placeholder - in a full implementation we'd create a proper fallback
            // This ensures the mod doesn't crash if there are model loading issues
            return new net.minecraft.client.resources.model.SimpleBakedModel.Builder(null, ItemOverrides.EMPTY, false)
                    .particle(stoneSprite)
                    .build();
        }
    }
}