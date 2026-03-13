package net.justsomeswitches.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Server-side configuration affecting gameplay mechanics, synchronized to clients. */
public class SwitchesServerConfig {

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    /** Controls whether blocks with BlockEntities can be used for texture customization (default: false). */
    public static final ForgeConfigSpec.BooleanValue ALLOW_BLOCK_ENTITIES;
    /** Controls whether the Switches Wrench can instantly break mod blocks (default: false = wrench CAN break). */
    public static final ForgeConfigSpec.BooleanValue DISABLE_WRENCH_INSTANT_BREAK;

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
        BUILDER.push("Wrench Settings");
        BUILDER.comment("Settings controlling Switches Wrench behavior");
        DISABLE_WRENCH_INSTANT_BREAK = BUILDER
                .comment(
                    "Disable the Switches Wrench instant block breaking feature.",
                    "",
                    "When enabled, the wrench will no longer instantly break mod blocks",
                    "on left-click. Useful for multiplayer servers to prevent griefing.",
                    "",
                    "Default: false (wrench instant break is allowed)"
                )
                .define("disableWrenchInstantBreak", false);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
