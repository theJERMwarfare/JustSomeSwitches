package net.justsomeswitches.gui;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nonnull;

/**
 * Custom slot that only accepts solid block items suitable for texture customization
 *---
 * Phase 3C Fix: Added static isValidTextureItem method for external validation
 * This slot is used in the Switch Texture GUI to ensure players can only
 * place solid, full-cube blocks that make good texture sources.
 */
public class TextureSlot extends Slot {

    /**
     * Constructor for the texture slot
     * @param container The container this slot belongs to
     * @param slotIndex The index of this slot in the container
     * @param xPosition The X position of the slot in the GUI
     * @param yPosition The Y position of the slot in the GUI
     */
    public TextureSlot(@Nonnull Container container, int slotIndex, int xPosition, int yPosition) {
        super(container, slotIndex, xPosition, yPosition);
    }

    /**
     * Checks if an item can be placed in this slot
     * Only allows solid, full-cube blocks that make good texture sources
     *
     * @param itemStack The item stack to check
     * @return True if the item can be placed in this slot
     */
    @Override
    public boolean mayPlace(@Nonnull ItemStack itemStack) {
        return isValidTextureItem(itemStack);
    }

    /**
     * Static method to check if an ItemStack is valid for texture customization
     * Only allows solid, full-cube blocks that make good texture sources
     * ---
     * Phase 3C Fix: Added static method for external validation calls
     *
     * @param itemStack The item stack to check
     * @return True if the item is valid for texture customization
     */
    public static boolean isValidTextureItem(@Nonnull ItemStack itemStack) {
        // Only allow block items
        if (!(itemStack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }

        Block block = blockItem.getBlock();

        try {
            var blockState = block.defaultBlockState();

            // Basic requirements: not air and can occlude
            if (blockState.isAir() || !blockState.canOcclude()) {
                return false;
            }

            // Check if it's a full cube block by comparing its shape to a full cube
            VoxelShape blockShape = blockState.getShape(null, null);
            VoxelShape fullCube = Shapes.block();

            // Only allow blocks that have the exact shape of a full cube
            // This excludes stairs, slabs, fences, etc.
            if (!blockShape.equals(fullCube)) {
                return false;
            }

            // Additional checks to exclude problematic blocks
            String blockName = block.toString().toLowerCase();

            // Exclude redstone components
            if (blockName.contains("redstone") || blockName.contains("repeater") ||
                    blockName.contains("comparator") || blockName.contains("observer")) {
                return false;
            }

            // Exclude complex blocks with GUIs or special functionality
            if (blockName.contains("furnace") || blockName.contains("chest") ||
                    blockName.contains("barrel") || blockName.contains("hopper") ||
                    blockName.contains("dispenser") || blockName.contains("dropper")) {
                return false;
            }

            // Exclude transparent blocks
            if (blockName.contains("glass") || blockName.contains("ice")) {
                return false;
            }

            // If it passes all checks, it's a good texture block
            return true;

        } catch (Exception e) {
            // If we can't determine the properties safely, reject it
            // This ensures we don't crash on unusual modded blocks
            return false;
        }
    }

    /**
     * Gets the maximum stack size for this slot
     * Texture slots should only hold 1 item for clarity
     *
     * @return The maximum stack size (always 1 for texture slots)
     */
    @Override
    public int getMaxStackSize() {
        return 1;
    }

    /**
     * Gets the maximum stack size for a specific item in this slot
     * Ensures only 1 block can be placed for texture reference
     *
     * @param itemStack The item stack to get the limit for
     * @return The maximum stack size for this item (always 1)
     */
    @Override
    public int getMaxStackSize(@Nonnull ItemStack itemStack) {
        return 1;
    }
}