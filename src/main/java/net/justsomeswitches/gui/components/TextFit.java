package net.justsomeswitches.gui.components;

/**
 * Shrinks a fixed text scale just enough to keep a label inside its box.
 * <p>
 * Call sites keep their own positioning arithmetic and only swap their hardcoded scale constant for
 * a call to {@link #scale}. English already fits everywhere, so this returns the original constant
 * and the layout is unchanged; only a longer translation takes the shrinking path.
 */
public final class TextFit {
    /** Below roughly half size the bitmap font stops being legible, so overflow is the better trade. */
    private static final float MIN_SCALE = 0.5f;

    private TextFit() {
    }

    /** Returns baseScale, or less when textWidth would not fit availableWidth at that scale. */
    public static float scale(float baseScale, float availableWidth, int textWidth) {
        if (textWidth <= 0 || availableWidth <= 0) {
            return baseScale;
        }
        return Math.max(MIN_SCALE, Math.min(baseScale, availableWidth / textWidth));
    }
}
