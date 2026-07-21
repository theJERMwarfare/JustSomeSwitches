package net.justsomeswitches;

import net.justsomeswitches.init.JustSomeSwitchesModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.registries.MissingMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles automatic migration of legacy item registry IDs from previous mod versions.
 * Currently remaps switches_wrench to switch_texture_brush (v1.20 rename).
 * Must stay in the codebase permanently: it converts unknown item IDs from pre-v1.20
 * saves to their new registry IDs, preserving all NBT such as copied texture settings.
 */
@Mod.EventBusSubscriber(modid = JustSomeSwitchesMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RegistryMigrationHandler {

    @SubscribeEvent
    public static void onMissingMappings(MissingMappingsEvent event) {
        event.getMappings(Registries.ITEM, JustSomeSwitchesMod.MODID).forEach(mapping -> {
            String oldPath = mapping.getKey().getPath();
            if ("switches_wrench".equals(oldPath)) {
                mapping.remap(JustSomeSwitchesModBlocks.SWITCH_TEXTURE_BRUSH.get());
                JustSomeSwitchesMod.LOGGER.info("Migrated legacy item ID: switches_wrench → switch_texture_brush");
            }
        });
    }
}
