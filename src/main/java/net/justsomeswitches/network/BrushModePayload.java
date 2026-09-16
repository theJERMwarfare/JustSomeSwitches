package net.justsomeswitches.network;

import net.justsomeswitches.JustSomeSwitchesMod;
import net.justsomeswitches.item.BrushMode;
import net.justsomeswitches.item.SwitchTextureBrushItem;
import net.justsomeswitches.util.SecurityUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;

/**
 * Network payload setting the Switch Texture Brush mode. Client decides, server applies.
 * wantsMessage carries the sender's own display preference: the server stays the authority on
 * whether the change happened, the client only says whether it wants that confirmed on screen.
 */
public record BrushModePayload(BrushMode mode, boolean wantsMessage) implements CustomPacketPayload {

    public static final Type<BrushModePayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(JustSomeSwitchesMod.MODID, "brush_mode"));
    // readEnum throws on an out-of-range ordinal, which rejects a malformed packet rather than
    // defaulting it to an action the sender did not ask for.
    public static final StreamCodec<FriendlyByteBuf, BrushModePayload> STREAM_CODEC =
        StreamCodec.of(
            (buf, payload) -> {
                buf.writeEnum(payload.mode());
                buf.writeBoolean(payload.wantsMessage());
            },
            buf -> new BrushModePayload(buf.readEnum(BrushMode.class), buf.readBoolean())
        );

    @Override
    @Nonnull
    public Type<BrushModePayload> type() {
        return TYPE;
    }
    /** Handles the payload on the server side. */
    public static void handle(BrushModePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        if (SecurityUtils.isRateLimited(player)) {
            SecurityUtils.logSecurityViolation(player, "RATE_LIMIT_EXCEEDED",
                "BrushMode packet rate limit exceeded");
            return;
        }
        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!(held.getItem() instanceof SwitchTextureBrushItem)) {
            return; // No brush in the main hand
        }
        if (SwitchTextureBrushItem.getMode(held) == payload.mode()) {
            return;
        }
        SwitchTextureBrushItem.setMode(held, payload.mode());
        if (payload.wantsMessage()) {
            NetworkHandler.sendBrushModeMessage(player, payload.mode());
        }
        player.inventoryMenu.broadcastChanges();
    }
}
