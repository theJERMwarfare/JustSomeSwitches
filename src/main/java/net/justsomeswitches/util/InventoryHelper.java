package net.justsomeswitches.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
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

    /** Checks if player has required items using single-pass inventory scan. */
    public static boolean hasAllItems(@Nonnull Player player, @Nonnull ItemStack... requiredItems) {
        if (player.getAbilities().instabuild) {
            return true;
        }

        if (requiredItems.length == 0) {
            return true;
        }

        // Key by Item so multiple same-type requirements sum into one count (consistent with the
        // isSameItem matching below). ItemStack has no value-based equals/hashCode, so keying by the
        // stack object miscounts two same-type requirements as two independent single-item needs.
        Map<Item, Integer> requirements = new HashMap<>();
        for (ItemStack item : requiredItems) {
            if (!item.isEmpty()) {
                requirements.merge(item.getItem(), 1, Integer::sum);
            }
        }

        if (requirements.isEmpty()) {
            return true;
        }

        Map<Item, Integer> found = new HashMap<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stackInSlot = player.getInventory().getItem(i);
            if (stackInSlot.isEmpty()) continue;
            Item slotItem = stackInSlot.getItem();
            if (requirements.containsKey(slotItem)) {
                found.merge(slotItem, stackInSlot.getCount(), Integer::sum);
            }
        }

        for (Map.Entry<Item, Integer> requirement : requirements.entrySet()) {
            if (found.getOrDefault(requirement.getKey(), 0) < requirement.getValue()) {
                return false;
            }
        }

        return true;
    }

    /** Removes multiple items from inventory using single-pass scan. */
    public static void removeItems(@Nonnull Player player, @Nonnull ItemStack... itemsToRemove) {
        if (player.getAbilities().instabuild) {
            return;
        }

        if (itemsToRemove.length == 0) {
            return;
        }

        // Key by Item so multiple same-type entries sum into one total to remove (see hasAllItems).
        Map<Item, Integer> toRemove = new HashMap<>();
        for (ItemStack item : itemsToRemove) {
            if (!item.isEmpty()) {
                toRemove.merge(item.getItem(), 1, Integer::sum);
            }
        }

        if (toRemove.isEmpty()) {
            return;
        }

        for (int i = 0; i < player.getInventory().getContainerSize() && !toRemove.isEmpty(); i++) {
            ItemStack stackInSlot = player.getInventory().getItem(i);
            if (stackInSlot.isEmpty()) continue;
            Item slotItem = stackInSlot.getItem();
            Integer remaining = toRemove.get(slotItem);
            if (remaining == null) continue;
            int toShrink = Math.min(remaining, stackInSlot.getCount());
            stackInSlot.shrink(toShrink);
            remaining -= toShrink;
            if (remaining <= 0) {
                toRemove.remove(slotItem);
            } else {
                toRemove.put(slotItem, remaining);
            }
        }
    }


}
