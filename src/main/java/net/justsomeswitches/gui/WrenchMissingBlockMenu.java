package net.justsomeswitches.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Menu for the Missing Block GUI
 * Displays when player is missing blocks needed for paste operation
 */
public class WrenchMissingBlockMenu extends AbstractContainerMenu {
    
    private final BlockPos blockPos;
    private final List<String> missingBlocks;
    
    /**
     * Constructor for server-side menu creation
     */
    public WrenchMissingBlockMenu(int containerId, Inventory playerInventory, BlockPos blockPos, List<String> missingBlocks) {
        super(JustSomeSwitchesMenuTypes.WRENCH_MISSING_BLOCK.get(), containerId);
        this.blockPos = blockPos;
        this.missingBlocks = missingBlocks;
    }
    
    /**
     * Constructor for client-side menu creation (from network data)
     */
    public WrenchMissingBlockMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        super(JustSomeSwitchesMenuTypes.WRENCH_MISSING_BLOCK.get(), containerId);
        this.blockPos = extraData.readBlockPos();
        
        // Read missing blocks list from network data
        int count = extraData.readInt();
        this.missingBlocks = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            this.missingBlocks.add(extraData.readUtf());
        }
    }
    
    public BlockPos getBlockPos() {
        return blockPos;
    }
    
    public List<String> getMissingBlocks() {
        return missingBlocks;
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
