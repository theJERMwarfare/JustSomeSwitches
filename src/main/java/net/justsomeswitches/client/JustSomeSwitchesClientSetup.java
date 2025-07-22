package net.justsomeswitches.client;

import net.justsomeswitches.gui.JustSomeSwitchesMenuTypes;
import net.justsomeswitches.gui.SwitchTextureScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client-side setup and registration for Just Some Switches mod
 * ---
 * CLEANED: Removed failed 3D custom texture preview system
 * PRESERVED: Working GUI and Block Entity Renderer registrations
 */
@Mod.EventBusSubscriber(modid = "justsomeswitches", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class JustSomeSwitchesClientSetup {

    /**
     * Client setup event - called during client-side initialization
     */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Register screens for our menu types
        event.enqueueWork(() -> {
            // Register the Switch Texture GUI screen
            MenuScreens.register(
                    JustSomeSwitchesMenuTypes.SWITCH_TEXTURE_MENU.get(),
                    SwitchTextureScreen::new
            );

            // Register Block Entity Renderer for world block custom textures
            net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
                    net.justsomeswitches.init.JustSomeSwitchesModBlockEntities.SWITCHES_LEVER.get(),
                    net.justsomeswitches.client.renderer.SwitchesLeverRenderer::new
            );
        });
    }
}