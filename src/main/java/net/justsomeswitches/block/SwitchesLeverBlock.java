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
 * FRAMED BLOCKS SOLUTION: Switches Lever Block with simplified lever toggle - relies on robust NBT persistence
 * ---
 * Key changes:
 * - Removed manual preservation approach (save/restore methods)
 * - Simplified lever toggle to rely entirely on enhanced NBT persistence in BlockEntity
 * - Enhanced neighbor change handling for texture persistence
 * - Follows Framed Blocks pattern of separating texture management from lever mechanics
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
    // FRAMED BLOCKS SOLUTION: SIMPLIFIED LEVER TOGGLE RELYING ON ROBUST NBT PERSISTENCE
    // ========================================

    /**
     * FRAMED BLOCKS APPROACH: Simplified lever toggle that relies entirely on enhanced NBT persistence
     * ---
     * Key improvements:
     * - No manual save/restore - relies on robust NBT persistence in BlockEntity
     * - Standard setBlock() call with enhanced update flags
     * - BlockEntity NBT persistence automatically survives the state change
     * - Enhanced post-toggle validation and refresh
     */
    @Override
    @Nonnull
    public InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player,
                                 @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit) {

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        System.out.println("FRAMED SOLUTION: Simplified lever toggle relying on robust NBT persistence");

        // Get current powered state
        boolean currentlyPowered = state.getValue(BlockStateProperties.POWERED);
        boolean newPoweredState = !currentlyPowered;

        System.out.println("FRAMED SOLUTION: Lever state change - Current: " + currentlyPowered + " → New: " + newPoweredState);

        // FRAMED BLOCKS APPROACH: Trust the robust NBT persistence - no manual save/restore needed
        System.out.println("FRAMED SOLUTION: Relying on enhanced NBT persistence to survive setBlock()");

        // Log pre-toggle state for verification
        if (level.getBlockEntity(pos) instanceof SwitchesLeverBlockEntity blockEntity) {
            System.out.println("FRAMED SOLUTION: PRE-TOGGLE - Base: " + blockEntity.getBaseFaceSelection() +
                    ", Toggle: " + blockEntity.getToggleFaceSelection() + ", Inverted: " + blockEntity.isInverted());
        }

        // Change the block state with enhanced update flags for better persistence
        BlockState newState = state.setValue(BlockStateProperties.POWERED, newPoweredState);
        level.setBlock(pos, newState, Block.UPDATE_ALL | Block.UPDATE_NEIGHBORS | Block.UPDATE_CLIENTS);

        // FRAMED BLOCKS PATTERN: Post-toggle validation and refresh
        if (level.getBlockEntity(pos) instanceof SwitchesLeverBlockEntity blockEntity) {
            System.out.println("FRAMED SOLUTION: POST-TOGGLE - Base: " + blockEntity.getBaseFaceSelection() +
                    ", Toggle: " + blockEntity.getToggleFaceSelection() + ", Inverted: " + blockEntity.isInverted());

            // Force immediate visual update to ensure textures are applied correctly
            blockEntity.applyCurrentTextureSettings();

            System.out.println("FRAMED SOLUTION: ✅ Lever toggle complete - NBT persistence should have preserved face selections");
        } else {
            System.out.println("FRAMED SOLUTION: ⚠️ Warning - BlockEntity not found after lever toggle");
        }

        // Play standard lever click sound
        level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS,
                0.3F, currentlyPowered ? 0.5F : 0.6F);

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void animateTick(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull net.minecraft.util.RandomSource randomSource) {
        // Intentionally empty to suppress particle effects
    }

    // ========================================
    // FRAMED BLOCKS PATTERN: ENHANCED NEIGHBOR UPDATES FOR TEXTURE PERSISTENCE
    // ========================================

    /**
     * FRAMED BLOCKS PATTERN: Enhanced neighbor change handling to maintain texture persistence
     */
    @Override
    public void neighborChanged(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
                                @Nonnull Block neighborBlock, @Nonnull BlockPos neighborPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, isMoving);

        // Enhanced texture persistence during neighbor updates
        if (level.getBlockEntity(pos) instanceof SwitchesLeverBlockEntity blockEntity) {
            if (blockEntity.hasCustomTextures()) {
                System.out.println("FRAMED SOLUTION: Neighbor change - maintaining texture persistence");

                // Force immediate NBT persistence and visual update
                blockEntity.forceImmediateNBTPersistence();

                // Send update to clients
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            }
        }
    }

    /**
     * FRAMED BLOCKS PATTERN: Enhanced placement with immediate persistence setup
     */
    @Override
    public void setPlacedBy(@Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState state,
                            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity placer,
                            @Nonnull net.minecraft.world.item.ItemStack stack) {

        super.setPlacedBy(level, pos, state, placer, stack);

        if (level.getBlockEntity(pos) instanceof SwitchesLeverBlockEntity blockEntity) {
            System.out.println("FRAMED SOLUTION: Block placed - setting up persistence");

            // Initialize robust persistence
            blockEntity.forceImmediateNBTPersistence();

            // Send initial update to clients
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
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
                // Drop any stored texture blocks when switch is broken
                switchEntity.dropStoredTextures(level, pos);
                System.out.println("FRAMED SOLUTION: Block removed - texture items dropped");
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    /**
     * Enhanced debug helper for testing persistence
     */
    public void debugTextureState(@Nonnull Level level, @Nonnull BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof SwitchesLeverBlockEntity blockEntity) {
            System.out.println("FRAMED SOLUTION: === TEXTURE STATE DEBUG ===");
            System.out.println("FRAMED SOLUTION: Position: " + pos);
            System.out.println("FRAMED SOLUTION: Base texture: " + blockEntity.getBaseTexture());
            System.out.println("FRAMED SOLUTION: Toggle texture: " + blockEntity.getToggleTexture());
            System.out.println("FRAMED SOLUTION: Base face: " + blockEntity.getBaseFaceSelection());
            System.out.println("FRAMED SOLUTION: Toggle face: " + blockEntity.getToggleFaceSelection());
            System.out.println("FRAMED SOLUTION: Inverted: " + blockEntity.isInverted());
            System.out.println("FRAMED SOLUTION: Has custom: " + blockEntity.hasCustomTextures());
            System.out.println("FRAMED SOLUTION: ===============================");
        } else {
            System.out.println("FRAMED SOLUTION: No BlockEntity found at " + pos);
        }
    }
}