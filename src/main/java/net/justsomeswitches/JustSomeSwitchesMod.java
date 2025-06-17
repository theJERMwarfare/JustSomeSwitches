package net.justsomeswitches;

import net.justsomeswitches.init.JustSomeSwitchesModBlocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main mod class for Just Some Switches
 *
 * This mod adds customizable switch variants that behave like vanilla levers
 * but with enhanced functionality and custom textures.
 */
@Mod(JustSomeSwitchesMod.MODID)
public class JustSomeSwitchesMod {

    // Mod identifier - must match the mod ID in mods.toml
    public static final String MODID = "justsomeswitches";

    // Logger for debugging and information output
    public static final Logger LOGGER = LoggerFactory.getLogger(JustSomeSwitchesMod.class);

    /**
     * Main mod constructor - called when NeoForge loads the mod
     *
     * @param modContainer The mod container provided by NeoForge
     */
    public JustSomeSwitchesMod(ModContainer modContainer) {
        // Get the mod event bus for registering mod-specific content
        IEventBus modEventBus = modContainer.getEventBus();

        // Register our blocks and items with the mod event bus
        JustSomeSwitchesModBlocks.BLOCKS.register(modEventBus);
        JustSomeSwitchesModBlocks.ITEMS.register(modEventBus);

        // Register event listeners
        modEventBus.addListener(this::commonSetup);

        LOGGER.info("Just Some Switches mod initialized successfully!");
    }

    /**
     * Common setup event - called after registration events are complete
     * Use this for any setup that needs to happen after all mods have registered their content
     *
     * @param event The common setup event
     */
    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Just Some Switches common setup complete!");
    }
}