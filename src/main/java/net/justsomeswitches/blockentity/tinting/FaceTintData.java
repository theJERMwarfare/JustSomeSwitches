package net.justsomeswitches.blockentity.tinting;

import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nonnull;

/** Stores tinting information for a block face. */
public class FaceTintData {
    private int tintIndex;
    
    public FaceTintData() {
        this.tintIndex = -1;
    }
    
    public FaceTintData(int tintIndex) {
        this.tintIndex = tintIndex;
    }
    
    public int getTintIndex() { return tintIndex; }
    public void setTintIndex(int tintIndex) { this.tintIndex = tintIndex; }
    
    /** Saves tint data to NBT. */
    public void save(@Nonnull CompoundTag tag) {
        tag.putInt("TintIndex", this.tintIndex);
    }
    
    /** Loads tint data from NBT. */
    @Nonnull
    public static FaceTintData load(@Nonnull CompoundTag tag) {
        FaceTintData data = new FaceTintData();
        data.tintIndex = tag.getInt("TintIndex");
        return data;
    }
    
    @Nonnull
    public FaceTintData copy() {
        return new FaceTintData(this.tintIndex);
    }
}
