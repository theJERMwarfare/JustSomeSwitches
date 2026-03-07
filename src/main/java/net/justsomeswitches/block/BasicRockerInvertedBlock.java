package net.justsomeswitches.block;

/**
 * Rocker switch with inverted visual appearance (tilts backward when ON, forward when OFF).
 * Redstone behavior remains standard - emits signal strength 15 when powered.
 * Visual inversion handled purely by model files.
 */
public class BasicRockerInvertedBlock extends BasicSwitchBlock {
    public BasicRockerInvertedBlock(Properties properties) {
        super(properties);
    }
}
