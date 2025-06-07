package net.justsomeswitches;

import net.justsomeswitches.init.JustSomeSwitchesModBlocks;
import net.justsomeswitches.init.JustSomeSwitchesModTabs;
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;

@Mod(JustSomeSwitchesMod.MODID)
public class JustSomeSwitchesMod {
    public static final String MODID = "justsomeswitches";

    public JustSomeSwitchesMod(IEventBus modEventBus) {
        JustSomeSwitchesModBlocks.BLOCKS.register(modEventBus);
        JustSomeSwitchesModTabs.CREATIVE_MODE_TABS.register(modEventBus);
    }
}