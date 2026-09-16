package net.justsomeswitches.client;

import net.justsomeswitches.config.BrushHudAnchor;
import net.justsomeswitches.config.BrushHudSize;
import net.justsomeswitches.config.SwitchesClientConfig;
import net.justsomeswitches.item.BrushMode;
import net.justsomeswitches.item.SwitchTextureBrushItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

/**
 * Draws the brush's current mode while it is held in the main hand. Registered next to the hotbar
 * layer, which leaves it outside vanilla's hide-GUI wrapper, so F1 has to be checked here.
 */
public final class BrushModeHudLayer {
    /** Gap from the screen edge for the corner anchors. */
    private static final int MARGIN = 4;
    /**
     * Above-hotbar sits one line clear of vanilla's action bar message, which is the topmost thing
     * in the bottom stack. Every row below it is claimed: hotbar 22..1, experience 35..25, health
     * and hunger 39..31, armour 49..41 when worn, held item name 59..51. Nothing there is free in
     * every situation, so the HUD goes above the lot. These mirror Gui.renderOverlayMessage.
     */
    private static final int ACTION_BAR_ROW_SHIFT = 9;
    private static final int ACTION_BAR_MIN_ROW = 68;
    private static final int ACTION_BAR_TEXT_OFFSET = 4;
    /** Clearance between the HUD and the action bar row it sits above. */
    private static final int ACTION_BAR_CLEARANCE = 2;

    private BrushModeHudLayer() {
    }

    /** Draws the indicator, once per frame. Adapted to LayeredDraw.Layer where it is registered. */
    public static void render(@Nonnull GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || !SwitchesClientConfig.SHOW_BRUSH_MODE_HUD.get()) {
            return;
        }
        LocalPlayer player = mc.player;
        if (player == null || player.isSpectator()) {
            return;
        }
        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!(held.getItem() instanceof SwitchTextureBrushItem)) {
            return;
        }
        BrushMode mode = SwitchTextureBrushItem.getMode(held);
        Component text = Component.translatable("gui.justsomeswitches.brush_hud",
            mode.getDisplayName().withStyle(mode.getColor()));
        float scale = resolveScale(mc);
        int textWidth = Mth.ceil(mc.font.width(text) * scale);
        int lineHeight = Mth.ceil(mc.font.lineHeight * scale);
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        BrushHudAnchor anchor = SwitchesClientConfig.BRUSH_MODE_HUD_ANCHOR.get();
        int x = anchorX(anchor, screenWidth, textWidth) + SwitchesClientConfig.BRUSH_MODE_HUD_OFFSET_X.get();
        int y = anchorY(anchor, screenHeight, lineHeight, mc) + SwitchesClientConfig.BRUSH_MODE_HUD_OFFSET_Y.get();
        // Clamp so an extreme nudge parks the text at the edge instead of losing it off screen.
        x = Mth.clamp(x, 0, Math.max(0, screenWidth - textWidth));
        y = Mth.clamp(y, 0, Math.max(0, screenHeight - lineHeight));
        graphics.pose().pushPose();
        // Translate first, then scale, so the anchor stays exact at any size.
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        // Shadow is deliberately not a setting: white text on snow or bright sky is unreadable without it.
        graphics.drawString(mc.font, text, 0, 0, 0xFFFFFF, true);
        graphics.pose().popPose();
    }

    /**
     * Snaps the chosen size to a whole number of real pixels per font pixel. The font is a bitmap,
     * so a fractional size doubles some letter columns and not others and looks ragged. At a low GUI
     * scale there is no whole step left, and the size options collapse together rather than blur.
     */
    private static float resolveScale(@Nonnull Minecraft mc) {
        BrushHudSize size = SwitchesClientConfig.BRUSH_MODE_HUD_SIZE.get();
        double guiScale = mc.getWindow().getGuiScale();
        if (guiScale <= 0.0) {
            return 1.0F;
        }
        int pixels = Math.max(1, (int) Math.round(guiScale * size.getTarget()));
        return (float) (pixels / guiScale);
    }

    private static int anchorX(@Nonnull BrushHudAnchor anchor, int screenWidth, int textWidth) {
        return switch (anchor) {
            case TOP_LEFT, BOTTOM_LEFT -> MARGIN;
            case TOP_RIGHT, BOTTOM_RIGHT -> screenWidth - MARGIN - textWidth;
            case ABOVE_HOTBAR -> (screenWidth - textWidth) / 2;
        };
    }

    private static int anchorY(@Nonnull BrushHudAnchor anchor, int screenHeight, int lineHeight, @Nonnull Minecraft mc) {
        return switch (anchor) {
            case TOP_LEFT, TOP_RIGHT -> MARGIN;
            case BOTTOM_LEFT, BOTTOM_RIGHT -> screenHeight - MARGIN - lineHeight;
            case ABOVE_HOTBAR -> screenHeight - actionBarLift(mc) - ACTION_BAR_CLEARANCE - lineHeight;
        };
    }

    /**
     * Top row of vanilla's action bar message, counted up from the bottom of the screen. Reading
     * Gui's own public row counters means the HUD follows armour, absorption and vehicle health
     * rather than sitting at a fixed height that the next chestplate buries.
     */
    private static int actionBarLift(@Nonnull Minecraft mc) {
        int rows = Math.max(mc.gui.leftHeight, mc.gui.rightHeight) + ACTION_BAR_ROW_SHIFT;
        return Math.max(rows, ACTION_BAR_MIN_ROW) + ACTION_BAR_TEXT_OFFSET;
    }
}
