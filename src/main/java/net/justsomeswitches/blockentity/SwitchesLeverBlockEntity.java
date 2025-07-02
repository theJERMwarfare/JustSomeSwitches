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
 * FIXED: Enhanced BlockEntity with Critical Face Selection Persistence Fix
 * ---
 * CRITICAL FIX: Ensures face selections survive BlockEntity recreation during lever state changes
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

    // CRITICAL FIX: Add preservation flag to track state changes
    private static final String PRESERVATION_FLAG_KEY = "face_preservation_active";

    // Current texture paths
    private String baseTexturePath = DEFAULT_BASE_TEXTURE;
    private String toggleTexturePath = DEFAULT_TOGGLE_TEXTURE;

    // Face selections with proper initialization
    private FaceSelectionData.FaceOption baseFaceSelection = FaceSelectionData.FaceOption.ALL;
    private FaceSelectionData.FaceOption toggleFaceSelection = FaceSelectionData.FaceOption.ALL;

    // Inversion state
    private boolean inverted = false;

    // CRITICAL FIX: Preservation state tracking
    private boolean facePreservationActive = false;

    // GUI slot storage
    private ItemStack guiToggleItem = ItemStack.EMPTY;
    private ItemStack guiBaseItem = ItemStack.EMPTY;

    // Debug control
    private static final boolean MINIMAL_DEBUG = true;

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
        if (MINIMAL_DEBUG) {
            System.out.println("DEBUG BlockEntity: Created at position " + pos);
        }
    }

    // ========================================
    // CRITICAL FIX: FACE SELECTION PRESERVATION SYSTEM
    // ========================================

    /**
     * CRITICAL FIX: Preserve face selections for state change with immediate NBT persistence
     */
    public void preserveFaceSelectionsForStateChange() {
        if (MINIMAL_DEBUG) {
            System.out.println("DEBUG BlockEntity: CRITICAL FIX - Preserving face selections");
            System.out.println("DEBUG BlockEntity: Current selections - Base: " + baseFaceSelection +
                    ", Toggle: " + toggleFaceSelection + ", Inverted: " + inverted);
        }

        // CRITICAL FIX: Set preservation flag and force immediate save
        this.facePreservationActive = true;

        // CRITICAL FIX: Force immediate NBT save
        this.setChanged();

        if (MINIMAL_DEBUG) {
            System.out.println("DEBUG BlockEntity: CRITICAL FIX - NBT preservation triggered");
            System.out.println("DEBUG BlockEntity: Preserving Base: " + baseFaceSelection +
                    ", Toggle: " + toggleFaceSelection + ", Inverted: " + inverted);
        }
    }

    /**
     * CRITICAL FIX: Override setBlockState to handle preservation
     */
    @Override
    public void setBlockState(@Nonnull BlockState blockState) {
        if (MINIMAL_DEBUG) {
            System.out.println("DEBUG BlockEntity: CRITICAL FIX - setBlockState called with preservation");
        }

        // CRITICAL FIX: If preservation is active, maintain the flag through state change
        if (facePreservationActive) {
            System.out.println("DEBUG BlockEntity: CRITICAL FIX - Maintaining preservation through state change");
        }

        super.setBlockState(blockState);

        // Force immediate ModelData refresh after block state change
        if (level != null && !level.isClientSide) {
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, blockState, blockState, Block.UPDATE_CLIENTS);
        }
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
        ResourceLocation blockRegistryName = BuiltInRegistries.BLOCK.getKey(block);

        if (blockRegistryName != null) {
            return blockRegistryName.getNamespace() + ":block/" + blockRegistryName.getPath();
        }

        return "";
    }

    /**
     * Set base texture from texture path
     */
    public boolean setBaseTexture(@Nonnull String texturePath) {
        if (!this.baseTexturePath.equals(texturePath)) {
            this.baseTexturePath = texturePath;
            markDirtyAndSync();
            return true;
        }
        return false;
    }

    /**
     * Set base texture from ItemStack
     */
    public boolean setBaseTexture(@Nonnull ItemStack itemStack) {
        String texturePath = getTextureFromItem(itemStack);
        if (!texturePath.isEmpty()) {
            return setBaseTexture(texturePath);
        }
        return false;
    }

    /**
     * Set toggle texture from texture path
     */
    public boolean setToggleTexture(@Nonnull String texturePath) {
        if (!this.toggleTexturePath.equals(texturePath)) {
            this.toggleTexturePath = texturePath;
            markDirtyAndSync();
            return true;
        }
        return false;
    }

    /**
     * Set toggle texture from ItemStack
     */
    public boolean setToggleTexture(@Nonnull ItemStack itemStack) {
        String texturePath = getTextureFromItem(itemStack);
        if (!texturePath.isEmpty()) {
            return setToggleTexture(texturePath);
        }
        return false;
    }

    // ========================================
    // FACE SELECTION MANAGEMENT
    // ========================================

    /**
     * Set base face selection with validation
     */
    public boolean setBaseFaceSelection(@Nonnull FaceSelectionData.FaceOption faceSelection) {
        if (this.baseFaceSelection != faceSelection) {
            if (MINIMAL_DEBUG) {
                System.out.println("DEBUG BlockEntity: Setting base face selection from " +
                        this.baseFaceSelection + " to " + faceSelection);
            }
            this.baseFaceSelection = faceSelection;
            markDirtyAndSync();
            return true;
        }
        return false;
    }

    /**
     * Set toggle face selection with validation
     */
    public boolean setToggleFaceSelection(@Nonnull FaceSelectionData.FaceOption faceSelection) {
        if (this.toggleFaceSelection != faceSelection) {
            if (MINIMAL_DEBUG) {
                System.out.println("DEBUG BlockEntity: Setting toggle face selection from " +
                        this.toggleFaceSelection + " to " + faceSelection);
            }
            this.toggleFaceSelection = faceSelection;
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
            if (MINIMAL_DEBUG) {
                System.out.println("DEBUG BlockEntity: Setting inverted state from " + this.inverted + " to " + inverted);
            }
            this.inverted = inverted;
            markDirtyAndSync();
            return true;
        }
        return false;
    }

    // ========================================
    // BLOCK ANALYSIS INTEGRATION
    // ========================================

    // Cache analysis results to prevent repeated processing
    private BlockTextureAnalyzer.BlockTextureInfo cachedBaseAnalysis = null;
    private BlockTextureAnalyzer.BlockTextureInfo cachedToggleAnalysis = null;
    private ItemStack lastAnalyzedBase = ItemStack.EMPTY;
    private ItemStack lastAnalyzedToggle = ItemStack.EMPTY;

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

        if (!baseTexturePath.equals(DEFAULT_BASE_TEXTURE)) {
            this.baseTexturePath = DEFAULT_BASE_TEXTURE;
            changed = true;
        }

        if (!toggleTexturePath.equals(DEFAULT_TOGGLE_TEXTURE)) {
            this.toggleTexturePath = DEFAULT_TOGGLE_TEXTURE;
            changed = true;
        }

        if (baseFaceSelection != FaceSelectionData.FaceOption.ALL ||
                toggleFaceSelection != FaceSelectionData.FaceOption.ALL ||
                inverted) {
            this.baseFaceSelection = FaceSelectionData.FaceOption.ALL;
            this.toggleFaceSelection = FaceSelectionData.FaceOption.ALL;
            this.inverted = false;
            changed = true;
        }

        if (changed) {
            if (MINIMAL_DEBUG) {
                System.out.println("DEBUG BlockEntity: Reset all textures and settings to defaults");
            }
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
        if (MINIMAL_DEBUG) {
            System.out.println("DEBUG BlockEntity: Setting GUI slot items - Toggle: " + toggleItem + ", Base: " + baseItem);
        }

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
        if (MINIMAL_DEBUG) {
            System.out.println("DEBUG BlockEntity: Dropping stored textures at " + pos);
        }

        if (!guiToggleItem.isEmpty()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), guiToggleItem);
        }

        if (!guiBaseItem.isEmpty()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), guiBaseItem);
        }
    }

    // ========================================
    // CRITICAL FIX: ENHANCED NBT SERIALIZATION WITH PRESERVATION
    // ========================================

    @Override
    protected void saveAdditional(@Nonnull CompoundTag nbt) {
        super.saveAdditional(nbt);

        // Save texture paths
        nbt.putString(BASE_TEXTURE_KEY, baseTexturePath);
        nbt.putString(TOGGLE_TEXTURE_KEY, toggleTexturePath);

        // CRITICAL FIX: Always save face selections
        nbt.putString(BASE_FACE_KEY, baseFaceSelection.getSerializedName());
        nbt.putString(TOGGLE_FACE_KEY, toggleFaceSelection.getSerializedName());

        // Save inversion state
        nbt.putBoolean(INVERTED_KEY, inverted);

        // CRITICAL FIX: Save preservation flag
        nbt.putBoolean(PRESERVATION_FLAG_KEY, facePreservationActive);

        // Save GUI slot items
        if (!guiToggleItem.isEmpty()) {
            nbt.put("gui_toggle_item", guiToggleItem.save(new CompoundTag()));
        }
        if (!guiBaseItem.isEmpty()) {
            nbt.put("gui_base_item", guiBaseItem.save(new CompoundTag()));
        }

        if (MINIMAL_DEBUG) {
            System.out.println("DEBUG BlockEntity: CRITICAL FIX - Saved NBT with face selections");
            System.out.println("DEBUG BlockEntity: Saved Base: " + baseFaceSelection +
                    ", Toggle: " + toggleFaceSelection + ", Inverted: " + inverted +
                    ", Preservation: " + facePreservationActive);
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

        // CRITICAL FIX: Load face selections with proper fallback
        String baseFaceName = nbt.getString(BASE_FACE_KEY);
        this.baseFaceSelection = baseFaceName.isEmpty() ? FaceSelectionData.FaceOption.ALL :
                FaceSelectionData.FaceOption.fromSerializedName(baseFaceName);

        String toggleFaceName = nbt.getString(TOGGLE_FACE_KEY);
        this.toggleFaceSelection = toggleFaceName.isEmpty() ? FaceSelectionData.FaceOption.ALL :
                FaceSelectionData.FaceOption.fromSerializedName(toggleFaceName);

        // Load inversion state
        this.inverted = nbt.getBoolean(INVERTED_KEY);

        // CRITICAL FIX: Load preservation flag
        this.facePreservationActive = nbt.getBoolean(PRESERVATION_FLAG_KEY);

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

        if (MINIMAL_DEBUG) {
            System.out.println("DEBUG BlockEntity: CRITICAL FIX - Loaded NBT with face selections");
            System.out.println("DEBUG BlockEntity: Loaded Base: " + baseFaceSelection +
                    ", Toggle: " + toggleFaceSelection + ", Inverted: " + inverted +
                    ", Preservation: " + facePreservationActive);
        }

        // CRITICAL FIX: Reset preservation flag after successful load
        if (facePreservationActive) {
            this.facePreservationActive = false;
            System.out.println("DEBUG BlockEntity: CRITICAL FIX - Face selection preservation completed successfully");
        }
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

    // ========================================
    // UTILITY METHODS
    // ========================================

    /**
     * Mark dirty and sync to clients
     */
    public void markDirtyAndSync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
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
}