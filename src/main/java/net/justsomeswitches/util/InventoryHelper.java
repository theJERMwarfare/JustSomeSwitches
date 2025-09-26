package net.justsomeswitches.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for optimized inventory operations
 */
public class InventoryHelper {
    
    private InventoryHelper() {
        // Utility class
    }
    
    /**
     * Efficiently checks if player has required items in inventory
     * Uses a single pass through inventory to check multiple items
     */
    public static boolean hasAllItems(@Nonnull Player player, @Nonnull ItemStack... requiredItems) {
        if (player.getAbilities().instabuild) {
            return true; // Creative mode has all items
        }
        
        if (requiredItems.length == 0) {
            return true;
        }
        
        // Build requirements map for efficient checking
        Map<ItemStack, Integer> requirements = new HashMap<>();
        for (ItemStack item : requiredItems) {
            if (!item.isEmpty()) {
                requirements.put(item, requirements.getOrDefault(item, 0) + 1);
            }
        }
        
        if (requirements.isEmpty()) {
            return true;
        }
        
        // Single pass through inventory
        Map<ItemStack, Integer> found = new HashMap<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stackInSlot = player.getInventory().getItem(i);
            if (stackInSlot.isEmpty()) continue;
            
            for (ItemStack required : requirements.keySet()) {
                if (ItemStack.isSameItem(stackInSlot, required)) {
                    int currentFound = found.getOrDefault(required, 0);
                    found.put(required, currentFound + stackInSlot.getCount());
                }
            }
        }
        
        // Check if all requirements are met
        for (Map.Entry<ItemStack, Integer> requirement : requirements.entrySet()) {
            int foundCount = found.getOrDefault(requirement.getKey(), 0);
            if (foundCount < requirement.getValue()) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Efficiently removes multiple items from inventory in a single pass
     */
    public static void removeItems(@Nonnull Player player, @Nonnull ItemStack... itemsToRemove) {
        if (player.getAbilities().instabuild) {
            return; // Don't consume in creative
        }
        
        if (itemsToRemove.length == 0) {
            return;
        }
        
        // Build removal map
        Map<ItemStack, Integer> toRemove = new HashMap<>();
        for (ItemStack item : itemsToRemove) {
            if (!item.isEmpty()) {
                toRemove.put(item, toRemove.getOrDefault(item, 0) + 1);
            }
        }
        
        if (toRemove.isEmpty()) {
            return;
        }
        
        // Single pass removal
        for (int i = 0; i < player.getInventory().getContainerSize() && !toRemove.isEmpty(); i++) {
            ItemStack stackInSlot = player.getInventory().getItem(i);
            if (stackInSlot.isEmpty()) continue;
            
            for (ItemStack required : toRemove.keySet()) {
                if (ItemStack.isSameItem(stackInSlot, required)) {
                    int remaining = toRemove.get(required);
                    int toShrink = Math.min(remaining, stackInSlot.getCount());
                    stackInSlot.shrink(toShrink);
                    
                    remaining -= toShrink;
                    if (remaining <= 0) {
                        toRemove.remove(required);
                    } else {
                        toRemove.put(required, remaining);
                    }
                    break;
                }
            }
        }
    }
    
    // Removed unused checkAndRemoveItems method
}
