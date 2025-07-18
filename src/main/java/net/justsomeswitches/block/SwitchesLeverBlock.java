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
 * Enhanced Switches Lever Block with Minimal NBT Protection - FIXED VERSION
 * MINIMAL FIX: Protects NBT data during blockstate changes without complex preservation
 *
 * APPROACH: Targeted protection of face selections during lever toggles
 */
public class SwitchesLeverBlock extends LeverBlock implements EntityBlock {

    // Bounding box definitions
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
    }

    // ========================================
    // BLOCK ENTITY IMPLEMENTATION
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
    // Z-FIGHTING RESOLUTION
    // ========================================

    @Override
    @Nonnull
    public RenderShape getRenderShape(@Nonnull BlockState state) {
        // Use INVISIBLE to prevent vanilla model rendering
        return RenderShape.INVISIBLE;
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

        // MINIMAL FIX: Protect NBT data before blockstate change
        if (level.getBlockEntity(pos) instanceof SwitchesLeverBlockEntity blockEntity) {
            blockEntity.protectNBTDuringStateChange();
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

    /**
     * MINIMAL FIX: Handle scheduled ticks for NBT protection cleanup
     */
    @Override
    public void tick(@Nonnull BlockState state, @Nonnull net.minecraft.server.level.ServerLevel level, @Nonnull BlockPos pos, @Nonnull net.minecraft.util.RandomSource random) {
        // End NBT protection after blockstate change completes
        if (level.getBlockEntity(pos) instanceof SwitchesLeverBlockEntity blockEntity) {
            blockEntity.endNBTProtection();

            // Trigger model update after protection ends
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            blockEntity.requestModelDataUpdate();
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
    // BLOCK ENTITY CLEANUP
    // ========================================

    @Override
    public void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
                         @Nonnull BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof SwitchesLeverBlockEntity switchEntity) {
                switchEntity.dropStoredTextures(level, pos);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    // ========================================
    // BLOCK UPDATES
    // ========================================

    @Override
    public void neighborChanged(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
                                @Nonnull Block neighborBlock, @Nonnull BlockPos neighborPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, isMoving);

        if (level.getBlockEntity(pos) instanceof SwitchesLeverBlockEntity blockEntity) {
            if (blockEntity.hasCustomTextures()) {
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            }
        }
    }

    @Override
    public void setPlacedBy(@Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState state,
                            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity placer,
                            @Nonnull net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (level.getBlockEntity(pos) instanceof SwitchesLeverBlockEntity blockEntity) {
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    /**
     * Debug method to check current texture state
     */
    public void debugTextureState(@Nonnull Level level, @Nonnull BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof SwitchesLeverBlockEntity blockEntity) {
            System.out.println("DEBUG Block: Switch at " + pos +
                    " - Base: " + blockEntity.getBaseTexture() +
                    ", Toggle: " + blockEntity.getToggleTexture() +
                    ", BaseVariable: " + blockEntity.getBaseTextureVariable() +
                    ", ToggleVariable: " + blockEntity.getToggleTextureVariable() +
                    ", HasCustom: " + blockEntity.hasCustomTextures());
        }
    }
}