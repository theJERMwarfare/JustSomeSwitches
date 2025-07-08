package net.justsomeswitches.config;

/**
 * MINIMAL Debug configuration - only critical state changes
 */
public class DebugConfig {

    /**
     * Master debug switch - MINIMAL logging only
     */
    public static final boolean DEBUG_ENABLED = true;

    /**
     * MINIMAL debug categories - only the most critical information
     */
    public static final boolean DEBUG_BLOCKENTITY = false;     // ← OFF - too much spam
    public static final boolean DEBUG_MENU = false;           // ← OFF - too much spam
    public static final boolean DEBUG_FACE_SELECTION = false; // ← OFF - too much spam
    public static final boolean DEBUG_MODEL = false;          // ← OFF - too much spam
    public static final boolean DEBUG_CLIENT_SETUP = false;   // ← OFF - too much spam

    /**
     * CRITICAL ONLY - only log state changes and failures
     */
    public static void logCritical(String message) {
        if (DEBUG_ENABLED) {
            System.out.println("CRITICAL: " + message);
        }
    }

    /**
     * STATE CHANGE - only log when important state changes
     */
    public static void logStateChange(String component, String change) {
        if (DEBUG_ENABLED) {
            System.out.println("STATE " + component + ": " + change);
        }
    }

    /**
     * PERSISTENCE - only log save/load operations
     */
    public static void logPersistence(String operation, String data) {
        if (DEBUG_ENABLED) {
            System.out.println("PERSIST " + operation + ": " + data);
        }
    }
}