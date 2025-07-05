package net.justsomeswitches.block;

import net.justsomeswitches.blockentity.SwitchesLeverBlockEntity;
import net.justsomeswitches.init.JustSomeSwitchesModBlockEntities;
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
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * CRITICAL FIX: Enhanced Switches Lever Block with Proper Preservation Timing
 * ---
 * SOLUTION: Ensure face selections are preserved BEFORE block state changes occur
 */
public class SwitchesLeverBlock extends LeverBlock implements EntityBlock {

    // ========================================
    // ENHANCED BOUNDING BOXES
    // ========================================

    // Floor lever shapes (6 pixels tall, rotated base)
    private static final VoxelShape FLOOR_NORTH_SOUTH = Block.box(5.0, 0.0, 3.0, 11.0, 6.0, 13.0);
    private static final VoxelShape FLOOR_EAST_WEST = Block.box(3.0, 0.0, 5.0, 13.0, 6.0, 11.0);

    // Ceiling lever shapes (hangs down 6 pixels)
    private static final VoxelShape CEILING_NORTH_SOUTH = Block.box(5.0, 10.0, 3.0, 11.0, 16.0, 13.0);
    private static final VoxelShape CEILING_EAST_WEST = Block.box(3.0, 10.0, 5.0, 13.0, 16.0, 11.0);

    // Wall lever shapes (extends 6 pixels from wall)
    private static final VoxelShape WALL_NORTH = Block.box(5.0, 3.0, 10.0, 11.0, 13.0, 16.0);
    private static final VoxelShape WALL_SOUTH = Block.box(5.0, 3.0, 0.0, 11.0, 13.0, 6.0);
    private static final VoxelShape WALL_EAST = Block.box(0.0, 3.0, 5.0, 6.0, 13.0, 11.0);
    private static final VoxelShape WALL_WEST = Block.box(10.0, 3.0, 5.0, 16.0, 13.0, 11.0);

    public SwitchesLeverBlock(Properties properties) {
        super(properties);
        System.out.println("DEBUG Block: SwitchesLeverBlock created with correct direction-aware bounding box");
    }

    // ========================================
    // ENHANCED DIRECTION-AWARE BOUNDING BOX
    // ========================================

