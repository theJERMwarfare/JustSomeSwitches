package net.justsomeswitches.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

/** Container menu for brush overwrite confirmation GUI. */
public class BrushOverwriteMenu extends AbstractContainerMenu {
    
    private final BlockPos blockPos;
    
    public BrushOverwriteMenu(int containerId, @SuppressWarnings("unused") Inventory playerInventory, BlockPos blockPos) {
        super(JustSomeSwitchesMenuTypes.BRUSH_OVERWRITE.get(), containerId);
        this.blockPos = blockPos;
    }
    
    @SuppressWarnings("unused") // Required for network registration
    public BrushOverwriteMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readBlockPos());
    }
    
    @Nonnull
    public BlockPos getBlockPos() {
        return blockPos;
    }
    
    @Override
    @Nonnull
    public ItemStack quickMoveStack(@Nonnull Player player, int index) {
        return ItemStack.EMPTY;
    }
    
    @Override
    public boolean stillValid(@Nonnull Player player) {
        return true;
    }
}
