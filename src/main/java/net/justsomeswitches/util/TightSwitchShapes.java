package net.justsomeswitches.util;

import net.justsomeswitches.block.*;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Pre-computed tight-fitting VoxelShapes for all switch blocks.
 * Diagonal elements are approximated with segmented axis-aligned boxes that
 * follow the rotation angle closely. All shapes cached at startup.
 */
public final class TightSwitchShapes {

    /** Model type identifier for shape lookup. */
    public enum SwitchModelType { LEVER, ROCKER, BUTTONS, SLIDE }

    private static final Map<Class<? extends Block>, SwitchModelType> MODEL_TYPES = new HashMap<>();
    private static final Map<Class<? extends Block>, Boolean> INVERTED_FLAGS = new HashMap<>();

    static {
        MODEL_TYPES.put(BasicLeverBlock.class, SwitchModelType.LEVER);
        MODEL_TYPES.put(BasicLeverInvertedBlock.class, SwitchModelType.LEVER);
        MODEL_TYPES.put(BasicRockerBlock.class, SwitchModelType.ROCKER);
        MODEL_TYPES.put(BasicRockerInvertedBlock.class, SwitchModelType.ROCKER);
        MODEL_TYPES.put(BasicButtonsBlock.class, SwitchModelType.BUTTONS);
        MODEL_TYPES.put(BasicButtonsInvertedBlock.class, SwitchModelType.BUTTONS);
        MODEL_TYPES.put(BasicSlideBlock.class, SwitchModelType.SLIDE);
        MODEL_TYPES.put(BasicSlideInvertedBlock.class, SwitchModelType.SLIDE);
        MODEL_TYPES.put(SwitchesLeverBlock.class, SwitchModelType.LEVER);
        INVERTED_FLAGS.put(BasicLeverBlock.class, false);
        INVERTED_FLAGS.put(BasicLeverInvertedBlock.class, true);
        INVERTED_FLAGS.put(BasicRockerBlock.class, false);
        INVERTED_FLAGS.put(BasicRockerInvertedBlock.class, true);
        INVERTED_FLAGS.put(BasicButtonsBlock.class, false);
        INVERTED_FLAGS.put(BasicButtonsInvertedBlock.class, true);
        INVERTED_FLAGS.put(BasicSlideBlock.class, false);
        INVERTED_FLAGS.put(BasicSlideInvertedBlock.class, true);
        INVERTED_FLAGS.put(SwitchesLeverBlock.class, false);
    }

    // ========================================================================
    // Base shapes (floor placement, facing south — the model's default)
    // Lever shaft/tip use segmented rotation for tight fit.
    // Rocker/lever indicators use clipped segmented rotation (Y >= base plate top).
    // ========================================================================

    // --- LEVER OFF ---
    // Shaft: [7,1,7]→[9,10,9] rotated 45° X — 9 segments
    // Tip: [7.5,9.5,7.5]→[8.5,10.5,8.5] rotated 45° X — 1 segment
    // Toggle caps: clipped to Y≥2 (only visible tip above base plate)
    // Indicators: flat boxes above base plate
    private static final VoxelShape LEVER_OFF_BASE = Shapes.or(
        Block.box(5, 0, 3, 11, 2, 13),
        segmentedRotatedElement(7, 1, 7, 9, 10, 9, 45, 'x', 8, 1, 8, 9),
        segmentedRotatedElement(7.5, 9.5, 7.5, 8.5, 10.5, 8.5, 45, 'x', 8, 1, 8, 1),
        clippedSegmentedRotatedElement(7, 0.99189, 5.501, 9, 1.99189, 6.501, -45, 'x', 8.001, 1.49189, 6.001, 5, 2),
        clippedSegmentedRotatedElement(7, 0.99189, 9.499, 9, 1.99189, 10.499, -45, 'x', 8.001, 1.49189, 9.999, 5, 2),
        Block.box(7, 2, 6, 9, 2.2, 8),
        Block.box(7, 2, 8, 9, 2.2, 10)
    );

