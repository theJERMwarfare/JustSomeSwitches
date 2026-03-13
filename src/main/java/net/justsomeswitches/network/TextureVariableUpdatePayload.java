package net.justsomeswitches.network;

import net.justsomeswitches.blockentity.SwitchBlockEntity;
import net.justsomeswitches.util.TextureRotation;
import net.justsomeswitches.util.SecurityUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Network packet for syncing texture variable changes from client to server. */
public record TextureVariableUpdatePayload(
    BlockPos blockPos,
    String category,
    String variable,
    String texturePath
) {
    public static void encode(TextureVariableUpdatePayload msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.blockPos());
        buf.writeUtf(msg.category());
        buf.writeUtf(msg.variable());
        buf.writeUtf(msg.texturePath());
    }
    public static TextureVariableUpdatePayload decode(FriendlyByteBuf buf) {
        return new TextureVariableUpdatePayload(buf.readBlockPos(), buf.readUtf(), buf.readUtf(), buf.readUtf());
    }
    public static void handle(TextureVariableUpdatePayload msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            if (SecurityUtils.isRateLimited(player)) {
                SecurityUtils.logSecurityViolation(player, "RATE_LIMIT_EXCEEDED",
                    "TextureVariableUpdate packet rate limit exceeded");
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
            if (!SecurityUtils.isValidCategory(msg.category())) {
                SecurityUtils.logSecurityViolation(player, "INVALID_CATEGORY",
                    "Invalid category: " + msg.category());
                return;
            }
            if (!SecurityUtils.isValidString(msg.variable(), SecurityUtils.getMaxStringLength())) {
                SecurityUtils.logSecurityViolation(player, "INVALID_VARIABLE",
                    "Invalid variable string: " + msg.variable());
                return;
            }
            boolean needsTexturePath = msg.category().equals("base") || msg.category().equals("toggle");
            if (needsTexturePath && !SecurityUtils.isValidTexturePath(msg.texturePath())) {
                SecurityUtils.logSecurityViolation(player, "INVALID_TEXTURE_PATH",
                    "Invalid texture path: " + msg.texturePath());
                return;
            }
            SecurityUtils.logSecurityEvent(player, "TEXTURE_VARIABLE_UPDATE", msg.blockPos(),
                "Category: " + msg.category() + ", Variable: " + msg.variable());
            BlockEntity blockEntity = level.getBlockEntity(msg.blockPos());
            if (!(blockEntity instanceof SwitchBlockEntity switchEntity)) {
                return;
            }
            switch (msg.category()) {
                case "base" -> {
                    switchEntity.setBaseTextureVariable(msg.variable());
                    switchEntity.setBaseTexture(msg.texturePath());
                    switchEntity.updateTextures();
                }
                case "toggle" -> {
                    switchEntity.setToggleTextureVariable(msg.variable());
                    switchEntity.setToggleTexture(msg.texturePath());
                    switchEntity.updateTextures();
                }
                case "power" -> {
                    try {
                        SwitchBlockEntity.PowerMode powerMode = SwitchBlockEntity.PowerMode.valueOf(msg.variable().toUpperCase());
                        switchEntity.setPowerMode(powerMode);
                        switchEntity.updateTextures();
                    } catch (IllegalArgumentException e) {
                        // Invalid power mode, ignore
                    }
                }
                case "base_rotation" -> {
                    try {
                        TextureRotation rotation = TextureRotation.valueOf(msg.variable().toUpperCase());
                        switchEntity.setBaseTextureRotation(rotation);
                        switchEntity.updateTextures();
                    } catch (IllegalArgumentException e) {
                        // Invalid rotation, ignore
                    }
                }
                case "toggle_rotation" -> {
                    try {
                        TextureRotation rotation = TextureRotation.valueOf(msg.variable().toUpperCase());
                        switchEntity.setToggleTextureRotation(rotation);
                        switchEntity.updateTextures();
                    } catch (IllegalArgumentException e) {
                        // Invalid rotation, ignore
                    }
                }
                default -> {}
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
