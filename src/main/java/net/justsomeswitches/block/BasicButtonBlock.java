package net.justsomeswitches.block;

/**
 * Simplified dual-button switch without texture customization.
 * Uses dual-button 3D model distinct from lever/rocker/slide variants.
 * No block entity overhead for better performance.
 */
public class BasicButtonBlock extends BasicSwitchBlock {
    public BasicButtonBlock(Properties properties) {
        super(properties);
    }
}
