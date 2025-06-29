package net.justsomeswitches.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.justsomeswitches.blockentity.SwitchesLeverBlockEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Server-side menu for the Switch Texture customization GUI
 * ---
 * Phase 4A: Layout Matching User Design Image
 * Corrected slot positions to match uploaded image exactly - 176px width x 176px height
 */
public class SwitchTextureMenu extends AbstractContainerMenu {

    // GUI Layout Constants
    private static final int TEXTURE_SLOT_COUNT = 2;
    private static final int TOGGLE_TEXTURE_SLOT = 0;
    private static final int BASE_TEXTURE_SLOT = 1;

    // Corrected positioning to match screen coordinates exactly
    private static final int TOGGLE_SLOT_X = 28;    // Left texture slot - matches screen
    private static final int TOGGLE_SLOT_Y = 28;    // Matches screen
    private static final int BASE_SLOT_X = 132;     // Right texture slot - matches screen
    private static final int BASE_SLOT_Y = 28;      // Matches screen

    // Player inventory positioning (standard for 176px width)
    private static final int PLAYER_INV_Y = 98;
    private static final int HOTBAR_Y = 156;

    // Instance data
    private final SimpleContainer textureContainer;
    private final BlockPos blockPos;
    private final Level level;
    private SwitchesLeverBlockEntity blockEntity;

