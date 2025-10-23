package net.justsomeswitches.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server-side configuration affecting gameplay mechanics, synchronized to clients. */
public class SwitchesServerConfig {
    
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;
    
    /** Controls whether blocks with BlockEntities can be used for texture customization (default: false). */
    public static final ModConfigSpec.BooleanValue ALLOW_BLOCK_ENTITIES;
    
    static {
        BUILDER.push("Block Validation");
        BUILDER.comment("Settings controlling which blocks can be used for switch texture customization");
        
        ALLOW_BLOCK_ENTITIES = BUILDER
                .comment(
                    "Allow blocks with BlockEntities (tile entities) as switch textures.",
                    "",
                    "WARNING: May cause crashes with certain modded blocks!",
                    "BlockEntities often have world-dependent logic that may not work correctly",
                    "when extracted for texture use only.",
                    "",
                    "Recommendation: Keep disabled unless you need specific blocks.",
                    "",
                    "Default: false"
                )
                .define("allowBlockEntities", false);
        
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
