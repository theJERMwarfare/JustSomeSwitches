package net.justsomeswitches.block;

import net.justsomeswitches.blockentity.SwitchBlockEntity;
import net.justsomeswitches.config.SwitchesCommonConfig;
import net.justsomeswitches.init.JustSomeSwitchesModBlockEntities;
import net.justsomeswitches.util.TightSwitchShapes;
import net.justsomeswitches.util.TightSwitchShapes.SwitchModelType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.item.context.BlockPlaceContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Base class for all advanced switch blocks with texture customization.
 * Provides shared placement, shape, tick, and block entity logic.
 * Subclasses only need to pass their SwitchModelType via constructor.
 */
public abstract class AbstractSwitchBlock extends LeverBlock implements EntityBlock, ISwitchBlock, SimpleWaterloggedBlock {
    private final SwitchModelType switchModelType;
    private static final ThreadLocal<String> PENDING_WALL_ORIENTATION = new ThreadLocal<>();
    private static final VoxelShape FLOOR_NORTH_SOUTH = Block.box(5.0, 0.0, 3.0, 11.0, 6.0, 13.0);
    private static final VoxelShape FLOOR_EAST_WEST = Block.box(3.0, 0.0, 5.0, 13.0, 6.0, 11.0);
    private static final VoxelShape CEILING_NORTH_SOUTH = Block.box(5.0, 10.0, 3.0, 11.0, 16.0, 13.0);
    private static final VoxelShape CEILING_EAST_WEST = Block.box(3.0, 10.0, 5.0, 13.0, 16.0, 11.0);
    private static final VoxelShape WALL_NORTH = Block.box(5.0, 3.0, 10.0, 11.0, 13.0, 16.0);
    private static final VoxelShape WALL_SOUTH = Block.box(5.0, 3.0, 0.0, 11.0, 13.0, 6.0);
    private static final VoxelShape WALL_WEST = Block.box(10.0, 3.0, 5.0, 16.0, 13.0, 11.0);
    private static final VoxelShape WALL_EAST = Block.box(0.0, 3.0, 5.0, 6.0, 13.0, 11.0);

