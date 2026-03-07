package net.justsomeswitches.block;

/**
 * Simplified rocker switch without texture customization.
 * Uses flat rocker-style 3D model distinct from lever/button/slide variants.
 * No block entity overhead for better performance.
 */
public class BasicRockerBlock extends BasicSwitchBlock {
    public BasicRockerBlock(Properties properties) {
        super(properties);
    }
}
