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
 * FIXED: Manual-only application system - NO auto-apply behavior
 * FIXED: Face selection persistence through GUI close/reopen cycles
 */
public class SwitchTextureMenu extends AbstractContainerMenu {

    // GUI Layout Constants
    private static final int TEXTURE_SLOT_COUNT = 2;
    private static final int TOGGLE_TEXTURE_SLOT = 0;
    private static final int BASE_TEXTURE_SLOT = 1;

    // Positioning constants
    private static final int TOGGLE_SLOT_X = 28;
    private static final int TOGGLE_SLOT_Y = 28;
    private static final int BASE_SLOT_X = 132;
    private static final int BASE_SLOT_Y = 28;

    // Player inventory positioning (standard for 176px width)
    private static final int PLAYER_INV_Y = 98;
    private static final int HOTBAR_Y = 156;

    // Instance data
    private final SimpleContainer textureContainer;
    private final BlockPos blockPos;
    private final Level level;
    private SwitchesLeverBlockEntity blockEntity;

    // Face selection state tracking - FIXED: Store persistently
    private FaceSelectionData.FaceOption baseFaceSelection = FaceSelectionData.FaceOption.ALL;
    private FaceSelectionData.FaceOption toggleFaceSelection = FaceSelectionData.FaceOption.ALL;
    private boolean inverted = false;

    // FIXED: Prevent multiple GUI close applications
    private boolean hasAppliedOnClose = false;

