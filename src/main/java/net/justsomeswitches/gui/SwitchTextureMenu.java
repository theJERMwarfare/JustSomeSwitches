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
 * CRITICAL FIX: Enhanced server-side menu with Immediate NBT Persistence for Face Selections
 * ---
 * SOLUTION: Force immediate NBT saves when face selections change to prevent loss during state changes
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

    // CRITICAL FIX: Face selection state tracking with proper initialization
    private FaceSelectionData.FaceOption baseFaceSelection = FaceSelectionData.FaceOption.ALL;
    private FaceSelectionData.FaceOption toggleFaceSelection = FaceSelectionData.FaceOption.ALL;
    private boolean inverted = false;

    // CRITICAL FIX: Prevent overwriting during initialization
    private boolean isInitializing = true;
    private boolean hasLoadedFromBlockEntity = false;

    /**
     * Constructor for the Switch Texture Menu with auto-apply system
     */
    public SwitchTextureMenu(int containerId, @Nonnull Inventory playerInventory, @Nullable BlockPos blockPos) {
        super(JustSomeSwitchesMenuTypes.SWITCH_TEXTURE_MENU.get(), containerId);

        this.blockPos = blockPos;
        this.level = playerInventory.player.level();

        System.out.println("DEBUG Menu: Menu opened with AUTO-APPLY system for position " + blockPos);

        // CRITICAL FIX: Load BlockEntity data FIRST and ONLY ONCE
        loadBlockEntityDataOnce();

        // AUTO-APPLY: Container that triggers immediate application
        this.textureContainer = new SimpleContainer(TEXTURE_SLOT_COUNT) {
            @Override
            public void setChanged() {
                super.setChanged();
                // AUTO-APPLY: Apply textures immediately when slots change
                onSlotChangedAutoApply();
            }
        };

        // CRITICAL FIX: Load GUI slot items AFTER face selections are synced
        loadGuiSlotItems();

        System.out.println("DEBUG Menu: Found BlockEntity: " + (blockEntity != null));

        // Add texture slots with auto-apply behavior
        addSlot(new EnhancedTextureSlot(textureContainer, TOGGLE_TEXTURE_SLOT, TOGGLE_SLOT_X, TOGGLE_SLOT_Y));
        addSlot(new EnhancedTextureSlot(textureContainer, BASE_TEXTURE_SLOT, BASE_SLOT_X, BASE_SLOT_Y));

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

        // CRITICAL FIX: Initialization complete - enable auto-apply
        this.isInitializing = false;
        System.out.println("DEBUG Menu: Initialization complete - auto-apply system enabled");
        System.out.println("DEBUG Menu: Face selections - Base: " + baseFaceSelection + ", Toggle: " + toggleFaceSelection);
    }

    /**
     * Enhanced texture slot for auto-apply functionality
     */
    private class EnhancedTextureSlot extends TextureSlot {
        public EnhancedTextureSlot(@Nonnull net.minecraft.world.Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public void set(@Nonnull ItemStack stack) {
            super.set(stack);
            // Silent operation for performance
        }

        @Override
        @Nonnull
        public ItemStack remove(int amount) {
            ItemStack result = super.remove(amount);
            // Silent operation for performance
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
     * AUTO-APPLY: Slot changed handler for immediate texture application
     */
    private void onSlotChangedAutoApply() {
        // CRITICAL FIX: Don't auto-apply during initialization to prevent face selection reset
        if (isInitializing) {
            return;
        }

        if (blockEntity != null) {
            System.out.println("DEBUG Menu: AUTO-APPLY triggered by slot change");

            // Get items from the GUI slots
            ItemStack toggleItem = textureContainer.getItem(TOGGLE_TEXTURE_SLOT);
            ItemStack baseItem = textureContainer.getItem(BASE_TEXTURE_SLOT);

            System.out.println("DEBUG Menu: Auto-applying toggle item: " + toggleItem + " with face: " + toggleFaceSelection);
            System.out.println("DEBUG Menu: Auto-applying base item: " + baseItem + " with face: " + baseFaceSelection);

            // Store GUI slot items
            blockEntity.setGuiSlotItems(toggleItem, baseItem);

            // CRITICAL FIX: Apply current face selections and textures together atomically
            applyTexturesWithCurrentFaceSelections(toggleItem, baseItem);

            // Force visual update
            forceBlockUpdate();
            System.out.println("DEBUG Menu: Auto-apply completed with visual update");
        }
    }

    /**
     * CRITICAL FIX: Apply textures using MENU's current face selections with immediate NBT save
     */
    private void applyTexturesWithCurrentFaceSelections(@Nonnull ItemStack toggleItem, @Nonnull ItemStack baseItem) {
        if (blockEntity == null) return;

        System.out.println("DEBUG Menu: CRITICAL FIX - Applying textures with MENU's face selections");
        System.out.println("DEBUG Menu: Current menu selections - Base: " + baseFaceSelection + ", Toggle: " + toggleFaceSelection + ", Inverted: " + inverted);

        // STEP 1: Set face selections in BlockEntity from MENU's current values
        boolean baseFaceChanged = blockEntity.setBaseFaceSelection(baseFaceSelection);
        boolean toggleFaceChanged = blockEntity.setToggleFaceSelection(toggleFaceSelection);
        boolean invertedChanged = blockEntity.setInverted(inverted);

        System.out.println("DEBUG Menu: Face selection sync - Base changed: " + baseFaceChanged +
                ", Toggle changed: " + toggleFaceChanged + ", Inverted changed: " + invertedChanged);

        // STEP 2: Apply textures with current face selections
        if (!toggleItem.isEmpty()) {
            String effectiveTexturePath = getEffectiveTexturePathForItem(toggleItem, toggleFaceSelection);
            blockEntity.setToggleTexture(effectiveTexturePath);
            System.out.println("DEBUG Menu: Applied toggle texture: " + effectiveTexturePath + " with face: " + toggleFaceSelection);
        } else {
            blockEntity.resetToggleTexture();
            System.out.println("DEBUG Menu: Reset toggle texture to default");
        }

        if (!baseItem.isEmpty()) {
            String effectiveTexturePath = getEffectiveTexturePathForItem(baseItem, baseFaceSelection);
            blockEntity.setBaseTexture(effectiveTexturePath);
            System.out.println("DEBUG Menu: Applied base texture: " + effectiveTexturePath + " with face: " + baseFaceSelection);
        } else {
            blockEntity.resetBaseTexture();
            System.out.println("DEBUG Menu: Reset base texture to default");
        }

        // CRITICAL FIX: Force immediate NBT persistence from Menu side as well
        if (baseFaceChanged || toggleFaceChanged || invertedChanged) {
            System.out.println("DEBUG Menu: CRITICAL FIX - Triggering immediate NBT save due to face selection changes");
            blockEntity.preserveFaceSelectionsForStateChange();
        }

        System.out.println("DEBUG Menu: CRITICAL FIX - Atomic texture+face application completed using menu's selections");
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
     * CRITICAL FIX: Load BlockEntity data ONLY ONCE during initialization
     */
    private void loadBlockEntityDataOnce() {
        if (hasLoadedFromBlockEntity) {
            System.out.println("DEBUG Menu: CRITICAL FIX - Skipping BlockEntity reload to preserve face selections");
            return;
        }

        this.blockEntity = getBlockEntity();
        if (blockEntity != null) {
            System.out.println("DEBUG Menu: CRITICAL FIX - Loading BlockEntity data ONCE during initialization");

            // CRITICAL FIX: Only load if this is the FIRST time
            FaceSelectionData.FaceOption blockEntityBaseFace = blockEntity.getBaseFaceSelection();
            FaceSelectionData.FaceOption blockEntityToggleFace = blockEntity.getToggleFaceSelection();
            boolean blockEntityInverted = blockEntity.isInverted();

            System.out.println("DEBUG Menu: BlockEntity stored state - Base: " + blockEntityBaseFace +
                    ", Toggle: " + blockEntityToggleFace + ", Inverted: " + blockEntityInverted);

            // CRITICAL FIX: Use BlockEntity's values only on initial load
            this.baseFaceSelection = blockEntityBaseFace;
            this.toggleFaceSelection = blockEntityToggleFace;
            this.inverted = blockEntityInverted;

            this.hasLoadedFromBlockEntity = true;

            System.out.println("DEBUG Menu: CRITICAL FIX - Initial sync complete - Base: " + baseFaceSelection +
                    ", Toggle: " + toggleFaceSelection + ", Inverted: " + inverted);
        }
    }

    /**
     * Load GUI slot items AFTER face selections are already synced
     */
    private void loadGuiSlotItems() {
        if (blockEntity != null) {
            System.out.println("DEBUG Menu: Loading GUI slot items (face selections already synced)");

            // Load GUI slot contents from BlockEntity
            ItemStack toggleItem = blockEntity.getGuiToggleItem();
            ItemStack baseItem = blockEntity.getGuiBaseItem();

            textureContainer.setItem(TOGGLE_TEXTURE_SLOT, toggleItem);
            textureContainer.setItem(BASE_TEXTURE_SLOT, baseItem);

            System.out.println("DEBUG Menu: Loaded toggle item: " + toggleItem);
            System.out.println("DEBUG Menu: Loaded base item: " + baseItem);
        }
    }

    /**
     * Checks if there's a valid BlockEntity for this menu
     */
    public boolean hasValidBlockEntity() {
        return blockEntity != null;
    }

    // ========================================
    // CRITICAL FIX: FACE SELECTION METHODS WITH MENU STATE PROTECTION
    // ========================================

    /**
     * CRITICAL FIX: Set face selection for base texture slot with menu state protection
     */
    public void setBaseFaceSelection(@Nonnull FaceSelectionData.FaceOption faceOption) {
        if (this.baseFaceSelection != faceOption) {
            System.out.println("DEBUG Menu: CRITICAL FIX - Base face selection changed from " + this.baseFaceSelection + " to " + faceOption);
            this.baseFaceSelection = faceOption;

            // CRITICAL FIX: Immediately apply with current menu state
            if (blockEntity != null && !isInitializing) {
                ItemStack baseItem = textureContainer.getItem(BASE_TEXTURE_SLOT);
                ItemStack toggleItem = textureContainer.getItem(TOGGLE_TEXTURE_SLOT);

                // Apply textures with MENU's current face selections
                applyTexturesWithCurrentFaceSelections(toggleItem, baseItem);
                forceBlockUpdate();

                System.out.println("DEBUG Menu: CRITICAL FIX - Base face selection applied with menu state protection");
            }
        }
    }

    /**
     * CRITICAL FIX: Set face selection for toggle texture slot with menu state protection
     */
    public void setToggleFaceSelection(@Nonnull FaceSelectionData.FaceOption faceOption) {
        if (this.toggleFaceSelection != faceOption) {
            System.out.println("DEBUG Menu: CRITICAL FIX - Toggle face selection changed from " + this.toggleFaceSelection + " to " + faceOption);
            this.toggleFaceSelection = faceOption;

            // CRITICAL FIX: Immediately apply with current menu state
            if (blockEntity != null && !isInitializing) {
                ItemStack baseItem = textureContainer.getItem(BASE_TEXTURE_SLOT);
                ItemStack toggleItem = textureContainer.getItem(TOGGLE_TEXTURE_SLOT);

                // Apply textures with MENU's current face selections
                applyTexturesWithCurrentFaceSelections(toggleItem, baseItem);
                forceBlockUpdate();

                System.out.println("DEBUG Menu: CRITICAL FIX - Toggle face selection applied with menu state protection");
            }
        }
    }

    /**
     * CRITICAL FIX: Set inversion state with menu state protection
     */
    public void setInverted(boolean inverted) {
        if (this.inverted != inverted) {
            System.out.println("DEBUG Menu: CRITICAL FIX - Inverted state changed from " + this.inverted + " to " + inverted);
            this.inverted = inverted;

            // CRITICAL FIX: Immediately apply with current menu state
            if (blockEntity != null && !isInitializing) {
                ItemStack baseItem = textureContainer.getItem(BASE_TEXTURE_SLOT);
                ItemStack toggleItem = textureContainer.getItem(TOGGLE_TEXTURE_SLOT);

                // Apply textures with MENU's current face selections
                applyTexturesWithCurrentFaceSelections(toggleItem, baseItem);
                forceBlockUpdate();

                System.out.println("DEBUG Menu: CRITICAL FIX - Inversion state applied with menu state protection");
            }
        }
    }

    /**
     * Get dropdown state for base texture slot (using menu's current selection)
     */
    @Nonnull
    public FaceSelectionData.DropdownState getBaseDropdownState() {
        if (blockEntity != null) {
            BlockTextureAnalyzer.BlockTextureInfo blockInfo = blockEntity.getBaseBlockAnalysis();
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

    /**
     * CRITICAL FIX: Save menu face selections to BlockEntity when GUI closes
     */
    @Override
    public void removed(@Nonnull Player player) {
        super.removed(player);

        System.out.println("DEBUG Menu: CRITICAL FIX - GUI closing, saving menu face selections to BlockEntity");

        if (blockEntity != null) {
            // CRITICAL FIX: Save menu's current face selections to BlockEntity permanently
            boolean baseFaceChanged = blockEntity.setBaseFaceSelection(baseFaceSelection);
            boolean toggleFaceChanged = blockEntity.setToggleFaceSelection(toggleFaceSelection);
            boolean invertedChanged = blockEntity.setInverted(inverted);

            System.out.println("DEBUG Menu: Final save - Base: " + baseFaceSelection + " (changed: " + baseFaceChanged + ")");
            System.out.println("DEBUG Menu: Final save - Toggle: " + toggleFaceSelection + " (changed: " + toggleFaceChanged + ")");
            System.out.println("DEBUG Menu: Final save - Inverted: " + inverted + " (changed: " + invertedChanged + ")");

            // CRITICAL FIX: Force immediate NBT save on GUI close
            System.out.println("DEBUG Menu: CRITICAL FIX - Forcing immediate NBT save on GUI close");
            blockEntity.preserveFaceSelectionsForStateChange();

            System.out.println("DEBUG Menu: CRITICAL FIX - Menu face selections permanently saved to BlockEntity");
        } else {
            System.out.println("DEBUG Menu: WARNING - No BlockEntity available for final face selection save");
        }

        System.out.println("DEBUG Menu: CRITICAL FIX - GUI closed with face selections preserved in BlockEntity");
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

        // AUTO-APPLY: Shift-click operations are handled by slot change events
        System.out.println("DEBUG Menu: Shift-click operation completed - auto-apply handled updates");

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