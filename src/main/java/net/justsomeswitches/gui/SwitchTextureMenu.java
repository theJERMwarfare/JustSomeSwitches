package net.justsomeswitches.gui;

import net.justsomeswitches.blockentity.SwitchesLeverBlockEntity;
import net.justsomeswitches.network.NetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Clean Architecture Texture Customization Menu
 * <p>
 * Implements independent texture categories with minimal complexity:
 * - Texture slots: Simple containers that trigger dropdown analysis
 * - Dropdown menus: Analyze blocks and handle user selections
 * - Texture previews: Display textures and save to NBT
 * - Complete independence: Each category works separately
 * 
 * @since 1.0.0
 */
public class SwitchTextureMenu extends AbstractContainerMenu {

    private static final Logger LOGGER = LoggerFactory.getLogger(SwitchTextureMenu.class);

    // Simple slot layout constants
    private static final int TEXTURE_SLOT_COUNT = 2;
    private static final int TOGGLE_TEXTURE_SLOT = 0;
    private static final int BASE_TEXTURE_SLOT = 1;

    // Positioning constants  
    private static final int TOGGLE_SLOT_X = 28;
    private static final int TOGGLE_SLOT_Y = 27;
    private static final int BASE_SLOT_X = 132;
    private static final int BASE_SLOT_Y = 27;
    private static final int PLAYER_INV_Y = 98;
    private static final int HOTBAR_Y = 156;

    // Core components
    private final ItemStackHandler textureItemHandler;
    private final BlockPos blockPos;
    private final Level level;
    private SwitchesLeverBlockEntity blockEntity;

    // Independent texture category state (read from BlockEntity)
    private String baseTextureVariable = "all";
    private String toggleTextureVariable = "all";
    private SwitchesLeverBlockEntity.PowerMode powerMode = SwitchesLeverBlockEntity.PowerMode.DEFAULT;
    
    // Initialization flag to prevent analysis during menu setup
    private boolean isInitializing = true;
    
    // Slot loading flag to prevent analysis during slot synchronization
    private boolean isLoadingSlots = false;
    
    // Comprehensive sync protection - track when any synchronization is happening
    private boolean isSynchronizing = false;
    
    // Track expected slot contents to detect genuine user changes vs sync operations
    private ItemStack expectedToggleItem = ItemStack.EMPTY;
    private ItemStack expectedBaseItem = ItemStack.EMPTY;

    /**
     * Creates a new Switch Texture Menu with clean independent architecture.
     * 
     * @param containerId the unique container ID for this menu instance
     * @param playerInventory the player's inventory for slot management
     * @param blockPos the position of the switch block being customized
     */
    public SwitchTextureMenu(int containerId, @Nonnull Inventory playerInventory, @Nullable BlockPos blockPos) {
        super(JustSomeSwitchesMenuTypes.SWITCH_TEXTURE_MENU.get(), containerId);

        this.blockPos = blockPos;
        this.level = playerInventory.player.level();

        // Initialize BlockEntity connection
        initializeBlockEntity();

        // Create simple texture item handler - just storage, minimal logic
        this.textureItemHandler = new ItemStackHandler(TEXTURE_SLOT_COUNT) {
            @Override
            protected void onContentsChanged(int slot) {
                // CLEAN ARCHITECTURE: Only basic storage operations
                if (blockEntity != null) {
                    blockEntity.setChanged();
                }
            }
            
            @Override
            public void setStackInSlot(int slot, @Nonnull ItemStack stack) {
                // Detect potential synchronization operations
                if (!isInitializing && !isLoadingSlots) {
                    // This might be a client-server sync operation
                    isSynchronizing = true;
                    try {
                        super.setStackInSlot(slot, stack);
                    } finally {
                        // Reset sync flag after a short delay to allow callbacks to complete
                        isSynchronizing = false;
                    }
                } else {
                    // During initialization/loading, use normal operation
                    super.setStackInSlot(slot, stack);
                }
            }
        };

        // Load current slot items from BlockEntity
        loadSlotItemsFromBlockEntity();

        // Add simple texture slots with minimal validation
        addSlot(new SimpleTextureSlot(textureItemHandler, TOGGLE_TEXTURE_SLOT, TOGGLE_SLOT_X, TOGGLE_SLOT_Y, this::onToggleSlotChanged));
        addSlot(new SimpleTextureSlot(textureItemHandler, BASE_TEXTURE_SLOT, BASE_SLOT_X, BASE_SLOT_Y, this::onBaseSlotChanged));

        // Add player inventory slots
        addPlayerInventorySlots(playerInventory);
        
        // Keep initialization flag true during construction to prevent slot callbacks from running analysis
        // The flag will be set to false after GUI is fully rendered and ready

        // CRITICAL FIX: Server-side menus need to complete initialization too
        // On server side, complete initialization immediately since there's no GUI rendering
        if (!level.isClientSide) {
            this.isInitializing = false;
        }

        LOGGER.info("Clean architecture texture menu created for block at {}", blockPos);
    }

