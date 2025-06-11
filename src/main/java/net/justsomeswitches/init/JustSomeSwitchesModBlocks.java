package net.justsomeswitches.init;

import net.justsomeswitches.JustSomeSwitchesMod;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegistryObject;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class JustSomeSwitchesModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, JustSomeSwitchesMod.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, JustSomeSwitchesMod.MODID);

    public static final RegistryObject<Block> SWITCHES_LEVER = BLOCKS.register("switches_lever", () ->
            new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion()
            )
    );

    public static final RegistryObject<Item> SWITCHES_LEVER_ITEM = ITEMS.register("switches_lever", () ->
            new BlockItem(SWITCHES_LEVER.get(), new Item.Properties())
    );

    public static void registerAll() {
        BLOCKS.register(JustSomeSwitchesMod.EVENT_BUS);
        ITEMS.register(JustSomeSwitchesMod.EVENT_BUS);
    }
}