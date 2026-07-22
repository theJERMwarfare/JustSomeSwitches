package net.justsomeswitches.init;

import net.justsomeswitches.JustSomeSwitchesMod;
import net.justsomeswitches.block.CustomizableLeverBlock;
import net.justsomeswitches.block.CustomizableRockerBlock;
import net.justsomeswitches.block.CustomizableButtonBlock;
import net.justsomeswitches.block.CustomizableSlideBlock;
import net.justsomeswitches.block.CustomizableTouchBlock;
import net.justsomeswitches.block.BasicLeverBlock;
import net.justsomeswitches.block.BasicLeverInvertedBlock;
import net.justsomeswitches.block.BasicRockerBlock;
import net.justsomeswitches.block.BasicRockerInvertedBlock;
import net.justsomeswitches.block.BasicButtonBlock;
import net.justsomeswitches.block.BasicButtonInvertedBlock;
import net.justsomeswitches.block.BasicSlideBlock;
import net.justsomeswitches.block.BasicSlideInvertedBlock;
import net.justsomeswitches.block.BasicTouchBlock;
import net.justsomeswitches.block.BasicTouchInvertedBlock;
import net.justsomeswitches.item.SwitchTextureBrushItem;
import net.justsomeswitches.item.SwitchBlockItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** Registers all blocks and items including switch variants and tools. */
public class JustSomeSwitchesModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, JustSomeSwitchesMod.MODID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, JustSomeSwitchesMod.MODID);

    /** Switches Lever block - customizable lever with block entity for texture storage. */
    public static final RegistryObject<CustomizableLeverBlock> SWITCHES_LEVER =
            BLOCKS.register("switches_lever", () -> new CustomizableLeverBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(0.5F)
                            .noOcclusion()
                            .pushReaction(PushReaction.DESTROY)
                            .noCollission()
            ));

    /** Switches Rocker block - customizable rocker with block entity for texture storage. */
    public static final RegistryObject<CustomizableRockerBlock> SWITCHES_ROCKER =
            BLOCKS.register("switches_rocker", () -> new CustomizableRockerBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(0.5F)
                            .noOcclusion()
                            .pushReaction(PushReaction.DESTROY)
                            .noCollission()
            ));
    /** Switches Slide block - customizable slide switch with block entity for texture storage. */
    public static final RegistryObject<CustomizableSlideBlock> SWITCHES_SLIDE =
            BLOCKS.register("switches_slide", () -> new CustomizableSlideBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(0.5F)
                            .noOcclusion()
                            .pushReaction(PushReaction.DESTROY)
                            .noCollission()
            ));
    /** Switches Buttons block - customizable buttons switch with block entity for texture storage. */
    public static final RegistryObject<CustomizableButtonBlock> SWITCHES_BUTTONS =
            BLOCKS.register("switches_buttons", () -> new CustomizableButtonBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(0.5F)
                            .noOcclusion()
                            .pushReaction(PushReaction.DESTROY)
                            .noCollission()
            ));
    /** Switches Touch block - customizable touch switch with block entity for texture storage. */
    public static final RegistryObject<CustomizableTouchBlock> SWITCHES_TOUCH =
            BLOCKS.register("switches_touch", () -> new CustomizableTouchBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(0.5F)
                            .noOcclusion()
                            .pushReaction(PushReaction.DESTROY)
                            .noCollission()
            ));
    /** Basic Lever block - simple lever without customization. */
    public static final RegistryObject<BasicLeverBlock> BASIC_LEVER =
            BLOCKS.register("basic_lever", () -> new BasicLeverBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(0.5F)
                            .noOcclusion()
                            .pushReaction(PushReaction.DESTROY)
                            .noCollission()
            ));

    /** Basic Lever Inverted block - simple lever with inverted visual appearance. */
    public static final RegistryObject<BasicLeverInvertedBlock> BASIC_LEVER_INVERTED =
            BLOCKS.register("basic_lever_inverted", () -> new BasicLeverInvertedBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(0.5F)
                            .noOcclusion()
                            .pushReaction(PushReaction.DESTROY)
                            .noCollission()
            ));

    /** Basic Rocker block - simple rocker switch without customization. */
    public static final RegistryObject<BasicRockerBlock> BASIC_ROCKER =
            BLOCKS.register("basic_rocker", () -> new BasicRockerBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(0.5F)
                            .noOcclusion()
                            .pushReaction(PushReaction.DESTROY)
                            .noCollission()
            ));

    /** Basic Rocker Inverted block - simple rocker switch with inverted visual appearance. */
    public static final RegistryObject<BasicRockerInvertedBlock> BASIC_ROCKER_INVERTED =
            BLOCKS.register("basic_rocker_inverted", () -> new BasicRockerInvertedBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(0.5F)
                            .noOcclusion()
                            .pushReaction(PushReaction.DESTROY)
                            .noCollission()
            ));

    /** Basic Buttons block - simple button switch without customization. */
    public static final RegistryObject<BasicButtonBlock> BASIC_BUTTONS =
            BLOCKS.register("basic_buttons", () -> new BasicButtonBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(0.5F)
                            .noOcclusion()
                            .pushReaction(PushReaction.DESTROY)
                            .noCollission()
            ));

    /** Basic Buttons Inverted block - simple button switch with inverted visual appearance. */
    public static final RegistryObject<BasicButtonInvertedBlock> BASIC_BUTTONS_INVERTED =
            BLOCKS.register("basic_buttons_inverted", () -> new BasicButtonInvertedBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(0.5F)
                            .noOcclusion()
                            .pushReaction(PushReaction.DESTROY)
                            .noCollission()
            ));

    /** Basic Slide block - simple slide switch without customization. */
    public static final RegistryObject<BasicSlideBlock> BASIC_SLIDE =
            BLOCKS.register("basic_slide", () -> new BasicSlideBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(0.5F)
                            .noOcclusion()
                            .pushReaction(PushReaction.DESTROY)
                            .noCollission()
            ));

    /** Basic Slide Inverted block - simple slide switch with inverted visual appearance. */
    public static final RegistryObject<BasicSlideInvertedBlock> BASIC_SLIDE_INVERTED =
            BLOCKS.register("basic_slide_inverted", () -> new BasicSlideInvertedBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(0.5F)
                            .noOcclusion()
                            .pushReaction(PushReaction.DESTROY)
                            .noCollission()
            ));

    /** Basic Touch block - simple touch switch without customization. */
    public static final RegistryObject<BasicTouchBlock> BASIC_TOUCH =
            BLOCKS.register("basic_touch", () -> new BasicTouchBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(0.5F)
                            .noOcclusion()
                            .pushReaction(PushReaction.DESTROY)
                            .noCollission()
            ));

    /** Basic Touch Inverted block - simple touch switch with inverted physical orientation. */
    public static final RegistryObject<BasicTouchInvertedBlock> BASIC_TOUCH_INVERTED =
            BLOCKS.register("basic_touch_inverted", () -> new BasicTouchInvertedBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(0.5F)
                            .noOcclusion()
                            .pushReaction(PushReaction.DESTROY)
                            .noCollission()
            ));

    /** Switches Lever item - uses custom placement behavior. */
    public static final RegistryObject<SwitchBlockItem> SWITCHES_LEVER_ITEM =
            ITEMS.register("switches_lever", () -> new SwitchBlockItem(
                    SWITCHES_LEVER.get(),
                    new Item.Properties()
            ));

    /** Switches Rocker item - uses custom placement behavior. */
    public static final RegistryObject<SwitchBlockItem> SWITCHES_ROCKER_ITEM =
            ITEMS.register("switches_rocker", () -> new SwitchBlockItem(
                    SWITCHES_ROCKER.get(),
                    new Item.Properties()
            ));
    /** Switches Slide item - uses custom placement behavior. */
    public static final RegistryObject<SwitchBlockItem> SWITCHES_SLIDE_ITEM =
            ITEMS.register("switches_slide", () -> new SwitchBlockItem(
                    SWITCHES_SLIDE.get(),
                    new Item.Properties()
            ));
    /** Switches Buttons item - uses custom placement behavior. */
    public static final RegistryObject<SwitchBlockItem> SWITCHES_BUTTONS_ITEM =
            ITEMS.register("switches_buttons", () -> new SwitchBlockItem(
                    SWITCHES_BUTTONS.get(),
                    new Item.Properties()
            ));
    /** Switches Touch item - uses custom placement behavior. */
    public static final RegistryObject<SwitchBlockItem> SWITCHES_TOUCH_ITEM =
            ITEMS.register("switches_touch", () -> new SwitchBlockItem(
                    SWITCHES_TOUCH.get(),
                    new Item.Properties()
            ));
    /** Switch Texture Brush - opens texture customization GUI on shift-right-click. */
    public static final RegistryObject<SwitchTextureBrushItem> SWITCH_TEXTURE_BRUSH =
            ITEMS.register("switch_texture_brush", () -> new SwitchTextureBrushItem(
                    new Item.Properties()
                            .stacksTo(1)
            ));

    public static final RegistryObject<BlockItem> BASIC_LEVER_ITEM =
            ITEMS.register("basic_lever", () -> new BlockItem(
                    BASIC_LEVER.get(),
                    new Item.Properties()
            ));

    public static final RegistryObject<BlockItem> BASIC_LEVER_INVERTED_ITEM =
            ITEMS.register("basic_lever_inverted", () -> new BlockItem(
                    BASIC_LEVER_INVERTED.get(),
                    new Item.Properties()
            ));

    public static final RegistryObject<BlockItem> BASIC_ROCKER_ITEM =
            ITEMS.register("basic_rocker", () -> new BlockItem(
                    BASIC_ROCKER.get(),
                    new Item.Properties()
            ));

    public static final RegistryObject<BlockItem> BASIC_ROCKER_INVERTED_ITEM =
            ITEMS.register("basic_rocker_inverted", () -> new BlockItem(
                    BASIC_ROCKER_INVERTED.get(),
                    new Item.Properties()
            ));

    public static final RegistryObject<BlockItem> BASIC_BUTTONS_ITEM =
            ITEMS.register("basic_buttons", () -> new BlockItem(
                    BASIC_BUTTONS.get(),
                    new Item.Properties()
            ));

    public static final RegistryObject<BlockItem> BASIC_BUTTONS_INVERTED_ITEM =
            ITEMS.register("basic_buttons_inverted", () -> new BlockItem(
                    BASIC_BUTTONS_INVERTED.get(),
                    new Item.Properties()
            ));

    public static final RegistryObject<BlockItem> BASIC_SLIDE_ITEM =
            ITEMS.register("basic_slide", () -> new BlockItem(
                    BASIC_SLIDE.get(),
                    new Item.Properties()
            ));

    public static final RegistryObject<BlockItem> BASIC_SLIDE_INVERTED_ITEM =
            ITEMS.register("basic_slide_inverted", () -> new BlockItem(
                    BASIC_SLIDE_INVERTED.get(),
                    new Item.Properties()
            ));

    public static final RegistryObject<BlockItem> BASIC_TOUCH_ITEM =
            ITEMS.register("basic_touch", () -> new BlockItem(
                    BASIC_TOUCH.get(),
                    new Item.Properties()
            ));

    public static final RegistryObject<BlockItem> BASIC_TOUCH_INVERTED_ITEM =
            ITEMS.register("basic_touch_inverted", () -> new BlockItem(
                    BASIC_TOUCH_INVERTED.get(),
                    new Item.Properties()
            ));
}
