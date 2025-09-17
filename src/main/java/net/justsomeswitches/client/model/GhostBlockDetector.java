package net.justsomeswitches.client.model;

import net.justsomeswitches.block.SwitchesLeverBlock;
import net.justsomeswitches.init.JustSomeSwitchesModBlocks;
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

/**
 * Client-side system to detect when ghost preview should appear.
 *
 * This class handles:
 * - Monitoring player held item (Switches Lever in main hand)
 * - Using existing getAdvancedPlacementState() for valid placement detection
 * - Creating ghost BlockState using existing placement logic
 * - Triggering ghost preview when conditions are met
 */
public class GhostBlockDetector {
    
    private static GhostBlockDetector instance;
    
    // Current ghost preview state
    private BlockPos currentGhostPos = null;
    private BlockState currentGhostState = null;
    private String currentWallOrientation = "center";
    private boolean isGhostActive = false;
    
    // Performance optimization
    private long lastUpdateTime = 0;
    private static final long UPDATE_INTERVAL_MS = 50; // Update every 50ms (20fps)
    
    private GhostBlockDetector() {}
    
    @Nonnull
    public static GhostBlockDetector getInstance() {
        if (instance == null) {
            instance = new GhostBlockDetector();
        }
        return instance;
    }
    
    /**
     * Main update method called from client tick event.
     * Handles ghost preview detection and state management.
     */
    public void updateGhostPreview() {
        // Performance throttling - only update 20 times per second
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
        
        // Check if player is holding Switches Lever item
        if (!isPreviewableItem(heldItem)) {
            clearGhostPreview();
            return;
        }
        
        // Process current hit result for placement detection
        HitResult hit = mc.hitResult;
        if (!(hit instanceof BlockHitResult blockHit)) {
            clearGhostPreview();
            return;
        }
        
        // Validate reach distance and hit result
        if (!isValidHitResult(blockHit, player)) {
            clearGhostPreview();
            return;
        }
        
        // Calculate placement position and validate
        BlockPos targetPos = calculatePlacementPosition(blockHit);
        if (targetPos == null || !canPlaceAt(mc.level, targetPos, heldItem, player, blockHit)) {
            clearGhostPreview();
            return;
        }
        
        // Calculate the ghost BlockState and wall orientation
        BlockState ghostState = calculateGhostState(targetPos, heldItem, blockHit, player);
        if (ghostState == null) {
            clearGhostPreview();
            return;
        }
        
        String wallOrientation = calculateWallOrientation(blockHit, ghostState);
        
        // Update ghost preview if position, state, or orientation changed
        updateGhostState(targetPos, ghostState, wallOrientation);
    }
    
