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
 * FIXED: Stateless Menu with reduced setChanged() calls and proper auto-apply
 * ---
 * ARCHITECTURE: Menu acts as view/controller only, BlockEntity holds ALL state
 * FIXES: Eliminated excessive setChanged() calls causing race conditions
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

    // FIXED: Only essential references - NO STATE STORAGE
    private final SimpleContainer textureContainer;
    private final BlockPos blockPos;
    private final Level level;

    // FIXED: Track last slot state to prevent unnecessary updates
    private ItemStack lastToggleItem = ItemStack.EMPTY;
    private ItemStack lastBaseItem = ItemStack.EMPTY;

    // REMOVED: All local face selection state variables
    // BlockEntity is the single source of truth

    /**
     * FIXED: Constructor with reduced change notification system
     */
    public SwitchTextureMenu(int containerId, @Nonnull Inventory playerInventory, @Nullable BlockPos blockPos) {
        super(JustSomeSwitchesMenuTypes.SWITCH_TEXTURE_MENU.get(), containerId);

        this.blockPos = blockPos;
        this.level = playerInventory.player.level();

        System.out.println("DEBUG Menu: FIXED - Stateless Menu Created");

        // FIXED: Container with smart change detection
        this.textureContainer = new SimpleContainer(TEXTURE_SLOT_COUNT) {
            @Override
            public void setChanged() {
                super.setChanged();
                onSlotChangedSmart();
            }
        };

        // Load initial slot items from BlockEntity
        loadSlotItemsFromBlockEntity();

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

        System.out.println("DEBUG Menu: FIXED - Stateless menu initialization complete");
    }

    /**
     * Network constructor
     */
    public SwitchTextureMenu(int containerId, @Nonnull Inventory playerInventory, @Nonnull FriendlyByteBuf extraData) {
        this(containerId, playerInventory, extraData.readBlockPos());
    }

    /**
     * FIXED: Smart slot change handling that only updates when items actually change
     */
    private void onSlotChangedSmart() {
        ItemStack currentToggleItem = textureContainer.getItem(TOGGLE_TEXTURE_SLOT);
        ItemStack currentBaseItem = textureContainer.getItem(BASE_TEXTURE_SLOT);

        // FIXED: Only trigger update if items actually changed
        boolean toggleChanged = !ItemStack.matches(lastToggleItem, currentToggleItem);
        boolean baseChanged = !ItemStack.matches(lastBaseItem, currentBaseItem);

        if (toggleChanged || baseChanged) {
            System.out.println("DEBUG Menu: FIXED - Smart change detected - Toggle changed: " + toggleChanged +
                    ", Base changed: " + baseChanged);

            // Update last known state
            lastToggleItem = currentToggleItem.copy();
            lastBaseItem = currentBaseItem.copy();

            SwitchesLeverBlockEntity blockEntity = getBlockEntity();
            if (blockEntity != null) {
                // FIXED: Single update call instead of multiple
                blockEntity.setGuiSlotItems(currentToggleItem, currentBaseItem);
                blockEntity.applyCurrentTextureSettings();

                System.out.println("DEBUG Menu: FIXED - Single slot update applied to BlockEntity");
            }
        }
    }

    /**
     * FIXED: Load slot items from BlockEntity with state tracking
     */
    private void loadSlotItemsFromBlockEntity() {
        SwitchesLeverBlockEntity blockEntity = getBlockEntity();
        if (blockEntity != null) {
            ItemStack toggleItem = blockEntity.getGuiToggleItem();
            ItemStack baseItem = blockEntity.getGuiBaseItem();

            textureContainer.setItem(TOGGLE_TEXTURE_SLOT, toggleItem);
            textureContainer.setItem(BASE_TEXTURE_SLOT, baseItem);

            // FIXED: Track initial state to prevent false change detection
            lastToggleItem = toggleItem.copy();
            lastBaseItem = baseItem.copy();

            System.out.println("DEBUG Menu: FIXED - Loaded slot items from BlockEntity - Toggle: " +
                    !toggleItem.isEmpty() + ", Base: " + !baseItem.isEmpty());
        }
    }

    // ========================================
    // FIXED: Stateless Face Selection Methods with immediate application
    // ========================================

    /**
     * FIXED: Direct BlockEntity face selection setter with immediate texture update
     */
    public void setBaseFaceSelection(@Nonnull FaceSelectionData.FaceOption faceOption) {
        SwitchesLeverBlockEntity blockEntity = getBlockEntity();
        if (blockEntity != null) {
            System.out.println("DEBUG Menu: FIXED - Setting base face selection to " + faceOption);
            blockEntity.setBaseFaceSelection(faceOption);
            // Face selection change will trigger applyCurrentTextureSettings automatically
        }
    }

    /**
     * FIXED: Direct BlockEntity face selection setter with immediate texture update
     */
    public void setToggleFaceSelection(@Nonnull FaceSelectionData.FaceOption faceOption) {
        SwitchesLeverBlockEntity blockEntity = getBlockEntity();
        if (blockEntity != null) {
            System.out.println("DEBUG Menu: FIXED - Setting toggle face selection to " + faceOption);
            blockEntity.setToggleFaceSelection(faceOption);
            // Face selection change will trigger applyCurrentTextureSettings automatically
        }
    }

    /**
     * FIXED: Direct BlockEntity inversion setter with immediate texture update
     */
    public void setInverted(boolean inverted) {
        SwitchesLeverBlockEntity blockEntity = getBlockEntity();
        if (blockEntity != null) {
            System.out.println("DEBUG Menu: FIXED - Setting inverted to " + inverted);
            blockEntity.setInverted(inverted);
            // Inversion change will trigger applyCurrentTextureSettings automatically
        }
    }

    // ========================================
    // FIXED: Stateless Getters (Read from BlockEntity)
    // ========================================

    @Nonnull
    public FaceSelectionData.FaceOption getBaseFaceSelection() {
        SwitchesLeverBlockEntity blockEntity = getBlockEntity();
        return blockEntity != null ? blockEntity.getBaseFaceSelection() : FaceSelectionData.FaceOption.ALL;
    }

    @Nonnull
    public FaceSelectionData.FaceOption getToggleFaceSelection() {
        SwitchesLeverBlockEntity blockEntity = getBlockEntity();
        return blockEntity != null ? blockEntity.getToggleFaceSelection() : FaceSelectionData.FaceOption.ALL;
    }

    public boolean isInverted() {
        SwitchesLeverBlockEntity blockEntity = getBlockEntity();
        return blockEntity != null && blockEntity.isInverted();
    }

    @Nonnull
    public FaceSelectionData.DropdownState getBaseDropdownState() {
        SwitchesLeverBlockEntity blockEntity = getBlockEntity();
        return blockEntity != null ? blockEntity.getBaseDropdownState() : FaceSelectionData.createDisabledState();
    }

    @Nonnull
    public FaceSelectionData.DropdownState getToggleDropdownState() {
        SwitchesLeverBlockEntity blockEntity = getBlockEntity();
        return blockEntity != null ? blockEntity.getToggleDropdownState() : FaceSelectionData.createDisabledState();
    }

    /**
     * Get BlockEntity reference
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

    // ========================================
    // FIXED: Simplified Cleanup
    // ========================================

    @Override
    public void removed(@Nonnull Player player) {
        super.removed(player);
        // FIXED: No state to save - BlockEntity already has current state
        System.out.println("DEBUG Menu: FIXED - Stateless menu closed, no cleanup needed");
    }

    // ========================================
    // Standard Container Methods
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