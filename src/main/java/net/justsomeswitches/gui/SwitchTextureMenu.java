package net.justsomeswitches.gui;

import net.justsomeswitches.blockentity.SwitchesLeverBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nonnull;

/**
 * Server-side menu for Switch Texture customization GUI
 * ---
 * Phase 3B Enhancement: Connected to BlockEntity with complete functionality
 */
public class SwitchTextureMenu extends AbstractContainerMenu {

    // GUI slot positions
    private static final int TOGGLE_TEXTURE_SLOT = 0;
    private static final int BASE_TEXTURE_SLOT = 1;

    // Container for texture slots
    private final TextureContainer textureContainer;

    // BlockEntity reference for Phase 3B
    private final SwitchesLeverBlockEntity blockEntity;
    private final BlockPos blockPos;
    private final Level level;

    /**
     * Constructor for client-side (from packet data)
     */
    public SwitchTextureMenu(int containerId, @Nonnull Inventory playerInventory, @Nonnull FriendlyByteBuf extraData) {
        this(containerId, playerInventory, extraData.readBlockPos());
    }

    /**
     * Constructor for server-side
     */
    public SwitchTextureMenu(int containerId, @Nonnull Inventory playerInventory, @Nonnull BlockPos blockPos) {
        super(JustSomeSwitchesMenuTypes.SWITCH_TEXTURE_MENU.get(), containerId);

        this.blockPos = blockPos;
        this.level = playerInventory.player.level();

        // Get BlockEntity
        BlockEntity entity = this.level.getBlockEntity(blockPos);
        if (entity instanceof SwitchesLeverBlockEntity switchEntity) {
            this.blockEntity = switchEntity;
        } else {
            this.blockEntity = null;
            System.err.println("Phase 3C Error: Expected SwitchesLeverBlockEntity but got: " +
                    (entity != null ? entity.getClass().getSimpleName() : "null"));
        }

        // Create texture container
        this.textureContainer = new TextureContainer();

        // Load current texture items from BlockEntity into GUI slots
        if (blockEntity != null) {
            textureContainer.setItem(TOGGLE_TEXTURE_SLOT, blockEntity.getGuiToggleItem());
            textureContainer.setItem(BASE_TEXTURE_SLOT, blockEntity.getGuiBaseItem());
            System.out.println("Phase 3C Debug: Menu initialized with BlockEntity at " + blockPos);
        }

        // Add texture slots
        addSlot(new TextureSlot(textureContainer, TOGGLE_TEXTURE_SLOT, 62, 35));
        addSlot(new TextureSlot(textureContainer, BASE_TEXTURE_SLOT, 98, 35));

        // Add player inventory slots
        addPlayerInventorySlots(playerInventory);
    }

    /**
     * Adds player inventory and hotbar slots
     */
    private void addPlayerInventorySlots(@Nonnull Inventory playerInventory) {
        // Player inventory (3 rows of 9 slots)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 94 + row * 18));
            }
        }

        // Player hotbar (1 row of 9 slots)
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 152));
        }
    }

    /**
     * Check if BlockEntity is valid
     */
    public boolean hasValidBlockEntity() {
        return blockEntity != null && !blockEntity.isRemoved();
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

            // Store GUI slot items in BlockEntity for persistence
            blockEntity.setGuiSlotItems(toggleItem, baseItem);

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

            // CRITICAL FIX: Force immediate client-side model update
            if (level.isClientSide()) {
                // Request model data update on client side
                blockEntity.requestModelDataUpdate();
                System.out.println("Phase 3C Debug: Requested client-side model data update");

                // Trigger block update to ensure visual refresh
                level.sendBlockUpdated(blockPos, blockEntity.getBlockState(), blockEntity.getBlockState(), 3);
                System.out.println("Phase 3C Debug: Triggered block update for visual refresh");
            }

            System.out.println("Phase 3C Debug: applyTextures - Final textures - Base: " + blockEntity.getBaseTexture() + ", Toggle: " + blockEntity.getToggleTexture());
        } else {
            System.out.println("Phase 3C Debug: applyTextures - blockEntity is null!");
        }
    }

    /**
     * Called when the menu is closed - apply textures automatically
     */
    @Override
    public void removed(@Nonnull Player player) {
        super.removed(player);

        // Auto-apply textures when GUI is closed
        System.out.println("Phase 3C Debug: Menu closing - auto-applying textures");
        applyTextures();
    }

    @Override
    public boolean stillValid(@Nonnull Player player) {
        if (blockEntity == null || blockEntity.isRemoved()) {
            return false;
        }
        return player.distanceToSqr(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5) <= 64.0;
    }

    @Override
    @Nonnull
    public ItemStack quickMoveStack(@Nonnull Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack slotItem = slot.getItem();
            itemStack = slotItem.copy();

            // If clicking on texture slots (0-1), move to player inventory
            if (index < 2) {
                if (!this.moveItemStackTo(slotItem, 2, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            }
            // If clicking on player inventory, try to move to texture slots
            else {
                // Try to move to texture slots if item is valid
                if (TextureSlot.isValidTextureItem(slotItem)) {
                    if (!this.moveItemStackTo(slotItem, 0, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            }

            if (slotItem.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemStack;
    }

    /**
     * Container for texture slots
     */
    private static class TextureContainer implements net.minecraft.world.Container {
        private final ItemStack[] items = new ItemStack[2];

        public TextureContainer() {
            clear();
        }

        @Override
        public int getContainerSize() {
            return 2;
        }

        @Override
        public boolean isEmpty() {
            for (ItemStack item : items) {
                if (!item.isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        @Nonnull
        public ItemStack getItem(int slot) {
            return slot >= 0 && slot < items.length ? items[slot] : ItemStack.EMPTY;
        }

        @Override
        @Nonnull
        public ItemStack removeItem(int slot, int count) {
            if (slot >= 0 && slot < items.length && !items[slot].isEmpty()) {
                ItemStack result = items[slot].split(count);
                if (items[slot].isEmpty()) {
                    items[slot] = ItemStack.EMPTY;
                }
                setChanged();
                return result;
            }
            return ItemStack.EMPTY;
        }

        @Override
        @Nonnull
        public ItemStack removeItemNoUpdate(int slot) {
            if (slot >= 0 && slot < items.length) {
                ItemStack result = items[slot];
                items[slot] = ItemStack.EMPTY;
                return result;
            }
            return ItemStack.EMPTY;
        }

        @Override
        public void setItem(int slot, @Nonnull ItemStack item) {
            if (slot >= 0 && slot < items.length) {
                items[slot] = item;
                setChanged();
            }
        }

        @Override
        public void setChanged() {
            // Container changed
        }

        @Override
        public boolean stillValid(@Nonnull Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            clear();
        }

        public void clear() {
            for (int i = 0; i < items.length; i++) {
                items[i] = ItemStack.EMPTY;
            }
        }
    }
}