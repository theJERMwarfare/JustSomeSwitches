package net.justsomeswitches.network;

import net.justsomeswitches.JustSomeSwitchesMod;
import net.justsomeswitches.blockentity.SwitchBlockEntity;
import net.justsomeswitches.util.TextureRotation;
import net.justsomeswitches.util.SecurityUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;

/**
 * Network payload for syncing texture variable changes from client to server
 * Handles dropdown selection persistence issue
 */
public record TextureVariableUpdatePayload(
    BlockPos blockPos,
    String category,
    String variable,
    String texturePath
) implements CustomPacketPayload {

    public static final Type<TextureVariableUpdatePayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(JustSomeSwitchesMod.MODID, "texture_variable_update"));
    public static final StreamCodec<FriendlyByteBuf, TextureVariableUpdatePayload> STREAM_CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC, TextureVariableUpdatePayload::blockPos,
            ByteBufCodecs.STRING_UTF8, TextureVariableUpdatePayload::category,
            ByteBufCodecs.STRING_UTF8, TextureVariableUpdatePayload::variable,
            ByteBufCodecs.STRING_UTF8, TextureVariableUpdatePayload::texturePath,
            TextureVariableUpdatePayload::new
        );
    @Override
    @Nonnull
    public Type<TextureVariableUpdatePayload> type() {
        return TYPE;
    }
    public static void handle(TextureVariableUpdatePayload payload, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        if (SecurityUtils.isRateLimited(player)) {
            SecurityUtils.logSecurityViolation(player, "RATE_LIMIT_EXCEEDED",
                "TextureVariableUpdate packet rate limit exceeded");
            return;
        }
        if (!SecurityUtils.isValidBlockPosition(payload.blockPos())) {
            SecurityUtils.logSecurityViolation(player, "INVALID_COORDINATES",
                "Invalid block position: " + payload.blockPos());
            return;
        }
        Level level = player.level();
        if (!SecurityUtils.canPlayerInteractWithBlock(player, level, payload.blockPos())) {
            SecurityUtils.logSecurityViolation(player, "UNAUTHORIZED_ACCESS",
                "Player cannot interact with block at: " + payload.blockPos());
            return;
        }
        if (!SecurityUtils.isValidCategory(payload.category())) {
            SecurityUtils.logSecurityViolation(player, "INVALID_CATEGORY",
                "Invalid category: " + payload.category());
            return;
        }
        if (!SecurityUtils.isValidString(payload.variable(), SecurityUtils.getMaxStringLength())) {
            SecurityUtils.logSecurityViolation(player, "INVALID_VARIABLE",
                "Invalid variable string: " + payload.variable());
            return;
        }
        // Only validate texture path for base and toggle categories
        boolean needsTexturePath = payload.category().equals("base") || payload.category().equals("toggle");
        if (needsTexturePath && !SecurityUtils.isValidTexturePath(payload.texturePath())) {
            SecurityUtils.logSecurityViolation(player, "INVALID_TEXTURE_PATH",
                "Invalid texture path: " + payload.texturePath());
            return;
        }
        SecurityUtils.logSecurityEvent(player, "TEXTURE_VARIABLE_UPDATE", payload.blockPos(),
            "Category: " + payload.category() + ", Variable: " + payload.variable());
        BlockEntity blockEntity = level.getBlockEntity(payload.blockPos());
        if (!(blockEntity instanceof SwitchBlockEntity switchEntity)) {
            return;
        }
        switch (payload.category()) {
            case "base" -> {
                switchEntity.setBaseTextureVariable(payload.variable());
                switchEntity.setBaseTexture(payload.texturePath());
                switchEntity.updateTextures();
            }
            case "toggle" -> {
                switchEntity.setToggleTextureVariable(payload.variable());
                switchEntity.setToggleTexture(payload.texturePath());
                switchEntity.updateTextures();
            }
            case "power" -> {
                try {
                    SwitchBlockEntity.PowerMode powerMode = SwitchBlockEntity.PowerMode.valueOf(payload.variable().toUpperCase());
                    switchEntity.setPowerMode(powerMode);
                    switchEntity.updateTextures();
                } catch (IllegalArgumentException e) {
                    // Invalid power mode, ignore
                }
            }
            case "base_rotation" -> {
                try {
                    TextureRotation rotation = TextureRotation.valueOf(payload.variable().toUpperCase());
                    switchEntity.setBaseTextureRotation(rotation);
                    switchEntity.updateTextures();
                } catch (IllegalArgumentException e) {
                    // Invalid rotation, ignore
                }
            }
            case "toggle_rotation" -> {
                try {
                    TextureRotation rotation = TextureRotation.valueOf(payload.variable().toUpperCase());
                    switchEntity.setToggleTextureRotation(rotation);
                    switchEntity.updateTextures();
                } catch (IllegalArgumentException e) {
                    // Invalid rotation, ignore
                }
            }
            default -> {}
        }
    }
}