    /**
     * Network constructor for client-side menu creation.
     */
    public SwitchTextureMenu(int containerId, @Nonnull Inventory playerInventory, @Nonnull FriendlyByteBuf extraData) {
        this(containerId, playerInventory, extraData.readBlockPos());
    }

    // ========================================
    // INDEPENDENT TEXTURE CATEGORY HANDLERS
    // ========================================

    /**
     * TOGGLE TEXTURE CATEGORY - Independent handler
     * Called when toggle slot contents change - triggers dropdown analysis
     */
    private void onToggleSlotChanged() {
        if (blockEntity == null) return;
        
        // Skip analysis during initialization, slot loading, or any synchronization operations
        if (isInitializing || isLoadingSlots || isSynchronizing) {
            return;
        }
        
        // Get current slot item
        ItemStack toggleItem = textureItemHandler.getStackInSlot(TOGGLE_TEXTURE_SLOT);
        
        // COMPREHENSIVE PROTECTION: Only analyze if this is a genuine user change
        if (ItemStack.matches(toggleItem, expectedToggleItem)) {
            return;
        }
        
        // Update expected item for future comparison
        expectedToggleItem = toggleItem.copy();
        
        // Update BlockEntity with ONLY toggle slot item - true independence
        blockEntity.setToggleSlotItem(toggleItem);
        
        // If slot is now empty, reset toggle texture with sync suppression
        if (toggleItem.isEmpty()) {
            blockEntity.setSyncSuppressed(true);
            try {
                blockEntity.resetToggleTexture();
                this.toggleTextureVariable = "all";
                blockEntity.setToggleTextureVariable("all");
            } finally {
                blockEntity.setSyncSuppressed(false);
            }
            return;
        }

        // Trigger dropdown analysis for new block
        analyzeToggleBlock(toggleItem);
        
        LOGGER.debug("Toggle slot changed to: {}", toggleItem.isEmpty() ? "empty" : toggleItem.getItem().toString());
    }

    /**
     * BASE TEXTURE CATEGORY - Independent handler  
     * Called when base slot contents change - triggers dropdown analysis
     */
    private void onBaseSlotChanged() {
        if (blockEntity == null) return;
        
        // Skip analysis during initialization, slot loading, or any synchronization operations
        if (isInitializing || isLoadingSlots || isSynchronizing) {
            return;
        }
        
        // Get current slot item
        ItemStack baseItem = textureItemHandler.getStackInSlot(BASE_TEXTURE_SLOT);
        
        // COMPREHENSIVE PROTECTION: Only analyze if this is a genuine user change
        if (ItemStack.matches(baseItem, expectedBaseItem)) {
            return;
        }
        
        // Update expected item for future comparison
        expectedBaseItem = baseItem.copy();
        
        // Update BlockEntity with ONLY base slot item - true independence
        blockEntity.setBaseSlotItem(baseItem);
        
        // If slot is now empty, reset base texture with sync suppression
        if (baseItem.isEmpty()) {
            blockEntity.setSyncSuppressed(true);
            try {
                blockEntity.resetBaseTexture();
                this.baseTextureVariable = "all";
                blockEntity.setBaseTextureVariable("all");
            } finally {
                blockEntity.setSyncSuppressed(false);
            }
            return;
        }

        // Trigger dropdown analysis for new block
        analyzeBaseBlock(baseItem);
        
        LOGGER.debug("Base slot changed to: {}", baseItem.isEmpty() ? "empty" : baseItem.getItem().toString());
    }

