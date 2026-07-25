package net.justsomeswitches.network;

import net.justsomeswitches.blockentity.SwitchBlockEntity;
import net.justsomeswitches.item.SwitchTextureBrushItem;
import net.justsomeswitches.item.service.CopyPasteService;
import net.justsomeswitches.util.SecurityUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;

import javax.annotation.Nonnull;

/**
 * Network payload for missing block GUI responses
 * Handles client->server communication when user chooses Apply or Cancel for missing blocks
 */
public record BrushMissingBlockPayload(
    BlockPos blockPos,
    boolean apply
) implements CustomPacketPayload {
    
    public static final ResourceLocation ID = new ResourceLocation("justsomeswitches", "wrench_missing_block");
    
    public BrushMissingBlockPayload(FriendlyByteBuf buf) {
        this(
            buf.readBlockPos(),
            buf.readBoolean()
        );
    }
    
    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(blockPos);
        buf.writeBoolean(apply);
    }
    
    @Override
    @Nonnull
    public ResourceLocation id() {
        return ID;
    }
    
    /**
     * Handles the payload on the server side
     */
    public static void handle(BrushMissingBlockPayload payload, PlayPayloadContext context) {
        context.workHandler().submitAsync(() -> {
            ServerPlayer player = SecurityUtils.validateAndGetSender(context, payload.blockPos(), "BrushMissingBlock");
            if (player == null) return;

            Level level = player.level();
            BlockPos blockPos = payload.blockPos();

            // Log security event for audit
            SecurityUtils.logSecurityEvent(player, "BRUSH_MISSING_BLOCK", blockPos, 
                "Apply: " + payload.apply());
            
            // Find the brush in player's hands
            ItemStack brushStack = SwitchTextureBrushItem.findBrushInHands(player);
            if (brushStack == null) {
                return; // No brush found
            }
            SwitchTextureBrushItem brush = (SwitchTextureBrushItem) brushStack.getItem();
            
            // Verify the block is still a switch
            if (!(level.getBlockEntity(blockPos) instanceof SwitchBlockEntity blockEntity)) {
                return;
            }
            
            if (payload.apply()) {
                // User chose to apply partial settings
                handlePartialApply(brush, brushStack, blockEntity, player);
            } else {
                // User chose to cancel
                handleCancel(player);
            }
            
            // Mark player inventory as dirty to sync changes
            player.inventoryMenu.broadcastChanges();
        });
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
