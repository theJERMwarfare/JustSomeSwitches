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
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.item.context.BlockPlaceContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Enhanced Switches Lever Block with advanced placement system and NBT persistence.
 * Features intelligent edge-based placement detection and custom texture management.
 */
public class SwitchesLeverBlock extends LeverBlock implements EntityBlock {

    // ThreadLocal to store wall orientation during placement
    private static final ThreadLocal<String> PENDING_WALL_ORIENTATION = new ThreadLocal<>();

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

    /**
     * Get rotated wall shape based on wall orientation
     * 
     * @param wallFace the direction the wall faces
     * @param wallOrientation the orientation on the wall surface (left/right/top/bottom/center)
     * @return appropriately rotated VoxelShape
     */
    private VoxelShape getRotatedWallShape(Direction wallFace, String wallOrientation) {
        // Get base shape for the wall face
        VoxelShape baseShape = switch (wallFace) {
            case NORTH -> WALL_NORTH;
            case SOUTH -> WALL_SOUTH;
            case WEST -> WALL_WEST;
            case EAST -> WALL_EAST;
            default -> WALL_NORTH;
        };
        
        // For left/right orientations, return rotated bounding box
        // This provides proper collision detection for rotated visual models
        if ("left".equals(wallOrientation) || "right".equals(wallOrientation)) {
            // Create rotated bounding box for left/right orientations
            return switch (wallFace) {
                case NORTH -> Block.box(3.0, 5.0, 10.0, 13.0, 11.0, 16.0); // Rotated horizontally
                case SOUTH -> Block.box(3.0, 5.0, 0.0, 13.0, 11.0, 6.0);   // Rotated horizontally
                case WEST -> Block.box(10.0, 5.0, 3.0, 16.0, 11.0, 13.0);  // Rotated horizontally
                case EAST -> Block.box(0.0, 5.0, 3.0, 6.0, 11.0, 13.0);    // Rotated horizontally
                default -> baseShape;
            };
        }
        
        // For top orientation (which rotates 180°), return vertically flipped bounding box
        if ("top".equals(wallOrientation)) {
            return switch (wallFace) {
                case NORTH -> Block.box(5.0, 3.0, 10.0, 11.0, 13.0, 16.0); // Standard shape (180° flip doesn't change)
                case SOUTH -> Block.box(5.0, 3.0, 0.0, 11.0, 13.0, 6.0);   // Standard shape (180° flip doesn't change)
                case WEST -> Block.box(10.0, 3.0, 5.0, 16.0, 13.0, 11.0);  // Standard shape (180° flip doesn't change)
                case EAST -> Block.box(0.0, 3.0, 5.0, 6.0, 13.0, 11.0);    // Standard shape (180° flip doesn't change)
                default -> baseShape;
            };
        }
        
        // For bottom and center orientations, return standard shape
        return baseShape;
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
    // ADVANCED PLACEMENT SYSTEM WITH POSITION OVERRIDE
    // ========================================

    @Override
    @Nullable
    public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        return getAdvancedPlacementState(context);
    }
    
    @Override
    public boolean canBeReplaced(@Nonnull BlockState state, @Nonnull BlockPlaceContext useContext) {
        return false; // Prevent accidental replacement
    }

    /**
     * CRITICAL OVERRIDE: This method controls WHERE the block gets placed
     * Override this to prevent vanilla placement logic from taking over
     */
    @Override
    public boolean canSurvive(@Nonnull BlockState state, @Nonnull net.minecraft.world.level.LevelReader level, @Nonnull BlockPos pos) {
        // For wall-mounted levers, ensure they can survive on the wall face
        if (state.getValue(BlockStateProperties.ATTACH_FACE) == AttachFace.WALL) {
            Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            BlockPos attachedPos = pos.relative(facing.getOpposite());
            return level.getBlockState(attachedPos).isFaceSturdy(level, attachedPos, facing);
        }
        
        // For floor/ceiling, use parent logic
        return super.canSurvive(state, level, pos);
    }

    /**
     * Advanced placement system with face-edge detection
     * Determines switch orientation based on which part of the face the player is looking at
     * CORRECTED APPROACH: Place lever ON the clicked block surface for ALL face types
     * 
     * @param context The placement context containing hit location and face information
     * @return BlockState with appropriate orientation, or null if placement is invalid
     */
    private BlockState getAdvancedPlacementState(@Nonnull BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        Vec3 clickLocation = context.getClickLocation();
        BlockPos clickedPos = context.getClickedPos();
        
        // Calculate relative position within the clicked face (0.0 to 1.0)
        Vec3 relativeHit = getRelativeHitLocation(clickLocation, clickedPos, clickedFace);
        
        // Determine orientation based on face type and edge detection
        BlockState proposedState = switch (clickedFace) {
            case UP -> getFloorPlacement(relativeHit, context);
            case DOWN -> getCeilingPlacement(relativeHit, context);
            case NORTH, SOUTH, EAST, WEST -> getWallPlacement(clickedFace, relativeHit, context);
        };
        
        // Validate placement at clicked position
        if (proposedState != null && proposedState.canSurvive(context.getLevel(), clickedPos)) {
            return proposedState;
        }
        
        // Fallback to parent's placement logic if our advanced placement fails
        return super.getStateForPlacement(context);
    }

