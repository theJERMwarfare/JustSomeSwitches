package net.justsomeswitches.gui;

import net.justsomeswitches.blockentity.SwitchesLeverBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Server-side container menu for the Switch Texture customization GUI
 * ---
 * Phase 3C Enhancement: Added comprehensive debugging to trace GUI-BlockEntity interaction
 */
public class SwitchTextureMenu extends AbstractContainerMenu {

    private static final int TEXTURE_SLOT_COUNT = 2;
    public static final int TOGGLE_TEXTURE_SLOT = 0;
    public static final int BASE_TEXTURE_SLOT = 1;

    // FIXED slot positioning for proper item alignment - moved right and down 1 pixel
    private static final int TOGGLE_SLOT_X = 63;  // Moved right 1 pixel for proper item positioning
    private static final int TOGGLE_SLOT_Y = 36;  // Moved down 1 pixel for proper item positioning
    private static final int BASE_SLOT_X = 99;    // Moved right 1 pixel for proper item positioning
    private static final int BASE_SLOT_Y = 36;    // Moved down 1 pixel for proper item positioning

    // FIXED player inventory positioning for proper item alignment - moved right and down 1 pixel
    private static final int PLAYER_INV_X = 9;    // Moved right 1 pixel for proper item positioning
    private static final int PLAYER_INV_Y = 95;   // Moved down 1 pixel for proper item positioning
    private static final int HOTBAR_X = 9;        // Moved right 1 pixel for proper item positioning
    private static final int HOTBAR_Y = 153;      // Moved down 1 pixel for proper item positioning

    // BlockEntity integration fields
    private final SimpleContainer textureContainer;
    private final BlockPos blockPos;
    private final Level level;
    private SwitchesLeverBlockEntity blockEntity;

    /**
     * Constructor for the Switch Texture Menu with BlockEntity integration
     * ---
     * @param containerId The container ID
     * @param playerInventory The player's inventory
     * @param blockPos The position of the switch block (can be null for fallback)
     */
    public SwitchTextureMenu(int containerId, @Nonnull Inventory playerInventory, @Nullable BlockPos blockPos) {
        super(JustSomeSwitchesMenuTypes.SWITCH_TEXTURE_MENU.get(), containerId);

        this.blockPos = blockPos;
        this.level = playerInventory.player.level();
        this.textureContainer = new SimpleContainer(TEXTURE_SLOT_COUNT);

        System.out.println("Phase 3C Debug: SwitchTextureMenu constructor - BlockPos: " + blockPos);

        // Try to get the BlockEntity and load GUI slot data
        loadGuiSlotData();

        // Add texture slots
        addSlot(new TextureSlot(textureContainer, TOGGLE_TEXTURE_SLOT, TOGGLE_SLOT_X, TOGGLE_SLOT_Y));
        addSlot(new TextureSlot(textureContainer, BASE_TEXTURE_SLOT, BASE_SLOT_X, BASE_SLOT_Y));

        // Add player inventory
        addPlayerInventory(playerInventory);
    }

    /**
     * Loads GUI slot data from the BlockEntity into the container
     * This represents what the player has placed in the slots (persistent)
     */
    private void loadGuiSlotData() {
        if (blockPos != null && level != null) {
            var be = level.getBlockEntity(blockPos);
            System.out.println("Phase 3C Debug: loadGuiSlotData - BlockEntity: " + be);

            if (be instanceof SwitchesLeverBlockEntity switchEntity) {
                this.blockEntity = switchEntity;
                System.out.println("Phase 3C Debug: loadGuiSlotData - Found SwitchesLeverBlockEntity");

                // Load the GUI slot contents (what player has placed in slots)
                ItemStack toggleItem = switchEntity.getGuiToggleItem();
                ItemStack baseItem = switchEntity.getGuiBaseItem();

                System.out.println("Phase 3C Debug: loadGuiSlotData - Loading Toggle: " + toggleItem + ", Base: " + baseItem);

                textureContainer.setItem(TOGGLE_TEXTURE_SLOT, toggleItem);
                textureContainer.setItem(BASE_TEXTURE_SLOT, baseItem);
            } else {
                System.out.println("Phase 3C Debug: loadGuiSlotData - BlockEntity is not SwitchesLeverBlockEntity: " + (be != null ? be.getClass() : "null"));
            }
        } else {
            System.out.println("Phase 3C Debug: loadGuiSlotData - blockPos or level is null");
        }
    }

