package net.justsomeswitches.gui;

import net.justsomeswitches.blockentity.SwitchesLeverBlockEntity;
import net.justsomeswitches.config.DebugConfig;
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
 * FIXED: Enhanced GUI state synchronization with BlockEntity
 */
public class SwitchTextureMenu extends AbstractContainerMenu {

    private static final int TEXTURE_SLOT_COUNT = 2;
    private static final int TOGGLE_TEXTURE_SLOT = 0;
    private static final int BASE_TEXTURE_SLOT = 1;

    // Positioning constants
    private static final int TOGGLE_SLOT_X = 28;
    private static final int TOGGLE_SLOT_Y = 28;
    private static final int BASE_SLOT_X = 132;
    private static final int BASE_SLOT_Y = 28;
    private static final int PLAYER_INV_Y = 98;
    private static final int HOTBAR_Y = 156;

    private final SimpleContainer textureContainer;
    private final BlockPos blockPos;
    private final Level level;

    // Enhanced state tracking for proper sync
    private ItemStack lastToggleItem = ItemStack.EMPTY;
    private ItemStack lastBaseItem = ItemStack.EMPTY;
    private boolean initialLoadComplete = false;

    /**
     * FIXED: Enhanced constructor with proper BlockEntity state loading
     */
    public SwitchTextureMenu(int containerId, @Nonnull Inventory playerInventory, @Nullable BlockPos blockPos) {
        super(JustSomeSwitchesMenuTypes.SWITCH_TEXTURE_MENU.get(), containerId);

        this.blockPos = blockPos;
        this.level = playerInventory.player.level();

        // Enhanced container with smart change detection
        this.textureContainer = new SimpleContainer(TEXTURE_SLOT_COUNT) {
            @Override
            public void setChanged() {
                super.setChanged();
                // Only trigger auto-apply after initial load is complete
                if (initialLoadComplete) {
                    onSlotChangedWithAutoApply();
                }
            }
        };

        // CRITICAL: Load initial state from BlockEntity BEFORE creating slots
        loadInitialStateFromBlockEntity();

        // Add texture slots
        addSlot(new TextureSlot(textureContainer, TOGGLE_TEXTURE_SLOT, TOGGLE_SLOT_X, TOGGLE_SLOT_Y));
        addSlot(new TextureSlot(textureContainer, BASE_TEXTURE_SLOT, BASE_SLOT_X, BASE_SLOT_Y));

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

        // Mark initial load as complete
        initialLoadComplete = true;

        DebugConfig.logSuccess("GUI initialized with preserved state");
    }

    /**
     * Network constructor
     */
    public SwitchTextureMenu(int containerId, @Nonnull Inventory playerInventory, @Nonnull FriendlyByteBuf extraData) {
        this(containerId, playerInventory, extraData.readBlockPos());
    }

    /**
     * CRITICAL FIX: Load complete initial state from BlockEntity
     */
    private void loadInitialStateFromBlockEntity() {
        SwitchesLeverBlockEntity blockEntity = getBlockEntity();
        if (blockEntity != null) {
            // Load slot items
            ItemStack toggleItem = blockEntity.getGuiToggleItem();
            ItemStack baseItem = blockEntity.getGuiBaseItem();

            // Set container items WITHOUT triggering change notifications
            textureContainer.setItem(TOGGLE_TEXTURE_SLOT, toggleItem);
            textureContainer.setItem(BASE_TEXTURE_SLOT, baseItem);

            // Track initial state to prevent false change detection
            lastToggleItem = toggleItem.copy();
            lastBaseItem = baseItem.copy();

            // Log current state for validation
            DebugConfig.logPersistence("GUI loaded - Base face: " + blockEntity.getBaseFaceSelection() +
                    ", Toggle face: " + blockEntity.getToggleFaceSelection());

            // Validate that face selections are correctly loaded
            FaceSelectionData.FaceOption baseFace = blockEntity.getBaseFaceSelection();
            FaceSelectionData.FaceOption toggleFace = blockEntity.getToggleFaceSelection();

            if (baseFace == null || toggleFace == null) {
                DebugConfig.logCritical("NULL face selections detected in BlockEntity!");
            }

            // Validate dropdown states
            FaceSelectionData.DropdownState baseDropdown = blockEntity.getBaseDropdownState();
            FaceSelectionData.DropdownState toggleDropdown = blockEntity.getToggleDropdownState();

            if (baseDropdown.getSelectedOption() != baseFace) {
                DebugConfig.logValidationFailure("Base dropdown sync", baseFace.toString(), baseDropdown.getSelectedOption().toString());
            }

            if (toggleDropdown.getSelectedOption() != toggleFace) {
                DebugConfig.logValidationFailure("Toggle dropdown sync", toggleFace.toString(), toggleDropdown.getSelectedOption().toString());
            }
        } else {
            DebugConfig.logCritical("BlockEntity not found during GUI initialization!");
        }
    }

