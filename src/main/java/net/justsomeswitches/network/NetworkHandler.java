package net.justsomeswitches.network;

import net.justsomeswitches.JustSomeSwitchesMod;
import net.justsomeswitches.util.SecurityUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import javax.annotation.Nonnull;

/** Network handler with security validation and packet registration. */
public class NetworkHandler {
    /** Bumping this breaks connection compatibility with un-updated clients/servers. */
    private static final String PROTOCOL_VERSION = "1";
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
        CHANNEL.registerMessage(packetId++, BrushActionPayload.class,
            BrushActionPayload::encode, BrushActionPayload::decode, BrushActionPayload::handle);
        CHANNEL.registerMessage(packetId++, BrushCopySelectionPayload.class,
            BrushCopySelectionPayload::encode, BrushCopySelectionPayload::decode, BrushCopySelectionPayload::handle);
        CHANNEL.registerMessage(packetId++, BrushOverwritePayload.class,
            BrushOverwritePayload::encode, BrushOverwritePayload::decode, BrushOverwritePayload::handle);
        CHANNEL.registerMessage(packetId++, BrushCopyOverwritePayload.class,
            BrushCopyOverwritePayload::encode, BrushCopyOverwritePayload::decode, BrushCopyOverwritePayload::handle);
        CHANNEL.registerMessage(packetId++, BrushMissingBlockPayload.class,
            BrushMissingBlockPayload::encode, BrushMissingBlockPayload::decode, BrushMissingBlockPayload::handle);
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
    /** Sends brush action packet with security validation. */
    public static void sendBrushAction(@Nonnull BlockPos blockPos,
                                      @Nonnull BrushActionPayload.BrushAction action,
                                      @Nonnull net.minecraft.world.InteractionHand hand) {
        if (!SecurityUtils.isValidBlockPosition(blockPos)) {
            JustSomeSwitchesMod.LOGGER.warn("Client attempted to send brush action with invalid block position: {}", blockPos);
            return;
        }
        CHANNEL.sendToServer(new BrushActionPayload(blockPos, action, hand));
    }
    /** Sends brush copy selection packet with security validation. */
    public static void sendBrushCopySelection(@Nonnull BlockPos blockPos,
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
        CHANNEL.sendToServer(new BrushCopySelectionPayload(
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
    /** Opens the missing block GUI for the player. Shared by BrushActionPayload and BrushOverwritePayload. */
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
                return new net.justsomeswitches.gui.BrushMissingBlockMenu(containerId, playerInventory, blockPos, missingBlocks);
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
    /** Opens the overwrite-confirmation GUI for the player (target block already has custom settings). */
    public static void openOverwriteConfirmationGUI(@Nonnull ServerPlayer player, @Nonnull BlockPos blockPos) {
        net.minecraft.world.MenuProvider menuProvider = new net.minecraft.world.MenuProvider() {
            @Override
            @Nonnull
            public net.minecraft.network.chat.Component getDisplayName() {
                return net.minecraft.network.chat.Component.literal("Settings Already Stored");
            }
            @Override
            @Nonnull
            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId,
                                                                                 @Nonnull net.minecraft.world.entity.player.Inventory playerInventory,
                                                                                 @Nonnull net.minecraft.world.entity.player.Player player) {
                return new net.justsomeswitches.gui.BrushOverwriteMenu(containerId, playerInventory, blockPos);
            }
        };
        net.minecraftforge.network.NetworkHooks.openScreen(player, menuProvider, buf -> buf.writeBlockPos(blockPos));
    }
    /** Opens the copy-texture-settings GUI for the player. */
    public static void openCopyTextureGUI(@Nonnull ServerPlayer player, @Nonnull BlockPos blockPos) {
        net.minecraft.world.MenuProvider menuProvider = new net.minecraft.world.MenuProvider() {
            @Override
            @Nonnull
            public net.minecraft.network.chat.Component getDisplayName() {
                return net.minecraft.network.chat.Component.literal("Copy Texture Settings");
            }
            @Override
            @Nonnull
            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int containerId,
                                                                                 @Nonnull net.minecraft.world.entity.player.Inventory playerInventory,
                                                                                 @Nonnull net.minecraft.world.entity.player.Player player) {
                return new net.justsomeswitches.gui.BrushCopyMenu(containerId, playerInventory, blockPos);
            }
        };
        net.minecraftforge.network.NetworkHooks.openScreen(player, menuProvider, buf -> buf.writeBlockPos(blockPos));
    }
    /** Sends brush overwrite response to server. */
    public static void sendBrushOverwrite(@Nonnull BlockPos blockPos, boolean overwrite) {
        CHANNEL.sendToServer(new BrushOverwritePayload(blockPos, overwrite));
    }
    /** Sends brush copy overwrite response to server. */
    public static void sendBrushCopyOverwrite(@Nonnull BlockPos blockPos, boolean overwrite) {
        CHANNEL.sendToServer(new BrushCopyOverwritePayload(blockPos, overwrite));
    }
    /** Sends brush missing block response to server. */
    public static void sendBrushMissingBlock(@Nonnull BlockPos blockPos, boolean apply) {
        CHANNEL.sendToServer(new BrushMissingBlockPayload(blockPos, apply));
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
