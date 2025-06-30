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
import net.minecraft.world.level.block.RenderShape;
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
 * Enhanced Switches Lever Block with BlockEntity support for texture customization
 * ---
 * FIXED: Manual-only texture application with comprehensive debug output
 */
public class SwitchesLeverBlock extends LeverBlock implements EntityBlock {

    // Existing bounding box definitions (unchanged from Phase 2)
    private static final VoxelShape FLOOR_NORTH_SOUTH = Block.box(5.0, 0.0, 3.0, 11.0, 6.0, 13.0);
    private static final VoxelShape FLOOR_EAST_WEST = Block.box(3.0, 0.0, 5.0, 13.0, 6.0, 11.0);
    private static final VoxelShape CEILING_NORTH_SOUTH = Block.box(5.0, 10.0, 3.0, 11.0, 16.0, 13.0);
    private static final VoxelShape CEILING_EAST_WEST = Block.box(3.0, 10.0, 5.0, 13.0, 16.0, 11.0);
    private static final VoxelShape WALL_NORTH = Block.box(5.0, 3.0, 10.0, 11.0, 13.0, 16.0);
    private static final VoxelShape WALL_SOUTH = Block.box(5.0, 3.0, 0.0, 11.0, 13.0, 6.0);
    private static final VoxelShape WALL_WEST = Block.box(10.0, 3.0, 5.0, 16.0, 13.0, 11.0);
    private static final VoxelShape WALL_EAST = Block.box(0.0, 3.0, 5.0, 6.0, 13.0, 11.0);

    public SwitchesLeverBlock(Properties properties) {
        super(properties);
        System.out.println("DEBUG Block: SwitchesLeverBlock created");
    }

    // ========================================
    // BLOCK ENTITY IMPLEMENTATION
    // ========================================

    @Override
    @Nullable
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        System.out.println("DEBUG Block: Creating new BlockEntity at " + pos);
        return new SwitchesLeverBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state,
                                                                  @Nonnull BlockEntityType<T> blockEntityType) {
        // NeoForge 1.20.4 compatible ticker implementation
        if (blockEntityType == JustSomeSwitchesModBlockEntities.SWITCHES_LEVER.get()) {
            if (level.isClientSide()) {
                return (level1, pos, state1, blockEntity) -> {
                    if (blockEntity instanceof SwitchesLeverBlockEntity switchEntity) {
                        SwitchesLeverBlockEntity.clientTick(level1, pos, state1, switchEntity);
                    }
                };
            } else {
                return (level1, pos, state1, blockEntity) -> {
                    if (blockEntity instanceof SwitchesLeverBlockEntity switchEntity) {
                        SwitchesLeverBlockEntity.serverTick(level1, pos, state1, switchEntity);
                    }
                };
            }
        }
        return null;
    }

    // ========================================
    // VANILLA RENDERING WITH CUSTOM MODELS
    // ========================================

    @Override
    @Nonnull
    public RenderShape getRenderShape(@Nonnull BlockState state) {
        // Use vanilla rendering with custom models
        // This allows our custom models to handle texture replacement
        // without z-fighting issues from Block Entity Renderers
        return RenderShape.MODEL;
    }

    // ========================================
    // EXISTING FUNCTIONALITY (UNCHANGED)
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

        System.out.println("DEBUG Block: Switch lever used at " + pos + " by player " + player.getName().getString());

        // Toggle the powered state (unchanged lever behavior)
        boolean currentlyPowered = state.getValue(BlockStateProperties.POWERED);
        BlockState newState = state.setValue(BlockStateProperties.POWERED, !currentlyPowered);

        level.setBlock(pos, newState, 3);

        System.out.println("DEBUG Block: Lever state changed from " + currentlyPowered + " to " + !currentlyPowered);

        // Play standard lever click sound with pitch variation
        level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS,
                0.3F, currentlyPowered ? 0.5F : 0.6F);

        // Update redstone neighbors
        level.updateNeighborsAt(pos, this);
        Direction attachedDirection = getAttachedDirection(state);
        level.updateNeighborsAt(pos.relative(attachedDirection), this);

        // Trigger model update for texture changes
        triggerModelUpdate(level, pos);

        return InteractionResult.CONSUME;
    }

    /**
     * Trigger model update when block state changes
     * This ensures custom models receive updated ModelData
     */
    private void triggerModelUpdate(@Nonnull Level level, @Nonnull BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof SwitchesLeverBlockEntity blockEntity) {
            System.out.println("DEBUG Block: Triggering model update for custom textures");
            // Force ModelData refresh by sending block update
            level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), Block.UPDATE_ALL);
        }
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

    // ========================================
    // ENHANCED BLOCK ENTITY CLEANUP
    // ========================================

    @Override
    public void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
                         @Nonnull BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            System.out.println("DEBUG Block: Switch lever being removed at " + pos);

            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof SwitchesLeverBlockEntity switchEntity) {
                // Drop any stored texture blocks when switch is broken
                switchEntity.dropStoredTextures(level, pos);
                System.out.println("DEBUG Block: Dropped stored texture blocks");
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    // ========================================
    // ENHANCED BLOCK UPDATES
    // ========================================

    @Override
    public void neighborChanged(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
                                @Nonnull Block neighborBlock, @Nonnull BlockPos neighborPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, isMoving);

        // Ensure model updates are triggered for texture changes
        if (level.getBlockEntity(pos) instanceof SwitchesLeverBlockEntity blockEntity) {
            if (blockEntity.hasCustomTextures()) {
                System.out.println("DEBUG Block: Neighbor changed, updating custom texture model");
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            }
        }
    }

    /**
     * Enhanced state change handling for texture updates
     */
    @Override
    public void setPlacedBy(@Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState state,
                            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity placer,
                            @Nonnull net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        System.out.println("DEBUG Block: Switch lever placed at " + pos + " by " +
                (placer != null ? placer.getName().getString() : "unknown"));

        // Initialize BlockEntity and trigger initial model update
        if (level.getBlockEntity(pos) instanceof SwitchesLeverBlockEntity blockEntity) {
            System.out.println("DEBUG Block: Initializing BlockEntity with default textures");
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
        }
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