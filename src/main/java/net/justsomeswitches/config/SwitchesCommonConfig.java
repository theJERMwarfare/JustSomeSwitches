package net.justsomeswitches.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Common configuration shared between client and server for gameplay mechanics. */
public class SwitchesCommonConfig {

    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    /** Use tight-fitting hitboxes for Basic switch blocks (default: false). */
    public static final ModConfigSpec.BooleanValue TIGHT_HITBOXES_BASIC;

    /** Use tight-fitting hitboxes for Switches blocks (default: true). */
    public static final ModConfigSpec.BooleanValue TIGHT_HITBOXES_SWITCHES;

    static {
        BUILDER.push("Hitbox Settings");
        BUILDER.comment("Settings controlling block hitbox shapes");
        TIGHT_HITBOXES_BASIC = BUILDER
                .comment(
                    "Use tight-fitting hitboxes for Basic switch blocks.",
                    "",
                    "When enabled, Basic switch hitboxes closely follow the model shape.",
                    "When disabled, Basic switches use simple rectangular hitboxes.",
                    "",
                    "Default: false"
                )
                .define("tightHitboxesBasic", false);
        TIGHT_HITBOXES_SWITCHES = BUILDER
                .comment(
                    "Use tight-fitting hitboxes for Switches blocks.",
                    "",
                    "When enabled, Switches block hitboxes closely follow the model shape.",
                    "When disabled, Switches blocks use simple rectangular hitboxes.",
                    "",
                    "Default: true"
                )
                .define("tightHitboxesSwitches", true);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    /** Safe accessor that returns false if config hasn't loaded yet (e.g. during registry freeze). */
    public static boolean isTightHitboxesBasic() {
        return SPEC.isLoaded() && TIGHT_HITBOXES_BASIC.get();
    }

    /** Safe accessor that returns false if config hasn't loaded yet (e.g. during registry freeze). */
    public static boolean isTightHitboxesSwitches() {
        return SPEC.isLoaded() && TIGHT_HITBOXES_SWITCHES.get();
    }
}
