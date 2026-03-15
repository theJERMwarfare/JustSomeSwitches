package net.justsomeswitches.network;

import net.justsomeswitches.blockentity.SwitchBlockEntity;
import net.justsomeswitches.item.SwitchesWrenchItem;
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
public record WrenchMissingBlockPayload(
    BlockPos blockPos,
    boolean apply
) implements CustomPacketPayload {

    public static final Type<WrenchMissingBlockPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("justsomeswitches", "wrench_missing_block"));
    public static final StreamCodec<FriendlyByteBuf, WrenchMissingBlockPayload> STREAM_CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC, WrenchMissingBlockPayload::blockPos,
            ByteBufCodecs.BOOL, WrenchMissingBlockPayload::apply,
            WrenchMissingBlockPayload::new
        );
    @Override
    @Nonnull
    public Type<WrenchMissingBlockPayload> type() {
        return TYPE;
    }
    /** Handles the payload on the server side. */
    public static void handle(WrenchMissingBlockPayload payload, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        if (SecurityUtils.isRateLimited(player)) {
            SecurityUtils.logSecurityViolation(player, "RATE_LIMIT_EXCEEDED",
                "WrenchMissingBlock packet rate limit exceeded");
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
        SecurityUtils.logSecurityEvent(player, "WRENCH_MISSING_BLOCK", blockPos,
            "Apply: " + payload.apply());
        ItemStack wrenchStack = null;
        if (player.getMainHandItem().getItem() instanceof SwitchesWrenchItem) {
            wrenchStack = player.getMainHandItem();
        } else if (player.getOffhandItem().getItem() instanceof SwitchesWrenchItem) {
            wrenchStack = player.getOffhandItem();
        }
        if (wrenchStack == null || !(wrenchStack.getItem() instanceof SwitchesWrenchItem wrench)) {
            return; // No wrench found
        }
        if (!(level.getBlockEntity(blockPos) instanceof SwitchBlockEntity blockEntity)) {
            return;
        }
        if (payload.apply()) {
            handlePartialApply(wrench, wrenchStack, blockEntity, player);
        } else {
            handleCancel(player);
        }
        player.inventoryMenu.broadcastChanges();
    }
    private static void handlePartialApply(SwitchesWrenchItem wrench, ItemStack wrenchStack,
                                         SwitchBlockEntity blockEntity, ServerPlayer player) {
        CopyPasteService.PasteResult result = wrench.applyPartialSettingsFromWrenchServer(wrenchStack, blockEntity, player);
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
