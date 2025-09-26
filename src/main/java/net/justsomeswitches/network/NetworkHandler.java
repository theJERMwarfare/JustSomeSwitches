package net.justsomeswitches.network;

import net.justsomeswitches.JustSomeSwitchesMod;
import net.justsomeswitches.util.WrenchConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;

import javax.annotation.Nonnull;


/**
 * Network handler for Just Some Switches mod
 * Registers and manages network packets for client-server communication
 */
@Mod.EventBusSubscriber(modid = JustSomeSwitchesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class NetworkHandler {
    

    
    // Using constant from WrenchConstants
    private static final String PROTOCOL_VERSION = WrenchConstants.NETWORK_PROTOCOL_VERSION;
    
    @SubscribeEvent
    public static void onRegisterPayloadHandler(RegisterPayloadHandlerEvent event) {
        final IPayloadRegistrar registrar = event.registrar(JustSomeSwitchesMod.MODID)
            .versioned(PROTOCOL_VERSION);
        

        
        // Register texture variable update packet (client -> server)
        registrar.play(TextureVariableUpdatePayload.ID, 
            TextureVariableUpdatePayload::new,
            handler -> handler.server(TextureVariableUpdatePayload::handle)
        );
        
        // Register wrench action packet (client -> server)
        registrar.play(WrenchActionPayload.ID, 
            WrenchActionPayload::new,
            handler -> handler.server(WrenchActionPayload::handle)
        );
        
        // Register wrench copy selection packet (client -> server)
        registrar.play(WrenchCopySelectionPayload.ID,
            WrenchCopySelectionPayload::new,
            handler -> handler.server(WrenchCopySelectionPayload::handle)
        );
        
        // Toast system removed - using action bar messages instead
        
        // Register wrench overwrite packet (client -> server)
        registrar.play(WrenchOverwritePayload.ID,
            WrenchOverwritePayload::new,
            handler -> handler.server(WrenchOverwritePayload::handle)
        );
        
        // Register wrench copy overwrite packet (client -> server)
        registrar.play(WrenchCopyOverwritePayload.ID,
            WrenchCopyOverwritePayload::new,
            handler -> handler.server(WrenchCopyOverwritePayload::handle)
        );
        
        // Register wrench missing block packet (client -> server)
        registrar.play(WrenchMissingBlockPayload.ID,
            WrenchMissingBlockPayload::new,
            handler -> handler.server(WrenchMissingBlockPayload::handle)
        );
        

    }
    
    /**
     * Send a texture variable update packet to the server
     * Optimized for better network performance
     */
    public static void sendTextureVariableUpdate(@Nonnull BlockPos blockPos, 
                                                @Nonnull String category, 
                                                @Nonnull String variable, 
                                                @Nonnull String texturePath) {
        PacketDistributor.SERVER.noArg().send(new TextureVariableUpdatePayload(blockPos, category, variable, texturePath));
    }
    
    /**
     * Send a wrench action packet to the server
     * Optimized for better network performance
     */
    public static void sendWrenchAction(@Nonnull BlockPos blockPos, 
                                      @Nonnull WrenchActionPayload.WrenchAction action, 
                                      @Nonnull net.minecraft.world.InteractionHand hand) {
        PacketDistributor.SERVER.noArg().send(new WrenchActionPayload(blockPos, action, hand));
    }
    
    /**
     * Send a wrench copy selection packet to the server
     * Optimized network call with reduced payload creation overhead
     */
    public static void sendWrenchCopySelection(@Nonnull BlockPos blockPos,
                                             boolean copyToggleBlock,
                                             boolean copyToggleFace,
                                             boolean copyToggleRotation,
                                             boolean copyIndicators,
                                             boolean copyBaseBlock,
                                             boolean copyBaseFace,
                                             boolean copyBaseRotation) {
        PacketDistributor.SERVER.noArg().send(new WrenchCopySelectionPayload(
            blockPos, copyToggleBlock, copyToggleFace, copyToggleRotation,
            copyIndicators, copyBaseBlock, copyBaseFace, copyBaseRotation
        ));
    }
    
    /**
     * Send an action bar message to a specific player
     * Optimized for direct server-side use
     */
    public static void sendActionBarMessage(@Nonnull ServerPlayer player, 
                                          @Nonnull String message, @Nonnull MessageType type) {
        net.minecraft.network.chat.Component styledMessage = formatActionBarMessage(message, type);
        player.displayClientMessage(styledMessage, true);
    }
    
    /**
     * Optimized action bar message formatting with constants
     */
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
    
    /**
     * Message types for action bar messages
     */
    public enum MessageType {
        SUCCESS,  // Green text
        ERROR,    // Red text  
        INFO      // Blue text
    }
    
    /**
     * Send a wrench overwrite response to the server (optimized)
     */
    public static void sendWrenchOverwrite(@Nonnull BlockPos blockPos, boolean overwrite) {
        PacketDistributor.SERVER.noArg().send(new WrenchOverwritePayload(blockPos, overwrite));
    }
    
    /**
     * Send a wrench copy overwrite response to the server (optimized)
     */
    public static void sendWrenchCopyOverwrite(@Nonnull BlockPos blockPos, boolean overwrite) {
        PacketDistributor.SERVER.noArg().send(new WrenchCopyOverwritePayload(blockPos, overwrite));
    }
    
    /**
     * Send a wrench missing block response to the server (optimized)
     */
    public static void sendWrenchMissingBlock(@Nonnull BlockPos blockPos, boolean apply) {
        PacketDistributor.SERVER.noArg().send(new WrenchMissingBlockPayload(blockPos, apply));
    }
}
