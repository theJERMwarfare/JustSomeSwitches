package net.justsomeswitches.client;

import net.justsomeswitches.JustSomeSwitchesMod;
import net.justsomeswitches.client.color.SwitchBlockColorHandler;
import net.justsomeswitches.init.JustSomeSwitchesModBlocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;

/** Client-side mod event handler for registering color handlers. */
@Mod.EventBusSubscriber(modid = JustSomeSwitchesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {
    
    /**
     * Registers block color handlers for dynamic switch tinting.
     * Fired on mod event bus, client-side only.
     */
    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        SwitchBlockColorHandler colorHandler = new SwitchBlockColorHandler();
        
        event.register(colorHandler,
            JustSomeSwitchesModBlocks.SWITCHES_LEVER.get(),
            JustSomeSwitchesModBlocks.SWITCHES_ROCKER.get(),
            JustSomeSwitchesModBlocks.SWITCHES_SLIDE.get(),
            JustSomeSwitchesModBlocks.SWITCHES_BUTTONS.get()
        );
        
        JustSomeSwitchesMod.LOGGER.info("Block color handlers registered for switches");
    }
}