    // --- LEVER ON ---
    // Shaft rotated -45° instead of +45°; caps and indicators same as OFF
    private static final VoxelShape LEVER_ON_BASE = Shapes.or(
        Block.box(5, 0, 3, 11, 2, 13),
        segmentedRotatedElement(7, 1, 7, 9, 10, 9, -45, 'x', 8, 1, 8, 9),
        segmentedRotatedElement(7.5, 9.5, 7.5, 8.5, 10.5, 8.5, -45, 'x', 8, 1, 8, 1),
        clippedSegmentedRotatedElement(7, 0.99189, 5.501, 9, 1.99189, 6.501, -45, 'x', 8.001, 1.49189, 6.001, 5, 2),
        clippedSegmentedRotatedElement(7, 0.99189, 9.499, 9, 1.99189, 10.499, -45, 'x', 8.001, 1.49189, 9.999, 5, 2),
        Block.box(7, 2, 6, 9, 2.2, 8),
        Block.box(7, 2, 8, 9, 2.2, 10)
    );

    // --- BUTTONS OFF ---
    private static final VoxelShape BUTTONS_OFF_BASE = Shapes.or(
        Block.box(5, 0, 3, 11, 2, 13),
        Block.box(6.5, 1.2, 4.5, 9.5, 2.2, 7.5),
        Block.box(6.5, 1.2, 8.5, 9.5, 2.2, 11.5),
        Block.box(7, 2, 9, 9, 3, 11),
        Block.box(7, 1.3, 5, 9, 2.3, 7)
    );

    // --- BUTTONS ON ---
    private static final VoxelShape BUTTONS_ON_BASE = Shapes.or(
        Block.box(5, 0, 3, 11, 2, 13),
        Block.box(6.5, 1.2, 4.5, 9.5, 2.2, 7.5),
        Block.box(6.5, 1.2, 8.5, 9.5, 2.2, 11.5),
        Block.box(7, 1.3, 9, 9, 2.3, 11),
        Block.box(7, 2, 5, 9, 3, 7)
    );

    // --- ROCKER OFF ---
    // Toggle bottom (flat): no rotation
    // Toggle top (-22.5°): clipped to Y≥2, 6 segments
    // Indicator upper (22.5°): clipped to Y≥2, 6 segments
    // Indicator lower (-45°): clipped to Y≥2, 8 segments
    private static final VoxelShape ROCKER_OFF_BASE = Shapes.or(
        Block.box(5, 0, 3, 11, 2, 13),
        Block.box(6, 0.1, 5, 10, 2.1, 9),
        clippedSegmentedRotatedElement(6, 0.077, 8.115, 10, 2.077, 11.115, -22.5, 'x', 8, 1.8, 8, 6, 2),
        clippedSegmentedRotatedElement(6, -1.071, 5.114, 10, 0.929, 6.114, 22.5, 'x', 8, 1.8, 8, 6, 2),
        clippedSegmentedRotatedElement(6, -1.135, 9.985, 10, 0.863, 10.983, -45, 'x', 8, 1.8, 8, 8, 2)
    );

    // --- ROCKER ON ---
    // Toggle bottom (22.5°): clipped to Y≥2, 6 segments
    // Toggle top (flat): no rotation
    // Indicator upper (45°): clipped to Y≥2, 8 segments
    // Indicator lower (-22.5°): clipped to Y≥2, 6 segments
    private static final VoxelShape ROCKER_ON_BASE = Shapes.or(
        Block.box(5, 0, 3, 11, 2, 13),
        clippedSegmentedRotatedElement(6, 0.1, 5, 10, 2.1, 9, 22.5, 'x', 8, 1.8, 8, 6, 2),
        Block.box(6, 0.077, 8.115, 10, 2.077, 11.115),
        clippedSegmentedRotatedElement(6, -1.071, 5.114, 10, 0.929, 6.114, 45, 'x', 8, 1.8, 8, 8, 2),
        clippedSegmentedRotatedElement(6, -1.135, 9.985, 10, 0.863, 10.983, -22.5, 'x', 8, 1.8, 8, 6, 2)
    );

