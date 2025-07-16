package net.justsomeswitches.blockentity;

import net.justsomeswitches.gui.FaceSelectionData;
import net.justsomeswitches.init.JustSomeSwitchesModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Switches Lever BlockEntity with Raw JSON Variable System - FIXED VERSION
 * REWRITTEN: Uses string-based face selections with universal block compatibility
 * 
 * FIXES APPLIED:
 * - Enhanced face selection preservation during blockstate changes
 * - Improved NBT loading during preservation periods
 * - Better synchronization of face selections
 */
public class SwitchesLeverBlockEntity extends BlockEntity {

    // ========================================
    // TEXTURE CONFIGURATION
    // ========================================

    // Default texture paths
    public static final String DEFAULT_BASE_TEXTURE = "minecraft:block/stone";
    public static final String DEFAULT_TOGGLE_TEXTURE = "minecraft:block/oak_planks";

    // NBT keys for texture storage
    private static final String BASE_TEXTURE_KEY = "base_texture_path";
    private static final String TOGGLE_TEXTURE_KEY = "toggle_texture_path";

    // NBT keys for face selection storage (now strings)
    private static final String BASE_VARIABLE_KEY = "base_texture_variable";
    private static final String TOGGLE_VARIABLE_KEY = "toggle_texture_variable";

    // NBT key for inversion state
    private static final String INVERTED_KEY = "inverted_state";

    // Current texture configuration
    private String baseTexturePath = DEFAULT_BASE_TEXTURE;
    private String toggleTexturePath = DEFAULT_TOGGLE_TEXTURE;

    // Raw face selections (JSON variable names)
    private String baseTextureVariable = "all";
    private String toggleTextureVariable = "all";

    // Inversion state
    private boolean inverted = false;

    // GUI slot storage
    private ItemStack guiToggleItem = ItemStack.EMPTY;
    private ItemStack guiBaseItem = ItemStack.EMPTY;

    // ========================================
    // ENHANCED FACE SELECTION PRESERVATION SYSTEM
    // ========================================

    // Temporary storage for preserving selections during state changes
    private String preservedBaseVariable = null;
    private String preservedToggleVariable = null;
    private boolean preservedInverted = false;
    private boolean preservationActive = false;
    
    // CRITICAL FIX: Track if we're in the middle of a blockstate change
    private boolean isInBlockStateChange = false;

    /**
     * FIXED: Preserve face selections before block state changes
     * Enhanced to handle multiple rapid state changes
     */
    public void preserveFaceSelectionsForStateChange() {
        if (!preservationActive) {
            this.preservedBaseVariable = this.baseTextureVariable;
            this.preservedToggleVariable = this.toggleTextureVariable;
            this.preservedInverted = this.inverted;
            this.preservationActive = true;
            this.isInBlockStateChange = true;
        }
    }

    /**
     * FIXED: Restore preserved face selections after state changes
     * Enhanced with better state management
     */
    private void restorePreservedFaceSelections() {
        if (preservationActive && preservedBaseVariable != null && preservedToggleVariable != null) {
            // Restore preserved values
            this.baseTextureVariable = preservedBaseVariable;
            this.toggleTextureVariable = preservedToggleVariable;
            this.inverted = preservedInverted;

            // Clear preservation state
            this.preservedBaseVariable = null;
            this.preservedToggleVariable = null;
            this.preservedInverted = false;
            this.preservationActive = false;
            this.isInBlockStateChange = false;

            // Force immediate save and update
            setChanged();
            
            // Update model data on client side
            if (level != null && level.isClientSide) {
                requestModelDataUpdate();
            }
        }
    }

    /**
     * CRITICAL FIX: Force restoration of preserved face selections
     * Called from the block when state changes are complete
     */
    public void forceRestorePreservedSelections() {
        if (preservationActive) {
            restorePreservedFaceSelections();
        }
    }

    // ========================================
    // ENHANCED MODEL DATA INTEGRATION
    // ========================================

    /**
     * ModelProperty for passing texture data to custom models
     */
    public static final ModelProperty<SwitchTextureData> TEXTURE_PROPERTY = new ModelProperty<>();

