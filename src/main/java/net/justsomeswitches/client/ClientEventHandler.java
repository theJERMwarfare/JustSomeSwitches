package net.justsomeswitches.client;

import net.justsomeswitches.JustSomeSwitchesMod;
import net.justsomeswitches.gui.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * Client-side event handler for Just Some Switches mod
 * Handles client-specific initialization and registrations
 */
@Mod.EventBusSubscriber(modid = JustSomeSwitchesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEventHandler {
    
    /**
     * Register menu screens for client-side GUI handling
     */
    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        // Register Switch Texture GUI screen
        event.register(JustSomeSwitchesMenuTypes.SWITCH_TEXTURE_MENU.get(), SwitchesTextureScreen::new);
        
        // Register Wrench Copy GUI screen
        event.register(JustSomeSwitchesMenuTypes.WRENCH_COPY.get(), WrenchCopyScreen::new);
        
        // Register Wrench Paste Overwrite confirmation screen
        event.register(JustSomeSwitchesMenuTypes.WRENCH_OVERWRITE.get(), WrenchOverwriteScreen::new);
        
        // Register Wrench Copy Overwrite confirmation screen
        event.register(JustSomeSwitchesMenuTypes.WRENCH_COPY_OVERWRITE.get(), WrenchCopyOverwriteScreen::new);
        
        // Register Wrench Missing Block notification screen
        event.register(JustSomeSwitchesMenuTypes.WRENCH_MISSING_BLOCK.get(), WrenchMissingBlockScreen::new);
    }
}
