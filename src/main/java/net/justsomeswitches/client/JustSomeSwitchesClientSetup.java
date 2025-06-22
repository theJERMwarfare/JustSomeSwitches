package net.justsomeswitches.client;

import net.justsomeswitches.client.renderer.SwitchesLeverBlockEntityRenderer;
import net.justsomeswitches.gui.JustSomeSwitchesMenuTypes;
import net.justsomeswitches.gui.SwitchTextureScreen;
import net.justsomeswitches.init.JustSomeSwitchesModBlockEntities;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client-side setup and registration for Just Some Switches mod
 * ---
 * Phase 3C FINAL: Enhanced Block Entity Renderer approach with proper model rendering
 * Registers Block Entity Renderer that replaces vanilla model rendering when custom textures are applied.
 * This class is only loaded on the client side.
 */
@Mod.EventBusSubscriber(modid = "justsomeswitches", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class JustSomeSwitchesClientSetup {

    /**
     * Client setup event - called during client-side initialization
     * Used for registering client-only content like screens and Block Entity Renderers
     * ---
     * Phase 3C FINAL: Enhanced Block Entity Renderer for full model replacement
     *
     * @param event The client setup event
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

            // Phase 3C FINAL: Register Enhanced Block Entity Renderer for full model replacement
            BlockEntityRenderers.register(
                    JustSomeSwitchesModBlockEntities.SWITCHES_LEVER.get(),
                    SwitchesLeverBlockEntityRenderer::new
            );
        });
    }
}