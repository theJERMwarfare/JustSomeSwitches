package net.justsomeswitches.network;

import net.justsomeswitches.blockentity.SwitchBlockEntity;
import net.justsomeswitches.item.SwitchTextureBrushItem;
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

/** Network payload for brush copy overwrite confirmation responses. */
public record BrushCopyOverwritePayload(
    BlockPos blockPos,
    boolean overwrite
) implements CustomPacketPayload {
    
    public static final ResourceLocation ID = new ResourceLocation("justsomeswitches", "wrench_copy_overwrite");
    
    public BrushCopyOverwritePayload(FriendlyByteBuf buf) {
        this(
            buf.readBlockPos(),
            buf.readBoolean()
        );
    }
    
    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(blockPos);
        buf.writeBoolean(overwrite);
    }
    
    @Override
    @Nonnull
    public ResourceLocation id() {
        return ID;
    }
    
    /** Handles the payload on server side. */
    public static void handle(BrushCopyOverwritePayload payload, PlayPayloadContext context) {
        context.workHandler().submitAsync(() -> {
            ServerPlayer player = SecurityUtils.validateAndGetSender(context, payload.blockPos(), "BrushCopyOverwrite");
            if (player == null) return;

            Level level = player.level();
            BlockPos blockPos = payload.blockPos();

            SecurityUtils.logSecurityEvent(player, "BRUSH_COPY_OVERWRITE", blockPos,
                "Overwrite: " + payload.overwrite());
            ItemStack brushStack = SwitchTextureBrushItem.findBrushInHands(player);
            if (brushStack == null) {
                return; // No brush found
            }
            SwitchTextureBrushItem brush = (SwitchTextureBrushItem) brushStack.getItem();
            

            if (!(level.getBlockEntity(blockPos) instanceof SwitchBlockEntity blockEntity)) {
                return;
            }
            
            if (payload.overwrite()) {
                handleCopyOverwriteConfirmed(brush, brushStack, blockEntity, player, blockPos);
            } else {
                handleCopyOverwriteCancelled(player);
            }
            

            player.inventoryMenu.broadcastChanges();
        });
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
