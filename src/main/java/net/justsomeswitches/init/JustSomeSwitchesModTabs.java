package net.justsomeswitches.init;

import net.justsomeswitches.JustSomeSwitchesMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** Registers all creative tabs containing mod items. */
public class JustSomeSwitchesModTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, JustSomeSwitchesMod.MODID);

    /** Just Some Switches creative tab - contains all switch variants and tools. */
    @SuppressWarnings("unused")
    public static final RegistryObject<CreativeModeTab> JUST_SOME_SWITCHES_TAB =
            CREATIVE_MODE_TABS.register("just_some_switches_tab", () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(JustSomeSwitchesModBlocks.SWITCHES_LEVER_ITEM.get()))
                    .title(Component.translatable("itemGroup.justsomeswitches"))
                    .displayItems((parameters, output) -> {
                        output.accept(JustSomeSwitchesModBlocks.SWITCHES_LEVER_ITEM.get());
                        output.accept(JustSomeSwitchesModBlocks.SWITCHES_ROCKER_ITEM.get());
                        output.accept(JustSomeSwitchesModBlocks.SWITCHES_SLIDE_ITEM.get());
                        output.accept(JustSomeSwitchesModBlocks.SWITCHES_BUTTONS_ITEM.get());
                        output.accept(JustSomeSwitchesModBlocks.SWITCHES_WRENCH.get());
                        output.accept(JustSomeSwitchesModBlocks.BASIC_LEVER_ITEM.get());
                        output.accept(JustSomeSwitchesModBlocks.BASIC_LEVER_INVERTED_ITEM.get());
                        output.accept(JustSomeSwitchesModBlocks.BASIC_ROCKER_ITEM.get());
                        output.accept(JustSomeSwitchesModBlocks.BASIC_ROCKER_INVERTED_ITEM.get());
                        output.accept(JustSomeSwitchesModBlocks.BASIC_BUTTONS_ITEM.get());
                        output.accept(JustSomeSwitchesModBlocks.BASIC_BUTTONS_INVERTED_ITEM.get());
                        output.accept(JustSomeSwitchesModBlocks.BASIC_SLIDE_ITEM.get());
                        output.accept(JustSomeSwitchesModBlocks.BASIC_SLIDE_INVERTED_ITEM.get());
                    })
                    .build()
            );
}
