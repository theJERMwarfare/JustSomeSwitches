package net.justsomeswitches.network;

import net.justsomeswitches.blockentity.SwitchesLeverBlockEntity;
import net.justsomeswitches.item.SwitchesWrenchItem;
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
 * Network payload for wrench copy overwrite confirmation responses
 * Handles client->server communication when user chooses Overwrite or Cancel for copy operations
 */
public record WrenchCopyOverwritePayload(
    BlockPos blockPos,
    boolean overwrite
) implements CustomPacketPayload {
    
    public static final ResourceLocation ID = new ResourceLocation("justsomeswitches", "wrench_copy_overwrite");
    
    public WrenchCopyOverwritePayload(FriendlyByteBuf buf) {
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
    
    /**
     * Handles the payload on the server side
     */
    public static void handle(WrenchCopyOverwritePayload payload, PlayPayloadContext context) {
        context.workHandler().submitAsync(() -> {
            ServerPlayer player = (ServerPlayer) context.player().orElse(null);
            if (player == null) return;
            
            Level level = player.level();
            BlockPos blockPos = payload.blockPos();
            
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
            if (!(level.getBlockEntity(blockPos) instanceof SwitchesLeverBlockEntity blockEntity)) {
                return;
            }
            
            if (payload.overwrite()) {
                // User chose to overwrite - clear previous settings and open copy GUI
                handleCopyOverwriteConfirmed(wrench, wrenchStack, blockEntity, player, blockPos);
            } else {
                // User chose to cancel
                handleCopyOverwriteCancelled(player);
            }
            
            // Mark player inventory as dirty to sync changes
            player.inventoryMenu.broadcastChanges();
        });
    }
    
    private static void handleCopyOverwriteConfirmed(SwitchesWrenchItem wrench, ItemStack wrenchStack,
                                                   @SuppressWarnings("unused") SwitchesLeverBlockEntity blockEntity, ServerPlayer player, BlockPos blockPos) {
        // Step 1: Clear previous settings from wrench
        wrench.clearAllSettingsServer(wrenchStack);
        
        // Step 2: Send confirmation message
        NetworkHandler.sendActionBarMessage(player, "Previous Texture Settings Cleared", NetworkHandler.MessageType.SUCCESS);
        
        // Step 3: Open the copy GUI for the user to select what to copy
        openCopyTextureGUI(player, blockPos);
    }
    
    private static void handleCopyOverwriteCancelled(ServerPlayer player) {
        // Send cancellation message
        NetworkHandler.sendActionBarMessage(player, "New Texture Settings Not Copied, Previous Texture Settings Retained", NetworkHandler.MessageType.INFO);
    }
    
    /**
     * Opens the copy texture settings GUI with block position integration
     */
    private static void openCopyTextureGUI(ServerPlayer player, BlockPos blockPos) {
        // Create a MenuProvider for the copy GUI
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

        // Open the menu with block position data
        player.openMenu(menuProvider, buf -> buf.writeBlockPos(blockPos));
    }
}
