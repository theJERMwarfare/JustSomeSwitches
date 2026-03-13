package net.justsomeswitches.network;

import net.justsomeswitches.blockentity.SwitchBlockEntity;
import net.justsomeswitches.item.SwitchesWrenchItem;
import net.justsomeswitches.util.SecurityUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Network payload for wrench copy selection operations. */
public record WrenchCopySelectionPayload(
    BlockPos blockPos,
    boolean copyToggleBlock,
    boolean copyToggleFace,
    boolean copyToggleRotation,
    boolean copyIndicators,
    boolean copyBaseBlock,
    boolean copyBaseFace,
    boolean copyBaseRotation
) {

    public static void encode(WrenchCopySelectionPayload msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.blockPos());
        buf.writeBoolean(msg.copyToggleBlock());
        buf.writeBoolean(msg.copyToggleFace());
        buf.writeBoolean(msg.copyToggleRotation());
        buf.writeBoolean(msg.copyIndicators());
        buf.writeBoolean(msg.copyBaseBlock());
        buf.writeBoolean(msg.copyBaseFace());
        buf.writeBoolean(msg.copyBaseRotation());
    }

    public static WrenchCopySelectionPayload decode(FriendlyByteBuf buf) {
        return new WrenchCopySelectionPayload(
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
    public static void handle(WrenchCopySelectionPayload msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            if (SecurityUtils.isRateLimited(player)) {
                SecurityUtils.logSecurityViolation(player, "RATE_LIMIT_EXCEEDED",
                    "WrenchCopySelection packet rate limit exceeded");
                return;
            }
            if (!SecurityUtils.isValidBlockPosition(msg.blockPos())) {
                SecurityUtils.logSecurityViolation(player, "INVALID_COORDINATES",
                    "Invalid block position: " + msg.blockPos());
                return;
            }
            Level level = player.level();
            if (!SecurityUtils.canPlayerInteractWithBlock(player, level, msg.blockPos())) {
                SecurityUtils.logSecurityViolation(player, "UNAUTHORIZED_ACCESS",
                    "Player cannot interact with block at: " + msg.blockPos());
                return;
            }
            SecurityUtils.logSecurityEvent(player, "WRENCH_COPY_SELECTION", msg.blockPos(),
                String.format("Toggle: %b/%b/%b, Base: %b/%b/%b, Indicators: %b",
                    msg.copyToggleBlock(), msg.copyToggleFace(), msg.copyToggleRotation(),
                    msg.copyBaseBlock(), msg.copyBaseFace(), msg.copyBaseRotation(),
                    msg.copyIndicators()));
            BlockEntity blockEntity = level.getBlockEntity(msg.blockPos());
            if (!(blockEntity instanceof SwitchBlockEntity switchEntity)) {
                return;
            }
            ItemStack mainHandStack = player.getMainHandItem();
            ItemStack offHandStack = player.getOffhandItem();
            ItemStack wrenchStack = null;

            if (mainHandStack.getItem() instanceof SwitchesWrenchItem) {
                wrenchStack = mainHandStack;
            } else if (offHandStack.getItem() instanceof SwitchesWrenchItem) {
                wrenchStack = offHandStack;
            }
            if (wrenchStack == null) {
                return;
            }
            if (!(wrenchStack.getItem() instanceof SwitchesWrenchItem wrench)) {
                return;
            }
            wrench.copySelectedSettingsToWrench(wrenchStack, switchEntity,
                msg.copyToggleBlock(), msg.copyToggleFace(), msg.copyToggleRotation(),
                msg.copyIndicators(), msg.copyBaseBlock(), msg.copyBaseFace(),
                msg.copyBaseRotation());
            NetworkHandler.sendActionBarMessage(player, "Texture Settings Copied Successfully", NetworkHandler.MessageType.SUCCESS);
            player.inventoryMenu.broadcastChanges();
        });
        ctx.get().setPacketHandled(true);
    }
}
