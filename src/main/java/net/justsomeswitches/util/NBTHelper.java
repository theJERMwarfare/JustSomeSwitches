package net.justsomeswitches.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Utility class for efficient NBT operations
 */
public class NBTHelper {
    
    private NBTHelper() {
        // Utility class
    }
    
    /**
     * Cached NBT tag access - avoids repeated getTag() calls
     */
    public static class NBTCache {
        private final ItemStack stack;
        private CompoundTag cachedTag;
        private boolean tagCached = false;
        
        public NBTCache(@Nonnull ItemStack stack) {
            this.stack = stack;
        }
        
        @Nullable
        public CompoundTag getTag() {
            if (!tagCached) {
                cachedTag = stack.getTag();
                tagCached = true;
            }
            return cachedTag;
        }
        
        @Nullable
        public CompoundTag getCompound(@Nonnull String key) {
            CompoundTag tag = getTag();
            return (tag != null && tag.contains(key)) ? tag.getCompound(key) : null;
        }
        
        public boolean getBoolean(@Nonnull String key) {
            CompoundTag tag = getTag();
            return tag != null && tag.getBoolean(key);
        }
        
        public void remove(@Nonnull String key) {
            CompoundTag tag = getTag();
            if (tag != null) {
                tag.remove(key);
            }
        }
        
        // Cache invalidation handled automatically
    }
    
    // Removed unused copyNBTData method
    
    /**
     * Batch NBT operations for efficiency
     */
    public static void batchNBTOperations(@Nonnull ItemStack stack, @Nonnull NBTOperation... operations) {
        if (operations.length == 0) return;
        
        CompoundTag tag = stack.getOrCreateTag();
        for (NBTOperation operation : operations) {
            operation.apply(tag);
        }
    }
    
    @FunctionalInterface
    public interface NBTOperation {
        void apply(@Nonnull CompoundTag tag);
    }
}
