package net.justsomeswitches.client;

import net.justsomeswitches.client.model.SwitchesGeometryLoader;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.ModelEvent;

/** Client-side setup for custom geometry loader registration. */
@Mod.EventBusSubscriber(modid = "justsomeswitches", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class JustSomeSwitchesClientSetup {

    /** Registers custom geometry loaders for dynamic texture support and proper lighting integration. */
    @SubscribeEvent
    public static void onRegisterGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(
                SwitchesGeometryLoader.ID.getPath(),
                SwitchesGeometryLoader.INSTANCE
        );
    }
}
