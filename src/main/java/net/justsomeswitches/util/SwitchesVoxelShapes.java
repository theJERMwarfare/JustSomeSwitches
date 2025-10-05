package net.justsomeswitches.util;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.core.Direction;

/** Centralized VoxelShape definitions for all switch blocks. */
public final class SwitchesVoxelShapes {
    
    /** Floor placement shape for switches facing North or South. */
    public static final VoxelShape FLOOR_NORTH_SOUTH = Block.box(5.0, 0.0, 3.0, 11.0, 6.0, 13.0);
    
    /** Floor placement shape for switches facing East or West. */
    public static final VoxelShape FLOOR_EAST_WEST = Block.box(3.0, 0.0, 5.0, 13.0, 6.0, 11.0);
    
    /** Ceiling placement shape for switches facing North or South. */
    public static final VoxelShape CEILING_NORTH_SOUTH = Block.box(5.0, 10.0, 3.0, 11.0, 16.0, 13.0);
    
    /** Ceiling placement shape for switches facing East or West. */
    public static final VoxelShape CEILING_EAST_WEST = Block.box(3.0, 10.0, 5.0, 13.0, 16.0, 11.0);
    
    /** Wall placement shape for switches on North wall. */
    public static final VoxelShape WALL_NORTH = Block.box(5.0, 3.0, 10.0, 11.0, 13.0, 16.0);
    
    /** Wall placement shape for switches on South wall. */
    public static final VoxelShape WALL_SOUTH = Block.box(5.0, 3.0, 0.0, 11.0, 13.0, 6.0);
    
    /** Wall placement shape for switches on West wall. */
    public static final VoxelShape WALL_WEST = Block.box(10.0, 3.0, 5.0, 16.0, 13.0, 11.0);
    
    /** Wall placement shape for switches on East wall. */
    public static final VoxelShape WALL_EAST = Block.box(0.0, 3.0, 5.0, 6.0, 13.0, 11.0);
    
    /** Prevents instantiation of utility class. */
    private SwitchesVoxelShapes() {
        throw new AssertionError("SwitchesVoxelShapes is a utility class and should not be instantiated");
    }
    
    /** Returns VoxelShape for switch based on attachment face and direction. */
    public static VoxelShape getShapeForConfiguration(AttachFace attachFace, Direction horizontalFacing) {
        return switch (attachFace) {
            case FLOOR -> switch (horizontalFacing) {
                case NORTH, SOUTH -> FLOOR_NORTH_SOUTH;
                case EAST, WEST -> FLOOR_EAST_WEST;
                default -> FLOOR_NORTH_SOUTH;
            };
            case CEILING -> switch (horizontalFacing) {
                case NORTH, SOUTH -> CEILING_NORTH_SOUTH;
                case EAST, WEST -> CEILING_EAST_WEST;
                default -> CEILING_NORTH_SOUTH;
            };
            case WALL -> switch (horizontalFacing) {
                case NORTH -> WALL_NORTH;
                case SOUTH -> WALL_SOUTH;
                case WEST -> WALL_WEST;
                case EAST -> WALL_EAST;
                default -> WALL_NORTH;
            };
        };
    }
}
