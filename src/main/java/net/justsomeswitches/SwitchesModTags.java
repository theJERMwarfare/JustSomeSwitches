package net.justsomeswitches;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/** Defines custom tags for block filtering in texture customization slots. */
public class SwitchesModTags {
    public static class Blocks {
        /** Whitelist tag bypassing property validation (data/justsomeswitches/tags/blocks/switches_allowed.json). */
        public static final TagKey<Block> SWITCHES_ALLOWED = create("switches_allowed");
        /** Blacklist tag rejecting blocks regardless of properties (data/justsomeswitches/tags/blocks/switches_blocked.json). */
        public static final TagKey<Block> SWITCHES_BLOCKED = create("switches_blocked");
        private static TagKey<Block> create(String name) {
            return TagKey.create(Registries.BLOCK, new ResourceLocation(JustSomeSwitchesMod.MODID, name));
        }
    }
}
