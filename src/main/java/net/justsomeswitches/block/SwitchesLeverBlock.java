package net.justsomeswitches.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Custom Switches Lever Block
 *
 * This block extends LeverBlock to inherit all vanilla lever functionality
 * while adding custom features like modified bounding box and particle suppression.
 *
 * Key Features:
 * - Custom bounding box (slightly larger than vanilla lever)
 * - No particles when powered (unlike vanilla lever)
 * - Same redstone behavior as vanilla lever
 * - Same placement rules as vanilla lever
 */
public class SwitchesLeverBlock extends LeverBlock {

    // Custom bounding boxes for different orientations
    // ALL bounding boxes are exactly 6x10x6 pixels, rotated based on placement orientation

    // Floor placement - facing North/South (10 pixels along Z-axis, 6 pixels along X-axis, 6 pixels along Y-axis)
    private static final VoxelShape FLOOR_AABB_NS = Block.box(5.0, 0.0, 3.0, 11.0, 6.0, 13.0);
    // Floor placement - facing East/West (10 pixels along X-axis, 6 pixels along Z-axis, 6 pixels along Y-axis)
    private static final VoxelShape FLOOR_AABB_EW = Block.box(3.0, 0.0, 5.0, 13.0, 6.0, 11.0);

    // Ceiling placement - facing North/South (10 pixels along Z-axis, 6 pixels along X-axis, 6 pixels along Y-axis)
    private static final VoxelShape CEILING_AABB_NS = Block.box(5.0, 10.0, 3.0, 11.0, 16.0, 13.0);
    // Ceiling placement - facing East/West (10 pixels along X-axis, 6 pixels along Z-axis, 6 pixels along Y-axis)
    private static final VoxelShape CEILING_AABB_EW = Block.box(3.0, 10.0, 5.0, 13.0, 16.0, 11.0);

    // Wall placements (6x10x6 pixels each)
    private static final VoxelShape WALL_NORTH_AABB = Block.box(5.0, 3.0, 10.0, 11.0, 13.0, 16.0);
    private static final VoxelShape WALL_SOUTH_AABB = Block.box(5.0, 3.0, 0.0, 11.0, 13.0, 6.0);
    private static final VoxelShape WALL_WEST_AABB = Block.box(10.0, 3.0, 5.0, 16.0, 13.0, 11.0);
    private static final VoxelShape WALL_EAST_AABB = Block.box(0.0, 3.0, 5.0, 6.0, 13.0, 11.0);

    /**
     * Constructor - sets up the block with the provided properties
     *
     * @param properties Block properties (hardness, material, etc.)
     */
    public SwitchesLeverBlock(Properties properties) {
        super(properties);

        // Initialize default blockstate (unpowered, facing north, on floor)
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(BlockStateProperties.POWERED, false)
                .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
        );
    }

    /**
     * Custom bounding box/shape for the block
     * Returns different shapes based on the block's attachment face and direction
     *
     * @param state The current blockstate
     * @param level The level/world
     * @param pos The block position
     * @param context Collision context
     * @return The VoxelShape for this block's bounding box
     */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        AttachFace attachFace = state.getValue(BlockStateProperties.ATTACH_FACE);
        Direction direction = state.getValue(BlockStateProperties.HORIZONTAL_FACING);

        return switch (attachFace) {
            case FLOOR -> switch (direction) {
                case NORTH, SOUTH -> FLOOR_AABB_NS;
                case EAST, WEST -> FLOOR_AABB_EW;
                default -> FLOOR_AABB_NS; // Fallback
            };
            case CEILING -> switch (direction) {
                case NORTH, SOUTH -> CEILING_AABB_NS;
                case EAST, WEST -> CEILING_AABB_EW;
                default -> CEILING_AABB_NS; // Fallback
            };
            case WALL -> switch (direction) {
                case NORTH -> WALL_NORTH_AABB;
                case SOUTH -> WALL_SOUTH_AABB;
                case WEST -> WALL_WEST_AABB;
                case EAST -> WALL_EAST_AABB;
                default -> FLOOR_AABB_NS; // Fallback, shouldn't happen
            };
        };
    }

    /**
     * Handle player interaction (right-click) with the block
     * Overridden to suppress vanilla particles while keeping all other functionality
     *
     * @param state Current blockstate
     * @param level The level/world
     * @param pos Block position
     * @param player The player interacting
     * @param hand The hand used for interaction
     * @param hit The hit result from the interaction
     * @return InteractionResult indicating success/failure
     */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {

        // Only process on server side to avoid desync
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        // Get current powered state
        boolean isPowered = state.getValue(BlockStateProperties.POWERED);

        // Toggle the powered state
        BlockState newState = state.setValue(BlockStateProperties.POWERED, !isPowered);
        level.setBlock(pos, newState, 3); // Flag 3 = notify neighbors and clients

        // Play the lever click sound (same as vanilla)
        level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS,
                0.3F, isPowered ? 0.5F : 0.6F);

        // Update neighboring blocks for redstone signal changes
        level.updateNeighborsAt(pos, this);

        // Update neighbors at the attached face for direct power (this is crucial!)
        Direction attachedFace = getAttachedDirection(state);
        level.updateNeighborsAt(pos.relative(attachedFace), this);

        return InteractionResult.CONSUME;
    }

    /**
     * Override to ensure we never spawn particles
     * The vanilla lever spawns smoke particles when toggled - we don't want this
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, net.minecraft.util.RandomSource random) {
        // Intentionally empty - suppresses vanilla lever particles
        // Could add custom particle effects here in the future if desired
    }

    /**
     * Determines if this block can provide redstone power
     * @return true - this block can provide redstone power like a vanilla lever
     */
    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    /**
     * Gets the direct redstone signal strength this block provides
     * Used for direct/strong power - provides strong power to the attached block
     *
     * @param state The blockstate
     * @param level The level/world
     * @param pos The block position
     * @param direction The direction being queried for power
     * @return Signal strength (0-15)
     */
    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        // Provide strong power (15) to the attached face when powered
        return state.getValue(BlockStateProperties.POWERED) && getAttachedDirection(state) == direction ? 15 : 0;
    }

    /**
     * Gets the indirect redstone signal strength this block provides
     * Used for indirect/weak power - provides weak power to all non-attached directions
     *
     * @param state The blockstate
     * @param level The level/world
     * @param pos The block position
     * @param direction The direction being queried for power
     * @return Signal strength (0-15)
     */
    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        // Provide weak power (15) to all directions EXCEPT the attached face when powered
        return state.getValue(BlockStateProperties.POWERED) && getAttachedDirection(state) != direction ? 15 : 0;
    }

    /**
     * Get the direction that this lever is attached to (the face it's attached to)
     * Helper method for redstone logic
     *
     * @param state The blockstate
     * @return The direction this lever is attached to
     */
    private Direction getAttachedDirection(BlockState state) {
        AttachFace attachFace = state.getValue(BlockStateProperties.ATTACH_FACE);
        return switch (attachFace) {
            case FLOOR -> Direction.DOWN;
            case CEILING -> Direction.UP;
            case WALL -> state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
        };
    }
}