package net.justsomeswitches.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.justsomeswitches.JustSomeSwitchesMod;
import net.justsomeswitches.config.BrushHudAnchor;
import net.justsomeswitches.config.SwitchesClientConfig;
import net.justsomeswitches.item.BrushMode;
import net.justsomeswitches.item.SwitchTextureBrushItem;
import net.justsomeswitches.network.NetworkHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;

import javax.annotation.Nullable;

/**
 * Client input for switching brush modes. Lives in the client package behind a dist check because
 * RuntimeDistCleaner scans a class's whole constant pool, and this references client-only types.
 */
@EventBusSubscriber(modid = JustSomeSwitchesMod.MODID, value = Dist.CLIENT)
public class BrushModeInputHandler {

    /**
     * Scroll is handled per gesture, not per notch. A free-spinning wheel (Logitech SmartShift and
     * similar) keeps firing events for a second or more after the flick, which otherwise runs the mode
     * on by itself and leaks the tail of the flick onto the hotbar once sneak is released.
     * A gesture is a run of scroll events less than GESTURE_IDLE_TICKS apart: its first notch changes
     * the mode once, and every later notch is swallowed.
     */
    private static final int GESTURE_IDLE_TICKS = 4;
    private static int tickCounter = 0;
    private static int lastScrollTick = -1000;
    private static boolean gestureOwned = false;
    private static boolean pendingChange = false;
    private static boolean pendingForward = false;

    /** Unbound by default so it cannot collide with anything in a large modpack. */
    public static final KeyMapping CYCLE_MODE = new KeyMapping(
        "key.justsomeswitches.cycle_brush_mode",
        InputConstants.UNKNOWN.getValue(),
        "key.categories.justsomeswitches");

    /** Sneak + scroll cycles the mode while the brush is held. One change per flick. */
    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        double delta = event.getScrollDeltaY();
        if (delta == 0) {
            return;
        }
        boolean continuing = (tickCounter - lastScrollTick) <= GESTURE_IDLE_TICKS;
        lastScrollTick = tickCounter;
        if (continuing) {
            // Tail of a flick we already acted on. Swallow it so a coasting wheel neither advances
            // the mode again nor reaches the hotbar after sneak is released.
            if (gestureOwned) {
                event.setCanceled(true);
            }
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        gestureOwned = player != null && player.isShiftKeyDown() && getHeldBrushMode(player) != null;
        if (!gestureOwned) {
            return; // Ordinary hotbar scrolling, leave it alone
        }
        // Scrolling down advances Customize -> Copy -> Paste, matching the keybind's order.
        pendingChange = true;
        pendingForward = delta < 0;
        event.setCanceled(true);
    }

    /** Applies a pending flick and any keybind presses, at most one packet per tick. */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        tickCounter++;
        int steps = 0;
        if (pendingChange) {
            steps += pendingForward ? 1 : -1;
            pendingChange = false;
        }
        while (CYCLE_MODE.consumeClick()) {
            steps++;
        }
        if (steps == 0) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        BrushMode current = getHeldBrushMode(player);
        if (current == null) {
            return;
        }
        BrushMode target = step(current, steps);
        if (target != current) {
            NetworkHandler.sendBrushMode(target, wantsModeMessage());
        }
    }

    /**
     * Whether this client wants the action bar to confirm the change. SMART suppresses it only when
     * the HUD is actually shown above the hotbar, where the two would land on top of each other;
     * with the HUD off there is no other feedback, so the message stays.
     */
    private static boolean wantsModeMessage() {
        return switch (SwitchesClientConfig.BRUSH_MODE_MESSAGE.get()) {
            case ON -> true;
            case OFF -> false;
            case SMART -> !(SwitchesClientConfig.SHOW_BRUSH_MODE_HUD.get()
                && SwitchesClientConfig.BRUSH_MODE_HUD_ANCHOR.get() == BrushHudAnchor.ABOVE_HOTBAR);
        };
    }

    /** Advances the mode by the given number of steps, forwards or backwards. */
    private static BrushMode step(BrushMode from, int steps) {
        BrushMode mode = from;
        for (int i = 0; i < Math.abs(steps); i++) {
            mode = steps > 0 ? mode.next() : mode.previous();
        }
        return mode;
    }

    /** Current mode of the main-hand brush, or null when no brush is held there. */
    @Nullable
    private static BrushMode getHeldBrushMode(LocalPlayer player) {
        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!(held.getItem() instanceof SwitchTextureBrushItem)) {
            return null;
        }
        return SwitchTextureBrushItem.getMode(held);
    }
}
