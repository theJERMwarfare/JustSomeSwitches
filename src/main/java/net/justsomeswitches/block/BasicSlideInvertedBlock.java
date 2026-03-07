package net.justsomeswitches.block;

/**
 * Sliding switch with inverted visual appearance (slides left when ON, right when OFF).
 * Redstone behavior remains standard - emits signal strength 15 when powered.
 * Visual inversion handled purely by model files.
 */
public class BasicSlideInvertedBlock extends BasicSwitchBlock {
    public BasicSlideInvertedBlock(Properties properties) {
        super(properties);
    }
}
