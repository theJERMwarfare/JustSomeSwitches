package net.justsomeswitches.client;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.client.ConfigScreenHandler;

/** Client-only config screen registration. Isolated in a separate class to keep client references out of the main mod class bytecode. */
public class ClientConfigHelper {
    public static void registerConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(
            ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory(
                (minecraft, screen) -> new SwitchesConfigScreen(screen)
            )
        );
    }
}
