package net.justsomeswitches.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Debug utilities for preventing spam and duplicate messages
 */
public class DebugUtils {

    private static final Map<String, Long> lastMessageTime = new HashMap<>();
    private static final Map<String, Integer> messageCount = new HashMap<>();
    private static final long MIN_INTERVAL_MS = 1000; // Minimum 1 second between identical messages

    /**
     * Log a message only if it hasn't been logged recently
     */
    public static void logThrottled(String category, String message) {
        String key = category + ":" + message;
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastMessageTime.get(key);

        if (lastTime == null || (currentTime - lastTime) > MIN_INTERVAL_MS) {
            Integer count = messageCount.get(key);
            if (count != null && count > 1) {
                System.out.println("DEBUG " + category + ": " + message + " (repeated " + count + " times)");
                messageCount.put(key, 1);
            } else {
                System.out.println("DEBUG " + category + ": " + message);
                messageCount.put(key, 1);
            }
            lastMessageTime.put(key, currentTime);
        } else {
            messageCount.put(key, messageCount.getOrDefault(key, 0) + 1);
        }
    }

    /**
     * Log a message only once per session
     */
    public static void logOnce(String category, String message) {
        String key = "ONCE:" + category + ":" + message;
        if (!lastMessageTime.containsKey(key)) {
            System.out.println("DEBUG " + category + ": " + message);
            lastMessageTime.put(key, System.currentTimeMillis());
        }
    }

    /**
     * Clear the message cache (for memory management)
     */
    public static void clearCache() {
        lastMessageTime.clear();
        messageCount.clear();
    }
}