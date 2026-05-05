package net.justsomeswitches.client;

import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.client.ConfigScreenHandler;

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
