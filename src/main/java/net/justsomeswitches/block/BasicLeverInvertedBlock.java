package net.justsomeswitches.block;

import net.justsomeswitches.util.SwitchesVoxelShapes;
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
 * Lever switch with inverted visual appearance (up when OFF, down when ON).
 * Redstone behavior remains standard - emits signal strength 15 when powered.
 * Visual inversion handled purely by model files.
 */
public class BasicLeverInvertedBlock extends LeverBlock {

    /** Creates a new Basic Lever Inverted Block instance. */
    public BasicLeverInvertedBlock(Properties properties) {
        super(properties);
    }



    /** Returns collision shape from {@link SwitchesVoxelShapes} (same as standard lever). */
    @Override
    @Nonnull
    public VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
        AttachFace attachFace = state.getValue(BlockStateProperties.ATTACH_FACE);
        Direction direction = state.getValue(BlockStateProperties.HORIZONTAL_FACING);


        return switch (attachFace) {
            case FLOOR -> switch (direction) {
                case NORTH, SOUTH -> SwitchesVoxelShapes.FLOOR_NORTH_SOUTH;
                case EAST, WEST -> SwitchesVoxelShapes.FLOOR_EAST_WEST;
                default -> SwitchesVoxelShapes.FLOOR_NORTH_SOUTH;
            };
            case CEILING -> switch (direction) {
                case NORTH, SOUTH -> SwitchesVoxelShapes.CEILING_NORTH_SOUTH;
                case EAST, WEST -> SwitchesVoxelShapes.CEILING_EAST_WEST;
                default -> SwitchesVoxelShapes.CEILING_NORTH_SOUTH;
            };
            case WALL -> switch (direction) {
                case NORTH -> SwitchesVoxelShapes.WALL_NORTH;
                case SOUTH -> SwitchesVoxelShapes.WALL_SOUTH;
                case WEST -> SwitchesVoxelShapes.WALL_WEST;
                case EAST -> SwitchesVoxelShapes.WALL_EAST;
                default -> SwitchesVoxelShapes.WALL_NORTH;
            };
        };
    }

    /** Toggles powered state and updates redstone signal output. */
    @Override
    @Nonnull
    public InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player,
                                 @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit) {


        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }


        boolean currentlyPowered = state.getValue(BlockStateProperties.POWERED);
        BlockState newState = state.setValue(BlockStateProperties.POWERED, !currentlyPowered);


        level.setBlock(pos, newState, Block.UPDATE_ALL);

        // Pitch: 0.5F off, 0.6F on
        level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS,
                0.3F, currentlyPowered ? 0.5F : 0.6F);


        level.updateNeighborsAt(pos, this);
        

        Direction attachedDirection = getAttachedDirection(state);
        level.updateNeighborsAt(pos.relative(attachedDirection), this);

        return InteractionResult.CONSUME;
    }

    /** Suppresses vanilla lever particle effects. */
    @Override
    public void animateTick(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull net.minecraft.util.RandomSource randomSource) {

    }

    /** Returns the direction pointing to the block this lever is attached to. */
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