    // --- SLIDE OFF ---
    private static final VoxelShape SLIDE_OFF_BASE = Shapes.or(
        Block.box(5, 0, 12, 11, 2, 13),
        Block.box(5, 0, 3, 11, 2, 4),
        Block.box(5, 0, 4, 6, 2, 12),
        Block.box(10, 0, 4, 11, 2, 12),
        Block.box(6, 0.4, 4, 10, 1.4, 5),
        Block.box(6, 0.4, 11, 10, 1.4, 12),
        Block.box(6, 0.7, 8, 10, 1.7, 11),
        Block.box(6, 0, 8, 10, 1, 12),
        Block.box(6, 0, 4, 10, 1, 8)
    );

    // --- SLIDE ON ---
    private static final VoxelShape SLIDE_ON_BASE = Shapes.or(
        Block.box(5, 0, 12, 11, 2, 13),
        Block.box(5, 0, 3, 11, 2, 4),
        Block.box(5, 0, 4, 6, 2, 12),
        Block.box(10, 0, 4, 11, 2, 12),
        Block.box(6, 0.4, 4, 10, 1.4, 5),
        Block.box(6, 0.4, 11, 10, 1.4, 12),
        Block.box(6, 0.7, 5, 10, 1.7, 8),
        Block.box(6, 0, 8, 10, 1, 12),
        Block.box(6, 0, 4, 10, 1, 8)
    );

    // ========================================================================
    // Pre-computed rotated shape cache
    // ========================================================================

    private static final Map<SwitchModelType, Map<Boolean, EnumMap<AttachFace, EnumMap<Direction, VoxelShape>>>> SHAPE_CACHE = new HashMap<>();
    private static final Map<String, Map<Boolean, EnumMap<Direction, VoxelShape>>> WALL_ORIENTATION_CACHE = new HashMap<>();
    private static final Map<Boolean, EnumMap<Direction, VoxelShape>> SWITCHES_CEILING_CACHE = new HashMap<>();

    static {
        for (SwitchModelType type : SwitchModelType.values()) {
            Map<Boolean, EnumMap<AttachFace, EnumMap<Direction, VoxelShape>>> poweredMap = new HashMap<>();
            for (boolean powered : new boolean[]{false, true}) {
                VoxelShape baseShape = getBaseShape(type, powered);
                EnumMap<AttachFace, EnumMap<Direction, VoxelShape>> faceMap = new EnumMap<>(AttachFace.class);
                for (AttachFace face : AttachFace.values()) {
                    EnumMap<Direction, VoxelShape> dirMap = new EnumMap<>(Direction.class);
                    for (Direction dir : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                        int xRot = getBlockstateXRotation(face);
                        int yRot = getBlockstateYRotation(face, dir);
                        dirMap.put(dir, rotateShape(baseShape, xRot, yRot));
                    }
                    faceMap.put(face, dirMap);
                }
                poweredMap.put(powered, faceMap);
            }
            SHAPE_CACHE.put(type, poweredMap);
        }
        for (String orientation : new String[]{"left", "right", "top", "bottom", "center"}) {
            Map<Boolean, EnumMap<Direction, VoxelShape>> poweredMap = new HashMap<>();
            for (boolean powered : new boolean[]{false, true}) {
                EnumMap<Direction, VoxelShape> dirMap = new EnumMap<>(Direction.class);
                for (Direction dir : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                    VoxelShape wallShape = SHAPE_CACHE.get(SwitchModelType.LEVER).get(powered).get(AttachFace.WALL).get(dir);
                    dirMap.put(dir, applyWallOrientationRotation(wallShape, orientation, dir));
                }
                poweredMap.put(powered, dirMap);
            }
            WALL_ORIENTATION_CACHE.put(orientation, poweredMap);
        }
        // SwitchesLever ceiling has swapped east/west Y rotations vs Basic blocks
        for (boolean powered : new boolean[]{false, true}) {
            VoxelShape baseShape = powered ? LEVER_ON_BASE : LEVER_OFF_BASE;
            EnumMap<Direction, VoxelShape> dirMap = new EnumMap<>(Direction.class);
            for (Direction dir : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                int yRot = getSwitchesLeverCeilingYRotation(dir);
                dirMap.put(dir, rotateShape(baseShape, 180, yRot));
            }
            SWITCHES_CEILING_CACHE.put(powered, dirMap);
        }
    }

