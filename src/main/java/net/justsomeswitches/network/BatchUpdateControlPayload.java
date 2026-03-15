package net.justsomeswitches.network;

import net.justsomeswitches.JustSomeSwitchesMod;
import net.justsomeswitches.blockentity.SwitchBlockEntity;
import net.justsomeswitches.util.SecurityUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;

/**
 * Network payload for controlling batch update mode on BlockEntities.
 * Signals when GUI opens (start batching) and closes (flush queue).
 */
public record BatchUpdateControlPayload(
    BlockPos blockPos,
    boolean startBatch  // true = start batching, false = end batch and flush
) implements CustomPacketPayload {

    public static final Type<BatchUpdateControlPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(JustSomeSwitchesMod.MODID, "batch_update_control"));
    public static final StreamCodec<FriendlyByteBuf, BatchUpdateControlPayload> STREAM_CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC, BatchUpdateControlPayload::blockPos,
            ByteBufCodecs.BOOL, BatchUpdateControlPayload::startBatch,
            BatchUpdateControlPayload::new
        );
    @Override
    @Nonnull
    public Type<BatchUpdateControlPayload> type() {
        return TYPE;
    }
    public static void handle(BatchUpdateControlPayload payload, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        if (SecurityUtils.isRateLimited(player)) {
            SecurityUtils.logSecurityViolation(player, "RATE_LIMIT_EXCEEDED",
                "BatchUpdateControl packet rate limit exceeded");
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
        SecurityUtils.logSecurityEvent(player, "BATCH_UPDATE_CONTROL", payload.blockPos(),
            "Start batch: " + payload.startBatch());
        BlockEntity blockEntity = level.getBlockEntity(payload.blockPos());
        if (!(blockEntity instanceof SwitchBlockEntity switchEntity)) {
            return;
        }
        if (payload.startBatch()) {
            switchEntity.startBatchMode();
        } else {
            switchEntity.endBatchModeAndFlush();
        }
    }
}
