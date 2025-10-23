package net.justsomeswitches.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client-side configuration affecting local client's visual/UI experience only. */
public class SwitchesClientConfig {
    
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;
    
    /** Controls whether ghost preview of switch blocks is shown during placement (default: true). */
    public static final ModConfigSpec.BooleanValue SHOW_SWITCHES_PREVIEW;
    
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