    private TightSwitchShapes() {
        throw new AssertionError("TightSwitchShapes is a utility class");
    }

    // ========================================================================
    // Public API
    // ========================================================================

    /** Returns tight-fitting shape for a Basic or Switches block. */
    public static VoxelShape getTightShape(Block block, AttachFace face, Direction dir, boolean powered) {
        SwitchModelType type = MODEL_TYPES.getOrDefault(block.getClass(), SwitchModelType.LEVER);
        boolean inverted = INVERTED_FLAGS.getOrDefault(block.getClass(), false);
        boolean effectivePowered = inverted != powered;
        return SHAPE_CACHE.get(type).get(effectivePowered).get(face).get(dir);
    }

    /** Returns tight-fitting shape for SwitchesLeverBlock with wall orientation support. */
    public static VoxelShape getTightSwitchesShape(AttachFace face, Direction dir, boolean powered, String wallOrientation) {
        if (face == AttachFace.WALL && !wallOrientation.isEmpty() && WALL_ORIENTATION_CACHE.containsKey(wallOrientation)) {
            return WALL_ORIENTATION_CACHE.get(wallOrientation).get(powered).get(dir);
        }
        if (face == AttachFace.CEILING) {
            return SWITCHES_CEILING_CACHE.get(powered).get(dir);
        }
        return SHAPE_CACHE.get(SwitchModelType.LEVER).get(powered).get(face).get(dir);
    }

    // ========================================================================
    // Segmented rotated element computation
    // ========================================================================

    /**
     * Approximates a rotated model element with multiple axis-aligned boxes.
     * Slices the element along its longest pre-rotation axis into N segments,
     * rotates each segment's corners, and computes per-segment AABBs.
     * All coordinates in Minecraft's 0-16 element space.
     */
    @SuppressWarnings("SameParameterValue")
    private static VoxelShape segmentedRotatedElement(
            double x1, double y1, double z1, double x2, double y2, double z2,
            double angleDeg, char axis, double ox, double oy, double oz, int segments) {
        return clippedSegmentedRotatedElement(x1, y1, z1, x2, y2, z2, angleDeg, axis, ox, oy, oz, segments, -1);
    }

