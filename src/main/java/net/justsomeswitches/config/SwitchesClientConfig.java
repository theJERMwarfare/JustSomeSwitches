package net.justsomeswitches.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Client-side configuration affecting local client's visual/UI experience only. */
public class SwitchesClientConfig {

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    /** Controls whether ghost preview of switch blocks is shown during placement (default: true). */
    public static final ForgeConfigSpec.BooleanValue SHOW_SWITCHES_PREVIEW;

    static {
        BUILDER.push("Visual Settings");
        BUILDER.comment("Client-side visual and UI settings");
        SHOW_SWITCHES_PREVIEW = BUILDER
                .comment(
                    "Show ghost preview of switch blocks before placement.",
                    "",
                    "The preview displays a transparent version of the switch block",
                    "at the target position, helping with precise placement.",
                    "",
                    "Default: true"
                )
                .define("showSwitchesPreview", true);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
