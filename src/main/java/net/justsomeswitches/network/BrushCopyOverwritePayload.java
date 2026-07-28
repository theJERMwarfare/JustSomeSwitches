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
        ServerPlayer player = (ServerPlayer) context.player();
        if (SecurityUtils.isRateLimited(player)) {
            SecurityUtils.logSecurityViolation(player, "RATE_LIMIT_EXCEEDED",
                "BrushCopyOverwrite packet rate limit exceeded");
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
        SecurityUtils.logSecurityEvent(player, "BRUSH_COPY_OVERWRITE", blockPos,
            "Overwrite: " + payload.overwrite());
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
        openCopyTextureGUI(player, blockPos);
    }
    private static void handleCopyOverwriteCancelled(ServerPlayer player) {
        NetworkHandler.sendActionBarMessage(player, "New Texture Settings Not Copied, Previous Texture Settings Retained", NetworkHandler.MessageType.INFO);
    }
    /** Opens the copy texture settings GUI. */
    private static void openCopyTextureGUI(ServerPlayer player, BlockPos blockPos) {
        net.minecraft.world.MenuProvider menuProvider = new net.minecraft.world.MenuProvider() {
            @Override
            @Nonnull
            public net.minecraft.network.chat.Component getDisplayName() {
                return net.minecraft.network.chat.Component.literal("Copy Texture Settings");
            }
            @Override
            @javax.annotation.Nonnull
            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId, @javax.annotation.Nonnull net.minecraft.world.entity.player.Inventory playerInventory, @javax.annotation.Nonnull net.minecraft.world.entity.player.Player player) {
                return new net.justsomeswitches.gui.BrushCopyMenu(containerId, playerInventory, blockPos);
            }
        };
        player.openMenu(menuProvider, buf -> buf.writeBlockPos(blockPos));
    }
}
