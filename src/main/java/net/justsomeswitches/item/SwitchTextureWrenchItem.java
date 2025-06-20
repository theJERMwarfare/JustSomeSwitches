package net.justsomeswitches.item;

import net.justsomeswitches.gui.SwitchTextureMenu;
import net.justsomeswitches.block.SwitchesLeverBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;

/**
 * Custom texture wrench item for switch texture customization
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

        // Open GUI on server side
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            openTextureCustomizationGUI(serverPlayer);
            return InteractionResult.SUCCESS;
        }

        return level.isClientSide ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    private boolean isSwitchBlock(@Nonnull Block block) {
        return block instanceof SwitchesLeverBlock;
    }

    private void openTextureCustomizationGUI(@Nonnull ServerPlayer player) {
        SimpleMenuProvider menuProvider = new SimpleMenuProvider(
                (containerId, playerInventory, serverPlayer) ->
                        new SwitchTextureMenu(containerId, playerInventory),
                Component.translatable("gui.justsomeswitches.switch_texture.title")
        );

        player.openMenu(menuProvider);
    }
}