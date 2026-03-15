package net.justsomeswitches.client;

import net.justsomeswitches.client.model.SwitchesGeometryLoader;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Client-side setup for custom geometry loader registration. */
@EventBusSubscriber(modid = "justsomeswitches", value = Dist.CLIENT)
public class JustSomeSwitchesClientSetup {

    /** Registers custom geometry loaders for dynamic texture support and proper lighting integration. */
    @SubscribeEvent
    public static void onRegisterGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(
                SwitchesGeometryLoader.ID,
                SwitchesGeometryLoader.INSTANCE
        );
    }
}
