package net.justsomeswitches.blockentity;

import net.justsomeswitches.config.DebugConfig;
import net.justsomeswitches.gui.FaceSelectionData;
import net.justsomeswitches.init.JustSomeSwitchesModBlockEntities;
import net.justsomeswitches.util.BlockTextureAnalyzer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
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
 * FRAMED BLOCKS APPROACH: Bulletproof NBT persistence with enhanced client-server sync
 */
public class SwitchesLeverBlockEntity extends BlockEntity {

    // ========================================
    // TEXTURE CONFIGURATION
    // ========================================

    public static final String DEFAULT_BASE_TEXTURE = "minecraft:block/stone";
    public static final String DEFAULT_TOGGLE_TEXTURE = "minecraft:block/oak_planks";

    // Enhanced NBT keys with versioning for future compatibility
    private static final String NBT_VERSION_KEY = "nbt_version";
    private static final int CURRENT_NBT_VERSION = 2;
    private static final String BASE_TEXTURE_KEY = "base_texture_path";
    private static final String TOGGLE_TEXTURE_KEY = "toggle_texture_path";
    private static final String BASE_FACE_KEY = "base_face_selection";
    private static final String TOGGLE_FACE_KEY = "toggle_face_selection";
    private static final String INVERTED_KEY = "inverted_state";
    private static final String GUI_TOGGLE_ITEM_KEY = "gui_toggle_item";
    private static final String GUI_BASE_ITEM_KEY = "gui_base_item";

    // Core texture data - NEVER reset during lever operations
    private String baseTexturePath = DEFAULT_BASE_TEXTURE;
    private String toggleTexturePath = DEFAULT_TOGGLE_TEXTURE;
    private FaceSelectionData.FaceOption baseFaceSelection = FaceSelectionData.FaceOption.ALL;
    private FaceSelectionData.FaceOption toggleFaceSelection = FaceSelectionData.FaceOption.ALL;
    private boolean inverted = false;

    // GUI slot items - persistent storage
    private ItemStack guiToggleItem = ItemStack.EMPTY;
    private ItemStack guiBaseItem = ItemStack.EMPTY;

    // Control flags
    private boolean suppressChangeNotifications = false;
    private boolean needsClientSync = false;

    // ========================================
    // MODEL DATA INTEGRATION
    // ========================================

    public static final ModelProperty<SwitchTextureData> TEXTURE_PROPERTY = new ModelProperty<>();

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