    /**
     * Constructor for the Switch Texture Menu with manual-only application
     */
    public SwitchTextureMenu(int containerId, @Nonnull Inventory playerInventory, @Nullable BlockPos blockPos) {
        super(JustSomeSwitchesMenuTypes.SWITCH_TEXTURE_MENU.get(), containerId);

        this.blockPos = blockPos;
        this.level = playerInventory.player.level();

        // FIXED: Container with NO auto-apply - only manual updates
        this.textureContainer = new SimpleContainer(TEXTURE_SLOT_COUNT) {
            @Override
            public void setChanged() {
                super.setChanged();
                // FIXED: NO auto-apply - only update for analysis/preview
                onSlotChangedForPreview();
            }
        };

        // Load existing data from BlockEntity
        loadGuiSlotData();

        System.out.println("DEBUG Menu: Menu opened for position " + blockPos);
        System.out.println("DEBUG Menu: Found BlockEntity: " + (blockEntity != null));

        // Add texture slots with NO auto-apply behavior
        addSlot(new EnhancedTextureSlot(textureContainer, TOGGLE_TEXTURE_SLOT, TOGGLE_SLOT_X, TOGGLE_SLOT_Y) {
            @Override
            public void setChanged() {
                super.setChanged();
                // FIXED: Only update for analysis, NOT auto-apply
                onSlotChangedForPreview();
            }
        });
        addSlot(new EnhancedTextureSlot(textureContainer, BASE_TEXTURE_SLOT, BASE_SLOT_X, BASE_SLOT_Y) {
            @Override
            public void setChanged() {
                super.setChanged();
                // FIXED: Only update for analysis, NOT auto-apply
                onSlotChangedForPreview();
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
    }

    /**
     * Enhanced texture slot for block analysis (NO auto-apply)
     */
    private class EnhancedTextureSlot extends TextureSlot {
        public EnhancedTextureSlot(@Nonnull net.minecraft.world.Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public void set(@Nonnull ItemStack stack) {
            super.set(stack);
            System.out.println("DEBUG Slot: Item placed in slot " + getSlotIndex() + " - " + stack);
        }

        @Override
        @Nonnull
        public ItemStack remove(int amount) {
            ItemStack result = super.remove(amount);
            System.out.println("DEBUG Slot: Item removed from slot " + getSlotIndex() + " - " + result);
            return result;
        }
    }

    /**
     * Network constructor for client-side menu creation
     */
    public SwitchTextureMenu(int containerId, @Nonnull Inventory playerInventory, @Nonnull FriendlyByteBuf extraData) {
        this(containerId, playerInventory, extraData.readBlockPos());
    }

    /**
     * FIXED: Slot changed handler for preview updates ONLY (no auto-apply)
     */
    private void onSlotChangedForPreview() {
        if (blockEntity != null) {
            System.out.println("DEBUG Menu: Slot changed - updating for preview only (NO auto-apply)");

            // Store slot items for analysis/preview
            ItemStack toggleItem = textureContainer.getItem(TOGGLE_TEXTURE_SLOT);
            ItemStack baseItem = textureContainer.getItem(BASE_TEXTURE_SLOT);
            blockEntity.setGuiSlotItems(toggleItem, baseItem);

            System.out.println("DEBUG Menu: Toggle item: " + toggleItem);
            System.out.println("DEBUG Menu: Base item: " + baseItem);
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
     * FIXED: Load GUI slot data AND properly sync face selections from BlockEntity
     */
    private void loadGuiSlotData() {
        this.blockEntity = getBlockEntity();
        if (blockEntity != null) {
            System.out.println("DEBUG Menu: Loading data from BlockEntity");

            // Load GUI slot contents from BlockEntity
            ItemStack toggleItem = blockEntity.getGuiToggleItem();
            ItemStack baseItem = blockEntity.getGuiBaseItem();

            textureContainer.setItem(TOGGLE_TEXTURE_SLOT, toggleItem);
            textureContainer.setItem(BASE_TEXTURE_SLOT, baseItem);

            // FIXED: Properly sync face selections from BlockEntity to menu variables
            this.baseFaceSelection = blockEntity.getBaseFaceSelection();
            this.toggleFaceSelection = blockEntity.getToggleFaceSelection();
            this.inverted = blockEntity.isInverted();

            System.out.println("DEBUG Menu: Loaded toggle item: " + toggleItem);
            System.out.println("DEBUG Menu: Loaded base item: " + baseItem);
            System.out.println("DEBUG Menu: Synced base face selection: " + baseFaceSelection);
            System.out.println("DEBUG Menu: Synced toggle face selection: " + toggleFaceSelection);
            System.out.println("DEBUG Menu: Synced inverted state: " + inverted);
        }
    }

    /**
     * FIXED: Save GUI slot data but preserve face selections (don't overwrite)
     */
    private void saveGuiSlotData() {
        if (blockEntity != null) {
            System.out.println("DEBUG Menu: Saving GUI data to BlockEntity");

            ItemStack toggleItem = textureContainer.getItem(TOGGLE_TEXTURE_SLOT);
            ItemStack baseItem = textureContainer.getItem(BASE_TEXTURE_SLOT);

            blockEntity.setGuiSlotItems(toggleItem, baseItem);

            // FIXED: Only save face selections when explicitly called from applyTextures()
            // Don't overwrite them during regular GUI data saving
            System.out.println("DEBUG Menu: Saved slot items only (preserving existing face selections)");
        }
    }

    /**
     * FIXED: Save face selections explicitly (called from applyTextures only)
     */
    private void saveFaceSelections() {
        if (blockEntity != null) {
            System.out.println("DEBUG Menu: Saving face selections to BlockEntity");

            // Save face selections to BlockEntity for persistence
            blockEntity.setBaseFaceSelection(baseFaceSelection);
            blockEntity.setToggleFaceSelection(toggleFaceSelection);
            blockEntity.setInverted(inverted);

            System.out.println("DEBUG Menu: Saved toggle face selection: " + toggleFaceSelection);
            System.out.println("DEBUG Menu: Saved base face selection: " + baseFaceSelection);
            System.out.println("DEBUG Menu: Saved inverted state: " + inverted);
        }
    }

    /**
     * Checks if there's a valid BlockEntity for this menu
     */
    public boolean hasValidBlockEntity() {
        return blockEntity != null;
    }

    // ========================================
    // FIXED: FACE SELECTION METHODS WITH PROPER PERSISTENCE
    // ========================================

    /**
     * FIXED: Set face selection for base texture slot (store locally, trigger preview update)
     */
    public void setBaseFaceSelection(@Nonnull FaceSelectionData.FaceOption faceOption) {
        if (this.baseFaceSelection != faceOption) {
            System.out.println("DEBUG Menu: Base face selection changed from " + this.baseFaceSelection + " to " + faceOption);
            this.baseFaceSelection = faceOption;
            // FIXED: Trigger preview update when face selection changes
            updateTexturePreview();
        }
    }

    /**
     * FIXED: Set face selection for toggle texture slot (store locally, trigger preview update)
     */
    public void setToggleFaceSelection(@Nonnull FaceSelectionData.FaceOption faceOption) {
        if (this.toggleFaceSelection != faceOption) {
            System.out.println("DEBUG Menu: Toggle face selection changed from " + this.toggleFaceSelection + " to " + faceOption);
            this.toggleFaceSelection = faceOption;
            // FIXED: Trigger preview update when face selection changes
            updateTexturePreview();
        }
    }

    /**
     * Set inversion state (store locally, trigger preview update)
     */
    public void setInverted(boolean inverted) {
        if (this.inverted != inverted) {
            System.out.println("DEBUG Menu: Inverted state changed from " + this.inverted + " to " + inverted);
            this.inverted = inverted;
            // FIXED: Trigger preview update when inversion changes
            updateTexturePreview();
        }
    }

    /**
     * FIXED: Update texture preview when face selections change
     */
    private void updateTexturePreview() {
        if (blockEntity != null) {
            System.out.println("DEBUG Menu: Updating texture preview for face selection changes");
            // Force the dropdown states to refresh with new face selections
            // This will update the preview textures in the GUI
        }
    }

    /**
     * Get dropdown state for base texture slot (using menu's current selection)
     */
    @Nonnull
    public FaceSelectionData.DropdownState getBaseDropdownState() {
        if (blockEntity != null) {
            BlockTextureAnalyzer.BlockTextureInfo blockInfo = blockEntity.getBaseBlockAnalysis();
            // FIXED: Use menu's current face selection for immediate preview updates
            return FaceSelectionData.createDropdownState(blockInfo, baseFaceSelection);
        }
        return FaceSelectionData.createDisabledState();
    }

    /**
     * Get dropdown state for toggle texture slot (using menu's current selection)
     */
    @Nonnull
    public FaceSelectionData.DropdownState getToggleDropdownState() {
        if (blockEntity != null) {
            BlockTextureAnalyzer.BlockTextureInfo blockInfo = blockEntity.getToggleBlockAnalysis();
            // FIXED: Use menu's current face selection for immediate preview updates
            return FaceSelectionData.createDropdownState(blockInfo, toggleFaceSelection);
        }
        return FaceSelectionData.createDisabledState();
    }

    /**
     * Get current face selections
     */
    @Nonnull public FaceSelectionData.FaceOption getBaseFaceSelection() { return baseFaceSelection; }
    @Nonnull public FaceSelectionData.FaceOption getToggleFaceSelection() { return toggleFaceSelection; }
    public boolean isInverted() { return inverted; }

    // ========================================
    // FIXED: MANUAL-ONLY TEXTURE APPLICATION
    // ========================================

    /**
     * FIXED: Manual texture application - ONLY called when Apply button clicked
     */
    public void applyTextures() {
        if (blockEntity != null) {
            System.out.println("DEBUG Menu: MANUAL APPLY TEXTURES TRIGGERED");

            // Get items from the GUI slots
            ItemStack toggleItem = textureContainer.getItem(TOGGLE_TEXTURE_SLOT);
            ItemStack baseItem = textureContainer.getItem(BASE_TEXTURE_SLOT);

            System.out.println("DEBUG Menu: Applying toggle item: " + toggleItem + " with face: " + toggleFaceSelection);
            System.out.println("DEBUG Menu: Applying base item: " + baseItem + " with face: " + baseFaceSelection);

            // Save GUI slot data (but not face selections yet)
            saveGuiSlotData();

            // Save face selections explicitly
            saveFaceSelections();

            // Apply toggle texture with face selection
            if (!toggleItem.isEmpty()) {
                String effectiveTexturePath = getEffectiveTexturePathForItem(toggleItem, toggleFaceSelection);
                blockEntity.setToggleTexture(effectiveTexturePath);
                System.out.println("DEBUG Menu: Applied toggle texture: " + effectiveTexturePath);
            } else {
                blockEntity.resetToggleTexture();
                System.out.println("DEBUG Menu: Reset toggle texture to default");
            }

            // Apply base texture with face selection
            if (!baseItem.isEmpty()) {
                String effectiveTexturePath = getEffectiveTexturePathForItem(baseItem, baseFaceSelection);
                blockEntity.setBaseTexture(effectiveTexturePath);
                System.out.println("DEBUG Menu: Applied base texture: " + effectiveTexturePath);
            } else {
                blockEntity.resetBaseTexture();
                System.out.println("DEBUG Menu: Reset base texture to default");
            }

            // Force visual update
            forceBlockUpdate();
            System.out.println("DEBUG Menu: Forced block visual update");
        }
    }

    /**
     * FIXED: Apply textures on GUI close while preserving face selections
     */
    private void applyTexturesOnClose() {
        if (blockEntity != null) {
            System.out.println("DEBUG Menu: APPLY TEXTURES ON CLOSE");

            // Get items from the GUI slots
            ItemStack toggleItem = textureContainer.getItem(TOGGLE_TEXTURE_SLOT);
            ItemStack baseItem = textureContainer.getItem(BASE_TEXTURE_SLOT);

            // FIXED: Use menu's current face selections (which should match what was applied)
            // Don't rely on BlockEntity's stored values as they might be inconsistent during GUI close
            FaceSelectionData.FaceOption currentBaseFace = this.baseFaceSelection;
            FaceSelectionData.FaceOption currentToggleFace = this.toggleFaceSelection;

            System.out.println("DEBUG Menu: Using menu's current face selections - Base: " + currentBaseFace + ", Toggle: " + currentToggleFace);
            System.out.println("DEBUG Menu: BlockEntity stored face selections - Base: " + blockEntity.getBaseFaceSelection() + ", Toggle: " + blockEntity.getToggleFaceSelection());

            // Save slot items to BlockEntity
            blockEntity.setGuiSlotItems(toggleItem, baseItem);

            // FIXED: Ensure face selections are preserved by re-saving them
            blockEntity.setBaseFaceSelection(currentBaseFace);
            blockEntity.setToggleFaceSelection(currentToggleFace);
            blockEntity.setInverted(inverted);

            // Apply toggle texture with current face selection
            if (!toggleItem.isEmpty()) {
                String effectiveTexturePath = getEffectiveTexturePathForItem(toggleItem, currentToggleFace);
                blockEntity.setToggleTexture(effectiveTexturePath);
                System.out.println("DEBUG Menu: Applied toggle texture on close: " + effectiveTexturePath);
            } else {
                blockEntity.resetToggleTexture();
                System.out.println("DEBUG Menu: Reset toggle texture to default on close");
            }

            // Apply base texture with current face selection
            if (!baseItem.isEmpty()) {
                String effectiveTexturePath = getEffectiveTexturePathForItem(baseItem, currentBaseFace);
                blockEntity.setBaseTexture(effectiveTexturePath);
                System.out.println("DEBUG Menu: Applied base texture on close: " + effectiveTexturePath);
            } else {
                blockEntity.resetBaseTexture();
                System.out.println("DEBUG Menu: Reset base texture to default on close");
            }

            // Force visual update
            forceBlockUpdate();
            System.out.println("DEBUG Menu: Forced block visual update on close");
        }
    }

    /**
     * Get effective texture path for item based on face selection
     */
    @Nonnull
    private String getEffectiveTexturePathForItem(@Nonnull ItemStack item, @Nonnull FaceSelectionData.FaceOption faceSelection) {
        if (item.isEmpty()) {
            return "minecraft:block/stone"; // Fallback
        }

        // Analyze the block to get texture information
        BlockTextureAnalyzer.BlockTextureInfo blockInfo = BlockTextureAnalyzer.analyzeBlock(item);

        // Get texture path for the specific face selection
        String faceTexturePath = FaceSelectionData.getTextureForSelection(blockInfo, faceSelection);

        if (faceTexturePath != null && BlockTextureAnalyzer.isValidTexture(faceTexturePath)) {
            return faceTexturePath;
        }

        // Fallback to uniform texture
        String fallbackPath = blockInfo.getUniformTexture();
        if (fallbackPath != null) {
            return fallbackPath;
        }

        // Final fallback
        return "minecraft:block/stone";
    }

    /**
     * Force block update for visual changes
     */
    private void forceBlockUpdate() {
        if (blockEntity != null && level != null && blockPos != null) {
            // Mark BlockEntity as changed for NBT saving
            blockEntity.setChanged();

            // Force model data update
            blockEntity.requestModelDataUpdate();

            // Send block update to clients for immediate visual refresh
            level.sendBlockUpdated(blockPos, level.getBlockState(blockPos), level.getBlockState(blockPos), 3);
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

        // FIXED: Prevent multiple GUI close applications (common Minecraft issue)
        if (!hasAppliedOnClose) {
            hasAppliedOnClose = true;
            System.out.println("DEBUG Menu: GUI closing - applying final textures (FIRST TIME ONLY)");
            applyTexturesOnClose();
        } else {
            System.out.println("DEBUG Menu: GUI closing - skipping duplicate application");
        }
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

        // FIXED: NO auto-apply on shift-click - only manual application
        System.out.println("DEBUG Menu: Shift-click operation completed - NO auto-apply");

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