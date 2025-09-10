package net.justsomeswitches;

import net.justsomeswitches.gui.JustSomeSwitchesMenuTypes;
import net.justsomeswitches.init.JustSomeSwitchesModBlocks;
import net.justsomeswitches.init.JustSomeSwitchesModBlockEntities;
import net.justsomeswitches.init.JustSomeSwitchesModTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import javax.annotation.Nonnull;

/**
 * Main mod class for Just Some Switches
 *
 * Adds customizable switch variants that behave like vanilla levers
 * with enhanced functionality and custom textures.
 */
@Mod(JustSomeSwitchesMod.MODID)
public class JustSomeSwitchesMod {

    public static final String MODID = "justsomeswitches";
    /**
     * Main mod constructor
     *
     * @param modContainer The mod container provided by NeoForge
     */
    @SuppressWarnings("DataFlowIssue")
    public JustSomeSwitchesMod(@Nonnull ModContainer modContainer) {
        @Nonnull IEventBus modEventBus = modContainer.getEventBus();

        JustSomeSwitchesModBlocks.BLOCKS.register(modEventBus);
        JustSomeSwitchesModBlocks.ITEMS.register(modEventBus);
        JustSomeSwitchesModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        JustSomeSwitchesModTabs.CREATIVE_MODE_TABS.register(modEventBus);
        JustSomeSwitchesMenuTypes.MENU_TYPES.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
    }

    /**
     * Common setup event
     */
    private void commonSetup(final FMLCommonSetupEvent event) {
        // Currently unused
    }
}