    @Override
    @Nonnull
    public VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
        AttachFace attachFace = state.getValue(BlockStateProperties.ATTACH_FACE);
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);

        return switch (attachFace) {
            case FLOOR -> switch (facing) {
                case NORTH, SOUTH -> FLOOR_NORTH_SOUTH;
                case EAST, WEST -> FLOOR_EAST_WEST;
                default -> FLOOR_NORTH_SOUTH;
            };
            case CEILING -> switch (facing) {
                case NORTH, SOUTH -> CEILING_NORTH_SOUTH;
                case EAST, WEST -> CEILING_EAST_WEST;
                default -> CEILING_NORTH_SOUTH;
            };
            case WALL -> switch (facing) {
                case NORTH -> WALL_NORTH;
                case SOUTH -> WALL_SOUTH;
                case EAST -> WALL_EAST;
                case WEST -> WALL_WEST;
                default -> WALL_NORTH;
            };
        };
    }

    // ========================================
    // BLOCK ENTITY CREATION AND MANAGEMENT
    // ========================================

    @Override
    @Nullable
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        System.out.println("DEBUG Block: Switch placed at " + pos + " - initialized with default textures");
        return new SwitchesLeverBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state,
                                                                  @Nonnull BlockEntityType<T> blockEntityType) {
        if (blockEntityType == JustSomeSwitchesModBlockEntities.SWITCHES_LEVER.get()) {
            return level.isClientSide ?
                    (lvl, pos, st, be) -> SwitchesLeverBlockEntity.clientTick(lvl, pos, st, (SwitchesLeverBlockEntity) be) :
                    (lvl, pos, st, be) -> SwitchesLeverBlockEntity.serverTick(lvl, pos, st, (SwitchesLeverBlockEntity) be);
        }
        return null;
    }

    // ========================================
    // CRITICAL FIX: ENHANCED LEVER INTERACTION WITH PROPER PRESERVATION TIMING
    // ========================================

    @Override
    @Nonnull
    public InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player,
                                 @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit) {

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        System.out.println("DEBUG Block: Switch lever used at " + pos + " by player " + player.getName().getString());

        // CRITICAL FIX: Get BlockEntity BEFORE any state changes
        SwitchesLeverBlockEntity blockEntity = null;
        if (level.getBlockEntity(pos) instanceof SwitchesLeverBlockEntity entity) {
            blockEntity = entity;
        }

        // CRITICAL FIX: If we have a BlockEntity, preserve face selections BEFORE state change
        if (blockEntity != null) {
            System.out.println("DEBUG Block: CRITICAL FIX - Preserving face selections before state change");
            System.out.println("DEBUG Block: Current state before preservation - Base: " + blockEntity.getBaseFaceSelection() +
                    ", Toggle: " + blockEntity.getToggleFaceSelection() + ", Inverted: " + blockEntity.isInverted());

            // CRITICAL FIX: Force immediate preservation
            blockEntity.preserveFaceSelectionsForStateChange();

            // Wait a moment to ensure NBT save completes
            try {
                Thread.sleep(1); // Minimal delay to ensure NBT persistence
            } catch (InterruptedException e) {
                // Continue if interrupted
            }
        }

        // Toggle the powered state (unchanged lever behavior)
        boolean currentlyPowered = state.getValue(BlockStateProperties.POWERED);
        BlockState newState = state.setValue(BlockStateProperties.POWERED, !currentlyPowered);

        System.out.println("DEBUG Block: CRITICAL FIX - Applying state change with preservation active");
        System.out.println("DEBUG Block: State changing from powered=" + currentlyPowered + " to powered=" + !currentlyPowered);

        // CRITICAL FIX: Use Block.UPDATE_ALL for proper BlockEntity preservation
        level.setBlock(pos, newState, Block.UPDATE_ALL);

        System.out.println("DEBUG Block: Lever state changed from " + currentlyPowered + " to " + !currentlyPowered);

        // Play standard lever click sound with pitch variation
        level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS,
                0.3F, currentlyPowered ? 0.5F : 0.6F);

        // CRITICAL FIX: Verify preservation worked and force visual update
        if (level.getBlockEntity(pos) instanceof SwitchesLeverBlockEntity updatedEntity) {
            System.out.println("DEBUG Block: CRITICAL FIX - Verifying preservation after state change");
            System.out.println("DEBUG Block: State after preservation - Base: " + updatedEntity.getBaseFaceSelection() +
                    ", Toggle: " + updatedEntity.getToggleFaceSelection() + ", Inverted: " + updatedEntity.isInverted());

            // Force immediate visual refresh
            level.sendBlockUpdated(pos, newState, newState, Block.UPDATE_CLIENTS);
            updatedEntity.requestModelDataUpdate();

            System.out.println("DEBUG Block: Face selection preservation verification completed");
        } else {
            System.out.println("DEBUG Block: ERROR - BlockEntity not found after state change!");
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void animateTick(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull net.minecraft.util.RandomSource randomSource) {
        // Intentionally empty to suppress particle effects
    }

    // ========================================
    // ENHANCED BLOCK UPDATES WITH PRESERVATION
    // ========================================

    @Override
    public void neighborChanged(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
                                @Nonnull Block neighborBlock, @Nonnull BlockPos neighborPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, isMoving);

        // Ensure model updates preserve face selections
        if (level.getBlockEntity(pos) instanceof SwitchesLeverBlockEntity blockEntity) {
            if (blockEntity.hasCustomTextures()) {
                System.out.println("DEBUG Block: Neighbor changed, updating custom texture model with face preservation");

                // Use UPDATE_CLIENTS to preserve face selections
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            }
        }
    }

    /**
     * CRITICAL FIX: Enhanced setPlacedBy to prevent BlockEntity recreation during operations
     */
    @Override
    public void setPlacedBy(@Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState state,
                            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity placer,
                            @Nonnull net.minecraft.world.item.ItemStack stack) {

        // Check if this is initial block placement vs lever state change
        boolean isInitialPlacement = placer != null && level.getBlockEntity(pos) == null;

        if (isInitialPlacement) {
            System.out.println("DEBUG Block: Switch placed at " + pos + " - initialized with default textures");

            super.setPlacedBy(level, pos, state, placer, stack);

            // Initialize BlockEntity for new placement only
            if (level.getBlockEntity(pos) instanceof SwitchesLeverBlockEntity blockEntity) {
                System.out.println("DEBUG Block: CRITICAL FIX - Initializing new BlockEntity with default state");

                // Use UPDATE_CLIENTS for initial setup
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            }
        } else {
            System.out.println("DEBUG Block: CRITICAL FIX - Skipping setPlacedBy for state change to preserve BlockEntity");
            // Don't call super.setPlacedBy() for state changes - this prevents BlockEntity recreation
        }
    }

    // ========================================
    // ENHANCED BLOCK ENTITY CLEANUP
    // ========================================

    @Override
    public void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
                         @Nonnull BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            System.out.println("DEBUG Block: Switch removed at " + pos + " - cleaning up textures");

            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof SwitchesLeverBlockEntity switchEntity) {
                // Drop any stored texture blocks when switch is broken
                switchEntity.dropStoredTextures(level, pos);
                System.out.println("DEBUG Block: Dropped stored texture blocks");
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    /**
     * Debug method to check current texture state (for development use)
     */
    public void debugTextureState(@Nonnull Level level, @Nonnull BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof SwitchesLeverBlockEntity blockEntity) {
            System.out.println("DEBUG Block: Switch at " + pos +
                    " - Base: " + blockEntity.getBaseTexture() +
                    ", Toggle: " + blockEntity.getToggleTexture() +
                    ", BaseFace: " + blockEntity.getBaseFaceSelection() +
                    ", ToggleFace: " + blockEntity.getToggleFaceSelection() +
                    ", Inverted: " + blockEntity.isInverted() +
                    ", HasCustom: " + blockEntity.hasCustomTextures());
        }
    }
}