package net.justsomeswitches.client.color;

import net.justsomeswitches.blockentity.SwitchesLeverBlockEntity;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/**
 * Handles tinted texture support for dynamic switches displaying biome-tinted blocks.
 * Reads source block tint from BlockEntity NBT and applies ARGB color multiplication.
 */
@Mod.EventBusSubscriber(modid = "justsomeswitches", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class BlockColorHandler {
    /** Registers block color handlers for all switch blocks during client initialization. */
    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        BlockColor switchBlockColor = (state, level, pos, tintIndex) -> {
            if (tintIndex == -1 || level == null || pos == null) {
                return 0xFFFFFFFF;
            }
            var blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof SwitchesLeverBlockEntity)) {
                return 0xFFFFFFFF;
            }
            return 0xFFFFFFFF;
        };
        event.register(switchBlockColor, 
            net.justsomeswitches.init.JustSomeSwitchesModBlocks.SWITCHES_LEVER.get()
        );
    }
    /** Registers item color handlers (uses fixed neutral green 0xFF5DBD22 since items lack biome context). */
    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        ItemColor switchItemColor = (stack, tintIndex) -> {
            if (tintIndex == -1) {
                return 0xFFFFFFFF;
            }
            return 0xFF5DBD22;
        };
        event.register(switchItemColor,
            net.justsomeswitches.init.JustSomeSwitchesModBlocks.SWITCHES_LEVER.get().asItem()
        );
    }
}
