package net.justsomeswitches.init;

import net.justsomeswitches.JustSomeSwitchesMod;
import net.justsomeswitches.blockentity.SwitchesLeverBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Registration class for all block entities in Just Some Switches mod
 * ---
 * This class handles the deferred registration of block entities that provide
 * extended functionality like texture customization and data storage.
 */
public class JustSomeSwitchesModBlockEntities {

    // Deferred register for block entities
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, JustSomeSwitchesMod.MODID);

    // ========================================
    // BLOCK ENTITY REGISTRATIONS
    // ========================================

    /**
     * Switches Lever Block Entity - handles texture customization and NBT storage
     * ---
     * This block entity is associated with the Switches Lever block and provides:
     * - NBT-based texture storage
     * - Client-server synchronization
     * - Item dropping when block is broken
     * - Texture management functionality
     */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SwitchesLeverBlockEntity>> SWITCHES_LEVER =
            BLOCK_ENTITIES.register("switches_lever", () ->
                    BlockEntityType.Builder.of(
                            SwitchesLeverBlockEntity::new,
                            JustSomeSwitchesModBlocks.SWITCHES_LEVER.get()
                    ).build(null)
            );

    // ========================================
    // FUTURE REGISTRATIONS
    // ========================================

    /*
     * TODO: Add block entity registrations for other switch variants:
     *
     * public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SwitchesRockerBlockEntity>> SWITCHES_ROCKER =
     *         BLOCK_ENTITIES.register("switches_rocker", () ->
     *                 BlockEntityType.Builder.of(
     *                         SwitchesRockerBlockEntity::new,
     *                         JustSomeSwitchesModBlocks.SWITCHES_ROCKER.get()
     *                 ).build(null)
     *         );
     *
     * Similar registrations for:
     * - SWITCHES_BUTTON
     * - SWITCHES_SLIDE
     */
}