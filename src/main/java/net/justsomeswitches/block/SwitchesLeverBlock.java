package net.justsomeswitches.block;

import net.justsomeswitches.blockentity.SwitchesLeverBlockEntity;
import net.justsomeswitches.init.JustSomeSwitchesModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
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
 * ENHANCED DEBUG: Switches Lever Block with Comprehensive Face Selection Preservation Tracking
 * ---
 * CRITICAL DEBUG: Added detailed tracing to identify where face selection data is lost
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
        System.out.println("DEBUG Block: SwitchesLeverBlock created with comprehensive debug tracking");
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
        System.out.println("DEBUG Block: ENHANCED DEBUG - newBlockEntity called at " + pos);
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
    // ENHANCED DEBUG: LEVER INTERACTION WITH COMPREHENSIVE TRACKING
    // ========================================

    @Override
    @Nonnull
    public InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player,
                                 @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit) {

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        System.out.println("DEBUG Block: ===== ENHANCED DEBUG - LEVER TOGGLE SEQUENCE START =====");
        System.out.println("DEBUG Block: Switch lever used at " + pos + " by player " + player.getName().getString());

        // ENHANCED DEBUG: Get current powered state
        boolean currentlyPowered = state.getValue(BlockStateProperties.POWERED);
        boolean newPoweredState = !currentlyPowered;
        System.out.println("DEBUG Block: State change - Current: " + currentlyPowered + " → New: " + newPoweredState);

        // ENHANCED DEBUG: Detailed BlockEntity inspection BEFORE any changes
        SwitchesLeverBlockEntity originalBlockEntity = null;
        CompoundTag preservedNBT = null;
        String originalInstanceId = null;

        if (level.getBlockEntity(pos) instanceof SwitchesLeverBlockEntity blockEntity) {
            originalBlockEntity = blockEntity;
            originalInstanceId = Integer.toHexString(System.identityHashCode(blockEntity));

            System.out.println("DEBUG Block: BEFORE - BlockEntity instance ID: " + originalInstanceId);
            System.out.println("DEBUG Block: BEFORE - Face selections - Base: " + blockEntity.getBaseFaceSelection() +
                    ", Toggle: " + blockEntity.getToggleFaceSelection() + ", Inverted: " + blockEntity.isInverted());
            System.out.println("DEBUG Block: BEFORE - Textures - Base: " + blockEntity.getBaseTexture() +
                    ", Toggle: " + blockEntity.getToggleTexture());

            // CRITICAL: Save complete NBT data
            preservedNBT = blockEntity.saveToNBT();
            System.out.println("DEBUG Block: NBT BACKUP - Created backup with " + preservedNBT.getAllKeys().size() + " keys");

            // ENHANCED DEBUG: Verify NBT contains face selection data
            if (preservedNBT.contains("base_face_selection")) {
                System.out.println("DEBUG Block: NBT BACKUP - Base face: '" + preservedNBT.getString("base_face_selection") + "'");
            } else {
                System.out.println("DEBUG Block: ⚠️ NBT BACKUP - Missing base_face_selection key!");
            }

            if (preservedNBT.contains("toggle_face_selection")) {
                System.out.println("DEBUG Block: NBT BACKUP - Toggle face: '" + preservedNBT.getString("toggle_face_selection") + "'");
            } else {
                System.out.println("DEBUG Block: ⚠️ NBT BACKUP - Missing toggle_face_selection key!");
            }

            System.out.println("DEBUG Block: NBT BACKUP - Complete NBT keys: " + preservedNBT.getAllKeys());
        } else {
            System.out.println("DEBUG Block: ⚠️ ERROR - No BlockEntity found before state change!");
        }

        // CRITICAL: Change block state
        BlockState newState = state.setValue(BlockStateProperties.POWERED, newPoweredState);
        System.out.println("DEBUG Block: Calling setBlock() - This may recreate BlockEntity");

        level.setBlock(pos, newState, Block.UPDATE_ALL);

        System.out.println("DEBUG Block: setBlock() completed");

        // ENHANCED DEBUG: Detailed BlockEntity inspection AFTER state change
        SwitchesLeverBlockEntity newBlockEntity = null;
        String newInstanceId = null;

        if (level.getBlockEntity(pos) instanceof SwitchesLeverBlockEntity blockEntity) {
            newBlockEntity = blockEntity;
            newInstanceId = Integer.toHexString(System.identityHashCode(blockEntity));

            System.out.println("DEBUG Block: AFTER - BlockEntity instance ID: " + newInstanceId);

            // ENHANCED DEBUG: Check if BlockEntity was recreated
            if (originalInstanceId != null && !originalInstanceId.equals(newInstanceId)) {
                System.out.println("DEBUG Block: ⚠️ BLOCKENTITY RECREATED - Old ID: " + originalInstanceId + " → New ID: " + newInstanceId);
            } else if (originalInstanceId != null) {
                System.out.println("DEBUG Block: ✅ BLOCKENTITY PRESERVED - Same instance: " + newInstanceId);
            }

            System.out.println("DEBUG Block: AFTER (before restore) - Face selections - Base: " + blockEntity.getBaseFaceSelection() +
                    ", Toggle: " + blockEntity.getToggleFaceSelection() + ", Inverted: " + blockEntity.isInverted());

            // CRITICAL: Restore NBT data if we have it
            if (preservedNBT != null) {
                System.out.println("DEBUG Block: NBT RESTORE - Attempting to restore from backup");

                // Enhanced debug: Show what we're about to restore
                System.out.println("DEBUG Block: NBT RESTORE - Restoring Base face: '" +
                        preservedNBT.getString("base_face_selection") + "'");
                System.out.println("DEBUG Block: NBT RESTORE - Restoring Toggle face: '" +
                        preservedNBT.getString("toggle_face_selection") + "'");

                blockEntity.loadFromNBT(preservedNBT);

                System.out.println("DEBUG Block: NBT RESTORE - Completed");
                System.out.println("DEBUG Block: AFTER (after restore) - Face selections - Base: " + blockEntity.getBaseFaceSelection() +
                        ", Toggle: " + blockEntity.getToggleFaceSelection() + ", Inverted: " + blockEntity.isInverted());

                // ENHANCED DEBUG: Verify restore was successful
                if (originalBlockEntity != null) {
                    boolean baseFaceMatch = blockEntity.getBaseFaceSelection() == originalBlockEntity.getBaseFaceSelection();
                    boolean toggleFaceMatch = blockEntity.getToggleFaceSelection() == originalBlockEntity.getToggleFaceSelection();
                    boolean invertedMatch = blockEntity.isInverted() == originalBlockEntity.isInverted();

                    System.out.println("DEBUG Block: RESTORE VERIFICATION - Base face match: " + baseFaceMatch);
                    System.out.println("DEBUG Block: RESTORE VERIFICATION - Toggle face match: " + toggleFaceMatch);
                    System.out.println("DEBUG Block: RESTORE VERIFICATION - Inverted match: " + invertedMatch);

                    if (baseFaceMatch && toggleFaceMatch && invertedMatch) {
                        System.out.println("DEBUG Block: ✅ NBT RESTORE SUCCESSFUL - All face selections preserved");
                    } else {
                        System.out.println("DEBUG Block: ❌ NBT RESTORE FAILED - Face selections not preserved");
                    }
                }

                // Force immediate block update and model data refresh
                level.sendBlockUpdated(pos, newState, newState, Block.UPDATE_CLIENTS);
                blockEntity.requestModelDataUpdate();

                System.out.println("DEBUG Block: Visual update triggered");
            } else {
                System.out.println("DEBUG Block: ⚠️ No NBT backup available for restore");
            }
        } else {
            System.out.println("DEBUG Block: ⚠️ ERROR - No BlockEntity found after state change!");
        }

        // Play standard lever click sound
        level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS,
                0.3F, currentlyPowered ? 0.5F : 0.6F);

        System.out.println("DEBUG Block: ===== ENHANCED DEBUG - LEVER TOGGLE SEQUENCE END =====");

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
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            }
        }
    }

    /**
     * ENHANCED DEBUG: Enhanced setPlacedBy with detailed tracking
     */
    @Override
    public void setPlacedBy(@Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState state,
                            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity placer,
                            @Nonnull net.minecraft.world.item.ItemStack stack) {

        // Check if this is initial block placement vs lever state change
        boolean isInitialPlacement = placer != null && level.getBlockEntity(pos) == null;

        System.out.println("DEBUG Block: setPlacedBy called - Initial placement: " + isInitialPlacement +
                ", Placer: " + (placer != null ? placer.getName().getString() : "null"));

        if (isInitialPlacement) {
            System.out.println("DEBUG Block: Processing initial block placement");
            super.setPlacedBy(level, pos, state, placer, stack);

            if (level.getBlockEntity(pos) instanceof SwitchesLeverBlockEntity blockEntity) {
                System.out.println("DEBUG Block: Initializing new BlockEntity with default state");
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            }
        } else {
            System.out.println("DEBUG Block: Skipping setPlacedBy for state change to preserve BlockEntity");
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
     * ENHANCED DEBUG: Check current texture state with comprehensive output
     */
    public void debugTextureState(@Nonnull Level level, @Nonnull BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof SwitchesLeverBlockEntity blockEntity) {
            String instanceId = Integer.toHexString(System.identityHashCode(blockEntity));
            System.out.println("DEBUG Block: === TEXTURE STATE DEBUG ===");
            System.out.println("DEBUG Block: Position: " + pos);
            System.out.println("DEBUG Block: BlockEntity ID: " + instanceId);
            System.out.println("DEBUG Block: Base texture: " + blockEntity.getBaseTexture());
            System.out.println("DEBUG Block: Toggle texture: " + blockEntity.getToggleTexture());
            System.out.println("DEBUG Block: Base face: " + blockEntity.getBaseFaceSelection());
            System.out.println("DEBUG Block: Toggle face: " + blockEntity.getToggleFaceSelection());
            System.out.println("DEBUG Block: Inverted: " + blockEntity.isInverted());
            System.out.println("DEBUG Block: Has custom: " + blockEntity.hasCustomTextures());
            System.out.println("DEBUG Block: ========================");
        } else {
            System.out.println("DEBUG Block: No BlockEntity found at " + pos);
        }
    }
}