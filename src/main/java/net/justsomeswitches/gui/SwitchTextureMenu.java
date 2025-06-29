package net.justsomeswitches.gui;

import net.justsomeswitches.blockentity.SwitchesLeverBlockEntity;
import net.justsomeswitches.util.BlockTextureAnalyzer;
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
 * Enhanced server-side menu for the Switch Texture customization GUI
 * ---
 * Phase 4B: Advanced face selection and block analysis integration
 * Supports dynamic dropdown states and texture preview functionality
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

    // Phase 4B: Face selection state tracking
    private FaceSelectionData.FaceOption baseFaceSelection = FaceSelectionData.FaceOption.ALL;
    private FaceSelectionData.FaceOption toggleFaceSelection = FaceSelectionData.FaceOption.ALL;
    private boolean inverted = false;

    /**
     * Constructor for the Switch Texture Menu with enhanced BlockEntity integration
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

        System.out.println("Phase 4B Debug: Enhanced SwitchTextureMenu constructor - BlockPos: " + blockPos);

        // Try to get the BlockEntity and load GUI slot data
        loadGuiSlotData();

        // Add enhanced texture slots positioned to match design image (176px width)
        addSlot(new EnhancedTextureSlot(textureContainer, TOGGLE_TEXTURE_SLOT, TOGGLE_SLOT_X, TOGGLE_SLOT_Y) {
            @Override
            public void setChanged() {
                super.setChanged();
                onSlotChanged();
            }
        });
        addSlot(new EnhancedTextureSlot(textureContainer, BASE_TEXTURE_SLOT, BASE_SLOT_X, BASE_SLOT_Y) {
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

        System.out.println("Phase 4B Debug: Enhanced menu initialized with face selection support");
    }

    /**
     * Enhanced texture slot that triggers block analysis
     */
    private class EnhancedTextureSlot extends TextureSlot {
        public EnhancedTextureSlot(@Nonnull net.minecraft.world.Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public void set(@Nonnull ItemStack stack) {
            super.set(stack);
            // Trigger block analysis when item is placed
            triggerBlockAnalysis();
        }

        @Override
        @Nonnull
        public ItemStack remove(int amount) {
            ItemStack result = super.remove(amount);
            // Trigger block analysis when item is removed
            triggerBlockAnalysis();
            return result;
        }
    }

    /**
     * Network constructor for client-side menu creation
     * Note: This constructor is used by NeoForge's network system - warning is false positive
     */
    public SwitchTextureMenu(int containerId, @Nonnull Inventory playerInventory, @Nonnull FriendlyByteBuf extraData) {
        this(containerId, playerInventory, extraData.readBlockPos());
    }

    /**
     * Enhanced auto-apply functionality - triggered when slots change
     */
    private void onSlotChanged() {
        System.out.println("Phase 4B Debug: Slot changed - triggering enhanced auto-apply");
        triggerBlockAnalysis();
        applyTextures();
    }

    /**
     * Trigger block analysis for dropdown state updates
     */
    private void triggerBlockAnalysis() {
        if (blockEntity != null) {
            // Update stored items for analysis
            ItemStack toggleItem = textureContainer.getItem(TOGGLE_TEXTURE_SLOT);
            ItemStack baseItem = textureContainer.getItem(BASE_TEXTURE_SLOT);
            blockEntity.setGuiSlotItems(toggleItem, baseItem);

            System.out.println("Phase 4B Debug: Block analysis triggered for slot changes");
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
            System.out.println("Phase 4B Debug: Enhanced menu initialized with BlockEntity at " + blockPos);

            // Load GUI slot contents from BlockEntity
            ItemStack toggleItem = blockEntity.getGuiToggleItem();
            ItemStack baseItem = blockEntity.getGuiBaseItem();

            textureContainer.setItem(TOGGLE_TEXTURE_SLOT, toggleItem);
            textureContainer.setItem(BASE_TEXTURE_SLOT, baseItem);

            // Load face selections and inversion state
            this.baseFaceSelection = blockEntity.getBaseFaceSelection();
            this.toggleFaceSelection = blockEntity.getToggleFaceSelection();
            this.inverted = blockEntity.isInverted();

            System.out.println("Phase 4B Debug: Loaded enhanced GUI state - Toggle: " + toggleItem +
                    ", Base: " + baseItem + ", BaseFace: " + baseFaceSelection.getDisplayName() +
                    ", ToggleFace: " + toggleFaceSelection.getDisplayName() + ", Inverted: " + inverted);
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

            System.out.println("Phase 4B Debug: Saved enhanced GUI slots");
        }
    }

    /**
     * Checks if there's a valid BlockEntity for this menu
     */
    public boolean hasValidBlockEntity() {
        return blockEntity != null;
    }

    // ========================================
    // PHASE 4B: FACE SELECTION METHODS
    // ========================================

    /**
     * Set face selection for base texture slot
     */
    public void setBaseFaceSelection(@Nonnull FaceSelectionData.FaceOption faceOption) {
        if (blockEntity != null && this.baseFaceSelection != faceOption) {
            this.baseFaceSelection = faceOption;
            blockEntity.setBaseFaceSelection(faceOption);
            forceBlockUpdate();
            System.out.println("Phase 4B Debug: Base face selection set to: " + faceOption.getDisplayName());
        }
    }

    /**
     * Set face selection for toggle texture slot
     */
    public void setToggleFaceSelection(@Nonnull FaceSelectionData.FaceOption faceOption) {
        if (blockEntity != null && this.toggleFaceSelection != faceOption) {
            this.toggleFaceSelection = faceOption;
            blockEntity.setToggleFaceSelection(faceOption);
            forceBlockUpdate();
            System.out.println("Phase 4B Debug: Toggle face selection set to: " + faceOption.getDisplayName());
        }
    }

    /**
     * Set inversion state
     */
    public void setInverted(boolean inverted) {
        if (blockEntity != null && this.inverted != inverted) {
            this.inverted = inverted;
            blockEntity.setInverted(inverted);
            forceBlockUpdate();
            System.out.println("Phase 4B Debug: Inversion state set to: " + inverted);
        }
    }

    /**
     * Get dropdown state for base texture slot
     */
    @Nonnull
    public FaceSelectionData.DropdownState getBaseDropdownState() {
        return blockEntity != null ? blockEntity.getBaseDropdownState() :
                FaceSelectionData.createDisabledState();
    }

    /**
     * Get dropdown state for toggle texture slot
     */
    @Nonnull
    public FaceSelectionData.DropdownState getToggleDropdownState() {
        return blockEntity != null ? blockEntity.getToggleDropdownState() :
                FaceSelectionData.createDisabledState();
    }

    /**
     * Get current face selections
     */
    @Nonnull public FaceSelectionData.FaceOption getBaseFaceSelection() { return baseFaceSelection; }
    @Nonnull public FaceSelectionData.FaceOption getToggleFaceSelection() { return toggleFaceSelection; }
    public boolean isInverted() { return inverted; }

    // ========================================
    // ENHANCED TEXTURE APPLICATION
    // ========================================

    /**
     * Enhanced texture application with face selection support
     */
    public void applyTextures() {
        System.out.println("Phase 4B Debug: Enhanced applyTextures called");

        if (blockEntity != null) {
            // Get items from the GUI slots
            ItemStack toggleItem = textureContainer.getItem(TOGGLE_TEXTURE_SLOT);
            ItemStack baseItem = textureContainer.getItem(BASE_TEXTURE_SLOT);

            System.out.println("Phase 4B Debug: Enhanced applyTextures - Toggle item: " + toggleItem);
            System.out.println("Phase 4B Debug: Enhanced applyTextures - Base item: " + baseItem);

            // Store GUI slot items for persistence and analysis
            blockEntity.setGuiSlotItems(toggleItem, baseItem);

            // Apply or reset toggle texture based on slot content
            if (!toggleItem.isEmpty()) {
                boolean success = blockEntity.setToggleTexture(toggleItem);
                System.out.println("Phase 4B Debug: Enhanced applyTextures - Toggle texture set success: " + success);
            } else {
                System.out.println("Phase 4B Debug: Enhanced applyTextures - Resetting toggle texture to default");
                blockEntity.resetToggleTexture();
                setToggleFaceSelection(FaceSelectionData.FaceOption.ALL);
            }

            // Apply or reset base texture based on slot content
            if (!baseItem.isEmpty()) {
                boolean success = blockEntity.setBaseTexture(baseItem);
                System.out.println("Phase 4B Debug: Enhanced applyTextures - Base texture set success: " + success);
            } else {
                System.out.println("Phase 4B Debug: Enhanced applyTextures - Resetting base texture to default");
                blockEntity.resetBaseTexture();
                setBaseFaceSelection(FaceSelectionData.FaceOption.ALL);
            }

            // Force visual update with enhanced data
            forceBlockUpdate();

            System.out.println("Phase 4B Debug: Enhanced applyTextures complete - " +
                    "Base: " + blockEntity.getBaseTexture() + " (" + baseFaceSelection.getDisplayName() + "), " +
                    "Toggle: " + blockEntity.getToggleTexture() + " (" + toggleFaceSelection.getDisplayName() + "), " +
                    "Inverted: " + inverted);
        } else {
            System.out.println("Phase 4B Debug: Enhanced applyTextures - blockEntity is null!");
        }
    }

    /**
     * Enhanced block update with face selection and inversion data
     */
    private void forceBlockUpdate() {
        if (blockEntity != null && level != null && blockPos != null) {
            // Mark BlockEntity as changed for NBT saving
            blockEntity.setChanged();

            // Force model data update for enhanced customization
            blockEntity.requestModelDataUpdate();

            // Send block update to clients for immediate visual refresh
            level.sendBlockUpdated(blockPos, level.getBlockState(blockPos), level.getBlockState(blockPos), 3);

            System.out.println("Phase 4B Debug: Enhanced block update with face selections and inversion");
        }
    }

    // ========================================
    // BLOCK ANALYSIS METHODS FOR GUI
    // ========================================

    /**
     * Get block analysis for current toggle item
     */
    @Nonnull
    public BlockTextureAnalyzer.BlockTextureInfo getToggleBlockAnalysis() {
        ItemStack toggleItem = textureContainer.getItem(TOGGLE_TEXTURE_SLOT);
        return BlockTextureAnalyzer.analyzeBlock(toggleItem);
    }

    /**
     * Get block analysis for current base item
     */
    @Nonnull
    public BlockTextureAnalyzer.BlockTextureInfo getBaseBlockAnalysis() {
        ItemStack baseItem = textureContainer.getItem(BASE_TEXTURE_SLOT);
        return BlockTextureAnalyzer.analyzeBlock(baseItem);
    }

    @Override
    public void removed(@Nonnull Player player) {
        super.removed(player);

        // Save GUI slot data when menu is closed
        saveGuiSlotData();

        // Apply textures one final time when closing GUI
        System.out.println("Phase 4B Debug: Enhanced menu closing - applying final textures");
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

        // Enhanced auto-apply with face selection support
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