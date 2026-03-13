package net.justsomeswitches.network;

import net.justsomeswitches.blockentity.SwitchBlockEntity;
import net.justsomeswitches.item.SwitchesWrenchItem;
import net.justsomeswitches.util.SecurityUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

/** Network payload for wrench copy overwrite confirmation responses. */
public record WrenchCopyOverwritePayload(
    BlockPos blockPos,
    boolean overwrite
) {

    public static void encode(WrenchCopyOverwritePayload msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.blockPos());
        buf.writeBoolean(msg.overwrite());
    }

    public static WrenchCopyOverwritePayload decode(FriendlyByteBuf buf) {
        return new WrenchCopyOverwritePayload(
            buf.readBlockPos(),
            buf.readBoolean()
        );
    }

    /** Handles the payload on server side. */
    public static void handle(WrenchCopyOverwritePayload msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            if (SecurityUtils.isRateLimited(player)) {
                SecurityUtils.logSecurityViolation(player, "RATE_LIMIT_EXCEEDED",
                    "WrenchCopyOverwrite packet rate limit exceeded");
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

            SecurityUtils.logSecurityEvent(player, "WRENCH_COPY_OVERWRITE", blockPos,
                "Overwrite: " + msg.overwrite());
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

            if (msg.overwrite()) {
                handleCopyOverwriteConfirmed(wrench, wrenchStack, blockEntity, player, blockPos);
            } else {
                handleCopyOverwriteCancelled(player);
            }


            player.inventoryMenu.broadcastChanges();
        });
        ctx.get().setPacketHandled(true);
    }

    private static void handleCopyOverwriteConfirmed(SwitchesWrenchItem wrench, ItemStack wrenchStack,
                                                   @SuppressWarnings("unused") SwitchBlockEntity blockEntity, ServerPlayer player, BlockPos blockPos) {
        wrench.clearAllSettingsServer(wrenchStack);
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
                return new net.justsomeswitches.gui.WrenchCopyMenu(containerId, playerInventory, blockPos);
            }
        };


        net.minecraftforge.network.NetworkHooks.openScreen(player, menuProvider, buf -> buf.writeBlockPos(blockPos));
    }
}
