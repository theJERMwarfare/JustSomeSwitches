package net.justsomeswitches.network;

import net.justsomeswitches.JustSomeSwitchesMod;
import net.minecraft.core.BlockPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Network handler for Just Some Switches mod
 * Registers and manages network packets for client-server communication
 */
@Mod.EventBusSubscriber(modid = JustSomeSwitchesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class NetworkHandler {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkHandler.class);
    
    // Network protocol version
    private static final String PROTOCOL_VERSION = "1";
    
    @SubscribeEvent
    public static void onRegisterPayloadHandler(RegisterPayloadHandlerEvent event) {
        final IPayloadRegistrar registrar = event.registrar(JustSomeSwitchesMod.MODID)
            .versioned(PROTOCOL_VERSION);
        
        LOGGER.info("Registering network packets for Just Some Switches...");
        
        // Register texture variable update packet (client -> server)
        registrar.play(TextureVariableUpdatePayload.ID, 
            TextureVariableUpdatePayload::new,
            handler -> handler.server(TextureVariableUpdatePayload::handle)
        );
        
        LOGGER.info("Network packets registered successfully");
    }
    
    /**
     * Send a texture variable update packet to the server
     * Called from client-side GUI when dropdown selections are made
     */
    public static void sendTextureVariableUpdate(BlockPos blockPos, 
                                                String category, 
                                                String variable, 
                                                String texturePath) {
        
        TextureVariableUpdatePayload payload = new TextureVariableUpdatePayload(blockPos, category, variable, texturePath);
        
        LOGGER.debug("[NETWORK] Sending texture update packet: {} category '{}' with texture '{}'", 
            category, variable, texturePath);
        
        // Send to server
        PacketDistributor.SERVER.noArg().send(payload);
    }
}
