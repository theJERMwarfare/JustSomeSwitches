package net.justsomeswitches.block;

/**
 * Lever switch with inverted visual appearance (up when OFF, down when ON).
 * Redstone behavior remains standard - emits signal strength 15 when powered.
 * Visual inversion handled purely by model files.
 */
public class BasicLeverInvertedBlock extends BasicSwitchBlock {
    public BasicLeverInvertedBlock(Properties properties) {
        super(properties);
    }
}
