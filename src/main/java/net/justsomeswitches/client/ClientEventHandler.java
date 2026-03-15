package net.justsomeswitches.client;

import net.justsomeswitches.JustSomeSwitchesMod;
import net.justsomeswitches.gui.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client-side event handler for menu screen registration. */
@EventBusSubscriber(modid = JustSomeSwitchesMod.MODID, value = Dist.CLIENT)
public class ClientEventHandler {

    /** Registers menu screens for client-side GUI handling. */
    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(JustSomeSwitchesMenuTypes.SWITCH_TEXTURE_MENU.get(), SwitchesTextureScreen::new);
        event.register(JustSomeSwitchesMenuTypes.WRENCH_COPY.get(), WrenchCopyScreen::new);
        event.register(JustSomeSwitchesMenuTypes.WRENCH_OVERWRITE.get(), WrenchOverwriteScreen::new);
        event.register(JustSomeSwitchesMenuTypes.WRENCH_COPY_OVERWRITE.get(), WrenchCopyOverwriteScreen::new);
        event.register(JustSomeSwitchesMenuTypes.WRENCH_MISSING_BLOCK.get(), WrenchMissingBlockScreen::new);
    }
}
