package net.justsomeswitches.gui;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side container menu for the Switch Texture customization GUI
 */
public class SwitchTextureMenu extends AbstractContainerMenu {

    // Static storage for persistence (temporary solution until Phase 3)
    private static final ConcurrentHashMap<String, SimpleContainer> TEXTURE_STORAGE = new ConcurrentHashMap<>();
    private static final String DEFAULT_KEY = "default_switch";

    private static final int TEXTURE_SLOT_COUNT = 2;
    public static final int TOGGLE_TEXTURE_SLOT = 0;
    public static final int BASE_TEXTURE_SLOT = 1;

    // Updated slot positioning to match the new screen layout
    private static final int TOGGLE_SLOT_X = 62;
    private static final int TOGGLE_SLOT_Y = 27;    // Moved down to match screen
    private static final int BASE_SLOT_X = 98;
    private static final int BASE_SLOT_Y = 27;      // Moved down to match screen

    // Player inventory positioning
    private static final int PLAYER_INV_X = 8;
    private static final int PLAYER_INV_Y = 58;     // Adjusted for new layout
    private static final int HOTBAR_X = 8;
    private static final int HOTBAR_Y = 116;        // Adjusted for new layout

    private final SimpleContainer textureContainer;
    private final String storageKey;

    /**
     * Constructor for the Switch Texture Menu
     */
    public SwitchTextureMenu(int containerId, @Nonnull Inventory playerInventory) {
        super(JustSomeSwitchesMenuTypes.SWITCH_TEXTURE_MENU.get(), containerId);

        // Use default key for now - Phase 3 will use block position
        this.storageKey = DEFAULT_KEY;

        // Get or create persistent container
        this.textureContainer = TEXTURE_STORAGE.computeIfAbsent(storageKey, k -> new SimpleContainer(TEXTURE_SLOT_COUNT));

        // Add texture slots
        addSlot(new TextureSlot(textureContainer, TOGGLE_TEXTURE_SLOT, TOGGLE_SLOT_X, TOGGLE_SLOT_Y));
        addSlot(new TextureSlot(textureContainer, BASE_TEXTURE_SLOT, BASE_SLOT_X, BASE_SLOT_Y));

        // Add player inventory
        addPlayerInventory(playerInventory);
    }

    private void addPlayerInventory(@Nonnull Inventory playerInventory) {
        // Player main inventory (3 rows of 9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = col + row * 9 + 9;  // +9 for hotbar
                int x = PLAYER_INV_X + col * 18;
                int y = PLAYER_INV_Y + row * 18;
                addSlot(new Slot(playerInventory, slotIndex, x, y));
            }
        }

        // Player hotbar
        for (int col = 0; col < 9; col++) {
            int x = HOTBAR_X + col * 18;
            addSlot(new Slot(playerInventory, col, x, HOTBAR_Y));
        }
    }

    @Override
    @Nonnull
    public ItemStack quickMoveStack(@Nonnull Player player, int slotIndex) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemStack = slotStack.copy();

            if (slotIndex < TEXTURE_SLOT_COUNT) {
                // Move from texture slots to player inventory
                if (!this.moveItemStackTo(slotStack, TEXTURE_SLOT_COUNT, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Move from player inventory to texture slots (if valid block)
                if (slotStack.getItem() instanceof net.minecraft.world.item.BlockItem) {
                    if (!this.moveItemStackTo(slotStack, 0, TEXTURE_SLOT_COUNT, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemStack;
    }

    @Override
    public boolean stillValid(@Nonnull Player player) {
        return true;
    }

    @Override
    public void removed(@Nonnull Player player) {
        super.removed(player);
        // Items now persist in static storage - no dropping!
        // The texture data will remain until Phase 3 implements proper NBT persistence
    }

    @Nonnull
    public SimpleContainer getTextureContainer() {
        return textureContainer;
    }

    /**
     * Clear all texture data (for testing/debugging)
     */
    public static void clearAllTextureData() {
        TEXTURE_STORAGE.clear();
    }
}