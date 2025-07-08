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
 * DEBUGGING VERSION: Targeted logging to identify auto-apply failure
 */
public class SwitchesLeverBlockEntity extends BlockEntity {

    public static final String DEFAULT_BASE_TEXTURE = "minecraft:block/stone";
    public static final String DEFAULT_TOGGLE_TEXTURE = "minecraft:block/oak_planks";

    private static final String NBT_VERSION_KEY = "nbt_version";
    private static final int CURRENT_NBT_VERSION = 2;
    private static final String BASE_TEXTURE_KEY = "base_texture_path";
    private static final String TOGGLE_TEXTURE_KEY = "toggle_texture_path";
    private static final String BASE_FACE_KEY = "base_face_selection";
    private static final String TOGGLE_FACE_KEY = "toggle_face_selection";
    private static final String INVERTED_KEY = "inverted_state";
    private static final String GUI_TOGGLE_ITEM_KEY = "gui_toggle_item";
    private static final String GUI_BASE_ITEM_KEY = "gui_base_item";

    // Core texture data
    private String baseTexturePath = DEFAULT_BASE_TEXTURE;
    private String toggleTexturePath = DEFAULT_TOGGLE_TEXTURE;
    private FaceSelectionData.FaceOption baseFaceSelection = FaceSelectionData.FaceOption.ALL;
    private FaceSelectionData.FaceOption toggleFaceSelection = FaceSelectionData.FaceOption.ALL;
    private boolean inverted = false;

    // GUI slot items
    private ItemStack guiToggleItem = ItemStack.EMPTY;
    private ItemStack guiBaseItem = ItemStack.EMPTY;

    // Analysis cache
    private BlockTextureAnalyzer.BlockTextureInfo cachedBaseAnalysis = null;
    private BlockTextureAnalyzer.BlockTextureInfo cachedToggleAnalysis = null;
    private ItemStack lastAnalyzedBase = ItemStack.EMPTY;
    private ItemStack lastAnalyzedToggle = ItemStack.EMPTY;

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
    // DEBUGGING: AUTO-APPLY SYSTEM WITH LOGGING
    // ========================================

    /**
     * DEBUGGING: Apply textures with detailed logging
     */
    private void applyTexturesImmediately() {
        System.out.println("DEBUG AUTO-APPLY: Starting applyTexturesImmediately()");
        System.out.println("DEBUG AUTO-APPLY: Current base texture: " + baseTexturePath);
        System.out.println("DEBUG AUTO-APPLY: Current toggle texture: " + toggleTexturePath);
        System.out.println("DEBUG AUTO-APPLY: Base face selection: " + baseFaceSelection);
        System.out.println("DEBUG AUTO-APPLY: Toggle face selection: " + toggleFaceSelection);
        System.out.println("DEBUG AUTO-APPLY: Base item: " + (guiBaseItem.isEmpty() ? "EMPTY" : guiBaseItem.getItem().toString()));
        System.out.println("DEBUG AUTO-APPLY: Toggle item: " + (guiToggleItem.isEmpty() ? "EMPTY" : guiToggleItem.getItem().toString()));

        boolean textureChanged = false;

        // Apply toggle texture with face selection
        String newToggleTexture = getEffectiveTextureForSlot(guiToggleItem, toggleFaceSelection, "TOGGLE");
        System.out.println("DEBUG AUTO-APPLY: Calculated toggle texture: " + newToggleTexture);
        if (!this.toggleTexturePath.equals(newToggleTexture)) {
            System.out.println("DEBUG AUTO-APPLY: TOGGLE TEXTURE CHANGED: " + this.toggleTexturePath + " -> " + newToggleTexture);
            this.toggleTexturePath = newToggleTexture;
            textureChanged = true;
        } else {
            System.out.println("DEBUG AUTO-APPLY: Toggle texture unchanged");
        }

        // Apply base texture with face selection
        String newBaseTexture = getEffectiveTextureForSlot(guiBaseItem, baseFaceSelection, "BASE");
        System.out.println("DEBUG AUTO-APPLY: Calculated base texture: " + newBaseTexture);
        if (!this.baseTexturePath.equals(newBaseTexture)) {
            System.out.println("DEBUG AUTO-APPLY: BASE TEXTURE CHANGED: " + this.baseTexturePath + " -> " + newBaseTexture);
            this.baseTexturePath = newBaseTexture;
            textureChanged = true;
        } else {
            System.out.println("DEBUG AUTO-APPLY: Base texture unchanged");
        }

        if (textureChanged) {
            System.out.println("DEBUG AUTO-APPLY: TRIGGERING SYNC");
            triggerImmediateSync();
        } else {
            System.out.println("DEBUG AUTO-APPLY: NO CHANGES - NO SYNC");
        }
    }

