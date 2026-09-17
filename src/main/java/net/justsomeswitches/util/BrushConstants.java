package net.justsomeswitches.util;

/**
 * Constants for the copy/paste functionality
 */
public final class BrushConstants {
    
    private BrushConstants() {
        // Utility class
    }
    
    public static final String COPIED_SETTINGS_KEY = "copied_settings";
    public static final String BRUSH_MODE_KEY = "brush_mode";
    public static final String HAS_COPIED_DATA_KEY = "has_copied_data";
    public static final String TOGGLE_BLOCK_KEY = "toggle_block";
    public static final String TOGGLE_FACE_KEY = "toggle_face";
    public static final String TOGGLE_ROTATION_KEY = "toggle_rotation";
    public static final String TOGGLE_TEXTURE_PATH_KEY = "toggle_texture_path";
    public static final String BASE_BLOCK_KEY = "base_block";
    public static final String BASE_FACE_KEY = "base_face";
    public static final String BASE_ROTATION_KEY = "base_rotation";
    public static final String BASE_TEXTURE_PATH_KEY = "base_texture_path";
    public static final String POWER_MODE_KEY = "power_mode";
    
    public static final int TOOLTIP_MAX_LINES = 8;
    
    // Translation keys, not English. The action bar and menu-title helpers resolve these with
    // Component.translatable, so the text lives in en_us.json and can be translated.
    public static final String MSG_NO_SETTINGS_TO_COPY = "message.justsomeswitches.no_settings_to_copy";
    public static final String MSG_SETTINGS_NOT_COPIED = "message.justsomeswitches.settings_not_copied";
    public static final String MSG_SETTINGS_CLEARED = "message.justsomeswitches.settings_cleared";
    public static final String MSG_SETTINGS_PASTED = "message.justsomeswitches.settings_pasted";
    public static final String MSG_SETTINGS_PARTIAL_APPLIED = "message.justsomeswitches.settings_partial_applied";
    public static final String MSG_SETTINGS_ALREADY_COPIED = "message.justsomeswitches.settings_already_copied";
    public static final String MSG_NO_SETTINGS_TO_PASTE = "message.justsomeswitches.no_settings_to_paste";
    public static final String MSG_ALL_BLOCKS_AVAILABLE = "message.justsomeswitches.all_blocks_available";
    public static final String MSG_USE_COPY_GUI = "message.justsomeswitches.use_copy_gui";
    public static final String MSG_SAME_SETTINGS = "message.justsomeswitches.same_settings";
    public static final String MSG_PREVIOUS_SETTINGS_CLEARED = "message.justsomeswitches.previous_settings_cleared";
    public static final String MSG_SETTINGS_NOT_COPIED_RETAINED = "message.justsomeswitches.settings_not_copied_retained";
    public static final String MSG_SETTINGS_COPIED = "message.justsomeswitches.settings_copied";
    public static final String MSG_SETTINGS_NOT_PASTED = "message.justsomeswitches.settings_not_pasted";
    public static final String MSG_PREVIOUS_SETTINGS_REMOVED = "message.justsomeswitches.previous_settings_removed";
    /**
     * NOT a message and NOT a translation key. A control sentinel compared with equals() in
     * BrushActionPayload and BrushOverwritePayload to mean "open the missing-block dialog instead of
     * showing text". On the frozen list. Never translate it, never pass it to a message helper.
     */
    public static final String MSG_MISSING_BLOCKS_GUI = "SHOW_MISSING_BLOCK_GUI";

    public static final String GUI_COPY_TEXTURE_TITLE = "gui.justsomeswitches.copy_texture.title";
    public static final String GUI_DIFFERENT_SETTINGS_FOUND = "gui.justsomeswitches.different_settings.title";
    public static final String GUI_SETTINGS_ALREADY_STORED = "gui.justsomeswitches.settings_already_stored.title";
    public static final String GUI_BLOCK_NOT_FOUND = "gui.justsomeswitches.block_not_found.title";
    public static final String GUI_BLOCKS_NOT_FOUND = "gui.justsomeswitches.blocks_not_found.title";
    
}