    // ========================================
    // DROPDOWN-DRIVEN TEXTURE ANALYSIS
    // ========================================

    /**
     * TOGGLE CATEGORY: Analyze block and set intelligent default
     * Called only when toggle slot receives a new block
     */
    private void analyzeToggleBlock(@Nonnull ItemStack toggleItem) {
        // Suppress sync during analysis to prevent cross-contamination
        blockEntity.setSyncSuppressed(true);
        
        try {
            FaceSelectionData.RawTextureSelection selection = 
                FaceSelectionData.createRawTextureSelection(toggleItem, "all");

            if (selection.enabled() && !selection.availableVariables().isEmpty()) {
                // Block has multiple texture options - set intelligent default
                String defaultVariable = FaceSelectionData.getDefaultVariable(selection.availableVariables());
                this.toggleTextureVariable = defaultVariable;
                blockEntity.setToggleTextureVariable(defaultVariable);
                
                // Apply texture with default variable
                String texturePath = FaceSelectionData.getTextureForVariable(toggleItem, defaultVariable);
                if (texturePath != null) {
                    blockEntity.setToggleTexture(texturePath);
                }
                
                LOGGER.info("Toggle block analyzed - Default variable: {} from options: {}", 
                    defaultVariable, selection.availableVariables());
            } else {
                // Block has single texture - use "all"
                this.toggleTextureVariable = "all";
                blockEntity.setToggleTextureVariable("all");
                
                String texturePath = FaceSelectionData.getTextureForVariable(toggleItem, "all");
                if (texturePath != null) {
                    blockEntity.setToggleTexture(texturePath);
                }
                
                LOGGER.info("Toggle block analyzed - Single texture, using 'all'");
            }
        } finally {
            // Re-enable sync
            blockEntity.setSyncSuppressed(false);
        }
    }

    /**
     * BASE CATEGORY: Analyze block and set intelligent default
     * Called only when base slot receives a new block
     */
    private void analyzeBaseBlock(@Nonnull ItemStack baseItem) {
        // Suppress sync during analysis to prevent cross-contamination
        blockEntity.setSyncSuppressed(true);
        
        try {
            FaceSelectionData.RawTextureSelection selection = 
                FaceSelectionData.createRawTextureSelection(baseItem, "all");

            if (selection.enabled() && !selection.availableVariables().isEmpty()) {
                // Block has multiple texture options - set intelligent default
                String defaultVariable = FaceSelectionData.getDefaultVariable(selection.availableVariables());
                this.baseTextureVariable = defaultVariable;
                blockEntity.setBaseTextureVariable(defaultVariable);
                
                // Apply texture with default variable
                String texturePath = FaceSelectionData.getTextureForVariable(baseItem, defaultVariable);
                if (texturePath != null) {
                    blockEntity.setBaseTexture(texturePath);
                }
                
                LOGGER.info("Base block analyzed - Default variable: {} from options: {}", 
                    defaultVariable, selection.availableVariables());
            } else {
                // Block has single texture - use "all"
                this.baseTextureVariable = "all";
                blockEntity.setBaseTextureVariable("all");
                
                String texturePath = FaceSelectionData.getTextureForVariable(baseItem, "all");
                if (texturePath != null) {
                    blockEntity.setBaseTexture(texturePath);
                }
                
                LOGGER.info("Base block analyzed - Single texture, using 'all'");
            }
        } finally {
            // Re-enable sync
            blockEntity.setSyncSuppressed(false);
        }
    }

    // ========================================
    // DROPDOWN SELECTION HANDLERS
    // ========================================

