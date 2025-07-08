package net.justsomeswitches.block;

import net.justsomeswitches.blockentity.SwitchesLeverBlockEntity;
import net.justsomeswitches.config.DebugConfig;
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
 * FRAMED BLOCKS APPROACH: Minimal lever block relying entirely on bulletproof NBT persistence
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
    }

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
    // BLOCK ENTITY MANAGEMENT
    // ========================================

    @Override
    @Nullable
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
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
    // FRAMED BLOCKS APPROACH: MINIMAL LEVER TOGGLE
    // ========================================

    @Override
    @Nonnull
    public InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player,
                                 @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit) {

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        // Current lever state
        boolean currentlyPowered = state.getValue(BlockStateProperties.POWERED);
        boolean newPoweredState = !currentlyPowered;

        DebugConfig.logUserAction("LEVER TOGGLE: " + currentlyPowered + " → " + newPoweredState);

        // PRE-TOGGLE: Validate BlockEntity state
        SwitchesLeverBlockEntity blockEntity = getBlockEntitySafe(level, pos);
        if (blockEntity != null) {
            DebugConfig.logPersistence("PRE-TOGGLE: Base=" + blockEntity.getBaseFaceSelection() +
                    ", Toggle=" + blockEntity.getToggleFaceSelection() +
                    ", Custom=" + blockEntity.hasCustomTextures());
        } else {
            DebugConfig.logCritical("No BlockEntity found before lever toggle!");
            return InteractionResult.FAIL;
        }

        // FRAMED BLOCKS APPROACH: Simple lever toggle - rely entirely on bulletproof NBT
        BlockState newState = state.setValue(BlockStateProperties.POWERED, newPoweredState);
        level.setBlock(pos, newState, Block.UPDATE_ALL);

        // POST-TOGGLE: Validate BlockEntity persistence
        SwitchesLeverBlockEntity postToggleEntity = getBlockEntitySafe(level, pos);
        if (postToggleEntity != null) {
            DebugConfig.logPersistence("POST-TOGGLE: Base=" + postToggleEntity.getBaseFaceSelection() +
                    ", Toggle=" + postToggleEntity.getToggleFaceSelection() +
                    ", Custom=" + postToggleEntity.hasCustomTextures());

            // Verify face selections survived the toggle
            if (blockEntity != null) {
                boolean basePersisted = blockEntity.getBaseFaceSelection() == postToggleEntity.getBaseFaceSelection();
                boolean togglePersisted = blockEntity.getToggleFaceSelection() == postToggleEntity.getToggleFaceSelection();

                if (basePersisted && togglePersisted) {
                    DebugConfig.logSuccess("Face selections survived lever toggle!");
                } else {
                    DebugConfig.logValidationFailure("Face persistence", "preserved",
                            "Base:" + basePersisted + " Toggle:" + togglePersisted);
                }
            }
        } else {
            DebugConfig.logCritical("BlockEntity missing after toggle!");
        }

        // Play lever sound
        level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS,
                0.3F, currentlyPowered ? 0.5F : 0.6F);

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * Safe BlockEntity getter with validation
     */
    @Nullable
    private SwitchesLeverBlockEntity getBlockEntitySafe(@Nonnull Level level, @Nonnull BlockPos pos) {
        try {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof SwitchesLeverBlockEntity switchEntity) {
                return switchEntity;
            } else if (entity != null) {
                DebugConfig.logCritical("Wrong BlockEntity type: " + entity.getClass().getSimpleName());
            }
        } catch (Exception e) {
            DebugConfig.logCritical("Exception getting BlockEntity: " + e.getMessage());
        }
        return null;
    }

    @Override
    public void animateTick(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull net.minecraft.util.RandomSource randomSource) {
        // Intentionally empty to suppress particle effects
    }

    // ========================================
    // ENHANCED NEIGHBOR UPDATES
    // ========================================

    @Override
    public void neighborChanged(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
                                @Nonnull Block neighborBlock, @Nonnull BlockPos neighborPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, isMoving);

        // Ensure texture persistence during neighbor updates
        SwitchesLeverBlockEntity blockEntity = getBlockEntitySafe(level, pos);
        if (blockEntity != null && blockEntity.hasCustomTextures()) {
            // Force immediate sync to prevent data loss
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            blockEntity.setChanged();
        }
    }

    @Override
    public void setPlacedBy(@Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState state,
                            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity placer,
                            @Nonnull net.minecraft.world.item.ItemStack stack) {

        super.setPlacedBy(level, pos, state, placer, stack);

        // Ensure BlockEntity is properly initialized
        SwitchesLeverBlockEntity blockEntity = getBlockEntitySafe(level, pos);
        if (blockEntity != null) {
            blockEntity.setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            DebugConfig.logSuccess("Lever placed with BlockEntity initialized");
        } else {
            DebugConfig.logCritical("BlockEntity not found after lever placement!");
        }
    }

    @Override
    public void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
                         @Nonnull BlockState newState, boolean isMoving) {

        // Handle texture dropping before removing
        if (!state.is(newState.getBlock())) {
            SwitchesLeverBlockEntity blockEntity = getBlockEntitySafe(level, pos);
            if (blockEntity != null) {
                blockEntity.dropStoredTextures(level, pos);
                DebugConfig.logPersistence("Lever removed - textures dropped");
            }
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }

    // ========================================
    // ENHANCED STATE CHANGE HANDLING
    // ========================================

    @Override
    public void onBlockStateChange(@Nonnull net.minecraft.world.level.LevelReader level, @Nonnull BlockPos pos,
                                   @Nonnull BlockState oldState, @Nonnull BlockState newState) {
        super.onBlockStateChange(level, pos, oldState, newState);

        // Ensure BlockEntity persistence during state changes
        if (level instanceof Level realLevel && !realLevel.isClientSide) {
            SwitchesLeverBlockEntity blockEntity = getBlockEntitySafe(realLevel, pos);
            if (blockEntity != null && blockEntity.hasCustomTextures()) {
                // Force sync to maintain texture data
                realLevel.sendBlockUpdated(pos, newState, newState, Block.UPDATE_CLIENTS);
            }
        }
    }

    // ========================================
    // VALIDATION HELPERS
    // ========================================

    /**
     * Validate that the BlockEntity is functioning correctly
     */
    public boolean validateBlockEntityIntegrity(@Nonnull Level level, @Nonnull BlockPos pos) {
        SwitchesLeverBlockEntity blockEntity = getBlockEntitySafe(level, pos);
        if (blockEntity == null) {
            DebugConfig.logCritical("BlockEntity validation failed - entity is null");
            return false;
        }

        try {
            // Test basic operations
            var baseFace = blockEntity.getBaseFaceSelection();
            var toggleFace = blockEntity.getToggleFaceSelection();
            var hasCustom = blockEntity.hasCustomTextures();

            if (baseFace == null || toggleFace == null) {
                DebugConfig.logCritical("BlockEntity validation failed - null face selections");
                return false;
            }

            DebugConfig.logSuccess("BlockEntity validation passed");
            return true;
        } catch (Exception e) {
            DebugConfig.logCritical("BlockEntity validation failed - exception: " + e.getMessage());
            return false;
        }
    }
}