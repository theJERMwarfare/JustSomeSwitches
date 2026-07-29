package net.justsomeswitches.network;

import net.justsomeswitches.blockentity.SwitchBlockEntity;
import net.justsomeswitches.item.SwitchTextureBrushItem;
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

/** Network payload for brush copy overwrite confirmation responses. */
public record BrushCopyOverwritePayload(
    BlockPos blockPos,
    boolean overwrite
) implements CustomPacketPayload {

    public static final Type<BrushCopyOverwritePayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("justsomeswitches", "wrench_copy_overwrite"));
    public static final StreamCodec<FriendlyByteBuf, BrushCopyOverwritePayload> STREAM_CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC, BrushCopyOverwritePayload::blockPos,
            ByteBufCodecs.BOOL, BrushCopyOverwritePayload::overwrite,
            BrushCopyOverwritePayload::new
        );
    @Override
    @Nonnull
    public Type<BrushCopyOverwritePayload> type() {
        return TYPE;
    }
    /** Handles the payload on server side. */
    public static void handle(BrushCopyOverwritePayload payload, IPayloadContext context) {
        ServerPlayer player = SecurityUtils.validateAndGetSender(context, payload.blockPos(), "BrushCopyOverwrite");
        if (player == null) {
            return;
        }
        Level level = player.level();
        BlockPos blockPos = payload.blockPos();
        SecurityUtils.logSecurityEvent(player, "BRUSH_COPY_OVERWRITE", blockPos,
            "Overwrite: " + payload.overwrite());
        ItemStack brushStack = SwitchTextureBrushItem.findBrushInHands(player);
        if (brushStack == null || !(brushStack.getItem() instanceof SwitchTextureBrushItem brush)) {
            return; // No brush found
        }
        if (!(level.getBlockEntity(blockPos) instanceof SwitchBlockEntity blockEntity)) {
            return;
        }
        if (payload.overwrite()) {
            handleCopyOverwriteConfirmed(brush, brushStack, blockEntity, player, blockPos);
        } else {
            handleCopyOverwriteCancelled(player);
        }
        player.inventoryMenu.broadcastChanges();
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
