package net.justsomeswitches.client;

import net.justsomeswitches.JustSomeSwitchesMod;
import net.justsomeswitches.gui.*;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Client-side event handler for menu screen registration. */
@Mod.EventBusSubscriber(modid = JustSomeSwitchesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEventHandler {

    /** Registers menu screens for client-side GUI handling. */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(JustSomeSwitchesMenuTypes.SWITCH_TEXTURE_MENU.get(), CustomizableTextureScreen::new);
            MenuScreens.register(JustSomeSwitchesMenuTypes.BRUSH_COPY.get(), BrushCopyScreen::new);
            MenuScreens.register(JustSomeSwitchesMenuTypes.BRUSH_OVERWRITE.get(), BrushOverwriteScreen::new);
            MenuScreens.register(JustSomeSwitchesMenuTypes.BRUSH_COPY_OVERWRITE.get(), BrushCopyOverwriteScreen::new);
            MenuScreens.register(JustSomeSwitchesMenuTypes.BRUSH_MISSING_BLOCK.get(), BrushMissingBlockScreen::new);
        });
    }
}
