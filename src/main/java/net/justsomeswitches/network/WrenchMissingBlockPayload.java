package net.justsomeswitches.network;

import net.justsomeswitches.blockentity.SwitchBlockEntity;
import net.justsomeswitches.item.SwitchesWrenchItem;
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
public record WrenchMissingBlockPayload(
    BlockPos blockPos,
    boolean apply
) implements CustomPacketPayload {
    
    public static final ResourceLocation ID = new ResourceLocation("justsomeswitches", "wrench_missing_block");
    
    public WrenchMissingBlockPayload(FriendlyByteBuf buf) {
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
    public static void handle(WrenchMissingBlockPayload payload, PlayPayloadContext context) {
        context.workHandler().submitAsync(() -> {
            ServerPlayer player = (ServerPlayer) context.player().orElse(null);
            if (player == null) return;
            
            // Security validation - Rate limiting
            if (SecurityUtils.isRateLimited(player)) {
                SecurityUtils.logSecurityViolation(player, "RATE_LIMIT_EXCEEDED", 
                    "WrenchMissingBlock packet rate limit exceeded");
                return;
            }
            
            // Security validation - Block position bounds
            if (!SecurityUtils.isValidBlockPosition(payload.blockPos())) {
                SecurityUtils.logSecurityViolation(player, "INVALID_COORDINATES", 
                    "Invalid block position: " + payload.blockPos());
                return;
            }
            
            Level level = player.level();
            BlockPos blockPos = payload.blockPos();
            
            // Security validation - Player permissions
            if (!SecurityUtils.canPlayerInteractWithBlock(player, level, blockPos)) {
                SecurityUtils.logSecurityViolation(player, "UNAUTHORIZED_ACCESS", 
                    "Player cannot interact with block at: " + blockPos);
                return;
            }
            
            // Log security event for audit
            SecurityUtils.logSecurityEvent(player, "WRENCH_MISSING_BLOCK", blockPos, 
                "Apply: " + payload.apply());
            
            // Find the wrench in player's hands
            ItemStack wrenchStack = null;
            
            if (player.getMainHandItem().getItem() instanceof SwitchesWrenchItem) {
                wrenchStack = player.getMainHandItem();
            } else if (player.getOffhandItem().getItem() instanceof SwitchesWrenchItem) {
                wrenchStack = player.getOffhandItem();
            }
            
            if (wrenchStack == null || !(wrenchStack.getItem() instanceof SwitchesWrenchItem wrench)) {
                return; // No wrench found
            }
            
            // Verify the block is still a switch
            if (!(level.getBlockEntity(blockPos) instanceof SwitchBlockEntity blockEntity)) {
                return;
            }
            
            if (payload.apply()) {
                // User chose to apply partial settings
                handlePartialApply(wrench, wrenchStack, blockEntity, player);
            } else {
                // User chose to cancel
                handleCancel(player);
            }
            
            // Mark player inventory as dirty to sync changes
            player.inventoryMenu.broadcastChanges();
        });
    }
    
    private static void handlePartialApply(SwitchesWrenchItem wrench, ItemStack wrenchStack,
                                         SwitchBlockEntity blockEntity, ServerPlayer player) {
        // Apply settings for categories that don't require missing blocks
        CopyPasteService.PasteResult result = wrench.applyPartialSettingsFromWrenchServer(wrenchStack, blockEntity, player);
        
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
