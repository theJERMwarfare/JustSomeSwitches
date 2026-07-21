package net.justsomeswitches.block;

/**
 * Touch switch with inverted physical orientation (indicator on the opposite side).
 * Redstone behavior remains standard - emits signal strength 15 when powered.
 * Visual inversion handled purely by model files.
 */
public class BasicTouchInvertedBlock extends BasicSwitchBlock {
    public BasicTouchInvertedBlock(Properties properties) {
        super(properties);
    }
}
