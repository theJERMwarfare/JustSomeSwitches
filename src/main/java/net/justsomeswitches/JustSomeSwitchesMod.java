package net.justsomeswitches;

import net.justsomeswitches.client.SwitchesConfigScreen;
import net.justsomeswitches.config.SwitchesClientConfig;
import net.justsomeswitches.config.SwitchesCommonConfig;
import net.justsomeswitches.config.SwitchesServerConfig;
import net.justsomeswitches.gui.JustSomeSwitchesMenuTypes;
import net.justsomeswitches.init.JustSomeSwitchesModBlocks;
import net.justsomeswitches.init.JustSomeSwitchesModBlockEntities;
import net.justsomeswitches.init.JustSomeSwitchesModTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.ConfigScreenHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/** Main mod class for Just Some Switches. Adds customizable switch variants that behave like vanilla levers. */
@Mod(JustSomeSwitchesMod.MODID)
public class JustSomeSwitchesMod {

    public static final String MODID = "justsomeswitches";
    public static final Logger LOGGER = LoggerFactory.getLogger(JustSomeSwitchesMod.class);

    /** Initializes mod by registering all components to the mod event bus. */
    @SuppressWarnings("DataFlowIssue")
    public JustSomeSwitchesMod(@Nonnull ModContainer modContainer) {
        @Nonnull IEventBus modEventBus = modContainer.getEventBus();

        JustSomeSwitchesModBlocks.BLOCKS.register(modEventBus);
        JustSomeSwitchesModBlocks.ITEMS.register(modEventBus);
        JustSomeSwitchesModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        JustSomeSwitchesModTabs.CREATIVE_MODE_TABS.register(modEventBus);
        JustSomeSwitchesMenuTypes.MENU_TYPES.register(modEventBus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SwitchesClientConfig.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SwitchesServerConfig.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SwitchesCommonConfig.SPEC);
        ModLoadingContext.get().registerExtensionPoint(
            ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory(
                (minecraft, screen) -> new SwitchesConfigScreen(screen)
            )
        );

        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
    }
}
