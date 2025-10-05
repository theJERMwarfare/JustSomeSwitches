package net.justsomeswitches.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.justsomeswitches.blockentity.SwitchesLeverBlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/** Security utilities for network packet validation and rate limiting. */
public class SecurityUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityUtils.class);
    
    private static final int MAX_PACKETS_PER_SECOND = 10;
    private static final long RATE_LIMIT_WINDOW_MS = 1000;
    private static final long CLEANUP_INTERVAL_MS = 60000;
    private static final int MAX_COORDINATE = 30000000;
    private static final int MIN_COORDINATE = -30000000;
    
    private static final int MAX_NBT_SIZE_BYTES = 32768;
    private static final int MAX_STRING_LENGTH = 512;
    private static final int MAX_TEXTURE_PATH_LENGTH = 256;
    

    private static final Pattern VALID_TEXTURE_PATH = Pattern.compile("^[a-zA-Z0-9_/\\-.:]+$");
    private static final Pattern DIRECTORY_TRAVERSAL = Pattern.compile("\\.\\.[\\\\/]");
    
    private static final ConcurrentHashMap<String, PlayerRateLimit> rateLimitMap = new ConcurrentHashMap<>();
    private static volatile long lastCleanupTime = System.currentTimeMillis();
    
    /**
     * Player rate limiting data structure
     */
    private static class PlayerRateLimit {
        private final AtomicLong packetCount = new AtomicLong(0);
        private volatile long windowStartTime = System.currentTimeMillis();
        
        synchronized boolean isRateLimited() {
            long currentTime = System.currentTimeMillis();
            
            if (currentTime - windowStartTime >= RATE_LIMIT_WINDOW_MS) {
                windowStartTime = currentTime;
                packetCount.set(0);
            }
            
            long currentCount = packetCount.incrementAndGet();
            return currentCount > MAX_PACKETS_PER_SECOND;
        }
        
        long getCurrentCount() {
            return packetCount.get();
        }
    }
    
    /** Checks if player is rate limited (10 packets/second sliding window). */
    public static boolean isRateLimited(@Nonnull ServerPlayer player) {
        String playerId = player.getUUID().toString();
        
        performPeriodicCleanup();
        
        PlayerRateLimit rateLimit = rateLimitMap.computeIfAbsent(playerId, k -> new PlayerRateLimit());
        
        boolean isLimited = rateLimit.isRateLimited();
        
        if (isLimited) {
            LOGGER.warn("Rate limit exceeded for player {} (UUID: {}). Current count: {}/{} packets/second",
                player.getName().getString(), playerId, rateLimit.getCurrentCount(), MAX_PACKETS_PER_SECOND);
        }
        
        return isLimited;
    }
    
    /** Validates block coordinates are within reasonable bounds. */
    public static boolean isValidBlockPosition(@Nonnull BlockPos blockPos) {
        return blockPos.getX() >= MIN_COORDINATE && blockPos.getX() <= MAX_COORDINATE &&
               blockPos.getY() >= -64 && blockPos.getY() <= 320 &&
               blockPos.getZ() >= MIN_COORDINATE && blockPos.getZ() <= MAX_COORDINATE;
    }
    
    /** Validates player permission to interact with block. */
    public static boolean canPlayerInteractWithBlock(@Nonnull ServerPlayer player, @Nonnull Level level, @Nonnull BlockPos blockPos) {
        if (!level.isLoaded(blockPos)) {
            LOGGER.warn("Player {} attempted to interact with unloaded chunk at {}",
                player.getName().getString(), blockPos);
            return false;
        }
        
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        if (!(blockEntity instanceof SwitchesLeverBlockEntity)) {
            LOGGER.warn("Player {} attempted to interact with non-switch block at {}",
                player.getName().getString(), blockPos);
            return false;
        }
        
        double distance = player.position().distanceToSqr(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);
        if (distance > 64.0) {
            LOGGER.warn("Player {} attempted long-range interaction at {} (distance: {})",
                player.getName().getString(), blockPos, Math.sqrt(distance));
            return false;
        }
        
        return true;
    }
    
    /** Validates texture path for security (prevents directory traversal). */
    public static boolean isValidTexturePath(@Nonnull String texturePath) {
        if (texturePath.isEmpty()) {
            return false;
        }
        
        if (texturePath.length() > MAX_TEXTURE_PATH_LENGTH) {
            LOGGER.warn("Texture path too long: {} characters (max: {})", texturePath.length(), MAX_TEXTURE_PATH_LENGTH);
            return false;
        }
        
        if (DIRECTORY_TRAVERSAL.matcher(texturePath).find()) {
            LOGGER.warn("Directory traversal attempt detected in texture path: {}", texturePath);
            return false;
        }
        
        if (!VALID_TEXTURE_PATH.matcher(texturePath).matches()) {
            LOGGER.warn("Invalid characters in texture path: {}", texturePath);
            return false;
        }
        
        if (texturePath.startsWith("/") || texturePath.startsWith("\\") || 
            texturePath.contains("..") || texturePath.contains("//")) {
            LOGGER.warn("Unsafe texture path pattern detected: {}", texturePath);
            return false;
        }
        
        return true;
    }
    
    /** Validates string input length and content. */
    public static boolean isValidString(@Nonnull String input, int maxLength) {
        
        if (input.length() > maxLength) {
            LOGGER.warn("String too long: {} characters (max: {})", input.length(), maxLength);
            return false;
        }
        
        if (input.contains("\u0000")) {
            LOGGER.warn("Null byte detected in string input");
            return false;
        }
        
        return true;
    }
    
    /** Validates category name for texture updates. */
    public static boolean isValidCategory(@Nonnull String category) {
        return switch (category) {
            case "base", "toggle", "power", "base_rotation", "toggle_rotation" -> true;
            default -> {
                LOGGER.warn("Invalid category name: {}", category);
                yield false;
            }
        };
    }
    
    /** Logs security events - disabled in production (only errors are logged). */
    public static void logSecurityEvent(@SuppressWarnings("unused") @Nonnull ServerPlayer player, 
                                      @SuppressWarnings("unused") @Nonnull String action, 
                                      @SuppressWarnings("unused") @Nonnull BlockPos blockPos, 
                                      @SuppressWarnings("unused") @Nonnull String details) {
        // Debug logging disabled - only actual errors/violations are logged
    }
    
    /** Logs security violations at WARN level for monitoring. */
    public static void logSecurityViolation(@Nonnull ServerPlayer player, @Nonnull String violation, @Nonnull String details) {
        LOGGER.warn("SECURITY VIOLATION - Player: {} (UUID: {}), Violation: {}, Details: {}",
            player.getName().getString(), player.getUUID(), violation, details);
    }
    
    /** Periodic cleanup of rate limiting data to prevent memory leaks. */
    private static void performPeriodicCleanup() {
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - lastCleanupTime >= CLEANUP_INTERVAL_MS) {
            lastCleanupTime = currentTime;
            
            rateLimitMap.entrySet().removeIf(entry -> {
                PlayerRateLimit rateLimit = entry.getValue();
                return currentTime - rateLimit.windowStartTime > 300000;
            });
        }
    }
    
    /** Returns maximum allowed NBT data size in bytes. */
    @SuppressWarnings("unused") // May be used by external integrations
    public static int getMaxNbtSize() {
        return MAX_NBT_SIZE_BYTES;
    }
    
    /** Returns maximum allowed string length in characters. */
    public static int getMaxStringLength() {
        return MAX_STRING_LENGTH;
    }
    
    /** Validates packet size to prevent memory exhaustion attacks. */
    public static boolean isValidPacketSize(int estimatedSize) {
        if (estimatedSize > MAX_NBT_SIZE_BYTES) {
            LOGGER.warn("Packet size too large: {} bytes (max: {} bytes)", estimatedSize, MAX_NBT_SIZE_BYTES);
            return false;
        }
        return true;
    }
}