package net.justsomeswitches;

import net.justsomeswitches.init.JustSomeSwitchesModBlocks;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(JustSomeSwitchesMod.MODID)
public class JustSomeSwitchesMod {
    public static final String MODID = "justsomeswitches";

    public static final net.neoforged.fml.javafmlmod.EventBus EVENT_BUS =
            FMLJavaModLoadingContext.get().getModEventBus();

    public JustSomeSwitchesMod() {
        JustSomeSwitchesModBlocks.registerAll();
    }
}
