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
 * ENHANCED: Comprehensive model registration debugging
 */
@Mod.EventBusSubscriber(modid = "justsomeswitches", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class JustSomeSwitchesClientSetup {

    /**
     * Client setup event - called during client-side initialization
     */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        System.out.println("DEBUG Client: ===== CLIENT SETUP STARTING =====");

        // Register screens for our menu types
        event.enqueueWork(() -> {
            System.out.println("DEBUG Client: Registering GUI screen");

            // Register the Switch Texture GUI screen
            MenuScreens.register(
                    JustSomeSwitchesMenuTypes.SWITCH_TEXTURE_MENU.get(),
                    SwitchTextureScreen::new
            );

            System.out.println("DEBUG Client: Registering Block Entity Renderer");

            // ALTERNATIVE APPROACH: Register Block Entity Renderer instead of custom models
            net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
                    net.justsomeswitches.init.JustSomeSwitchesModBlockEntities.SWITCHES_LEVER.get(),
                    net.justsomeswitches.client.renderer.SwitchesLeverRenderer::new
            );

            System.out.println("DEBUG Client: ===== CLIENT SETUP COMPLETE =====");
        });
    }

    /**
     * Model baking event - DISABLED for Block Entity Renderer approach
     * ---
     * We're switching to Block Entity Renderer which is more reliable for texture replacement
     */
    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        // DISABLED: Custom model replacement - using Block Entity Renderer instead
        System.out.println("DEBUG Client: Using Block Entity Renderer approach - skipping custom model registration");

        /*
        Map<ResourceLocation, BakedModel> models = event.getModels();

        System.out.println("DEBUG Client: Starting model replacement with enhanced debugging");

        // ENHANCED DEBUG: List ALL switch-related models before replacement
        System.out.println("DEBUG Client: Available switch models before replacement:");
        models.keySet().stream()
                .filter(location -> location.getNamespace().equals("justsomeswitches") &&
                                   location.getPath().contains("switches_lever"))
                .forEach(location -> System.out.println("  - " + location));

        // Replace switch lever models with enhanced coverage verification
        replaceSwitchModelsWithDebug(models);
        */
    }

    /**
     * ENHANCED: Replace switch models with comprehensive debugging and coverage verification
     */
    private static void replaceSwitchModelsWithDebug(Map<ResourceLocation, BakedModel> models) {
        int replacedCount = 0;

        // Track which powered states we find
        boolean foundPoweredTrue = false;
        boolean foundPoweredFalse = false;

        // Create a copy of the entries to avoid concurrent modification
        var entries = new java.util.ArrayList<>(models.entrySet());

        System.out.println("DEBUG Client: Processing " + entries.size() + " total models");

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

                    // Track powered states
                    boolean isPowered = representativeState.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED);
                    if (isPowered) {
                        foundPoweredTrue = true;
                    } else {
                        foundPoweredFalse = true;
                    }

                    // ENHANCED DEBUG: Log each successful replacement
                    System.out.println("DEBUG Client: ✅ Replaced model: " + location +
                            " (powered=" + isPowered + ")");
                } else {
                    System.out.println("DEBUG Client: ❌ Could not determine blockstate for: " + location);
                }
            }
        }

        System.out.println("DEBUG Client: Model replacement summary:");
        System.out.println("  - Total models replaced: " + replacedCount);
        System.out.println("  - Powered=true coverage: " + (foundPoweredTrue ? "✅ YES" : "❌ NO"));
        System.out.println("  - Powered=false coverage: " + (foundPoweredFalse ? "✅ YES" : "❌ NO"));

        if (!foundPoweredTrue || !foundPoweredFalse) {
            System.out.println("DEBUG Client: ⚠️  WARNING - Incomplete powered state coverage!");
            System.out.println("DEBUG Client: This could cause texture resets during lever toggles!");
        }

        // ENHANCED DEBUG: Verify final model state
        System.out.println("DEBUG Client: Final switch models after replacement:");
        models.entrySet().stream()
                .filter(entry -> entry.getKey().getNamespace().equals("justsomeswitches") &&
                        entry.getKey().getPath().contains("switches_lever"))
                .forEach(entry -> {
                    String modelType = entry.getValue() instanceof SwitchesLeverModel ? "CUSTOM" : "VANILLA";
                    System.out.println("  - " + entry.getKey() + " → " + modelType);
                });
    }

    /**
     * ENHANCED: Get representative blockstate with better powered state detection
     */
    private static BlockState getRepresentativeState(ResourceLocation modelLocation) {
        try {
            // Default to unpowered state
            BlockState defaultState = JustSomeSwitchesModBlocks.SWITCHES_LEVER.get().defaultBlockState();

            // ENHANCED: Better detection of powered variants
            String path = modelLocation.getPath();

            // Check for "_on" suffix OR "powered=true" in path
            boolean isPowered = path.contains("_on") || path.contains("powered=true");

            if (isPowered && defaultState.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED)) {
                BlockState poweredState = defaultState.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED, true);
                System.out.println("DEBUG Client: Created powered=true state for: " + modelLocation);
                return poweredState;
            } else {
                System.out.println("DEBUG Client: Created powered=false state for: " + modelLocation);
                return defaultState;
            }
        } catch (Exception e) {
            System.out.println("DEBUG Client: Error creating blockstate for " + modelLocation + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * Enhanced models loaded debug event
     */
    @SubscribeEvent
    public static void onModelsLoaded(ModelEvent.BakingCompleted event) {
        System.out.println("DEBUG Client: Model baking completed - custom switch models should be active");

        // Final verification of model registration
        Map<ResourceLocation, BakedModel> models = event.getModels();
        long customModelCount = models.entrySet().stream()
                .filter(entry -> entry.getKey().getNamespace().equals("justsomeswitches") &&
                        entry.getValue() instanceof SwitchesLeverModel)
                .count();

        System.out.println("DEBUG Client: Final custom model count: " + customModelCount);

        if (customModelCount == 0) {
            System.out.println("DEBUG Client: ⚠️  WARNING - No custom models found after baking!");
        }
    }
}