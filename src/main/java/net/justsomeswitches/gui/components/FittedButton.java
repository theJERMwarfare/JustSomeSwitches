package net.justsomeswitches.gui.components;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;

/**
 * Button that shrinks its label to fit instead of scrolling it. Vanilla draws an oversized
 * label with renderScrollingString, which slides the text back and forth. Scaling keeps
 * longer labels readable, which matters once translations are added.
 */
public class FittedButton extends Button {
    private static final int PADDING = 4;
    private static final float MIN_SCALE = 0.5f;

    public FittedButton(int x, int y, int width, int height, @Nonnull Component message, @Nonnull OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    @Override
    public void renderString(@Nonnull GuiGraphics graphics, @Nonnull Font font, int color) {
        Component message = getMessage();
        int textWidth = font.width(message);
        int available = getWidth() - (PADDING * 2);
        if (textWidth <= available || available <= 0) {
            super.renderString(graphics, font, color);
            return;
        }
        float scale = Math.max(MIN_SCALE, (float) available / textWidth);
        float scaledWidth = textWidth * scale;
        float scaledHeight = font.lineHeight * scale;
        int x = Math.round((getX() + ((getWidth() - scaledWidth) / 2.0f)) / scale);
        int y = Math.round((getY() + ((getHeight() - scaledHeight) / 2.0f)) / scale);
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.drawString(font, message, x, y, color, false);
        graphics.pose().popPose();
    }
}