    /**
     * Enhanced data class for passing texture information to custom models
     */
    public static class SwitchTextureData {
        private final String baseTexture;
        private final String toggleTexture;
        private final String baseVariable;
        private final String toggleVariable;
        private final boolean inverted;

        public SwitchTextureData(String baseTexture, String toggleTexture,
                                 String baseVariable, String toggleVariable,
                                 boolean inverted) {
            this.baseTexture = baseTexture;
            this.toggleTexture = toggleTexture;
            this.baseVariable = baseVariable;
            this.toggleVariable = toggleVariable;
            this.inverted = inverted;
        }

        public String getBaseTexture() { return baseTexture; }
        public String getToggleTexture() { return toggleTexture; }
        public String getBaseVariable() { return baseVariable; }
        public String getToggleVariable() { return toggleVariable; }
        public boolean isInverted() { return inverted; }

        /**
         * Check if using custom textures (different from defaults)
         */
        public boolean hasCustomTextures() {
            return !baseTexture.equals(DEFAULT_BASE_TEXTURE) ||
                    !toggleTexture.equals(DEFAULT_TOGGLE_TEXTURE) ||
                    !baseVariable.equals("all") ||
                    !toggleVariable.equals("all") ||
                    inverted;
        }
    }

    /**
     * Get ModelData for custom model rendering
     */
    @Override
    @Nonnull
    public ModelData getModelData() {
        return ModelData.builder()
                .with(TEXTURE_PROPERTY, new SwitchTextureData(
                        baseTexturePath, toggleTexturePath,
                        baseTextureVariable, toggleTextureVariable, inverted))
                .build();
    }

    // ========================================
    // CONSTRUCTOR AND BASIC SETUP
    // ========================================

    public SwitchesLeverBlockEntity(BlockPos pos, BlockState blockState) {
        super(JustSomeSwitchesModBlockEntities.SWITCHES_LEVER.get(), pos, blockState);
    }

    // ========================================
    // ENHANCED BLOCK STATE MANAGEMENT WITH PRESERVATION
    // ========================================

