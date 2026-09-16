package net.justsomeswitches.client;

import net.justsomeswitches.JustSomeSwitchesMod;
import net.justsomeswitches.client.color.SwitchBlockColorHandler;
import net.justsomeswitches.init.JustSomeSwitchesModBlocks;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** Client-side mod event handler for registering color handlers. */
@EventBusSubscriber(modid = JustSomeSwitchesMod.MODID, value = Dist.CLIENT)
public class ClientModEvents {
    
    /** Registers the brush mode keybind. Ships unbound; players or pack authors assign it. */
    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(BrushModeInputHandler.CYCLE_MODE);
    }

    /**
     * Registers the brush mode HUD. Anchored after the item name rather than the hotbar because the
     * health, armour and food layers run in between and are what set Gui's row counters, which the
     * HUD reads to place itself clear of the action bar.
     */
    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.SELECTED_ITEM_NAME,
            ResourceLocation.fromNamespaceAndPath(JustSomeSwitchesMod.MODID, "brush_mode_hud"),
            (graphics, deltaTracker) -> BrushModeHudLayer.render(graphics));
    }

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
            JustSomeSwitchesModBlocks.SWITCHES_BUTTONS.get(),
            JustSomeSwitchesModBlocks.SWITCHES_TOUCH.get()
        );
        
        JustSomeSwitchesMod.LOGGER.info("Block color handlers registered for switches");
    }
}