    /**
     * TOGGLE CATEGORY: Handle dropdown selection change
     * Called by GUI when user selects different texture variable in toggle dropdown
     * NOW USES NETWORK PACKETS for client-server sync
     */
    public void setToggleTextureVariable(@Nonnull String variable) {
        if (blockEntity == null) {
            LOGGER.warn("Cannot set toggle texture variable - no BlockEntity");
            return;
        }

        ItemStack toggleItem = textureItemHandler.getStackInSlot(TOGGLE_TEXTURE_SLOT);
        if (toggleItem.isEmpty()) {
            LOGGER.warn("Cannot set toggle texture variable - no item in slot");
            return;
        }

        // Apply texture with selected variable
        String texturePath = FaceSelectionData.getTextureForVariable(toggleItem, variable);
        if (texturePath != null) {
            // Update local state immediately for responsive UI
            this.toggleTextureVariable = variable;
            
            if (level.isClientSide) {
                // Client side: Send network packet to server
                NetworkHandler.sendTextureVariableUpdate(blockPos, "toggle", variable, texturePath);
            } else {
                // Server side: Update directly (shouldn't happen in normal GUI usage)
                blockEntity.setToggleTextureVariable(variable);
                blockEntity.setToggleTexture(texturePath);
                forceBlockUpdate();
            }
            
            LOGGER.info("Toggle texture variable changed to: {} with texture: {}", variable, texturePath);
        } else {
            LOGGER.warn("No texture found for toggle variable: {}", variable);
        }
    }

    /**
     * BASE CATEGORY: Handle dropdown selection change
     * Called by GUI when user selects different texture variable in base dropdown
     * NOW USES NETWORK PACKETS for client-server sync
     */
    public void setBaseTextureVariable(@Nonnull String variable) {
        if (blockEntity == null) {
            LOGGER.warn("Cannot set base texture variable - no BlockEntity");
            return;
        }

        ItemStack baseItem = textureItemHandler.getStackInSlot(BASE_TEXTURE_SLOT);
        if (baseItem.isEmpty()) {
            LOGGER.warn("Cannot set base texture variable - no item in slot");
            return;
        }

        // Apply texture with selected variable
        String texturePath = FaceSelectionData.getTextureForVariable(baseItem, variable);
        if (texturePath != null) {
            // Update local state immediately for responsive UI
            this.baseTextureVariable = variable;
            
            if (level.isClientSide) {
                // Client side: Send network packet to server
                NetworkHandler.sendTextureVariableUpdate(blockPos, "base", variable, texturePath);
            } else {
                // Server side: Update directly (shouldn't happen in normal GUI usage)
                blockEntity.setBaseTextureVariable(variable);
                blockEntity.setBaseTexture(texturePath);
                forceBlockUpdate();
            }
            
            LOGGER.info("Base texture variable changed to: {} with texture: {}", variable, texturePath);
        } else {
            LOGGER.warn("No texture found for base variable: {}", variable);
        }
    }

    // ========================================
    // GUI STATE ACCESS (READ-ONLY)
    // ========================================

    /**
     * TOGGLE CATEGORY: Get current selection state for GUI rendering
     */
    @Nonnull
    public FaceSelectionData.RawTextureSelection getToggleTextureSelection() {
        ItemStack toggleItem = textureItemHandler.getStackInSlot(TOGGLE_TEXTURE_SLOT);
        
        // Sync local state with BlockEntity (BlockEntity is authoritative)
        if (blockEntity != null) {
            String blockEntityVariable = blockEntity.getToggleTextureVariable();
            if (!blockEntityVariable.equals(this.toggleTextureVariable)) {
                this.toggleTextureVariable = blockEntityVariable;
            }
        }
        
        return FaceSelectionData.createRawTextureSelection(toggleItem, toggleTextureVariable);
    }

