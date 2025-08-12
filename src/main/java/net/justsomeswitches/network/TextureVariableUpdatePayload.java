package net.justsomeswitches.network;

import net.justsomeswitches.JustSomeSwitchesMod;
import net.justsomeswitches.blockentity.SwitchesLeverBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TextureVariableUpdatePayload.class);
    
    public static final ResourceLocation ID = new ResourceLocation(JustSomeSwitchesMod.MODID, "texture_variable_update");
    
    public TextureVariableUpdatePayload(FriendlyByteBuf buf) {
        this(
            buf.readBlockPos(),
            buf.readUtf(),
            buf.readUtf(),
            buf.readUtf()
        );
    }
    
    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(blockPos);
        buf.writeUtf(category);
        buf.writeUtf(variable);
        buf.writeUtf(texturePath);
    }
    
    @Override
    @Nonnull
    public ResourceLocation id() {
        return ID;
    }
    
    public static void handle(TextureVariableUpdatePayload payload, PlayPayloadContext context) {
        context.workHandler().submitAsync(() -> {
            ServerPlayer player = (ServerPlayer) context.player().orElse(null);
            if (player == null) {
                LOGGER.warn("TextureVariableUpdatePayload: No player found");
                return;
            }
            
            Level level = player.level();
            BlockEntity blockEntity = level.getBlockEntity(payload.blockPos());
            
            if (!(blockEntity instanceof SwitchesLeverBlockEntity switchEntity)) {
                LOGGER.warn("TextureVariableUpdatePayload: BlockEntity not found or wrong type at {}", payload.blockPos());
                return;
            }
            
            LOGGER.debug("[NETWORK] Received texture update: {} category '{}' -> '{}' with texture '{}'", 
                payload.category(), getCurrentVariable(switchEntity, payload.category()), payload.variable(), payload.texturePath());
            
            // Update the server-side BlockEntity directly
            switch (payload.category()) {
                case "base" -> {
                    switchEntity.setBaseTextureVariable(payload.variable());
                    switchEntity.setBaseTexture(payload.texturePath());
                    LOGGER.debug("[NETWORK] Updated server-side base texture: {} -> {}", payload.variable(), payload.texturePath());
                }
                case "toggle" -> {
                    switchEntity.setToggleTextureVariable(payload.variable());
                    switchEntity.setToggleTexture(payload.texturePath());
                    LOGGER.debug("[NETWORK] Updated server-side toggle texture: {} -> {}", payload.variable(), payload.texturePath());
                }
                case "power" -> {
                    // Parse power mode from variable name
                    try {
                        SwitchesLeverBlockEntity.PowerMode powerMode = SwitchesLeverBlockEntity.PowerMode.valueOf(payload.variable().toUpperCase());
                        switchEntity.setPowerMode(powerMode);
                        LOGGER.debug("[NETWORK] Updated server-side power mode: {}", powerMode);
                    } catch (IllegalArgumentException e) {
                        LOGGER.warn("[NETWORK] Invalid power mode: {}", payload.variable());
                        return;
                    }
                }
                default -> {
                    LOGGER.warn("[NETWORK] Unknown texture category: {}", payload.category());
                    return;
                }
            }
            
            // Force save and sync to all clients
            switchEntity.setChanged();
            switchEntity.requestModelDataUpdate();
            level.sendBlockUpdated(payload.blockPos(), level.getBlockState(payload.blockPos()), level.getBlockState(payload.blockPos()), 3);
            
            LOGGER.debug("[NETWORK] Server-side texture update complete for {} category", payload.category());
        });
    }
    
    private static String getCurrentVariable(SwitchesLeverBlockEntity entity, String category) {
        return switch (category) {
            case "base" -> entity.getBaseTextureVariable();
            case "toggle" -> entity.getToggleTextureVariable();
            case "power" -> entity.getPowerMode().name().toLowerCase();
            default -> "unknown";
        };
    }
}
