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
 * REDESIGNED: Stateless Menu - Pure BlockEntity State Management
 * ---
 * ARCHITECTURE: Menu acts as view/controller only, BlockEntity holds ALL state
 * ELIMINATES: State duplication, preservation logic, synchronization issues
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

    // REDESIGNED: Only essential references - NO STATE STORAGE
    private final SimpleContainer textureContainer;
    private final BlockPos blockPos;
    private final Level level;

    // REMOVED: All local face selection state variables
    // BlockEntity is the single source of truth

    /**
     * REDESIGNED: Constructor with pure BlockEntity dependency
     */
    public SwitchTextureMenu(int containerId, @Nonnull Inventory playerInventory, @Nullable BlockPos blockPos) {
        super(JustSomeSwitchesMenuTypes.SWITCH_TEXTURE_MENU.get(), containerId);

        this.blockPos = blockPos;
        this.level = playerInventory.player.level();

        System.out.println("DEBUG Menu: REDESIGNED - Stateless Menu Created");

        // REDESIGNED: Auto-apply container with immediate BlockEntity updates
        this.textureContainer = new SimpleContainer(TEXTURE_SLOT_COUNT) {
            @Override
            public void setChanged() {
                super.setChanged();
                onSlotChangedImmediate();
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

        System.out.println("DEBUG Menu: REDESIGNED - Stateless menu initialization complete");
    }

    /**
     * Network constructor
     */
    public SwitchTextureMenu(int containerId, @Nonnull Inventory playerInventory, @Nonnull FriendlyByteBuf extraData) {
        this(containerId, playerInventory, extraData.readBlockPos());
    }

    /**
     * REDESIGNED: Immediate slot change handling with direct BlockEntity updates
     */
    private void onSlotChangedImmediate() {
        SwitchesLeverBlockEntity blockEntity = getBlockEntity();
        if (blockEntity != null) {
            ItemStack toggleItem = textureContainer.getItem(TOGGLE_TEXTURE_SLOT);
            ItemStack baseItem = textureContainer.getItem(BASE_TEXTURE_SLOT);

            // REDESIGNED: Immediate update to BlockEntity (single source of truth)
            blockEntity.setGuiSlotItems(toggleItem, baseItem);
            blockEntity.applyCurrentTextureSettings();

            System.out.println("DEBUG Menu: REDESIGNED - Immediate slot update applied to BlockEntity");
        }
    }

    /**
     * Load slot items from BlockEntity (stateless approach)
     */
    private void loadSlotItemsFromBlockEntity() {
        SwitchesLeverBlockEntity blockEntity = getBlockEntity();
        if (blockEntity != null) {
            textureContainer.setItem(TOGGLE_TEXTURE_SLOT, blockEntity.getGuiToggleItem());
            textureContainer.setItem(BASE_TEXTURE_SLOT, blockEntity.getGuiBaseItem());
            System.out.println("DEBUG Menu: REDESIGNED - Loaded slot items from BlockEntity");
        }
    }

    // ========================================
    // REDESIGNED: Stateless Face Selection Methods
    // ========================================

    /**
     * REDESIGNED: Direct BlockEntity face selection setter
     */
    public void setBaseFaceSelection(@Nonnull FaceSelectionData.FaceOption faceOption) {
        SwitchesLeverBlockEntity blockEntity = getBlockEntity();
        if (blockEntity != null) {
            System.out.println("DEBUG Menu: REDESIGNED - Setting base face selection to " + faceOption);
            blockEntity.setBaseFaceSelection(faceOption);
            blockEntity.applyCurrentTextureSettings();
        }
    }

    /**
     * REDESIGNED: Direct BlockEntity face selection setter
     */
    public void setToggleFaceSelection(@Nonnull FaceSelectionData.FaceOption faceOption) {
        SwitchesLeverBlockEntity blockEntity = getBlockEntity();
        if (blockEntity != null) {
            System.out.println("DEBUG Menu: REDESIGNED - Setting toggle face selection to " + faceOption);
            blockEntity.setToggleFaceSelection(faceOption);
            blockEntity.applyCurrentTextureSettings();
        }
    }

    /**
     * REDESIGNED: Direct BlockEntity inversion setter
     */
    public void setInverted(boolean inverted) {
        SwitchesLeverBlockEntity blockEntity = getBlockEntity();
        if (blockEntity != null) {
            System.out.println("DEBUG Menu: REDESIGNED - Setting inverted to " + inverted);
            blockEntity.setInverted(inverted);
            blockEntity.applyCurrentTextureSettings();
        }
    }

    // ========================================
    // REDESIGNED: Stateless Getters (Read from BlockEntity)
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
    // REDESIGNED: Simplified Cleanup
    // ========================================

    @Override
    public void removed(@Nonnull Player player) {
        super.removed(player);
        // REDESIGNED: No state to save - BlockEntity already has current state
        System.out.println("DEBUG Menu: REDESIGNED - Stateless menu closed, no cleanup needed");
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