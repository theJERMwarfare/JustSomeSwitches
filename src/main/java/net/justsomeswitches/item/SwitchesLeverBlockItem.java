package net.justsomeswitches.item;

import net.justsomeswitches.block.SwitchesLeverBlock;
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

/**
 * Block item for switches lever with proper placement handling.
 */
public class SwitchesLeverBlockItem extends BlockItem {

    public SwitchesLeverBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    @Nonnull
    public InteractionResult useOn(@Nonnull UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        BlockState clickedState = level.getBlockState(clickedPos);

        if (clickedState.getBlock() instanceof SwitchesLeverBlock && context.getPlayer() != null) {
            net.minecraft.world.phys.BlockHitResult hitResult = new net.minecraft.world.phys.BlockHitResult(
                context.getClickLocation(), context.getClickedFace(), clickedPos, context.isInside());
            return clickedState.use(level, context.getPlayer(), context.getHand(), hitResult);
        }

        Direction clickedFace = context.getClickedFace();
        BlockPos targetPos;
        
        if (clickedFace == Direction.UP || clickedFace == Direction.DOWN) {
            targetPos = context.getClickedPos().relative(clickedFace);
        } else {
            targetPos = context.getClickedPos().relative(clickedFace);
        }
        if (!level.getBlockState(targetPos).canBeReplaced() && !level.getBlockState(targetPos).isAir()) {
            return InteractionResult.FAIL;
        }
        
        BlockPlaceContext placeContext = new BlockPlaceContext(context.getLevel(), context.getPlayer(), 
                context.getHand(), context.getItemInHand(), 
                new net.minecraft.world.phys.BlockHitResult(context.getClickLocation(), clickedFace, targetPos, context.isInside()));
        
        SwitchesLeverBlock leverBlock = (SwitchesLeverBlock) this.getBlock();
        BlockState blockState = leverBlock.getStateForPlacement(placeContext);
        
        if (blockState == null) {
            return InteractionResult.FAIL;
        }
        
        if (level.setBlock(targetPos, blockState, Block.UPDATE_ALL_IMMEDIATE)) {
            leverBlock.setPlacedBy(level, targetPos, blockState, context.getPlayer(), context.getItemInHand());
            
            if (context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
            
            return InteractionResult.sidedSuccess(level.isClientSide());
        } else {
            return InteractionResult.FAIL;
        }
    }
}