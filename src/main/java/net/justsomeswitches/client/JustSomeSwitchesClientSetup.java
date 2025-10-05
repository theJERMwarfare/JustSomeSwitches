package net.justsomeswitches.client;

import net.justsomeswitches.client.model.SwitchesGeometryLoader;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Client-side setup for custom geometry loader registration. */
@Mod.EventBusSubscriber(modid = "justsomeswitches", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
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