    /**
     * Enhanced slot change handling with validation
     */
    private void onSlotChangedWithAutoApply() {
        ItemStack currentToggleItem = textureContainer.getItem(TOGGLE_TEXTURE_SLOT);
        ItemStack currentBaseItem = textureContainer.getItem(BASE_TEXTURE_SLOT);

        // Only trigger update if items actually changed
        boolean toggleChanged = !ItemStack.matches(lastToggleItem, currentToggleItem);
        boolean baseChanged = !ItemStack.matches(lastBaseItem, currentBaseItem);

        if (toggleChanged || baseChanged) {
            // Update tracking
            lastToggleItem = currentToggleItem.copy();
            lastBaseItem = currentBaseItem.copy();

            SwitchesLeverBlockEntity blockEntity = getBlockEntity();
            if (blockEntity != null) {
                DebugConfig.logUserAction("GUI slot change - applying to BlockEntity");
                blockEntity.setGuiSlotItems(currentToggleItem, currentBaseItem);
            } else {
                DebugConfig.logCritical("BlockEntity missing during slot change!");
            }
        }
    }

    // ========================================
    // ENHANCED FACE SELECTION WITH VALIDATION
    // ========================================

    /**
     * FIXED: Enhanced base face selection with validation
     */
    public void setBaseFaceSelection(@Nonnull FaceSelectionData.FaceOption faceOption) {
        SwitchesLeverBlockEntity blockEntity = getBlockEntity();
        if (blockEntity != null) {
            FaceSelectionData.FaceOption oldSelection = blockEntity.getBaseFaceSelection();

            DebugConfig.logUserAction("GUI: Base face " + oldSelection + " → " + faceOption);

            if (blockEntity.setBaseFaceSelection(faceOption)) {
                // Verify the change took effect
                FaceSelectionData.FaceOption newSelection = blockEntity.getBaseFaceSelection();
                if (newSelection != faceOption) {
                    DebugConfig.logValidationFailure("Base face set", faceOption.toString(), newSelection.toString());
                }
            }
        } else {
            DebugConfig.logCritical("BlockEntity missing during base face selection!");
        }
    }

    /**
     * FIXED: Enhanced toggle face selection with validation
     */
    public void setToggleFaceSelection(@Nonnull FaceSelectionData.FaceOption faceOption) {
        SwitchesLeverBlockEntity blockEntity = getBlockEntity();
        if (blockEntity != null) {
            FaceSelectionData.FaceOption oldSelection = blockEntity.getToggleFaceSelection();

            DebugConfig.logUserAction("GUI: Toggle face " + oldSelection + " → " + faceOption);

            if (blockEntity.setToggleFaceSelection(faceOption)) {
                // Verify the change took effect
                FaceSelectionData.FaceOption newSelection = blockEntity.getToggleFaceSelection();
                if (newSelection != faceOption) {
                    DebugConfig.logValidationFailure("Toggle face set", faceOption.toString(), newSelection.toString());
                }
            }
        } else {
            DebugConfig.logCritical("BlockEntity missing during toggle face selection!");
        }
    }

    /**
     * Enhanced inversion setting with validation
     */
    public void setInverted(boolean inverted) {
        SwitchesLeverBlockEntity blockEntity = getBlockEntity();
        if (blockEntity != null) {
            boolean oldInverted = blockEntity.isInverted();

            DebugConfig.logUserAction("GUI: Inverted " + oldInverted + " → " + inverted);

            if (blockEntity.setInverted(inverted)) {
                // Verify the change took effect
                boolean newInverted = blockEntity.isInverted();
                if (newInverted != inverted) {
                    DebugConfig.logValidationFailure("Inversion set", Boolean.toString(inverted), Boolean.toString(newInverted));
                }
            }
        } else {
            DebugConfig.logCritical("BlockEntity missing during inversion setting!");
        }
    }

    // ========================================
    // ENHANCED GETTERS WITH NULL SAFETY
    // ========================================

