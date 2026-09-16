package net.justsomeswitches.client;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.gui.widget.ExtendedButton;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.function.IntConsumer;

/** Scrollable list widget for the mod config screen. */
public class ConfigOptionsList extends ContainerObjectSelectionList<ConfigOptionsList.Entry> {
    public ConfigOptionsList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
        super(minecraft, width, height, y, itemHeight);
        setRenderHeader(false, 0);
    }
    @Override
    public int addEntry(@Nonnull Entry entry) {
        return super.addEntry(entry);
    }
    @Override
    public int getRowWidth() {
        return 200;
    }
    @Override
    protected int getScrollbarPosition() {
        return this.width / 2 + 120;
    }

    /** Base class for all config list entries. */
    public abstract static class Entry extends ContainerObjectSelectionList.Entry<Entry> {
    }

    /** Bold centered section header with underline. */
    public static class HeaderEntry extends Entry {
        private final Component text;
        public HeaderEntry(String text) {
            this.text = Component.literal(text).withStyle(style -> style.withBold(true));
        }
        @Override
        public void render(@Nonnull GuiGraphics graphics, int index, int top, int left, int width,
                           int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
            Minecraft mc = Minecraft.getInstance();
            int centerX = left + width / 2;
            graphics.drawCenteredString(mc.font, text, centerX, top + 2, 0xFFFFFF);
            int headerWidth = mc.font.width(text);
            graphics.fill(centerX - headerWidth / 2, top + 13, centerX + headerWidth / 2, top + 14, 0xFFFFFFFF);
        }
        @Override @Nonnull
        public List<? extends GuiEventListener> children() { return Collections.emptyList(); }
        @Override @Nonnull
        public List<? extends NarratableEntry> narratables() { return Collections.emptyList(); }
    }

    /** Centered toggle button entry. */
    public static class ButtonEntry extends Entry {
        private final ExtendedButton button;
        public ButtonEntry(ExtendedButton button) { this.button = button; }
        @Override
        public void render(@Nonnull GuiGraphics graphics, int index, int top, int left, int width,
                           int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
            button.setX(left + width / 2 - 100);
            button.setY(top);
            button.render(graphics, mouseX, mouseY, partialTick);
        }
        @Override @Nonnull
        public List<? extends GuiEventListener> children() { return ImmutableList.of(button); }
        @Override @Nonnull
        public List<? extends NarratableEntry> narratables() { return ImmutableList.of(button); }
    }

    /** Centered integer slider with a live label. */
    public static class SliderEntry extends Entry {
        private final IntSlider slider;
        public SliderEntry(String labelKey, int min, int max, int initial, IntConsumer onChange) {
            this.slider = new IntSlider(labelKey, min, max, initial, onChange);
        }
        @Override
        public void render(@Nonnull GuiGraphics graphics, int index, int top, int left, int width,
                           int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
            slider.setX(left + width / 2 - 100);
            slider.setY(top);
            slider.render(graphics, mouseX, mouseY, partialTick);
        }
        @Override @Nonnull
        public List<? extends GuiEventListener> children() { return ImmutableList.of(slider); }
        @Override @Nonnull
        public List<? extends NarratableEntry> narratables() { return ImmutableList.of(slider); }

        /** Maps the slider's 0..1 position onto an inclusive integer range. */
        private static final class IntSlider extends AbstractSliderButton {
            private final String labelKey;
            private final int min;
            private final int max;
            private final IntConsumer onChange;
            IntSlider(String labelKey, int min, int max, int initial, IntConsumer onChange) {
                super(0, 0, 200, 20, Component.empty(), (double)(initial - min) / (double)(max - min));
                this.labelKey = labelKey;
                this.min = min;
                this.max = max;
                this.onChange = onChange;
                updateMessage();
            }
            private int intValue() { return min + (int)Math.round(value * (max - min)); }
            @Override
            protected void updateMessage() { setMessage(Component.translatable(labelKey, intValue())); }
            @Override
            protected void applyValue() { onChange.accept(intValue()); }
        }
    }

    /** Centered help/warning text with dynamic scaling. */
    public static class TextEntry extends Entry {
        private final Component text;
        private final int color;
        public TextEntry(String text, int color) { this(Component.literal(text), color); }
        public TextEntry(Component text, int color) { this.text = text; this.color = color; }
        @Override
        public void render(@Nonnull GuiGraphics graphics, int index, int top, int left, int width,
                           int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
            Minecraft mc = Minecraft.getInstance();
            int centerX = left + width / 2;
            float scale = Math.min(0.9f, (float)(width + 30) / mc.font.width(text));
            graphics.pose().pushPose();
            graphics.pose().scale(scale, scale, 1.0f);
            int scaledX = (int)(centerX / scale);
            int scaledY = (int)((top + 4) / scale);
            graphics.drawCenteredString(mc.font, text, scaledX, scaledY, color);
            graphics.pose().popPose();
        }
        @Override @Nonnull
        public List<? extends GuiEventListener> children() { return Collections.emptyList(); }
        @Override @Nonnull
        public List<? extends NarratableEntry> narratables() { return Collections.emptyList(); }
    }
}
