package net.justsomeswitches.init;

import net.justsomeswitches.JustSomeSwitchesMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Registration class for all creative tabs in Just Some Switches mod
 *
 * This class handles the deferred registration of custom creative mode tabs.
 * The main tab will contain all switches variants and related items.
 */
public class JustSomeSwitchesModTabs {

    // Deferred register for creative tabs - handles tab registration with NeoForge
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, JustSomeSwitchesMod.MODID);

    // ========================================
    // CREATIVE TAB REGISTRATIONS
    // ========================================

    /**
     * Just Some Switches Creative Tab - main tab containing all mod content
     * Icon: Switches Lever item
     * Contains: All switch variants and the texture wrench
     *
     * Note: This field appears "unused" but is automatically registered via deferred registration
     * when CREATIVE_MODE_TABS is registered to the mod event bus in JustSomeSwitchesMod.
     */
    @SuppressWarnings("unused")
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> JUST_SOME_SWITCHES_TAB =
            CREATIVE_MODE_TABS.register("just_some_switches_tab", () -> CreativeModeTab.builder()
                    // Set the tab icon to the switches lever item
                    .icon(() -> new ItemStack(JustSomeSwitchesModBlocks.SWITCHES_LEVER_ITEM.get()))
                    // Set the display name (will use language file)
                    .title(Component.translatable("itemGroup.justsomeswitches"))
                    // Configure tab contents
                    .displayItems((parameters, output) -> {
                        // Add Switches Lever
                        output.accept(JustSomeSwitchesModBlocks.SWITCHES_LEVER_ITEM.get());

                        // Add Switches Texture Wrench
                        output.accept(JustSomeSwitchesModBlocks.SWITCHES_TEXTURE_WRENCH.get());

                        // Add Basic Block Variants (8 blocks)
                        output.accept(JustSomeSwitchesModBlocks.BASIC_LEVER_ITEM.get());
                        output.accept(JustSomeSwitchesModBlocks.BASIC_LEVER_INVERTED_ITEM.get());
                        output.accept(JustSomeSwitchesModBlocks.BASIC_ROCKER_ITEM.get());
                        output.accept(JustSomeSwitchesModBlocks.BASIC_ROCKER_INVERTED_ITEM.get());
                        output.accept(JustSomeSwitchesModBlocks.BASIC_BUTTONS_ITEM.get());
                        output.accept(JustSomeSwitchesModBlocks.BASIC_BUTTONS_INVERTED_ITEM.get());
                        output.accept(JustSomeSwitchesModBlocks.BASIC_SLIDE_ITEM.get());
                        output.accept(JustSomeSwitchesModBlocks.BASIC_SLIDE_INVERTED_ITEM.get());

                        // TODO: Add future advanced switch variants here:
                        // output.accept(JustSomeSwitchesModBlocks.SWITCHES_ROCKER_ITEM.get());
                        // output.accept(JustSomeSwitchesModBlocks.SWITCHES_BUTTON_ITEM.get());
                        // output.accept(JustSomeSwitchesModBlocks.SWITCHES_SLIDE_ITEM.get());
                    })
                    .build()
            );
}