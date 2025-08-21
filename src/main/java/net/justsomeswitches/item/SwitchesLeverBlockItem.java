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
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;

/**
 * Custom BlockItem for SwitchesLeverBlock that properly handles advanced placement
 * This ensures the block gets placed in the correct position with correct orientation
 */
public class SwitchesLeverBlockItem extends BlockItem {

    public SwitchesLeverBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    @Nonnull
    public InteractionResult useOn(@Nonnull UseOnContext context) {
        // FIXED: Check if clicking on an existing Switches Lever - if so, toggle it instead of placing
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        BlockState clickedState = level.getBlockState(clickedPos);
        
        // If clicking on an existing Switches Lever, delegate to its toggle interaction
        if (clickedState.getBlock() instanceof SwitchesLeverBlock) {
            // Create a proper BlockHitResult and delegate to the block's use method
            net.minecraft.world.phys.BlockHitResult hitResult = new net.minecraft.world.phys.BlockHitResult(
                context.getClickLocation(), context.getClickedFace(), clickedPos, context.isInside());
            return clickedState.use(level, context.getPlayer(), context.getHand(), hitResult);
        }
        
        // Otherwise, continue with placement logic
        // For wall placement, we want to place the lever ON the clicked block
        // For floor/ceiling placement, we want to place it adjacent
        Direction clickedFace = context.getClickedFace();
        BlockPos targetPos;
        
        if (clickedFace == Direction.UP || clickedFace == Direction.DOWN) {
            // Floor/Ceiling: Place on adjacent block (on top/bottom of clicked block)
            targetPos = context.getClickedPos().relative(clickedFace);
        } else {
            // Wall: Place in air block adjacent to wall (lever attaches TO wall but occupies air space)
            targetPos = context.getClickedPos().relative(clickedFace);
        }
        
        // Check if we can place at the target position
        if (!level.getBlockState(targetPos).canBeReplaced() && !level.getBlockState(targetPos).isAir()) {
            return InteractionResult.FAIL;
        }
        
        // Create a new context with the correct target position
        BlockPlaceContext placeContext = new BlockPlaceContext(context.getLevel(), context.getPlayer(), 
                context.getHand(), context.getItemInHand(), 
                new net.minecraft.world.phys.BlockHitResult(context.getClickLocation(), clickedFace, targetPos, context.isInside()));
        
        // Get the block state from our custom block
        SwitchesLeverBlock leverBlock = (SwitchesLeverBlock) this.getBlock();
        BlockState blockState = leverBlock.getStateForPlacement(placeContext);
        
        if (blockState == null) {
            return InteractionResult.FAIL;
        }
        
        // Place the block with our custom state
        if (level.setBlock(targetPos, blockState, Block.UPDATE_ALL_IMMEDIATE)) {
            // Call setPlacedBy to finalize placement
            leverBlock.setPlacedBy(level, targetPos, blockState, context.getPlayer(), context.getItemInHand());
            
            // Consume the item
            if (context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
            
            return InteractionResult.sidedSuccess(level.isClientSide());
        } else {
            return InteractionResult.FAIL;
        }
    }
}