    /**
     * BASE CATEGORY: Get current selection state for GUI rendering  
     */
    @Nonnull
    public FaceSelectionData.RawTextureSelection getBaseTextureSelection() {
        ItemStack baseItem = textureItemHandler.getStackInSlot(BASE_TEXTURE_SLOT);
        
        // Sync local state with BlockEntity (BlockEntity is authoritative)
        if (blockEntity != null) {
            String blockEntityVariable = blockEntity.getBaseTextureVariable();
            if (!blockEntityVariable.equals(this.baseTextureVariable)) {
                this.baseTextureVariable = blockEntityVariable;
            }
        }
        
        return FaceSelectionData.createRawTextureSelection(baseItem, baseTextureVariable);
    }

    // ========================================
    // POWER CATEGORY MANAGEMENT
    // ========================================

    /**
     * POWER CATEGORY: Handle power mode selection change
     * Called by GUI when user selects different power mode in dropdown
     * Uses network packets for client-server sync
     */
    public void setPowerMode(@Nonnull SwitchesLeverBlockEntity.PowerMode mode) {
        if (blockEntity == null) {
            LOGGER.warn("Cannot set power mode - no BlockEntity");
            return;
        }

        // Update local state immediately for responsive UI
        this.powerMode = mode;
        
        if (level.isClientSide) {
            // Client side: Send network packet to server
            NetworkHandler.sendTextureVariableUpdate(blockPos, "power", mode.name().toLowerCase(), "");
        } else {
            // Server side: Update directly (shouldn't happen in normal GUI usage)
            blockEntity.setPowerMode(mode);
            forceBlockUpdate();
        }
        
        LOGGER.info("Power mode changed to: {}", mode);
    }

    /**
     * POWER CATEGORY: Get current power mode for GUI rendering
     */
    @Nonnull
    public SwitchesLeverBlockEntity.PowerMode getPowerMode() {
        // Sync local state with BlockEntity (BlockEntity is authoritative)
        if (blockEntity != null) {
            SwitchesLeverBlockEntity.PowerMode blockEntityMode = blockEntity.getPowerMode();
            if (blockEntityMode != this.powerMode) {
                this.powerMode = blockEntityMode;
            }
        }
        
        return this.powerMode;
    }

    /**
     * Get unpowered texture preview based on current power mode
     */
    @Nonnull
    public String getUnpoweredTexturePreview() {
        if (blockEntity == null) return "";
        return blockEntity.getUnpoweredTexture();
    }

    /**
     * Get powered texture preview based on current power mode
     */
    @Nonnull
    public String getPoweredTexturePreview() {
        if (blockEntity == null) return "";
        return blockEntity.getPoweredTexture();
    }

    // ========================================
    // INITIALIZATION AND UTILITY METHODS
    // ========================================

    /**
     * Complete initialization after GUI is fully rendered.
     * Called by the GUI screen once all components are ready.
     * This allows slot change callbacks to work properly for user interactions.
     */
    public void completeInitialization() {
        // Ensure all protection flags are properly cleared
        this.isInitializing = false;
        this.isLoadingSlots = false;
        this.isSynchronizing = false;
        
        // Set expected items to current slot contents to enable user change detection
        expectedToggleItem = textureItemHandler.getStackInSlot(TOGGLE_TEXTURE_SLOT).copy();
        expectedBaseItem = textureItemHandler.getStackInSlot(BASE_TEXTURE_SLOT).copy();
    }

    /**
     * Initialize BlockEntity connection and sync texture variables
     */
    private void initializeBlockEntity() {
        this.blockEntity = getBlockEntity();
        if (blockEntity != null) {
            // Sync texture variables from BlockEntity (server is authoritative)
            this.baseTextureVariable = blockEntity.getBaseTextureVariable();
            this.toggleTextureVariable = blockEntity.getToggleTextureVariable();
            this.powerMode = blockEntity.getPowerMode();
        }
    }

