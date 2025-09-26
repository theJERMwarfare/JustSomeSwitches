package net.justsomeswitches.client;

import net.justsomeswitches.client.model.SwitchesGeometryLoader;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * Client-side setup for Just Some Switches mod
 * Handles MOD bus events for model loader registration only
 * ---
 * Menu screen registration moved to ClientEventHandler using RegisterMenuScreensEvent (modern approach)
 */
@Mod.EventBusSubscriber(modid = "justsomeswitches", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class JustSomeSwitchesClientSetup {
    
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
