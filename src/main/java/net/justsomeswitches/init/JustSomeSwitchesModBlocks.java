package net.justsomeswitches.init;

import net.justsomeswitches.JustSomeSwitchesMod;
import net.justsomeswitches.block.SwitchesLeverBlock;
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
 *
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
    // ITEM REGISTRATIONS
    // ========================================

    /**
     * Switches Lever Item - the item form of the switches lever block
     * Uses default BlockItem behavior
     */
    public static final DeferredHolder<Item, BlockItem> SWITCHES_LEVER_ITEM =
            ITEMS.register("switches_lever", () -> new BlockItem(
                    SWITCHES_LEVER.get(),
                    new Item.Properties()
            ));

    /**
     * Switches Texture Wrench - tool used to customize switch textures
     * Opens texture customization GUI when shift-right-clicking on switches
     */
    public static final DeferredHolder<Item, Item> SWITCHES_TEXTURE_WRENCH =
            ITEMS.register("switches_texture_wrench", () -> new Item(
                    new Item.Properties()
                            .stacksTo(1)    // Only allow 1 in a stack (like tools)
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