    /**
     * DEBUGGING: Get effective texture with logging
     */
    @Nonnull
    private String getEffectiveTextureForSlot(@Nonnull ItemStack item, @Nonnull FaceSelectionData.FaceOption faceSelection, String slotName) {
        System.out.println("DEBUG TEXTURE-CALC (" + slotName + "): Item=" + (item.isEmpty() ? "EMPTY" : item.getItem().toString()) + ", Face=" + faceSelection);

        if (item.isEmpty()) {
            System.out.println("DEBUG TEXTURE-CALC (" + slotName + "): Item empty, returning default");
            return DEFAULT_BASE_TEXTURE;
        }

        BlockTextureAnalyzer.BlockTextureInfo blockInfo = BlockTextureAnalyzer.analyzeBlock(item);
        System.out.println("DEBUG TEXTURE-CALC (" + slotName + "): Block analysis completed");

        String faceTexturePath = FaceSelectionData.getTextureForSelection(blockInfo, faceSelection);
        System.out.println("DEBUG TEXTURE-CALC (" + slotName + "): Face texture path: " + faceTexturePath);

        if (faceTexturePath != null && BlockTextureAnalyzer.isValidTexture(faceTexturePath)) {
            System.out.println("DEBUG TEXTURE-CALC (" + slotName + "): Using face texture: " + faceTexturePath);
            return faceTexturePath;
        }

        String fallbackPath = blockInfo.getUniformTexture();
        String result = fallbackPath != null ? fallbackPath : DEFAULT_BASE_TEXTURE;
        System.out.println("DEBUG TEXTURE-CALC (" + slotName + "): Using fallback: " + result);
        return result;
    }

    /**
     * DEBUGGING: Immediate sync with logging
     */
    private void triggerImmediateSync() {
        System.out.println("DEBUG SYNC: Starting triggerImmediateSync()");

        if (level != null) {
            setChanged();
            System.out.println("DEBUG SYNC: setChanged() called");

            if (!level.isClientSide) {
                BlockState currentState = getBlockState();
                level.sendBlockUpdated(worldPosition, currentState, currentState, Block.UPDATE_ALL);
                System.out.println("DEBUG SYNC: sendBlockUpdated() called on server");
            } else {
                System.out.println("DEBUG SYNC: Client side - skipping sendBlockUpdated");
            }

            requestModelDataUpdate();
            System.out.println("DEBUG SYNC: requestModelDataUpdate() called");
        } else {
            System.out.println("DEBUG SYNC: Level is null!");
        }
    }

    // ========================================
    // DEBUGGING: FACE SELECTION WITH LOGGING
    // ========================================

    public boolean setBaseFaceSelection(@Nonnull FaceSelectionData.FaceOption faceSelection) {
        System.out.println("DEBUG FACE: setBaseFaceSelection called: " + this.baseFaceSelection + " -> " + faceSelection);
        if (this.baseFaceSelection != faceSelection) {
            this.baseFaceSelection = faceSelection;
            System.out.println("DEBUG FACE: Base face changed, calling applyTexturesImmediately");
            applyTexturesImmediately();
            return true;
        } else {
            System.out.println("DEBUG FACE: Base face unchanged");
            return false;
        }
    }

    public boolean setToggleFaceSelection(@Nonnull FaceSelectionData.FaceOption faceSelection) {
        System.out.println("DEBUG FACE: setToggleFaceSelection called: " + this.toggleFaceSelection + " -> " + faceSelection);
        if (this.toggleFaceSelection != faceSelection) {
            this.toggleFaceSelection = faceSelection;
            System.out.println("DEBUG FACE: Toggle face changed, calling applyTexturesImmediately");
            applyTexturesImmediately();
            return true;
        } else {
            System.out.println("DEBUG FACE: Toggle face unchanged");
            return false;
        }
    }

    public boolean setInverted(boolean inverted) {
        System.out.println("DEBUG FACE: setInverted called: " + this.inverted + " -> " + inverted);
        if (this.inverted != inverted) {
            this.inverted = inverted;
            System.out.println("DEBUG FACE: Inverted changed, calling applyTexturesImmediately");
            applyTexturesImmediately();
            return true;
        } else {
            System.out.println("DEBUG FACE: Inverted unchanged");
            return false;
        }
    }

    // ========================================
    // DEBUGGING: SLOT MANAGEMENT WITH LOGGING
    // ========================================

    public void setGuiSlotItems(@Nonnull ItemStack toggleItem, @Nonnull ItemStack baseItem) {
        System.out.println("DEBUG SLOTS: setGuiSlotItems called");
        System.out.println("DEBUG SLOTS: Old toggle: " + (guiToggleItem.isEmpty() ? "EMPTY" : guiToggleItem.getItem().toString()));
        System.out.println("DEBUG SLOTS: New toggle: " + (toggleItem.isEmpty() ? "EMPTY" : toggleItem.getItem().toString()));
        System.out.println("DEBUG SLOTS: Old base: " + (guiBaseItem.isEmpty() ? "EMPTY" : guiBaseItem.getItem().toString()));
        System.out.println("DEBUG SLOTS: New base: " + (baseItem.isEmpty() ? "EMPTY" : baseItem.getItem().toString()));

        boolean changed = false;

        if (!ItemStack.matches(this.guiToggleItem, toggleItem)) {
            System.out.println("DEBUG SLOTS: Toggle item changed");
            this.guiToggleItem = toggleItem.copy();
            changed = true;
        }

        if (!ItemStack.matches(this.guiBaseItem, baseItem)) {
            System.out.println("DEBUG SLOTS: Base item changed");
            this.guiBaseItem = baseItem.copy();
            changed = true;
        }

        if (changed) {
            System.out.println("DEBUG SLOTS: Items changed, clearing cache and applying textures");
            clearAnalysisCache();
            applyTexturesImmediately();
        } else {
            System.out.println("DEBUG SLOTS: No item changes detected");
        }
    }

