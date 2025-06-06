package net.justsomeswitches.init;

import net.justsomeswitches.JustSomeSwitchesMod;
import net.justsomeswitches.block.SwitchesLeverBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class JustSomeSwitchesModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, JustSomeSwitchesMod.MODID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, JustSomeSwitchesMod.MODID);

    public static final DeferredBlock<Block> SWITCHES_LEVER = registerBlock(
            "switches_lever",
            () -> new SwitchesLeverBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.LEVER))
    );

    private static DeferredBlock<Block> registerBlock(String name, Supplier<Block> blockSupplier) {
        DeferredHolder<Block, Block> block = BLOCKS.register(name, blockSupplier);
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return BLOCKS.register(name, blockSupplier);
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }
}