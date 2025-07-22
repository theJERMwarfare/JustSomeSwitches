package net.justsomeswitches.config;

/**
 * PRODUCTION-READY DEBUG: Completely silent for normal operation
 * CRITICAL FIX: Eliminates all console spam from GUI render loops
 */
public class DebugConfig {

    /**
     * PRODUCTION MODE - All debug disabled for clean console output
     */
    public static final boolean DEBUG_ENABLED = false;
    public static final boolean ENABLE_CRITICAL_LOGGING = false; // DISABLED to prevent spam

    /**
     * ONLY for actual system-breaking errors that prevent functionality
     * USAGE: Only call for errors that completely break the mod
     */
    public static void logCritical(String message) {
        if (ENABLE_CRITICAL_LOGGING) {
            System.err.println("🚨 CRITICAL JSS: " + message);
        }
        // Otherwise completely silent
    }

    /**
     * COMPLETELY DISABLED - No texture preview logging to prevent render loop spam
     * This was causing hundreds of messages per second during GUI rendering
     */
    public static void logTexturePreview(String message) {
        // SILENT: No output in production to prevent render loop spam
    }

    /**
     * MINIMAL MODEL REGISTRATION - Only for startup issues
     */
    public static void logModelRegistration(String message) {
        // Only log during startup, not during render loops
        if (ENABLE_CRITICAL_LOGGING) {
            System.out.println("MODEL_REG: " + message);
        }
    }

    /**
     * COMPLETELY DISABLED - All other debug categories silent
     */
    public static void logUserAction(String action) { /* SILENT */ }
    public static void logPersistence(String operation) { /* SILENT */ }
    public static void logStateChange(String component, String change) { /* SILENT */ }
    public static void logSuccess(String achievement) { /* SILENT */ }
    public static void logValidationFailure(String validation, String expected, String actual) { /* SILENT */ }

    /**
     * Emergency debug method - only use if system completely broken
     */
    public static void emergencyLog(String message) {
        System.err.println("EMERGENCY JSS: " + message);
    }
}