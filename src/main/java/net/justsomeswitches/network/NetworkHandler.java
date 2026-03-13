package net.justsomeswitches.network;

import net.justsomeswitches.JustSomeSwitchesMod;
import net.justsomeswitches.util.WrenchConstants;
import net.justsomeswitches.util.SecurityUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import javax.annotation.Nonnull;

/** Network handler with security validation and packet registration. */
public class NetworkHandler {
    private static final String PROTOCOL_VERSION = WrenchConstants.NETWORK_PROTOCOL_VERSION;
    private static int packetId = 0;
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(JustSomeSwitchesMod.MODID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );
    /** Registers all network packets. Called during mod setup. */
    public static void registerPackets() {
        CHANNEL.registerMessage(packetId++, TextureVariableUpdatePayload.class,
            TextureVariableUpdatePayload::encode, TextureVariableUpdatePayload::decode, TextureVariableUpdatePayload::handle);
        CHANNEL.registerMessage(packetId++, WrenchActionPayload.class,
            WrenchActionPayload::encode, WrenchActionPayload::decode, WrenchActionPayload::handle);
        CHANNEL.registerMessage(packetId++, WrenchCopySelectionPayload.class,
            WrenchCopySelectionPayload::encode, WrenchCopySelectionPayload::decode, WrenchCopySelectionPayload::handle);
        CHANNEL.registerMessage(packetId++, WrenchOverwritePayload.class,
            WrenchOverwritePayload::encode, WrenchOverwritePayload::decode, WrenchOverwritePayload::handle);
        CHANNEL.registerMessage(packetId++, WrenchCopyOverwritePayload.class,
            WrenchCopyOverwritePayload::encode, WrenchCopyOverwritePayload::decode, WrenchCopyOverwritePayload::handle);
        CHANNEL.registerMessage(packetId++, WrenchMissingBlockPayload.class,
            WrenchMissingBlockPayload::encode, WrenchMissingBlockPayload::decode, WrenchMissingBlockPayload::handle);
        CHANNEL.registerMessage(packetId++, BatchUpdateControlPayload.class,
            BatchUpdateControlPayload::encode, BatchUpdateControlPayload::decode, BatchUpdateControlPayload::handle);
    }
    /** Sends texture variable update packet with security validation. */
    public static void sendTextureVariableUpdate(@Nonnull BlockPos blockPos,
                                                @Nonnull String category,
                                                @Nonnull String variable,
                                                @Nonnull String texturePath) {
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
        boolean needsTexturePath = category.equals("base") || category.equals("toggle");
        if (needsTexturePath && !SecurityUtils.isValidTexturePath(texturePath)) {
            JustSomeSwitchesMod.LOGGER.warn("Client attempted to send packet with invalid texture path: {}", texturePath);
            return;
        }
        int estimatedSize = blockPos.toString().length() + category.length() + variable.length() + texturePath.length() + 64;
        if (!SecurityUtils.isValidPacketSize(estimatedSize)) {
            JustSomeSwitchesMod.LOGGER.warn("Client attempted to send oversized packet: {} bytes", estimatedSize);
            return;
        }
        CHANNEL.sendToServer(new TextureVariableUpdatePayload(blockPos, category, variable, texturePath));
    }
    /** Sends wrench action packet with security validation. */
    public static void sendWrenchAction(@Nonnull BlockPos blockPos,
                                      @Nonnull WrenchActionPayload.WrenchAction action,
                                      @Nonnull net.minecraft.world.InteractionHand hand) {
        if (!SecurityUtils.isValidBlockPosition(blockPos)) {
            JustSomeSwitchesMod.LOGGER.warn("Client attempted to send wrench action with invalid block position: {}", blockPos);
            return;
        }
        CHANNEL.sendToServer(new WrenchActionPayload(blockPos, action, hand));
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
        if (!SecurityUtils.isValidBlockPosition(blockPos)) {
            JustSomeSwitchesMod.LOGGER.warn("Client attempted to send copy selection with invalid block position: {}", blockPos);
            return;
        }
        CHANNEL.sendToServer(new WrenchCopySelectionPayload(
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
            @SuppressWarnings("NullableProblems")
            @javax.annotation.Nullable
            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId,
                                                                                 net.minecraft.world.entity.player.Inventory playerInventory,
                                                                                 net.minecraft.world.entity.player.Player player) {
                return new net.justsomeswitches.gui.WrenchMissingBlockMenu(containerId, playerInventory, blockPos, missingBlocks);
            }
        };
        net.minecraftforge.network.NetworkHooks.openScreen(player, menuProvider, buf -> {
            buf.writeBlockPos(blockPos);
            buf.writeInt(missingBlocks.size());
            for (String missingBlock : missingBlocks) {
                buf.writeUtf(missingBlock);
            }
        });
    }
    /** Sends wrench overwrite response to server. */
    public static void sendWrenchOverwrite(@Nonnull BlockPos blockPos, boolean overwrite) {
        CHANNEL.sendToServer(new WrenchOverwritePayload(blockPos, overwrite));
    }
    /** Sends wrench copy overwrite response to server. */
    public static void sendWrenchCopyOverwrite(@Nonnull BlockPos blockPos, boolean overwrite) {
        CHANNEL.sendToServer(new WrenchCopyOverwritePayload(blockPos, overwrite));
    }
    /** Sends wrench missing block response to server. */
    public static void sendWrenchMissingBlock(@Nonnull BlockPos blockPos, boolean apply) {
        CHANNEL.sendToServer(new WrenchMissingBlockPayload(blockPos, apply));
    }
    /** Sends batch update control packet to server. */
    public static void sendBatchUpdateControl(@Nonnull BlockPos blockPos, boolean startBatch) {
        if (!SecurityUtils.isValidBlockPosition(blockPos)) {
            JustSomeSwitchesMod.LOGGER.warn("Client attempted to send batch control with invalid block position: {}", blockPos);
            return;
        }
        CHANNEL.sendToServer(new BatchUpdateControlPayload(blockPos, startBatch));
    }
}
