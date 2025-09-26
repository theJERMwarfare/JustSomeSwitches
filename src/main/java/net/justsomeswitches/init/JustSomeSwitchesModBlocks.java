package net.justsomeswitches.init;

import net.justsomeswitches.JustSomeSwitchesMod;
import net.justsomeswitches.block.SwitchesLeverBlock;
import net.justsomeswitches.block.BasicLeverBlock;
import net.justsomeswitches.block.BasicLeverInvertedBlock;
import net.justsomeswitches.block.BasicRockerBlock;
import net.justsomeswitches.block.BasicRockerInvertedBlock;
import net.justsomeswitches.block.BasicButtonsBlock;
import net.justsomeswitches.block.BasicButtonsInvertedBlock;
import net.justsomeswitches.block.BasicSlideBlock;
import net.justsomeswitches.block.BasicSlideInvertedBlock;
import net.justsomeswitches.item.SwitchesWrenchItem;
import net.justsomeswitches.item.SwitchesLeverBlockItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Registration class for all blocks and items in Just Some Switches mod
 * ---
 * This class handles the deferred registration of blocks and their corresponding items.
 * All switch variants and tools will eventually be registered here.
 */
public class JustSomeSwitchesModBlocks {

    // Deferred register for blocks - handles block registration with NeoForge
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, JustSomeSwitchesMod.MODID);

    // Deferred register for items - handles item registration with NeoForge
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, JustSomeSwitchesMod.MODID);

    // ========================================
    // BLOCK REGISTRATIONS
    // ========================================

    /**
     * Switches Lever Block - the main lever variant
     * Properties match vanilla lever but with custom behavior
     */
    public static final DeferredHolder<Block, SwitchesLeverBlock> SWITCHES_LEVER =
            BLOCKS.register("switches_lever", () -> new SwitchesLeverBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)              // Visual map color
                            .strength(0.5F)                        // Same hardness as vanilla lever
                            .noOcclusion()                         // Doesn't block light/transparency
                            .pushReaction(PushReaction.DESTROY)    // Destroyed by pistons like vanilla lever
                            .noCollission()                        // No collision with entities (like vanilla lever)
            ));

    // ========================================
    // BASIC BLOCK REGISTRATIONS (8 BLOCKS)
    // ========================================

    /**
     * Basic Lever Block - simple lever without block entity or custom textures
     */
    public static final DeferredHolder<Block, BasicLeverBlock> BASIC_LEVER =
            BLOCKS.register("basic_lever", () -> new BasicLeverBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(0.5F)
                            .noOcclusion()
                            .pushReaction(PushReaction.DESTROY)
                            .noCollission()
            ));

    /**
     * Basic Lever Inverted Block - simple lever with inverted redstone signal
     */
    public static final DeferredHolder<Block, BasicLeverInvertedBlock> BASIC_LEVER_INVERTED =
            BLOCKS.register("basic_lever_inverted", () -> new BasicLeverInvertedBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(0.5F)
                            .noOcclusion()
                            .pushReaction(PushReaction.DESTROY)
                            .noCollission()
            ));

    /**
     * Basic Rocker Block - simple rocker switch without block entity or custom textures
     */
    public static final DeferredHolder<Block, BasicRockerBlock> BASIC_ROCKER =
            BLOCKS.register("basic_rocker", () -> new BasicRockerBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(0.5F)
                            .noOcclusion()
                            .pushReaction(PushReaction.DESTROY)
                            .noCollission()
            ));

    /**
     * Basic Rocker Inverted Block - simple rocker switch with inverted redstone signal
     */
    public static final DeferredHolder<Block, BasicRockerInvertedBlock> BASIC_ROCKER_INVERTED =
            BLOCKS.register("basic_rocker_inverted", () -> new BasicRockerInvertedBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(0.5F)
                            .noOcclusion()
                            .pushReaction(PushReaction.DESTROY)
                            .noCollission()
            ));

    /**
     * Basic Buttons Block - simple button switch without block entity or custom textures
     */
    public static final DeferredHolder<Block, BasicButtonsBlock> BASIC_BUTTONS =
            BLOCKS.register("basic_buttons", () -> new BasicButtonsBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(0.5F)
                            .noOcclusion()
                            .pushReaction(PushReaction.DESTROY)
                            .noCollission()
            ));

    /**
     * Basic Buttons Inverted Block - simple button switch with inverted redstone signal
     */
    public static final DeferredHolder<Block, BasicButtonsInvertedBlock> BASIC_BUTTONS_INVERTED =
            BLOCKS.register("basic_buttons_inverted", () -> new BasicButtonsInvertedBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(0.5F)
                            .noOcclusion()
                            .pushReaction(PushReaction.DESTROY)
                            .noCollission()
            ));

    /**
     * Basic Slide Block - simple slide switch without block entity or custom textures
     */
    public static final DeferredHolder<Block, BasicSlideBlock> BASIC_SLIDE =
            BLOCKS.register("basic_slide", () -> new BasicSlideBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(0.5F)
                            .noOcclusion()
                            .pushReaction(PushReaction.DESTROY)
                            .noCollission()
            ));

    /**
     * Basic Slide Inverted Block - simple slide switch with inverted redstone signal
     */
    public static final DeferredHolder<Block, BasicSlideInvertedBlock> BASIC_SLIDE_INVERTED =
            BLOCKS.register("basic_slide_inverted", () -> new BasicSlideInvertedBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(0.5F)
                            .noOcclusion()
                            .pushReaction(PushReaction.DESTROY)
                            .noCollission()
            ));

    // ========================================
    // ITEM REGISTRATIONS
    // ========================================

    /**
     * Switches Lever Item - the item form of the switches lever block
     * Uses custom SwitchesLeverBlockItem for advanced placement behavior
     */
    public static final DeferredHolder<Item, SwitchesLeverBlockItem> SWITCHES_LEVER_ITEM =
            ITEMS.register("switches_lever", () -> new SwitchesLeverBlockItem(
                    SWITCHES_LEVER.get(),
                    new Item.Properties()
            ));

    /**
     * Switches Wrench - tool used to customize switches and manage functionality
     * Opens texture customization GUI when shift-right-clicking on switches
     */
    public static final DeferredHolder<Item, SwitchesWrenchItem> SWITCHES_WRENCH =
            ITEMS.register("switches_wrench", () -> new SwitchesWrenchItem(
                    new Item.Properties()
                            .stacksTo(1)    // Only allow 1 in a stack (like tools)
            ));

    // ========================================
    // BASIC BLOCK ITEM REGISTRATIONS (8 ITEMS)
    // ========================================

    /**
     * Basic Lever Item - the item form of the basic lever block
     */
    public static final DeferredHolder<Item, BlockItem> BASIC_LEVER_ITEM =
            ITEMS.register("basic_lever", () -> new BlockItem(
                    BASIC_LEVER.get(),
                    new Item.Properties()
            ));

    /**
     * Basic Lever Inverted Item - the item form of the basic lever inverted block
     */
    public static final DeferredHolder<Item, BlockItem> BASIC_LEVER_INVERTED_ITEM =
            ITEMS.register("basic_lever_inverted", () -> new BlockItem(
                    BASIC_LEVER_INVERTED.get(),
                    new Item.Properties()
            ));

    /**
     * Basic Rocker Item - the item form of the basic rocker block
     */
    public static final DeferredHolder<Item, BlockItem> BASIC_ROCKER_ITEM =
            ITEMS.register("basic_rocker", () -> new BlockItem(
                    BASIC_ROCKER.get(),
                    new Item.Properties()
            ));

    /**
     * Basic Rocker Inverted Item - the item form of the basic rocker inverted block
     */
    public static final DeferredHolder<Item, BlockItem> BASIC_ROCKER_INVERTED_ITEM =
            ITEMS.register("basic_rocker_inverted", () -> new BlockItem(
                    BASIC_ROCKER_INVERTED.get(),
                    new Item.Properties()
            ));

    /**
     * Basic Buttons Item - the item form of the basic buttons block
     */
    public static final DeferredHolder<Item, BlockItem> BASIC_BUTTONS_ITEM =
            ITEMS.register("basic_buttons", () -> new BlockItem(
                    BASIC_BUTTONS.get(),
                    new Item.Properties()
            ));

    /**
     * Basic Buttons Inverted Item - the item form of the basic buttons inverted block
     */
    public static final DeferredHolder<Item, BlockItem> BASIC_BUTTONS_INVERTED_ITEM =
            ITEMS.register("basic_buttons_inverted", () -> new BlockItem(
                    BASIC_BUTTONS_INVERTED.get(),
                    new Item.Properties()
            ));

    /**
     * Basic Slide Item - the item form of the basic slide block
     */
    public static final DeferredHolder<Item, BlockItem> BASIC_SLIDE_ITEM =
            ITEMS.register("basic_slide", () -> new BlockItem(
                    BASIC_SLIDE.get(),
                    new Item.Properties()
            ));

    /**
     * Basic Slide Inverted Item - the item form of the basic slide inverted block
     */
    public static final DeferredHolder<Item, BlockItem> BASIC_SLIDE_INVERTED_ITEM =
            ITEMS.register("basic_slide_inverted", () -> new BlockItem(
                    BASIC_SLIDE_INVERTED.get(),
                    new Item.Properties()
            ));

    // ========================================
    // FUTURE REGISTRATIONS
    // ========================================

    /*
     * TODO: Add registrations for other switch variants:
     * - SWITCHES_ROCKER & SWITCHES_ROCKER_ITEM
     * - SWITCHES_BUTTON & SWITCHES_BUTTON_ITEM
     * - SWITCHES_SLIDE & SWITCHES_SLIDE_ITEM
     * - Inverted variants (these won't have items since they can't be crafted/placed directly)
     */
}