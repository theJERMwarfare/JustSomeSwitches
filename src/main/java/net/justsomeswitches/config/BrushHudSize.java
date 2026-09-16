package net.justsomeswitches.config;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import javax.annotation.Nonnull;

/**
 * Size of the brush mode HUD text. The target is a rough multiplier only: the layer snaps it to a
 * whole number of real pixels per font pixel, because Minecraft's font is a bitmap and a fractional
 * size renders with uneven letter widths.
 */
public enum BrushHudSize {
    SMALL("gui.justsomeswitches.hud_size.small", 0.75F),
    NORMAL("gui.justsomeswitches.hud_size.normal", 1.0F),
    LARGE("gui.justsomeswitches.hud_size.large", 1.25F);

    private final String translationKey;
    private final float target;

    BrushHudSize(@Nonnull String translationKey, float target) {
        this.translationKey = translationKey;
        this.target = target;
    }

    /** Requested multiplier before it is snapped to whole pixels. */
    public float getTarget() {
        return target;
    }

    /** Display name for the config screen button. */
    @Nonnull
    public MutableComponent getDisplayName() {
        return Component.translatable(translationKey);
    }

    /** Next size in the cycle, wrapping back to the first. */
    @Nonnull
    public BrushHudSize next() {
        BrushHudSize[] sizes = values();
        return sizes[(ordinal() + 1) % sizes.length];
    }
}
