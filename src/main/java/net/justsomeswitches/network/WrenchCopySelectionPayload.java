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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;

import javax.annotation.Nonnull;

/**
 * Network payload for wrench copy selection operations
 * Handles selective copying based on GUI checkbox selections
 */
public record WrenchCopySelectionPayload(
    BlockPos blockPos,
    boolean copyToggleBlock,
    boolean copyToggleFace,
    boolean copyToggleRotation,
    boolean copyIndicators,
    boolean copyBaseBlock,
    boolean copyBaseFace,
    boolean copyBaseRotation
) implements CustomPacketPayload {
    
    public static final ResourceLocation ID = new ResourceLocation("justsomeswitches", "wrench_copy_selection");
    
    public WrenchCopySelectionPayload(FriendlyByteBuf buf) {
        this(
            buf.readBlockPos(),
            buf.readBoolean(),
            buf.readBoolean(),
            buf.readBoolean(),
            buf.readBoolean(),
            buf.readBoolean(),
            buf.readBoolean(),
            buf.readBoolean()
        );
    }
    
    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(blockPos);
        buf.writeBoolean(copyToggleBlock);
        buf.writeBoolean(copyToggleFace);
        buf.writeBoolean(copyToggleRotation);
        buf.writeBoolean(copyIndicators);
        buf.writeBoolean(copyBaseBlock);
        buf.writeBoolean(copyBaseFace);
        buf.writeBoolean(copyBaseRotation);
    }
    
    @Override
    @Nonnull
    public ResourceLocation id() {
        return ID;
    }
    
    /**
     * Handle copy selection on server side
     */
    public static void handle(WrenchCopySelectionPayload payload, PlayPayloadContext context) {
        context.workHandler().submitAsync(() -> {
            ServerPlayer player = (ServerPlayer) context.player().orElse(null);
            if (player == null) {
                return;
            }
            
            @SuppressWarnings("resource")
            Level level = player.level();
            BlockEntity blockEntity = level.getBlockEntity(payload.blockPos());
            
            if (!(blockEntity instanceof SwitchesLeverBlockEntity switchEntity)) {
                return;
            }
            
            // Find wrench in player's hands
            ItemStack mainHandStack = player.getMainHandItem();
            ItemStack offHandStack = player.getOffhandItem();
            ItemStack wrenchStack = null;
            
            if (mainHandStack.getItem() instanceof SwitchesWrenchItem) {
                wrenchStack = mainHandStack;
            } else if (offHandStack.getItem() instanceof SwitchesWrenchItem) {
                wrenchStack = offHandStack;
            }
            
            if (wrenchStack == null || !(wrenchStack.getItem() instanceof SwitchesWrenchItem wrench)) {
                return;
            }
            
            // Perform selective copying based on user selections
            wrench.copySelectedSettingsToWrench(wrenchStack, switchEntity,
                payload.copyToggleBlock(), payload.copyToggleFace(), payload.copyToggleRotation(),
                payload.copyIndicators(), payload.copyBaseBlock(), payload.copyBaseFace(),
                payload.copyBaseRotation());
            
            // Send success feedback to player
            NetworkHandler.sendActionBarMessage(player, "Texture Settings Copied Successfully", NetworkHandler.MessageType.SUCCESS);
            
            // Mark inventory as dirty to sync changes
            player.inventoryMenu.broadcastChanges();
        });
    }
}
