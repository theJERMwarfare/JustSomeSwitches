package net.justsomeswitches.gui;

import net.justsomeswitches.blockentity.SwitchBlockEntity;
import net.justsomeswitches.blockentity.tinting.FaceTintData;
import net.justsomeswitches.blockentity.tinting.OverlayLayer;
import net.justsomeswitches.network.NetworkHandler;
import net.justsomeswitches.util.DynamicBlockModelAnalyzer;
import net.justsomeswitches.util.SwitchesBlockValidator;
import net.justsomeswitches.util.TextureRotation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;


import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Texture customization menu for switches.
 */
public class SwitchesTextureMenu extends AbstractContainerMenu {

    private static final int TEXTURE_SLOT_COUNT = 2;
    private static final int TOGGLE_TEXTURE_SLOT = 0;
    private static final int BASE_TEXTURE_SLOT = 1;

    // GUI layout constants
    private static final int TOGGLE_SLOT_X = 13;
    private static final int TOGGLE_SLOT_Y = 29;
    private static final int BASE_SLOT_X = 147;
    private static final int BASE_SLOT_Y = 29;
    private static final int PLAYER_INV_Y = 98;
    private static final int HOTBAR_Y = 156;


    private final ItemStackHandler textureItemHandler;
    private final BlockPos blockPos;
    private final Level level;
    private SwitchBlockEntity blockEntity;


    private String baseTextureVariable = "all";
    private String toggleTextureVariable = "all";
    private SwitchBlockEntity.PowerMode powerMode = SwitchBlockEntity.PowerMode.DEFAULT;
    private TextureRotation baseTextureRotation = TextureRotation.NORMAL;
    private TextureRotation toggleTextureRotation = TextureRotation.NORMAL;

    private boolean isInitializing = true;

    private boolean isLoadingSlots = false;

    private boolean isSynchronizing = false;

    private ItemStack expectedToggleItem = ItemStack.EMPTY;
    private ItemStack expectedBaseItem = ItemStack.EMPTY;


    public SwitchesTextureMenu(int containerId, @Nonnull Inventory playerInventory, @Nullable BlockPos blockPos) {
        super(JustSomeSwitchesMenuTypes.SWITCH_TEXTURE_MENU.get(), containerId);

        this.blockPos = blockPos;
        this.level = playerInventory.player.level();


        initializeBlockEntity();


        this.textureItemHandler = new ItemStackHandler(TEXTURE_SLOT_COUNT) {
            @Override
            protected void onContentsChanged(int slot) {
                if (blockEntity != null) {
                    blockEntity.setChanged();
                }
            }
            
            @Override
            public void setStackInSlot(int slot, @Nonnull ItemStack stack) {
                if (!isInitializing && !isLoadingSlots) {
                    isSynchronizing = true;
                    try {
                        super.setStackInSlot(slot, stack);
                    } finally {
                        isSynchronizing = false;
                    }
                } else {
                    super.setStackInSlot(slot, stack);
                }
            }
        };


        loadSlotItemsFromBlockEntity();


        addSlot(new SimpleTextureSlot(textureItemHandler, TOGGLE_TEXTURE_SLOT, TOGGLE_SLOT_X, TOGGLE_SLOT_Y, this::onToggleSlotChanged));
        addSlot(new SimpleTextureSlot(textureItemHandler, BASE_TEXTURE_SLOT, BASE_SLOT_X, BASE_SLOT_Y, this::onBaseSlotChanged));


        addPlayerInventorySlots(playerInventory);
        
        if (!level.isClientSide) {
            this.isInitializing = false;
        }
    }


    /** Client-side constructor for network deserialization. */
    @SuppressWarnings("unused")
    public SwitchesTextureMenu(int containerId, @Nonnull Inventory playerInventory, @Nonnull FriendlyByteBuf extraData) {
        this(containerId, playerInventory, extraData.readBlockPos());
    }

    /** Toggle texture category handler. */
    private void onToggleSlotChanged() {
        if (blockEntity == null) return;
        if (isInitializing || isLoadingSlots || isSynchronizing) {
            return;
        }

        ItemStack toggleItem = textureItemHandler.getStackInSlot(TOGGLE_TEXTURE_SLOT);

        if (ItemStack.matches(toggleItem, expectedToggleItem)) {
            return;
        }

        expectedToggleItem = toggleItem.copy();

        blockEntity.setToggleSlotItem(toggleItem);

        if (toggleItem.isEmpty()) {
            blockEntity.setSyncSuppressed(true);
            try {
                blockEntity.resetToggleTexture();
                this.toggleTextureVariable = "all";
                blockEntity.setToggleTextureVariable("all");
                blockEntity.updateTextures();
            } finally {
                blockEntity.setSyncSuppressed(false);
            }
            return;
        }


        analyzeToggleBlock(toggleItem);
    }

