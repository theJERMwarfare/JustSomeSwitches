package net.justsomeswitches.network;

import net.justsomeswitches.blockentity.SwitchBlockEntity;
import net.justsomeswitches.item.SwitchTextureBrushItem;
import net.justsomeswitches.item.service.CopyPasteService;
import net.justsomeswitches.util.SecurityUtils;
import net.justsomeswitches.util.BrushConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;

import javax.annotation.Nonnull;

/** Network payload for Switches Brush copy/paste operations. */
public record BrushActionPayload(
    BlockPos blockPos,
    BrushAction action,
    InteractionHand hand
) implements CustomPacketPayload {
    
    public static final ResourceLocation ID = new ResourceLocation("justsomeswitches", "wrench_action");
    
    public BrushActionPayload(FriendlyByteBuf buf) {
        this(
            buf.readBlockPos(),
            buf.readEnum(BrushAction.class),
            buf.readEnum(InteractionHand.class)
        );
    }
    
    public enum BrushAction {
        COPY,
        PASTE
    }
    
    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(blockPos);
        buf.writeEnum(action);
        buf.writeEnum(hand);
    }
    
    @Override
    @Nonnull
    public ResourceLocation id() {
        return ID;
    }
    
    /** Handles the payload on the server side. */
    public static void handle(BrushActionPayload payload, PlayPayloadContext context) {
        context.workHandler().submitAsync(() -> {
            ServerPlayer player = SecurityUtils.validateAndGetSender(context, payload.blockPos(), "BrushAction");
            if (player == null) return;
            Level level = player.level();
            BlockPos blockPos = payload.blockPos();
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
        });
    }
    
    @SuppressWarnings("unused") // Parameters kept for API consistency
    private static void handleCopyAction(SwitchTextureBrushItem brush, ItemStack stack, 
                                       SwitchBlockEntity blockEntity, ServerPlayer player) {
        NetworkHandler.sendActionBarMessage(player, "Use Copy GUI for copying settings", NetworkHandler.MessageType.INFO);
    }
    
    private static void handlePasteAction(SwitchTextureBrushItem brush, ItemStack stack,
                                        SwitchBlockEntity blockEntity, ServerPlayer player, BlockPos blockPos) {
        if (!brush.hasCopiedSettingsServer(stack)) {
            return;
        }
        // Check if target block has identical settings
        if (brush.hasIdenticalSettingsServer(stack, blockEntity)) {
            Level level = player.level();
            net.minecraft.world.level.block.state.BlockState blockState = level.getBlockState(blockPos);
            net.minecraft.world.item.ItemStack blockItem = new net.minecraft.world.item.ItemStack(blockState.getBlock());
            String blockName = blockItem.getDisplayName().getString();
            NetworkHandler.sendActionBarMessage(player, blockName + " Already Has the Same Texture Settings", NetworkHandler.MessageType.INFO);
            return;
        }
        // Check if target block already has custom settings
        if (blockEntity.hasCustomTextures()) {
            NetworkHandler.openOverwriteConfirmationGUI(player, blockEntity.getBlockPos());
            return;
        }
        // Apply settings directly
        CopyPasteService.PasteResult result = brush.applySettingsFromBrushServer(stack, blockEntity, player);
        // Check if missing blocks GUI should be shown
        if (!result.success && BrushConstants.MSG_MISSING_BLOCKS_GUI.equals(result.message)) {
            NetworkHandler.openMissingBlockGUI(player, blockPos, result.missingBlocks);
            return;
        }
        if (result.success) {
            NetworkHandler.sendActionBarMessage(player, result.message, NetworkHandler.MessageType.SUCCESS);
        } else {
            NetworkHandler.sendActionBarMessage(player, result.message, NetworkHandler.MessageType.ERROR);
        }
    }
    
}
