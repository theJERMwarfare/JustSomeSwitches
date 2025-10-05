package net.justsomeswitches.network;

import net.justsomeswitches.blockentity.SwitchesLeverBlockEntity;
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

/** Network payload for wrench overwrite confirmation responses. */
public record WrenchOverwritePayload(
    BlockPos blockPos,
    boolean overwrite
) implements CustomPacketPayload {
    
    public static final ResourceLocation ID = new ResourceLocation("justsomeswitches", "wrench_overwrite");
    
    public WrenchOverwritePayload(FriendlyByteBuf buf) {
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
    public static void handle(WrenchOverwritePayload payload, PlayPayloadContext context) {
        context.workHandler().submitAsync(() -> {
            ServerPlayer player = (ServerPlayer) context.player().orElse(null);
            if (player == null) return;
            
            if (SecurityUtils.isRateLimited(player)) {
                SecurityUtils.logSecurityViolation(player, "RATE_LIMIT_EXCEEDED", 
                    "WrenchOverwrite packet rate limit exceeded");
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
            
            SecurityUtils.logSecurityEvent(player, "WRENCH_OVERWRITE", blockPos, 
                "Overwrite: " + payload.overwrite());
            ItemStack wrenchStack = null;
            
            if (player.getMainHandItem().getItem() instanceof SwitchesWrenchItem) {
                wrenchStack = player.getMainHandItem();
            } else if (player.getOffhandItem().getItem() instanceof SwitchesWrenchItem) {
                wrenchStack = player.getOffhandItem();
            }
            
            if (wrenchStack == null || !(wrenchStack.getItem() instanceof SwitchesWrenchItem wrench)) {
                return; // No wrench found
            }
            

            if (!(level.getBlockEntity(blockPos) instanceof SwitchesLeverBlockEntity blockEntity)) {
                return;
            }
            
            if (payload.overwrite()) {
                handleOverwriteConfirmed(wrench, wrenchStack, blockEntity, player);
            } else {
                handleOverwriteCancelled(player);
            }
            

            player.inventoryMenu.broadcastChanges();
        });
    }
    
    private static void handleOverwriteConfirmed(SwitchesWrenchItem wrench, ItemStack wrenchStack,
                                               SwitchesLeverBlockEntity blockEntity, ServerPlayer player) {
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
        blockEntity.setPowerMode(SwitchesLeverBlockEntity.PowerMode.DEFAULT);
        blockEntity.setToggleTextureRotation(net.justsomeswitches.util.TextureRotation.NORMAL);
        blockEntity.setBaseTextureRotation(net.justsomeswitches.util.TextureRotation.NORMAL);
        
        blockEntity.updateTextures();
        
        NetworkHandler.sendActionBarMessage(player, "Previous Settings Removed Successfully", NetworkHandler.MessageType.SUCCESS);
        CopyPasteService.PasteResult inventoryCheck = wrench.checkInventoryForPasteServer(wrenchStack, player);
        
        if (!inventoryCheck.success && "SHOW_MISSING_BLOCK_GUI".equals(inventoryCheck.message)) {
            openMissingBlockGUI(player, blockEntity.getBlockPos(), inventoryCheck.missingBlocks);
            return;
        }
        
        CopyPasteService.PasteResult result = wrench.applySettingsFromWrenchServer(wrenchStack, blockEntity, player);
        
        if (result.success) {
            NetworkHandler.sendActionBarMessage(player, result.message, NetworkHandler.MessageType.SUCCESS);
        } else {
            NetworkHandler.sendActionBarMessage(player, result.message, NetworkHandler.MessageType.ERROR);
        }
    }
    
    private static void handleOverwriteCancelled(ServerPlayer player) {

        NetworkHandler.sendActionBarMessage(player, "New Texture Settings Not Pasted", NetworkHandler.MessageType.INFO);
    }
    
    /** Opens the missing block GUI for the player. */
    private static void openMissingBlockGUI(ServerPlayer player, BlockPos blockPos, java.util.List<String> missingBlocks) {
        net.minecraft.world.MenuProvider menuProvider = new net.minecraft.world.MenuProvider() {
            @Override
            @javax.annotation.Nonnull
            public net.minecraft.network.chat.Component getDisplayName() {
                String title = missingBlocks.size() == 1 ? "Block Not Found In Inventory" : "Blocks Not Found In Inventory";
                return net.minecraft.network.chat.Component.literal(title);
            }

            @Override
            @javax.annotation.Nonnull
            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId, 
                                                                                 @javax.annotation.Nonnull net.minecraft.world.entity.player.Inventory playerInventory, 
                                                                                 @javax.annotation.Nonnull net.minecraft.world.entity.player.Player player) {
                return new net.justsomeswitches.gui.WrenchMissingBlockMenu(containerId, playerInventory, blockPos, missingBlocks);
            }
        };


        player.openMenu(menuProvider, buf -> {
            buf.writeBlockPos(blockPos);
            buf.writeInt(missingBlocks.size());
            for (String missingBlock : missingBlocks) {
                buf.writeUtf(missingBlock);
            }
        });
    }
}
