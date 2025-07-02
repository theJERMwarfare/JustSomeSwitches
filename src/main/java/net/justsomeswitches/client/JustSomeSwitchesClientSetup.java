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
 * FIXED: Client-side setup with Custom Model enabled and BlockEntityRenderer removed
 * ---
 * CRITICAL FIX: Eliminate z-fighting by using ONLY custom model approach
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

            // FIXED: REMOVED BlockEntityRenderer registration to eliminate z-fighting
            // Using ONLY custom model approach for clean rendering
            System.out.println("DEBUG Client: Using CUSTOM MODEL approach - no BlockEntityRenderer needed");

            System.out.println("DEBUG Client: ===== CLIENT SETUP COMPLETE =====");
        });
    }

    /**
     * FIXED: Custom model registration enabled with comprehensive debugging
     */
    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<ResourceLocation, BakedModel> models = event.getModels();

        System.out.println("DEBUG Client: Starting custom model registration");

        // Replace switch lever models with our custom model
        int replacedCount = replaceSwitchModels(models);

        System.out.println("DEBUG Client: Model baking completed - custom switch models active");
        System.out.println("DEBUG Client: Final custom model count: " + replacedCount);

        if (replacedCount == 0) {
            System.out.println("DEBUG Client: ⚠️ WARNING - No custom models found after baking!");
        }
    }

    /**
     * Replace switch models with custom texture-replaceable models
     */
    private static int replaceSwitchModels(Map<ResourceLocation, BakedModel> models) {
        int replacedCount = 0;

        // Create a copy of the entries to avoid concurrent modification
        var entries = new java.util.ArrayList<>(models.entrySet());

        for (var entry : entries) {
            ResourceLocation location = entry.getKey();
            BakedModel originalModel = entry.getValue();

            // Check if this is a switches lever model
            if (isSwitchesLeverModel(location)) {
                System.out.println("DEBUG Client: Replacing model: " + location);

                // Get representative block state for this model
                BlockState state = getRepresentativeState(location);
                if (state != null) {
                    // Create custom model
                    SwitchesLeverModel customModel = new SwitchesLeverModel(state, originalModel);
                    models.put(location, customModel);
                    replacedCount++;

                    System.out.println("DEBUG Client: ✅ Successfully replaced: " + location);
                } else {
                    System.out.println("DEBUG Client: ❌ Failed to get state for: " + location);
                }
            }
        }

        return replacedCount;
    }

    /**
     * Check if this model location represents a switches lever block
     */
    private static boolean isSwitchesLeverModel(ResourceLocation location) {
        return location.getNamespace().equals("justsomeswitches") &&
                location.getPath().contains("switches_lever");
    }

    /**
     * Get representative BlockState for a model location
     */
    private static BlockState getRepresentativeState(ResourceLocation location) {
        try {
            // Extract state information from model path if needed
            // For now, use default state
            return JustSomeSwitchesModBlocks.SWITCHES_LEVER.get().defaultBlockState();
        } catch (Exception e) {
            System.out.println("DEBUG Client: Error getting representative state: " + e.getMessage());
            return null;
        }
    }
}