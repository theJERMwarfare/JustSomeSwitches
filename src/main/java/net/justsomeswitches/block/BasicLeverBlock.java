package net.justsomeswitches.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nonnull;

/**
 * Basic Lever Block - Simple lever without block entity or custom textures
 * ---
 * Features:
 * - Same placement conditions as Switches Lever
 * - Same placement rotation as Switches Lever 
 * - Same redstone behavior as Switches Lever
 * - Standard block (NOT block entity)
 * - No GUI, no custom textures
 * - Minimal resource usage
 */
public class BasicLeverBlock extends LeverBlock {

    // Bounding box definitions (same as Switches Lever)
    private static final VoxelShape FLOOR_NORTH_SOUTH = Block.box(5.0, 0.0, 3.0, 11.0, 6.0, 13.0);
    private static final VoxelShape FLOOR_EAST_WEST = Block.box(3.0, 0.0, 5.0, 13.0, 6.0, 11.0);
    private static final VoxelShape CEILING_NORTH_SOUTH = Block.box(5.0, 10.0, 3.0, 11.0, 16.0, 13.0);
    private static final VoxelShape CEILING_EAST_WEST = Block.box(3.0, 10.0, 5.0, 13.0, 16.0, 11.0);
    private static final VoxelShape WALL_NORTH = Block.box(5.0, 3.0, 10.0, 11.0, 13.0, 16.0);
    private static final VoxelShape WALL_SOUTH = Block.box(5.0, 3.0, 0.0, 11.0, 13.0, 6.0);
    private static final VoxelShape WALL_WEST = Block.box(10.0, 3.0, 5.0, 16.0, 13.0, 11.0);
    private static final VoxelShape WALL_EAST = Block.box(0.0, 3.0, 5.0, 6.0, 13.0, 11.0);

    public BasicLeverBlock(Properties properties) {
        super(properties);
    }

    // ========================================
    // SHAPE AND INTERACTION HANDLING
    // ========================================

    @Override
    @Nonnull
    public VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
        AttachFace attachFace = state.getValue(BlockStateProperties.ATTACH_FACE);
        Direction direction = state.getValue(BlockStateProperties.HORIZONTAL_FACING);

        return switch (attachFace) {
            case FLOOR -> switch (direction) {
                case NORTH, SOUTH -> FLOOR_NORTH_SOUTH;
                case EAST, WEST -> FLOOR_EAST_WEST;
                default -> FLOOR_NORTH_SOUTH;
            };
            case CEILING -> switch (direction) {
                case NORTH, SOUTH -> CEILING_NORTH_SOUTH;
                case EAST, WEST -> CEILING_EAST_WEST;
                default -> CEILING_NORTH_SOUTH;
            };
            case WALL -> switch (direction) {
                case NORTH -> WALL_NORTH;
                case SOUTH -> WALL_SOUTH;
                case WEST -> WALL_WEST;
                case EAST -> WALL_EAST;
                default -> WALL_NORTH;
            };
        };
    }

    @Override
    @Nonnull
    public InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player,
                                 @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit) {

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        // Toggle the powered state
        boolean currentlyPowered = state.getValue(BlockStateProperties.POWERED);
        BlockState newState = state.setValue(BlockStateProperties.POWERED, !currentlyPowered);

        // Set the new state
        level.setBlock(pos, newState, Block.UPDATE_ALL);

        // Play standard lever click sound
        level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS,
                0.3F, currentlyPowered ? 0.5F : 0.6F);

        // Update redstone neighbors
        level.updateNeighborsAt(pos, this);
        Direction attachedDirection = getAttachedDirection(state);
        level.updateNeighborsAt(pos.relative(attachedDirection), this);

        return InteractionResult.CONSUME;
    }

    @Override
    public void animateTick(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull net.minecraft.util.RandomSource randomSource) {
        // Intentionally empty to suppress particle effects
    }

    @Nonnull
    private Direction getAttachedDirection(@Nonnull BlockState state) {
        AttachFace attachFace = state.getValue(BlockStateProperties.ATTACH_FACE);
        return switch (attachFace) {
            case FLOOR -> Direction.DOWN;
            case CEILING -> Direction.UP;
            case WALL -> state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
        };
    }
}
