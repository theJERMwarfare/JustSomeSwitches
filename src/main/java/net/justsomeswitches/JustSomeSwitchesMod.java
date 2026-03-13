package net.justsomeswitches;

import net.justsomeswitches.client.SwitchesConfigScreen;
import net.justsomeswitches.config.SwitchesClientConfig;
import net.justsomeswitches.config.SwitchesCommonConfig;
import net.justsomeswitches.config.SwitchesServerConfig;
import net.justsomeswitches.gui.JustSomeSwitchesMenuTypes;
import net.justsomeswitches.init.JustSomeSwitchesModBlocks;
import net.justsomeswitches.init.JustSomeSwitchesModBlockEntities;
import net.justsomeswitches.init.JustSomeSwitchesModTabs;
import net.justsomeswitches.network.NetworkHandler;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.client.ConfigScreenHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Main mod class for Just Some Switches. Adds customizable switch variants that behave like vanilla levers. */
@Mod(JustSomeSwitchesMod.MODID)
public class JustSomeSwitchesMod {

    public static final String MODID = "justsomeswitches";
    public static final Logger LOGGER = LoggerFactory.getLogger(JustSomeSwitchesMod.class);

    /** Initializes mod by registering all components to the mod event bus. */
    public JustSomeSwitchesMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        JustSomeSwitchesModBlocks.BLOCKS.register(modEventBus);
        JustSomeSwitchesModBlocks.ITEMS.register(modEventBus);
        JustSomeSwitchesModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        JustSomeSwitchesModTabs.CREATIVE_MODE_TABS.register(modEventBus);
        JustSomeSwitchesMenuTypes.MENU_TYPES.register(modEventBus);
        NetworkHandler.registerPackets();

        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SwitchesClientConfig.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SwitchesServerConfig.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SwitchesCommonConfig.SPEC);
        ModLoadingContext.get().registerExtensionPoint(
            ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory(
                (minecraft, screen) -> new SwitchesConfigScreen(screen)
            )
        );

    }
}
