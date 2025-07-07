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
 * SIMPLIFIED FIX: Direct BlockEntity loading instead of delayed system
 * ---
 * FIXED: Loads face selections immediately from BlockEntity
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

    // Player inventory positioning
    private static final int PLAYER_INV_Y = 98;
    private static final int HOTBAR_Y = 156;

    // Instance data
    private final SimpleContainer textureContainer;
    private final BlockPos blockPos;
    private final Level level;
    private SwitchesLeverBlockEntity blockEntity;

    // Instance tracking
    private final String menuInstanceId;

    // SIMPLIFIED: Direct face selection storage
    private FaceSelectionData.FaceOption baseFaceSelection = FaceSelectionData.FaceOption.ALL;
    private FaceSelectionData.FaceOption toggleFaceSelection = FaceSelectionData.FaceOption.ALL;
    private boolean inverted = false;

    // Initialization control
    private boolean isInitializing = true;

    /**
     * SIMPLIFIED: Constructor with direct BlockEntity loading
     */
    public SwitchTextureMenu(int containerId, @Nonnull Inventory playerInventory, @Nullable BlockPos blockPos) {
        super(JustSomeSwitchesMenuTypes.SWITCH_TEXTURE_MENU.get(), containerId);

        this.blockPos = blockPos;
        this.level = playerInventory.player.level();
        this.menuInstanceId = Integer.toHexString(System.identityHashCode(this));

        System.out.println("DEBUG Menu: ===== SIMPLIFIED FIX - DIRECT LOADING MENU =====");
        System.out.println("DEBUG Menu: Menu instance ID: " + menuInstanceId);

        // Get BlockEntity reference
        this.blockEntity = getBlockEntity();

        if (blockEntity != null) {
            System.out.println("DEBUG Menu: Found BlockEntity, loading face selections directly");
            loadBlockEntityDataDirectly();
        } else {
            System.out.println("DEBUG Menu: ⚠️ No BlockEntity found");
        }

        // AUTO-APPLY container
        this.textureContainer = new SimpleContainer(TEXTURE_SLOT_COUNT) {
            @Override
            public void setChanged() {
                super.setChanged();
                onSlotChangedAutoApply();
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

        this.isInitializing = false;
        System.out.println("DEBUG Menu: ===== SIMPLIFIED MENU CREATED =====");
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
        }

        @Override
        @Nonnull
        public ItemStack remove(int amount) {
            return super.remove(amount);
        }
    }

    /**
     * Network constructor
     */
    public SwitchTextureMenu(int containerId, @Nonnull Inventory playerInventory, @Nonnull FriendlyByteBuf extraData) {
        this(containerId, playerInventory, extraData.readBlockPos());
    }

    /**
     * SIMPLIFIED: Direct BlockEntity data loading
     */
    private void loadBlockEntityDataDirectly() {
        if (blockEntity == null) {
            System.out.println("DEBUG Menu: No BlockEntity for direct loading");
            return;
        }

        System.out.println("DEBUG Menu: SIMPLIFIED - Loading face selections directly from BlockEntity");

        // Load face selections directly - no delayed loading
        this.baseFaceSelection = blockEntity.getBaseFaceSelection();
        this.toggleFaceSelection = blockEntity.getToggleFaceSelection();
        this.inverted = blockEntity.isInverted();

        System.out.println("DEBUG Menu: SIMPLIFIED - Loaded directly - Base: " + baseFaceSelection +
                ", Toggle: " + toggleFaceSelection + ", Inverted: " + inverted);
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
     * Load GUI slot items
     */
    private void loadGuiSlotItems() {
        System.out.println("DEBUG Menu: Loading GUI slot items");

        if (blockEntity != null) {
            ItemStack toggleItem = blockEntity.getGuiToggleItem();
            ItemStack baseItem = blockEntity.getGuiBaseItem();

            textureContainer.setItem(TOGGLE_TEXTURE_SLOT, toggleItem);
            textureContainer.setItem(BASE_TEXTURE_SLOT, baseItem);

            System.out.println("DEBUG Menu: Loaded toggle item: " + toggleItem);
            System.out.println("DEBUG Menu: Loaded base item: " + baseItem);
        }
    }

    /**
     * AUTO-APPLY: Slot changed handler
     */
    private void onSlotChangedAutoApply() {
        if (isInitializing) {
            System.out.println("DEBUG Menu: Skipping auto-apply during initialization");
            return;
        }

        if (blockEntity != null) {
            System.out.println("DEBUG Menu: AUTO-APPLY triggered by slot change");

            ItemStack toggleItem = textureContainer.getItem(TOGGLE_TEXTURE_SLOT);
            ItemStack baseItem = textureContainer.getItem(BASE_TEXTURE_SLOT);

            // Store GUI slot items
            blockEntity.setGuiSlotItems(toggleItem, baseItem);

            // Apply textures with current face selections
            applyTexturesWithCurrentFaceSelections(toggleItem, baseItem);

            // Force visual update
            forceBlockUpdate();
            System.out.println("DEBUG Menu: Auto-apply completed");
        }
    }

    /**
     * Apply textures using Menu's current face selections
     */
    private void applyTexturesWithCurrentFaceSelections(@Nonnull ItemStack toggleItem, @Nonnull ItemStack baseItem) {
        if (blockEntity == null) return;

        System.out.println("DEBUG Menu: Applying textures with menu face selections");
        System.out.println("DEBUG Menu: Menu selections - Base: " + baseFaceSelection +
                ", Toggle: " + toggleFaceSelection + ", Inverted: " + inverted);

        // Sync face selections to BlockEntity
        blockEntity.setBaseFaceSelection(baseFaceSelection);
        blockEntity.setToggleFaceSelection(toggleFaceSelection);
        blockEntity.setInverted(inverted);

        // Apply textures
        if (!toggleItem.isEmpty()) {
            String effectiveTexturePath = getEffectiveTexturePathForItem(toggleItem, toggleFaceSelection);
            blockEntity.setToggleTexture(effectiveTexturePath);
        } else {
            blockEntity.resetToggleTexture();
        }

        if (!baseItem.isEmpty()) {
            String effectiveTexturePath = getEffectiveTexturePathForItem(baseItem, baseFaceSelection);
            blockEntity.setBaseTexture(effectiveTexturePath);
        } else {
            blockEntity.resetBaseTexture();
        }

        blockEntity.setChanged();
    }

    // ========================================
    // FACE SELECTION METHODS
    // ========================================

    public void setBaseFaceSelection(@Nonnull FaceSelectionData.FaceOption faceOption) {
        System.out.println("DEBUG Menu: Setting base face selection to " + faceOption);

        if (this.baseFaceSelection != faceOption) {
            this.baseFaceSelection = faceOption;

            if (blockEntity != null && !isInitializing) {
                ItemStack baseItem = textureContainer.getItem(BASE_TEXTURE_SLOT);
                ItemStack toggleItem = textureContainer.getItem(TOGGLE_TEXTURE_SLOT);
                applyTexturesWithCurrentFaceSelections(toggleItem, baseItem);
                forceBlockUpdate();
            }
        }
    }

    public void setToggleFaceSelection(@Nonnull FaceSelectionData.FaceOption faceOption) {
        System.out.println("DEBUG Menu: Setting toggle face selection to " + faceOption);

        if (this.toggleFaceSelection != faceOption) {
            this.toggleFaceSelection = faceOption;

            if (blockEntity != null && !isInitializing) {
                ItemStack baseItem = textureContainer.getItem(BASE_TEXTURE_SLOT);
                ItemStack toggleItem = textureContainer.getItem(TOGGLE_TEXTURE_SLOT);
                applyTexturesWithCurrentFaceSelections(toggleItem, baseItem);
                forceBlockUpdate();
            }
        }
    }

    public void setInverted(boolean inverted) {
        System.out.println("DEBUG Menu: Setting inverted state to " + inverted);

        if (this.inverted != inverted) {
            this.inverted = inverted;

            if (blockEntity != null && !isInitializing) {
                ItemStack baseItem = textureContainer.getItem(BASE_TEXTURE_SLOT);
                ItemStack toggleItem = textureContainer.getItem(TOGGLE_TEXTURE_SLOT);
                applyTexturesWithCurrentFaceSelections(toggleItem, baseItem);
                forceBlockUpdate();
            }
        }
    }

    /**
     * Get dropdown state for base texture slot
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
     * Get dropdown state for toggle texture slot
     */
    @Nonnull
    public FaceSelectionData.DropdownState getToggleDropdownState() {
        if (blockEntity != null) {
            BlockTextureAnalyzer.BlockTextureInfo blockInfo = blockEntity.getToggleBlockAnalysis();
            return FaceSelectionData.createDropdownState(blockInfo, toggleFaceSelection);
        }
        return FaceSelectionData.createDisabledState();
    }

    // Getters
    @Nonnull public FaceSelectionData.FaceOption getBaseFaceSelection() { return baseFaceSelection; }
    @Nonnull public FaceSelectionData.FaceOption getToggleFaceSelection() { return toggleFaceSelection; }
    public boolean isInverted() { return inverted; }

    /**
     * Get effective texture path for item based on face selection
     */
    @Nonnull
    private String getEffectiveTexturePathForItem(@Nonnull ItemStack item, @Nonnull FaceSelectionData.FaceOption faceSelection) {
        if (item.isEmpty()) {
            return "minecraft:block/stone";
        }

        BlockTextureAnalyzer.BlockTextureInfo blockInfo = BlockTextureAnalyzer.analyzeBlock(item);
        String faceTexturePath = FaceSelectionData.getTextureForSelection(blockInfo, faceSelection);

        if (faceTexturePath != null && BlockTextureAnalyzer.isValidTexture(faceTexturePath)) {
            return faceTexturePath;
        }

        String fallbackPath = blockInfo.getUniformTexture();
        if (fallbackPath != null) {
            return fallbackPath;
        }

        return "minecraft:block/stone";
    }

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

    /**
     * Block analysis methods for GUI
     */
    @Nonnull
    public BlockTextureAnalyzer.BlockTextureInfo getToggleBlockAnalysis() {
        ItemStack toggleItem = textureContainer.getItem(TOGGLE_TEXTURE_SLOT);
        return BlockTextureAnalyzer.analyzeBlock(toggleItem);
    }

    @Nonnull
    public BlockTextureAnalyzer.BlockTextureInfo getBaseBlockAnalysis() {
        ItemStack baseItem = textureContainer.getItem(BASE_TEXTURE_SLOT);
        return BlockTextureAnalyzer.analyzeBlock(baseItem);
    }

    /**
     * Save menu face selections to BlockEntity when GUI closes
     */
    @Override
    public void removed(@Nonnull Player player) {
        System.out.println("DEBUG Menu: ===== GUI CLOSING =====");
        System.out.println("DEBUG Menu: Current menu state - Base: " + baseFaceSelection +
                ", Toggle: " + toggleFaceSelection + ", Inverted: " + inverted);

        super.removed(player);

        if (blockEntity != null) {
            // Save menu's current face selections to BlockEntity
            blockEntity.setBaseFaceSelection(baseFaceSelection);
            blockEntity.setToggleFaceSelection(toggleFaceSelection);
            blockEntity.setInverted(inverted);
            blockEntity.setChanged();

            System.out.println("DEBUG Menu: Face selections saved to BlockEntity");
        }

        System.out.println("DEBUG Menu: ===== GUI CLOSED =====");
    }

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