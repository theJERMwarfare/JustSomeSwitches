package net.justsomeswitches.network;

import net.justsomeswitches.blockentity.SwitchBlockEntity;
import net.justsomeswitches.item.SwitchesWrenchItem;
import net.justsomeswitches.item.service.CopyPasteService;
import net.justsomeswitches.util.SecurityUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Network payload for Switches Wrench copy/paste operations. */
public record WrenchActionPayload(
    BlockPos blockPos,
    WrenchAction action,
    InteractionHand hand
) {

    public enum WrenchAction {
        COPY,
        PASTE
    }

    public static void encode(WrenchActionPayload msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.blockPos());
        buf.writeEnum(msg.action());
        buf.writeEnum(msg.hand());
    }

    public static WrenchActionPayload decode(FriendlyByteBuf buf) {
        return new WrenchActionPayload(
            buf.readBlockPos(),
            buf.readEnum(WrenchAction.class),
            buf.readEnum(InteractionHand.class)
        );
    }

    /** Handles the payload on the server side. */
    public static void handle(WrenchActionPayload msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (SecurityUtils.isRateLimited(player)) {
                SecurityUtils.logSecurityViolation(player, "RATE_LIMIT_EXCEEDED",
                    "WrenchAction packet rate limit exceeded");
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
            SecurityUtils.logSecurityEvent(player, "WRENCH_ACTION", blockPos,
                "Action: " + msg.action() + ", Hand: " + msg.hand());
            ItemStack stack = player.getItemInHand(msg.hand());
            if (!(stack.getItem() instanceof SwitchesWrenchItem wrench)) {
                return;
            }
            if (!(level.getBlockEntity(blockPos) instanceof SwitchBlockEntity blockEntity)) {
                return;
            }
            switch (msg.action()) {
                case COPY -> handleCopyAction(wrench, stack, blockEntity, player);
                case PASTE -> handlePasteAction(wrench, stack, blockEntity, player, blockPos);
            }
            player.inventoryMenu.broadcastChanges();
        });
        ctx.get().setPacketHandled(true);
    }

    @SuppressWarnings("unused") // Parameters kept for API consistency
    private static void handleCopyAction(SwitchesWrenchItem wrench, ItemStack stack,
                                       SwitchBlockEntity blockEntity, ServerPlayer player) {
        NetworkHandler.sendActionBarMessage(player, "Use Copy GUI for copying settings", NetworkHandler.MessageType.INFO);
    }

    private static void handlePasteAction(SwitchesWrenchItem wrench, ItemStack stack,
                                        SwitchBlockEntity blockEntity, ServerPlayer player, BlockPos blockPos) {
        if (!wrench.hasCopiedSettingsServer(stack)) {
            return;
        }
        // Check if target block has identical settings
        if (wrench.hasIdenticalSettingsServer(stack, blockEntity)) {
            Level level = player.level();
            net.minecraft.world.level.block.state.BlockState blockState = level.getBlockState(blockPos);
            net.minecraft.world.item.ItemStack blockItem = new net.minecraft.world.item.ItemStack(blockState.getBlock());
            String blockName = blockItem.getDisplayName().getString();
            NetworkHandler.sendActionBarMessage(player, blockName + " Already Has the Same Texture Settings", NetworkHandler.MessageType.INFO);
            return;
        }
        // Check if target block already has custom settings
        if (blockEntity.hasCustomTextures()) {
            openOverwriteConfirmationGUI(player, blockEntity.getBlockPos());
            return;
        }
        // Apply settings directly
        CopyPasteService.PasteResult result = wrench.applySettingsFromWrenchServer(stack, blockEntity, player);
        // Check if missing blocks GUI should be shown
        if (!result.success && "SHOW_MISSING_BLOCK_GUI".equals(result.message)) {
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
            @SuppressWarnings("NullableProblems") // MenuProvider interface contract
            @javax.annotation.Nullable
            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId,
                                                                                 net.minecraft.world.entity.player.Inventory playerInventory,
                                                                                 net.minecraft.world.entity.player.Player player) {
                return new net.justsomeswitches.gui.WrenchOverwriteMenu(containerId, playerInventory, blockPos);
            }
        };

        // Open the menu with block position data
        net.minecraftforge.network.NetworkHooks.openScreen(player, menuProvider, buf -> buf.writeBlockPos(blockPos));
    }

}
