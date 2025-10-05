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
    
    /** Cached NBT tag access to avoid repeated getTag() calls. */
    public static class NBTCache {
        private final ItemStack stack;
        private CompoundTag cachedTag;
        private boolean tagCached = false;
        
        /** Creates NBT cache for the given ItemStack. */
        public NBTCache(@Nonnull ItemStack stack) {
            this.stack = stack;
        }
        
        /** Returns cached NBT tag or null if none exists. */
        @Nullable
        public CompoundTag getTag() {
            if (!tagCached) {
                cachedTag = stack.getTag();
                tagCached = true;
            }
            return cachedTag;
        }
        
        /** Returns nested compound tag for key or null if not found. */
        @Nullable
        public CompoundTag getCompound(@Nonnull String key) {
            CompoundTag tag = getTag();
            return (tag != null && tag.contains(key)) ? tag.getCompound(key) : null;
        }
        
        /** Returns boolean value for key or false if not found. */
        public boolean getBoolean(@Nonnull String key) {
            CompoundTag tag = getTag();
            return tag != null && tag.getBoolean(key);
        }
        
        /** Removes the specified key from the NBT tag. */
        public void remove(@Nonnull String key) {
            CompoundTag tag = getTag();
            if (tag != null) {
                tag.remove(key);
            }
        }
        
    }
    
    /** Applies multiple NBT operations in a single batch. */
    public static void batchNBTOperations(@Nonnull ItemStack stack, @Nonnull NBTOperation... operations) {
        if (operations.length == 0) return;
        
        CompoundTag tag = stack.getOrCreateTag();
        for (NBTOperation operation : operations) {
            operation.apply(tag);
        }
    }
    
    /** Functional interface for NBT operations. */
    @FunctionalInterface
    public interface NBTOperation {
        /** Applies operation to the given NBT tag. */
        void apply(@Nonnull CompoundTag tag);
    }
}
