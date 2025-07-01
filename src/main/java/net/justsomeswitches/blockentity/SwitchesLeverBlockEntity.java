package net.justsomeswitches.blockentity;

import net.justsomeswitches.gui.FaceSelectionData;
import net.justsomeswitches.init.JustSomeSwitchesModBlockEntities;
import net.justsomeswitches.util.BlockTextureAnalyzer;
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
 * Enhanced Block Entity for Switches Lever - Auto-Apply System with Face Selection Persistence
 * ---
 * FIXED: Face selection persistence through block state changes and NBT operations
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

    // NBT keys for face selection storage
    private static final String BASE_FACE_KEY = "base_face_selection";
    private static final String TOGGLE_FACE_KEY = "toggle_face_selection";

    // NBT keys for inversion state
    private static final String INVERTED_KEY = "inverted_state";

    // Current texture paths
    private String baseTexturePath = DEFAULT_BASE_TEXTURE;
    private String toggleTexturePath = DEFAULT_TOGGLE_TEXTURE;

    // Face selections with proper initialization
    private FaceSelectionData.FaceOption baseFaceSelection = FaceSelectionData.FaceOption.ALL;
    private FaceSelectionData.FaceOption toggleFaceSelection = FaceSelectionData.FaceOption.ALL;

    // Inversion state
    private boolean inverted = false;

    // GUI slot storage
    private ItemStack guiToggleItem = ItemStack.EMPTY;
    private ItemStack guiBaseItem = ItemStack.EMPTY;

    // ========================================
    // FIXED: FACE SELECTION PRESERVATION SYSTEM
    // ========================================

    // Temporary storage for preserving face selections during state changes
    private FaceSelectionData.FaceOption preservedBaseFace = null;
    private FaceSelectionData.FaceOption preservedToggleFace = null;
    private boolean preservedInverted = false;
    private boolean preservationActive = false;

    /**
     * FIXED: Preserve face selections before block state changes
     * Called before any operation that might trigger NBT reload
     */
    public void preserveFaceSelectionsForStateChange() {
        System.out.println("DEBUG BlockEntity: Preserving face selections before state change - Base: " +
                baseFaceSelection + ", Toggle: " + toggleFaceSelection + ", Inverted: " + inverted);

        this.preservedBaseFace = this.baseFaceSelection;
        this.preservedToggleFace = this.toggleFaceSelection;
        this.preservedInverted = this.inverted;
        this.preservationActive = true;
    }

    /**
     * FIXED: Restore preserved face selections after state changes
     */
    private void restorePreservedFaceSelections() {
        if (preservationActive && preservedBaseFace != null && preservedToggleFace != null) {
            System.out.println("DEBUG BlockEntity: Restoring preserved face selections - Base: " +
                    preservedBaseFace + ", Toggle: " + preservedToggleFace + ", Inverted: " + preservedInverted);

            this.baseFaceSelection = preservedBaseFace;
            this.toggleFaceSelection = preservedToggleFace;
            this.inverted = preservedInverted;

            // Clear preservation state
            this.preservedBaseFace = null;
            this.preservedToggleFace = null;
            this.preservedInverted = false;
            this.preservationActive = false;

            // Ensure the restored state is saved immediately
            setChanged();
        }
    }

    // ========================================
    // PERFORMANCE OPTIMIZATION: CACHED ANALYSIS
    // ========================================

    // Cache analysis results to prevent repeated processing
    private BlockTextureAnalyzer.BlockTextureInfo cachedBaseAnalysis = null;
    private BlockTextureAnalyzer.BlockTextureInfo cachedToggleAnalysis = null;
    private ItemStack lastAnalyzedBase = ItemStack.EMPTY;
    private ItemStack lastAnalyzedToggle = ItemStack.EMPTY;

    // ========================================
    // ENHANCED MODEL DATA INTEGRATION
    // ========================================

    /**
     * ModelProperty for passing enhanced texture data to custom models
     */
    public static final ModelProperty<SwitchTextureData> TEXTURE_PROPERTY = new ModelProperty<>();

    /**
     * Enhanced data class for passing comprehensive texture information to custom models
     */
    public static class SwitchTextureData {
        private final String baseTexture;
        private final String toggleTexture;
        private final FaceSelectionData.FaceOption baseFace;
        private final FaceSelectionData.FaceOption toggleFace;
        private final boolean inverted;

        public SwitchTextureData(String baseTexture, String toggleTexture,
                                 FaceSelectionData.FaceOption baseFace,
                                 FaceSelectionData.FaceOption toggleFace,
                                 boolean inverted) {
            this.baseTexture = baseTexture;
            this.toggleTexture = toggleTexture;
            this.baseFace = baseFace;
            this.toggleFace = toggleFace;
            this.inverted = inverted;
        }

        public String getBaseTexture() { return baseTexture; }
        public String getToggleTexture() { return toggleTexture; }
        public FaceSelectionData.FaceOption getBaseFace() { return baseFace; }
        public FaceSelectionData.FaceOption getToggleFace() { return toggleFace; }
        public boolean isInverted() { return inverted; }

        /**
         * Check if using custom textures (different from defaults)
         */
        public boolean hasCustomTextures() {
            return !baseTexture.equals(DEFAULT_BASE_TEXTURE) ||
                    !toggleTexture.equals(DEFAULT_TOGGLE_TEXTURE) ||
                    baseFace != FaceSelectionData.FaceOption.ALL ||
                    toggleFace != FaceSelectionData.FaceOption.ALL ||
                    inverted;
        }
    }

    /**
     * Get enhanced ModelData for custom model rendering
     */
    @Override
    @Nonnull
    public ModelData getModelData() {
        return ModelData.builder()
                .with(TEXTURE_PROPERTY, new SwitchTextureData(
                        baseTexturePath, toggleTexturePath,
                        baseFaceSelection, toggleFaceSelection, inverted))
                .build();
    }

    // ========================================
    // CONSTRUCTOR AND BASIC SETUP
    // ========================================

    public SwitchesLeverBlockEntity(BlockPos pos, BlockState blockState) {
        super(JustSomeSwitchesModBlockEntities.SWITCHES_LEVER.get(), pos, blockState);
        System.out.println("DEBUG BlockEntity: Created at position " + pos);
    }

    // ========================================
    // FIXED: BLOCK STATE MANAGEMENT WITH FACE SELECTION PRESERVATION
    // ========================================

    /**
     * FIXED: Override setBlockState to preserve face selections AND trigger model refresh
     */
    @Override
    public void setBlockState(@Nonnull BlockState blockState) {
        System.out.println("DEBUG BlockEntity: setBlockState called - preserving face selections and triggering model refresh");

        // Preserve current face selections before state change
        if (!preservationActive) {
            preserveFaceSelectionsForStateChange();
        }

        super.setBlockState(blockState);

        // Restore face selections after state change
        restorePreservedFaceSelections();

        // CRITICAL FIX: Force immediate ModelData refresh after block state change
        if (level != null && !level.isClientSide) {
            // Request model data update to ensure custom model gets current face selections
            requestModelDataUpdate();

            // Force block update to refresh custom model rendering with preserved face selections
            level.sendBlockUpdated(worldPosition, blockState, blockState, Block.UPDATE_CLIENTS);

            System.out.println("DEBUG BlockEntity: Triggered model refresh with current face selections - Base: " +
                    baseFaceSelection + ", Toggle: " + toggleFaceSelection);
        }

        System.out.println("DEBUG BlockEntity: setBlockState completed with preserved face selections and model refresh");
    }

    // ========================================
    // CLIENT AND SERVER TICK METHODS
    // ========================================

    /**
     * Client-side tick method for any client-specific updates
     */
    public static void clientTick(Level level, BlockPos pos, BlockState state, SwitchesLeverBlockEntity blockEntity) {
        // Client-side logic can be added here if needed
    }

    /**
     * Server-side tick method for any server-specific updates
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, SwitchesLeverBlockEntity blockEntity) {
        // Server-side logic can be added here if needed
    }

    // ========================================
    // TEXTURE MANAGEMENT
    // ========================================

    /**
     * Extract texture path from ItemStack using proper NeoForge 1.20.4 registry access
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
            System.out.println("DEBUG BlockEntity: Error getting texture from item " + itemStack + " - " + e.getMessage());
        }

        return "";
    }

    /**
     * Mark BlockEntity as dirty and trigger client synchronization
     */
    private void markDirtyAndSync() {
        System.out.println("DEBUG BlockEntity: Marking dirty and syncing to clients");
        setChanged();
        if (level != null && !level.isClientSide) {
            // FIXED: Use UPDATE_CLIENTS to avoid triggering unnecessary NBT reloads
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            requestModelDataUpdate();
        }
    }

    /**
     * Set the base texture for the switch (String version)
     */
    public boolean setBaseTexture(@Nonnull String texturePath) {
        if (!texturePath.equals(this.baseTexturePath)) {
            System.out.println("DEBUG BlockEntity: Setting base texture from " + this.baseTexturePath + " to " + texturePath);
            this.baseTexturePath = texturePath;
            markDirtyAndSync();
            return true;
        }
        return false;
    }

    /**
     * Set the base texture for the switch (ItemStack version for GUI compatibility)
     */
    public boolean setBaseTexture(@Nonnull ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return setBaseTexture(DEFAULT_BASE_TEXTURE);
        }
        String texturePath = getTextureFromItem(itemStack);
        return texturePath.isEmpty() ? false : setBaseTexture(texturePath);
    }

    /**
     * Set the toggle texture for the switch (String version)
     */
    public boolean setToggleTexture(@Nonnull String texturePath) {
        if (!texturePath.equals(this.toggleTexturePath)) {
            System.out.println("DEBUG BlockEntity: Setting toggle texture from " + this.toggleTexturePath + " to " + texturePath);
            this.toggleTexturePath = texturePath;
            markDirtyAndSync();
            return true;
        }
        return false;
    }

    /**
     * Set the toggle texture for the switch (ItemStack version for GUI compatibility)
     */
    public boolean setToggleTexture(@Nonnull ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return setToggleTexture(DEFAULT_TOGGLE_TEXTURE);
        }
        String texturePath = getTextureFromItem(itemStack);
        return texturePath.isEmpty() ? false : setToggleTexture(texturePath);
    }

    // ========================================
    // FACE SELECTION MANAGEMENT WITH AUTO-APPLY
    // ========================================

    /**
     * FIXED: Set base face selection with immediate NBT persistence
     */
    public boolean setBaseFaceSelection(@Nonnull FaceSelectionData.FaceOption faceOption) {
        if (this.baseFaceSelection != faceOption) {
            System.out.println("DEBUG BlockEntity: Setting base face selection from " + this.baseFaceSelection + " to " + faceOption);
            this.baseFaceSelection = faceOption;

            // CRITICAL FIX: Immediately save to NBT to prevent race conditions
            forceSaveToNBT();
            markDirtyAndSync();
            return true;
        }
        return false;
    }

    /**
     * FIXED: Set toggle face selection with immediate NBT persistence
     */
    public boolean setToggleFaceSelection(@Nonnull FaceSelectionData.FaceOption faceOption) {
        if (this.toggleFaceSelection != faceOption) {
            System.out.println("DEBUG BlockEntity: Setting toggle face selection from " + this.toggleFaceSelection + " to " + faceOption);
            this.toggleFaceSelection = faceOption;

            // CRITICAL FIX: Immediately save to NBT to prevent race conditions
            forceSaveToNBT();
            markDirtyAndSync();
            return true;
        }
        return false;
    }

    /**
     * FIXED: Set inversion state with immediate NBT persistence
     */
    public boolean setInverted(boolean inverted) {
        if (this.inverted != inverted) {
            System.out.println("DEBUG BlockEntity: Setting inverted state from " + this.inverted + " to " + inverted);
            this.inverted = inverted;

            // CRITICAL FIX: Immediately save to NBT to prevent race conditions
            forceSaveToNBT();
            markDirtyAndSync();
            return true;
        }
        return false;
    }

    /**
     * CRITICAL FIX: Force immediate NBT save to prevent race conditions
     */
    private void forceSaveToNBT() {
        if (level != null && !level.isClientSide) {
            System.out.println("DEBUG BlockEntity: Force saving face selections to NBT immediately - Base: " +
                    baseFaceSelection + ", Toggle: " + toggleFaceSelection + ", Inverted: " + inverted);

            // Immediately save current state to the world's BlockEntity data
            // This ensures that any subsequent NBT reloads will have the correct data
            setChanged();

            // Force the world to save this BlockEntity's data immediately
            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                try {
                    // Get the chunk and mark it for immediate saving
                    var chunk = serverLevel.getChunkAt(worldPosition);
                    // FIXED: Simplified instanceof check to avoid compilation error
                    if (chunk instanceof net.minecraft.world.level.chunk.LevelChunk) {
                        ((net.minecraft.world.level.chunk.LevelChunk) chunk).setUnsaved(true);
                        System.out.println("DEBUG BlockEntity: Chunk marked for immediate save");
                    }
                } catch (Exception e) {
                    System.out.println("DEBUG BlockEntity: Could not force immediate chunk save: " + e.getMessage());
                }
            }
        }
    }

    // ========================================
    // BLOCK ANALYSIS INTEGRATION
    // ========================================

    /**
     * Get block analysis for base texture slot with caching
     */
    @Nonnull
    public BlockTextureAnalyzer.BlockTextureInfo getBaseBlockAnalysis() {
        // Only analyze if item changed or cache is empty
        if (cachedBaseAnalysis == null || !ItemStack.matches(lastAnalyzedBase, guiBaseItem)) {
            System.out.println("DEBUG BlockEntity: Analyzing base item " + guiBaseItem);
            cachedBaseAnalysis = BlockTextureAnalyzer.analyzeBlock(guiBaseItem);
            lastAnalyzedBase = guiBaseItem.copy();
        }
        return cachedBaseAnalysis;
    }

    /**
     * Get block analysis for toggle texture slot with caching
     */
    @Nonnull
    public BlockTextureAnalyzer.BlockTextureInfo getToggleBlockAnalysis() {
        // Only analyze if item changed or cache is empty
        if (cachedToggleAnalysis == null || !ItemStack.matches(lastAnalyzedToggle, guiToggleItem)) {
            System.out.println("DEBUG BlockEntity: Analyzing toggle item " + guiToggleItem);
            cachedToggleAnalysis = BlockTextureAnalyzer.analyzeBlock(guiToggleItem);
            lastAnalyzedToggle = guiToggleItem.copy();
        }
        return cachedToggleAnalysis;
    }

    /**
     * Get dropdown state for base texture slot
     */
    @Nonnull
    public FaceSelectionData.DropdownState getBaseDropdownState() {
        if (guiBaseItem.isEmpty()) {
            return FaceSelectionData.createDisabledState();
        }

        BlockTextureAnalyzer.BlockTextureInfo blockInfo = getBaseBlockAnalysis();
        return FaceSelectionData.createDropdownState(blockInfo, baseFaceSelection);
    }

    /**
     * Get dropdown state for toggle texture slot
     */
    @Nonnull
    public FaceSelectionData.DropdownState getToggleDropdownState() {
        if (guiToggleItem.isEmpty()) {
            return FaceSelectionData.createDisabledState();
        }

        BlockTextureAnalyzer.BlockTextureInfo blockInfo = getToggleBlockAnalysis();
        return FaceSelectionData.createDropdownState(blockInfo, toggleFaceSelection);
    }

    // ========================================
    // GETTERS FOR GUI AND MODEL INTEGRATION
    // ========================================

    @Nonnull public String getBaseTexture() { return baseTexturePath; }
    @Nonnull public String getToggleTexture() { return toggleTexturePath; }
    @Nonnull public FaceSelectionData.FaceOption getBaseFaceSelection() { return baseFaceSelection; }
    @Nonnull public FaceSelectionData.FaceOption getToggleFaceSelection() { return toggleFaceSelection; }
    public boolean isInverted() { return inverted; }

    /**
     * Reset textures to defaults
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

        if (baseFaceSelection != FaceSelectionData.FaceOption.ALL) {
            this.baseFaceSelection = FaceSelectionData.FaceOption.ALL;
            changed = true;
        }

        if (toggleFaceSelection != FaceSelectionData.FaceOption.ALL) {
            this.toggleFaceSelection = FaceSelectionData.FaceOption.ALL;
            changed = true;
        }

        if (inverted) {
            this.inverted = false;
            changed = true;
        }

        if (changed) {
            System.out.println("DEBUG BlockEntity: Reset all textures and settings to defaults");
            markDirtyAndSync();
        }
    }

    /**
     * Reset base texture to default
     */
    public void resetBaseTexture() {
        setBaseTexture(DEFAULT_BASE_TEXTURE);
    }

    /**
     * Reset toggle texture to default
     */
    public void resetToggleTexture() {
        setToggleTexture(DEFAULT_TOGGLE_TEXTURE);
    }

    /**
     * Check if using custom textures
     */
    public boolean hasCustomTextures() {
        return !baseTexturePath.equals(DEFAULT_BASE_TEXTURE) ||
                !toggleTexturePath.equals(DEFAULT_TOGGLE_TEXTURE) ||
                baseFaceSelection != FaceSelectionData.FaceOption.ALL ||
                toggleFaceSelection != FaceSelectionData.FaceOption.ALL ||
                inverted;
    }

    // ========================================
    // GUI SLOT MANAGEMENT
    // ========================================

    /**
     * Get toggle item from GUI slot
     */
    @Nonnull
    public ItemStack getGuiToggleItem() { return guiToggleItem; }

    /**
     * Get base item from GUI slot
     */
    @Nonnull
    public ItemStack getGuiBaseItem() { return guiBaseItem; }

    /**
     * Set GUI slot items (for GUI compatibility)
     */
    public void setGuiSlotItems(@Nonnull ItemStack toggleItem, @Nonnull ItemStack baseItem) {
        System.out.println("DEBUG BlockEntity: Setting GUI slot items - Toggle: " + toggleItem + ", Base: " + baseItem);

        // Clear cache if items changed
        if (!ItemStack.matches(this.guiToggleItem, toggleItem)) {
            cachedToggleAnalysis = null;
        }
        if (!ItemStack.matches(this.guiBaseItem, baseItem)) {
            cachedBaseAnalysis = null;
        }

        this.guiToggleItem = toggleItem.copy();
        this.guiBaseItem = baseItem.copy();
    }

    /**
     * Drop stored texture blocks when switch is broken
     */
    public void dropStoredTextures(@Nonnull Level level, @Nonnull BlockPos pos) {
        System.out.println("DEBUG BlockEntity: Dropping stored textures at " + pos);

        if (!guiToggleItem.isEmpty()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), guiToggleItem);
            System.out.println("DEBUG BlockEntity: Dropped toggle item: " + guiToggleItem);
        }

        if (!guiBaseItem.isEmpty()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), guiBaseItem);
            System.out.println("DEBUG BlockEntity: Dropped base item: " + guiBaseItem);
        }
    }

    // ========================================
    // FIXED: NBT SERIALIZATION WITH FACE SELECTION PRESERVATION
    // ========================================

    @Override
    protected void saveAdditional(@Nonnull CompoundTag nbt) {
        super.saveAdditional(nbt);

        System.out.println("DEBUG BlockEntity: Saving NBT data with current face selections - Base: " +
                baseFaceSelection + ", Toggle: " + toggleFaceSelection + ", Inverted: " + inverted);

        // Save texture paths
        nbt.putString(BASE_TEXTURE_KEY, baseTexturePath);
        nbt.putString(TOGGLE_TEXTURE_KEY, toggleTexturePath);

        // FIXED: Save current face selections (not defaults)
        nbt.putString(BASE_FACE_KEY, baseFaceSelection.getSerializedName());
        nbt.putString(TOGGLE_FACE_KEY, toggleFaceSelection.getSerializedName());

        // Save inversion state
        nbt.putBoolean(INVERTED_KEY, inverted);

        // Save GUI slot items
        if (!guiToggleItem.isEmpty()) {
            nbt.put("gui_toggle_item", guiToggleItem.save(new CompoundTag()));
        }
        if (!guiBaseItem.isEmpty()) {
            nbt.put("gui_base_item", guiBaseItem.save(new CompoundTag()));
        }

        System.out.println("DEBUG BlockEntity: NBT save completed - Base: " + baseFaceSelection +
                ", Toggle: " + toggleFaceSelection + ", Inverted: " + inverted);
    }

    @Override
    public void load(@Nonnull CompoundTag nbt) {
        super.load(nbt);

        System.out.println("DEBUG BlockEntity: Loading NBT data");

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

        // FIXED: Enhanced face selection loading with preservation handling
        if (preservationActive) {
            // If preservation is active, don't overwrite current face selections
            System.out.println("DEBUG BlockEntity: Preservation active - keeping current face selections");
            restorePreservedFaceSelections();
        } else {
            // Load face selections from NBT
            String baseFaceName = nbt.getString(BASE_FACE_KEY);
            FaceSelectionData.FaceOption nbtBaseFace = baseFaceName.isEmpty() ? FaceSelectionData.FaceOption.ALL :
                    FaceSelectionData.FaceOption.fromSerializedName(baseFaceName);

            String toggleFaceName = nbt.getString(TOGGLE_FACE_KEY);
            FaceSelectionData.FaceOption nbtToggleFace = toggleFaceName.isEmpty() ? FaceSelectionData.FaceOption.ALL :
                    FaceSelectionData.FaceOption.fromSerializedName(toggleFaceName);

            boolean nbtInverted = nbt.getBoolean(INVERTED_KEY);

            // CRITICAL FIX: Only update face selections if NBT data is newer than current memory state
            // This prevents race conditions where NBT overwrites recent changes
            boolean shouldUpdateFromNBT = true;

            // If face selections were recently changed (not defaults), and NBT has defaults,
            // the NBT data is probably stale - don't overwrite recent changes
            if ((this.baseFaceSelection != FaceSelectionData.FaceOption.ALL ||
                    this.toggleFaceSelection != FaceSelectionData.FaceOption.ALL ||
                    this.inverted) &&
                    (nbtBaseFace == FaceSelectionData.FaceOption.ALL &&
                            nbtToggleFace == FaceSelectionData.FaceOption.ALL &&
                            !nbtInverted)) {

                System.out.println("DEBUG BlockEntity: NBT data appears stale - keeping current face selections");
                shouldUpdateFromNBT = false;
            }

            if (shouldUpdateFromNBT) {
                this.baseFaceSelection = nbtBaseFace;
                this.toggleFaceSelection = nbtToggleFace;
                this.inverted = nbtInverted;

                System.out.println("DEBUG BlockEntity: Loaded face selections from NBT - Base: " + baseFaceSelection +
                        ", Toggle: " + toggleFaceSelection + ", Inverted: " + inverted);
            } else {
                System.out.println("DEBUG BlockEntity: Keeping current face selections - Base: " + baseFaceSelection +
                        ", Toggle: " + toggleFaceSelection + ", Inverted: " + inverted);
            }
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

        // Clear analysis cache on load
        cachedBaseAnalysis = null;
        cachedToggleAnalysis = null;
    }

    // ========================================
    // CLIENT-SERVER SYNCHRONIZATION
    // ========================================

    /**
     * Get update packet for client synchronization
     */
    @Override
    @Nullable
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /**
     * Get enhanced update tag for client synchronization
     */
    @Override
    @Nonnull
    public CompoundTag getUpdateTag() {
        CompoundTag nbt = super.getUpdateTag();

        // Include all customization data for client sync
        nbt.putString(BASE_TEXTURE_KEY, baseTexturePath);
        nbt.putString(TOGGLE_TEXTURE_KEY, toggleTexturePath);
        nbt.putString(BASE_FACE_KEY, baseFaceSelection.getSerializedName());
        nbt.putString(TOGGLE_FACE_KEY, toggleFaceSelection.getSerializedName());
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

    /**
     * Handle data packet from server
     */
    @Override
    public void onDataPacket(@Nonnull net.minecraft.network.Connection net, @Nonnull ClientboundBlockEntityDataPacket pkt) {
        CompoundTag nbt = pkt.getTag();
        if (nbt != null) {
            load(nbt);
            requestModelDataUpdate();
        }
    }
}