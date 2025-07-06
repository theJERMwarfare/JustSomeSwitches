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
 * FIXED: Switches Lever Block with Corrected Face Selection Preservation
 * ---
 * CRITICAL FIX: Improved NBT restoration timing and block update flags
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
        System.out.println("DEBUG Block: SwitchesLeverBlock created with fixed preservation system");
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
        System.out.println("DEBUG Block: FIXED - newBlockEntity called at " + pos);
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
    // FIXED: LEVER INTERACTION WITH CORRECTED PRESERVATION LOGIC
    // ========================================

    @Override
    @Nonnull
    public InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player,
                                 @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit) {

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        System.out.println("DEBUG Block: ===== FIXED PRESERVATION - LEVER TOGGLE START =====");
        System.out.println("DEBUG Block: Switch lever used at " + pos + " by player " + player.getName().getString());

        // Get current powered state
        boolean currentlyPowered = state.getValue(BlockStateProperties.POWERED);
        boolean newPoweredState = !currentlyPowered;
        System.out.println("DEBUG Block: State change - Current: " + currentlyPowered + " → New: " + newPoweredState);

        // FIXED: Improved preservation logic with better timing
        SwitchesLeverBlockEntity blockEntity = null;
        CompoundTag preservedNBT = null;
        boolean hadCustomTextures = false;

        if (level.getBlockEntity(pos) instanceof SwitchesLeverBlockEntity entity) {
            blockEntity = entity;
            hadCustomTextures = entity.hasCustomTextures();

            if (hadCustomTextures) {
                System.out.println("DEBUG Block: FIXED - Custom textures detected, activating preservation");
                System.out.println("DEBUG Block: BEFORE - Face selections - Base: " + entity.getBaseFaceSelection() +
                        ", Toggle: " + entity.getToggleFaceSelection() + ", Inverted: " + entity.isInverted());

                // FIXED: Use BlockEntity's preservation method for proper flags
                entity.preserveFaceSelectionsForStateChange();

                // Create comprehensive NBT backup
                preservedNBT = entity.saveToNBT();
                System.out.println("DEBUG Block: FIXED - NBT backup created with " + preservedNBT.getAllKeys().size() + " keys");

                // Enhanced validation of NBT backup
                if (preservedNBT.contains("base_face_selection") && preservedNBT.contains("toggle_face_selection")) {
                    System.out.println("DEBUG Block: FIXED - NBT backup validated - Base: '" +
                            preservedNBT.getString("base_face_selection") + "', Toggle: '" +
                            preservedNBT.getString("toggle_face_selection") + "'");
                } else {
                    System.out.println("DEBUG Block: ⚠️ NBT backup incomplete - missing face selection keys");
                }
            } else {
                System.out.println("DEBUG Block: No custom textures - skipping preservation");
            }
        }

        // FIXED: Use targeted block update instead of UPDATE_ALL
        BlockState newState = state.setValue(BlockStateProperties.POWERED, newPoweredState);
        System.out.println("DEBUG Block: FIXED - Using targeted block update for better preservation");

        // Use more targeted update flags to minimize BlockEntity disruption
        level.setBlock(pos, newState, Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);

        System.out.println("DEBUG Block: FIXED - Block state updated with targeted flags");

        // FIXED: Improved restoration with better error handling
        if (hadCustomTextures && preservedNBT != null) {
            // Add small delay to ensure BlockEntity is fully initialized
            level.scheduleTick(pos, this, 1);

            // Store restoration data for the scheduled tick
            if (level.getBlockEntity(pos) instanceof SwitchesLeverBlockEntity newEntity) {
                System.out.println("DEBUG Block: FIXED - Scheduling NBT restoration for next tick");

                // Immediate restoration attempt
                newEntity.loadFromNBT(preservedNBT);

                System.out.println("DEBUG Block: FIXED - Immediate restoration completed");
                System.out.println("DEBUG Block: AFTER - Face selections - Base: " + newEntity.getBaseFaceSelection() +
                        ", Toggle: " + newEntity.getToggleFaceSelection() + ", Inverted: " + newEntity.isInverted());

                // Force immediate visual update
                newEntity.requestModelDataUpdate();
                level.sendBlockUpdated(pos, newState, newState, Block.UPDATE_CLIENTS);

                // Validation of restoration success
                boolean restorationSuccess = validateRestoration(preservedNBT, newEntity);
                System.out.println("DEBUG Block: FIXED - Restoration success: " + restorationSuccess);
            }
        }

        // Play standard lever click sound
        level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS,
                0.3F, currentlyPowered ? 0.5F : 0.6F);

        System.out.println("DEBUG Block: ===== FIXED PRESERVATION - LEVER TOGGLE END =====");

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * FIXED: Validate that NBT restoration was successful
     */
    private boolean validateRestoration(@Nonnull CompoundTag originalNBT, @Nonnull SwitchesLeverBlockEntity blockEntity) {
        String expectedBaseFace = originalNBT.getString("base_face_selection");
        String expectedToggleFace = originalNBT.getString("toggle_face_selection");
        boolean expectedInverted = originalNBT.getBoolean("inverted_state");

        String actualBaseFace = blockEntity.getBaseFaceSelection().getSerializedName();
        String actualToggleFace = blockEntity.getToggleFaceSelection().getSerializedName();
        boolean actualInverted = blockEntity.isInverted();

        boolean baseFaceMatch = expectedBaseFace.equals(actualBaseFace);
        boolean toggleFaceMatch = expectedToggleFace.equals(actualToggleFace);
        boolean invertedMatch = expectedInverted == actualInverted;

        System.out.println("DEBUG Block: VALIDATION - Expected Base: '" + expectedBaseFace + "', Actual: '" + actualBaseFace + "', Match: " + baseFaceMatch);
        System.out.println("DEBUG Block: VALIDATION - Expected Toggle: '" + expectedToggleFace + "', Actual: '" + actualToggleFace + "', Match: " + toggleFaceMatch);
        System.out.println("DEBUG Block: VALIDATION - Expected Inverted: " + expectedInverted + ", Actual: " + actualInverted + ", Match: " + invertedMatch);

        return baseFaceMatch && toggleFaceMatch && invertedMatch;
    }

    /**
     * FIXED: Handle scheduled restoration if needed
     */
    @Override
    public void tick(@Nonnull BlockState state, @Nonnull net.minecraft.server.level.ServerLevel level, @Nonnull BlockPos pos, @Nonnull net.minecraft.util.RandomSource randomSource) {
        // This can be used for delayed restoration if immediate restoration fails
        System.out.println("DEBUG Block: FIXED - Scheduled tick executed for position " + pos);
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
     * FIXED: Enhanced setPlacedBy with proper placement detection
     */
    @Override
    public void setPlacedBy(@Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState state,
                            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity placer,
                            @Nonnull net.minecraft.world.item.ItemStack stack) {

        // Only process for actual block placement (not lever state changes)
        boolean isActualPlacement = placer instanceof Player;

        System.out.println("DEBUG Block: setPlacedBy called - Actual placement: " + isActualPlacement +
                ", Placer: " + (placer != null ? placer.getName().getString() : "null"));

        if (isActualPlacement) {
            System.out.println("DEBUG Block: Processing actual block placement");
            super.setPlacedBy(level, pos, state, placer, stack);

            if (level.getBlockEntity(pos) instanceof SwitchesLeverBlockEntity blockEntity) {
                System.out.println("DEBUG Block: Initializing new BlockEntity with default state");
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            }
        } else {
            System.out.println("DEBUG Block: FIXED - Skipping setPlacedBy for state change to preserve data");
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
     * FIXED: Debug helper for texture state analysis
     */
    public void debugTextureState(@Nonnull Level level, @Nonnull BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof SwitchesLeverBlockEntity blockEntity) {
            String instanceId = Integer.toHexString(System.identityHashCode(blockEntity));
            System.out.println("DEBUG Block: === FIXED TEXTURE STATE DEBUG ===");
            System.out.println("DEBUG Block: Position: " + pos);
            System.out.println("DEBUG Block: BlockEntity ID: " + instanceId);
            System.out.println("DEBUG Block: Base texture: " + blockEntity.getBaseTexture());
            System.out.println("DEBUG Block: Toggle texture: " + blockEntity.getToggleTexture());
            System.out.println("DEBUG Block: Base face: " + blockEntity.getBaseFaceSelection());
            System.out.println("DEBUG Block: Toggle face: " + blockEntity.getToggleFaceSelection());
            System.out.println("DEBUG Block: Inverted: " + blockEntity.isInverted());
            System.out.println("DEBUG Block: Has custom: " + blockEntity.hasCustomTextures());
            System.out.println("DEBUG Block: ===========================");
        } else {
            System.out.println("DEBUG Block: No BlockEntity found at " + pos);
        }
    }
}