    /**
     * Calculate relative hit location within the face (0.0 to 1.0 coordinates)
     * FIXED: Consistent coordinate mapping for all wall faces to prevent left/right flipping
     * 
     * @param clickLocation Absolute world coordinates of the click
     * @param clickedPos Position of the block that was clicked
     * @param clickedFace Face that was clicked
     * @return Vec3 with relative coordinates within the face (0.0 to 1.0)
     */
    private Vec3 getRelativeHitLocation(Vec3 clickLocation, BlockPos clickedPos, Direction clickedFace) {
        // Get fractional part of the click location
        double fracX = clickLocation.x - Math.floor(clickLocation.x);
        double fracY = clickLocation.y - Math.floor(clickLocation.y);
        double fracZ = clickLocation.z - Math.floor(clickLocation.z);
        
        // Convert to 0.0-1.0 range and map to face coordinates CONSISTENTLY
        // Fixed: No coordinate flipping to ensure consistent left/right behavior across all wall faces
        return switch (clickedFace) {
            case UP -> new Vec3(fracX, fracZ, 0); // Floor: X=horizontal, Y=depth
            case DOWN -> new Vec3(fracX, 1.0 - fracZ, 0); // Ceiling: X=horizontal, Y=depth (flipped)
            case NORTH -> new Vec3(fracX, 1.0 - fracY, 0); // North wall: X=horizontal, Y=vertical (FIXED: no flip)
            case SOUTH -> new Vec3(fracX, 1.0 - fracY, 0); // South wall: X=horizontal, Y=vertical (unchanged)
            case WEST -> new Vec3(fracZ, 1.0 - fracY, 0); // West wall: X=depth, Y=vertical (unchanged)  
            case EAST -> new Vec3(fracZ, 1.0 - fracY, 0); // East wall: X=depth, Y=vertical (FIXED: no flip)
        };
    }

