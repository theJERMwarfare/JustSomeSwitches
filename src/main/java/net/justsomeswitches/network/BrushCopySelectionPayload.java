package net.justsomeswitches.network;

import net.justsomeswitches.blockentity.SwitchBlockEntity;
import net.justsomeswitches.item.SwitchTextureBrushItem;
import net.justsomeswitches.util.SecurityUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

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
) {

    public static void encode(BrushCopySelectionPayload msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.blockPos());
        buf.writeBoolean(msg.copyToggleBlock());
        buf.writeBoolean(msg.copyToggleFace());
        buf.writeBoolean(msg.copyToggleRotation());
        buf.writeBoolean(msg.copyIndicators());
        buf.writeBoolean(msg.copyBaseBlock());
        buf.writeBoolean(msg.copyBaseFace());
        buf.writeBoolean(msg.copyBaseRotation());
    }

    public static BrushCopySelectionPayload decode(FriendlyByteBuf buf) {
        return new BrushCopySelectionPayload(
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

    /** Handles copy selection on server side. */
    public static void handle(BrushCopySelectionPayload msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = SecurityUtils.validateAndGetSender(ctx.get(), msg.blockPos(), "BrushCopySelection");
            if (player == null) {
                return;
            }
            Level level = player.level();
            SecurityUtils.logSecurityEvent(player, "BRUSH_COPY_SELECTION", msg.blockPos(),
                String.format("Toggle: %b/%b/%b, Base: %b/%b/%b, Indicators: %b",
                    msg.copyToggleBlock(), msg.copyToggleFace(), msg.copyToggleRotation(),
                    msg.copyBaseBlock(), msg.copyBaseFace(), msg.copyBaseRotation(),
                    msg.copyIndicators()));
            BlockEntity blockEntity = level.getBlockEntity(msg.blockPos());
            if (!(blockEntity instanceof SwitchBlockEntity switchEntity)) {
                return;
            }
            ItemStack brushStack = SwitchTextureBrushItem.findBrushInHands(player);
            if (brushStack == null) {
                return;
            }
            SwitchTextureBrushItem brush = (SwitchTextureBrushItem) brushStack.getItem();
            brush.copySelectedSettingsToBrush(brushStack, switchEntity,
                msg.copyToggleBlock(), msg.copyToggleFace(), msg.copyToggleRotation(),
                msg.copyIndicators(), msg.copyBaseBlock(), msg.copyBaseFace(),
                msg.copyBaseRotation());
            NetworkHandler.sendActionBarMessage(player, "Texture Settings Copied Successfully", NetworkHandler.MessageType.SUCCESS);
            player.inventoryMenu.broadcastChanges();
        });
        ctx.get().setPacketHandled(true);
    }
}
