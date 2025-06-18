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
 * Custom lever block with enhanced visual design and particle-free operation.
 * Extends the vanilla LeverBlock to inherit all standard redstone functionality while providing:
 * - Custom 3D models with configurable textures
 * - Precision-fitted bounding boxes for all orientations
 * - Clean operation without particle effects
 * - Identical redstone behavior and placement mechanics to vanilla levers
 */
public class SwitchesLeverBlock extends LeverBlock {

    // Bounding box definitions for all placement orientations
    // All boxes maintain consistent 6×10×6 pixel dimensions with the 10-pixel length
    // aligned to the lever's facing direction for proper collision detection

    // Floor-mounted lever bounding boxes
    private static final VoxelShape FLOOR_NORTH_SOUTH = Block.box(5.0, 0.0, 3.0, 11.0, 6.0, 13.0);
    private static final VoxelShape FLOOR_EAST_WEST = Block.box(3.0, 0.0, 5.0, 13.0, 6.0, 11.0);

    // Ceiling-mounted lever bounding boxes
    private static final VoxelShape CEILING_NORTH_SOUTH = Block.box(5.0, 10.0, 3.0, 11.0, 16.0, 13.0);
    private static final VoxelShape CEILING_EAST_WEST = Block.box(3.0, 10.0, 5.0, 13.0, 16.0, 11.0);

    // Wall-mounted lever bounding boxes
    private static final VoxelShape WALL_NORTH = Block.box(5.0, 3.0, 10.0, 11.0, 13.0, 16.0);
    private static final VoxelShape WALL_SOUTH = Block.box(5.0, 3.0, 0.0, 11.0, 13.0, 6.0);
    private static final VoxelShape WALL_WEST = Block.box(10.0, 3.0, 5.0, 16.0, 13.0, 11.0);
    private static final VoxelShape WALL_EAST = Block.box(0.0, 3.0, 5.0, 6.0, 13.0, 11.0);

    /**
     * Constructs a new switches lever block with the specified properties.
     * Delegates blockstate initialization to the parent LeverBlock to ensure
     * proper setup of POWERED, ATTACH_FACE, and HORIZONTAL_FACING properties.
     * @param properties Block properties defining material behavior, hardness, etc.
     */
    public SwitchesLeverBlock(Properties properties) {
        super(properties);
    }

    /**
     * Determines the collision and selection shape for this block based on its current state.
     * Returns orientation-specific bounding boxes that properly fit the 3D model geometry.
     * The shape adapts to the lever's attachment face and horizontal facing direction.
     * @param state Current blockstate containing orientation and power information
     * @param level World reference (unused but required by interface)
     * @param pos Block position (unused but required by interface)
     * @param context Collision context for shape calculation
     * @return VoxelShape defining the block's physical boundaries
     */
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

    /**
     * Handles player interaction with the lever.
     * Toggles the lever's powered state while maintaining all vanilla functionality
     * including sound effects, redstone updates, and neighbor notifications.
     * Deliberately excludes particle effects for cleaner visual operation.
     * @param state Current blockstate
     * @param level World instance
     * @param pos Block position
     * @param player Player performing the interaction
     * @param hand Hand used for interaction
     * @param hit Ray trace result from player targeting
     * @return Interaction result indicating success and consumption
     */
    @Override
    @Nonnull
    public InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player,
                                 @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit) {

        // Process interaction only on server side to prevent client-server desynchronization
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        // Toggle the powered state
        boolean currentlyPowered = state.getValue(BlockStateProperties.POWERED);
        BlockState newState = state.setValue(BlockStateProperties.POWERED, !currentlyPowered);

        // Update the world with the new state and notify clients
        level.setBlock(pos, newState, 3);

        // Play standard lever click sound with pitch variation based on state
        level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS,
                0.3F, currentlyPowered ? 0.5F : 0.6F);

        // Notify adjacent blocks of redstone changes
        level.updateNeighborsAt(pos, this);

        // Notify the directly attached block of power changes for strong redstone signal
        Direction attachedDirection = getAttachedDirection(state);
        level.updateNeighborsAt(pos.relative(attachedDirection), this);

        return InteractionResult.CONSUME;
    }

    /**
     * Suppresses particle generation during block updates.
     * Overrides the parent implementation to prevent any visual particle effects
     * while the lever is in operation, providing cleaner visual feedback.
     * @param state Current blockstate
     * @param level World instance
     * @param pos Block position
     * @param randomSource Random number generator for particle variations
     */
    @Override
    public void animateTick(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull net.minecraft.util.RandomSource randomSource) {
        // Intentionally empty to suppress all particle effects
    }

    /**
     * Calculates the direction this lever is attached to its supporting block.
     * Used for redstone neighbor updates to ensure proper signal propagation
     * to the block the lever is mounted on.
     * @param state Current blockstate containing attachment information
     * @return Direction pointing toward the supporting block
     */
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