package net.justsomeswitches.config;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import javax.annotation.Nonnull;

/** When the action bar should confirm a brush mode change. */
public enum BrushModeMessage {
    /** Hidden only when the HUD is shown above the hotbar, where the two would overlap. */
    SMART("gui.justsomeswitches.mode_message.smart"),
    ON("gui.justsomeswitches.mode_message.on"),
    OFF("gui.justsomeswitches.mode_message.off");

    private final String translationKey;

    BrushModeMessage(@Nonnull String translationKey) {
        this.translationKey = translationKey;
    }

    /** Display name for the config screen button. */
    @Nonnull
    public MutableComponent getDisplayName() {
        return Component.translatable(translationKey);
    }

    /** Next option in the cycle, wrapping back to the first. */
    @Nonnull
    public BrushModeMessage next() {
        BrushModeMessage[] options = values();
        return options[(ordinal() + 1) % options.length];
    }
}
