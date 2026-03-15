package net.justsomeswitches.item;

import net.justsomeswitches.block.AbstractSwitchBlock;
import net.justsomeswitches.block.ISwitchBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;

/** Block item for all advanced switch blocks with proper placement handling. */
public class SwitchBlockItem extends BlockItem {

    public SwitchBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    @Nonnull
    public InteractionResult useOn(@Nonnull UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        BlockState clickedState = level.getBlockState(clickedPos);

        // When clicking on any advanced switch block, check the block's response
        if (clickedState.getBlock() instanceof ISwitchBlock && context.getPlayer() != null) {
            net.minecraft.world.phys.BlockHitResult hitResult = new net.minecraft.world.phys.BlockHitResult(
                context.getClickLocation(), context.getClickedFace(), clickedPos, context.isInside());
            
            InteractionResult blockResult = clickedState.useWithoutItem(level, context.getPlayer(), hitResult);
            
            // If block returns PASS, proceed with item placement logic
            // If block returns SUCCESS/CONSUME, respect that and don't place
            if (blockResult != InteractionResult.PASS) {
                return blockResult;
            }
            // Continue to placement logic below when block returns PASS
        }

        Direction clickedFace = context.getClickedFace();
        BlockPos targetPos = context.getClickedPos().relative(clickedFace);
        if (!level.getBlockState(targetPos).canBeReplaced() && !level.getBlockState(targetPos).isAir()) {
            return InteractionResult.FAIL;
        }
        
        BlockPlaceContext placeContext = new BlockPlaceContext(context.getLevel(), context.getPlayer(), 
                context.getHand(), context.getItemInHand(), 
                new net.minecraft.world.phys.BlockHitResult(context.getClickLocation(), clickedFace, targetPos, context.isInside()));
        
        AbstractSwitchBlock switchBlock = (AbstractSwitchBlock) this.getBlock();
        BlockState blockState = switchBlock.getStateForPlacement(placeContext);
        if (blockState == null) {
            return InteractionResult.FAIL;
        }
        if (level.setBlock(targetPos, blockState, Block.UPDATE_ALL_IMMEDIATE)) {
            switchBlock.setPlacedBy(level, targetPos, blockState, context.getPlayer(), context.getItemInHand());
            
            if (context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
            
            return InteractionResult.sidedSuccess(level.isClientSide());
        } else {
            return InteractionResult.FAIL;
        }
    }
}