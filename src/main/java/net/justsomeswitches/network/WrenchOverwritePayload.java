package net.justsomeswitches.network;

import net.justsomeswitches.blockentity.SwitchBlockEntity;
import net.justsomeswitches.item.SwitchesWrenchItem;
import net.justsomeswitches.item.service.CopyPasteService;
import net.justsomeswitches.util.SecurityUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;

/** Network payload for wrench overwrite confirmation responses. */
public record WrenchOverwritePayload(
    BlockPos blockPos,
    boolean overwrite
) implements CustomPacketPayload {

    public static final Type<WrenchOverwritePayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("justsomeswitches", "wrench_overwrite"));
    public static final StreamCodec<FriendlyByteBuf, WrenchOverwritePayload> STREAM_CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC, WrenchOverwritePayload::blockPos,
            ByteBufCodecs.BOOL, WrenchOverwritePayload::overwrite,
            WrenchOverwritePayload::new
        );
    @Override
    @Nonnull
    public Type<WrenchOverwritePayload> type() {
        return TYPE;
    }
    /** Handles the payload on server side. */
    public static void handle(WrenchOverwritePayload payload, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
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
        if (!(level.getBlockEntity(blockPos) instanceof SwitchBlockEntity blockEntity)) {
            return;
        }
        if (payload.overwrite()) {
            handleOverwriteConfirmed(wrench, wrenchStack, blockEntity, player);
        } else {
            handleOverwriteCancelled(player);
        }
        player.inventoryMenu.broadcastChanges();
    }
    private static void handleOverwriteConfirmed(SwitchesWrenchItem wrench, ItemStack wrenchStack,
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
        NetworkHandler.openMissingBlockGUI(player, blockPos, missingBlocks);
    }
}
