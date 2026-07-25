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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;

import javax.annotation.Nonnull;

/** Network payload for brush copy selection operations. */
public record BrushCopySelectionPayload(
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
    
    public BrushCopySelectionPayload(FriendlyByteBuf buf) {
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
    
    /** Handles copy selection on server side. */
    public static void handle(BrushCopySelectionPayload payload, PlayPayloadContext context) {
        context.workHandler().submitAsync(() -> {
            ServerPlayer player = SecurityUtils.validateAndGetSender(context, payload.blockPos(), "BrushCopySelection");
            if (player == null) {
                return;
            }
            Level level = player.level();
            SecurityUtils.logSecurityEvent(player, "BRUSH_COPY_SELECTION", payload.blockPos(),
                String.format("Toggle: %b/%b/%b, Base: %b/%b/%b, Indicators: %b", 
                    payload.copyToggleBlock(), payload.copyToggleFace(), payload.copyToggleRotation(),
                    payload.copyBaseBlock(), payload.copyBaseFace(), payload.copyBaseRotation(), 
                    payload.copyIndicators()));
            BlockEntity blockEntity = level.getBlockEntity(payload.blockPos());
            if (!(blockEntity instanceof SwitchBlockEntity switchEntity)) {
                return;
            }
            ItemStack brushStack = SwitchTextureBrushItem.findBrushInHands(player);
            if (brushStack == null) {
                return;
            }
            SwitchTextureBrushItem brush = (SwitchTextureBrushItem) brushStack.getItem();
            brush.copySelectedSettingsToBrush(brushStack, switchEntity,
                payload.copyToggleBlock(), payload.copyToggleFace(), payload.copyToggleRotation(),
                payload.copyIndicators(), payload.copyBaseBlock(), payload.copyBaseFace(),
                payload.copyBaseRotation());
            NetworkHandler.sendActionBarMessage(player, "Texture Settings Copied Successfully", NetworkHandler.MessageType.SUCCESS);
            player.inventoryMenu.broadcastChanges();
        });
    }
}
