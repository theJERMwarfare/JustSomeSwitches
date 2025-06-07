package net.justsomeswitches.init;

import net.justsomeswitches.JustSomeSwitchesMod;
import net.justsomeswitches.block.SwitchesLeverBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class JustSomeSwitchesModBlocks {

    // Create a deferred register for blocks using your mod ID
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(JustSomeSwitchesMod.MODID);

    // Register your custom block (switches_lever) with copied properties from vanilla LEVER
    public static final DeferredBlock<Block> SWITCHES_LEVER =
            BLOCKS.register("switches_lever", () ->
                    new SwitchesLeverBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.LEVER)));
}