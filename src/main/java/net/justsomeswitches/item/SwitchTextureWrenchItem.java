package net.justsomeswitches.item;

import net.justsomeswitches.gui.SwitchTextureMenu;
import net.justsomeswitches.block.SwitchesLeverBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Custom texture wrench item for switch texture customization
 * ---
 * Phase 3B Enhancement: Added block position integration for BlockEntity connection
 */
public class SwitchTextureWrenchItem extends Item {

    public SwitchTextureWrenchItem(@Nonnull Properties properties) {
        super(properties);
    }

    @Override
    @Nonnull
    public InteractionResult useOn(@Nonnull UseOnContext context) {
        Level level = context.getLevel();
        BlockPos blockPos = context.getClickedPos();
        Player player = context.getPlayer();
        BlockState blockState = level.getBlockState(blockPos);
        Block block = blockState.getBlock();

        if (player == null) {
            return InteractionResult.FAIL;
        }

        // Check if player is sneaking
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        // Check if it's a switch block
        if (!isSwitchBlock(block)) {
            return InteractionResult.PASS;
        }

        // Open GUI on server side with block position data
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            openTextureCustomizationGUI(serverPlayer, blockPos);
            return InteractionResult.SUCCESS;
        }

        return level.isClientSide ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    private boolean isSwitchBlock(@Nonnull Block block) {
        return block instanceof SwitchesLeverBlock;
        // TODO: Add other switch types when implemented:
        // || block instanceof SwitchesRockerBlock
        // || block instanceof SwitchesButtonBlock
        // || block instanceof SwitchesSlideBlock
    }

    /**
     * Opens the texture customization GUI with block position integration
     * ---
     * Phase 3B Enhancement: Now passes block position for BlockEntity connection
     */
    private void openTextureCustomizationGUI(@Nonnull ServerPlayer player, @Nonnull BlockPos blockPos) {
        // Create a MenuProvider that includes block position data
        MenuProvider menuProvider = new MenuProvider() {
            @Override
            @Nonnull
            public Component getDisplayName() {
                return Component.translatable("gui.justsomeswitches.switch_texture.title");
            }

            @Override
            @Nullable
            public AbstractContainerMenu createMenu(int containerId, @Nonnull Inventory playerInventory, @Nonnull Player player) {
                return new SwitchTextureMenu(containerId, playerInventory, blockPos);
            }
        };

        // Open the menu with block position data
        player.openMenu(menuProvider, buf -> buf.writeBlockPos(blockPos));
    }
}