    /** Base texture category handler. */
    private void onBaseSlotChanged() {
        if (blockEntity == null) return;

        if (isInitializing || isLoadingSlots || isSynchronizing) {
            return;
        }

        ItemStack baseItem = textureItemHandler.getStackInSlot(BASE_TEXTURE_SLOT);

        if (ItemStack.matches(baseItem, expectedBaseItem)) {
            return;
        }

        expectedBaseItem = baseItem.copy();

        blockEntity.setBaseSlotItem(baseItem);

        if (baseItem.isEmpty()) {
            blockEntity.setSyncSuppressed(true);
            try {
                blockEntity.resetBaseTexture();
                this.baseTextureVariable = "all";
                blockEntity.setBaseTextureVariable("all");
                blockEntity.updateTextures();
            } finally {
                blockEntity.setSyncSuppressed(false);
            }
            return;
        }


        analyzeBaseBlock(baseItem);
    }

    /** Analyzes toggle block and sets intelligent default. */
    private void analyzeToggleBlock(@Nonnull ItemStack toggleItem) {
        // Model analysis only works client-side (BakedModel APIs unavailable on server)
        if (!level.isClientSide) return;
        blockEntity.setSyncSuppressed(true);
        try {
            FaceSelectionData.RawTextureSelection selection = 
                FaceSelectionData.createRawTextureSelection(toggleItem, "all");
            // Always resolve actual variable name (e.g. "pattern" for terracotta, not "all")
            String resolvedVariable = FaceSelectionData.getDefaultVariable(selection.availableVariables());
            this.toggleTextureVariable = resolvedVariable;
            blockEntity.setToggleTextureVariable(resolvedVariable);
            String texturePath = FaceSelectionData.getTextureForVariable(toggleItem, resolvedVariable);
            if (texturePath != null) {
                blockEntity.setToggleTexture(texturePath);
                blockEntity.updateTextures();
            }
            analyzeTinting(toggleItem, true);
        } finally {
            blockEntity.setSyncSuppressed(false);
        }
        // Force model refresh so renderer picks up overlay/tint data before server sync
        blockEntity.requestModelDataUpdate();
        // Send resolved texture to server (server cannot analyze models)
        String resolvedPath = FaceSelectionData.getTextureForVariable(toggleItem, toggleTextureVariable);
        if (resolvedPath != null) {
            NetworkHandler.sendTextureVariableUpdate(blockPos, "toggle", toggleTextureVariable, resolvedPath);
        }
    }

    /** Analyzes base block and sets intelligent default. */
    private void analyzeBaseBlock(@Nonnull ItemStack baseItem) {
        // Model analysis only works client-side (BakedModel APIs unavailable on server)
        if (!level.isClientSide) return;
        blockEntity.setSyncSuppressed(true);
        try {
            FaceSelectionData.RawTextureSelection selection =
                FaceSelectionData.createRawTextureSelection(baseItem, "all");
            // Always resolve actual variable name (e.g. "pattern" for terracotta, not "all")
            String resolvedVariable = FaceSelectionData.getDefaultVariable(selection.availableVariables());
            this.baseTextureVariable = resolvedVariable;
            blockEntity.setBaseTextureVariable(resolvedVariable);
            String texturePath = FaceSelectionData.getTextureForVariable(baseItem, resolvedVariable);
            if (texturePath != null) {
                blockEntity.setBaseTexture(texturePath);
                blockEntity.updateTextures();
            }
            analyzeTinting(baseItem, false);
        } finally {
            blockEntity.setSyncSuppressed(false);
        }
        // Force model refresh so renderer picks up overlay/tint data before server sync
        blockEntity.requestModelDataUpdate();
        // Send resolved texture to server (server cannot analyze models)
        String texturePath = FaceSelectionData.getTextureForVariable(baseItem, baseTextureVariable);
        if (texturePath != null) {
            NetworkHandler.sendTextureVariableUpdate(blockPos, "base", baseTextureVariable, texturePath);
        }
    }

