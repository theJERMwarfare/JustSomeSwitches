package net.justsomeswitches.network;

import net.justsomeswitches.blockentity.SwitchBlockEntity;
import net.justsomeswitches.item.SwitchTextureBrushItem;
import net.justsomeswitches.item.service.CopyPasteService;
import net.justsomeswitches.util.SecurityUtils;
import net.justsomeswitches.util.BrushConstants;
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
import java.util.List;

/** Network payload for brush overwrite confirmation responses. */
public record BrushOverwritePayload(
    BlockPos blockPos,
    boolean overwrite
) implements CustomPacketPayload {

    public static final Type<BrushOverwritePayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("justsomeswitches", "wrench_overwrite"));
    public static final StreamCodec<FriendlyByteBuf, BrushOverwritePayload> STREAM_CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC, BrushOverwritePayload::blockPos,
            ByteBufCodecs.BOOL, BrushOverwritePayload::overwrite,
            BrushOverwritePayload::new
        );
    @Override
    @Nonnull
    public Type<BrushOverwritePayload> type() {
        return TYPE;
    }
    /** Handles the payload on server side. */
    public static void handle(BrushOverwritePayload payload, IPayloadContext context) {
        ServerPlayer player = SecurityUtils.validateAndGetSender(context, payload.blockPos(), "BrushOverwrite");
        if (player == null) {
            return;
        }
        Level level = player.level();
        BlockPos blockPos = payload.blockPos();
        ItemStack brushStack = SwitchTextureBrushItem.findBrushInHands(player);
        if (brushStack == null || !(brushStack.getItem() instanceof SwitchTextureBrushItem brush)) {
            return; // No brush found
        }
        if (!(level.getBlockEntity(blockPos) instanceof SwitchBlockEntity blockEntity)) {
            return;
        }
        if (payload.overwrite()) {
            handleOverwriteConfirmed(brush, brushStack, blockEntity, player);
        } else {
            handleOverwriteCancelled(player);
        }
        player.inventoryMenu.broadcastChanges();
    }
    private static void handleOverwriteConfirmed(SwitchTextureBrushItem brush, ItemStack brushStack,
                                               SwitchBlockEntity blockEntity, ServerPlayer player) {
        // Check affordability BEFORE stripping. The switch's own stored blocks are handed back to the
        // player by this operation, so they count toward what the paste can be paid with.
        List<ItemStack> returnedToPlayer = List.of(blockEntity.getGuiToggleItem(), blockEntity.getGuiBaseItem());
        CopyPasteService.PasteResult inventoryCheck =
            brush.checkInventoryForPasteServer(brushStack, player, returnedToPlayer);
        if (!inventoryCheck.success && BrushConstants.MSG_MISSING_BLOCKS_GUI.equals(inventoryCheck.message)) {
            NetworkHandler.openMissingBlockGUI(player, blockEntity.getBlockPos(), inventoryCheck.missingBlocks);
            return;
        }
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
        NetworkHandler.sendActionBarMessage(player, net.justsomeswitches.util.BrushConstants.MSG_PREVIOUS_SETTINGS_REMOVED, NetworkHandler.MessageType.SUCCESS);
        CopyPasteService.PasteResult result = brush.applySettingsFromBrushServer(brushStack, blockEntity, player);
        if (result.success) {
            NetworkHandler.sendActionBarMessage(player, result.message, NetworkHandler.MessageType.SUCCESS);
        } else {
            NetworkHandler.sendActionBarMessage(player, result.message, NetworkHandler.MessageType.ERROR);
        }
    }
    private static void handleOverwriteCancelled(ServerPlayer player) {
        NetworkHandler.sendActionBarMessage(player, net.justsomeswitches.util.BrushConstants.MSG_SETTINGS_NOT_PASTED, NetworkHandler.MessageType.INFO);
    }
}
