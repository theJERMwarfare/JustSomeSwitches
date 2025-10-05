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

/** Custom slot for texture blocks accepting only solid full-cube blocks. */
public class TextureSlot extends Slot {

    public TextureSlot(@Nonnull Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    @Override
    public boolean mayPlace(@Nonnull ItemStack itemStack) {
        if (!(itemStack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }

        Block block = blockItem.getBlock();

        BlockState defaultState = block.defaultBlockState();

        if (!isFullBlock(defaultState)) {
            return false;
        }

        if (!defaultState.canOcclude()) {
            return false;
        }

        try {
            //noinspection DataFlowIssue - Intentional null for redstone connectivity check
            if (defaultState.canRedstoneConnectTo(null, null, Direction.NORTH) ||
                    defaultState.hasAnalogOutputSignal()) {
                return false;
            }
        } catch (Exception e) {
            // Intentionally ignore - some blocks don't support redstone checks
        }

        String blockName = block.getDescriptionId().toLowerCase();
        return !blockName.contains("stairs") &&
                !blockName.contains("slab") &&
                !blockName.contains("fence") &&
                !blockName.contains("gate") &&
                !blockName.contains("door") &&
                !blockName.contains("trapdoor") &&
                !blockName.contains("button") &&
                !blockName.contains("lever") &&
                !blockName.contains("pressure") &&
                !blockName.contains("redstone") &&
                !blockName.contains("torch") &&
                !blockName.contains("rail") &&
                !blockName.contains("chest") &&
                !blockName.contains("furnace") &&
                !blockName.contains("dispenser") &&
                !blockName.contains("dropper");
    }

    /** Checks if block has a full cube shape. */
    private boolean isFullBlock(@Nonnull BlockState state) {
        try {
            //noinspection DataFlowIssue - Intentional null for shape check without world context
            VoxelShape shape = state.getShape(null, null);

            var bounds = shape.bounds();
            return bounds.minX <= 0.001 && bounds.minY <= 0.001 && bounds.minZ <= 0.001 &&
                    bounds.maxX >= 0.999 && bounds.maxY >= 0.999 && bounds.maxZ >= 0.999;
        } catch (Exception e) {
            // Intentionally ignore - some blocks require world context for shape
            return false;
        }
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public void setChanged() {
        super.setChanged();
    }

    @Override
    @Nonnull
    public ItemStack remove(int amount) {
        return super.remove(amount);
    }

    @Override
    public void set(@Nonnull ItemStack stack) {
        super.set(stack);
    }
}