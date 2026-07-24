package net.justsomeswitches.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server-side configuration affecting gameplay mechanics, synchronized to clients. */
public class SwitchesServerConfig {

    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    /** Controls whether blocks with BlockEntities can be used for texture customization (default: false). */
    public static final ModConfigSpec.BooleanValue ALLOW_BLOCK_ENTITIES;
    /** Controls whether the Switch Texture Brush can instantly break mod blocks (default: false = brush CAN break). */
    public static final ModConfigSpec.BooleanValue DISABLE_BRUSH_INSTANT_BREAK;

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
        BUILDER.push("Brush Settings");
        BUILDER.comment("Settings controlling Switch Texture Brush behavior");
        DISABLE_BRUSH_INSTANT_BREAK = BUILDER
                .comment(
                    "Disable the Switch Texture Brush instant block breaking feature.",
                    "",
                    "When enabled, the brush will no longer instantly break mod blocks",
                    "on left-click. Useful for multiplayer servers to prevent griefing.",
                    "",
                    "Default: false (brush instant break is allowed)"
                )
                .define("disableBrushInstantBreak", false);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
