package net.justsomeswitches.gui;

import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nonnull;

/**
 * Custom slot for texture blocks that only accepts solid, full-cube blocks
 * ---
 * Phase 3B Enhancement: Comprehensive block validation with shape checking
 * FIXED: Proper change notification to trigger texture updates
 */
public class TextureSlot extends Slot {

    public TextureSlot(@Nonnull Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    @Override
    public boolean mayPlace(@Nonnull ItemStack itemStack) {
        // Only accept block items
        if (!(itemStack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }

        Block block = blockItem.getBlock();

        // Get the default block state for validation
        BlockState defaultState = block.defaultBlockState();

        // Check if it's a full block (no complex shapes)
        if (!isFullBlock(defaultState)) {
            return false;
        }

        // Reject transparent blocks
        if (!defaultState.canOcclude()) {
            return false;
        }

        // FIXED: Check redstone connection capability with proper parameters
        try {
            if (defaultState.canRedstoneConnectTo(null, null, Direction.NORTH) ||
                    defaultState.hasAnalogOutputSignal()) {
                return false;
            }
        } catch (Exception e) {
            // If redstone check fails, continue with other validation
        }

        // Reject blocks that have special behaviors
        String blockName = block.getDescriptionId().toLowerCase();
        if (blockName.contains("stairs") ||
                blockName.contains("slab") ||
                blockName.contains("fence") ||
                blockName.contains("gate") ||
                blockName.contains("door") ||
                blockName.contains("trapdoor") ||
                blockName.contains("button") ||
                blockName.contains("lever") ||
                blockName.contains("pressure") ||
                blockName.contains("redstone") ||
                blockName.contains("torch") ||
                blockName.contains("rail") ||
                blockName.contains("chest") ||
                blockName.contains("furnace") ||
                blockName.contains("dispenser") ||
                blockName.contains("dropper")) {
            return false;
        }

        return true;
    }

    /**
     * Check if block has a full cube shape
     */
    private boolean isFullBlock(@Nonnull BlockState state) {
        try {
            VoxelShape shape = state.getShape(null, null);

            // Check if the shape bounds match a full block (0,0,0 to 1,1,1)
            var bounds = shape.bounds();
            return bounds.minX <= 0.001 && bounds.minY <= 0.001 && bounds.minZ <= 0.001 &&
                    bounds.maxX >= 0.999 && bounds.maxY >= 0.999 && bounds.maxZ >= 0.999;
        } catch (Exception e) {
            // If we can't determine the shape, reject it to be safe
            return false;
        }
    }

    @Override
    public int getMaxStackSize() {
        return 1; // Only allow 1 block per slot
    }

    @Override
    public void setChanged() {
        super.setChanged();
        System.out.println("Phase 3C Debug: TextureSlot changed - slot " + getSlotIndex() + ", item: " + getItem());
    }

    @Override
    @Nonnull
    public ItemStack remove(int amount) {
        ItemStack result = super.remove(amount);
        System.out.println("Phase 3C Debug: TextureSlot item removed - slot " + getSlotIndex());
        return result;
    }

    @Override
    public void set(@Nonnull ItemStack stack) {
        super.set(stack);
        System.out.println("Phase 3C Debug: TextureSlot item set - slot " + getSlotIndex() + ", item: " + stack);
    }
}