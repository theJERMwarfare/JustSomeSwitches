package net.justsomeswitches.network;

import net.justsomeswitches.blockentity.SwitchesLeverBlockEntity;
import net.justsomeswitches.item.SwitchesWrenchItem;
import net.justsomeswitches.item.service.CopyPasteService;
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

/**
 * Network payload for Switches Wrench copy/paste operations
 * Handles client->server communication for copy and paste actions
 */
public record WrenchActionPayload(
    BlockPos blockPos,
    WrenchAction action,
    InteractionHand hand
) implements CustomPacketPayload {
    
    public static final ResourceLocation ID = new ResourceLocation("justsomeswitches", "wrench_action");
    
    public WrenchActionPayload(FriendlyByteBuf buf) {
        this(
            buf.readBlockPos(),
            buf.readEnum(WrenchAction.class),
            buf.readEnum(InteractionHand.class)
        );
    }
    
    public enum WrenchAction {
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
    
    /**
     * Handles the payload on the server side
     */
    public static void handle(WrenchActionPayload payload, PlayPayloadContext context) {
        context.workHandler().submitAsync(() -> {
            ServerPlayer player = (ServerPlayer) context.player().orElse(null);
            if (player == null) return;
            
            Level level = player.level();
            BlockPos blockPos = payload.blockPos();
            ItemStack stack = player.getItemInHand(payload.hand());
            
            // Verify the item is still a wrench
            if (!(stack.getItem() instanceof SwitchesWrenchItem wrench)) {
                return;
            }
            
            // Verify the block is still a switch
            if (!(level.getBlockEntity(blockPos) instanceof SwitchesLeverBlockEntity blockEntity)) {
                return;
            }
            
            // Perform the action
            switch (payload.action()) {
                case COPY -> handleCopyAction(wrench, stack, blockEntity, player);
                case PASTE -> handlePasteAction(wrench, stack, blockEntity, player, blockPos);
            }
            
            // Mark player inventory as dirty to sync changes
            player.inventoryMenu.broadcastChanges();
        });
    }
    
    private static void handleCopyAction(SwitchesWrenchItem wrench, ItemStack stack, 
                                       SwitchesLeverBlockEntity blockEntity, ServerPlayer player) {
        // COPY operations should go through the Copy GUI system, not this payload
        // This payload is only for simple PASTE operations from the client
        NetworkHandler.sendActionBarMessage(player, "Use Copy GUI for copying settings", NetworkHandler.MessageType.INFO);
    }
    
    private static void handlePasteAction(SwitchesWrenchItem wrench, ItemStack stack,
                                        SwitchesLeverBlockEntity blockEntity, ServerPlayer player, BlockPos blockPos) {
        if (!wrench.hasCopiedSettingsServer(stack)) {
            return; // No settings to paste, do nothing
        }
        
        // Check if target block has identical settings to what's being pasted
        if (wrench.hasIdenticalSettingsServer(stack, blockEntity)) {
            // Get the block item name for the message
            Level level = player.level();
            net.minecraft.world.level.block.state.BlockState blockState = level.getBlockState(blockPos);
            net.minecraft.world.item.ItemStack blockItem = new net.minecraft.world.item.ItemStack(blockState.getBlock());
            String blockName = blockItem.getDisplayName().getString();
            
            NetworkHandler.sendActionBarMessage(player, blockName + " Already Has the Same Texture Settings", NetworkHandler.MessageType.INFO);
            return;
        }
        
        // Check if target block already has custom settings
        if (blockEntity.hasCustomTextures()) {
            // Show overwrite confirmation GUI
            openOverwriteConfirmationGUI(player, blockEntity.getBlockPos());
            return;
        }
        
        // Target has no custom settings - apply directly
        CopyPasteService.PasteResult result = wrench.applySettingsFromWrenchServer(stack, blockEntity, player);
        
        // Check if missing blocks GUI should be shown
        if (!result.success && "SHOW_MISSING_BLOCK_GUI".equals(result.message)) {
            openMissingBlockGUI(player, blockPos, result.missingBlocks);
            return;
        }
        
        // Send appropriate action bar message based on result
        if (result.success) {
            NetworkHandler.sendActionBarMessage(player, result.message, NetworkHandler.MessageType.SUCCESS);
        } else {
            NetworkHandler.sendActionBarMessage(player, result.message, NetworkHandler.MessageType.ERROR);
        }
    }
    
    /**
     * Opens the missing block GUI for the player
     */
    private static void openMissingBlockGUI(ServerPlayer player, BlockPos blockPos, java.util.List<String> missingBlocks) {
        net.minecraft.world.MenuProvider menuProvider = new net.minecraft.world.MenuProvider() {
            @Override
            @javax.annotation.Nonnull
            public net.minecraft.network.chat.Component getDisplayName() {
                String title = missingBlocks.size() == 1 ? "Block Not Found In Inventory" : "Blocks Not Found In Inventory";
                return net.minecraft.network.chat.Component.literal(title);
            }

            @Override
            @javax.annotation.Nullable
            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId, 
                                                                                 net.minecraft.world.entity.player.Inventory playerInventory, 
                                                                                 net.minecraft.world.entity.player.Player player) {
                return new net.justsomeswitches.gui.WrenchMissingBlockMenu(containerId, playerInventory, blockPos, missingBlocks);
            }
        };

        // Open the menu with block position and missing blocks data
        player.openMenu(menuProvider, buf -> {
            buf.writeBlockPos(blockPos);
            buf.writeInt(missingBlocks.size());
            for (String missingBlock : missingBlocks) {
                buf.writeUtf(missingBlock);
            }
        });
    }
    
    /**
     * Opens the overwrite confirmation GUI for the player
     */
    private static void openOverwriteConfirmationGUI(ServerPlayer player, BlockPos blockPos) {
        net.minecraft.world.MenuProvider menuProvider = new net.minecraft.world.MenuProvider() {
            @Override
            @javax.annotation.Nonnull
            public net.minecraft.network.chat.Component getDisplayName() {
                return net.minecraft.network.chat.Component.literal("Settings Already Stored");
            }

            @Override
            @javax.annotation.Nullable
            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId, 
                                                                                 net.minecraft.world.entity.player.Inventory playerInventory, 
                                                                                 net.minecraft.world.entity.player.Player player) {
                return new net.justsomeswitches.gui.WrenchOverwriteMenu(containerId, playerInventory, blockPos);
            }
        };

        // Open the menu with block position data
        player.openMenu(menuProvider, buf -> buf.writeBlockPos(blockPos));
    }

}
