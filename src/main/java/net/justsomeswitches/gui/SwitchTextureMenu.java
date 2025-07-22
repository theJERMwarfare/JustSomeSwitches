package net.justsomeswitches.gui;

import net.justsomeswitches.blockentity.SwitchesLeverBlockEntity;
import net.justsomeswitches.init.JustSomeSwitchesModBlocks;
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
 * Switch Texture Menu with Clean Single-Path Architecture - GUI REFINED
 * UPDATED: Adjusted slot positions for GUI background refinement
 */
public class SwitchTextureMenu extends AbstractContainerMenu {

    // GUI Layout Constants
    private static final int TEXTURE_SLOT_COUNT = 2;
    private static final int TOGGLE_TEXTURE_SLOT = 0;
    private static final int BASE_TEXTURE_SLOT = 1;

    // Positioning constants - UPDATED for GUI refinement
    private static final int TOGGLE_SLOT_X = 28;
    private static final int TOGGLE_SLOT_Y = 27; // Moved up 1 pixel
    private static final int BASE_SLOT_X = 132;
    private static final int BASE_SLOT_Y = 27; // Moved up 1 pixel

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
     * Constructor for the Switch Texture Menu
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

        // Add texture slots with updated positions
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
     * Enhanced texture slot for analysis-only functionality
     */
    private class EnhancedTextureSlot extends TextureSlot {
        public EnhancedTextureSlot(@Nonnull net.minecraft.world.Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }
    }

    /**
     * Network constructor for client-side menu creation
     */
    public SwitchTextureMenu(int containerId, @Nonnull Inventory playerInventory, @Nonnull FriendlyByteBuf extraData) {
        this(containerId, playerInventory, extraData.readBlockPos());
    }

    /**
     * Slot changes trigger auto-apply with smart default selection
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
     * Apply auto-defaults ONLY when current variable is invalid
     */
    private void applySmartDefaults(@Nonnull ItemStack toggleItem, @Nonnull ItemStack baseItem) {
        boolean needsUpdate = false;

        // Smart auto-default for toggle slot
        if (!toggleItem.isEmpty()) {
            FaceSelectionData.RawTextureSelection toggleSelection =
                    FaceSelectionData.createRawTextureSelection(toggleItem, toggleTextureVariable);

            if (toggleSelection.isEnabled() && !toggleSelection.getAvailableVariables().isEmpty()) {
                if (toggleTextureVariable.equals("all") ||
                        !toggleSelection.getAvailableVariables().contains(toggleTextureVariable)) {

                    String defaultVariable = FaceSelectionData.getDefaultVariable(toggleSelection.getAvailableVariables());
                    this.toggleTextureVariable = defaultVariable;
                    needsUpdate = true;
                    System.out.println("AUTO-DEFAULT: Toggle slot set to " + defaultVariable);
                }
            }
        }

        // Smart auto-default for base slot
        if (!baseItem.isEmpty()) {
            FaceSelectionData.RawTextureSelection baseSelection =
                    FaceSelectionData.createRawTextureSelection(baseItem, baseTextureVariable);

            if (baseSelection.isEnabled() && !baseSelection.getAvailableVariables().isEmpty()) {
                if (baseTextureVariable.equals("all") ||
                        !baseSelection.getAvailableVariables().contains(baseTextureVariable)) {

                    String defaultVariable = FaceSelectionData.getDefaultVariable(baseSelection.getAvailableVariables());
                    this.baseTextureVariable = defaultVariable;
                    needsUpdate = true;
                    System.out.println("AUTO-DEFAULT: Base slot set to " + defaultVariable);
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
     * Apply textures and variables atomically
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
            System.out.println("ERROR: Failed to apply textures - " + e.getMessage());
            resetToDefaults();
        }
    }

    /**
     * Get BlockEntity for this menu's position
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
     * Load BlockEntity data and synchronize texture variables
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
     * Load GUI slot items from BlockEntity
     */
    private void loadGuiSlotItems() {
        if (blockEntity != null) {
            ItemStack toggleItem = blockEntity.getGuiToggleItem();
            ItemStack baseItem = blockEntity.getGuiBaseItem();

            textureContainer.setItem(TOGGLE_TEXTURE_SLOT, toggleItem);
            textureContainer.setItem(BASE_TEXTURE_SLOT, baseItem);
        }
    }

    /**
     * Check if there's a valid BlockEntity
     */
    public boolean hasValidBlockEntity() {
        return blockEntity != null;
    }

    // ========================================
    // DROPDOWN-DRIVEN SAVE OPERATIONS
    // ========================================

    /**
     * Set base texture variable with immediate apply
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
     * Set toggle texture variable with immediate apply
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
     * Reset to defaults on error
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
     * Get effective texture path for item based on variable selection
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
     * Get raw texture selection for base texture slot
     */
    @Nonnull
    public FaceSelectionData.RawTextureSelection getBaseTextureSelection() {
        ItemStack baseItem = textureContainer.getItem(BASE_TEXTURE_SLOT);
        return FaceSelectionData.createRawTextureSelection(baseItem, baseTextureVariable);
    }

    /**
     * Get raw texture selection for toggle texture slot
     */
    @Nonnull
    public FaceSelectionData.RawTextureSelection getToggleTextureSelection() {
        ItemStack toggleItem = textureContainer.getItem(TOGGLE_TEXTURE_SLOT);
        return FaceSelectionData.createRawTextureSelection(toggleItem, toggleTextureVariable);
    }

    /**
     * Get current texture variables (read-only)
     */
    @Nonnull public String getBaseTextureVariable() { return baseTextureVariable; }
    @Nonnull public String getToggleTextureVariable() { return toggleTextureVariable; }

    /**
     * Force block update for visual changes
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