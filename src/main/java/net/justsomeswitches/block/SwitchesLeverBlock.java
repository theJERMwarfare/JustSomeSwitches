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
 * FINAL FIX: Switches Lever Block with immediate face selection capture before any state changes
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
    // FINAL FIX: IMMEDIATE FACE SELECTION CAPTURE
    // ========================================

    @Override
    @Nonnull
    public InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player,
                                 @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit) {

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        System.out.println("FINAL FIX: Immediate face selection capture before any state changes");

        // CRITICAL: Capture face selections IMMEDIATELY before any operations
        net.justsomeswitches.gui.FaceSelectionData.FaceOption capturedBaseFace = null;
        net.justsomeswitches.gui.FaceSelectionData.FaceOption capturedToggleFace = null;
        boolean capturedInverted = false;
        CompoundTag capturedSlotData = null;

        // Get BlockEntity and capture its current state IMMEDIATELY
        if (level.getBlockEntity(pos) instanceof SwitchesLeverBlockEntity blockEntity) {
            // IMMEDIATE CAPTURE - no delays, no other operations first
            capturedBaseFace = blockEntity.getBaseFaceSelection();
            capturedToggleFace = blockEntity.getToggleFaceSelection();
            capturedInverted = blockEntity.isInverted();

            // Also capture slot items immediately
            capturedSlotData = new CompoundTag();
            if (!blockEntity.getGuiToggleItem().isEmpty()) {
                capturedSlotData.put("gui_toggle_item", blockEntity.getGuiToggleItem().save(new CompoundTag()));
            }
            if (!blockEntity.getGuiBaseItem().isEmpty()) {
                capturedSlotData.put("gui_base_item", blockEntity.getGuiBaseItem().save(new CompoundTag()));
            }

            System.out.println("FINAL FIX: IMMEDIATE CAPTURE COMPLETE - Base: " + capturedBaseFace +
                    ", Toggle: " + capturedToggleFace + ", Inverted: " + capturedInverted);
        }

        // Get current powered state for sound and lever operation
        boolean currentlyPowered = state.getValue(BlockStateProperties.POWERED);
        boolean newPoweredState = !currentlyPowered;

        System.out.println("FINAL FIX: Lever state change - Current: " + currentlyPowered + " → New: " + newPoweredState);

        // Change the block state using minimal update flags to prevent BlockEntity destruction
        BlockState newState = state.setValue(BlockStateProperties.POWERED, newPoweredState);

        // Use minimal update flags - avoid BLOCK_UPDATE which can trigger BlockEntity recreation
        int updateFlags = Block.UPDATE_NEIGHBORS | Block.UPDATE_CLIENTS;
        level.setBlock(pos, newState, updateFlags);

        // IMMEDIATE RESTORATION: Apply captured data to BlockEntity after state change
        if (capturedBaseFace != null && level.getBlockEntity(pos) instanceof SwitchesLeverBlockEntity blockEntity) {
            System.out.println("FINAL FIX: Restoring captured face selections...");

            // Force immediate restoration without triggering change notifications
            blockEntity.forceSetFaceSelectionsWithoutNotification(capturedBaseFace, capturedToggleFace, capturedInverted);

            // Restore slot items if captured
            if (capturedSlotData != null) {
                net.minecraft.world.item.ItemStack toggleItem = capturedSlotData.contains("gui_toggle_item") ?
                        net.minecraft.world.item.ItemStack.of(capturedSlotData.getCompound("gui_toggle_item")) :
                        net.minecraft.world.item.ItemStack.EMPTY;

                net.minecraft.world.item.ItemStack baseItem = capturedSlotData.contains("gui_base_item") ?
                        net.minecraft.world.item.ItemStack.of(capturedSlotData.getCompound("gui_base_item")) :
                        net.minecraft.world.item.ItemStack.EMPTY;

                blockEntity.forceSetSlotItemsWithoutNotification(toggleItem, baseItem);
            }

            // Apply textures with restored face selections
            blockEntity.applyCurrentTextureSettings();

            // Verify restoration
            System.out.println("FINAL FIX: VERIFICATION - Restored Base: " + blockEntity.getBaseFaceSelection() +
                    ", Toggle: " + blockEntity.getToggleFaceSelection() + ", Inverted: " + blockEntity.isInverted());

            System.out.println("FINAL FIX: ✅ Face selection preservation through lever toggle COMPLETE");
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
    // STANDARD BLOCK UPDATES
    // ========================================

    @Override
    public void neighborChanged(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
                                @Nonnull Block neighborBlock, @Nonnull BlockPos neighborPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, isMoving);

        // Ensure texture persistence during neighbor updates
        if (level.getBlockEntity(pos) instanceof SwitchesLeverBlockEntity blockEntity) {
            if (blockEntity.hasCustomTextures()) {
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            }
        }
    }

    /**
     * Standard setPlacedBy without complex logic
     */
    @Override
    public void setPlacedBy(@Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState state,
                            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity placer,
                            @Nonnull net.minecraft.world.item.ItemStack stack) {

        super.setPlacedBy(level, pos, state, placer, stack);

        if (level.getBlockEntity(pos) instanceof SwitchesLeverBlockEntity blockEntity) {
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
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    /**
     * Debug helper
     */
    public void debugTextureState(@Nonnull Level level, @Nonnull BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof SwitchesLeverBlockEntity blockEntity) {
            System.out.println("DEBUG Block: === TEXTURE STATE DEBUG ===");
            System.out.println("DEBUG Block: Position: " + pos);
            System.out.println("DEBUG Block: Base texture: " + blockEntity.getBaseTexture());
            System.out.println("DEBUG Block: Toggle texture: " + blockEntity.getToggleTexture());
            System.out.println("DEBUG Block: Base face: " + blockEntity.getBaseFaceSelection());
            System.out.println("DEBUG Block: Toggle face: " + blockEntity.getToggleFaceSelection());
            System.out.println("DEBUG Block: Inverted: " + blockEntity.isInverted());
            System.out.println("DEBUG Block: Has custom: " + blockEntity.hasCustomTextures());
            System.out.println("DEBUG Block: =============================");
        } else {
            System.out.println("DEBUG Block: No BlockEntity found at " + pos);
        }
    }
}