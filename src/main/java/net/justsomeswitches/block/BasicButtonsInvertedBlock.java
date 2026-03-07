package net.justsomeswitches.block;

/**
 * Dual-button switch with inverted visual appearance (bottom pressed when OFF, top when ON).
 * Redstone behavior remains standard - emits signal strength 15 when powered.
 * Visual inversion handled purely by model files.
 */
public class BasicButtonsInvertedBlock extends BasicSwitchBlock {
    public BasicButtonsInvertedBlock(Properties properties) {
        super(properties);
    }
}