    /**
     * Like segmentedRotatedElement but clips each segment's AABB to Y >= minVisibleY.
     * Segments entirely below minVisibleY are discarded. This prevents below-baseplate
     * portions from creating AABB bloat while preserving correct rotation behavior
     * for wall/ceiling orientations. Use minVisibleY < 0 to disable clipping.
     */
    private static VoxelShape clippedSegmentedRotatedElement(
            double x1, double y1, double z1, double x2, double y2, double z2,
            double angleDeg, char axis, double ox, double oy, double oz,
            int segments, double minVisibleY) {
        if (segments <= 1) {
            return clippedRotatedAABB(x1, y1, z1, x2, y2, z2, angleDeg, axis, ox, oy, oz, minVisibleY);
        }
        double rad = Math.toRadians(angleDeg);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double extY = y2 - y1;
        double extZ = z2 - z1;
        double extX = x2 - x1;
        VoxelShape result = Shapes.empty();
        if (axis == 'x') {
            boolean sliceAlongY = extY >= extZ;
            for (int i = 0; i < segments; i++) {
                double t0 = (double) i / segments;
                double t1 = (double) (i + 1) / segments;
                double sy1, sy2, sz1, sz2;
                if (sliceAlongY) {
                    sy1 = y1 + t0 * extY;
                    sy2 = y1 + t1 * extY;
                    sz1 = z1;
                    sz2 = z2;
                } else {
                    sy1 = y1;
                    sy2 = y2;
                    sz1 = z1 + t0 * extZ;
                    sz2 = z1 + t1 * extZ;
                }
                VoxelShape seg = computeClippedRotatedAABB(
                    x1, sy1, sz1, x2, sy2, sz2, cos, sin, axis, ox, oy, oz, minVisibleY);
                if (seg != null) {
                    result = Shapes.or(result, seg);
                }
            }
        } else if (axis == 'y') {
            boolean sliceAlongX = extX >= extZ;
            for (int i = 0; i < segments; i++) {
                double t0 = (double) i / segments;
                double t1 = (double) (i + 1) / segments;
                double sx1, sx2, sz1, sz2;
                if (sliceAlongX) {
                    sx1 = x1 + t0 * extX;
                    sx2 = x1 + t1 * extX;
                    sz1 = z1;
                    sz2 = z2;
                } else {
                    sx1 = x1;
                    sx2 = x2;
                    sz1 = z1 + t0 * extZ;
                    sz2 = z1 + t1 * extZ;
                }
                VoxelShape seg = computeClippedRotatedAABB(
                    sx1, y1, sz1, sx2, y2, sz2, cos, sin, axis, ox, oy, oz, minVisibleY);
                if (seg != null) {
                    result = Shapes.or(result, seg);
                }
            }
        } else {
            boolean sliceAlongX = extX >= extY;
            for (int i = 0; i < segments; i++) {
                double t0 = (double) i / segments;
                double t1 = (double) (i + 1) / segments;
                double sx1, sx2, sy1, sy2;
                if (sliceAlongX) {
                    sx1 = x1 + t0 * extX;
                    sx2 = x1 + t1 * extX;
                    sy1 = y1;
                    sy2 = y2;
                } else {
                    sx1 = x1;
                    sx2 = x2;
                    sy1 = y1 + t0 * extY;
                    sy2 = y1 + t1 * extY;
                }
                VoxelShape seg = computeClippedRotatedAABB(
                    sx1, sy1, z1, sx2, sy2, z2, cos, sin, axis, ox, oy, oz, minVisibleY);
                if (seg != null) {
                    result = Shapes.or(result, seg);
                }
            }
        }
        return result;
    }

    /** Single-segment convenience for clipped rotation. */
    private static VoxelShape clippedRotatedAABB(
            double x1, double y1, double z1, double x2, double y2, double z2,
            double angleDeg, char axis, double ox, double oy, double oz, double minVisibleY) {
        double rad = Math.toRadians(angleDeg);
        VoxelShape result = computeClippedRotatedAABB(x1, y1, z1, x2, y2, z2,
            Math.cos(rad), Math.sin(rad), axis, ox, oy, oz, minVisibleY);
        return result != null ? result : Shapes.empty();
    }


