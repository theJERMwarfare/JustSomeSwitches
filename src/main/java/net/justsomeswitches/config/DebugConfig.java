package net.justsomeswitches.config;

/**
 * Global debug configuration for Just Some Switches mod
 * ---
 * USAGE: Set DEBUG_ENABLED = true for testing the face selection fix
 */
public class DebugConfig {

    /**
     * Master debug switch - set to true for testing face selection fix
     */
    public static final boolean DEBUG_ENABLED = true;  // ← Set to true for testing

    /**
     * Specific debug categories
     */
    public static final boolean DEBUG_BLOCKENTITY = DEBUG_ENABLED && true;
    public static final boolean DEBUG_MENU = DEBUG_ENABLED && true;
    public static final boolean DEBUG_FACE_SELECTION = false;  // ← Always off due to spam
    public static final boolean DEBUG_MODEL = DEBUG_ENABLED && true;
    public static final boolean DEBUG_CLIENT_SETUP = DEBUG_ENABLED && true;

    /**
     * Helper method for conditional debug logging
     */
    public static void log(String category, String message) {
        if (DEBUG_ENABLED) {
            System.out.println("DEBUG " + category + ": " + message);
        }
    }

    /**
     * Critical messages that should always be shown
     */
    public static void critical(String message) {
        System.out.println("CRITICAL: " + message);
    }

    /**
     * Warning messages
     */
    public static void warn(String message) {
        System.out.println("WARNING: " + message);
    }
}