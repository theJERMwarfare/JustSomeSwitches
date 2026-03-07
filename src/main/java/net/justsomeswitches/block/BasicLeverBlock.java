package net.justsomeswitches.block;

/**
 * Simplified lever switch without texture customization.
 * Uses traditional lever-style 3D model distinct from rocker/button/slide variants.
 * No block entity overhead for better performance.
 */
public class BasicLeverBlock extends BasicSwitchBlock {
    public BasicLeverBlock(Properties properties) {
        super(properties);
    }
}