    /** Handles toggle dropdown selection change. */
    public void setToggleTextureVariable(@Nonnull String variable) {
        if (blockEntity == null) return;
        
        ItemStack toggleItem = textureItemHandler.getStackInSlot(TOGGLE_TEXTURE_SLOT);
        if (toggleItem.isEmpty()) return;

        String texturePath = FaceSelectionData.getTextureForVariable(toggleItem, variable);
        if (texturePath != null) {
            this.toggleTextureVariable = variable;
            sendOrApplyUpdate("toggle", variable, texturePath,
                    () -> {
                        blockEntity.setToggleTextureVariable(variable);
                        blockEntity.setToggleTexture(texturePath);
                        blockEntity.updateTextures();
                    });
        }
    }

    /** Handles base dropdown selection change. */
    public void setBaseTextureVariable(@Nonnull String variable) {
        if (blockEntity == null) return;
        
        ItemStack baseItem = textureItemHandler.getStackInSlot(BASE_TEXTURE_SLOT);
        if (baseItem.isEmpty()) return;

        String texturePath = FaceSelectionData.getTextureForVariable(baseItem, variable);
        if (texturePath != null) {
            this.baseTextureVariable = variable;
            sendOrApplyUpdate("base", variable, texturePath,
                    () -> {
                        blockEntity.setBaseTextureVariable(variable);
                        blockEntity.setBaseTexture(texturePath);
                        blockEntity.updateTextures();
                    });
        }
    }

    /** Returns current toggle selection state for GUI rendering. */
    @Nonnull
    public FaceSelectionData.RawTextureSelection getToggleTextureSelection() {
        ItemStack toggleItem = textureItemHandler.getStackInSlot(TOGGLE_TEXTURE_SLOT);
        syncLocalVariableFromBlockEntity(true);
        return FaceSelectionData.createRawTextureSelection(toggleItem, toggleTextureVariable);
    }

    /** Returns current base selection state for GUI rendering. */
    @Nonnull
    public FaceSelectionData.RawTextureSelection getBaseTextureSelection() {
        ItemStack baseItem = textureItemHandler.getStackInSlot(BASE_TEXTURE_SLOT);
        syncLocalVariableFromBlockEntity(false);
        return FaceSelectionData.createRawTextureSelection(baseItem, baseTextureVariable);
    }

    /** Analyzes tinting for all faces of a block and stores in the correct category on the block entity. */
    private void analyzeTinting(@Nonnull ItemStack item, boolean isToggle) {
        if (!(item.getItem() instanceof BlockItem blockItem)) {
            return;
        }
        Block block = blockItem.getBlock();
        BlockState blockState = block.defaultBlockState();
        // Store source block state for the correct category
        if (isToggle) {
            blockEntity.setToggleSourceBlockState(blockState);
        } else {
            blockEntity.setBaseSourceBlockState(blockState);
        }
        int tintedFaces = 0;
        int overlayFaces = 0;
        for (Direction direction : Direction.values()) {
            FaceTintData tintData = DynamicBlockModelAnalyzer.analyzeTinting(blockState, direction);
            if (isToggle) {
                blockEntity.setToggleTintData(direction, tintData);
            } else {
                blockEntity.setBaseTintData(direction, tintData);
            }
            if (tintData.getTintIndex() >= 0) {
                tintedFaces++;
            }
            java.util.List<OverlayLayer> overlayLayers = DynamicBlockModelAnalyzer.analyzeOverlays(blockState, direction);
            if (isToggle) {
                blockEntity.setToggleOverlayLayers(direction, overlayLayers);
            } else {
                blockEntity.setBaseOverlayLayers(direction, overlayLayers);
            }
            if (overlayLayers.size() > 1) {
                overlayFaces++;
            }
        }
        if (tintedFaces > 0) {
            net.justsomeswitches.JustSomeSwitchesMod.LOGGER.info(
                "Tinting detected: {} [{}] - {} tinted faces",
                block.getName().getString(), isToggle ? "toggle" : "base", tintedFaces
            );
        }
        if (overlayFaces > 0) {
            net.justsomeswitches.JustSomeSwitchesMod.LOGGER.info(
                "Overlay detected: {} [{}] - {} faces with multiple layers",
                block.getName().getString(), isToggle ? "toggle" : "base", overlayFaces
            );
        }
    }

    /** Handles power mode selection change. */
    public void setPowerMode(@Nonnull SwitchBlockEntity.PowerMode mode) {
        if (blockEntity == null) return;
        
        this.powerMode = mode;
        sendOrApplyUpdate("power", mode.name().toLowerCase(), "",
                () -> {
                    blockEntity.setPowerMode(mode);
                    blockEntity.updateTextures();
                });
    }

    /** Returns current power mode for GUI rendering. */
    @Nonnull
    public SwitchBlockEntity.PowerMode getPowerMode() {
        if (blockEntity != null) {
            this.powerMode = blockEntity.getPowerMode();
        }
        return this.powerMode;
    }

