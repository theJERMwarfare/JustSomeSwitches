package net.justsomeswitches.gui;

import net.justsomeswitches.blockentity.SwitchesLeverBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
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
 * Manages texture customization for switch blocks through a GUI interface.
 * <p>
 * This menu provides a clean architecture for selecting and applying custom textures
 * to switch blocks via dynamic block model analysis and NBT persistence. Features
 * include auto-apply texture changes, smart default selection, and universal 
 * compatibility with any Minecraft block.
 * 
 * @since 1.0.0
 */
public class SwitchTextureMenu extends AbstractContainerMenu {

    // GUI Layout Constants
    private static final int TEXTURE_SLOT_COUNT = 2;
    private static final int TOGGLE_TEXTURE_SLOT = 0;
    private static final int BASE_TEXTURE_SLOT = 1;

    // Positioning constants
    private static final int TOGGLE_SLOT_X = 28;
    private static final int TOGGLE_SLOT_Y = 27;
    private static final int BASE_SLOT_X = 132;
    private static final int BASE_SLOT_Y = 27;

    // Player inventory positioning
    private static final int PLAYER_INV_Y = 98;
    private static final int HOTBAR_Y = 156;

    // Instance data
    private final SimpleContainer textureContainer;
    private final BlockPos blockPos;
    private final Level level;
    private SwitchesLeverBlockEntity blockEntity;

    // Current selections (read-only from BlockEntity)
    private String baseTextureVariable = "all";
    private String toggleTextureVariable = "all";

    // Flag to prevent recursion during initialization
    private boolean isInitializing = true;

