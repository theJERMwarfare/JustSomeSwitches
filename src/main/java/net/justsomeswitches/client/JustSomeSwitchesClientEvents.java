package net.justsomeswitches.client;

import net.justsomeswitches.client.model.GhostBlockDetector;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.TickEvent;

/**
 * FORGE bus event handlers for real-time client events
 * Handles client tick events for ghost preview functionality
 * ---
 * Professional separation: Each class in its own file for maintainability
 */
@Mod.EventBusSubscriber(modid = "justsomeswitches", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class JustSomeSwitchesClientEvents {
    
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