    /** Handles base texture rotation selection change. */
    public void setBaseTextureRotation(@Nonnull TextureRotation rotation) {
        if (blockEntity == null) return;
        
        this.baseTextureRotation = rotation;
        sendOrApplyUpdate("base_rotation", rotation.name().toLowerCase(), "",
                () -> {
                    blockEntity.setBaseTextureRotation(rotation);
                    blockEntity.updateTextures();
                });
    }

    /** Returns current base texture rotation for GUI rendering. */
    @Nonnull
    public TextureRotation getBaseTextureRotation() {
        if (blockEntity != null) {
            this.baseTextureRotation = blockEntity.getBaseTextureRotation();
        }
        return this.baseTextureRotation;
    }

    /** Handles toggle texture rotation selection change. */
    public void setToggleTextureRotation(@Nonnull TextureRotation rotation) {
        if (blockEntity == null) return;
        
        this.toggleTextureRotation = rotation;
        sendOrApplyUpdate("toggle_rotation", rotation.name().toLowerCase(), "",
                () -> {
                    blockEntity.setToggleTextureRotation(rotation);
                    blockEntity.updateTextures();
                });
    }

    /** Returns current toggle texture rotation for GUI rendering. */
    @Nonnull
    public TextureRotation getToggleTextureRotation() {
        if (blockEntity != null) {
            this.toggleTextureRotation = blockEntity.getToggleTextureRotation();
        }
        return this.toggleTextureRotation;
    }




    @Nonnull
    public String getUnpoweredTexturePreview() {
        if (blockEntity == null) return "";
        return blockEntity.getUnpoweredTexture();
    }


    @Nonnull
    public String getPoweredTexturePreview() {
        if (blockEntity == null) return "";
        return blockEntity.getPoweredTexture();
    }

    /** Completes initialization after GUI is fully rendered. */
    public void completeInitialization() {
        this.isInitializing = false;
        this.isLoadingSlots = false;
        this.isSynchronizing = false;

        expectedToggleItem = textureItemHandler.getStackInSlot(TOGGLE_TEXTURE_SLOT).copy();
        expectedBaseItem = textureItemHandler.getStackInSlot(BASE_TEXTURE_SLOT).copy();
    }

    /** Synchronizes local variables with BlockEntity state. */
    private void syncLocalVariableFromBlockEntity(boolean isToggle) {
        if (blockEntity == null) return;
        
        if (isToggle) {
            this.toggleTextureVariable = blockEntity.getToggleTextureVariable();
        } else {
            this.baseTextureVariable = blockEntity.getBaseTextureVariable();
        }
    }

    /** Unified method for sending network updates or applying changes directly. */
    private void sendOrApplyUpdate(String category, String variable, String texturePath, Runnable serverAction) {
        if (level.isClientSide) {
            NetworkHandler.sendTextureVariableUpdate(blockPos, category, variable, texturePath);
        } else {
            serverAction.run();
        }
    }


    private void initializeBlockEntity() {
        this.blockEntity = getBlockEntity();
        if (blockEntity != null) {

            this.baseTextureVariable = blockEntity.getBaseTextureVariable();
            this.toggleTextureVariable = blockEntity.getToggleTextureVariable();
            this.powerMode = blockEntity.getPowerMode();
            this.baseTextureRotation = blockEntity.getBaseTextureRotation();
            this.toggleTextureRotation = blockEntity.getToggleTextureRotation();
        }
    }

    /** Loads slot items from BlockEntity storage. */
    private void loadSlotItemsFromBlockEntity() {
        if (blockEntity != null) {

            this.isLoadingSlots = true;
            
            try {
                ItemStack toggleItem = blockEntity.getGuiToggleItem();
                ItemStack baseItem = blockEntity.getGuiBaseItem();


                expectedToggleItem = toggleItem.copy();
                expectedBaseItem = baseItem.copy();
                
                textureItemHandler.setStackInSlot(TOGGLE_TEXTURE_SLOT, toggleItem);
                textureItemHandler.setStackInSlot(BASE_TEXTURE_SLOT, baseItem);
            } finally {

                this.isLoadingSlots = false;
            }
        }
    }


