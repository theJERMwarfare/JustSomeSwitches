package net.justsomeswitches.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client-side configuration affecting local client's visual/UI experience only. */
public class SwitchesClientConfig {

    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    /** Controls whether ghost preview of switch blocks is shown during placement (default: true). */
    public static final ModConfigSpec.BooleanValue SHOW_SWITCHES_PREVIEW;
    /** Controls whether the brush mode indicator is drawn while the brush is held (default: true). */
    public static final ModConfigSpec.BooleanValue SHOW_BRUSH_MODE_HUD;
    /** Screen corner the brush mode indicator is anchored to (default: BOTTOM_LEFT). */
    public static final ModConfigSpec.EnumValue<BrushHudAnchor> BRUSH_MODE_HUD_ANCHOR;
    /** Text size of the brush mode indicator (default: SMALL). */
    public static final ModConfigSpec.EnumValue<BrushHudSize> BRUSH_MODE_HUD_SIZE;
    /** When the action bar confirms a brush mode change (default: SMART). */
    public static final ModConfigSpec.EnumValue<BrushModeMessage> BRUSH_MODE_MESSAGE;
    /** Horizontal nudge applied to the brush mode indicator, in pixels (default: 0). */
    public static final ModConfigSpec.IntValue BRUSH_MODE_HUD_OFFSET_X;
    /** Vertical nudge applied to the brush mode indicator, in pixels (default: 0). */
    public static final ModConfigSpec.IntValue BRUSH_MODE_HUD_OFFSET_Y;

    /** Nudge limit in either direction, large enough to clear another mod's overlay. */
    public static final int HUD_OFFSET_LIMIT = 100;

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
        BUILDER.push("HUD Settings");
        BUILDER.comment("Brush mode indicator drawn while the Switch Texture Brush is held");
        SHOW_BRUSH_MODE_HUD = BUILDER
                .comment(
                    "Show the brush's current mode on screen.",
                    "",
                    "Only drawn while the brush is in your main hand, and hidden",
                    "along with the rest of the HUD when you press F1.",
                    "",
                    "Default: true"
                )
                .define("showBrushModeHud", true);
        BRUSH_MODE_HUD_ANCHOR = BUILDER
                .comment(
                    "Where the brush mode indicator is anchored on screen.",
                    "",
                    "Presets are used rather than fixed coordinates so the indicator",
                    "stays put when the GUI scale or window size changes.",
                    "",
                    "Default: BOTTOM_LEFT"
                )
                .defineEnum("brushModeHudAnchor", BrushHudAnchor.BOTTOM_LEFT);
        BRUSH_MODE_HUD_SIZE = BUILDER
                .comment(
                    "Text size of the brush mode indicator.",
                    "",
                    "The size is snapped to whole screen pixels so the font stays crisp,",
                    "so at a low GUI scale the options may look the same.",
                    "",
                    "Default: SMALL"
                )
                .defineEnum("brushModeHudSize", BrushHudSize.SMALL);
        BRUSH_MODE_MESSAGE = BUILDER
                .comment(
                    "When the action bar confirms a brush mode change.",
                    "",
                    "SMART hides it only while the indicator is shown above the hotbar,",
                    "where the two would sit on top of each other. Other action bar",
                    "messages are not affected.",
                    "",
                    "Default: SMART"
                )
                .defineEnum("brushModeMessage", BrushModeMessage.SMART);
        BRUSH_MODE_HUD_OFFSET_X = BUILDER
                .comment(
                    "Horizontal nudge from the anchor, in pixels. Positive moves right.",
                    "",
                    "Use this to move the indicator clear of another mod's overlay.",
                    "",
                    "Default: 0"
                )
                .defineInRange("brushModeHudOffsetX", 0, -HUD_OFFSET_LIMIT, HUD_OFFSET_LIMIT);
        BRUSH_MODE_HUD_OFFSET_Y = BUILDER
                .comment(
                    "Vertical nudge from the anchor, in pixels. Positive moves down.",
                    "",
                    "Use this to move the indicator clear of another mod's overlay.",
                    "",
                    "Default: 0"
                )
                .defineInRange("brushModeHudOffsetY", 0, -HUD_OFFSET_LIMIT, HUD_OFFSET_LIMIT);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
