package net.justsomeswitches;

import net.justsomeswitches.init.JustSomeSwitchesModBlocks;
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;

@Mod(JustSomeSwitchesMod.MODID)
public class JustSomeSwitchesMod {
    public static final String MODID = "justsomeswitches";

    public JustSomeSwitchesMod(IEventBus modEventBus) {
        JustSomeSwitchesModBlocks.register(modEventBus);
    }
}