    /**
     * FIXED: Override setBlockState to preserve face selections
     * Enhanced to handle rapid state changes better
     */
    @Override
    public void setBlockState(@Nonnull BlockState blockState) {
        // Preserve current face selections before state change
        if (!preservationActive && !isInBlockStateChange) {
            preserveFaceSelectionsForStateChange();
        }

        super.setBlockState(blockState);

        // Restore face selections after a brief delay to ensure state change completes
        if (level != null && !level.isClientSide) {
            level.scheduleTick(worldPosition, blockState.getBlock(), 1);
        }

        // Force model data update
        if (level != null && !level.isClientSide) {
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, blockState, blockState, Block.UPDATE_CLIENTS);
        }
    }

    // ========================================
    // CLIENT AND SERVER TICK METHODS
    // ========================================

    public static void clientTick(Level level, BlockPos pos, BlockState state, SwitchesLeverBlockEntity blockEntity) {
        // Restore preserved selections if needed
        if (blockEntity.preservationActive && blockEntity.isInBlockStateChange) {
            blockEntity.restorePreservedFaceSelections();
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SwitchesLeverBlockEntity blockEntity) {
        // Restore preserved selections if needed
        if (blockEntity.preservationActive && blockEntity.isInBlockStateChange) {
            blockEntity.restorePreservedFaceSelections();
        }
    }

    // ========================================
    // TEXTURE MANAGEMENT
    // ========================================

    /**
     * Extract texture path from ItemStack
     */
    @Nonnull
    private String getTextureFromItem(@Nonnull ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return "";
        }

        Item item = itemStack.getItem();
        if (!(item instanceof BlockItem blockItem)) {
            return "";
        }

        Block block = blockItem.getBlock();
        try {
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
            if (blockId != null) {
                return blockId.getNamespace() + ":block/" + blockId.getPath();
            }
        } catch (Exception e) {
            // Silent failure
        }

        return "";
    }

    /**
     * Mark BlockEntity as dirty and trigger client synchronization
     */
    private void markDirtyAndSync() {
        if (level != null && !level.isClientSide) {
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            requestModelDataUpdate();
        }
    }

    /**
     * Set the base texture for the switch
     */
    public boolean setBaseTexture(@Nonnull String texturePath) {
        if (!texturePath.equals(this.baseTexturePath)) {
            this.baseTexturePath = texturePath;
            markDirtyAndSync();
            return true;
        }
        return false;
    }

    /**
     * Set the base texture from ItemStack
     */
    public boolean setBaseTexture(@Nonnull ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return setBaseTexture(DEFAULT_BASE_TEXTURE);
        }
        String texturePath = getTextureFromItem(itemStack);
        return texturePath.isEmpty() ? false : setBaseTexture(texturePath);
    }

    /**
     * Set the toggle texture for the switch
     */
    public boolean setToggleTexture(@Nonnull String texturePath) {
        if (!texturePath.equals(this.toggleTexturePath)) {
            this.toggleTexturePath = texturePath;
            markDirtyAndSync();
            return true;
        }
        return false;
    }

    /**
     * Set the toggle texture from ItemStack
     */
    public boolean setToggleTexture(@Nonnull ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return setToggleTexture(DEFAULT_TOGGLE_TEXTURE);
        }
        String texturePath = getTextureFromItem(itemStack);
        return texturePath.isEmpty() ? false : setToggleTexture(texturePath);
    }

    // ========================================
    // RAW TEXTURE VARIABLE MANAGEMENT
    // ========================================

    /**
     * Set base texture variable (raw JSON variable name)
     */
    public boolean setBaseTextureVariable(@Nonnull String variable) {
        if (!variable.equals(this.baseTextureVariable)) {
            this.baseTextureVariable = variable;
            markDirtyAndSync();
            return true;
        }
        return false;
    }

    /**
     * Set toggle texture variable (raw JSON variable name)
     */
    public boolean setToggleTextureVariable(@Nonnull String variable) {
        if (!variable.equals(this.toggleTextureVariable)) {
            this.toggleTextureVariable = variable;
            markDirtyAndSync();
            return true;
        }
        return false;
    }

    /**
     * Set inversion state
     */
    public boolean setInverted(boolean inverted) {
        if (this.inverted != inverted) {
            this.inverted = inverted;
            markDirtyAndSync();
            return true;
        }
        return false;
    }

    // ========================================
    // RAW TEXTURE SELECTION INTEGRATION
    // ========================================

    /**
     * Get raw texture selection for base texture slot
     */
    @Nonnull
    public FaceSelectionData.RawTextureSelection getBaseTextureSelection() {
        return FaceSelectionData.createRawTextureSelection(guiBaseItem, baseTextureVariable);
    }

    /**
     * Get raw texture selection for toggle texture slot
     */
    @Nonnull
    public FaceSelectionData.RawTextureSelection getToggleTextureSelection() {
        return FaceSelectionData.createRawTextureSelection(guiToggleItem, toggleTextureVariable);
    }

    // ========================================
    // GETTERS FOR GUI AND MODEL INTEGRATION
    // ========================================

    @Nonnull public String getBaseTexture() { return baseTexturePath; }
    @Nonnull public String getToggleTexture() { return toggleTexturePath; }
    @Nonnull public String getBaseTextureVariable() { return baseTextureVariable; }
    @Nonnull public String getToggleTextureVariable() { return toggleTextureVariable; }
    public boolean isInverted() { return inverted; }

    /**
     * Reset all textures and settings to defaults
     */
    public void resetTextures() {
        boolean changed = false;

        if (!baseTexturePath.equals(DEFAULT_BASE_TEXTURE)) {
            this.baseTexturePath = DEFAULT_BASE_TEXTURE;
            changed = true;
        }

        if (!toggleTexturePath.equals(DEFAULT_TOGGLE_TEXTURE)) {
            this.toggleTexturePath = DEFAULT_TOGGLE_TEXTURE;
            changed = true;
        }

        if (!baseTextureVariable.equals("all")) {
            this.baseTextureVariable = "all";
            changed = true;
        }

        if (!toggleTextureVariable.equals("all")) {
            this.toggleTextureVariable = "all";
            changed = true;
        }

        if (inverted) {
            this.inverted = false;
            changed = true;
        }

        if (changed) {
            markDirtyAndSync();
        }
    }

    /**
     * Reset base texture to default
     */
    public void resetBaseTexture() {
        setBaseTexture(DEFAULT_BASE_TEXTURE);
        setBaseTextureVariable("all");
    }

    /**
     * Reset toggle texture to default
     */
    public void resetToggleTexture() {
        setToggleTexture(DEFAULT_TOGGLE_TEXTURE);
        setToggleTextureVariable("all");
    }

    /**
     * Check if using custom textures
     */
    public boolean hasCustomTextures() {
        return !baseTexturePath.equals(DEFAULT_BASE_TEXTURE) ||
                !toggleTexturePath.equals(DEFAULT_TOGGLE_TEXTURE) ||
                !baseTextureVariable.equals("all") ||
                !toggleTextureVariable.equals("all") ||
                inverted;
    }

    // ========================================
    // GUI SLOT MANAGEMENT
    // ========================================

    @Nonnull public ItemStack getGuiToggleItem() { return guiToggleItem; }
    @Nonnull public ItemStack getGuiBaseItem() { return guiBaseItem; }

    /**
     * Set GUI slot items
     */
    public void setGuiSlotItems(@Nonnull ItemStack toggleItem, @Nonnull ItemStack baseItem) {
        this.guiToggleItem = toggleItem.copy();
        this.guiBaseItem = baseItem.copy();
    }

    /**
     * Drop stored texture blocks when switch is broken
     */
    public void dropStoredTextures(@Nonnull Level level, @Nonnull BlockPos pos) {
        if (!guiToggleItem.isEmpty()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), guiToggleItem);
        }

        if (!guiBaseItem.isEmpty()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), guiBaseItem);
        }
    }

    // ========================================
    // ENHANCED NBT SERIALIZATION (STRING-BASED)
    // ========================================

    @Override
    protected void saveAdditional(@Nonnull CompoundTag nbt) {
        super.saveAdditional(nbt);

        // Save texture paths
        nbt.putString(BASE_TEXTURE_KEY, baseTexturePath);
        nbt.putString(TOGGLE_TEXTURE_KEY, toggleTexturePath);

        // Save raw texture variables
        nbt.putString(BASE_VARIABLE_KEY, baseTextureVariable);
        nbt.putString(TOGGLE_VARIABLE_KEY, toggleTextureVariable);

        // Save inversion state
        nbt.putBoolean(INVERTED_KEY, inverted);

        // Save GUI slot items
        if (!guiToggleItem.isEmpty()) {
            nbt.put("gui_toggle_item", guiToggleItem.save(new CompoundTag()));
        }
        if (!guiBaseItem.isEmpty()) {
            nbt.put("gui_base_item", guiBaseItem.save(new CompoundTag()));
        }

        // CRITICAL FIX: Save preservation state
        if (preservationActive) {
            nbt.putBoolean("preservation_active", true);
            if (preservedBaseVariable != null) {
                nbt.putString("preserved_base_variable", preservedBaseVariable);
            }
            if (preservedToggleVariable != null) {
                nbt.putString("preserved_toggle_variable", preservedToggleVariable);
            }
            nbt.putBoolean("preserved_inverted", preservedInverted);
        }
    }

    @Override
    public void load(@Nonnull CompoundTag nbt) {
        super.load(nbt);

        // Load texture paths with defaults
        this.baseTexturePath = nbt.getString(BASE_TEXTURE_KEY);
        this.toggleTexturePath = nbt.getString(TOGGLE_TEXTURE_KEY);

        // Validate loaded paths
        if (this.baseTexturePath.isEmpty()) {
            this.baseTexturePath = DEFAULT_BASE_TEXTURE;
        }
        if (this.toggleTexturePath.isEmpty()) {
            this.toggleTexturePath = DEFAULT_TOGGLE_TEXTURE;
        }

        // CRITICAL FIX: Enhanced preservation handling during NBT loading
        boolean hadPreservationData = nbt.getBoolean("preservation_active");
        
        if (hadPreservationData) {
            // Restore preservation state
            this.preservationActive = true;
            this.preservedBaseVariable = nbt.getString("preserved_base_variable");
            this.preservedToggleVariable = nbt.getString("preserved_toggle_variable");
            this.preservedInverted = nbt.getBoolean("preserved_inverted");
            
            // Use preserved values if available
            if (!preservedBaseVariable.isEmpty()) {
                this.baseTextureVariable = preservedBaseVariable;
            } else {
                this.baseTextureVariable = nbt.getString(BASE_VARIABLE_KEY);
            }
            
            if (!preservedToggleVariable.isEmpty()) {
                this.toggleTextureVariable = preservedToggleVariable;
            } else {
                this.toggleTextureVariable = nbt.getString(TOGGLE_VARIABLE_KEY);
            }
            
            this.inverted = preservedInverted;
            
            // Clear preservation after loading
            this.preservationActive = false;
            this.preservedBaseVariable = null;
            this.preservedToggleVariable = null;
            this.preservedInverted = false;
            
        } else {
            // Normal loading without preservation
            this.baseTextureVariable = nbt.getString(BASE_VARIABLE_KEY);
            this.toggleTextureVariable = nbt.getString(TOGGLE_VARIABLE_KEY);
            this.inverted = nbt.getBoolean(INVERTED_KEY);
        }

        // Validate loaded variables
        if (this.baseTextureVariable.isEmpty()) {
            this.baseTextureVariable = "all";
        }
        if (this.toggleTextureVariable.isEmpty()) {
            this.toggleTextureVariable = "all";
        }

        // Load GUI slot items
        if (nbt.contains("gui_toggle_item")) {
            this.guiToggleItem = ItemStack.of(nbt.getCompound("gui_toggle_item"));
        } else {
            this.guiToggleItem = ItemStack.EMPTY;
        }

        if (nbt.contains("gui_base_item")) {
            this.guiBaseItem = ItemStack.of(nbt.getCompound("gui_base_item"));
        } else {
            this.guiBaseItem = ItemStack.EMPTY;
        }
    }

    // ========================================
    // CLIENT-SERVER SYNCHRONIZATION
    // ========================================

    @Override
    @Nullable
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    @Nonnull
    public CompoundTag getUpdateTag() {
        CompoundTag nbt = super.getUpdateTag();

        // Include all data for client sync
        nbt.putString(BASE_TEXTURE_KEY, baseTexturePath);
        nbt.putString(TOGGLE_TEXTURE_KEY, toggleTexturePath);
        nbt.putString(BASE_VARIABLE_KEY, baseTextureVariable);
        nbt.putString(TOGGLE_VARIABLE_KEY, toggleTextureVariable);
        nbt.putBoolean(INVERTED_KEY, inverted);

        // Sync GUI slot items
        if (!guiToggleItem.isEmpty()) {
            nbt.put("gui_toggle_item", guiToggleItem.save(new CompoundTag()));
        }
        if (!guiBaseItem.isEmpty()) {
            nbt.put("gui_base_item", guiBaseItem.save(new CompoundTag()));
        }

        return nbt;
    }

    @Override
    public void onDataPacket(@Nonnull net.minecraft.network.Connection net, @Nonnull ClientboundBlockEntityDataPacket pkt) {
        CompoundTag nbt = pkt.getTag();
        if (nbt != null) {
            load(nbt);
            requestModelDataUpdate();
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
        FaceSelectionData.RawTextureSelection rawSelection = getBaseTextureSelection();
        return new FaceSelectionData.DropdownState(rawSelection);
    }

    /**
     * @deprecated Use getToggleTextureSelection() instead
     */
    @Deprecated
    @Nonnull
    public FaceSelectionData.DropdownState getToggleDropdownState() {
        FaceSelectionData.RawTextureSelection rawSelection = getToggleTextureSelection();
        return new FaceSelectionData.DropdownState(rawSelection);
    }
}