package net.justsomeswitches.config;

/**
 * DRASTICALLY REDUCED DEBUG: Only critical failures and major user actions
 * ---
 * FIXED: Eliminated 368-page console spam by disabling routine operations
 */
public class DebugConfig {

    /**
     * CRITICAL ONLY - Disabled for production testing
     */
    public static final boolean DEBUG_ENABLED = false;

    /**
     * CRITICAL FAILURES ONLY - System breaking errors
     */
    public static void logCritical(String message) {
        System.out.println("🚨 CRITICAL: " + message);
    }

    /**
     * USER ACTIONS ONLY - Direct user interactions (lever toggle, face selection)
     */
    public static void logUserAction(String action) {
        System.out.println("👤 USER: " + action);
    }

    /**
     * PERSISTENCE CRITICAL - Only actual save/load operations, not every NBT access
     */
    public static void logPersistence(String operation) {
        System.out.println("💾 PERSIST: " + operation);
    }

    /**
     * COMPLETELY DISABLED - No routine state logging to prevent console spam
     */
    public static void logStateChange(String component, String change) {
        // SILENT: Completely disabled to prevent 368-page console output
        // System.out.println("STATE " + component + ": " + change);
    }

    /**
     * SUCCESS TRACKING - Only log when problems are resolved
     */
    public static void logSuccess(String achievement) {
        System.out.println("✅ SUCCESS: " + achievement);
    }

    /**
     * VALIDATION FAILURES - When expected data is missing/incorrect
     */
    public static void logValidationFailure(String validation, String expected, String actual) {
        System.out.println("❌ VALIDATION: " + validation + " - Expected: " + expected + ", Got: " + actual);
    }
}