        public boolean hasCustomTextures() {
            return !baseTexture.equals(DEFAULT_BASE_TEXTURE) ||
                    !toggleTexture.equals(DEFAULT_TOGGLE_TEXTURE) ||
                    baseFace != FaceSelectionData.FaceOption.ALL ||
                    toggleFace != FaceSelectionData.FaceOption.ALL ||
                    inverted;
        }
    }

    @Override
    @Nonnull
    public ModelData getModelData() {
        return ModelData.builder()
                .with(TEXTURE_PROPERTY, new SwitchTextureData(
                        baseTexturePath, toggleTexturePath,
                        baseFaceSelection, toggleFaceSelection, inverted))
                .build();
    }

    public SwitchesLeverBlockEntity(BlockPos pos, BlockState blockState) {
        super(JustSomeSwitchesModBlockEntities.SWITCHES_LEVER.get(), pos, blockState);
    }

    // ========================================
    // ENHANCED TEXTURE APPLICATION WITH FRAMED BLOCKS APPROACH
    // ========================================

    /**
     * Apply current texture settings with comprehensive persistence
     */
    public void applyCurrentTextureSettings() {
        suppressChangeNotifications = true;
        boolean textureChanged = false;

        // Apply toggle texture with face selection
        if (!guiToggleItem.isEmpty()) {
            String effectiveTexturePath = getEffectiveTexturePathForItem(guiToggleItem, toggleFaceSelection);
            if (setToggleTextureInternal(effectiveTexturePath)) {
                textureChanged = true;
            }
        } else {
            if (resetToggleTextureInternal()) {
                textureChanged = true;
            }
        }

        // Apply base texture with face selection
        if (!guiBaseItem.isEmpty()) {
            String effectiveTexturePath = getEffectiveTexturePathForItem(guiBaseItem, baseFaceSelection);
            if (setBaseTextureInternal(effectiveTexturePath)) {
                textureChanged = true;
            }
        } else {
            if (resetBaseTextureInternal()) {
                textureChanged = true;
            }
        }

        suppressChangeNotifications = false;

        if (textureChanged) {
            triggerCompleteSync();
        }
    }

    /**
     * FRAMED BLOCKS APPROACH: Complete synchronization that survives setBlock() calls
     */
    private void triggerCompleteSync() {
        if (level != null) {
            // Mark for immediate NBT persistence
            setChanged();
            needsClientSync = true;

            if (!level.isClientSide) {
                // Force immediate server-to-client sync
                BlockState currentState = getBlockState();
                level.sendBlockUpdated(worldPosition, currentState, currentState, Block.UPDATE_ALL);

                // Update model data
                requestModelDataUpdate();
            } else {
                // Client-side model refresh
                requestModelDataUpdate();
            }
        }
    }

    @Nonnull
    private String getEffectiveTexturePathForItem(@Nonnull ItemStack item, @Nonnull FaceSelectionData.FaceOption faceSelection) {
        if (item.isEmpty()) {
            return DEFAULT_BASE_TEXTURE;
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

        return DEFAULT_BASE_TEXTURE;
    }

    // ========================================
    // INTERNAL TEXTURE MANAGEMENT
    // ========================================

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

    private boolean setBaseTextureInternal(@Nonnull String texturePath) {
        if (!this.baseTexturePath.equals(texturePath)) {
            this.baseTexturePath = texturePath;
            return true;
        }
        return false;
    }

    private boolean setToggleTextureInternal(@Nonnull String texturePath) {
        if (!this.toggleTexturePath.equals(texturePath)) {
            this.toggleTexturePath = texturePath;
            return true;
        }
        return false;
    }

    private boolean resetBaseTextureInternal() {
        return setBaseTextureInternal(DEFAULT_BASE_TEXTURE);
    }

    private boolean resetToggleTextureInternal() {
        return setToggleTextureInternal(DEFAULT_TOGGLE_TEXTURE);
    }

    // ========================================
    // PUBLIC TEXTURE API
    // ========================================

    public boolean setBaseTexture(@Nonnull String texturePath) {
        if (setBaseTextureInternal(texturePath)) {
            if (!suppressChangeNotifications) {
                triggerCompleteSync();
            }
            return true;
        }
        return false;
    }

    public boolean setToggleTexture(@Nonnull String texturePath) {
        if (setToggleTextureInternal(texturePath)) {
            if (!suppressChangeNotifications) {
                triggerCompleteSync();
            }
            return true;
        }
        return false;
    }

    // ========================================
    // ENHANCED FACE SELECTION WITH BULLETPROOF PERSISTENCE
    // ========================================

    public boolean setBaseFaceSelection(@Nonnull FaceSelectionData.FaceOption faceSelection) {
        if (this.baseFaceSelection != faceSelection) {
            FaceSelectionData.FaceOption oldSelection = this.baseFaceSelection;
            this.baseFaceSelection = faceSelection;

            DebugConfig.logUserAction("Base face: " + oldSelection + " → " + faceSelection);

            if (!suppressChangeNotifications) {
                applyCurrentTextureSettings();
            }
            return true;
        }
        return false;
    }

    public boolean setToggleFaceSelection(@Nonnull FaceSelectionData.FaceOption faceSelection) {
        if (this.toggleFaceSelection != faceSelection) {
            FaceSelectionData.FaceOption oldSelection = this.toggleFaceSelection;
            this.toggleFaceSelection = faceSelection;

            DebugConfig.logUserAction("Toggle face: " + oldSelection + " → " + faceSelection);

            if (!suppressChangeNotifications) {
                applyCurrentTextureSettings();
            }
            return true;
        }
        return false;
    }

    public boolean setInverted(boolean inverted) {
        if (this.inverted != inverted) {
            this.inverted = inverted;

            DebugConfig.logUserAction("Inverted: " + inverted);

            if (!suppressChangeNotifications) {
                applyCurrentTextureSettings();
            }
            return true;
        }
        return false;
    }

    // ========================================
    // BLOCK ANALYSIS WITH CACHING
    // ========================================

    private BlockTextureAnalyzer.BlockTextureInfo cachedBaseAnalysis = null;
    private BlockTextureAnalyzer.BlockTextureInfo cachedToggleAnalysis = null;
    private ItemStack lastAnalyzedBase = ItemStack.EMPTY;
    private ItemStack lastAnalyzedToggle = ItemStack.EMPTY;

    @Nonnull
    public BlockTextureAnalyzer.BlockTextureInfo getBaseBlockAnalysis() {
        if (cachedBaseAnalysis == null || !ItemStack.matches(lastAnalyzedBase, guiBaseItem)) {
            cachedBaseAnalysis = BlockTextureAnalyzer.analyzeBlock(guiBaseItem);
            lastAnalyzedBase = guiBaseItem.copy();
        }
        return cachedBaseAnalysis;
    }

    @Nonnull
    public BlockTextureAnalyzer.BlockTextureInfo getToggleBlockAnalysis() {
        if (cachedToggleAnalysis == null || !ItemStack.matches(lastAnalyzedToggle, guiToggleItem)) {
            cachedToggleAnalysis = BlockTextureAnalyzer.analyzeBlock(guiToggleItem);
            lastAnalyzedToggle = guiToggleItem.copy();
        }
        return cachedToggleAnalysis;
    }

    @Nonnull
    public FaceSelectionData.DropdownState getBaseDropdownState() {
        if (guiBaseItem.isEmpty()) {
            return FaceSelectionData.createDisabledState();
        }

        BlockTextureAnalyzer.BlockTextureInfo blockInfo = getBaseBlockAnalysis();
        return FaceSelectionData.createDropdownState(blockInfo, baseFaceSelection);
    }

    @Nonnull
    public FaceSelectionData.DropdownState getToggleDropdownState() {
        if (guiToggleItem.isEmpty()) {
            return FaceSelectionData.createDisabledState();
        }

        BlockTextureAnalyzer.BlockTextureInfo blockInfo = getToggleBlockAnalysis();
        return FaceSelectionData.createDropdownState(blockInfo, toggleFaceSelection);
    }

    // ========================================
    // GETTERS
    // ========================================

    @Nonnull public String getBaseTexture() { return baseTexturePath; }
    @Nonnull public String getToggleTexture() { return toggleTexturePath; }
    @Nonnull public FaceSelectionData.FaceOption getBaseFaceSelection() { return baseFaceSelection; }
    @Nonnull public FaceSelectionData.FaceOption getToggleFaceSelection() { return toggleFaceSelection; }
    public boolean isInverted() { return inverted; }
    @Nonnull public ItemStack getGuiToggleItem() { return guiToggleItem; }
    @Nonnull public ItemStack getGuiBaseItem() { return guiBaseItem; }

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

    public void setGuiSlotItems(@Nonnull ItemStack toggleItem, @Nonnull ItemStack baseItem) {
        // Clear analysis cache if items changed
        if (!ItemStack.matches(this.guiToggleItem, toggleItem)) {
            cachedToggleAnalysis = null;
        }
        if (!ItemStack.matches(this.guiBaseItem, baseItem)) {
            cachedBaseAnalysis = null;
        }

        this.guiToggleItem = toggleItem.copy();
        this.guiBaseItem = baseItem.copy();

        // Apply with preserved face selections
        applyCurrentTextureSettings();
    }

    public void dropStoredTextures(@Nonnull Level level, @Nonnull BlockPos pos) {
        if (!guiToggleItem.isEmpty()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), guiToggleItem);
        }

        if (!guiBaseItem.isEmpty()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), guiBaseItem);
        }
    }

    // ========================================
    // FRAMED BLOCKS APPROACH: BULLETPROOF NBT PERSISTENCE
    // ========================================

    @Override
    protected void saveAdditional(@Nonnull CompoundTag nbt) {
        super.saveAdditional(nbt);

        // Version for future compatibility
        nbt.putInt(NBT_VERSION_KEY, CURRENT_NBT_VERSION);

        // Core texture data - ALWAYS preserved
        nbt.putString(BASE_TEXTURE_KEY, baseTexturePath);
        nbt.putString(TOGGLE_TEXTURE_KEY, toggleTexturePath);

        // Face selections - CRITICAL data that must survive lever toggles
        nbt.putString(BASE_FACE_KEY, baseFaceSelection.getSerializedName());
        nbt.putString(TOGGLE_FACE_KEY, toggleFaceSelection.getSerializedName());

        // Inversion state
        nbt.putBoolean(INVERTED_KEY, inverted);

        // GUI slot items with comprehensive storage
        if (!guiToggleItem.isEmpty()) {
            CompoundTag toggleItemTag = new CompoundTag();
            guiToggleItem.save(toggleItemTag);
            nbt.put(GUI_TOGGLE_ITEM_KEY, toggleItemTag);
        }

        if (!guiBaseItem.isEmpty()) {
            CompoundTag baseItemTag = new CompoundTag();
            guiBaseItem.save(baseItemTag);
            nbt.put(GUI_BASE_ITEM_KEY, baseItemTag);
        }

        // Critical logging for diagnosis
        DebugConfig.logPersistence("SAVED v" + CURRENT_NBT_VERSION +
                " - Base: " + baseFaceSelection + ", Toggle: " + toggleFaceSelection);
    }

    @Override
    public void load(@Nonnull CompoundTag nbt) {
        super.load(nbt);

        // Handle version compatibility
        int version = nbt.getInt(NBT_VERSION_KEY);
        if (version != CURRENT_NBT_VERSION) {
            DebugConfig.logCritical("NBT version mismatch: expected " + CURRENT_NBT_VERSION + ", got " + version);
        }

        // Load texture paths with validation
        String loadedBasePath = nbt.getString(BASE_TEXTURE_KEY);
        String loadedTogglePath = nbt.getString(TOGGLE_TEXTURE_KEY);

        this.baseTexturePath = loadedBasePath.isEmpty() ? DEFAULT_BASE_TEXTURE : loadedBasePath;
        this.toggleTexturePath = loadedTogglePath.isEmpty() ? DEFAULT_TOGGLE_TEXTURE : loadedTogglePath;

        // CRITICAL: Face selection loading with robust error handling
        String baseFaceName = nbt.getString(BASE_FACE_KEY);
        if (!baseFaceName.isEmpty()) {
            try {
                this.baseFaceSelection = FaceSelectionData.FaceOption.fromSerializedName(baseFaceName);
            } catch (Exception e) {
                DebugConfig.logCritical("Failed to load base face selection: " + baseFaceName);
                this.baseFaceSelection = FaceSelectionData.FaceOption.ALL;
            }
        } else {
            this.baseFaceSelection = FaceSelectionData.FaceOption.ALL;
        }

        String toggleFaceName = nbt.getString(TOGGLE_FACE_KEY);
        if (!toggleFaceName.isEmpty()) {
            try {
                this.toggleFaceSelection = FaceSelectionData.FaceOption.fromSerializedName(toggleFaceName);
            } catch (Exception e) {
                DebugConfig.logCritical("Failed to load toggle face selection: " + toggleFaceName);
                this.toggleFaceSelection = FaceSelectionData.FaceOption.ALL;
            }
        } else {
            this.toggleFaceSelection = FaceSelectionData.FaceOption.ALL;
        }

        // Load inversion state
        this.inverted = nbt.getBoolean(INVERTED_KEY);

        // Load GUI slot items with error handling
        if (nbt.contains(GUI_TOGGLE_ITEM_KEY)) {
            try {
                this.guiToggleItem = ItemStack.of(nbt.getCompound(GUI_TOGGLE_ITEM_KEY));
            } catch (Exception e) {
                DebugConfig.logCritical("Failed to load toggle item: " + e.getMessage());
                this.guiToggleItem = ItemStack.EMPTY;
            }
        } else {
            this.guiToggleItem = ItemStack.EMPTY;
        }

        if (nbt.contains(GUI_BASE_ITEM_KEY)) {
            try {
                this.guiBaseItem = ItemStack.of(nbt.getCompound(GUI_BASE_ITEM_KEY));
            } catch (Exception e) {
                DebugConfig.logCritical("Failed to load base item: " + e.getMessage());
                this.guiBaseItem = ItemStack.EMPTY;
            }
        } else {
            this.guiBaseItem = ItemStack.EMPTY;
        }

        // Clear analysis cache after loading
        cachedBaseAnalysis = null;
        cachedToggleAnalysis = null;

        // Critical logging for diagnosis
        DebugConfig.logPersistence("LOADED v" + version +
                " - Base: " + baseFaceSelection + ", Toggle: " + toggleFaceSelection);

        // Mark for client sync if on server
        if (level != null && !level.isClientSide) {
            needsClientSync = true;
        }
    }

    // ========================================
    // ENHANCED CLIENT-SERVER SYNCHRONIZATION
    // ========================================

    @Override
    @Nonnull
    public CompoundTag getUpdateTag() {
        CompoundTag nbt = super.getUpdateTag();

        // Include ALL data for chunk loading sync
        nbt.putInt(NBT_VERSION_KEY, CURRENT_NBT_VERSION);
        nbt.putString(BASE_TEXTURE_KEY, baseTexturePath);
        nbt.putString(TOGGLE_TEXTURE_KEY, toggleTexturePath);
        nbt.putString(BASE_FACE_KEY, baseFaceSelection.getSerializedName());
        nbt.putString(TOGGLE_FACE_KEY, toggleFaceSelection.getSerializedName());
        nbt.putBoolean(INVERTED_KEY, inverted);

        if (!guiToggleItem.isEmpty()) {
            nbt.put(GUI_TOGGLE_ITEM_KEY, guiToggleItem.save(new CompoundTag()));
        }
        if (!guiBaseItem.isEmpty()) {
            nbt.put(GUI_BASE_ITEM_KEY, guiBaseItem.save(new CompoundTag()));
        }

        return nbt;
    }

    @Override
    @Nullable
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(@Nonnull Connection net, @Nonnull ClientboundBlockEntityDataPacket pkt) {
        CompoundTag receivedData = pkt.getTag();
        if (receivedData != null) {
            CompoundTag oldData = saveWithoutMetadata();
            load(receivedData);

            // Only trigger client updates if data actually changed
            if (!receivedData.equals(oldData)) {
                requestModelDataUpdate();
                needsClientSync = false;
            }
        }
    }

    @Override
    public void handleUpdateTag(@Nonnull CompoundTag tag) {
        load(tag);
        requestModelDataUpdate();
    }

    // ========================================
    // RESET FUNCTIONALITY
    // ========================================

    public void resetTextures() {
        suppressChangeNotifications = true;

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

        suppressChangeNotifications = false;

        if (changed) {
            triggerCompleteSync();
        }
    }

    // ========================================
    // TICK METHODS
    // ========================================

    public static void clientTick(Level level, BlockPos pos, BlockState state, SwitchesLeverBlockEntity blockEntity) {
        // Handle any pending client sync
        if (blockEntity.needsClientSync) {
            blockEntity.requestModelDataUpdate();
            blockEntity.needsClientSync = false;
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SwitchesLeverBlockEntity blockEntity) {
        // Handle any pending server sync
        if (blockEntity.needsClientSync) {
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            blockEntity.needsClientSync = false;
        }
    }
}