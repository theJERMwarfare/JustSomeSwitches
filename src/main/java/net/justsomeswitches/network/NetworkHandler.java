package net.justsomeswitches.network;

import net.justsomeswitches.JustSomeSwitchesMod;
import net.justsomeswitches.util.SecurityUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import javax.annotation.Nonnull;


/** Network handler with security validation and packet registration. */
@EventBusSubscriber(modid = JustSomeSwitchesMod.MODID)
public class NetworkHandler {
    @SubscribeEvent
    public static void onRegisterPayloadHandler(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(JustSomeSwitchesMod.MODID);
        registrar.playToServer(TextureVariableUpdatePayload.TYPE,
            TextureVariableUpdatePayload.STREAM_CODEC, TextureVariableUpdatePayload::handle);
        registrar.playToServer(WrenchActionPayload.TYPE,
            WrenchActionPayload.STREAM_CODEC, WrenchActionPayload::handle);
        registrar.playToServer(WrenchCopySelectionPayload.TYPE,
            WrenchCopySelectionPayload.STREAM_CODEC, WrenchCopySelectionPayload::handle);
        registrar.playToServer(WrenchOverwritePayload.TYPE,
            WrenchOverwritePayload.STREAM_CODEC, WrenchOverwritePayload::handle);
        registrar.playToServer(WrenchCopyOverwritePayload.TYPE,
            WrenchCopyOverwritePayload.STREAM_CODEC, WrenchCopyOverwritePayload::handle);
        registrar.playToServer(WrenchMissingBlockPayload.TYPE,
            WrenchMissingBlockPayload.STREAM_CODEC, WrenchMissingBlockPayload::handle);
        registrar.playToServer(BatchUpdateControlPayload.TYPE,
            BatchUpdateControlPayload.STREAM_CODEC, BatchUpdateControlPayload::handle);
    }
    /** Sends texture variable update packet with security validation. */
    public static void sendTextureVariableUpdate(@Nonnull BlockPos blockPos,
                                                @Nonnull String category,
                                                @Nonnull String variable,
                                                @Nonnull String texturePath) {
        // Client-side pre-validation to reduce invalid packets
        if (!SecurityUtils.isValidBlockPosition(blockPos)) {
            JustSomeSwitchesMod.LOGGER.warn("Client attempted to send packet with invalid block position: {}", blockPos);
            return;
        }
        if (!SecurityUtils.isValidCategory(category)) {
            JustSomeSwitchesMod.LOGGER.warn("Client attempted to send packet with invalid category: {}", category);
            return;
        }
        if (!SecurityUtils.isValidString(variable, SecurityUtils.getMaxStringLength())) {
            JustSomeSwitchesMod.LOGGER.warn("Client attempted to send packet with invalid variable: {}", variable);
            return;
        }
        // Only validate texture path for base and toggle categories (not for power or rotation)
        boolean needsTexturePath = category.equals("base") || category.equals("toggle");
        if (needsTexturePath && !SecurityUtils.isValidTexturePath(texturePath)) {
            JustSomeSwitchesMod.LOGGER.warn("Client attempted to send packet with invalid texture path: {}", texturePath);
            return;
        }
        // Estimate packet size for validation
        int estimatedSize = blockPos.toString().length() + category.length() + variable.length() + texturePath.length() + 64; // overhead
        if (!SecurityUtils.isValidPacketSize(estimatedSize)) {
            JustSomeSwitchesMod.LOGGER.warn("Client attempted to send oversized packet: {} bytes", estimatedSize);
            return;
        }
        PacketDistributor.sendToServer(new TextureVariableUpdatePayload(blockPos, category, variable, texturePath));
    }
    /** Sends wrench action packet with security validation. */
    public static void sendWrenchAction(@Nonnull BlockPos blockPos,
                                      @Nonnull WrenchActionPayload.WrenchAction action,
                                      @Nonnull net.minecraft.world.InteractionHand hand) {
        // Client-side pre-validation
        if (!SecurityUtils.isValidBlockPosition(blockPos)) {
            JustSomeSwitchesMod.LOGGER.warn("Client attempted to send wrench action with invalid block position: {}", blockPos);
            return;
        }
        PacketDistributor.sendToServer(new WrenchActionPayload(blockPos, action, hand));
    }
    /** Sends wrench copy selection packet with security validation. */
    public static void sendWrenchCopySelection(@Nonnull BlockPos blockPos,
                                             boolean copyToggleBlock,
                                             boolean copyToggleFace,
                                             boolean copyToggleRotation,
                                             boolean copyIndicators,
                                             boolean copyBaseBlock,
                                             boolean copyBaseFace,
                                             boolean copyBaseRotation) {
        // Client-side pre-validation
        if (!SecurityUtils.isValidBlockPosition(blockPos)) {
            JustSomeSwitchesMod.LOGGER.warn("Client attempted to send copy selection with invalid block position: {}", blockPos);
            return;
        }
        PacketDistributor.sendToServer(new WrenchCopySelectionPayload(
            blockPos, copyToggleBlock, copyToggleFace, copyToggleRotation,
            copyIndicators, copyBaseBlock, copyBaseFace, copyBaseRotation
        ));
    }
    /** Sends action bar message to player. */
    public static void sendActionBarMessage(@Nonnull ServerPlayer player,
                                          @Nonnull String message, @Nonnull MessageType type) {
        net.minecraft.network.chat.Component styledMessage = formatActionBarMessage(message, type);
        player.displayClientMessage(styledMessage, true);
    }
    /** Formats action bar message with color based on type. */
    private static net.minecraft.network.chat.Component formatActionBarMessage(@Nonnull String message, @Nonnull MessageType type) {
        return switch (type) {
            case SUCCESS -> net.minecraft.network.chat.Component.literal(message)
                    .withStyle(net.minecraft.ChatFormatting.GREEN);
            case ERROR -> net.minecraft.network.chat.Component.literal(message)
                    .withStyle(net.minecraft.ChatFormatting.RED);
            case INFO -> net.minecraft.network.chat.Component.literal(message)
                    .withStyle(net.minecraft.ChatFormatting.BLUE);
        };
    }
    /** Message types for action bar messages. */
    public enum MessageType {
        SUCCESS,  // Green text
        ERROR,    // Red text
        INFO      // Blue text
    }
    /** Opens the missing block GUI for the player. Shared by WrenchActionPayload and WrenchOverwritePayload. */
    public static void openMissingBlockGUI(@Nonnull ServerPlayer player, @Nonnull BlockPos blockPos, @Nonnull java.util.List<String> missingBlocks) {
        net.minecraft.world.MenuProvider menuProvider = new net.minecraft.world.MenuProvider() {
            @Override
            @Nonnull
            public net.minecraft.network.chat.Component getDisplayName() {
                String title = missingBlocks.size() == 1 ? "Block Not Found In Inventory" : "Blocks Not Found In Inventory";
                return net.minecraft.network.chat.Component.literal(title);
            }
            @Override
            @Nonnull
            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId,
                                                                                 @Nonnull net.minecraft.world.entity.player.Inventory playerInventory,
                                                                                 @Nonnull net.minecraft.world.entity.player.Player player) {
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
    /** Sends wrench overwrite response to server. */
    public static void sendWrenchOverwrite(@Nonnull BlockPos blockPos, boolean overwrite) {
        PacketDistributor.sendToServer(new WrenchOverwritePayload(blockPos, overwrite));
    }
    /** Sends wrench copy overwrite response to server. */
    public static void sendWrenchCopyOverwrite(@Nonnull BlockPos blockPos, boolean overwrite) {
        PacketDistributor.sendToServer(new WrenchCopyOverwritePayload(blockPos, overwrite));
    }
    /** Sends wrench missing block response to server. */
    public static void sendWrenchMissingBlock(@Nonnull BlockPos blockPos, boolean apply) {
        PacketDistributor.sendToServer(new WrenchMissingBlockPayload(blockPos, apply));
    }
    /** Sends batch update control packet to server. */
    public static void sendBatchUpdateControl(@Nonnull BlockPos blockPos, boolean startBatch) {
        // Client-side pre-validation
        if (!SecurityUtils.isValidBlockPosition(blockPos)) {
            JustSomeSwitchesMod.LOGGER.warn("Client attempted to send batch control with invalid block position: {}", blockPos);
            return;
        }
        PacketDistributor.sendToServer(new BatchUpdateControlPayload(blockPos, startBatch));
    }
}
