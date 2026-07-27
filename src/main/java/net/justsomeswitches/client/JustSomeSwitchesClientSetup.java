package net.justsomeswitches.client;

import net.justsomeswitches.JustSomeSwitchesMod;
import net.justsomeswitches.client.model.SwitchesGeometryLoader;
import net.justsomeswitches.init.JustSomeSwitchesModBlocks;
import net.justsomeswitches.item.service.CopyPasteService;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Client-side setup for custom geometry loader and item property registration. */
@EventBusSubscriber(modid = "justsomeswitches", value = Dist.CLIENT)
public class JustSomeSwitchesClientSetup {

    /** Registers custom geometry loaders for dynamic texture support and proper lighting integration. */
    @SubscribeEvent
    public static void onRegisterGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(
                SwitchesGeometryLoader.ID,
                SwitchesGeometryLoader.INSTANCE
        );
    }

    /** Registers item property predicates for dynamic textures (Switch Texture Brush active/inactive state). */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(
                JustSomeSwitchesModBlocks.SWITCH_TEXTURE_BRUSH.get(),
                ResourceLocation.fromNamespaceAndPath(JustSomeSwitchesMod.MODID, "has_copied_data"),
                (stack, level, entity, seed) -> CopyPasteService.hasCopiedSettings(stack) ? 1.0F : 0.0F
        ));
    }
}