    private void addPlayerInventorySlots(@Nonnull Inventory playerInventory) {

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, PLAYER_INV_Y + row * 18));
            }
        }


        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, HOTBAR_Y));
        }
    }


    @Nullable
    private SwitchBlockEntity getBlockEntity() {
        if (blockPos != null && level != null) {
            var blockEntity = level.getBlockEntity(blockPos);
            if (blockEntity instanceof SwitchBlockEntity switchBlockEntity) {
                return switchBlockEntity;
            }
        }
        return null;
    }


    private boolean isValidTextureItem(@Nonnull ItemStack stack) {
        return SwitchesBlockValidator.isValidTextureItem(stack);
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
            }

            else {

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
    public void clicked(int slotId, int button, @Nonnull net.minecraft.world.inventory.ClickType clickType, @Nonnull Player player) {

        if (slotId >= 0 && slotId < TEXTURE_SLOT_COUNT && clickType == net.minecraft.world.inventory.ClickType.PICKUP) {
            Slot slot = this.slots.get(slotId);
            ItemStack carriedItem = getCarried();
            
            if (!carriedItem.isEmpty() && slot instanceof SimpleTextureSlot textureSlot) {
                boolean slotOccupied = slot.hasItem();
                boolean playerHasStack = carriedItem.getCount() > 1;
                
                if (slotOccupied && playerHasStack) {
                    return;
                }
                
                if (!slotOccupied && textureSlot.mayPlace(carriedItem)) {
                    ItemStack singleItem = carriedItem.copy();
                    singleItem.setCount(1);
                    slot.set(singleItem);

                    carriedItem.shrink(1);
                    if (carriedItem.isEmpty()) {
                        setCarried(ItemStack.EMPTY);
                    }
                    
                    slot.setChanged();
                    return;
                }
            }
        }

        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public boolean stillValid(@Nonnull Player player) {
        if (blockPos == null || level == null) {
            return false;
        }


        if (!(level.getBlockState(blockPos).getBlock() instanceof net.justsomeswitches.block.ISwitchBlock)) {
            return false;
        }


        return player.distanceToSqr(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void removed(@Nonnull Player player) {

        super.removed(player);
    }

    /** Simple texture slot with change callback. */
    private class SimpleTextureSlot extends SlotItemHandler {
        private final Runnable onChangeCallback;

        public SimpleTextureSlot(ItemStackHandler itemHandler, int index, int xPosition, int yPosition, Runnable onChangeCallback) {
            super(itemHandler, index, xPosition, yPosition);
            this.onChangeCallback = onChangeCallback;
        }

        @Override
        public boolean mayPlace(@Nonnull ItemStack stack) {

            return SwitchesTextureMenu.this.isValidTextureItem(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
        
        @Override
        @Nonnull
        public ItemStack safeInsert(@Nonnull ItemStack stack, int amount) {

            if (stack.isEmpty() || amount <= 0) {
                return stack;
            }

            if (this.hasItem()) {
                return stack;
            }

            ItemStack toInsert = stack.copy();
            toInsert.setCount(1);
            
            if (this.mayPlace(toInsert)) {
                this.set(toInsert);

                ItemStack remaining = stack.copy();
                remaining.shrink(1);
                return remaining.isEmpty() ? ItemStack.EMPTY : remaining;
            }
            
            return stack;
        }
        
        @Override
        public void setByPlayer(@Nonnull ItemStack stack) {

            if (stack.isEmpty()) {

                this.set(ItemStack.EMPTY);
                return;
            }
            
            if (!this.mayPlace(stack)) {
                return;
            }
            
            boolean slotOccupied = this.hasItem();
            boolean playerHasStack = stack.getCount() > 1;
            
            if (slotOccupied && playerHasStack) {
                return;
            }
            
            if (slotOccupied) {
                // Slot occupied but player has single item - replace it
                this.set(stack.copy());
            } else {
                // Slot empty - place single item
                ItemStack singleItem = stack.copy();
                singleItem.setCount(1);
                this.set(singleItem);
            }
        }
        
        @Override
        @Nonnull
        public ItemStack safeTake(int amount, int decrement, @Nonnull Player player) {

            if (!this.hasItem()) {
                return ItemStack.EMPTY;
            }
            
            ItemStack currentItem = this.getItem();
            this.set(ItemStack.EMPTY);
            return currentItem;
        }
        
        @Override
        public boolean mayPickup(@Nonnull Player player) {

            return true;
        }
        
        @Override
        public void onTake(@Nonnull Player player, @Nonnull ItemStack stack) {

            super.onTake(player, stack);
        }

        @Override
        public void setChanged() {
            super.setChanged();

            if (onChangeCallback != null) {
                onChangeCallback.run();
            }
        }
    }

    /** Returns block position for this menu. */
    @Nullable
    public BlockPos getBlockPos() {
        return blockPos;
    }

    /** Returns level for this menu. */
    @Nullable
    public Level getLevel() {
        return level;
    }


}