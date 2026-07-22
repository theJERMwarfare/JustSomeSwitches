package net.justsomeswitches.network;

import net.justsomeswitches.blockentity.SwitchBlockEntity;
import net.justsomeswitches.item.SwitchTextureBrushItem;
import net.justsomeswitches.item.service.CopyPasteService;
import net.justsomeswitches.util.SecurityUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Network payload for missing block GUI responses
 * Handles client->server communication when user chooses Apply or Cancel for missing blocks
 */
public record BrushMissingBlockPayload(
    BlockPos blockPos,
    boolean apply
) {

    public static void encode(BrushMissingBlockPayload msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.blockPos());
        buf.writeBoolean(msg.apply());
    }

    public static BrushMissingBlockPayload decode(FriendlyByteBuf buf) {
        return new BrushMissingBlockPayload(
            buf.readBlockPos(),
            buf.readBoolean()
        );
    }

    /**
     * Handles the payload on the server side
     */
    public static void handle(BrushMissingBlockPayload msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // Security validation - Rate limiting
            if (SecurityUtils.isRateLimited(player)) {
                SecurityUtils.logSecurityViolation(player, "RATE_LIMIT_EXCEEDED",
                    "BrushMissingBlock packet rate limit exceeded");
                return;
            }

            // Security validation - Block position bounds
            if (!SecurityUtils.isValidBlockPosition(msg.blockPos())) {
                SecurityUtils.logSecurityViolation(player, "INVALID_COORDINATES",
                    "Invalid block position: " + msg.blockPos());
                return;
            }

            Level level = player.level();
            BlockPos blockPos = msg.blockPos();

            // Security validation - Player permissions
            if (!SecurityUtils.canPlayerInteractWithBlock(player, level, blockPos)) {
                SecurityUtils.logSecurityViolation(player, "UNAUTHORIZED_ACCESS",
                    "Player cannot interact with block at: " + blockPos);
                return;
            }

            // Log security event for audit
            SecurityUtils.logSecurityEvent(player, "BRUSH_MISSING_BLOCK", blockPos,
                "Apply: " + msg.apply());

            // Find the brush in player's hands
            ItemStack brushStack = null;

            if (player.getMainHandItem().getItem() instanceof SwitchTextureBrushItem) {
                brushStack = player.getMainHandItem();
            } else if (player.getOffhandItem().getItem() instanceof SwitchTextureBrushItem) {
                brushStack = player.getOffhandItem();
            }

            if (brushStack == null || !(brushStack.getItem() instanceof SwitchTextureBrushItem brush)) {
                return; // No brush found
            }

            // Verify the block is still a switch
            if (!(level.getBlockEntity(blockPos) instanceof SwitchBlockEntity blockEntity)) {
                return;
            }

            if (msg.apply()) {
                // User chose to apply partial settings
                handlePartialApply(brush, brushStack, blockEntity, player);
            } else {
                // User chose to cancel
                handleCancel(player);
            }

            // Mark player inventory as dirty to sync changes
            player.inventoryMenu.broadcastChanges();
        });
        ctx.get().setPacketHandled(true);
    }

    private static void handlePartialApply(SwitchTextureBrushItem brush, ItemStack brushStack,
                                         SwitchBlockEntity blockEntity, ServerPlayer player) {
        // Apply settings for categories that don't require missing blocks
        CopyPasteService.PasteResult result = brush.applyPartialSettingsFromBrushServer(brushStack, blockEntity, player);

        if (result.success) {
            NetworkHandler.sendActionBarMessage(player, result.message, NetworkHandler.MessageType.SUCCESS);
        } else {
            NetworkHandler.sendActionBarMessage(player, result.message, NetworkHandler.MessageType.ERROR);
        }
    }

    private static void handleCancel(ServerPlayer player) {
        // Send cancellation message
        NetworkHandler.sendActionBarMessage(player, "New Texture Settings Not Pasted", NetworkHandler.MessageType.INFO);
    }
}
