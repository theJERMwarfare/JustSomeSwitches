package net.justsomeswitches.init;

import net.justsomeswitches.JustSomeSwitchesMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Sets up the custom Creative Mode tab for the JustSomeSwitches mod.
 */
public class JustSomeSwitchesModTabs {

    // Register the creative tab under the mod ID
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, JustSomeSwitchesMod.MODID);

    // Create the tab with a title, icon, and list of items/blocks
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> JUST_SOME_SWITCHES_TAB =
            CREATIVE_MODE_TABS.register("just_some_switches_tab", () ->
                    CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.just_some_switches_tab"))
                            .icon(() -> new ItemStack(Blocks.LEVER)) // Replace with your block later
                            .displayItems((parameters, output) -> {
                                // Add your custom blocks/items to the creative tab
                                output.accept(JustSomeSwitchesModBlocks.SWITCHES_LEVER.get().asItem());
                            })
                            .build());
}