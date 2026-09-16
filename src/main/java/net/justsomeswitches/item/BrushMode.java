package net.justsomeswitches.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import javax.annotation.Nonnull;

/**
 * What the Switch Texture Brush does on shift + right-click. Stored on the ItemStack, so the
 * server already knows it by the time the interaction arrives. An unset mode reads as CUSTOMIZE,
 * which keeps every brush made before this feature working unchanged.
 */
public enum BrushMode {
    CUSTOMIZE("gui.justsomeswitches.brush_mode.customize", ChatFormatting.WHITE,
        "tooltip.justsomeswitches.brush.action.customize"),
    COPY("gui.justsomeswitches.brush_mode.copy", ChatFormatting.GREEN,
        "tooltip.justsomeswitches.brush.action.copy"),
    PASTE("gui.justsomeswitches.brush_mode.paste", ChatFormatting.AQUA,
        "tooltip.justsomeswitches.brush.action.paste");

    private final String translationKey;
    private final ChatFormatting color;
    private final String actionKey;

    BrushMode(@Nonnull String translationKey, @Nonnull ChatFormatting color, @Nonnull String actionKey) {
        this.translationKey = translationKey;
        this.color = color;
        this.actionKey = actionKey;
    }

    /** Display name for the action bar, HUD and tooltip. */
    @Nonnull
    public MutableComponent getDisplayName() {
        return Component.translatable(translationKey);
    }

    /** Colour the mode name is shown in, so the HUD and tooltip read at a glance. */
    @Nonnull
    public ChatFormatting getColor() {
        return color;
    }

    /** Tooltip line describing what sneak plus right-click does in this mode. Takes the sneak key. */
    @Nonnull
    public MutableComponent getActionDescription(@Nonnull Component sneakKey) {
        return Component.translatable(actionKey, sneakKey);
    }

    /** Next mode in the cycle, wrapping back to the first. */
    @Nonnull
    public BrushMode next() {
        BrushMode[] modes = values();
        return modes[(ordinal() + 1) % modes.length];
    }

    /** Previous mode in the cycle, wrapping to the last. */
    @Nonnull
    public BrushMode previous() {
        BrushMode[] modes = values();
        return modes[(ordinal() + modes.length - 1) % modes.length];
    }

    /** Parses a stored name, falling back to CUSTOMIZE for anything unrecognised. */
    @Nonnull
    public static BrushMode fromName(@Nonnull String name) {
        for (BrushMode mode : values()) {
            if (mode.name().equals(name)) {
                return mode;
            }
        }
        return CUSTOMIZE;
    }
}
