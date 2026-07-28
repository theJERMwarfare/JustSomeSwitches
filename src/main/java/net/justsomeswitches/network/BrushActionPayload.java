package net.justsomeswitches.network;

import net.justsomeswitches.blockentity.SwitchBlockEntity;
import net.justsomeswitches.item.SwitchTextureBrushItem;
import net.justsomeswitches.item.service.CopyPasteService;
import net.justsomeswitches.util.SecurityUtils;
import net.justsomeswitches.util.BrushConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;

/** Network payload for Switches Brush copy/paste operations. */
public record BrushActionPayload(
    BlockPos blockPos,
    BrushAction action,
    InteractionHand hand
) implements CustomPacketPayload {

    public static final Type<BrushActionPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("justsomeswitches", "wrench_action"));
    public static final StreamCodec<FriendlyByteBuf, BrushActionPayload> STREAM_CODEC =
        StreamCodec.of(
            (buf, payload) -> {
                buf.writeBlockPos(payload.blockPos());
                buf.writeEnum(payload.action());
                buf.writeEnum(payload.hand());
            },
            buf -> new BrushActionPayload(
                buf.readBlockPos(),
                buf.readEnum(BrushAction.class),
                buf.readEnum(InteractionHand.class)
            )
        );
    public enum BrushAction {
        COPY,
        PASTE
    }
    @Override
    @Nonnull
    public Type<BrushActionPayload> type() {
        return TYPE;
    }
    /** Handles the payload on the server side. */
    public static void handle(BrushActionPayload payload, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        if (SecurityUtils.isRateLimited(player)) {
            SecurityUtils.logSecurityViolation(player, "RATE_LIMIT_EXCEEDED",
                "BrushAction packet rate limit exceeded");
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
        SecurityUtils.logSecurityEvent(player, "BRUSH_ACTION", blockPos,
            "Action: " + payload.action() + ", Hand: " + payload.hand());
        ItemStack stack = player.getItemInHand(payload.hand());
        if (!(stack.getItem() instanceof SwitchTextureBrushItem brush)) {
            return;
        }
        if (!(level.getBlockEntity(blockPos) instanceof SwitchBlockEntity blockEntity)) {
            return;
        }
        switch (payload.action()) {
            case COPY -> handleCopyAction(brush, stack, blockEntity, player);
            case PASTE -> handlePasteAction(brush, stack, blockEntity, player, blockPos);
        }
        player.inventoryMenu.broadcastChanges();
    }
    @SuppressWarnings("unused") // Parameters kept for API consistency
    private static void handleCopyAction(SwitchTextureBrushItem brush, ItemStack stack,
                                       SwitchBlockEntity blockEntity, ServerPlayer player) {
        NetworkHandler.sendActionBarMessage(player, "Use Copy GUI for copying settings", NetworkHandler.MessageType.INFO);
    }
    @SuppressWarnings("resource") // Level lifecycle managed by Minecraft
    private static void handlePasteAction(SwitchTextureBrushItem brush, ItemStack stack,
                                        SwitchBlockEntity blockEntity, ServerPlayer player, BlockPos blockPos) {
        if (!brush.hasCopiedSettingsServer(stack)) {
            return;
        }
        if (brush.hasIdenticalSettingsServer(stack, blockEntity)) {
            Level level = player.level();
            net.minecraft.world.level.block.state.BlockState blockState = level.getBlockState(blockPos);
            net.minecraft.world.item.ItemStack blockItem = new net.minecraft.world.item.ItemStack(blockState.getBlock());
            String blockName = blockItem.getDisplayName().getString();
            NetworkHandler.sendActionBarMessage(player, blockName + " Already Has the Same Texture Settings", NetworkHandler.MessageType.INFO);
            return;
        }
        if (blockEntity.hasCustomTextures()) {
            openOverwriteConfirmationGUI(player, blockEntity.getBlockPos());
            return;
        }
        CopyPasteService.PasteResult result = brush.applySettingsFromBrushServer(stack, blockEntity, player);
        if (!result.success && BrushConstants.MSG_MISSING_BLOCKS_GUI.equals(result.message)) {
            openMissingBlockGUI(player, blockPos, result.missingBlocks);
            return;
        }
        if (result.success) {
            NetworkHandler.sendActionBarMessage(player, result.message, NetworkHandler.MessageType.SUCCESS);
        } else {
            NetworkHandler.sendActionBarMessage(player, result.message, NetworkHandler.MessageType.ERROR);
        }
    }
    /** Opens the missing block GUI for the player. */
    private static void openMissingBlockGUI(ServerPlayer player, BlockPos blockPos, java.util.List<String> missingBlocks) {
        NetworkHandler.openMissingBlockGUI(player, blockPos, missingBlocks);
    }
    /** Opens the overwrite confirmation GUI for the player. */
    private static void openOverwriteConfirmationGUI(ServerPlayer player, BlockPos blockPos) {
        net.minecraft.world.MenuProvider menuProvider = new net.minecraft.world.MenuProvider() {
            @Override
            @javax.annotation.Nonnull
            public net.minecraft.network.chat.Component getDisplayName() {
                return net.minecraft.network.chat.Component.literal("Settings Already Stored");
            }
            @Override
            @javax.annotation.Nonnull
            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId,
                                                                                 @javax.annotation.Nonnull net.minecraft.world.entity.player.Inventory playerInventory,
                                                                                 @javax.annotation.Nonnull net.minecraft.world.entity.player.Player player) {
                return new net.justsomeswitches.gui.BrushOverwriteMenu(containerId, playerInventory, blockPos);
            }
        };
        player.openMenu(menuProvider, buf -> buf.writeBlockPos(blockPos));
    }
}