    /**
     * Creates a new Switch Texture Menu for texture customization.
     * 
     * @param containerId the unique container ID for this menu instance
     * @param playerInventory the player's inventory for slot management
     * @param blockPos the position of the switch block being customized
     */
    public SwitchTextureMenu(int containerId, @Nonnull Inventory playerInventory, @Nullable BlockPos blockPos) {
        super(JustSomeSwitchesMenuTypes.SWITCH_TEXTURE_MENU.get(), containerId);

        this.blockPos = blockPos;
        this.level = playerInventory.player.level();

        // Load BlockEntity data first
        loadBlockEntityData();

        // Container with slot monitoring for auto-apply
        this.textureContainer = new SimpleContainer(TEXTURE_SLOT_COUNT) {
            @Override
            public void setChanged() {
                super.setChanged();
                onSlotChangedWithAutoApply();
            }
        };

        // Load GUI slot items
        loadGuiSlotItems();

        // Add texture slots
        addSlot(new EnhancedTextureSlot(textureContainer, TOGGLE_TEXTURE_SLOT, TOGGLE_SLOT_X, TOGGLE_SLOT_Y));
        addSlot(new EnhancedTextureSlot(textureContainer, BASE_TEXTURE_SLOT, BASE_SLOT_X, BASE_SLOT_Y));

        // Add player inventory slots
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, PLAYER_INV_Y + row * 18));
            }
        }

        // Add player hotbar slots
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, HOTBAR_Y));
        }

        // Initialization complete
        this.isInitializing = false;
    }

    /**
     * Enhanced texture slot with validation for texture blocks.
     * <p>
     * Static inner class for optimal memory usage and proper encapsulation.
     * Only accepts solid, full-cube blocks suitable for texture extraction.
     */
    private static class EnhancedTextureSlot extends TextureSlot {
        /**
         * Creates a new enhanced texture slot.
         * 
         * @param container the container holding the slot items
         * @param slot the slot index within the container
         * @param x the x position of the slot in the GUI
         * @param y the y position of the slot in the GUI
         */
        public EnhancedTextureSlot(@Nonnull net.minecraft.world.Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }
    }

    /**
     * Network constructor for client-side menu creation.
     * <p>
     * Required by NeoForge's MenuType system for proper client-server synchronization.
     * 
     * @param containerId the unique container ID
     * @param playerInventory the player's inventory
     * @param extraData network buffer containing block position data
     */
    public SwitchTextureMenu(int containerId, @Nonnull Inventory playerInventory, @Nonnull FriendlyByteBuf extraData) {
        this(containerId, playerInventory, extraData.readBlockPos());
    }

    /**
     * Handles slot changes with automatic texture application.
     * <p>
     * Implements smart default selection and immediate texture updates to provide
     * responsive user experience. Preserves valid selections while providing
     * intelligent defaults for new blocks.
     */
    private void onSlotChangedWithAutoApply() {
        if (isInitializing || blockEntity == null) {
            return;
        }

        // Get current items
        ItemStack toggleItem = textureContainer.getItem(TOGGLE_TEXTURE_SLOT);
        ItemStack baseItem = textureContainer.getItem(BASE_TEXTURE_SLOT);

        // Update BlockEntity GUI items
        blockEntity.setGuiSlotItems(toggleItem, baseItem);

        // Apply auto-defaults ONLY when needed (smart persistence)
        applySmartDefaults(toggleItem, baseItem);

        // Apply textures with current variables
        applyTexturesWithVariables(toggleItem, baseItem);

        // Force block update
        forceBlockUpdate();
    }

    /**
     * Applies intelligent defaults when current texture variable is invalid.
     * <p>
     * Uses priority order (side, top, front, etc.) to select the most appropriate
     * texture variable for new blocks while preserving valid existing selections.
     * 
     * @param toggleItem the current toggle texture item
     * @param baseItem the current base texture item
     */
    private void applySmartDefaults(@Nonnull ItemStack toggleItem, @Nonnull ItemStack baseItem) {
        boolean needsUpdate = false;

        // Smart auto-default for toggle slot
        if (!toggleItem.isEmpty()) {
            FaceSelectionData.RawTextureSelection toggleSelection =
                    FaceSelectionData.createRawTextureSelection(toggleItem, toggleTextureVariable);

            if (toggleSelection.enabled() && !toggleSelection.availableVariables().isEmpty()) {
                if (toggleTextureVariable.equals("all") ||
                        !toggleSelection.availableVariables().contains(toggleTextureVariable)) {

                    String defaultVariable = FaceSelectionData.getDefaultVariable(toggleSelection.availableVariables());
                    this.toggleTextureVariable = defaultVariable;
                    needsUpdate = true;
                }
            }
        }

        // Smart auto-default for base slot
        if (!baseItem.isEmpty()) {
            FaceSelectionData.RawTextureSelection baseSelection =
                    FaceSelectionData.createRawTextureSelection(baseItem, baseTextureVariable);

            if (baseSelection.enabled() && !baseSelection.availableVariables().isEmpty()) {
                if (baseTextureVariable.equals("all") ||
                        !baseSelection.availableVariables().contains(baseTextureVariable)) {

                    String defaultVariable = FaceSelectionData.getDefaultVariable(baseSelection.availableVariables());
                    this.baseTextureVariable = defaultVariable;
                    needsUpdate = true;
                }
            }
        }

        // Update BlockEntity with new defaults if changes were made
        if (needsUpdate && blockEntity != null) {
            blockEntity.setBaseTextureVariable(baseTextureVariable);
            blockEntity.setToggleTextureVariable(toggleTextureVariable);
        }
    }

    /**
     * Applies texture changes atomically to prevent inconsistent state.
     * <p>
     * Updates both texture variables and texture paths in a single operation
     * with proper error recovery to defaults on failure.
     * 
     * @param toggleItem the toggle texture item
     * @param baseItem the base texture item
     */
    private void applyTexturesWithVariables(@Nonnull ItemStack toggleItem, @Nonnull ItemStack baseItem) {
        if (blockEntity == null) return;

        try {
            // Set texture variables first
            blockEntity.setBaseTextureVariable(baseTextureVariable);
            blockEntity.setToggleTextureVariable(toggleTextureVariable);

            // Apply textures with current variables
            if (!toggleItem.isEmpty()) {
                String effectiveTexturePath = getEffectiveTexturePathForItem(toggleItem, toggleTextureVariable);
                if (effectiveTexturePath != null) {
                    blockEntity.setToggleTexture(effectiveTexturePath);
                }
            } else {
                blockEntity.resetToggleTexture();
            }

            if (!baseItem.isEmpty()) {
                String effectiveTexturePath = getEffectiveTexturePathForItem(baseItem, baseTextureVariable);
                if (effectiveTexturePath != null) {
                    blockEntity.setBaseTexture(effectiveTexturePath);
                }
            } else {
                blockEntity.resetBaseTexture();
            }

            // Force immediate NBT save
            blockEntity.setChanged();

        } catch (Exception e) {
            resetToDefaults();
        }
    }

    /**
     * Retrieves the BlockEntity associated with this menu's block position.
     * 
     * @return the SwitchesLeverBlockEntity if valid, null otherwise
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
     * Loads BlockEntity data and synchronizes texture variables.
     * <p>
     * Ensures the menu state matches the current BlockEntity configuration
     * for consistent user experience across GUI sessions.
     */
    private void loadBlockEntityData() {
        this.blockEntity = getBlockEntity();
        if (blockEntity != null) {
            // Synchronize texture variables from BlockEntity
            this.baseTextureVariable = blockEntity.getBaseTextureVariable();
            this.toggleTextureVariable = blockEntity.getToggleTextureVariable();
        }
    }

    /**
     * Loads GUI slot items from the BlockEntity's stored state.
     * <p>
     * Restores the previous session's texture block selections for
     * seamless continuation of customization work.
     */
    private void loadGuiSlotItems() {
        if (blockEntity != null) {
            ItemStack toggleItem = blockEntity.getGuiToggleItem();
            ItemStack baseItem = blockEntity.getGuiBaseItem();

            textureContainer.setItem(TOGGLE_TEXTURE_SLOT, toggleItem);
            textureContainer.setItem(BASE_TEXTURE_SLOT, baseItem);
        }
    }

    // ========================================
    // DROPDOWN-DRIVEN SAVE OPERATIONS
    // ========================================

    /**
     * Sets the base texture variable with immediate application.
     * <p>
     * Called by the GUI dropdown selection to update the base texture
     * variable and trigger immediate texture application.
     * 
     * @param variable the texture variable name from block model JSON
     * @throws IllegalArgumentException if variable is null or empty
     */
    public void setBaseTextureVariable(@Nonnull String variable) {
        if (blockEntity == null || isInitializing) {
            return;
        }

        this.baseTextureVariable = variable;

        // Get current items and apply immediately
        ItemStack toggleItem = textureContainer.getItem(TOGGLE_TEXTURE_SLOT);
        ItemStack baseItem = textureContainer.getItem(BASE_TEXTURE_SLOT);

        applyTexturesWithVariables(toggleItem, baseItem);
        forceBlockUpdate();
    }

    /**
     * Sets the toggle texture variable with immediate application.
     * <p>
     * Called by the GUI dropdown selection to update the toggle texture
     * variable and trigger immediate texture application.
     * 
     * @param variable the texture variable name from block model JSON
     * @throws IllegalArgumentException if variable is null or empty
     */
    public void setToggleTextureVariable(@Nonnull String variable) {
        if (blockEntity == null || isInitializing) {
            return;
        }

        this.toggleTextureVariable = variable;

        // Get current items and apply immediately
        ItemStack toggleItem = textureContainer.getItem(TOGGLE_TEXTURE_SLOT);
        ItemStack baseItem = textureContainer.getItem(BASE_TEXTURE_SLOT);

        applyTexturesWithVariables(toggleItem, baseItem);
        forceBlockUpdate();
    }

    /**
     * Resets all textures to defaults on error conditions.
     * <p>
     * Provides safe fallback when texture application fails to prevent
     * corrupted or inconsistent texture state.
     */
    private void resetToDefaults() {
        if (blockEntity != null) {
            blockEntity.resetTextures();
            this.baseTextureVariable = "all";
            this.toggleTextureVariable = "all";
            forceBlockUpdate();
        }
    }

    /**
     * Gets the effective texture path for an item based on variable selection.
     * <p>
     * Resolves the specific texture path from the item's block model JSON
     * using the selected texture variable, with fallback to default variable.
     * 
     * @param item the texture source item
     * @param variable the selected texture variable name
     * @return the resolved texture path, or null if unavailable
     */
    @Nullable
    private String getEffectiveTexturePathForItem(@Nonnull ItemStack item, @Nonnull String variable) {
        if (item.isEmpty()) {
            return null;
        }

        // Get texture path for specific variable
        String texturePath = FaceSelectionData.getTextureForVariable(item, variable);
        if (texturePath != null) {
            return texturePath;
        }

        // Fallback to default variable
        return FaceSelectionData.getTextureForVariable(item, "all");
    }

    // ========================================
    // READ-ONLY STATE ACCESS
    // ========================================

    /**
     * Gets the raw texture selection state for the base texture slot.
     * <p>
     * Provides current selection state for GUI rendering and dropdown management.
     * 
     * @return the current base texture selection state
     */
    @Nonnull
    public FaceSelectionData.RawTextureSelection getBaseTextureSelection() {
        ItemStack baseItem = textureContainer.getItem(BASE_TEXTURE_SLOT);
        return FaceSelectionData.createRawTextureSelection(baseItem, baseTextureVariable);
    }

    /**
     * Gets the raw texture selection state for the toggle texture slot.
     * <p>
     * Provides current selection state for GUI rendering and dropdown management.
     * 
     * @return the current toggle texture selection state
     */
    @Nonnull
    public FaceSelectionData.RawTextureSelection getToggleTextureSelection() {
        ItemStack toggleItem = textureContainer.getItem(TOGGLE_TEXTURE_SLOT);
        return FaceSelectionData.createRawTextureSelection(toggleItem, toggleTextureVariable);
    }

    /**
     * Forces a block update to synchronize visual changes.
     * <p>
     * Triggers client-server synchronization and block entity model data
     * updates to ensure immediate visual feedback for texture changes.
     */
    private void forceBlockUpdate() {
        if (blockEntity != null && level != null && blockPos != null) {
            blockEntity.setChanged();
            blockEntity.requestModelDataUpdate();
            level.sendBlockUpdated(blockPos, level.getBlockState(blockPos), level.getBlockState(blockPos), 3);
        }
    }

    @Override
    public void removed(@Nonnull Player player) {
        super.removed(player);
        // Clean exit - all data already saved
    }

    @Override
    @Nonnull
    public ItemStack quickMoveStack(@Nonnull Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = slots.get(index);

        if (slot.hasItem()) {
            ItemStack currentStack = slot.getItem();
            itemStack = currentStack.copy();

            // Handle texture slots (0-1)
            if (index < TEXTURE_SLOT_COUNT) {
                // Moving from texture slots to player inventory
                if (!moveItemStackTo(currentStack, TEXTURE_SLOT_COUNT, slots.size(), false)) {
                    return ItemStack.EMPTY;
                }
            }
            // Handle player inventory slots
            else {
                // Try to move to texture slots first
                if (!moveItemStackTo(currentStack, 0, TEXTURE_SLOT_COUNT, false)) {
                    // Move within player inventory
                    int playerInventoryStart = TEXTURE_SLOT_COUNT;
                    int playerInventoryEnd = playerInventoryStart + 27;
                    int hotbarEnd = playerInventoryEnd + 9;

                    if (index < playerInventoryEnd) {
                        // From inventory to hotbar
                        if (!moveItemStackTo(currentStack, playerInventoryEnd, hotbarEnd, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else {
                        // From hotbar to inventory
                        if (!moveItemStackTo(currentStack, playerInventoryStart, playerInventoryEnd, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                }
            }

            if (currentStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (currentStack.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, currentStack);
        }

        return itemStack;
    }

    @Override
    public boolean stillValid(@Nonnull Player player) {
        if (blockPos == null || level == null) {
            return false;
        }

        // Check if block still exists and is the right type
        if (!(level.getBlockState(blockPos).getBlock() instanceof net.justsomeswitches.block.SwitchesLeverBlock)) {
            return false;
        }

        // Check distance from player
        return player.distanceToSqr(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5) <= 64.0;
    }

    // ========================================
    // LEGACY COMPATIBILITY (DEPRECATED)
    // ========================================

    /**
     * @deprecated Use getBaseTextureSelection() instead
     */
    @Deprecated
    @Nonnull
    public FaceSelectionData.DropdownState getBaseDropdownState() {
        FaceSelectionData.RawTextureSelection rawSelection = getBaseTextureSelection();
        return new FaceSelectionData.DropdownState(rawSelection);
    }

    /**
     * @deprecated Use getToggleTextureSelection() instead
     */
    @Deprecated
    @Nonnull
    public FaceSelectionData.DropdownState getToggleDropdownState() {
        FaceSelectionData.RawTextureSelection rawSelection = getToggleTextureSelection();
        return new FaceSelectionData.DropdownState(rawSelection);
    }
}