    /**
     * Load slot items from BlockEntity persistent storage
     * Protected by isLoadingSlots flag to prevent analysis during loading
     */
    private void loadSlotItemsFromBlockEntity() {
        if (blockEntity != null) {
            // Set flag to prevent analysis during slot loading
            this.isLoadingSlots = true;
            
            try {
                ItemStack toggleItem = blockEntity.getGuiToggleItem();
                ItemStack baseItem = blockEntity.getGuiBaseItem();

                // Set expected items BEFORE loading to prevent sync detection triggering
                expectedToggleItem = toggleItem.copy();
                expectedBaseItem = baseItem.copy();
                
                textureItemHandler.setStackInSlot(TOGGLE_TEXTURE_SLOT, toggleItem);
                textureItemHandler.setStackInSlot(BASE_TEXTURE_SLOT, baseItem);
            } finally {
                // Clear flag after slot loading complete
                this.isLoadingSlots = false;
            }
        }
    }

    /**
     * Add player inventory slots with standard positioning
     */
    private void addPlayerInventorySlots(@Nonnull Inventory playerInventory) {
        // Player inventory (3x9 grid)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, PLAYER_INV_Y + row * 18));
            }
        }

        // Player hotbar (1x9 grid)
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, HOTBAR_Y));
        }
    }

    /**
     * Get BlockEntity for the menu's block position
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
     * Validate if an item stack is suitable for texture extraction
     */
    private boolean isValidTextureItem(@Nonnull ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }

        Block block = blockItem.getBlock();
        BlockState state = block.defaultBlockState();

        // Check for full block shape
        try {
            VoxelShape shape = state.getShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            var bounds = shape.bounds();
            if (bounds.minX > 0.001 || bounds.minY > 0.001 || bounds.minZ > 0.001 ||
                bounds.maxX < 0.999 || bounds.maxY < 0.999 || bounds.maxZ < 0.999) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }

        // Must be opaque
        if (!state.canOcclude()) {
            return false;
        }

        // Exclude problematic block types
        String blockName = block.getDescriptionId().toLowerCase();
        return !blockName.contains("stairs") && !blockName.contains("slab") &&
               !blockName.contains("fence") && !blockName.contains("door") &&
               !blockName.contains("redstone") && !blockName.contains("glass");
    }

    /**
     * Force block update to synchronize visual changes
     */
    private void forceBlockUpdate() {
        if (blockEntity != null && level != null && blockPos != null) {
            blockEntity.setChanged();
            blockEntity.requestModelDataUpdate();
            
            if (!level.isClientSide) {
                // Server side: Send update to clients
                level.sendBlockUpdated(blockPos, level.getBlockState(blockPos), level.getBlockState(blockPos), 3);
            }
        }
    }

    // ========================================
    // STANDARD MENU IMPLEMENTATIONS
    // ========================================

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

    @Override
    public void removed(@Nonnull Player player) {
        LOGGER.info("Clean architecture texture menu closed for player: {}", player.getDisplayName().getString());
        super.removed(player);
    }

    // ========================================
    // SIMPLE TEXTURE SLOT IMPLEMENTATION
    // ========================================

    /**
     * Simple texture slot with minimal logic and change callback
     */
    private class SimpleTextureSlot extends SlotItemHandler {
        private final Runnable onChangeCallback;

        public SimpleTextureSlot(ItemStackHandler itemHandler, int index, int xPosition, int yPosition, Runnable onChangeCallback) {
            super(itemHandler, index, xPosition, yPosition);
            this.onChangeCallback = onChangeCallback;
        }

        @Override
        public boolean mayPlace(@Nonnull ItemStack stack) {
            // Use outer class validation method
            return SwitchTextureMenu.this.isValidTextureItem(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1; // Only one block per texture slot
        }

        @Override
        public void setChanged() {
            super.setChanged();
            // Trigger independent category handler
            if (onChangeCallback != null) {
                onChangeCallback.run();
            }
        }
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
        return new FaceSelectionData.DropdownState(getBaseTextureSelection());
    }

    /**
     * @deprecated Use getToggleTextureSelection() instead
     */
    @Deprecated
    @Nonnull
    public FaceSelectionData.DropdownState getToggleDropdownState() {
        return new FaceSelectionData.DropdownState(getToggleTextureSelection());
    }
}