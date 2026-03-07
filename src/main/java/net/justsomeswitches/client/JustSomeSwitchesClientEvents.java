package net.justsomeswitches.client;

import net.justsomeswitches.client.model.GhostBlockDetector;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.TickEvent;

/** FORGE bus event handlers for ghost preview client tick updates. */
@Mod.EventBusSubscriber(modid = "justsomeswitches", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class JustSomeSwitchesClientEvents {

    private static int tickCounter = 0;
    private static final int UPDATE_INTERVAL = 2;

    /** Client tick event handler for ghost preview updates every 2 ticks. */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        tickCounter++;
        if (tickCounter < UPDATE_INTERVAL) {
            return;
        }
        tickCounter = 0;
        GhostBlockDetector.getInstance().updateGhostPreview();
    }
}
