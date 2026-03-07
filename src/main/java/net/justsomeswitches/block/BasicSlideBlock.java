package net.justsomeswitches.block;

/**
 * Simplified sliding switch without texture customization.
 * Uses sliding toggle 3D model distinct from lever/rocker/button variants.
 * No block entity overhead for better performance.
 */
public class BasicSlideBlock extends BasicSwitchBlock {
    public BasicSlideBlock(Properties properties) {
        super(properties);
    }
}
