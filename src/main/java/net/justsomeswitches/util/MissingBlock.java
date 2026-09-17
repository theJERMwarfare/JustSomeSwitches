package net.justsomeswitches.util;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * One unsatisfied paste requirement: which item is missing, and which category needed it.
 * Carries IDs rather than a finished sentence, so the CLIENT composes the line and resolves the
 * block name in its own language. Building the sentence server-side left it English on every client.
 */
public record MissingBlock(@Nonnull ResourceLocation itemId, @Nonnull Category category) {

    /** Which texture slot needed the block. */
    public enum Category {
        TOGGLE("gui.justsomeswitches.category.toggle"),
        BASE("gui.justsomeswitches.category.base");

        private final String translationKey;

        Category(@Nonnull String translationKey) {
            this.translationKey = translationKey;
        }

        @Nonnull
        public MutableComponent getDisplayName() {
            return Component.translatable(translationKey);
        }
    }

    /**
     * How the block name is picked out from the rest of the line.
     * ⚠ Underline rather than a colour ON PURPOSE. BrushMissingBlockScreen fakes its shadow by drawing
     * each line twice, and Font uses a segment's OWN colour in place of the colour passed in, so a
     * coloured name would render at full strength in the shadow pass as a 1px smear. Underline is not a
     * colour, so both passes keep theirs. Colouring this needs the screen moved to vanilla's drop shadow
     * first. The dialog is also light grey, where every bright colour reads worse than the plain white.
     */
    private static final ChatFormatting NAME_STYLE = ChatFormatting.UNDERLINE;

    /** The display line. Built where it is shown, so both names resolve in the viewer's language. */
    @Nonnull
    public Component toDisplay() {
        return Component.translatable("gui.justsomeswitches.missing_block.entry",
                BuiltInRegistries.ITEM.get(itemId).getDescription().copy().withStyle(NAME_STYLE),
                category.getDisplayName());
    }

    /** Writes one entry. Must stay symmetric with read. */
    public void write(@Nonnull FriendlyByteBuf buf) {
        buf.writeResourceLocation(itemId);
        buf.writeEnum(category);
    }

    /**
     * Reads one entry, or null if the id is malformed. The string is length-capped rather than using
     * readResourceLocation, which would accept the 32767 default. Both fields are always consumed so
     * the buffer stays in step even when the entry is discarded.
     */
    @Nullable
    public static MissingBlock read(@Nonnull FriendlyByteBuf buf) {
        ResourceLocation itemId = ResourceLocation.tryParse(buf.readUtf(SecurityUtils.getMaxStringLength()));
        Category category = buf.readEnum(Category.class);
        return itemId == null ? null : new MissingBlock(itemId, category);
    }
}