    private void clearAnalysisCache() {
        cachedBaseAnalysis = null;
        cachedToggleAnalysis = null;
        lastAnalyzedBase = ItemStack.EMPTY;
        lastAnalyzedToggle = ItemStack.EMPTY;
    }

    // ========================================
    // ANALYSIS AND DROPDOWN STATE
    // ========================================

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

    public void dropStoredTextures(@Nonnull Level level, @Nonnull BlockPos pos) {
        if (!guiToggleItem.isEmpty()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), guiToggleItem);
        }

        if (!guiBaseItem.isEmpty()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), guiBaseItem);
        }
    }

    // ========================================
    // NBT PERSISTENCE
    // ========================================

    @Override
    protected void saveAdditional(@Nonnull CompoundTag nbt) {
        super.saveAdditional(nbt);

        nbt.putInt(NBT_VERSION_KEY, CURRENT_NBT_VERSION);
        nbt.putString(BASE_TEXTURE_KEY, baseTexturePath);
        nbt.putString(TOGGLE_TEXTURE_KEY, toggleTexturePath);
        nbt.putString(BASE_FACE_KEY, baseFaceSelection.getSerializedName());
        nbt.putString(TOGGLE_FACE_KEY, toggleFaceSelection.getSerializedName());
        nbt.putBoolean(INVERTED_KEY, inverted);

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

        System.out.println("DEBUG NBT: SAVED - Base: " + baseFaceSelection + ", Toggle: " + toggleFaceSelection);
    }

    @Override
    public void load(@Nonnull CompoundTag nbt) {
        super.load(nbt);

        int version = nbt.getInt(NBT_VERSION_KEY);

        String loadedBasePath = nbt.getString(BASE_TEXTURE_KEY);
        String loadedTogglePath = nbt.getString(TOGGLE_TEXTURE_KEY);

        this.baseTexturePath = loadedBasePath.isEmpty() ? DEFAULT_BASE_TEXTURE : loadedBasePath;
        this.toggleTexturePath = loadedTogglePath.isEmpty() ? DEFAULT_TOGGLE_TEXTURE : loadedTogglePath;

        String baseFaceName = nbt.getString(BASE_FACE_KEY);
        if (!baseFaceName.isEmpty()) {
            try {
                this.baseFaceSelection = FaceSelectionData.FaceOption.fromSerializedName(baseFaceName);
            } catch (Exception e) {
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
                this.toggleFaceSelection = FaceSelectionData.FaceOption.ALL;
            }
        } else {
            this.toggleFaceSelection = FaceSelectionData.FaceOption.ALL;
        }

        this.inverted = nbt.getBoolean(INVERTED_KEY);

        if (nbt.contains(GUI_TOGGLE_ITEM_KEY)) {
            try {
                this.guiToggleItem = ItemStack.of(nbt.getCompound(GUI_TOGGLE_ITEM_KEY));
            } catch (Exception e) {
                this.guiToggleItem = ItemStack.EMPTY;
            }
        } else {
            this.guiToggleItem = ItemStack.EMPTY;
        }

        if (nbt.contains(GUI_BASE_ITEM_KEY)) {
            try {
                this.guiBaseItem = ItemStack.of(nbt.getCompound(GUI_BASE_ITEM_KEY));
            } catch (Exception e) {
                this.guiBaseItem = ItemStack.EMPTY;
            }
        } else {
            this.guiBaseItem = ItemStack.EMPTY;
        }

        clearAnalysisCache();

        System.out.println("DEBUG NBT: LOADED - Base: " + baseFaceSelection + ", Toggle: " + toggleFaceSelection);
    }

    // ========================================
    // CLIENT-SERVER SYNC
    // ========================================

    @Override
    @Nonnull
    public CompoundTag getUpdateTag() {
        CompoundTag nbt = super.getUpdateTag();
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
            load(receivedData);
            requestModelDataUpdate();
        }
    }

    @Override
    public void handleUpdateTag(@Nonnull CompoundTag tag) {
        load(tag);
        requestModelDataUpdate();
    }

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
            triggerImmediateSync();
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, SwitchesLeverBlockEntity blockEntity) {
        // Minimal client tick
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SwitchesLeverBlockEntity blockEntity) {
        // Minimal server tick
    }
}