    /**
     * Determine switch orientation for floor placement
     */
    private BlockState getFloorPlacement(Vec3 relativeHit, BlockPlaceContext context) {
        final double EDGE_THRESHOLD = 4.0 / 16.0; // 4 pixels out of 16
        
        // Check edges (within 4 pixels)
        if (relativeHit.y < EDGE_THRESHOLD) {
            // North edge - switch points north
            return this.defaultBlockState()
                .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                .setValue(BlockStateProperties.POWERED, false);
        } else if (relativeHit.y > (1.0 - EDGE_THRESHOLD)) {
            // South edge - switch points south
            return this.defaultBlockState()
                .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH)
                .setValue(BlockStateProperties.POWERED, false);
        } else if (relativeHit.x < EDGE_THRESHOLD) {
            // West edge - switch points west
            return this.defaultBlockState()
                .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST)
                .setValue(BlockStateProperties.POWERED, false);
        } else if (relativeHit.x > (1.0 - EDGE_THRESHOLD)) {
            // East edge - switch points east
            return this.defaultBlockState()
                .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST)
                .setValue(BlockStateProperties.POWERED, false);
        } else {
            // Center - default orientation based on player facing
            return this.defaultBlockState()
                .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection())
                .setValue(BlockStateProperties.POWERED, false);
        }
    }

    /**
     * Determine switch orientation for ceiling placement
     */
    private BlockState getCeilingPlacement(Vec3 relativeHit, BlockPlaceContext context) {
        final double EDGE_THRESHOLD = 4.0 / 16.0; // 4 pixels out of 16
        
        // Check edges (within 4 pixels)
        if (relativeHit.y < EDGE_THRESHOLD) {
            // North edge - switch points north
            return this.defaultBlockState()
                .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.CEILING)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                .setValue(BlockStateProperties.POWERED, false);
        } else if (relativeHit.y > (1.0 - EDGE_THRESHOLD)) {
            // South edge - switch points south
            return this.defaultBlockState()
                .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.CEILING)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH)
                .setValue(BlockStateProperties.POWERED, false);
        } else if (relativeHit.x < EDGE_THRESHOLD) {
            // West edge - switch points west
            return this.defaultBlockState()
                .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.CEILING)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST)
                .setValue(BlockStateProperties.POWERED, false);
        } else if (relativeHit.x > (1.0 - EDGE_THRESHOLD)) {
            // East edge - switch points east
            return this.defaultBlockState()
                .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.CEILING)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST)
                .setValue(BlockStateProperties.POWERED, false);
        } else {
            // Center - default orientation based on player facing
            return this.defaultBlockState()
                .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.CEILING)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection())
                .setValue(BlockStateProperties.POWERED, false);
        }
    }

    /**
     * Determine switch orientation for wall placement
     * FIXED: HORIZONTAL_FACING must match wall face for validation to pass
     * Rotation effect achieved through block entity orientation data
     */
    private BlockState getWallPlacement(Direction clickedFace, Vec3 relativeHit, BlockPlaceContext context) {
        final double EDGE_THRESHOLD = 4.0 / 16.0; // 4 pixels out of 16
        
        // Determine lever orientation based on clicked edge
        String leverOrientation;
        
        if (relativeHit.y < EDGE_THRESHOLD) {
            // Bottom edge - lever rotated down
            leverOrientation = "bottom";
        } else if (relativeHit.y > (1.0 - EDGE_THRESHOLD)) {
            // Top edge - lever in normal orientation
            leverOrientation = "top";
        } else if (relativeHit.x < EDGE_THRESHOLD) {
            // Left edge - lever rotated left
            leverOrientation = "left";
        } else if (relativeHit.x > (1.0 - EDGE_THRESHOLD)) {
            // Right edge - lever rotated right
            leverOrientation = "right";
        } else {
            // Center - lever in normal orientation
            leverOrientation = "center";
        }
        
        // Store orientation for block entity (for custom rendering)
        PENDING_WALL_ORIENTATION.set(leverOrientation);
        
        // CRITICAL FIX: HORIZONTAL_FACING must always be the wall face for validation to pass
        // The rotation effect is achieved through block entity orientation and renderer
        BlockState result = this.defaultBlockState()
            .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.WALL)
            .setValue(BlockStateProperties.HORIZONTAL_FACING, clickedFace) // Always use wall face
            .setValue(BlockStateProperties.POWERED, false);
            
        return result;
    }

    // ========================================
    // SHAPE AND INTERACTION HANDLING
    // ========================================

    @Override
    @Nonnull
    public VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
        AttachFace attachFace = state.getValue(BlockStateProperties.ATTACH_FACE);
        Direction direction = state.getValue(BlockStateProperties.HORIZONTAL_FACING);

        // For wall-mounted levers, check wall orientation for rotated bounding boxes
        if (attachFace == AttachFace.WALL && level.getBlockEntity(pos) instanceof SwitchesLeverBlockEntity blockEntity) {
            String wallOrientation = blockEntity.getWallOrientation();
            if (wallOrientation != null && !wallOrientation.isEmpty()) {
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

    /**
     * FIXED: Handle both toggle interaction and placement logic correctly
     * Allow toggling when holding Switches Lever (consistent with vanilla behavior)
     */
    @Override
    @Nonnull
    public InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player,
                                 @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit) {
        // Check if holding a Switches Lever and clicking on THIS SAME lever (should toggle)
        // Note: We allow toggle interaction when holding the same item type (vanilla behavior)
        if (!player.getItemInHand(hand).isEmpty() && player.getItemInHand(hand).getItem() instanceof net.minecraft.world.item.BlockItem blockItem) {
            if (blockItem.getBlock() == this) {
                // This is a Switches Lever clicking on a Switches Lever
                // Since we're in the use() method, this means clicking on THIS lever
                // So we should handle toggle interaction, not pass to placement
                // Fall through to toggle logic below
            }
        }
        
        // This is lever interaction
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        // Protect NBT data before blockstate change
        if (level.getBlockEntity(pos) instanceof SwitchesLeverBlockEntity blockEntity) {
            blockEntity.protectNBTDuringStateChange();
        }

        // Toggle the powered state
        boolean currentlyPowered = state.getValue(BlockStateProperties.POWERED);
        BlockState newState = state.setValue(BlockStateProperties.POWERED, !currentlyPowered);

        // Set the new state - NBT data will persist properly now
        level.setBlock(pos, newState, Block.UPDATE_ALL);

        // Play standard lever click sound
        level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS,
                0.3F, currentlyPowered ? 0.5F : 0.6F);

        // Update redstone neighbors
        level.updateNeighborsAt(pos, this);
        Direction attachedDirection = getAttachedDirection(newState);
        level.updateNeighborsAt(pos.relative(attachedDirection), this);

        return InteractionResult.CONSUME;
    }

    /**
     * Handle scheduled ticks for NBT protection cleanup
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
            // Check if this is a wall placement with pending orientation
            String wallOrientation = PENDING_WALL_ORIENTATION.get();
            if (wallOrientation != null && state.getValue(BlockStateProperties.ATTACH_FACE) == AttachFace.WALL) {
                blockEntity.setWallOrientation(wallOrientation);
                PENDING_WALL_ORIENTATION.remove(); // Clean up
            }
            
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }


}