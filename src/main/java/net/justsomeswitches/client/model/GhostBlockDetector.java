package net.justsomeswitches.client.model;

import net.justsomeswitches.block.AbstractSwitchBlock;
import net.justsomeswitches.block.ISwitchBlock;
import net.justsomeswitches.item.SwitchBlockItem;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Client-side system for ghost preview detection using existing placement logic. */
public class GhostBlockDetector {

    private static GhostBlockDetector instance;
    private BlockPos currentGhostPos = null;
    private BlockState currentGhostState = null;
    private String currentWallOrientation = "center";
    private boolean isGhostActive = false;
    private long lastUpdateTime = 0;
    private static final long UPDATE_INTERVAL_MS = 50;

    private GhostBlockDetector() {}

    @Nonnull
    public static GhostBlockDetector getInstance() {
        if (instance == null) {
            instance = new GhostBlockDetector();
        }
        return instance;
    }

    /** Updates ghost preview detection from client tick event. */
    public void updateGhostPreview() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime < UPDATE_INTERVAL_MS) {
            return;
        }
        lastUpdateTime = currentTime;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            clearGhostPreview();
            return;
        }

        Player player = mc.player;
        ItemStack heldItem = player.getMainHandItem();

        if (!isPreviewableItem(heldItem)) {
            clearGhostPreview();
            return;
        }

        HitResult hit = mc.hitResult;
        if (!(hit instanceof BlockHitResult blockHit)) {
            clearGhostPreview();
            return;
        }

        if (!isValidHitResult(blockHit, player)) {
            clearGhostPreview();
            return;
        }

        BlockPos targetPos = calculatePlacementPosition(blockHit);
        if (!canPlaceAt(mc.level, targetPos, heldItem, player, blockHit)) {
            clearGhostPreview();
            return;
        }

        BlockState ghostState = calculateGhostState(targetPos, heldItem, blockHit, player);
        if (ghostState == null) {
            clearGhostPreview();
            return;
        }

        String wallOrientation = calculateWallOrientation(blockHit, ghostState);
        updateGhostState(targetPos, ghostState, wallOrientation);
    }

    /** Validates hit result is within reach distance. */
    private boolean isValidHitResult(@Nonnull BlockHitResult hit, @Nonnull Player player) {
        Vec3 eyePosition = player.getEyePosition();
        Vec3 hitLocation = hit.getLocation();
        double distanceToHit = eyePosition.distanceTo(hitLocation);
        double maxReachDistance = getPlayerReachDistance(player);
        return distanceToHit <= maxReachDistance + 0.5;
    }

    /** Gets player's block placement reach distance based on game mode. */
    private double getPlayerReachDistance(@Nonnull Player player) {
        return player.getAbilities().instabuild ? 5.0 : 4.5;
    }

    /** Checks if held item should trigger ghost previews. */
    private boolean isPreviewableItem(@Nonnull ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return stack.getItem() instanceof SwitchBlockItem;
    }

    /** Calculates target position for block placement based on hit result. */
    @Nonnull
    private BlockPos calculatePlacementPosition(@Nonnull BlockHitResult hit) {
        return hit.getBlockPos().relative(hit.getDirection());
    }

    /** Validates whether block can be placed at target position using existing placement logic. */
    private boolean canPlaceAt(@Nonnull Level level, @Nonnull BlockPos targetPos, @Nonnull ItemStack stack,
                              @Nonnull Player player, @Nonnull BlockHitResult hit) {
        if (!level.getBlockState(targetPos).canBeReplaced()) {
            return false;
        }
        double distanceToTarget = player.getEyePosition().distanceTo(Vec3.atCenterOf(targetPos));
        if (distanceToTarget > getPlayerReachDistance(player) + 2.0) {
            return false;
        }
        if (!(stack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem)) {
            return false;
        }
        net.minecraft.world.level.block.Block block = blockItem.getBlock();
        if (!(block instanceof ISwitchBlock)) {
            return false;
        }
        BlockPlaceContext context = new BlockPlaceContext(
            level, player, InteractionHand.MAIN_HAND, stack, hit
        );
        BlockState proposedState = block.getStateForPlacement(context);
        return proposedState != null && proposedState.canSurvive(level, targetPos);
    }

    /** Calculates exact BlockState that would be placed using existing placement logic. */
    @Nullable
    private BlockState calculateGhostState(@SuppressWarnings("unused") @Nonnull BlockPos pos, @Nonnull ItemStack stack,
                                          @Nonnull BlockHitResult hit, @Nonnull Player player) {
        if (!(stack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem)) {
            return null;
        }
        net.minecraft.world.level.block.Block block = blockItem.getBlock();
        if (!(block instanceof AbstractSwitchBlock switchBlock)) {
            return null;
        }
        Level level = player.level();
        BlockPlaceContext context = new BlockPlaceContext(
            level, player, InteractionHand.MAIN_HAND, stack, hit
        );
        return switchBlock.getStateForPlacement(context);
    }

    /** Calculates wall orientation for ghost preview replicating AbstractSwitchBlock logic. */
    @Nonnull
    private String calculateWallOrientation(@Nonnull BlockHitResult hit, @Nonnull BlockState previewState) {
        try {
            if (previewState.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.ATTACH_FACE)) {
                var attachFace = previewState.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.ATTACH_FACE);
                if (attachFace == net.minecraft.world.level.block.state.properties.AttachFace.WALL) {
                    Direction clickedFace = hit.getDirection();
                    Vec3 clickLocation = hit.getLocation();
                    Vec3 relativeHit = getRelativeHitLocationForWall(clickLocation, clickedFace);
                    final double EDGE_THRESHOLD = 4.0 / 16.0;
                    if (relativeHit.y < EDGE_THRESHOLD) {
                        return "bottom";
                    } else if (relativeHit.y > (1.0 - EDGE_THRESHOLD)) {
                        return "top";
                    } else if (relativeHit.x < EDGE_THRESHOLD) {
                        return "left";
                    } else if (relativeHit.x > (1.0 - EDGE_THRESHOLD)) {
                        return "right";
                    } else {
                        return "center";
                    }
                }
            }
        } catch (Exception e) {
            // Silently handle orientation calculation errors - fallback to center
        }
        return "center";
    }

    /** Calculates relative hit position for wall placements replicating AbstractSwitchBlock logic. */
    @Nonnull
    private Vec3 getRelativeHitLocationForWall(@Nonnull Vec3 clickLocation, @Nonnull Direction clickedFace) {
        double fracX = clickLocation.x - Math.floor(clickLocation.x);
        double fracY = clickLocation.y - Math.floor(clickLocation.y);
        double fracZ = clickLocation.z - Math.floor(clickLocation.z);
        return switch (clickedFace) {
            case NORTH -> new Vec3(fracX, 1.0 - fracY, 0);
            case SOUTH -> new Vec3(fracX, 1.0 - fracY, 0);
            case WEST -> new Vec3(fracZ, 1.0 - fracY, 0);
            case EAST -> new Vec3(fracZ, 1.0 - fracY, 0);
            default -> new Vec3(0.5, 0.5, 0);
        };
    }

    /** Updates current ghost preview state. */
    private void updateGhostState(@Nonnull BlockPos pos, @Nonnull BlockState state, @Nonnull String wallOrientation) {
        boolean posChanged = !pos.equals(currentGhostPos);
        boolean stateChanged = !state.equals(currentGhostState);
        boolean orientationChanged = !wallOrientation.equals(currentWallOrientation);
        if (posChanged || stateChanged || orientationChanged || !isGhostActive) {
            currentGhostPos = pos;
            currentGhostState = state;
            currentWallOrientation = wallOrientation;
            isGhostActive = true;
            GhostModelDataProvider.getInstance().updateGhostPreview(pos, state, wallOrientation);
        }
    }

    /** Clears current ghost preview. */
    public void clearGhostPreview() {
        if (isGhostActive) {
            currentGhostPos = null;
            currentGhostState = null;
            currentWallOrientation = "center";
            isGhostActive = false;
            GhostModelDataProvider.getInstance().clearGhostPreview();
        }
    }

    @Nullable
    public BlockPos getCurrentGhostPos() { return currentGhostPos; }

    @Nullable
    public BlockState getCurrentGhostState() { return currentGhostState; }

    @Nonnull
    public String getCurrentWallOrientation() { return currentWallOrientation; }

    public boolean isGhostActive() { return isGhostActive; }
}
