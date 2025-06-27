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
 * Phase 3C Fix: Fixed GUI closing logic to preserve applied textures
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

        // Add player inventory slots (3x9 grid) - FIXED: corrected positioning
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, PLAYER_INV_X + col * 18, PLAYER_INV_Y + row * 18));
            }
        }

        // Add player hotbar slots (1x9 grid) - FIXED: corrected positioning
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, HOTBAR_X + col * 18, HOTBAR_Y));
        }
    }

    /**
     * Gets the BlockEntity for this menu's position
     */
    @Nullable
    private SwitchesLeverBlockEntity getBlockEntity() {
        if (blockPos != null && level != null) {
            var blockEntity = level.getBlockEntity(blockPos);
            if (blockEntity instanceof SwitchesLeverBlockEntity switchBlockEntity) {
                return switchBlockEntity;
            }
        }
        return null;
    }

    /**
     * Loads GUI slot data from the BlockEntity (for persistence)
     */
    private void loadGuiSlotData() {
        this.blockEntity = getBlockEntity();
        if (blockEntity != null) {
            System.out.println("Phase 3C Debug: Menu initialized with BlockEntity at " + blockPos);

            // Load GUI slot contents from BlockEntity
            ItemStack toggleItem = blockEntity.getGuiToggleItem();
            ItemStack baseItem = blockEntity.getGuiBaseItem();

            textureContainer.setItem(TOGGLE_TEXTURE_SLOT, toggleItem);
            textureContainer.setItem(BASE_TEXTURE_SLOT, baseItem);

            System.out.println("Phase 3C Debug: Loaded GUI slots - Toggle: " + toggleItem + ", Base: " + baseItem);
        }
    }

    /**
     * Saves GUI slot data to the BlockEntity (for persistence)
     */
    private void saveGuiSlotData() {
        if (blockEntity != null) {
            ItemStack toggleItem = textureContainer.getItem(TOGGLE_TEXTURE_SLOT);
            ItemStack baseItem = textureContainer.getItem(BASE_TEXTURE_SLOT);

            blockEntity.setGuiSlotItems(toggleItem, baseItem);

            System.out.println("Phase 3C Debug: Saved GUI slots - Toggle: " + toggleItem + ", Base: " + baseItem);
        }
    }

    /**
     * Checks if there's a valid BlockEntity for this menu
     */
    public boolean hasValidBlockEntity() {
        return blockEntity != null;
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

            // Store GUI slot items for persistence
            blockEntity.setGuiSlotItems(toggleItem, baseItem);
            System.out.println("Phase 3C Debug: GUI slot items updated - Toggle: " + toggleItem + ", Base: " + baseItem);

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

            // Request client-side visual update
            blockEntity.requestModelDataUpdate();
            blockEntity.setChanged();

            System.out.println("Phase 3C Debug: Requested client-side model data update");
            System.out.println("Phase 3C Debug: Triggered block update for visual refresh");
            System.out.println("Phase 3C Debug: applyTextures - Final textures - Base: " + blockEntity.getBaseTexture() + ", Toggle: " + blockEntity.getToggleTexture());
        } else {
            System.out.println("Phase 3C Debug: applyTextures - blockEntity is null!");
        }
    }

    /**
     * Auto-apply textures when GUI closes, but only if slots contain items
     * ---
     * Phase 3C Fix: Only apply if there are items in slots to prevent unwanted resets
     */
    private void autoApplyTexturesIfItemsPresent() {
        if (blockEntity != null) {
            // Get items from the GUI slots
            ItemStack toggleItem = textureContainer.getItem(TOGGLE_TEXTURE_SLOT);
            ItemStack baseItem = textureContainer.getItem(BASE_TEXTURE_SLOT);

            // Only auto-apply if there are actually items in the slots
            boolean hasToggleItem = !toggleItem.isEmpty();
            boolean hasBaseItem = !baseItem.isEmpty();

            if (hasToggleItem || hasBaseItem) {
                System.out.println("Phase 3C Debug: Menu closing - auto-applying textures (items present)");
                applyTextures();
            } else {
                System.out.println("Phase 3C Debug: Menu closing - preserving existing textures (no items in slots)");
                // Just save slot data for persistence, don't change applied textures
                saveGuiSlotData();
            }
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

            // Handle texture slots (slots 0-1)
            if (slotIndex < TEXTURE_SLOT_COUNT) {
                // Move from texture slot to player inventory
                if (!this.moveItemStackTo(slotStack, TEXTURE_SLOT_COUNT, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Move from player inventory to texture slots (only if valid)
                if (TextureSlot.isValidTextureItem(slotStack)) {
                    // Try toggle slot first, then base slot
                    if (!this.moveItemStackTo(slotStack, TOGGLE_TEXTURE_SLOT, TOGGLE_TEXTURE_SLOT + 1, false)) {
                        if (!this.moveItemStackTo(slotStack, BASE_TEXTURE_SLOT, BASE_TEXTURE_SLOT + 1, false)) {
                            return ItemStack.EMPTY;
                        }
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

        System.out.println("Phase 3C Debug: Menu closing - auto-applying textures");

        // Auto-apply textures when GUI closes, but only if slots have items
        autoApplyTexturesIfItemsPresent();
    }

    @Nonnull
    public SimpleContainer getTextureContainer() {
        return textureContainer;
    }
}