    protected AbstractSwitchBlock(Properties properties, SwitchModelType switchModelType) {
        super(properties);
        this.switchModelType = switchModelType;
        this.registerDefaultState(this.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, false));
    }
    @Override
    protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BlockStateProperties.WATERLOGGED);
    }
    @Override
    public SwitchModelType getSwitchModelType() {
        return switchModelType;
    }

    /**
     * Returns wall shape adjusted for lever orientation.
     */
    private VoxelShape getRotatedWallShape(Direction wallFace, String wallOrientation) {
        VoxelShape baseShape = switch (wallFace) {
            case NORTH -> WALL_NORTH;
            case SOUTH -> WALL_SOUTH;
            case WEST -> WALL_WEST;
            case EAST -> WALL_EAST;
            default -> WALL_NORTH;
        };
        
        if ("left".equals(wallOrientation) || "right".equals(wallOrientation)) {
            return switch (wallFace) {
                case NORTH -> Block.box(3.0, 5.0, 10.0, 13.0, 11.0, 16.0);
                case SOUTH -> Block.box(3.0, 5.0, 0.0, 13.0, 11.0, 6.0);
                case WEST -> Block.box(10.0, 5.0, 3.0, 16.0, 11.0, 13.0);
                case EAST -> Block.box(0.0, 5.0, 3.0, 6.0, 11.0, 13.0);
                default -> baseShape;
            };
        }
        
        if ("top".equals(wallOrientation)) {
            return switch (wallFace) {
                case NORTH -> Block.box(5.0, 3.0, 10.0, 11.0, 13.0, 16.0);
                case SOUTH -> Block.box(5.0, 3.0, 0.0, 11.0, 13.0, 6.0);
                case WEST -> Block.box(10.0, 3.0, 5.0, 16.0, 13.0, 11.0);
                case EAST -> Block.box(0.0, 3.0, 5.0, 6.0, 13.0, 11.0);
                default -> baseShape;
            };
        }
        return baseShape;
    }
    @Override
    @Nullable
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new SwitchBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state,
                                                                  @Nonnull BlockEntityType<T> blockEntityType) {
        if (blockEntityType == JustSomeSwitchesModBlockEntities.SWITCHES_LEVER.get()) {
            if (level.isClientSide()) {
                return (level1, pos, state1, blockEntity) -> {
                    if (blockEntity instanceof SwitchBlockEntity switchEntity) {
                        SwitchBlockEntity.clientTick(level1, pos, state1, switchEntity);
                    }
                };
            } else {
                return (level1, pos, state1, blockEntity) -> {
                    if (blockEntity instanceof SwitchBlockEntity switchEntity) {
                        SwitchBlockEntity.serverTick(level1, pos, state1, switchEntity);
                    }
                };
            }
        }
        return null;
    }

    /**
     * Light emission level.
     */
    @Override
    public int getLightEmission(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos) {
        return 0;
    }

    @Override
    public boolean propagatesSkylightDown(@Nonnull BlockState state, @Nonnull BlockGetter reader, @Nonnull BlockPos pos) {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getLightBlock(@Nonnull BlockState state, @Nonnull BlockGetter worldIn, @Nonnull BlockPos pos) {
        return 0;
    }

    @Override
    @Nonnull
    @SuppressWarnings("deprecation")
    public RenderShape getRenderShape(@Nonnull BlockState state) {
        return RenderShape.MODEL;
    }
    @Override
    @Nullable
    public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        return getAdvancedPlacementState(context);
    }
    @Override
    @SuppressWarnings("deprecation")
    public boolean canBeReplaced(@Nonnull BlockState state, @Nonnull BlockPlaceContext useContext) {
        return false;
    }

    /**
     * Validates lever placement.
     */
    @Override
    public boolean canSurvive(@Nonnull BlockState state, @Nonnull net.minecraft.world.level.LevelReader level, @Nonnull BlockPos pos) {
        AttachFace attachFace = state.getValue(BlockStateProperties.ATTACH_FACE);
        
        switch (attachFace) {
            case WALL -> {
                Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
                BlockPos attachedPos = pos.relative(facing.getOpposite());
                BlockState attachedBlock = level.getBlockState(attachedPos);
                
                return attachedBlock.isFaceSturdy(level, attachedPos, facing);
            }
            
            case FLOOR -> {
                BlockPos belowPos = pos.below();
                BlockState belowBlock = level.getBlockState(belowPos);
                
                return belowBlock.isFaceSturdy(level, belowPos, Direction.UP);
            }
            
            case CEILING -> {
                BlockPos abovePos = pos.above();
                BlockState aboveBlock = level.getBlockState(abovePos);
                
                return aboveBlock.isFaceSturdy(level, abovePos, Direction.DOWN);
            }
        }
        
        return false;
    }

    /**
     * Determines lever placement based on click location.
     */
    private BlockState getAdvancedPlacementState(@Nonnull BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        Vec3 clickLocation = context.getClickLocation();
        Vec3 relativeHit = getRelativeHitLocation(clickLocation, clickedFace);
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        boolean waterlogged = fluidState.is(Fluids.WATER);
        BlockState proposedState = switch (clickedFace) {
            case UP -> getFloorPlacement(relativeHit, context);
            case DOWN -> getCeilingPlacement(relativeHit, context);
            case NORTH, SOUTH, EAST, WEST -> getWallPlacement(clickedFace, relativeHit);
        };
        proposedState = proposedState.setValue(BlockStateProperties.WATERLOGGED, waterlogged);
        if (proposedState.canSurvive(context.getLevel(), context.getClickedPos())) {
            return proposedState;
        }
        BlockState fallback = super.getStateForPlacement(context);
        if (fallback != null) {
            fallback = fallback.setValue(BlockStateProperties.WATERLOGGED, waterlogged);
        }
        return fallback;
    }

    /**
     * Calculates relative hit position within block face.
     */
    private Vec3 getRelativeHitLocation(Vec3 clickLocation, Direction clickedFace) {
        double fracX = clickLocation.x - Math.floor(clickLocation.x);
        double fracY = clickLocation.y - Math.floor(clickLocation.y);
        double fracZ = clickLocation.z - Math.floor(clickLocation.z);
        
        return switch (clickedFace) {
            case UP -> new Vec3(fracX, fracZ, 0);
            case DOWN -> new Vec3(fracX, 1.0 - fracZ, 0);
            case NORTH -> new Vec3(fracX, 1.0 - fracY, 0);
            case SOUTH -> new Vec3(fracX, 1.0 - fracY, 0);
            case WEST -> new Vec3(fracZ, 1.0 - fracY, 0);
            case EAST -> new Vec3(fracZ, 1.0 - fracY, 0);
        };
    }

    /**
     * Determines floor placement orientation.
     */
    private BlockState getFloorPlacement(Vec3 relativeHit, BlockPlaceContext context) {
        final double EDGE_THRESHOLD = 4.0 / 16.0;
        if (relativeHit.y < EDGE_THRESHOLD) {
            return this.defaultBlockState()
                .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                .setValue(BlockStateProperties.POWERED, false);
        } else if (relativeHit.y > (1.0 - EDGE_THRESHOLD)) {
            return this.defaultBlockState()
                .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH)
                .setValue(BlockStateProperties.POWERED, false);
        } else if (relativeHit.x < EDGE_THRESHOLD) {
            return this.defaultBlockState()
                .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST)
                .setValue(BlockStateProperties.POWERED, false);
        } else if (relativeHit.x > (1.0 - EDGE_THRESHOLD)) {
            return this.defaultBlockState()
                .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST)
                .setValue(BlockStateProperties.POWERED, false);
        } else {
            return this.defaultBlockState()
                .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection())
                .setValue(BlockStateProperties.POWERED, false);
        }
    }

    /**
     * Determines ceiling placement orientation.
     */
    private BlockState getCeilingPlacement(Vec3 relativeHit, BlockPlaceContext context) {
        final double EDGE_THRESHOLD = 4.0 / 16.0;
        if (relativeHit.y < EDGE_THRESHOLD) {
            return this.defaultBlockState()
                .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.CEILING)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                .setValue(BlockStateProperties.POWERED, false);
        } else if (relativeHit.y > (1.0 - EDGE_THRESHOLD)) {

            return this.defaultBlockState()
                .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.CEILING)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH)
                .setValue(BlockStateProperties.POWERED, false);
        } else if (relativeHit.x < EDGE_THRESHOLD) {

            return this.defaultBlockState()
                .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.CEILING)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST)
                .setValue(BlockStateProperties.POWERED, false);
        } else if (relativeHit.x > (1.0 - EDGE_THRESHOLD)) {
            return this.defaultBlockState()
                .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.CEILING)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST)
                .setValue(BlockStateProperties.POWERED, false);
        } else {
            Direction playerFacing = context.getHorizontalDirection();
            
            Direction leverFacing = switch (playerFacing) {
                case EAST -> Direction.WEST;
                case WEST -> Direction.EAST;
                case NORTH, SOUTH -> playerFacing;
                default -> playerFacing;
            };
            return this.defaultBlockState()
                .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.CEILING)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, leverFacing)
                .setValue(BlockStateProperties.POWERED, false);
        }
    }

    /**
     * Determines wall placement orientation.
     */
    private BlockState getWallPlacement(Direction clickedFace, Vec3 relativeHit) {
        final double EDGE_THRESHOLD = 4.0 / 16.0;
        
        String leverOrientation;
        if (relativeHit.y < EDGE_THRESHOLD) {
            leverOrientation = "bottom";
        } else if (relativeHit.y > (1.0 - EDGE_THRESHOLD)) {
            leverOrientation = "top";
        } else if (relativeHit.x < EDGE_THRESHOLD) {
            leverOrientation = "left";
        } else if (relativeHit.x > (1.0 - EDGE_THRESHOLD)) {
            leverOrientation = "right";
        } else {
            leverOrientation = "center";
        }
        
        PENDING_WALL_ORIENTATION.set(leverOrientation);
        
        return this.defaultBlockState()
            .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.WALL)
            .setValue(BlockStateProperties.HORIZONTAL_FACING, clickedFace)
            .setValue(BlockStateProperties.POWERED, false);
    }
    @Override
    @Nonnull
    public VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
        AttachFace attachFace = state.getValue(BlockStateProperties.ATTACH_FACE);
        Direction direction = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        if (SwitchesCommonConfig.isTightHitboxesSwitches()) {
            boolean powered = state.getValue(BlockStateProperties.POWERED);
            if (attachFace == AttachFace.WALL && level.getBlockEntity(pos) instanceof SwitchBlockEntity be) {
                String wallOrientation = be.getWallOrientation();
                if (!wallOrientation.isEmpty()) {
                    return TightSwitchShapes.getTightSwitchesShape(switchModelType, attachFace, direction, powered, wallOrientation);
                }
            }
            return TightSwitchShapes.getTightSwitchesShape(switchModelType, attachFace, direction, powered, "");
        }
        if (attachFace == AttachFace.WALL && level.getBlockEntity(pos) instanceof SwitchBlockEntity blockEntity) {
            String wallOrientation = blockEntity.getWallOrientation();
            if (!wallOrientation.isEmpty()) {
                return getRotatedWallShape(direction, wallOrientation);
            }
        }
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
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof SwitchBlockEntity blockEntity) {
            blockEntity.protectNBTDuringStateChange();
        }

        boolean currentlyPowered = state.getValue(BlockStateProperties.POWERED);
        BlockState newState = state.setValue(BlockStateProperties.POWERED, !currentlyPowered);
        level.setBlock(pos, newState, Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS,
                0.3F, currentlyPowered ? 0.5F : 0.6F);
        
        level.updateNeighborsAt(pos, this);
        Direction attachedDirection = getAttachedDirection(newState);
        level.updateNeighborsAt(pos.relative(attachedDirection), this);
        return InteractionResult.CONSUME;
    }
    @Override
    @SuppressWarnings("deprecation")
    public void tick(@Nonnull BlockState state, @Nonnull net.minecraft.server.level.ServerLevel level, @Nonnull BlockPos pos, @Nonnull net.minecraft.util.RandomSource random) {
        if (level.getBlockEntity(pos) instanceof SwitchBlockEntity blockEntity) {
            blockEntity.endNBTProtection();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            blockEntity.requestModelDataUpdate();
        }
    }

    @Override
    public void animateTick(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull net.minecraft.util.RandomSource randomSource) {
        // No particles for switch blocks - empty method intentional
    }
    @Override
    @Nonnull
    @SuppressWarnings("deprecation")
    public FluidState getFluidState(@Nonnull BlockState state) {
        return state.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }
    @Override
    @Nonnull
    public BlockState updateShape(@Nonnull BlockState state, @Nonnull Direction direction, @Nonnull BlockState neighborState,
                                  @Nonnull LevelAccessor level, @Nonnull BlockPos pos, @Nonnull BlockPos neighborPos) {
        if (state.getValue(BlockStateProperties.WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
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
    @Override
    public void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
                         @Nonnull BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof SwitchBlockEntity switchEntity) {
                switchEntity.dropStoredTextures(level, pos);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
                                @Nonnull Block neighborBlock, @Nonnull BlockPos neighborPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, isMoving);
        if (level.getBlockEntity(pos) instanceof SwitchBlockEntity blockEntity) {
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
        if (level.getBlockEntity(pos) instanceof SwitchBlockEntity blockEntity) {
            String wallOrientation = PENDING_WALL_ORIENTATION.get();
            if (wallOrientation != null && state.getValue(BlockStateProperties.ATTACH_FACE) == AttachFace.WALL) {
                blockEntity.setWallOrientation(wallOrientation);
                PENDING_WALL_ORIENTATION.remove();
            }
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }
}