    /**
     * Validates hit result is within reach distance.
     */
    private boolean isValidHitResult(@Nonnull BlockHitResult hit, @Nonnull Player player) {
        // Check hit result type first (fastest check)
        if (hit.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        
        // Calculate and validate reach distance
        Vec3 eyePosition = player.getEyePosition();
        Vec3 hitLocation = hit.getLocation();
        double distanceToHit = eyePosition.distanceTo(hitLocation);
        double maxReachDistance = getPlayerReachDistance(player);
        
        return distanceToHit <= maxReachDistance + 0.5; // Allow slight tolerance
    }
    
    /**
     * Gets the player's block placement reach distance based on game mode.
     */
    private double getPlayerReachDistance(@Nonnull Player player) {
        return player.getAbilities().instabuild ? 5.0 : 4.5;
    }
    
    /**
     * Checks if the held item should trigger ghost previews.
     */
    private boolean isPreviewableItem(@Nonnull ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        
        return stack.getItem() == JustSomeSwitchesModBlocks.SWITCHES_LEVER_ITEM.get();
    }
    
    /**
     * Calculates the target position for block placement based on hit result.
     */
    @Nonnull
    private BlockPos calculatePlacementPosition(@Nonnull BlockHitResult hit) {
        return hit.getBlockPos().relative(hit.getDirection());
    }
    
    /**
     * Validates whether a block can be placed at the target position.
     * Uses the existing SwitchesLeverBlock placement logic with reach distance validation.
     */
    private boolean canPlaceAt(@Nonnull Level level, @Nonnull BlockPos targetPos, @Nonnull ItemStack stack, 
                              @Nonnull Player player, @Nonnull BlockHitResult hit) {
        // Validate target position can be replaced
        if (!level.getBlockState(targetPos).canBeReplaced()) {
            return false;
        }
        
        // Check reach distance
        double distanceToTarget = player.getEyePosition().distanceTo(Vec3.atCenterOf(targetPos));
        if (distanceToTarget > getPlayerReachDistance(player) + 2.0) {
            return false;
        }
        
        // Use existing placement logic to validate
        BlockPlaceContext context = new BlockPlaceContext(
            level, player, InteractionHand.MAIN_HAND, stack, hit
        );
        
        SwitchesLeverBlock leverBlock = JustSomeSwitchesModBlocks.SWITCHES_LEVER.get();
        BlockState proposedState = leverBlock.getStateForPlacement(context);
        
        return proposedState != null && proposedState.canSurvive(level, targetPos);
    }
    
    /**
     * Calculates the exact BlockState that would be placed using existing placement logic.
     */
    @Nullable
    private BlockState calculateGhostState(@SuppressWarnings("unused") @Nonnull BlockPos pos, @Nonnull ItemStack stack, 
                                          @Nonnull BlockHitResult hit, @Nonnull Player player) {
        Level level = player.level();
        
        // Create placement context matching the exact conditions
        BlockPlaceContext context = new BlockPlaceContext(
            level, player, InteractionHand.MAIN_HAND, stack, hit
        );
        
        // Use existing SwitchesLeverBlock placement logic
        SwitchesLeverBlock leverBlock = JustSomeSwitchesModBlocks.SWITCHES_LEVER.get();
        return leverBlock.getStateForPlacement(context);
    }
    
    /**
     * Calculates wall orientation for ghost preview (replicates SwitchesLeverBlock logic).
     */
    @Nonnull
    private String calculateWallOrientation(@Nonnull BlockHitResult hit, @Nonnull BlockState previewState) {
        // Only calculate wall orientation for wall placements
        try {
            if (previewState.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.ATTACH_FACE)) {
                var attachFace = previewState.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.ATTACH_FACE);
                
                if (attachFace == net.minecraft.world.level.block.state.properties.AttachFace.WALL) {
                    // Calculate relative hit location (same logic as SwitchesLeverBlock)
                    Direction clickedFace = hit.getDirection();
                    Vec3 clickLocation = hit.getLocation();
                    Vec3 relativeHit = getRelativeHitLocationForWall(clickLocation, clickedFace);
                    
                    // Apply same edge detection logic as SwitchesLeverBlock
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
            // Handle calculation errors gracefully
        }
        
        return "center"; // Default for non-wall placements
    }
    
    /**
     * Calculates relative hit position for wall placements (replicates SwitchesLeverBlock logic).
     */
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
            default -> new Vec3(0.5, 0.5, 0); // Fallback
        };
    }
    
    /**
     * Updates the current ghost preview state.
     */
    private void updateGhostState(@Nonnull BlockPos pos, @Nonnull BlockState state, @Nonnull String wallOrientation) {
        // Check if anything actually changed
        boolean posChanged = !pos.equals(currentGhostPos);
        boolean stateChanged = !state.equals(currentGhostState);
        boolean orientationChanged = !wallOrientation.equals(currentWallOrientation);
        
        if (posChanged || stateChanged || orientationChanged || !isGhostActive) {
            currentGhostPos = pos;
            currentGhostState = state;
            currentWallOrientation = wallOrientation;
            isGhostActive = true;
            
            // Trigger model data update for ghost preview
            GhostModelDataProvider.getInstance().updateGhostPreview(pos, state, wallOrientation);
        }
    }
    
    /**
     * Clears the current ghost preview.
     */
    public void clearGhostPreview() {
        if (isGhostActive) {
            currentGhostPos = null;
            currentGhostState = null;
            currentWallOrientation = "center";
            isGhostActive = false;
            
            // Clear ghost preview from model data provider
            GhostModelDataProvider.getInstance().clearGhostPreview();
        }
    }
    
    // Getters for current ghost state
    @Nullable
    public BlockPos getCurrentGhostPos() { return currentGhostPos; }
    
    @Nullable
    public BlockState getCurrentGhostState() { return currentGhostState; }
    
    @Nonnull
    public String getCurrentWallOrientation() { return currentWallOrientation; }
    
    public boolean isGhostActive() { return isGhostActive; }
}
