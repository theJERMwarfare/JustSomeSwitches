package net.justsomeswitches.config;

/**
 * SILENT DEBUG: Only critical failures and user actions
 */
public class DebugConfig {

    /**
     * SILENT OPERATION - Only log critical failures
     */
    public static final boolean DEBUG_ENABLED = false;  // ← DISABLED for silent operation

    /**
     * CRITICAL ONLY - Only log when something breaks
     */
    public static void logCritical(String message) {
        // SILENT: Only enable if debugging critical failures
        if (DEBUG_ENABLED) {
            System.out.println("CRITICAL: " + message);
        }
    }

    /**
     * STATE CHANGE - Only log user-initiated actions
     */
    public static void logStateChange(String component, String change) {
        // SILENT: Comment out for production testing
        // System.out.println("STATE " + component + ": " + change);
    }

    /**
     * PERSISTENCE - Only log save/load failures
     */
    public static void logPersistence(String operation, String data) {
        // SILENT: Comment out for production testing
        // System.out.println("PERSIST " + operation + ": " + data);
    }

    /**
     * USER ACTION - Only log direct user interactions for debugging
     */
    public static void logUserAction(String action) {
        if (DEBUG_ENABLED) {
            System.out.println("USER: " + action);
        }
    }
}