    /**
     * Core AABB computation: rotates 8 corners and returns the bounding box.
     * If minVisibleY >= 0, clips the result to Y >= minVisibleY and also
     * tightens the Z bounds to match the actual geometry at the visible Y range.
     * Returns null if the entire AABB is below the threshold.
     */
    private static VoxelShape computeClippedRotatedAABB(
            double x1, double y1, double z1, double x2, double y2, double z2,
            double cos, double sin, char axis, double ox, double oy, double oz,
            double minVisibleY) {
        // Rotate all 8 corners
        double[][] corners = new double[8][3];
        int idx = 0;
        for (double cx : new double[]{x1, x2}) {
            for (double cy : new double[]{y1, y2}) {
                for (double cz : new double[]{z1, z2}) {
                    double dx = cx - ox, dy = cy - oy, dz = cz - oz;
                    if (axis == 'x') {
                        corners[idx][0] = dx + ox;
                        corners[idx][1] = (dy * cos - dz * sin) + oy;
                        corners[idx][2] = (dy * sin + dz * cos) + oz;
                    } else if (axis == 'y') {
                        corners[idx][0] = (dx * cos + dz * sin) + ox;
                        corners[idx][1] = dy + oy;
                        corners[idx][2] = (-dx * sin + dz * cos) + oz;
                    } else {
                        corners[idx][0] = (dx * cos - dy * sin) + ox;
                        corners[idx][1] = (dx * sin + dy * cos) + oy;
                        corners[idx][2] = dz + oz;
                    }
                    idx++;
                }
            }
        }
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (double[] c : corners) {
            minX = Math.min(minX, c[0]); maxX = Math.max(maxX, c[0]);
            minY = Math.min(minY, c[1]); maxY = Math.max(maxY, c[1]);
            minZ = Math.min(minZ, c[2]); maxZ = Math.max(maxZ, c[2]);
        }
        if (minVisibleY < 0) {
            return Block.box(
                Math.max(0, minX), Math.max(0, minY), Math.max(0, minZ),
                Math.min(16, maxX), Math.min(16, maxY), Math.min(16, maxZ));
        }
        if (maxY <= minVisibleY) {
            return null;
        }
        // Tighten Z bounds to match actual geometry at visible Y range.
        // For each edge of the box, if it crosses Y=minVisibleY, find the Z at that crossing.
        // Also include Z of any corner above minVisibleY.
        double tightMinZ = Double.MAX_VALUE;
        double tightMaxZ = -Double.MAX_VALUE;
        double tightMinX = Double.MAX_VALUE;
        double tightMaxX = -Double.MAX_VALUE;
        // Include corners above threshold
        for (double[] c : corners) {
            if (c[1] >= minVisibleY) {
                tightMinZ = Math.min(tightMinZ, c[2]); tightMaxZ = Math.max(tightMaxZ, c[2]);
                tightMinX = Math.min(tightMinX, c[0]); tightMaxX = Math.max(tightMaxX, c[0]);
            }
        }
        // Check edges for crossings at Y=minVisibleY
        // Box edges connect corners that differ in exactly one pre-rotation axis
        int[][] edges = {
            {0,1},{0,2},{0,4}, {1,3},{1,5}, {2,3},{2,6}, {3,7}, {4,5},{4,6}, {5,7},{6,7}
        };
        for (int[] e : edges) {
            double[] a = corners[e[0]], b = corners[e[1]];
            double ya = a[1], yb = b[1];
            if ((ya < minVisibleY) != (yb < minVisibleY)) {
                double t = (minVisibleY - ya) / (yb - ya);
                double crossZ = a[2] + t * (b[2] - a[2]);
                double crossX = a[0] + t * (b[0] - a[0]);
                tightMinZ = Math.min(tightMinZ, crossZ); tightMaxZ = Math.max(tightMaxZ, crossZ);
                tightMinX = Math.min(tightMinX, crossX); tightMaxX = Math.max(tightMaxX, crossX);
            }
        }
        if (tightMinZ > tightMaxZ) {
            return null;
        }
        return Block.box(
            Math.max(0, tightMinX), Math.max(0, minVisibleY), Math.max(0, tightMinZ),
            Math.min(16, tightMaxX), Math.min(16, maxY), Math.min(16, tightMaxZ)
        );
    }

    // ========================================================================
    // Shape selection helpers
    // ========================================================================

    private static VoxelShape getBaseShape(SwitchModelType type, boolean powered) {
        return switch (type) {
            case LEVER -> powered ? LEVER_ON_BASE : LEVER_OFF_BASE;
            case BUTTONS -> powered ? BUTTONS_ON_BASE : BUTTONS_OFF_BASE;
            case ROCKER -> powered ? ROCKER_ON_BASE : ROCKER_OFF_BASE;
            case SLIDE -> powered ? SLIDE_ON_BASE : SLIDE_OFF_BASE;
        };
    }

