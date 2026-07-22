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

import javax.annotation.Nonnull;
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
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            if (SecurityUtils.isRateLimited(player)) {
                SecurityUtils.logSecurityViolation(player, "RATE_LIMIT_EXCEEDED",
                    "BrushCopyOverwrite packet rate limit exceeded");
                return;
            }

            if (!SecurityUtils.isValidBlockPosition(msg.blockPos())) {
                SecurityUtils.logSecurityViolation(player, "INVALID_COORDINATES",
                    "Invalid block position: " + msg.blockPos());
                return;
            }

            Level level = player.level();
            BlockPos blockPos = msg.blockPos();

            if (!SecurityUtils.canPlayerInteractWithBlock(player, level, blockPos)) {
                SecurityUtils.logSecurityViolation(player, "UNAUTHORIZED_ACCESS",
                    "Player cannot interact with block at: " + blockPos);
                return;
            }

            SecurityUtils.logSecurityEvent(player, "BRUSH_COPY_OVERWRITE", blockPos,
                "Overwrite: " + msg.overwrite());
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


        net.minecraftforge.network.NetworkHooks.openScreen(player, menuProvider, buf -> buf.writeBlockPos(blockPos));
    }
}
