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
 * Switches Lever BlockEntity with Raw JSON Variable System - MINIMAL FIX VERSION
 * CRITICAL FIX: Prevents NBT corruption during blockstate changes without complex preservation
 *
 * APPROACH: Protect NBT data during blockstate changes using minimal, targeted intervention
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

    // Current texture configuration
    private String baseTexturePath = DEFAULT_BASE_TEXTURE;
    private String toggleTexturePath = DEFAULT_TOGGLE_TEXTURE;

    // Raw face selections (JSON variable names)
    private String baseTextureVariable = "all";
    private String toggleTextureVariable = "all";

    // GUI slot storage
    private ItemStack guiToggleItem = ItemStack.EMPTY;
    private ItemStack guiBaseItem = ItemStack.EMPTY;

    // ========================================
    // MINIMAL BLOCKSTATE PROTECTION SYSTEM
    // ========================================

    // CRITICAL FIX: Track if we're in a blockstate change to prevent NBT corruption
    private boolean isInBlockStateChange = false;
    private boolean skipNextNBTLoad = false;

    /**
     * MINIMAL FIX: Mark start of blockstate change to protect NBT data
     */
    public void protectNBTDuringStateChange() {
        this.isInBlockStateChange = true;
        this.skipNextNBTLoad = true;
    }

    /**
     * MINIMAL FIX: Mark end of blockstate change and restore normal NBT processing
     */
    public void endNBTProtection() {
        this.isInBlockStateChange = false;
        this.skipNextNBTLoad = false;
    }

    // ========================================
    // MODEL DATA INTEGRATION
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

        public SwitchTextureData(String baseTexture, String toggleTexture,
                                 String baseVariable, String toggleVariable) {
            this.baseTexture = baseTexture;
            this.toggleTexture = toggleTexture;
            this.baseVariable = baseVariable;
            this.toggleVariable = toggleVariable;
        }

        public String getBaseTexture() { return baseTexture; }
        public String getToggleTexture() { return toggleTexture; }
        public String getBaseVariable() { return baseVariable; }
        public String getToggleVariable() { return toggleVariable; }

        /**
         * Check if using custom textures (different from defaults)
         */
        public boolean hasCustomTextures() {
            return !baseTexture.equals(DEFAULT_BASE_TEXTURE) ||
                    !toggleTexture.equals(DEFAULT_TOGGLE_TEXTURE) ||
                    !baseVariable.equals("all") ||
                    !toggleVariable.equals("all");
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
                        baseTextureVariable, toggleTextureVariable))
                .build();
    }

    // ========================================
    // CONSTRUCTOR AND BASIC SETUP
    // ========================================

    public SwitchesLeverBlockEntity(BlockPos pos, BlockState blockState) {
        super(JustSomeSwitchesModBlockEntities.SWITCHES_LEVER.get(), pos, blockState);
    }

    // ========================================
    // MINIMAL BLOCKSTATE OVERRIDE PROTECTION
    // ========================================

    /**
     * MINIMAL FIX: Override setBlockState to protect NBT data
     */
    @Override
    public void setBlockState(@Nonnull BlockState blockState) {
        // Mark that we're in a blockstate change
        protectNBTDuringStateChange();

        super.setBlockState(blockState);

        // Schedule NBT protection end after blockstate change completes
        if (level != null && !level.isClientSide) {
            level.scheduleTick(worldPosition, blockState.getBlock(), 1);
        }

        // Force model data update
        if (level != null) {
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, blockState, blockState, Block.UPDATE_CLIENTS);
        }
    }

    // ========================================
    // TICK METHODS FOR NBT PROTECTION CLEANUP
    // ========================================

    public static void clientTick(Level level, BlockPos pos, BlockState state, SwitchesLeverBlockEntity blockEntity) {
        // Clean up NBT protection if active
        if (blockEntity.isInBlockStateChange) {
            blockEntity.endNBTProtection();
            blockEntity.requestModelDataUpdate();
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SwitchesLeverBlockEntity blockEntity) {
        // Clean up NBT protection if active
        if (blockEntity.isInBlockStateChange) {
            blockEntity.endNBTProtection();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
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
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
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
        return !texturePath.isEmpty() && setBaseTexture(texturePath);
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
        return !texturePath.isEmpty() && setToggleTexture(texturePath);
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
                !toggleTextureVariable.equals("all");
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
    // ENHANCED NBT SERIALIZATION WITH PROTECTION
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

        // Save GUI slot items
        if (!guiToggleItem.isEmpty()) {
            nbt.put("gui_toggle_item", guiToggleItem.save(new CompoundTag()));
        }
        if (!guiBaseItem.isEmpty()) {
            nbt.put("gui_base_item", guiBaseItem.save(new CompoundTag()));
        }
    }

    @Override
    public void load(@Nonnull CompoundTag nbt) {
        super.load(nbt);

        // CRITICAL FIX: Skip NBT loading during blockstate changes to prevent corruption
        if (skipNextNBTLoad) {
            skipNextNBTLoad = false;
            return;
        }

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

        // Load raw texture variables
        this.baseTextureVariable = nbt.getString(BASE_VARIABLE_KEY);
        this.toggleTextureVariable = nbt.getString(TOGGLE_VARIABLE_KEY);

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
