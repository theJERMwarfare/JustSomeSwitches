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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;

import javax.annotation.Nonnull;

/** Network payload for brush overwrite confirmation responses. */
public record BrushOverwritePayload(
    BlockPos blockPos,
    boolean overwrite
) implements CustomPacketPayload {
    
    public static final ResourceLocation ID = new ResourceLocation("justsomeswitches", "wrench_overwrite");
    
    public BrushOverwritePayload(FriendlyByteBuf buf) {
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
    public static void handle(BrushOverwritePayload payload, PlayPayloadContext context) {
        context.workHandler().submitAsync(() -> {
            ServerPlayer player = SecurityUtils.validateAndGetSender(context, payload.blockPos(), "BrushOverwrite");
            if (player == null) return;

            Level level = player.level();
            BlockPos blockPos = payload.blockPos();

            SecurityUtils.logSecurityEvent(player, "BRUSH_OVERWRITE", blockPos,
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
                handleOverwriteConfirmed(brush, brushStack, blockEntity, player);
            } else {
                handleOverwriteCancelled(player);
            }
            

            player.inventoryMenu.broadcastChanges();
        });
    }
    
    private static void handleOverwriteConfirmed(SwitchTextureBrushItem brush, ItemStack brushStack,
                                               SwitchBlockEntity blockEntity, ServerPlayer player) {
        if (!blockEntity.getGuiToggleItem().isEmpty()) {
            if (!player.addItem(blockEntity.getGuiToggleItem().copy())) {
                player.drop(blockEntity.getGuiToggleItem().copy(), false);
            }
            blockEntity.setToggleSlotItem(ItemStack.EMPTY);
        }
        
        if (!blockEntity.getGuiBaseItem().isEmpty()) {
            if (!player.addItem(blockEntity.getGuiBaseItem().copy())) {
                player.drop(blockEntity.getGuiBaseItem().copy(), false);
            }
            blockEntity.setBaseSlotItem(ItemStack.EMPTY);
        }
        
        blockEntity.resetToggleTexture();
        blockEntity.resetBaseTexture();
        blockEntity.setPowerMode(SwitchBlockEntity.PowerMode.DEFAULT);
        blockEntity.setToggleTextureRotation(net.justsomeswitches.util.TextureRotation.NORMAL);
        blockEntity.setBaseTextureRotation(net.justsomeswitches.util.TextureRotation.NORMAL);
        
        blockEntity.updateTextures();
        
        NetworkHandler.sendActionBarMessage(player, "Previous Settings Removed Successfully", NetworkHandler.MessageType.SUCCESS);
        CopyPasteService.PasteResult inventoryCheck = brush.checkInventoryForPasteServer(brushStack, player);
        
        if (!inventoryCheck.success && BrushConstants.MSG_MISSING_BLOCKS_GUI.equals(inventoryCheck.message)) {
            NetworkHandler.openMissingBlockGUI(player, blockEntity.getBlockPos(), inventoryCheck.missingBlocks);
            return;
        }
        
        CopyPasteService.PasteResult result = brush.applySettingsFromBrushServer(brushStack, blockEntity, player);
        
        if (result.success) {
            NetworkHandler.sendActionBarMessage(player, result.message, NetworkHandler.MessageType.SUCCESS);
        } else {
            NetworkHandler.sendActionBarMessage(player, result.message, NetworkHandler.MessageType.ERROR);
        }
    }
    
    private static void handleOverwriteCancelled(ServerPlayer player) {

        NetworkHandler.sendActionBarMessage(player, "New Texture Settings Not Pasted", NetworkHandler.MessageType.INFO);
    }
}