    /**
     * Applies wall orientation rotation to an already wall-placed shape.
     * Matches the rotation logic in SwitchDynamicModel.WALL_ROTATION_CACHE:
     * N/S walls rotate around Z axis, E/W walls rotate around X axis.
     */
    private static VoxelShape applyWallOrientationRotation(VoxelShape wallShape, String orientation, Direction wallDir) {
        int degrees = switch (orientation) {
            case "top" -> 180;
            case "left" -> 90;
            case "right" -> -90;
            default -> 0;
        };
        if (degrees == 0) return wallShape;
        boolean useZAxis = (wallDir == Direction.NORTH || wallDir == Direction.SOUTH);
        if (useZAxis) {
            return rotateAroundAxis(wallShape, 'z', degrees);
        } else {
            return rotateAroundAxis(wallShape, 'x', degrees);
        }
    }

    // ========================================================================
    // Blockstate rotation mapping
    // ========================================================================

    private static int getBlockstateXRotation(AttachFace face) {
        return switch (face) {
            case FLOOR -> 0;
            case WALL -> 90;
            case CEILING -> 180;
        };
    }

    /** SwitchesLever ceiling has east/west Y rotations swapped vs Basic blocks. */
    private static int getSwitchesLeverCeilingYRotation(Direction dir) {
        return switch (dir) {
            case SOUTH -> 0;
            case WEST -> 270;
            case NORTH -> 180;
            case EAST -> 90;
            default -> 0;
        };
    }

    private static int getBlockstateYRotation(AttachFace face, Direction dir) {
        return switch (face) {
            case FLOOR -> switch (dir) {
                case SOUTH -> 0;
                case WEST -> 90;
                case NORTH -> 180;
                case EAST -> 270;
                default -> 0;
            };
            case WALL -> switch (dir) {
                case NORTH -> 0;
                case EAST -> 90;
                case SOUTH -> 180;
                case WEST -> 270;
                default -> 0;
            };
            case CEILING -> switch (dir) {
                case SOUTH -> 0;
                case WEST -> 90;
                case NORTH -> 180;
                case EAST -> 270;
                default -> 0;
            };
        };
    }

    // ========================================================================
    // VoxelShape rotation utilities (blockstate 90° increments)
    // ========================================================================

    private static VoxelShape rotateShape(VoxelShape shape, int xRot, int yRot) {
        VoxelShape result = shape;
        for (int i = 0; i < (xRot / 90); i++) {
            result = rotateX90(result);
        }
        for (int i = 0; i < (yRot / 90); i++) {
            result = rotateY90(result);
        }
        return result;
    }

    private static VoxelShape rotateY90(VoxelShape shape) {
        VoxelShape[] result = {Shapes.empty()};
        shape.forAllBoxes((x1, y1, z1, x2, y2, z2) ->
            result[0] = Shapes.or(result[0], Shapes.box(1 - z2, y1, x1, 1 - z1, y2, x2))
        );
        return result[0];
    }

    private static VoxelShape rotateX90(VoxelShape shape) {
        VoxelShape[] result = {Shapes.empty()};
        shape.forAllBoxes((x1, y1, z1, x2, y2, z2) ->
            result[0] = Shapes.or(result[0], Shapes.box(x1, z1, 1 - y2, x2, z2, 1 - y1))
        );
        return result[0];
    }

    @SuppressWarnings("SuspiciousNameCombination") // Axis swap is intentional for Z rotation
    private static VoxelShape rotateZ90(VoxelShape shape) {
        VoxelShape[] result = {Shapes.empty()};
        shape.forAllBoxes((x1, y1, z1, x2, y2, z2) ->
            result[0] = Shapes.or(result[0], Shapes.box(1 - y2, x1, z1, 1 - y1, x2, z2))
        );
        return result[0];
    }

    /** Rotates a VoxelShape by an arbitrary multiple of 90° around the specified axis. */
    private static VoxelShape rotateAroundAxis(VoxelShape shape, char axis, int degrees) {
        int steps = ((degrees % 360) + 360) % 360 / 90;
        VoxelShape result = shape;
        for (int i = 0; i < steps; i++) {
            result = switch (axis) {
                case 'x' -> rotateX90(result);
                case 'y' -> rotateY90(result);
                case 'z' -> rotateZ90(result);
                default -> result;
            };
        }
        return result;
    }
}
