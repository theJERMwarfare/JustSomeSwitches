package net.justsomeswitches.network;

import net.justsomeswitches.JustSomeSwitchesMod;
import net.justsomeswitches.blockentity.SwitchBlockEntity;
import net.justsomeswitches.util.SecurityUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;

import javax.annotation.Nonnull;

/**
 * Network payload for controlling batch update mode on BlockEntities.
 * Signals when GUI opens (start batching) and closes (flush queue).
 */
public record BatchUpdateControlPayload(
    BlockPos blockPos,
    boolean startBatch  // true = start batching, false = end batch and flush
) implements CustomPacketPayload {
    
    public static final ResourceLocation ID = new ResourceLocation(JustSomeSwitchesMod.MODID, "batch_update_control");
    
    public BatchUpdateControlPayload(FriendlyByteBuf buf) {
        this(
            buf.readBlockPos(),
            buf.readBoolean()
        );
    }
    
    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(blockPos);
        buf.writeBoolean(startBatch);
    }
    
    @Override
    @Nonnull
    public ResourceLocation id() {
        return ID;
    }
    
    public static void handle(BatchUpdateControlPayload payload, PlayPayloadContext context) {
        context.workHandler().submitAsync(() -> {
            ServerPlayer player = SecurityUtils.validateAndGetSender(context, payload.blockPos(), "BatchUpdateControl");
            if (player == null) {
                return;
            }
            Level level = player.level();
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
        });
    }
}
