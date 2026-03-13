package net.justsomeswitches.init;

import net.justsomeswitches.JustSomeSwitchesMod;
import net.justsomeswitches.blockentity.SwitchBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** Registers all block entities for texture customization and NBT storage. */
public class JustSomeSwitchesModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, JustSomeSwitchesMod.MODID);

    /** Shared block entity for all advanced switch variants (lever, rocker, etc.). */
    @SuppressWarnings("DataFlowIssue") // Null data fixer is standard for mod block entities
    public static final RegistryObject<BlockEntityType<SwitchBlockEntity>> SWITCHES_LEVER =
            BLOCK_ENTITIES.register("switches_lever", () ->
                    BlockEntityType.Builder.of(
                            SwitchBlockEntity::new,
                            JustSomeSwitchesModBlocks.SWITCHES_LEVER.get(),
                            JustSomeSwitchesModBlocks.SWITCHES_ROCKER.get(),
                            JustSomeSwitchesModBlocks.SWITCHES_SLIDE.get(),
                            JustSomeSwitchesModBlocks.SWITCHES_BUTTONS.get()
                    ).build(null)
            );
}
