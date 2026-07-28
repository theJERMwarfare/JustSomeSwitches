package net.justsomeswitches.network;

import net.justsomeswitches.blockentity.SwitchBlockEntity;
import net.justsomeswitches.item.SwitchTextureBrushItem;
import net.justsomeswitches.util.SecurityUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;

/** Network payload for brush copy selection operations. */
public record BrushCopySelectionPayload(
    BlockPos blockPos,
    boolean copyToggleBlock,
    boolean copyToggleFace,
    boolean copyToggleRotation,
    boolean copyIndicators,
    boolean copyBaseBlock,
    boolean copyBaseFace,
    boolean copyBaseRotation
) implements CustomPacketPayload {

    public static final Type<BrushCopySelectionPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("justsomeswitches", "wrench_copy_selection"));
    public static final StreamCodec<FriendlyByteBuf, BrushCopySelectionPayload> STREAM_CODEC =
        StreamCodec.of(
            (buf, payload) -> {
                buf.writeBlockPos(payload.blockPos());
                buf.writeBoolean(payload.copyToggleBlock());
                buf.writeBoolean(payload.copyToggleFace());
                buf.writeBoolean(payload.copyToggleRotation());
                buf.writeBoolean(payload.copyIndicators());
                buf.writeBoolean(payload.copyBaseBlock());
                buf.writeBoolean(payload.copyBaseFace());
                buf.writeBoolean(payload.copyBaseRotation());
            },
            buf -> new BrushCopySelectionPayload(
                buf.readBlockPos(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean()
            )
        );
    @Override
    @Nonnull
    public Type<BrushCopySelectionPayload> type() {
        return TYPE;
    }
    /** Handles copy selection on server side. */
    public static void handle(BrushCopySelectionPayload payload, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        if (SecurityUtils.isRateLimited(player)) {
            SecurityUtils.logSecurityViolation(player, "RATE_LIMIT_EXCEEDED",
                "BrushCopySelection packet rate limit exceeded");
            return;
        }
        if (!SecurityUtils.isValidBlockPosition(payload.blockPos())) {
            SecurityUtils.logSecurityViolation(player, "INVALID_COORDINATES",
                "Invalid block position: " + payload.blockPos());
            return;
        }
        Level level = player.level();
        if (!SecurityUtils.canPlayerInteractWithBlock(player, level, payload.blockPos())) {
            SecurityUtils.logSecurityViolation(player, "UNAUTHORIZED_ACCESS",
                "Player cannot interact with block at: " + payload.blockPos());
            return;
        }
        SecurityUtils.logSecurityEvent(player, "BRUSH_COPY_SELECTION", payload.blockPos(),
            String.format("Toggle: %b/%b/%b, Base: %b/%b/%b, Indicators: %b",
                payload.copyToggleBlock(), payload.copyToggleFace(), payload.copyToggleRotation(),
                payload.copyBaseBlock(), payload.copyBaseFace(), payload.copyBaseRotation(),
                payload.copyIndicators()));
        BlockEntity blockEntity = level.getBlockEntity(payload.blockPos());
        if (!(blockEntity instanceof SwitchBlockEntity switchEntity)) {
            return;
        }
        ItemStack mainHandStack = player.getMainHandItem();
        ItemStack offHandStack = player.getOffhandItem();
        ItemStack brushStack;
        if (mainHandStack.getItem() instanceof SwitchTextureBrushItem) {
            brushStack = mainHandStack;
        } else if (offHandStack.getItem() instanceof SwitchTextureBrushItem) {
            brushStack = offHandStack;
        } else {
            return;
        }
        SwitchTextureBrushItem brush = (SwitchTextureBrushItem) brushStack.getItem();
        brush.copySelectedSettingsToBrush(brushStack, switchEntity,
            payload.copyToggleBlock(), payload.copyToggleFace(), payload.copyToggleRotation(),
            payload.copyIndicators(), payload.copyBaseBlock(), payload.copyBaseFace(),
            payload.copyBaseRotation());
        NetworkHandler.sendActionBarMessage(player, "Texture Settings Copied Successfully", NetworkHandler.MessageType.SUCCESS);
        player.inventoryMenu.broadcastChanges();
    }
}
