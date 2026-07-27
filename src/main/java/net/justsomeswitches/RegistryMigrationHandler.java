package net.justsomeswitches;

import net.justsomeswitches.init.JustSomeSwitchesModBlocks;
import net.minecraft.resources.ResourceLocation;

/**
 * Handles automatic migration of legacy item registry IDs from previous mod versions.
 * Currently remaps switches_wrench to switch_texture_brush (v1.20 rename).
 * Must stay in the codebase permanently: it converts unknown item IDs from pre-v1.20
 * saves to their new registry IDs, preserving all NBT such as copied texture settings.
 * <p>
 * NeoForge equivalent of Forge's {@code MissingMappingsEvent}, which does not exist here -
 * NeoForge resolves legacy IDs through {@code DeferredRegister.addAlias(old, new)} instead.
 * Must be called during mod construction, before the registers are attached to the event bus.
 */
public final class RegistryMigrationHandler {

    private RegistryMigrationHandler() {
        throw new AssertionError("RegistryMigrationHandler is a utility class");
    }

    /** Registers legacy item-ID aliases. Call once during mod construction. */
    public static void registerAliases() {
        JustSomeSwitchesModBlocks.ITEMS.addAlias(
                ResourceLocation.fromNamespaceAndPath(JustSomeSwitchesMod.MODID, "switches_wrench"),
                ResourceLocation.fromNamespaceAndPath(JustSomeSwitchesMod.MODID, "switch_texture_brush")
        );
        JustSomeSwitchesMod.LOGGER.info("Registered legacy item ID alias: switches_wrench -> switch_texture_brush");
    }
}
