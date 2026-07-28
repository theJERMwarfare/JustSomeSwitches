package net.justsomeswitches.network;

import net.justsomeswitches.blockentity.SwitchBlockEntity;
import net.justsomeswitches.item.SwitchTextureBrushItem;
import net.justsomeswitches.item.service.CopyPasteService;
import net.justsomeswitches.util.SecurityUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;

/**
 * Network payload for missing block GUI responses
 * Handles client->server communication when user chooses Apply or Cancel for missing blocks
 */
public record BrushMissingBlockPayload(
    BlockPos blockPos,
    boolean apply
) implements CustomPacketPayload {

    public static final Type<BrushMissingBlockPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("justsomeswitches", "wrench_missing_block"));
    public static final StreamCodec<FriendlyByteBuf, BrushMissingBlockPayload> STREAM_CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC, BrushMissingBlockPayload::blockPos,
            ByteBufCodecs.BOOL, BrushMissingBlockPayload::apply,
            BrushMissingBlockPayload::new
        );
    @Override
    @Nonnull
    public Type<BrushMissingBlockPayload> type() {
        return TYPE;
    }
    /** Handles the payload on the server side. */
    public static void handle(BrushMissingBlockPayload payload, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        if (SecurityUtils.isRateLimited(player)) {
            SecurityUtils.logSecurityViolation(player, "RATE_LIMIT_EXCEEDED",
                "BrushMissingBlock packet rate limit exceeded");
            return;
        }
        if (!SecurityUtils.isValidBlockPosition(payload.blockPos())) {
            SecurityUtils.logSecurityViolation(player, "INVALID_COORDINATES",
                "Invalid block position: " + payload.blockPos());
            return;
        }
        Level level = player.level();
        BlockPos blockPos = payload.blockPos();
        if (!SecurityUtils.canPlayerInteractWithBlock(player, level, blockPos)) {
            SecurityUtils.logSecurityViolation(player, "UNAUTHORIZED_ACCESS",
                "Player cannot interact with block at: " + blockPos);
            return;
        }
        SecurityUtils.logSecurityEvent(player, "BRUSH_MISSING_BLOCK", blockPos,
            "Apply: " + payload.apply());
        ItemStack brushStack = null;
        if (player.getMainHandItem().getItem() instanceof SwitchTextureBrushItem) {
            brushStack = player.getMainHandItem();
        } else if (player.getOffhandItem().getItem() instanceof SwitchTextureBrushItem) {
            brushStack = player.getOffhandItem();
        }
        if (brushStack == null || !(brushStack.getItem() instanceof SwitchTextureBrushItem brush)) {
            return; // No brush found
        }
        if (!(level.getBlockEntity(blockPos) instanceof SwitchBlockEntity blockEntity)) {
            return;
        }
        if (payload.apply()) {
            handlePartialApply(brush, brushStack, blockEntity, player);
        } else {
            handleCancel(player);
        }
        player.inventoryMenu.broadcastChanges();
    }
    private static void handlePartialApply(SwitchTextureBrushItem brush, ItemStack brushStack,
                                         SwitchBlockEntity blockEntity, ServerPlayer player) {
        CopyPasteService.PasteResult result = brush.applyPartialSettingsFromBrushServer(brushStack, blockEntity, player);
        if (result.success) {
            NetworkHandler.sendActionBarMessage(player, result.message, NetworkHandler.MessageType.SUCCESS);
        } else {
            NetworkHandler.sendActionBarMessage(player, result.message, NetworkHandler.MessageType.ERROR);
        }
    }
    private static void handleCancel(ServerPlayer player) {
        NetworkHandler.sendActionBarMessage(player, "New Texture Settings Not Pasted", NetworkHandler.MessageType.INFO);
    }
}
