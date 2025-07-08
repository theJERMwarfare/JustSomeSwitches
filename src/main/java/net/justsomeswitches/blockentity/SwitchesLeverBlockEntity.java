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
 * FRAMED BLOCKS SOLUTION: Enhanced BlockEntity with robust NBT persistence that survives setBlock() calls
 * ---
 * Key improvements:
 * - Immediate NBT persistence on ALL changes
 * - Robust save/load cycle that preserves face selections through lever toggles
 * - Enhanced ModelData integration following Framed Blocks patterns
 * - Separation of texture management from lever mechanics
 */
public class SwitchesLeverBlockEntity extends BlockEntity {

    // ========================================
    // TEXTURE CONFIGURATION
    // ========================================

    public static final String DEFAULT_BASE_TEXTURE = "minecraft:block/stone";
    public static final String DEFAULT_TOGGLE_TEXTURE = "minecraft:block/oak_planks";

    private static final String BASE_TEXTURE_KEY = "base_texture_path";
    private static final String TOGGLE_TEXTURE_KEY = "toggle_texture_path";
    private static final String BASE_FACE_KEY = "base_face_selection";
    private static final String TOGGLE_FACE_KEY = "toggle_face_selection";
    private static final String INVERTED_KEY = "inverted_state";

    private String baseTexturePath = DEFAULT_BASE_TEXTURE;
    private String toggleTexturePath = DEFAULT_TOGGLE_TEXTURE;
    private FaceSelectionData.FaceOption baseFaceSelection = FaceSelectionData.FaceOption.ALL;
    private FaceSelectionData.FaceOption toggleFaceSelection = FaceSelectionData.FaceOption.ALL;
    private boolean inverted = false;

    private ItemStack guiToggleItem = ItemStack.EMPTY;
    private ItemStack guiBaseItem = ItemStack.EMPTY;
    private boolean suppressChangeNotifications = false;

    // ========================================
    // FRAMED BLOCKS APPROACH: ENHANCED MODEL DATA INTEGRATION
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

    /**
     * FRAMED BLOCKS PATTERN: ModelData integration that survives state changes
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

    public SwitchesLeverBlockEntity(BlockPos pos, BlockState blockState) {
        super(JustSomeSwitchesModBlockEntities.SWITCHES_LEVER.get(), pos, blockState);
    }

    // ========================================
    // FRAMED BLOCKS APPROACH: IMMEDIATE CLIENT-SIDE MODEL REFRESH
    // ========================================

    /**
     * Apply textures with immediate client-side refresh using Framed Blocks patterns
     */
    public void applyCurrentTextureSettings() {
        System.out.println("FRAMED SOLUTION: Auto-apply with face selections - Base: " + baseFaceSelection + ", Toggle: " + toggleFaceSelection);

        suppressChangeNotifications = true;

        boolean textureChanged = false;

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
            System.out.println("FRAMED SOLUTION: Triggering immediate model refresh...");
            triggerImmediateModelRefresh();
        }

