package net.justsomeswitches.client;

import net.justsomeswitches.client.model.SwitchesLeverModel;
import net.justsomeswitches.gui.JustSomeSwitchesMenuTypes;
import net.justsomeswitches.gui.SwitchTextureScreen;
import net.justsomeswitches.init.JustSomeSwitchesModBlocks;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.Map;

/**
 * Client-side setup and registration for Just Some Switches mod
 * ---
 * Phase 3C: Simplified Model-Based Texture System
 * Replaces vanilla models with custom models that support dynamic texture replacement
 */
@Mod.EventBusSubscriber(modid = "justsomeswitches", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class JustSomeSwitchesClientSetup {

    /**
     * Client setup event - called during client-side initialization
     */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Register screens for our menu types
        event.enqueueWork(() -> {
            // Register the Switch Texture GUI screen
            MenuScreens.register(
                    JustSomeSwitchesMenuTypes.SWITCH_TEXTURE_MENU.get(),
                    SwitchTextureScreen::new
            );
        });
    }

    /**
     * Model baking event - replaces vanilla models with custom models
     * ---
     * Phase 3C: This is where we replace the vanilla switch models with our custom models
     * that support dynamic texture replacement through ModelData
     */
    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<ResourceLocation, BakedModel> models = event.getModels();

        System.out.println("Phase 3C Debug: Starting model replacement for switches lever");

        // Replace switch lever models
        replaceSwitchModels(models);

        System.out.println("Phase 3C Debug: Model replacement completed");
    }

    /**
     * Replace switch models with custom texture-enabled models
     */
    private static void replaceSwitchModels(Map<ResourceLocation, BakedModel> models) {
        // Focus on replacing ALL blockstate variant models since base models don't exist
        replaceAllSwitchVariants(models);
    }

    /**
     * Replace all switch variant models
     */
    private static void replaceAllSwitchVariants(Map<ResourceLocation, BakedModel> models) {
        int replacedCount = 0;

        // Create a copy of the entries to avoid concurrent modification
        var entries = new java.util.ArrayList<>(models.entrySet());

        for (var entry : entries) {
            ResourceLocation location = entry.getKey();

            // Check if this is a switch model variant
            if (location.getNamespace().equals("justsomeswitches") &&
                    location.getPath().contains("switches_lever")) {

                BakedModel originalModel = entry.getValue();

                // Determine the blockstate this model represents
                BlockState representativeState = getRepresentativeState(location);
                if (representativeState != null) {
                    // Create custom model with enhanced ModelData support
                    SwitchesLeverModel customModel = new SwitchesLeverModel(representativeState, originalModel);

                    // Replace the model
                    models.put(location, customModel);
                    replacedCount++;

                    System.out.println("Phase 3C Debug: Replaced variant model: " + location);
                }
            }
        }

        System.out.println("Phase 3C Debug: Replaced " + replacedCount + " switch models total");
    }

    /**
     * Get representative blockstate for a model location
     */
    private static BlockState getRepresentativeState(ResourceLocation modelLocation) {
        try {
            // Default to unpowered state
            BlockState defaultState = JustSomeSwitchesModBlocks.SWITCHES_LEVER.get().defaultBlockState();

            // Check if this is a powered variant
            if (modelLocation.getPath().contains("_on")) {
                if (defaultState.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED)) {
                    return defaultState.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED, true);
                }
            }

            return defaultState;
        } catch (Exception e) {
            System.out.println("Phase 3C Debug: Error getting representative state for: " + modelLocation + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * Debug method called after models are loaded
     */
    @SubscribeEvent
    public static void onModelsLoaded(ModelEvent.BakingCompleted event) {
        System.out.println("Phase 3C Debug: Model baking completed - custom switch models should be active");

        // Debug: List some model locations
        Map<ResourceLocation, BakedModel> models = event.getModels();
        models.keySet().stream()
                .filter(location -> location.getNamespace().equals("justsomeswitches"))
                .limit(5) // Limit output to avoid spam
                .forEach(location -> System.out.println("Phase 3C Debug: Available model: " + location));
    }
}