    @Nonnull
    public FaceSelectionData.FaceOption getBaseFaceSelection() {
        SwitchesLeverBlockEntity blockEntity = getBlockEntity();
        if (blockEntity != null) {
            FaceSelectionData.FaceOption selection = blockEntity.getBaseFaceSelection();
            return selection != null ? selection : FaceSelectionData.FaceOption.ALL;
        }
        DebugConfig.logCritical("BlockEntity missing during base face get!");
        return FaceSelectionData.FaceOption.ALL;
    }

    @Nonnull
    public FaceSelectionData.FaceOption getToggleFaceSelection() {
        SwitchesLeverBlockEntity blockEntity = getBlockEntity();
        if (blockEntity != null) {
            FaceSelectionData.FaceOption selection = blockEntity.getToggleFaceSelection();
            return selection != null ? selection : FaceSelectionData.FaceOption.ALL;
        }
        DebugConfig.logCritical("BlockEntity missing during toggle face get!");
        return FaceSelectionData.FaceOption.ALL;
    }

    public boolean isInverted() {
        SwitchesLeverBlockEntity blockEntity = getBlockEntity();
        if (blockEntity != null) {
            return blockEntity.isInverted();
        }
        DebugConfig.logCritical("BlockEntity missing during inversion get!");
        return false;
    }

    @Nonnull
    public FaceSelectionData.DropdownState getBaseDropdownState() {
        SwitchesLeverBlockEntity blockEntity = getBlockEntity();
        if (blockEntity != null) {
            return blockEntity.getBaseDropdownState();
        }
        DebugConfig.logCritical("BlockEntity missing during base dropdown get!");
        return FaceSelectionData.createDisabledState();
    }

    @Nonnull
    public FaceSelectionData.DropdownState getToggleDropdownState() {
        SwitchesLeverBlockEntity blockEntity = getBlockEntity();
        if (blockEntity != null) {
            return blockEntity.getToggleDropdownState();
        }
        DebugConfig.logCritical("BlockEntity missing during toggle dropdown get!");
        return FaceSelectionData.createDisabledState();
    }

    /**
     * Enhanced BlockEntity getter with validation
     */
    @Nullable
    private SwitchesLeverBlockEntity getBlockEntity() {
        if (blockPos == null) {
            DebugConfig.logCritical("Block position is null!");
            return null;
        }

        if (level == null) {
            DebugConfig.logCritical("Level is null!");
            return null;
        }

        var blockEntity = level.getBlockEntity(blockPos);
        if (blockEntity instanceof SwitchesLeverBlockEntity switchBlockEntity) {
            return switchBlockEntity;
        }

        if (blockEntity == null) {
            DebugConfig.logCritical("No BlockEntity found at " + blockPos);
        } else {
            DebugConfig.logCritical("Wrong BlockEntity type at " + blockPos + ": " + blockEntity.getClass().getSimpleName());
        }

        return null;
    }

    // ========================================
    // ENHANCED CLEANUP
    // ========================================

    @Override
    public void removed(@Nonnull Player player) {
        super.removed(player);

        // Final state validation on close
        SwitchesLeverBlockEntity blockEntity = getBlockEntity();
        if (blockEntity != null) {
            DebugConfig.logPersistence("GUI closed - Final state: Base=" + blockEntity.getBaseFaceSelection() +
                    ", Toggle=" + blockEntity.getToggleFaceSelection());
        }
    }

    // ========================================
    // STANDARD CONTAINER METHODS
    // ========================================

    @Override
    @Nonnull
    public ItemStack quickMoveStack(@Nonnull Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = slots.get(index);

        if (slot.hasItem()) {
            ItemStack currentStack = slot.getItem();
            itemStack = currentStack.copy();

            if (index < TEXTURE_SLOT_COUNT) {
                if (!moveItemStackTo(currentStack, TEXTURE_SLOT_COUNT, slots.size(), false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!moveItemStackTo(currentStack, 0, TEXTURE_SLOT_COUNT, false)) {
                    int playerInventoryStart = TEXTURE_SLOT_COUNT;
                    int playerInventoryEnd = playerInventoryStart + 27;
                    int hotbarEnd = playerInventoryEnd + 9;

                    if (index < playerInventoryEnd) {
                        if (!moveItemStackTo(currentStack, playerInventoryEnd, hotbarEnd, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else {
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

        if (!(level.getBlockState(blockPos).getBlock() instanceof net.justsomeswitches.block.SwitchesLeverBlock)) {
            return false;
        }

        return player.distanceToSqr(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5) <= 64.0;
    }
}