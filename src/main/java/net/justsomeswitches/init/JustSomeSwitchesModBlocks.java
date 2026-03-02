package net.justsomeswitches.init;

import net.justsomeswitches.JustSomeSwitchesMod;
import net.justsomeswitches.block.SwitchesLeverBlock;
import net.justsomeswitches.block.SwitchesRockerBlock;
import net.justsomeswitches.block.SwitchesSlideBlock;
import net.justsomeswitches.block.BasicLeverBlock;
import net.justsomeswitches.block.BasicLeverInvertedBlock;
import net.justsomeswitches.block.BasicRockerBlock;
import net.justsomeswitches.block.BasicRockerInvertedBlock;
import net.justsomeswitches.block.BasicButtonsBlock;
import net.justsomeswitches.block.BasicButtonsInvertedBlock;
import net.justsomeswitches.block.BasicSlideBlock;
import net.justsomeswitches.block.BasicSlideInvertedBlock;
import net.justsomeswitches.item.SwitchesWrenchItem;
import net.justsomeswitches.item.SwitchBlockItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Registers all blocks and items including switch variants and tools. */
public class JustSomeSwitchesModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, JustSomeSwitchesMod.MODID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, JustSomeSwitchesMod.MODID);

    /** Switches Lever block - customizable lever with block entity for texture storage. */
    public static final DeferredHolder<Block, SwitchesLeverBlock> SWITCHES_LEVER =
            BLOCKS.register("switches_lever", () -> new SwitchesLeverBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(0.5F)
                            .noOcclusion()
                            .pushReaction(PushReaction.DESTROY)
                            .noCollission()
            ));

    /** Switches Rocker block - customizable rocker with block entity for texture storage. */
    public static final DeferredHolder<Block, SwitchesRockerBlock> SWITCHES_ROCKER =
            BLOCKS.register("switches_rocker", () -> new SwitchesRockerBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(0.5F)
                            .noOcclusion()
                            .pushReaction(PushReaction.DESTROY)
                            .noCollission()
            ));
    /** Switches Slide block - customizable slide switch with block entity for texture storage. */
    public static final DeferredHolder<Block, SwitchesSlideBlock> SWITCHES_SLIDE =
            BLOCKS.register("switches_slide", () -> new SwitchesSlideBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(0.5F)
                            .noOcclusion()
                            .pushReaction(PushReaction.DESTROY)
                            .noCollission()
            ));
    /** Basic Lever block - simple lever without customization. */
    public static final DeferredHolder<Block, BasicLeverBlock> BASIC_LEVER =
            BLOCKS.register("basic_lever", () -> new BasicLeverBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(0.5F)
                            .noOcclusion()
                            .pushReaction(PushReaction.DESTROY)
                            .noCollission()
            ));

    /** Basic Lever Inverted block - simple lever with inverted signal. */
    public static final DeferredHolder<Block, BasicLeverInvertedBlock> BASIC_LEVER_INVERTED =
            BLOCKS.register("basic_lever_inverted", () -> new BasicLeverInvertedBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(0.5F)
                            .noOcclusion()
                            .pushReaction(PushReaction.DESTROY)
                            .noCollission()
            ));

    /** Basic Rocker block - simple rocker switch without customization. */
    public static final DeferredHolder<Block, BasicRockerBlock> BASIC_ROCKER =
            BLOCKS.register("basic_rocker", () -> new BasicRockerBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(0.5F)
                            .noOcclusion()
                            .pushReaction(PushReaction.DESTROY)
                            .noCollission()
            ));

    /** Basic Rocker Inverted block - simple rocker switch with inverted signal. */
    public static final DeferredHolder<Block, BasicRockerInvertedBlock> BASIC_ROCKER_INVERTED =
            BLOCKS.register("basic_rocker_inverted", () -> new BasicRockerInvertedBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(0.5F)
                            .noOcclusion()
                            .pushReaction(PushReaction.DESTROY)
                            .noCollission()
            ));

    /** Basic Buttons block - simple button switch without customization. */
    public static final DeferredHolder<Block, BasicButtonsBlock> BASIC_BUTTONS =
            BLOCKS.register("basic_buttons", () -> new BasicButtonsBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(0.5F)
                            .noOcclusion()
                            .pushReaction(PushReaction.DESTROY)
                            .noCollission()
            ));

    /** Basic Buttons Inverted block - simple button switch with inverted signal. */
    public static final DeferredHolder<Block, BasicButtonsInvertedBlock> BASIC_BUTTONS_INVERTED =
            BLOCKS.register("basic_buttons_inverted", () -> new BasicButtonsInvertedBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(0.5F)
                            .noOcclusion()
                            .pushReaction(PushReaction.DESTROY)
                            .noCollission()
            ));

    /** Basic Slide block - simple slide switch without customization. */
    public static final DeferredHolder<Block, BasicSlideBlock> BASIC_SLIDE =
            BLOCKS.register("basic_slide", () -> new BasicSlideBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(0.5F)
                            .noOcclusion()
                            .pushReaction(PushReaction.DESTROY)
                            .noCollission()
            ));

    /** Basic Slide Inverted block - simple slide switch with inverted signal. */
    public static final DeferredHolder<Block, BasicSlideInvertedBlock> BASIC_SLIDE_INVERTED =
            BLOCKS.register("basic_slide_inverted", () -> new BasicSlideInvertedBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(0.5F)
                            .noOcclusion()
                            .pushReaction(PushReaction.DESTROY)
                            .noCollission()
            ));

    /** Switches Lever item - uses custom placement behavior. */
    public static final DeferredHolder<Item, SwitchBlockItem> SWITCHES_LEVER_ITEM =
            ITEMS.register("switches_lever", () -> new SwitchBlockItem(
                    SWITCHES_LEVER.get(),
                    new Item.Properties()
            ));

    /** Switches Rocker item - uses custom placement behavior. */
    public static final DeferredHolder<Item, SwitchBlockItem> SWITCHES_ROCKER_ITEM =
            ITEMS.register("switches_rocker", () -> new SwitchBlockItem(
                    SWITCHES_ROCKER.get(),
                    new Item.Properties()
            ));
    /** Switches Slide item - uses custom placement behavior. */
    public static final DeferredHolder<Item, SwitchBlockItem> SWITCHES_SLIDE_ITEM =
            ITEMS.register("switches_slide", () -> new SwitchBlockItem(
                    SWITCHES_SLIDE.get(),
                    new Item.Properties()
            ));
    /** Switches Wrench - opens texture customization GUI on shift-right-click. */
    public static final DeferredHolder<Item, SwitchesWrenchItem> SWITCHES_WRENCH =
            ITEMS.register("switches_wrench", () -> new SwitchesWrenchItem(
                    new Item.Properties()
                            .stacksTo(1)
            ));

    public static final DeferredHolder<Item, BlockItem> BASIC_LEVER_ITEM =
            ITEMS.register("basic_lever", () -> new BlockItem(
                    BASIC_LEVER.get(),
                    new Item.Properties()
            ));

    public static final DeferredHolder<Item, BlockItem> BASIC_LEVER_INVERTED_ITEM =
            ITEMS.register("basic_lever_inverted", () -> new BlockItem(
                    BASIC_LEVER_INVERTED.get(),
                    new Item.Properties()
            ));

    public static final DeferredHolder<Item, BlockItem> BASIC_ROCKER_ITEM =
            ITEMS.register("basic_rocker", () -> new BlockItem(
                    BASIC_ROCKER.get(),
                    new Item.Properties()
            ));

    public static final DeferredHolder<Item, BlockItem> BASIC_ROCKER_INVERTED_ITEM =
            ITEMS.register("basic_rocker_inverted", () -> new BlockItem(
                    BASIC_ROCKER_INVERTED.get(),
                    new Item.Properties()
            ));

    public static final DeferredHolder<Item, BlockItem> BASIC_BUTTONS_ITEM =
            ITEMS.register("basic_buttons", () -> new BlockItem(
                    BASIC_BUTTONS.get(),
                    new Item.Properties()
            ));

    public static final DeferredHolder<Item, BlockItem> BASIC_BUTTONS_INVERTED_ITEM =
            ITEMS.register("basic_buttons_inverted", () -> new BlockItem(
                    BASIC_BUTTONS_INVERTED.get(),
                    new Item.Properties()
            ));

    public static final DeferredHolder<Item, BlockItem> BASIC_SLIDE_ITEM =
            ITEMS.register("basic_slide", () -> new BlockItem(
                    BASIC_SLIDE.get(),
                    new Item.Properties()
            ));

    public static final DeferredHolder<Item, BlockItem> BASIC_SLIDE_INVERTED_ITEM =
            ITEMS.register("basic_slide_inverted", () -> new BlockItem(
                    BASIC_SLIDE_INVERTED.get(),
                    new Item.Properties()
            ));
}
