package net.justsomeswitches.client;

import net.justsomeswitches.client.model.GhostBlockDetector;
import net.justsomeswitches.client.model.SwitchesGeometryLoader;
import net.justsomeswitches.gui.JustSomeSwitchesMenuTypes;
import net.justsomeswitches.gui.SwitchTextureScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.event.TickEvent;

/**
 * Client-side setup and registration for Just Some Switches mod
 *
 * Features:
 * - GUI screen registration for switch texture customization
 * - Custom Model Loader registration for proper lighting and performance
 * - Ghost preview system integration
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

/**
 * FORGE bus event handlers for real-time client events.
 * Separated from MOD bus events for proper event handling.
 */
@Mod.EventBusSubscriber(modid = "justsomeswitches", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
class JustSomeSwitchesClientEvents {
    
    // Performance optimization - only update ghost preview every few ticks
    private static int tickCounter = 0;
    private static final int UPDATE_INTERVAL = 2; // Update every 2-3 ticks for responsive preview
    
    /**
     * Client tick event handler for ghost preview updates.
     * Called every client tick for real-time ghost preview functionality.
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        // Only process on tick end to avoid double processing
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        
        // Performance optimization - update every 2-3 ticks
        tickCounter++;
        if (tickCounter < UPDATE_INTERVAL) {
            return;
        }
        tickCounter = 0;
        
        // Update ghost preview through the detection system
        GhostBlockDetector.getInstance().updateGhostPreview();
    }
}
