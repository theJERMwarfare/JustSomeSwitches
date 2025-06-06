package net.justsomeswitches.init;

import net.justsomeswitches.JustSomeSwitchesMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.bus.api.IEventBus;

//@Mod.EventBusSubscriber(modid = JustSomeSwitchesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class JustSomeSwitchesModTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, JustSomeSwitchesMod.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> JUST_SOME_SWITCHES_TAB =
            TABS.register("just_some_switches_tab", () ->
                    CreativeModeTab.builder()
                            .title(net.minecraft.network.chat.Component.literal("Just Some Switches"))
                            .icon(() -> new ItemStack(JustSomeSwitchesModBlocks.SWITCHES_LEVER.get()))
                            .displayItems((params, output) -> {
                                output.accept(JustSomeSwitchesModBlocks.SWITCHES_LEVER.get().asItem());
                            })
                            .build()
            );

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}