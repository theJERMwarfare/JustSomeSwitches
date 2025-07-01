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
 * CRITICAL DEBUG VERSION: Track variable overwrites and fix face mapping
 * ---
 * DIAGNOSTIC: Adding comprehensive tracking to identify when/why face selections get overwritten
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
    // CRITICAL DEBUG: VARIABLE OVERWRITE TRACKING
    // ========================================

    // Track when face selections are set to detect overwrites
    private FaceSelectionData.FaceOption debugLastSetBaseFace = FaceSelectionData.FaceOption.ALL;
    private FaceSelectionData.FaceOption debugLastSetToggleFace = FaceSelectionData.FaceOption.ALL;
    private boolean debugLastSetInverted = false;
    private long debugLastSetTimestamp = 0;

    // Track call stack to identify source of overwrites
    private String debugLastSetSource = "initial";
    private int debugSetCount = 0;

    /**
     * CRITICAL DEBUG: Track face selection changes and detect overwrites
     */
    private void debugTrackFaceSelectionChange(String source, FaceSelectionData.FaceOption newBase,
                                               FaceSelectionData.FaceOption newToggle, boolean newInverted) {
        debugSetCount++;
        long now = System.currentTimeMillis();

        // Check for unexpected overwrites
        if (!baseFaceSelection.equals(debugLastSetBaseFace) &&
                !newBase.equals(debugLastSetBaseFace) &&
                debugSetCount > 1) {
            System.out.println("🚨 CRITICAL DEBUG: UNEXPECTED FACE SELECTION OVERWRITE DETECTED!");
            System.out.println("   Expected base: " + debugLastSetBaseFace + " → Actual: " + baseFaceSelection + " → New: " + newBase);
            System.out.println("   Last set by: " + debugLastSetSource + " (" + (now - debugLastSetTimestamp) + "ms ago)");
            System.out.println("   New set by: " + source);
            System.out.println("   Call #" + debugSetCount);

            // Print stack trace to identify source
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            for (int i = 2; i < Math.min(8, stack.length); i++) {
                System.out.println("   Stack[" + i + "]: " + stack[i]);
            }
        }

        // Update tracking
        debugLastSetBaseFace = newBase;
        debugLastSetToggleFace = newToggle;
        debugLastSetInverted = newInverted;
        debugLastSetTimestamp = now;
        debugLastSetSource = source;

        System.out.println("DEBUG TRACK: [" + source + "] Set face selections - Base: " + newBase +
                ", Toggle: " + newToggle + ", Inverted: " + newInverted + " (Call #" + debugSetCount + ")");
    }

    /**
     * CRITICAL DEBUG: Verify face selections before any operation
     */
    private void debugVerifyFaceSelections(String operation) {
        if (!baseFaceSelection.equals(debugLastSetBaseFace) ||
                !toggleFaceSelection.equals(debugLastSetToggleFace) ||
                inverted != debugLastSetInverted) {

            System.out.println("🚨 CRITICAL DEBUG: FACE SELECTION MISMATCH in " + operation + "!");
            System.out.println("   Expected: Base=" + debugLastSetBaseFace + ", Toggle=" + debugLastSetToggleFace +
                    ", Inverted=" + debugLastSetInverted);
            System.out.println("   Actual: Base=" + baseFaceSelection + ", Toggle=" + toggleFaceSelection +
                    ", Inverted=" + inverted);
            System.out.println("   Last set by: " + debugLastSetSource + " (" +
                    (System.currentTimeMillis() - debugLastSetTimestamp) + "ms ago)");
        }
    }

    // ========================================
    // FACE SELECTION PRESERVATION SYSTEM
    // ========================================

    // Temporary storage for preserving face selections during state changes
    private FaceSelectionData.FaceOption preservedBaseFace = null;
    private FaceSelectionData.FaceOption preservedToggleFace = null;
    private boolean preservedInverted = false;
    private boolean preservationActive = false;

    /**
     * Preserve face selections before block state changes
     */
    public void preserveFaceSelectionsForStateChange() {
        debugVerifyFaceSelections("preserveFaceSelectionsForStateChange");

        System.out.println("DEBUG BlockEntity: Preserving face selections before state change - Base: " +
                baseFaceSelection + ", Toggle: " + toggleFaceSelection + ", Inverted: " + inverted);

        this.preservedBaseFace = this.baseFaceSelection;
        this.preservedToggleFace = this.toggleFaceSelection;
        this.preservedInverted = this.inverted;
        this.preservationActive = true;
    }

    /**
     * Restore preserved face selections after state changes
     */
    private void restorePreservedFaceSelections() {
        if (preservationActive && preservedBaseFace != null && preservedToggleFace != null) {
            System.out.println("DEBUG BlockEntity: Restoring preserved face selections - Base: " +
                    preservedBaseFace + ", Toggle: " + preservedToggleFace + ", Inverted: " + preservedInverted);

            // Update tracking BEFORE setting values
            debugTrackFaceSelectionChange("restorePreservedFaceSelections",
                    preservedBaseFace, preservedToggleFace, preservedInverted);

            this.baseFaceSelection = preservedBaseFace;
            this.toggleFaceSelection = preservedToggleFace;
            this.inverted = preservedInverted;

            // Clear preservation state
            this.preservedBaseFace = null;
            this.preservedToggleFace = null;
            this.preservedInverted = false;
            this.preservationActive = false;

            // Force immediate save
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
        debugVerifyFaceSelections("getModelData");
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
        debugTrackFaceSelectionChange("constructor",
                FaceSelectionData.FaceOption.ALL, FaceSelectionData.FaceOption.ALL, false);
        System.out.println("DEBUG BlockEntity: Created at position " + pos);
    }

    // ========================================
    // BLOCK STATE MANAGEMENT WITH FACE SELECTION PRESERVATION
    // ========================================

    /**
     * Override setBlockState to preserve face selections AND trigger model refresh
     */
    @Override
    public void setBlockState(@Nonnull BlockState blockState) {
        System.out.println("DEBUG BlockEntity: setBlockState called - preserving face selections and triggering model refresh");
        debugVerifyFaceSelections("setBlockState-before");

        // Preserve current face selections before state change
        if (!preservationActive) {
            preserveFaceSelectionsForStateChange();
        }

        super.setBlockState(blockState);

        // Restore face selections after state change
        restorePreservedFaceSelections();

        debugVerifyFaceSelections("setBlockState-after");

        // Force immediate ModelData refresh after block state change
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
        debugVerifyFaceSelections("markDirtyAndSync");
        System.out.println("DEBUG BlockEntity: Marking dirty and syncing to clients");

        if (level != null && !level.isClientSide) {
            setChanged();
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
    // CRITICAL DEBUG: FACE SELECTION MANAGEMENT WITH TRACKING
    // ========================================

    /**
     * CRITICAL DEBUG: Set base face selection with comprehensive tracking
     */
    public boolean setBaseFaceSelection(@Nonnull FaceSelectionData.FaceOption faceOption) {
        debugVerifyFaceSelections("setBaseFaceSelection-before");

        if (this.baseFaceSelection != faceOption) {
            System.out.println("DEBUG BlockEntity: Setting base face selection from " + this.baseFaceSelection + " to " + faceOption);

            // Update tracking BEFORE setting value
            debugTrackFaceSelectionChange("setBaseFaceSelection", faceOption, this.toggleFaceSelection, this.inverted);

            this.baseFaceSelection = faceOption;
            markDirtyAndSync();

            debugVerifyFaceSelections("setBaseFaceSelection-after");
            return true;
        }
        return false;
    }

    /**
     * CRITICAL DEBUG: Set toggle face selection with comprehensive tracking
     */
    public boolean setToggleFaceSelection(@Nonnull FaceSelectionData.FaceOption faceOption) {
        debugVerifyFaceSelections("setToggleFaceSelection-before");

        if (this.toggleFaceSelection != faceOption) {
            System.out.println("DEBUG BlockEntity: Setting toggle face selection from " + this.toggleFaceSelection + " to " + faceOption);

            // Update tracking BEFORE setting value
            debugTrackFaceSelectionChange("setToggleFaceSelection", this.baseFaceSelection, faceOption, this.inverted);

            this.toggleFaceSelection = faceOption;
            markDirtyAndSync();

            debugVerifyFaceSelections("setToggleFaceSelection-after");
            return true;
        }
        return false;
    }

    /**
     * CRITICAL DEBUG: Set inversion state with comprehensive tracking
     */
    public boolean setInverted(boolean inverted) {
        debugVerifyFaceSelections("setInverted-before");

        if (this.inverted != inverted) {
            System.out.println("DEBUG BlockEntity: Setting inverted state from " + this.inverted + " to " + inverted);

            // Update tracking BEFORE setting value
            debugTrackFaceSelectionChange("setInverted", this.baseFaceSelection, this.toggleFaceSelection, inverted);

            this.inverted = inverted;
            markDirtyAndSync();

            debugVerifyFaceSelections("setInverted-after");
            return true;
        }
        return false;
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

        FaceSelectionData.FaceOption newBaseFace = FaceSelectionData.FaceOption.ALL;
        FaceSelectionData.FaceOption newToggleFace = FaceSelectionData.FaceOption.ALL;
        boolean newInverted = false;

        if (!baseTexturePath.equals(DEFAULT_BASE_TEXTURE)) {
            this.baseTexturePath = DEFAULT_BASE_TEXTURE;
            changed = true;
        }

        if (!toggleTexturePath.equals(DEFAULT_TOGGLE_TEXTURE)) {
            this.toggleTexturePath = DEFAULT_TOGGLE_TEXTURE;
            changed = true;
        }

        if (baseFaceSelection != newBaseFace || toggleFaceSelection != newToggleFace || inverted != newInverted) {
            debugTrackFaceSelectionChange("resetTextures", newBaseFace, newToggleFace, newInverted);
            this.baseFaceSelection = newBaseFace;
            this.toggleFaceSelection = newToggleFace;
            this.inverted = newInverted;
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
    // CRITICAL DEBUG: NBT SERIALIZATION WITH COMPREHENSIVE TRACKING
    // ========================================

    @Override
    protected void saveAdditional(@Nonnull CompoundTag nbt) {
        super.saveAdditional(nbt);

        // CRITICAL DEBUG: Check if variables were overwritten before save
        debugVerifyFaceSelections("saveAdditional");

        if (!baseFaceSelection.equals(debugLastSetBaseFace) ||
                !toggleFaceSelection.equals(debugLastSetToggleFace) ||
                inverted != debugLastSetInverted) {

            System.out.println("🚨 CRITICAL: Face selections were OVERWRITTEN before NBT save!");
            System.out.println("   Expected: Base=" + debugLastSetBaseFace + ", Toggle=" + debugLastSetToggleFace +
                    ", Inverted=" + debugLastSetInverted);
            System.out.println("   Found: Base=" + baseFaceSelection + ", Toggle=" + toggleFaceSelection +
                    ", Inverted=" + inverted);
            System.out.println("   Last set by: " + debugLastSetSource + " (" +
                    (System.currentTimeMillis() - debugLastSetTimestamp) + "ms ago)");

            // Print stack trace to find the culprit
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            for (int i = 2; i < Math.min(10, stack.length); i++) {
                System.out.println("   Stack[" + i + "]: " + stack[i]);
            }

            // EMERGENCY FIX: Use the values we expected to save
            System.out.println("🔧 EMERGENCY: Saving expected values instead of corrupted ones");
            FaceSelectionData.FaceOption saveBaseFace = debugLastSetBaseFace;
            FaceSelectionData.FaceOption saveToggleFace = debugLastSetToggleFace;
            boolean saveInverted = debugLastSetInverted;

            // Save expected values
            nbt.putString(BASE_TEXTURE_KEY, baseTexturePath);
            nbt.putString(TOGGLE_TEXTURE_KEY, toggleTexturePath);
            nbt.putString(BASE_FACE_KEY, saveBaseFace.getSerializedName());
            nbt.putString(TOGGLE_FACE_KEY, saveToggleFace.getSerializedName());
            nbt.putBoolean(INVERTED_KEY, saveInverted);

            System.out.println("DEBUG BlockEntity: NBT save completed with CORRECTED values - Base: " + saveBaseFace +
                    ", Toggle: " + saveToggleFace + ", Inverted: " + saveInverted);
        } else {
            // Normal save - variables are correct
            System.out.println("DEBUG BlockEntity: Saving NBT data with current face selections - Base: " +
                    baseFaceSelection + ", Toggle: " + toggleFaceSelection + ", Inverted: " + inverted);

            // Save texture paths
            nbt.putString(BASE_TEXTURE_KEY, baseTexturePath);
            nbt.putString(TOGGLE_TEXTURE_KEY, toggleTexturePath);

            // Save current face selections
            nbt.putString(BASE_FACE_KEY, baseFaceSelection.getSerializedName());
            nbt.putString(TOGGLE_FACE_KEY, toggleFaceSelection.getSerializedName());

            // Save inversion state
            nbt.putBoolean(INVERTED_KEY, inverted);

            System.out.println("DEBUG BlockEntity: NBT save completed - Base: " + baseFaceSelection +
                    ", Toggle: " + toggleFaceSelection + ", Inverted: " + inverted);
        }

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

        System.out.println("DEBUG BlockEntity: Loading NBT data");
        debugVerifyFaceSelections("load-before");

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

        // CRITICAL FIX: Check if we just finished preserving face selections
        if (preservationActive) {
            System.out.println("DEBUG BlockEntity: ⚠️ SKIPPING NBT face selection load - preservation active, would overwrite restored values!");
            // Don't call restorePreservedFaceSelections() here - it was already called in setBlockState
        } else {
            // Check if this NBT load is happening shortly after a face selection change
            long timeSinceLastSet = System.currentTimeMillis() - debugLastSetTimestamp;
            if (timeSinceLastSet < 1000 && debugSetCount > 0) {
                System.out.println("DEBUG BlockEntity: ⚠️ SUSPICIOUS NBT load " + timeSinceLastSet + "ms after face selection change - keeping current values to prevent overwrites");
                System.out.println("   Would load from NBT: Base=" + nbt.getString(BASE_FACE_KEY) + ", Toggle=" + nbt.getString(TOGGLE_FACE_KEY));
                System.out.println("   Keeping current: Base=" + baseFaceSelection + ", Toggle=" + toggleFaceSelection);
            } else {
                // Safe to load from NBT
                String baseFaceName = nbt.getString(BASE_FACE_KEY);
                FaceSelectionData.FaceOption nbtBaseFace = baseFaceName.isEmpty() ? FaceSelectionData.FaceOption.ALL :
                        FaceSelectionData.FaceOption.fromSerializedName(baseFaceName);

                String toggleFaceName = nbt.getString(TOGGLE_FACE_KEY);
                FaceSelectionData.FaceOption nbtToggleFace = toggleFaceName.isEmpty() ? FaceSelectionData.FaceOption.ALL :
                        FaceSelectionData.FaceOption.fromSerializedName(toggleFaceName);

                boolean nbtInverted = nbt.getBoolean(INVERTED_KEY);

                // Update tracking BEFORE setting values
                debugTrackFaceSelectionChange("load-NBT", nbtBaseFace, nbtToggleFace, nbtInverted);

                this.baseFaceSelection = nbtBaseFace;
                this.toggleFaceSelection = nbtToggleFace;
                this.inverted = nbtInverted;

                System.out.println("DEBUG BlockEntity: Loaded face selections from NBT - Base: " + baseFaceSelection +
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

        debugVerifyFaceSelections("load-after");
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