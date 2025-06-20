package net.justsomeswitches;

import net.justsomeswitches.gui.JustSomeSwitchesMenuTypes;
import net.justsomeswitches.init.JustSomeSwitchesModBlocks;
import net.justsomeswitches.init.JustSomeSwitchesModBlockEntities;
import net.justsomeswitches.init.JustSomeSwitchesModTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/**
 * Main mod class for Just Some Switches - Phase 3A Enhanced
 * ---
 * This mod adds customizable switch variants that behave like vanilla levers
 * but with enhanced functionality and custom textures.
 * ---
 * Phase 3A additions:
 * - Block Entity registration for NBT-based texture storage
 */
@Mod(JustSomeSwitchesMod.MODID)
public class JustSomeSwitchesMod {

    // Mod identifier - must match the mod ID in mods.toml
    public static final String MODID = "justsomeswitches";

    // Logger for debugging and information output
    public static final Logger LOGGER = LoggerFactory.getLogger(JustSomeSwitchesMod.class);

    /**
     * Main mod constructor - called when NeoForge loads the mod
     * Enhanced for Phase 3A with Block Entity support
     *
     * @param modContainer The mod container provided by NeoForge
     */
    public JustSomeSwitchesMod(@Nonnull ModContainer modContainer) {
        // Get the mod event bus for registering mod-specific content
        @Nonnull IEventBus modEventBus = modContainer.getEventBus();

        // Register our blocks and items with the mod event bus
        JustSomeSwitchesModBlocks.BLOCKS.register(modEventBus);
        JustSomeSwitchesModBlocks.ITEMS.register(modEventBus);

        // NEW: Register our block entities with the mod event bus (Phase 3A)
        JustSomeSwitchesModBlockEntities.BLOCK_ENTITIES.register(modEventBus);

        // Register our creative tabs with the mod event bus
        JustSomeSwitchesModTabs.CREATIVE_MODE_TABS.register(modEventBus);

        // Register our menu types with the mod event bus
        JustSomeSwitchesMenuTypes.MENU_TYPES.register(modEventBus);

        // Register event listeners
        modEventBus.addListener(this::commonSetup);

        LOGGER.info("Just Some Switches mod initialized successfully!");
        LOGGER.info("Phase 3A: Block Entity infrastructure ready");
    }

    /**
     * Common setup event - called after registration events are complete
     * Use this for any setup that needs to happen after all mods have registered their content
     *
     * @param event The common setup event
     */
    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Just Some Switches common setup complete!");
        LOGGER.info("Phase 3A: NBT foundation infrastructure initialized");
    }
}