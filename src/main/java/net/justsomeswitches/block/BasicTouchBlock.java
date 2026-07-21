package net.justsomeswitches.block;

/**
 * Simplified touch switch without texture customization.
 * No moving parts — powered state changes indicator texture only.
 * Uses touch 3D model distinct from lever/rocker/slide/button variants.
 */
public class BasicTouchBlock extends BasicSwitchBlock {
    public BasicTouchBlock(Properties properties) {
        super(properties);
    }
}
