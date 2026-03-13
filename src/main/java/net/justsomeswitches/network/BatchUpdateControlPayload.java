package net.justsomeswitches.network;

import net.justsomeswitches.blockentity.SwitchBlockEntity;
import net.justsomeswitches.util.SecurityUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Network payload for controlling batch update mode on BlockEntities.
 * Signals when GUI opens (start batching) and closes (flush queue).
 */
public record BatchUpdateControlPayload(
    BlockPos blockPos,
    boolean startBatch  // true = start batching, false = end batch and flush
) {

    public static void encode(BatchUpdateControlPayload msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.blockPos());
        buf.writeBoolean(msg.startBatch());
    }

    public static BatchUpdateControlPayload decode(FriendlyByteBuf buf) {
        return new BatchUpdateControlPayload(
            buf.readBlockPos(),
            buf.readBoolean()
        );
    }

    public static void handle(BatchUpdateControlPayload msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            if (SecurityUtils.isRateLimited(player)) {
                SecurityUtils.logSecurityViolation(player, "RATE_LIMIT_EXCEEDED",
                    "BatchUpdateControl packet rate limit exceeded");
                return;
            }
            if (!SecurityUtils.isValidBlockPosition(msg.blockPos())) {
                SecurityUtils.logSecurityViolation(player, "INVALID_COORDINATES",
                    "Invalid block position: " + msg.blockPos());
                return;
            }
            Level level = player.level();
            if (!SecurityUtils.canPlayerInteractWithBlock(player, level, msg.blockPos())) {
                SecurityUtils.logSecurityViolation(player, "UNAUTHORIZED_ACCESS",
                    "Player cannot interact with block at: " + msg.blockPos());
                return;
            }
            SecurityUtils.logSecurityEvent(player, "BATCH_UPDATE_CONTROL", msg.blockPos(),
                "Start batch: " + msg.startBatch());
            BlockEntity blockEntity = level.getBlockEntity(msg.blockPos());
            if (!(blockEntity instanceof SwitchBlockEntity switchEntity)) {
                return;
            }
            if (msg.startBatch()) {
                switchEntity.startBatchMode();
            } else {
                switchEntity.endBatchModeAndFlush();
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
