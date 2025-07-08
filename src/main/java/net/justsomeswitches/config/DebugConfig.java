package net.justsomeswitches.config;

/**
 * SILENT DEBUG: Only critical failures and major state changes
 * ---
 * FIXED: Drastically reduced output to prevent 300+ page console spam
 */
public class DebugConfig {

    /**
     * SILENT OPERATION - Only log major failures and user actions
     */
    public static final boolean DEBUG_ENABLED = false;  // ← COMPLETELY DISABLED for clean testing

    /**
     * CRITICAL ONLY - Only log when something actually breaks
     */
    public static void logCritical(String message) {
        // Only enable if debugging actual failures
        if (DEBUG_ENABLED) {
            System.out.println("CRITICAL: " + message);
        }
    }

    /**
     * USER ACTION - Only log direct user interactions (lever toggle, face selection)
     */
    public static void logUserAction(String action) {
        // SELECTIVE: Only major user actions
        System.out.println("USER: " + action);
    }

    /**
     * PERSISTENCE ONLY - Only log save/load operations, not every NBT access
     */
    public static void logPersistence(String operation) {
        // MINIMAL: Only actual save/load cycles
        if (DEBUG_ENABLED) {
            System.out.println("PERSIST: " + operation);
        }
    }

    /**
     * SILENT STATE - No routine state logging
     */
    public static void logStateChange(String component, String change) {
        // SILENT: Comment out to eliminate routine state spam
        // System.out.println("STATE " + component + ": " + change);
    }
}