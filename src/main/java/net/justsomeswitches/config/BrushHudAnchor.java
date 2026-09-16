package net.justsomeswitches.config;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import javax.annotation.Nonnull;

/**
 * Where the brush mode HUD sits on screen. Presets rather than raw coordinates, because a stored
 * pixel position drifts off screen the moment the GUI scale or window size changes.
 */
public enum BrushHudAnchor {
    BOTTOM_LEFT("gui.justsomeswitches.hud_anchor.bottom_left"),
    ABOVE_HOTBAR("gui.justsomeswitches.hud_anchor.above_hotbar"),
    TOP_LEFT("gui.justsomeswitches.hud_anchor.top_left"),
    TOP_RIGHT("gui.justsomeswitches.hud_anchor.top_right"),
    BOTTOM_RIGHT("gui.justsomeswitches.hud_anchor.bottom_right");

    private final String translationKey;

    BrushHudAnchor(@Nonnull String translationKey) {
        this.translationKey = translationKey;
    }

    /** Display name for the config screen button. */
    @Nonnull
    public MutableComponent getDisplayName() {
        return Component.translatable(translationKey);
    }

    /** Next anchor in the cycle, wrapping back to the first. */
    @Nonnull
    public BrushHudAnchor next() {
        BrushHudAnchor[] anchors = values();
        return anchors[(ordinal() + 1) % anchors.length];
    }
}