    /**
     * Saves GUI slot data from the container to the BlockEntity
     * This preserves what the player has placed in the slots
     */
    private void saveGuiSlotData() {
        if (blockEntity != null) {
            // Save the current GUI slot contents for persistence
            ItemStack toggleItem = textureContainer.getItem(TOGGLE_TEXTURE_SLOT);
            ItemStack baseItem = textureContainer.getItem(BASE_TEXTURE_SLOT);

            System.out.println("Phase 3C Debug: saveGuiSlotData - Saving Toggle: " + toggleItem + ", Base: " + baseItem);

            blockEntity.setGuiSlotItems(toggleItem, baseItem);
        } else {
            System.out.println("Phase 3C Debug: saveGuiSlotData - blockEntity is null!");
        }
    }

    /**
     * Applies the current textures from GUI slots to the switch
     * Called when Apply button is clicked (Phase 3B feature)
     */
    public void applyTextures() {
        System.out.println("Phase 3C Debug: applyTextures called");

        if (blockEntity != null) {
            // Get items from the GUI slots
            ItemStack toggleItem = textureContainer.getItem(TOGGLE_TEXTURE_SLOT);
            ItemStack baseItem = textureContainer.getItem(BASE_TEXTURE_SLOT);

            System.out.println("Phase 3C Debug: applyTextures - Toggle item: " + toggleItem);
            System.out.println("Phase 3C Debug: applyTextures - Base item: " + baseItem);

            // Apply or reset toggle texture based on slot content
            if (!toggleItem.isEmpty()) {
                boolean success = blockEntity.setToggleTexture(toggleItem);
                System.out.println("Phase 3C Debug: applyTextures - Toggle texture set success: " + success);
            } else {
                // Reset toggle texture to default if slot is empty
                System.out.println("Phase 3C Debug: applyTextures - Resetting toggle texture to default");
                blockEntity.resetToggleTexture();
            }

            // Apply or reset base texture based on slot content
            if (!baseItem.isEmpty()) {
                boolean success = blockEntity.setBaseTexture(baseItem);
                System.out.println("Phase 3C Debug: applyTextures - Base texture set success: " + success);
            } else {
                // Reset base texture to default if slot is empty
                System.out.println("Phase 3C Debug: applyTextures - Resetting base texture to default");
                blockEntity.resetBaseTexture();
            }

            System.out.println("Phase 3C Debug: applyTextures - Final textures - Base: " + blockEntity.getBaseTexture() + ", Toggle: " + blockEntity.getToggleTexture());
        } else {
            System.out.println("Phase 3C Debug: applyTextures - blockEntity is null!");
        }
    }

    /**
     * Checks if the menu has a valid BlockEntity connection
     * @return true if connected to a BlockEntity
     */
    public boolean hasValidBlockEntity() {
        return blockEntity != null;
    }

    /**
     * Gets the block position this menu is connected to
     * @return The block position or null if not connected
     */
    @Nullable
    public BlockPos getBlockPos() {
        return blockPos;
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
        // Check if the player is still close enough to the switch block
        if (blockPos != null && level != null) {
            return player.distanceToSqr(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5) <= 64.0;
        }
        return true; // Fallback for edge cases
    }

    @Override
    public void removed(@Nonnull Player player) {
        super.removed(player);

        System.out.println("Phase 3C Debug: GUI removed - auto-applying textures and saving slot data");

        // Auto-apply textures when GUI closes (as requested)
        applyTextures();

        // Save GUI slot contents when closing so items persist
        saveGuiSlotData();
    }

    @Nonnull
    public SimpleContainer getTextureContainer() {
        return textureContainer;
    }
}