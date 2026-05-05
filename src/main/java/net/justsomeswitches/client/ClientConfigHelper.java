package net.justsomeswitches.client;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** Client-only config screen registration. Isolated in a separate class to keep client references out of the main mod class bytecode. */
public class ClientConfigHelper {
    public static void registerConfigScreen(ModContainer modContainer) {
        modContainer.registerExtensionPoint(IConfigScreenFactory.class,
            (container, screen) -> new SwitchesConfigScreen(screen));
    }
}
