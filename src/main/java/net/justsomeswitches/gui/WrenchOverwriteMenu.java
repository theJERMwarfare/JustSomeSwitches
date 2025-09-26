package net.justsomeswitches.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

/**
 * Container menu for wrench overwrite confirmation GUI
 * Handles the "Replace existing settings?" dialog when pasting to a switch with custom settings
 */
public class WrenchOverwriteMenu extends AbstractContainerMenu {
    
    private final BlockPos blockPos;
    
    public WrenchOverwriteMenu(int containerId, Inventory playerInventory, BlockPos blockPos) {
        super(JustSomeSwitchesMenuTypes.WRENCH_OVERWRITE.get(), containerId);
        this.blockPos = blockPos;
    }
    
    public WrenchOverwriteMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBlockPos());
    }
    
    @Nonnull
    public BlockPos getBlockPos() {
        return blockPos;
    }
    
    @Override
    @Nonnull
    public ItemStack quickMoveStack(@Nonnull Player player, int index) {
        return ItemStack.EMPTY; // No slots to move items between
    }
    
    @Override
    public boolean stillValid(@Nonnull Player player) {
        // Always valid since it's just a confirmation dialog
        return true;
    }
}
