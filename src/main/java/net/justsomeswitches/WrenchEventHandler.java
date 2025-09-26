package net.justsomeswitches;

import net.justsomeswitches.init.JustSomeSwitchesModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import javax.annotation.Nonnull;

/**
 * Event handler for Switches Wrench instant block breaking
 */
@Mod.EventBusSubscriber(modid = JustSomeSwitchesMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WrenchEventHandler {
    
    @SubscribeEvent
    public static void onLeftClickBlock(@Nonnull PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        if (!player.getMainHandItem().is(JustSomeSwitchesModBlocks.SWITCHES_WRENCH.get())) {
            return;
        }
        
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState blockState = level.getBlockState(pos);
        
        ResourceLocation blockRegistryName = BuiltInRegistries.BLOCK.getKey(blockState.getBlock());
        if (!blockRegistryName.getNamespace().equals(JustSomeSwitchesMod.MODID)) {
            return;
        }
        
        event.setCanceled(true);
        
        if (!level.isClientSide) {
            level.destroyBlock(pos, true, player);
        }
    }
}