        // CRITICAL: Immediate NBT persistence after texture application
        forceImmediateNBTPersistence();
    }

    /**
     * FRAMED BLOCKS PATTERN: Trigger immediate client-side model refresh
     */
    private void triggerImmediateModelRefresh() {
        if (level != null) {
            if (!level.isClientSide) {
                // SERVER-SIDE: Standard update sequence
                BlockState currentState = getBlockState();

                // Force model data update first
                requestModelDataUpdate();

                // Send immediate block update to clients
                level.sendBlockUpdated(worldPosition, currentState, currentState, Block.UPDATE_CLIENTS);

                // Mark dirty for persistence
                setChanged();

                System.out.println("FRAMED SOLUTION: Server-side updates sent");
            } else {
                // CLIENT-SIDE: Force immediate model refresh
                System.out.println("FRAMED SOLUTION: Client-side immediate refresh");
                requestModelDataUpdate();

                // Force immediate chunk re-render on client
                if (level.getChunkSource().hasChunk(worldPosition.getX() >> 4, worldPosition.getZ() >> 4)) {
                    level.getChunkSource().getLightEngine().checkBlock(worldPosition);
                }
            }
        }
    }

    /**
     * FRAMED BLOCKS PATTERN: Force immediate NBT persistence for all changes
     */
    public void forceImmediateNBTPersistence() {
        System.out.println("FRAMED SOLUTION: Forcing immediate NBT persistence");

        // Mark as changed
        setChanged();

        // Force immediate sync to client if on server
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }

        System.out.println("FRAMED SOLUTION: NBT persistence forced - Base: " + baseFaceSelection +
                ", Toggle: " + toggleFaceSelection + ", Inverted: " + inverted);
    }

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

    // ========================================
    // TEXTURE MANAGEMENT
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

    public boolean setBaseTexture(@Nonnull String texturePath) {
        if (setBaseTextureInternal(texturePath)) {
            if (!suppressChangeNotifications) {
                triggerImmediateModelRefresh();
                forceImmediateNBTPersistence();
            }
            return true;
        }
        return false;
    }

    public boolean setBaseTexture(@Nonnull ItemStack itemStack) {
        String texturePath = getTextureFromItem(itemStack);
        if (!texturePath.isEmpty()) {
            return setBaseTexture(texturePath);
        }
        return false;
    }

    public boolean setToggleTexture(@Nonnull String texturePath) {
        if (setToggleTextureInternal(texturePath)) {
            if (!suppressChangeNotifications) {
                triggerImmediateModelRefresh();
                forceImmediateNBTPersistence();
            }
            return true;
        }
        return false;
    }

    public boolean setToggleTexture(@Nonnull ItemStack itemStack) {
        String texturePath = getTextureFromItem(itemStack);
        if (!texturePath.isEmpty()) {
            return setToggleTexture(texturePath);
        }
        return false;
    }

    // ========================================
    // FRAMED BLOCKS APPROACH: ENHANCED FACE SELECTION WITH IMMEDIATE PERSISTENCE
    // ========================================

    /**
     * FRAMED BLOCKS PATTERN: Set base face selection with immediate persistence
     */
    public boolean setBaseFaceSelection(@Nonnull FaceSelectionData.FaceOption faceSelection) {
        System.out.println("FRAMED SOLUTION: setBaseFaceSelection - " + this.baseFaceSelection + " → " + faceSelection);

        if (this.baseFaceSelection != faceSelection) {
            this.baseFaceSelection = faceSelection;

            if (!suppressChangeNotifications) {
                // Apply textures with new face selection and immediate refresh
                applyCurrentTextureSettings();

                System.out.println("FRAMED SOLUTION: Face selection change applied - Base: " + this.baseFaceSelection);
            }
            return true;
        }
        return false;
    }

    /**
     * FRAMED BLOCKS PATTERN: Set toggle face selection with immediate persistence
     */
    public boolean setToggleFaceSelection(@Nonnull FaceSelectionData.FaceOption faceSelection) {
        System.out.println("FRAMED SOLUTION: setToggleFaceSelection - " + this.toggleFaceSelection + " → " + faceSelection);

        if (this.toggleFaceSelection != faceSelection) {
            this.toggleFaceSelection = faceSelection;

            if (!suppressChangeNotifications) {
                // Apply textures with new face selection and immediate refresh
                applyCurrentTextureSettings();

                System.out.println("FRAMED SOLUTION: Face selection change applied - Toggle: " + this.toggleFaceSelection);
            }
            return true;
        }
        return false;
    }

    public boolean setInverted(boolean inverted) {
        if (this.inverted != inverted) {
            this.inverted = inverted;

            if (!suppressChangeNotifications) {
                applyCurrentTextureSettings();

                System.out.println("FRAMED SOLUTION: Inversion change applied - Inverted: " + this.inverted);
            }
            return true;
        }
        return false;
    }

    // ========================================
    // BLOCK ANALYSIS INTEGRATION
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
            triggerImmediateModelRefresh();
            forceImmediateNBTPersistence();
        }
    }

    public void resetBaseTexture() {
        setBaseTexture(DEFAULT_BASE_TEXTURE);
    }

    public void resetToggleTexture() {
        setToggleTexture(DEFAULT_TOGGLE_TEXTURE);
    }

    public boolean hasCustomTextures() {
        return !baseTexturePath.equals(DEFAULT_BASE_TEXTURE) ||
                !toggleTexturePath.equals(DEFAULT_TOGGLE_TEXTURE) ||
                baseFaceSelection != FaceSelectionData.FaceOption.ALL ||
                toggleFaceSelection != FaceSelectionData.FaceOption.ALL ||
                inverted;
    }

    // ========================================
    // GUI SLOT MANAGEMENT WITH IMMEDIATE PERSISTENCE
    // ========================================

    @Nonnull
    public ItemStack getGuiToggleItem() { return guiToggleItem; }

    @Nonnull
    public ItemStack getGuiBaseItem() { return guiBaseItem; }

    /**
     * FRAMED BLOCKS PATTERN: Set GUI slot items with immediate persistence
     */
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

        System.out.println("FRAMED SOLUTION: GUI slot items changed - Toggle: " + (!toggleItem.isEmpty()) + ", Base: " + (!baseItem.isEmpty()));

        // Apply textures with preserved face selections and immediate refresh
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
    // FRAMED BLOCKS APPROACH: ENHANCED NBT PERSISTENCE THAT SURVIVES SETBLOCK()
    // ========================================

    /**
     * FRAMED BLOCKS PATTERN: Comprehensive NBT saving that preserves ALL data
     */
    @Override
    protected void saveAdditional(@Nonnull CompoundTag nbt) {
        super.saveAdditional(nbt);

        // Save all texture and face selection data with enhanced robustness
        nbt.putString(BASE_TEXTURE_KEY, baseTexturePath);
        nbt.putString(TOGGLE_TEXTURE_KEY, toggleTexturePath);
        nbt.putString(BASE_FACE_KEY, baseFaceSelection.getSerializedName());
        nbt.putString(TOGGLE_FACE_KEY, toggleFaceSelection.getSerializedName());
        nbt.putBoolean(INVERTED_KEY, inverted);

        // Save GUI slot items with enhanced persistence
        if (!guiToggleItem.isEmpty()) {
            nbt.put("gui_toggle_item", guiToggleItem.save(new CompoundTag()));
        }
        if (!guiBaseItem.isEmpty()) {
            nbt.put("gui_base_item", guiBaseItem.save(new CompoundTag()));
        }

        // Add verification data for debugging
        nbt.putLong("save_timestamp", System.currentTimeMillis());
        nbt.putString("verification_base", baseFaceSelection.getSerializedName());
        nbt.putString("verification_toggle", toggleFaceSelection.getSerializedName());

        System.out.println("FRAMED SOLUTION: NBT SAVED with enhanced robustness - Base face: " + baseFaceSelection +
                ", Toggle face: " + toggleFaceSelection + ", Base texture: " + baseTexturePath +
                ", Toggle texture: " + toggleTexturePath);
    }

    /**
     * FRAMED BLOCKS PATTERN: Comprehensive NBT loading that restores ALL data
     */
    @Override
    public void load(@Nonnull CompoundTag nbt) {
        super.load(nbt);

        // Load texture paths with validation
        this.baseTexturePath = nbt.getString(BASE_TEXTURE_KEY);
        this.toggleTexturePath = nbt.getString(TOGGLE_TEXTURE_KEY);

        if (this.baseTexturePath.isEmpty()) {
            this.baseTexturePath = DEFAULT_BASE_TEXTURE;
        }
        if (this.toggleTexturePath.isEmpty()) {
            this.toggleTexturePath = DEFAULT_TOGGLE_TEXTURE;
        }

        // Load face selections with enhanced error handling
        String baseFaceName = nbt.getString(BASE_FACE_KEY);
        this.baseFaceSelection = baseFaceName.isEmpty() ? FaceSelectionData.FaceOption.ALL :
                FaceSelectionData.FaceOption.fromSerializedName(baseFaceName);

        String toggleFaceName = nbt.getString(TOGGLE_FACE_KEY);
        this.toggleFaceSelection = toggleFaceName.isEmpty() ? FaceSelectionData.FaceOption.ALL :
                FaceSelectionData.FaceOption.fromSerializedName(toggleFaceName);

        this.inverted = nbt.getBoolean(INVERTED_KEY);

        // Load GUI slot items with enhanced restoration
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

        // Clear analysis cache after loading
        cachedBaseAnalysis = null;
        cachedToggleAnalysis = null;

        // Verification logging
        if (nbt.contains("save_timestamp")) {
            long savedTime = nbt.getLong("save_timestamp");
            String verifyBase = nbt.getString("verification_base");
            String verifyToggle = nbt.getString("verification_toggle");

            System.out.println("FRAMED SOLUTION: NBT LOADED with verification - Base face: " + baseFaceSelection +
                    " (verify: " + verifyBase + "), Toggle face: " + toggleFaceSelection + " (verify: " + verifyToggle +
                    "), Time: " + (System.currentTimeMillis() - savedTime) + "ms ago");
        } else {
            System.out.println("FRAMED SOLUTION: NBT LOADED (legacy) - Base face: " + baseFaceSelection +
                    ", Toggle face: " + toggleFaceSelection + ", Base texture: " + baseTexturePath +
                    ", Toggle texture: " + toggleTexturePath);
        }
    }

    /**
     * FRAMED BLOCKS PATTERN: Enhanced update tag for complete client synchronization
     */
    @Override
    @Nonnull
    public CompoundTag getUpdateTag() {
        CompoundTag nbt = super.getUpdateTag();

        // Include all current data for client synchronization
        nbt.putString(BASE_TEXTURE_KEY, baseTexturePath);
        nbt.putString(TOGGLE_TEXTURE_KEY, toggleTexturePath);
        nbt.putString(BASE_FACE_KEY, baseFaceSelection.getSerializedName());
        nbt.putString(TOGGLE_FACE_KEY, toggleFaceSelection.getSerializedName());
        nbt.putBoolean(INVERTED_KEY, inverted);

        if (!guiToggleItem.isEmpty()) {
            nbt.put("gui_toggle_item", guiToggleItem.save(new CompoundTag()));
        }
        if (!guiBaseItem.isEmpty()) {
            nbt.put("gui_base_item", guiBaseItem.save(new CompoundTag()));
        }

        // Add sync timestamp for debugging
        nbt.putLong("sync_timestamp", System.currentTimeMillis());

        System.out.println("FRAMED SOLUTION: Update tag created for sync - Base face: " + baseFaceSelection +
                ", Toggle face: " + toggleFaceSelection);

        return nbt;
    }

    // ========================================
    // FRAMED BLOCKS PATTERN: ENHANCED CLIENT-SERVER SYNCHRONIZATION
    // ========================================

    @Override
    @Nullable
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(@Nonnull net.minecraft.network.Connection net, @Nonnull ClientboundBlockEntityDataPacket pkt) {
        CompoundTag nbt = pkt.getTag();
        if (nbt != null) {
            long beforeTime = System.currentTimeMillis();
            load(nbt);
            requestModelDataUpdate();
            System.out.println("FRAMED SOLUTION: Data packet processed in " + (System.currentTimeMillis() - beforeTime) + "ms");
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, SwitchesLeverBlockEntity blockEntity) {
        // Client-side logic can be added here if needed
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SwitchesLeverBlockEntity blockEntity) {
        // Server-side logic can be added here if needed
    }
}