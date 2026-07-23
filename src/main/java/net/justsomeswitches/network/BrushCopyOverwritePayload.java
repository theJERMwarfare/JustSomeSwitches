package net.justsomeswitches.network;

import net.justsomeswitches.blockentity.SwitchBlockEntity;
import net.justsomeswitches.item.SwitchTextureBrushItem;
import net.justsomeswitches.util.SecurityUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Network payload for brush copy overwrite confirmation responses. */
public record BrushCopyOverwritePayload(
    BlockPos blockPos,
    boolean overwrite
) {

    public static void encode(BrushCopyOverwritePayload msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.blockPos());
        buf.writeBoolean(msg.overwrite());
    }

    public static BrushCopyOverwritePayload decode(FriendlyByteBuf buf) {
        return new BrushCopyOverwritePayload(
            buf.readBlockPos(),
            buf.readBoolean()
        );
    }

    /** Handles the payload on server side. */
    public static void handle(BrushCopyOverwritePayload msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = SecurityUtils.validateAndGetSender(ctx.get(), msg.blockPos(), "BrushCopyOverwrite");
            if (player == null) return;

            Level level = player.level();
            BlockPos blockPos = msg.blockPos();

            SecurityUtils.logSecurityEvent(player, "BRUSH_COPY_OVERWRITE", blockPos,
                "Overwrite: " + msg.overwrite());
            ItemStack brushStack = SwitchTextureBrushItem.findBrushInHands(player);
            if (brushStack == null) {
                return; // No brush found
            }
            SwitchTextureBrushItem brush = (SwitchTextureBrushItem) brushStack.getItem();


            if (!(level.getBlockEntity(blockPos) instanceof SwitchBlockEntity blockEntity)) {
                return;
            }

            if (msg.overwrite()) {
                handleCopyOverwriteConfirmed(brush, brushStack, blockEntity, player, blockPos);
            } else {
                handleCopyOverwriteCancelled(player);
            }


            player.inventoryMenu.broadcastChanges();
        });
        ctx.get().setPacketHandled(true);
    }

    private static void handleCopyOverwriteConfirmed(SwitchTextureBrushItem brush, ItemStack brushStack,
                                                   @SuppressWarnings("unused") SwitchBlockEntity blockEntity, ServerPlayer player, BlockPos blockPos) {
        brush.clearAllSettingsServer(brushStack);
        NetworkHandler.sendActionBarMessage(player, "Previous Texture Settings Cleared", NetworkHandler.MessageType.SUCCESS);
        NetworkHandler.openCopyTextureGUI(player, blockPos);
    }

    private static void handleCopyOverwriteCancelled(ServerPlayer player) {

        NetworkHandler.sendActionBarMessage(player, "New Texture Settings Not Copied, Previous Texture Settings Retained", NetworkHandler.MessageType.INFO);
    }

}
