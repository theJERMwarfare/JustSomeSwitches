package net.justsomeswitches.client;

import net.justsomeswitches.client.model.SwitchesGeometryLoader;
import net.justsomeswitches.gui.JustSomeSwitchesMenuTypes;
import net.justsomeswitches.gui.SwitchTextureScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * Client-side setup and registration for Just Some Switches mod
 *
 * Features:
 * - GUI screen registration for switch texture customization
 * - Custom Model Loader registration for proper lighting and performance
 */
@Mod.EventBusSubscriber(modid = "justsomeswitches", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class JustSomeSwitchesClientSetup {

    /**
     * Client setup event - called during client-side initialization
     */
    @SubscribeEvent
    @SuppressWarnings("deprecation")
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Register screens for our menu types
        event.enqueueWork(() -> {
            // Register the Switch Texture GUI screen
            MenuScreens.register(
                    JustSomeSwitchesMenuTypes.SWITCH_TEXTURE_MENU.get(),
                    SwitchTextureScreen::new
            );

            // Initialize ModelData integration for proper texture rendering
            // ModelData flows automatically from BlockEntity to Custom Model Loader
        });
    }

    /**
     * Register custom geometry loaders for the Custom Model Loader system.
     * Provides proper lighting integration, performance improvements, and dynamic texture support.
     */
    @SubscribeEvent
    public static void onRegisterGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        // Register our custom model loader for switches lever
        event.register(
                SwitchesGeometryLoader.ID,
                SwitchesGeometryLoader.INSTANCE
        );
    }
}
