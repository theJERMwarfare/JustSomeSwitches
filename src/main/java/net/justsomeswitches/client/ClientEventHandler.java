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
        event.register(JustSomeSwitchesMenuTypes.SWITCH_TEXTURE_MENU.get(), CustomizableTextureScreen::new);
        event.register(JustSomeSwitchesMenuTypes.BRUSH_COPY.get(), BrushCopyScreen::new);
        event.register(JustSomeSwitchesMenuTypes.BRUSH_OVERWRITE.get(), BrushOverwriteScreen::new);
        event.register(JustSomeSwitchesMenuTypes.BRUSH_COPY_OVERWRITE.get(), BrushCopyOverwriteScreen::new);
        event.register(JustSomeSwitchesMenuTypes.BRUSH_MISSING_BLOCK.get(), BrushMissingBlockScreen::new);
    }
}
