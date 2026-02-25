package net.justsomeswitches.block;

import net.justsomeswitches.util.TightSwitchShapes.SwitchModelType;

/** Marker interface for all advanced switch blocks with block entity support. */
public interface ISwitchBlock {
    /** Returns the switch model type for shape lookup and variant identification. */
    SwitchModelType getSwitchModelType();
}
