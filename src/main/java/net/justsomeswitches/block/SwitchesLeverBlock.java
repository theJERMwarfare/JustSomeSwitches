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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * FIXED: Enhanced Switches Lever Block with BlockEntity Recreation Prevention
 * ---
 * CRITICAL FIX: Prevent BlockEntity recreation during state changes that causes face selection loss
 */
public class SwitchesLeverBlock extends LeverBlock implements EntityBlock {

    public SwitchesLeverBlock(Properties properties) {
        super(properties);
        System.out.println("DEBUG Block: SwitchesLeverBlock created with EntityBlock support");
    }

    // ========================================
    // BLOCK ENTITY CREATION AND MANAGEMENT
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
        if (blockEntityType == JustSomeSwitchesModBlockEntities.SWITCHES_LEVER.get()) {
            return level.isClientSide ?
                    (lvl, pos, st, be) -> SwitchesLeverBlockEntity.clientTick(lvl, pos, st, (SwitchesLeverBlockEntity) be) :
                    (lvl, pos, st, be) -> SwitchesLeverBlockEntity.serverTick(lvl, pos, st, (SwitchesLeverBlockEntity) be);
        }
        return null;
    }

    // ========================================
    // LEVER INTERACTION WITH FACE SELECTION PRESERVATION
    // ========================================

    @Override
    @Nonnull
    public InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player,
                                 @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit) {

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        System.out.println("DEBUG Block: Switch lever used at " + pos + " by player " + player.getName().getString());

        // FIXED: Preserve face selections before state change
        if (level.getBlockEntity(pos) instanceof SwitchesLeverBlockEntity blockEntity) {
            blockEntity.preserveFaceSelectionsForStateChange();
        }

        // Toggle the powered state (unchanged lever behavior)
        boolean currentlyPowered = state.getValue(BlockStateProperties.POWERED);
        BlockState newState = state.setValue(BlockStateProperties.POWERED, !currentlyPowered);

        level.setBlock(pos, newState, 3);

        System.out.println("DEBUG Block: Lever state changed from " + currentlyPowered + " to " + !currentlyPowered);

        // Play standard lever click sound with pitch variation
        level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS,
                0.3F, currentlyPowered ? 0.5F : 0.6F);

        // FIXED: Force model update with preserved face selections after state change
        if (level.getBlockEntity(pos) instanceof SwitchesLeverBlockEntity blockEntity) {
            System.out.println("DEBUG Block: Triggering model update preserving face selections");

            // Force block update to refresh model rendering with preserved face selections
            level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), Block.UPDATE_CLIENTS);

            // Force ModelData refresh without triggering NBT reload
            blockEntity.requestModelDataUpdate();

            System.out.println("DEBUG Block: Model update completed with face selection preservation");
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
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
    // ENHANCED BLOCK UPDATES WITH FACE SELECTION PRESERVATION
    // ========================================

    @Override
    public void neighborChanged(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
                                @Nonnull Block neighborBlock, @Nonnull BlockPos neighborPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, isMoving);

        // FIXED: Ensure model updates preserve face selections
        if (level.getBlockEntity(pos) instanceof SwitchesLeverBlockEntity blockEntity) {
            if (blockEntity.hasCustomTextures()) {
                System.out.println("DEBUG Block: Neighbor changed, updating custom texture model with face preservation");

                // FIXED: Use UPDATE_CLIENTS to preserve face selections
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            }
        }
    }

    /**
     * CRITICAL FIX: Prevent BlockEntity recreation during lever operations
     */
    @Override
    public void setPlacedBy(@Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState state,
                            @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity placer,
                            @Nonnull net.minecraft.world.item.ItemStack stack) {
        // FIXED: Only call super.setPlacedBy for INITIAL placement, not lever state changes

        // Check if this is initial block placement vs lever state change
        boolean isInitialPlacement = placer != null && level.getBlockEntity(pos) == null;

        if (isInitialPlacement) {
            System.out.println("DEBUG Block: INITIAL placement of switch lever at " + pos + " by " +
                    placer.getName().getString());

            super.setPlacedBy(level, pos, state, placer, stack);

            // Initialize BlockEntity for new placement only
            if (level.getBlockEntity(pos) instanceof SwitchesLeverBlockEntity blockEntity) {
                System.out.println("DEBUG Block: Initializing NEW BlockEntity with default textures");

                // FIXED: Use UPDATE_CLIENTS for initial setup to avoid unnecessary NBT operations
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            }
        } else {
            System.out.println("DEBUG Block: SKIPPED setPlacedBy for lever state change - preserving existing BlockEntity");
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