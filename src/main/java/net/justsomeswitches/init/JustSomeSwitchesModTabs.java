package net.justsomeswitches.init;

import net.justsomeswitches.JustSomeSwitchesMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class JustSomeSwitchesModTabs {

    // Create a deferred register for creative mode tabs, linked to your mod ID
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, JustSomeSwitchesMod.MODID);

    // Register a new creative tab and define its contents and icon
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> JUST_SOME_SWITCHES_TAB =
            CREATIVE_MODE_TABS.register("just_some_switches_tab", () ->
                    CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.just_some_switches_tab"))
                            .icon(() -> new ItemStack(Blocks.LEVER)) // TEMP icon, replace if needed
                            .displayItems((parameters, output) -> {
                                output.accept(JustSomeSwitchesModBlocks.SWITCHES_LEVER.get().asItem());
                            })
                            .build());
}