    /**
     * Constructor for the Switch Texture Menu with BlockEntity integration
     */
    public SwitchTextureMenu(int containerId, @Nonnull Inventory playerInventory, @Nullable BlockPos blockPos) {
        super(JustSomeSwitchesMenuTypes.SWITCH_TEXTURE_MENU.get(), containerId);

        this.blockPos = blockPos;
        this.level = playerInventory.player.level();
        this.textureContainer = new SimpleContainer(TEXTURE_SLOT_COUNT) {
            @Override
            public void setChanged() {
                super.setChanged();
                // Trigger auto-apply when container changes
                onSlotChanged();
            }
        };

        System.out.println("Phase 4A Debug: SwitchTextureMenu constructor - BlockPos: " + blockPos);

        // Try to get the BlockEntity and load GUI slot data
        loadGuiSlotData();

        // Add texture slots positioned to match design image (176px width)
        addSlot(new TextureSlot(textureContainer, TOGGLE_TEXTURE_SLOT, TOGGLE_SLOT_X, TOGGLE_SLOT_Y) {
            @Override
            public void setChanged() {
                super.setChanged();
                onSlotChanged();
            }
        });
        addSlot(new TextureSlot(textureContainer, BASE_TEXTURE_SLOT, BASE_SLOT_X, BASE_SLOT_Y) {
            @Override
            public void setChanged() {
                super.setChanged();
                onSlotChanged();
            }
        });

        // Add player inventory slots (standard positioning)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, PLAYER_INV_Y + row * 18));
            }
        }

        // Add player hotbar slots (standard positioning)
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, HOTBAR_Y));
        }

        System.out.println("Phase 4A Debug: Menu initialized with pixel-perfect layout - Toggle(" +
                TOGGLE_SLOT_X + "," + TOGGLE_SLOT_Y + "), Base(" + BASE_SLOT_X + "," + BASE_SLOT_Y + ")");
    }

    /**
     * Network constructor for client-side menu creation
     * Note: This constructor is used by NeoForge's network system - warning is false positive
     */
    public SwitchTextureMenu(int containerId, @Nonnull Inventory playerInventory, @Nonnull FriendlyByteBuf extraData) {
        this(containerId, playerInventory, extraData.readBlockPos());
    }

    /**
     * Auto-apply functionality - triggered when slots change
     */
    private void onSlotChanged() {
        System.out.println("Phase 4A Debug: Slot changed - auto-applying textures");
        applyTextures();
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
            System.out.println("Phase 4A Debug: Menu initialized with BlockEntity at " + blockPos);

            // Load GUI slot contents from BlockEntity
            ItemStack toggleItem = blockEntity.getGuiToggleItem();
            ItemStack baseItem = blockEntity.getGuiBaseItem();

            textureContainer.setItem(TOGGLE_TEXTURE_SLOT, toggleItem);
            textureContainer.setItem(BASE_TEXTURE_SLOT, baseItem);

            System.out.println("Phase 4A Debug: Loaded GUI slots - Toggle: " + toggleItem + ", Base: " + baseItem);
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

            System.out.println("Phase 4A Debug: Saved GUI slots - Toggle: " + toggleItem + ", Base: " + baseItem);
        }
    }

    /**
     * Checks if there's a valid BlockEntity for this menu
     */
    public boolean hasValidBlockEntity() {
        return blockEntity != null;
    }

    /**
     * Applies textures and forces visual update
     */
    public void applyTextures() {
        System.out.println("Phase 4A Debug: applyTextures called");

        if (blockEntity != null) {
            // Get items from the GUI slots
            ItemStack toggleItem = textureContainer.getItem(TOGGLE_TEXTURE_SLOT);
            ItemStack baseItem = textureContainer.getItem(BASE_TEXTURE_SLOT);

            System.out.println("Phase 4A Debug: applyTextures - Toggle item: " + toggleItem);
            System.out.println("Phase 4A Debug: applyTextures - Base item: " + baseItem);

            // Store GUI slot items for persistence
            blockEntity.setGuiSlotItems(toggleItem, baseItem);

            // Apply or reset toggle texture based on slot content
            if (!toggleItem.isEmpty()) {
                boolean success = blockEntity.setToggleTexture(toggleItem);
                System.out.println("Phase 4A Debug: applyTextures - Toggle texture set success: " + success);
            } else {
                System.out.println("Phase 4A Debug: applyTextures - Resetting toggle texture to default");
                blockEntity.resetToggleTexture();
            }

            // Apply or reset base texture based on slot content
            if (!baseItem.isEmpty()) {
                boolean success = blockEntity.setBaseTexture(baseItem);
                System.out.println("Phase 4A Debug: applyTextures - Base texture set success: " + success);
            } else {
                System.out.println("Phase 4A Debug: applyTextures - Resetting base texture to default");
                blockEntity.resetBaseTexture();
            }

            // Force visual update
            forceBlockUpdate();

            System.out.println("Phase 4A Debug: applyTextures - Final textures - Base: " + blockEntity.getBaseTexture() + ", Toggle: " + blockEntity.getToggleTexture());
        } else {
            System.out.println("Phase 4A Debug: applyTextures - blockEntity is null!");
        }
    }

    /**
     * Force block update to refresh visual appearance
     */
    private void forceBlockUpdate() {
        if (blockEntity != null && level != null && blockPos != null) {
            // Mark BlockEntity as changed for NBT saving
            blockEntity.setChanged();

            // Force model data update
            blockEntity.requestModelDataUpdate();

            // Send block update to clients for visual refresh
            level.sendBlockUpdated(blockPos, level.getBlockState(blockPos), level.getBlockState(blockPos), 3);

            System.out.println("Phase 4A Debug: Forced block update at " + blockPos);
        }
    }

    @Override
    public void removed(@Nonnull Player player) {
        super.removed(player);

        // Save GUI slot data when menu is closed
        saveGuiSlotData();

        // Apply textures one final time when closing GUI
        System.out.println("Phase 4A Debug: Menu closing - applying final textures");
        applyTextures();
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
                    // If that fails, try moving within player inventory
                    int playerInventoryStart = TEXTURE_SLOT_COUNT;
                    int playerInventoryEnd = playerInventoryStart + 27; // 3x9 grid
                    int hotbarEnd = playerInventoryEnd + 9; // 1x9 hotbar

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

        // Auto-apply textures when items are moved
        applyTextures();

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
}