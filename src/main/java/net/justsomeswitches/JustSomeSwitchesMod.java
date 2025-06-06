package net.justsomeswitches;

import net.justsomeswitches.init.JustSomeSwitchesModBlocks;
import net.justsomeswitches.init.JustSomeSwitchesModTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
//import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(JustSomeSwitchesMod.MODID)
public class JustSomeSwitchesMod {
    public static final String MODID = "justsomeswitches";

    public JustSomeSwitchesMod(IEventBus modEventBus) {

        JustSomeSwitchesModBlocks.register(modEventBus);
        JustSomeSwitchesModTabs.register(modEventBus);
    }
}