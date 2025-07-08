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
 * FIXED: BlockEntity with proper model update pipeline matching lever toggle behavior
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
    // FIXED: AUTO-APPLY WITH PROPER MODEL UPDATES
    // ========================================

    /**
     * FIXED: Apply textures using exact same update sequence as lever toggle
     */
    public void applyCurrentTextureSettings() {
        suppressChangeNotifications = true;

        if (!guiToggleItem.isEmpty()) {
            String effectiveTexturePath = getEffectiveTexturePathForItem(guiToggleItem, toggleFaceSelection);
            setToggleTextureInternal(effectiveTexturePath);
        } else {
            resetToggleTextureInternal();
        }

        if (!guiBaseItem.isEmpty()) {
            String effectiveTexturePath = getEffectiveTexturePathForItem(guiBaseItem, baseFaceSelection);
            setBaseTextureInternal(effectiveTexturePath);
        } else {
            resetBaseTextureInternal();
        }

        suppressChangeNotifications = false;

        // FIXED: Use exact same update sequence as lever toggle
        triggerFullModelUpdate();
    }

    /**
     * FIXED: Trigger full model update using same sequence as lever toggle
     */
    private void triggerFullModelUpdate() {
        if (level != null && !level.isClientSide) {
            // Get current block state
            BlockState currentState = getBlockState();

            // CRITICAL: Use Block.UPDATE_ALL like lever toggle does
            level.setBlock(worldPosition, currentState, Block.UPDATE_ALL);

            // Request model data update
            requestModelDataUpdate();

            // Send block update to clients
            level.sendBlockUpdated(worldPosition, currentState, currentState, Block.UPDATE_CLIENTS);

            // Mark dirty for persistence
            setChanged();
        }
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

    private void resetBaseTextureInternal() {
        setBaseTextureInternal(DEFAULT_BASE_TEXTURE);
    }

    private void resetToggleTextureInternal() {
        setToggleTextureInternal(DEFAULT_TOGGLE_TEXTURE);
    }

    public boolean setBaseTexture(@Nonnull String texturePath) {
        if (setBaseTextureInternal(texturePath)) {
            if (!suppressChangeNotifications) {
                triggerFullModelUpdate();
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
                triggerFullModelUpdate();
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
    // FIXED: FACE SELECTION WITH PROPER PERSISTENCE
    // ========================================

    /**
     * FIXED: Set base face selection with immediate model update
     */
    public boolean setBaseFaceSelection(@Nonnull FaceSelectionData.FaceOption faceSelection) {
        if (this.baseFaceSelection != faceSelection) {
            this.baseFaceSelection = faceSelection;

            if (!suppressChangeNotifications) {
                // Apply textures with new face selection and trigger full model update
                applyCurrentTextureSettings();
            }
            return true;
        }
        return false;
    }

    /**
     * FIXED: Set toggle face selection with immediate model update
     */
    public boolean setToggleFaceSelection(@Nonnull FaceSelectionData.FaceOption faceSelection) {
        if (this.toggleFaceSelection != faceSelection) {
            this.toggleFaceSelection = faceSelection;

            if (!suppressChangeNotifications) {
                // Apply textures with new face selection and trigger full model update
                applyCurrentTextureSettings();
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
            triggerFullModelUpdate();
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
    // GUI SLOT MANAGEMENT
    // ========================================

    @Nonnull
    public ItemStack getGuiToggleItem() { return guiToggleItem; }

    @Nonnull
    public ItemStack getGuiBaseItem() { return guiBaseItem; }

    /**
     * FIXED: Set GUI slot items without changing face selections
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

        // Apply textures with current face selections (preserve user's face choices)
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
    // FIXED: NBT PERSISTENCE
    // ========================================

    @Override
    protected void saveAdditional(@Nonnull CompoundTag nbt) {
        super.saveAdditional(nbt);

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
    }

    /**
     * FIXED: Load with proper face selection restoration
     */
    @Override
    public void load(@Nonnull CompoundTag nbt) {
        super.load(nbt);

        this.baseTexturePath = nbt.getString(BASE_TEXTURE_KEY);
        this.toggleTexturePath = nbt.getString(TOGGLE_TEXTURE_KEY);

        if (this.baseTexturePath.isEmpty()) {
            this.baseTexturePath = DEFAULT_BASE_TEXTURE;
        }
        if (this.toggleTexturePath.isEmpty()) {
            this.toggleTexturePath = DEFAULT_TOGGLE_TEXTURE;
        }

        // FIXED: Properly restore face selections
        String baseFaceName = nbt.getString(BASE_FACE_KEY);
        this.baseFaceSelection = baseFaceName.isEmpty() ? FaceSelectionData.FaceOption.ALL :
                FaceSelectionData.FaceOption.fromSerializedName(baseFaceName);

        String toggleFaceName = nbt.getString(TOGGLE_FACE_KEY);
        this.toggleFaceSelection = toggleFaceName.isEmpty() ? FaceSelectionData.FaceOption.ALL :
                FaceSelectionData.FaceOption.fromSerializedName(toggleFaceName);

        this.inverted = nbt.getBoolean(INVERTED_KEY);

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
    // UTILITY METHODS
    // ========================================

    @Nonnull
    public CompoundTag saveToNBT() {
        CompoundTag nbt = new CompoundTag();
        saveAdditional(nbt);
        return nbt;
    }

    public void loadFromNBT(@Nonnull CompoundTag nbt) {
        load(nbt);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, SwitchesLeverBlockEntity blockEntity) {
        // Client-side logic can be added here if needed
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SwitchesLeverBlockEntity blockEntity) {
        // Server-side logic can be added here if needed
    }
}