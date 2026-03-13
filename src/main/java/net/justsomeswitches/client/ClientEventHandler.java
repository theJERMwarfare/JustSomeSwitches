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
            MenuScreens.register(JustSomeSwitchesMenuTypes.SWITCH_TEXTURE_MENU.get(), SwitchesTextureScreen::new);
            MenuScreens.register(JustSomeSwitchesMenuTypes.WRENCH_COPY.get(), WrenchCopyScreen::new);
            MenuScreens.register(JustSomeSwitchesMenuTypes.WRENCH_OVERWRITE.get(), WrenchOverwriteScreen::new);
            MenuScreens.register(JustSomeSwitchesMenuTypes.WRENCH_COPY_OVERWRITE.get(), WrenchCopyOverwriteScreen::new);
            MenuScreens.register(JustSomeSwitchesMenuTypes.WRENCH_MISSING_BLOCK.get(), WrenchMissingBlockScreen::new);
        });
    }
}
