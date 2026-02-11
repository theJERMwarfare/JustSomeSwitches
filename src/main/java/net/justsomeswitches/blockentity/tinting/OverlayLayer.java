package net.justsomeswitches.blockentity.tinting;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;

/** Represents a single texture layer for overlay rendering. */
public class OverlayLayer {
    private final ResourceLocation sprite;
    private final int tintIndex;
    private final int order;
    
    public OverlayLayer(@Nonnull ResourceLocation sprite, int tintIndex, int order) {
        this.sprite = sprite;
        this.tintIndex = tintIndex;
        this.order = order;
    }
    
    @Nonnull
    public ResourceLocation getSprite() { return sprite; }
    
    public int getTintIndex() { return tintIndex; }
    
    public int getOrder() { return order; }
    
    /** Saves overlay layer data to NBT. */
    public void save(@Nonnull CompoundTag tag) {
        tag.putString("Sprite", sprite.toString());
        tag.putInt("TintIndex", tintIndex);
        tag.putInt("Order", order);
    }
    
    /** Loads overlay layer data from NBT. */
    @Nonnull
    public static OverlayLayer load(@Nonnull CompoundTag tag) {
        ResourceLocation sprite = new ResourceLocation(tag.getString("Sprite"));
        int tintIndex = tag.getInt("TintIndex");
        int order = tag.getInt("Order");
        return new OverlayLayer(sprite, tintIndex, order);
    }
}
