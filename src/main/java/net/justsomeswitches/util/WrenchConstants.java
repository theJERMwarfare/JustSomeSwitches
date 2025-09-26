package net.justsomeswitches.util;

/**
 * Constants for the copy/paste functionality
 */
public final class WrenchConstants {
    
    private WrenchConstants() {
        // Utility class
    }
    
    // NBT Keys
    public static final String COPIED_SETTINGS_KEY = "copied_settings";
    public static final String HAS_COPIED_DATA_KEY = "has_copied_data";
    public static final String TOGGLE_BLOCK_KEY = "toggle_block";
    public static final String TOGGLE_FACE_KEY = "toggle_face";
    public static final String TOGGLE_ROTATION_KEY = "toggle_rotation";
    public static final String BASE_BLOCK_KEY = "base_block";
    public static final String BASE_FACE_KEY = "base_face";
    public static final String BASE_ROTATION_KEY = "base_rotation";
    public static final String POWER_MODE_KEY = "power_mode";
    
    // UI Constants
    public static final int TOOLTIP_MAX_LINES = 8;
    
    // Action Bar Messages
    public static final String MSG_NO_SETTINGS_TO_COPY = "No Custom Texture Settings to Copy";
    public static final String MSG_SETTINGS_NOT_COPIED = "Texture Settings Have Not Been Copied";
    public static final String MSG_SETTINGS_CLEARED = "Texture Settings Cleared";
    public static final String MSG_SETTINGS_PASTED = "Texture Settings Pasted Successfully";
    public static final String MSG_SETTINGS_PARTIAL_APPLIED = "Settings Partially Applied";
    public static final String MSG_SETTINGS_ALREADY_COPIED = "These Texture Settings Have Already Been Copied";
    public static final String MSG_MISSING_BLOCKS_GUI = "SHOW_MISSING_BLOCK_GUI";
    
    // GUI Display Names
    public static final String GUI_COPY_TEXTURE_TITLE = "Copy Texture Settings";
    public static final String GUI_SETTINGS_ALREADY_STORED = "Settings Already Stored";
    public static final String GUI_DIFFERENT_SETTINGS_FOUND = "Different Settings Found";
    
    // Copy Categories
    public static final String CATEGORY_TOGGLE = "Toggle";
    public static final String CATEGORY_BASE = "Base";
    
    // Network Constants
    public static final String NETWORK_PROTOCOL_VERSION = "1";
}
