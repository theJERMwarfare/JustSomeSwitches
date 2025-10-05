package net.justsomeswitches.network;

import net.justsomeswitches.blockentity.SwitchesLeverBlockEntity;
import net.justsomeswitches.item.SwitchesWrenchItem;
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

/** Network payload for wrench copy overwrite confirmation responses. */
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
    
    /** Handles the payload on server side. */
    public static void handle(WrenchCopyOverwritePayload payload, PlayPayloadContext context) {
        context.workHandler().submitAsync(() -> {
            ServerPlayer player = (ServerPlayer) context.player().orElse(null);
            if (player == null) return;
            
            if (SecurityUtils.isRateLimited(player)) {
                SecurityUtils.logSecurityViolation(player, "RATE_LIMIT_EXCEEDED", 
                    "WrenchCopyOverwrite packet rate limit exceeded");
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
            
            SecurityUtils.logSecurityEvent(player, "WRENCH_COPY_OVERWRITE", blockPos, 
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
                handleCopyOverwriteConfirmed(wrench, wrenchStack, blockEntity, player, blockPos);
            } else {
                handleCopyOverwriteCancelled(player);
            }
            

            player.inventoryMenu.broadcastChanges();
        });
    }
    
    private static void handleCopyOverwriteConfirmed(SwitchesWrenchItem wrench, ItemStack wrenchStack,
                                                   @SuppressWarnings("unused") SwitchesLeverBlockEntity blockEntity, ServerPlayer player, BlockPos blockPos) {
        wrench.clearAllSettingsServer(wrenchStack);
        NetworkHandler.sendActionBarMessage(player, "Previous Texture Settings Cleared", NetworkHandler.MessageType.SUCCESS);
        openCopyTextureGUI(player, blockPos);
    }
    
    private static void handleCopyOverwriteCancelled(ServerPlayer player) {

        NetworkHandler.sendActionBarMessage(player, "New Texture Settings Not Copied, Previous Texture Settings Retained", NetworkHandler.MessageType.INFO);
    }
    
    /** Opens the copy texture settings GUI. */
    private static void openCopyTextureGUI(ServerPlayer player, BlockPos blockPos) {
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


        player.openMenu(menuProvider, buf -> buf.writeBlockPos(blockPos));
    }
}
