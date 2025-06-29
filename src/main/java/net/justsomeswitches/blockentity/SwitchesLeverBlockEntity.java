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
 * Enhanced Block Entity for Switches Lever - Phase 4B
 * ---
 * This BlockEntity provides comprehensive NBT-based storage for individual switch blocks,
 * including advanced face selection and dynamic texture analysis capabilities.
 * ---
 * Phase 4B enhancements:
 * - Face selection storage for each texture slot
 * - Dynamic block analysis integration
 * - Enhanced texture preview capabilities
 * - Professional error handling and validation
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

    // NBT keys for face selection storage (Phase 4B)
    private static final String BASE_FACE_KEY = "base_face_selection";
    private static final String TOGGLE_FACE_KEY = "toggle_face_selection";

    // NBT keys for inversion state (Phase 4B)
    private static final String INVERTED_KEY = "inverted_state";

    // Current texture paths
    private String baseTexturePath = DEFAULT_BASE_TEXTURE;
    private String toggleTexturePath = DEFAULT_TOGGLE_TEXTURE;

    // Face selections (Phase 4B)
    private FaceSelectionData.FaceOption baseFaceSelection = FaceSelectionData.FaceOption.ALL;
    private FaceSelectionData.FaceOption toggleFaceSelection = FaceSelectionData.FaceOption.ALL;

    // Inversion state (Phase 4B)
    private boolean inverted = false;

    // ========================================
    // PHASE 4B: ENHANCED MODEL DATA INTEGRATION
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
        System.out.println("Phase 4B Debug: Enhanced SwitchesLeverBlockEntity created at " + pos);
    }

    // ========================================
    // CLIENT AND SERVER TICK METHODS
    // ========================================

    /**
     * Client-side tick method for any client-specific updates
     */
    public static void clientTick(Level level, BlockPos pos, BlockState state, SwitchesLeverBlockEntity blockEntity) {
        // Client-side logic can be added here if needed
        // Currently no client-specific ticking required
    }

    /**
     * Server-side tick method for any server-specific updates
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, SwitchesLeverBlockEntity blockEntity) {
        // Server-side logic can be added here if needed
        // Currently no server-specific ticking required
    }

    // ========================================
    // PHASE 4B: ENHANCED TEXTURE MANAGEMENT
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
                String texturePath = blockId.getNamespace() + ":block/" + blockId.getPath();
                System.out.println("Phase 4B Debug: Extracted texture path: " + texturePath + " from item: " + blockId);
                return texturePath;
            }
        } catch (Exception e) {
            System.err.println("Phase 4B Error: Failed to extract texture from item: " + e.getMessage());
        }

        return "";
    }

    /**
     * Mark BlockEntity as dirty and trigger client synchronization
     */
    private void markDirtyAndSync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            requestModelDataUpdate();
            System.out.println("Phase 4B Debug: BlockEntity marked dirty and synced at " + worldPosition);
        }
    }

    /**
     * Set the base texture for the switch (String version)
     */
    public boolean setBaseTexture(@Nonnull String texturePath) {
        if (!texturePath.equals(this.baseTexturePath)) {
            this.baseTexturePath = texturePath;
            markDirtyAndSync();
            System.out.println("Phase 4B Debug: BaseTexture updated to: " + texturePath);
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
            this.toggleTexturePath = texturePath;
            markDirtyAndSync();
            System.out.println("Phase 4B Debug: ToggleTexture updated to: " + texturePath);
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
    // PHASE 4B: FACE SELECTION MANAGEMENT
    // ========================================

    /**
     * Set base face selection
     */
    public boolean setBaseFaceSelection(@Nonnull FaceSelectionData.FaceOption faceOption) {
        if (this.baseFaceSelection != faceOption) {
            this.baseFaceSelection = faceOption;
            markDirtyAndSync();
            System.out.println("Phase 4B Debug: Base face selection updated to: " + faceOption.getDisplayName());
            return true;
        }
        return false;
    }

    /**
     * Set toggle face selection
     */
    public boolean setToggleFaceSelection(@Nonnull FaceSelectionData.FaceOption faceOption) {
        if (this.toggleFaceSelection != faceOption) {
            this.toggleFaceSelection = faceOption;
            markDirtyAndSync();
            System.out.println("Phase 4B Debug: Toggle face selection updated to: " + faceOption.getDisplayName());
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
            System.out.println("Phase 4B Debug: Inversion state updated to: " + inverted);
            return true;
        }
        return false;
    }

    // ========================================
    // PHASE 4B: BLOCK ANALYSIS INTEGRATION
    // ========================================

    /**
     * Get block analysis for base texture slot
     */
    @Nonnull
    public BlockTextureAnalyzer.BlockTextureInfo getBaseBlockAnalysis() {
        return BlockTextureAnalyzer.analyzeBlock(guiBaseItem);
    }

    /**
     * Get block analysis for toggle texture slot
     */
    @Nonnull
    public BlockTextureAnalyzer.BlockTextureInfo getToggleBlockAnalysis() {
        return BlockTextureAnalyzer.analyzeBlock(guiToggleItem);
    }

    /**
     * Get dropdown state for base texture slot
     */
    @Nonnull
    public FaceSelectionData.DropdownState getBaseDropdownState() {
        BlockTextureAnalyzer.BlockTextureInfo blockInfo = getBaseBlockAnalysis();
        return FaceSelectionData.createDropdownState(blockInfo, baseFaceSelection);
    }

    /**
     * Get dropdown state for toggle texture slot
     */
    @Nonnull
    public FaceSelectionData.DropdownState getToggleDropdownState() {
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
            markDirtyAndSync();
            System.out.println("Phase 4B Debug: All customizations reset to defaults");
        }
    }

    /**
     * Reset base texture to default (for GUI compatibility)
     */
    public void resetBaseTexture() {
        setBaseTexture(DEFAULT_BASE_TEXTURE);
        setBaseFaceSelection(FaceSelectionData.FaceOption.ALL);
    }

    /**
     * Reset toggle texture to default (for GUI compatibility)
     */
    public void resetToggleTexture() {
        setToggleTexture(DEFAULT_TOGGLE_TEXTURE);
        setToggleFaceSelection(FaceSelectionData.FaceOption.ALL);
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
    // GUI SLOT MANAGEMENT (For GUI Compatibility)
    // ========================================

    // GUI slot storage
    private ItemStack guiToggleItem = ItemStack.EMPTY;
    private ItemStack guiBaseItem = ItemStack.EMPTY;

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
        this.guiToggleItem = toggleItem.copy();
        this.guiBaseItem = baseItem.copy();
        System.out.println("Phase 4B Debug: GUI slot items updated - Toggle: " + toggleItem + ", Base: " + baseItem);
    }

    /**
     * Apply textures from GUI slots to the switch
     */
    public void applyTexturesFromGui() {
        boolean textureChanged = false;

        // Apply toggle texture
        if (!guiToggleItem.isEmpty()) {
            textureChanged |= setToggleTexture(guiToggleItem);
        } else {
            textureChanged |= setToggleTexture(DEFAULT_TOGGLE_TEXTURE);
            setToggleFaceSelection(FaceSelectionData.FaceOption.ALL);
        }

        // Apply base texture
        if (!guiBaseItem.isEmpty()) {
            textureChanged |= setBaseTexture(guiBaseItem);
        } else {
            textureChanged |= setBaseTexture(DEFAULT_BASE_TEXTURE);
            setBaseFaceSelection(FaceSelectionData.FaceOption.ALL);
        }

        if (textureChanged) {
            System.out.println("Phase 4B Debug: Textures applied from GUI at " + worldPosition);
        }
    }

    /**
     * Drop stored texture blocks when switch is broken
     */
    public void dropStoredTextures(@Nonnull Level level, @Nonnull BlockPos pos) {
        if (!guiToggleItem.isEmpty()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), guiToggleItem);
            System.out.println("Phase 4B Debug: Dropped toggle texture item: " + guiToggleItem);
        }

        if (!guiBaseItem.isEmpty()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), guiBaseItem);
            System.out.println("Phase 4B Debug: Dropped base texture item: " + guiBaseItem);
        }
    }

    // ========================================
    // ENHANCED NBT SERIALIZATION (PHASE 4B)
    // ========================================

    @Override
    protected void saveAdditional(@Nonnull CompoundTag nbt) {
        super.saveAdditional(nbt);

        // Save texture paths
        nbt.putString(BASE_TEXTURE_KEY, baseTexturePath);
        nbt.putString(TOGGLE_TEXTURE_KEY, toggleTexturePath);

        // Save face selections (Phase 4B)
        nbt.putString(BASE_FACE_KEY, baseFaceSelection.getSerializedName());
        nbt.putString(TOGGLE_FACE_KEY, toggleFaceSelection.getSerializedName());

        // Save inversion state (Phase 4B)
        nbt.putBoolean(INVERTED_KEY, inverted);

        // Save GUI slot items
        if (!guiToggleItem.isEmpty()) {
            nbt.put("gui_toggle_item", guiToggleItem.save(new CompoundTag()));
        }
        if (!guiBaseItem.isEmpty()) {
            nbt.put("gui_base_item", guiBaseItem.save(new CompoundTag()));
        }

        System.out.println("Phase 4B Debug: Enhanced NBT saved - Base: " + baseTexturePath +
                ", Toggle: " + toggleTexturePath + ", BaseFace: " + baseFaceSelection.getDisplayName() +
                ", ToggleFace: " + toggleFaceSelection.getDisplayName() + ", Inverted: " + inverted);
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

        // Load face selections (Phase 4B)
        String baseFaceName = nbt.getString(BASE_FACE_KEY);
        this.baseFaceSelection = baseFaceName.isEmpty() ? FaceSelectionData.FaceOption.ALL :
                FaceSelectionData.FaceOption.fromSerializedName(baseFaceName);

        String toggleFaceName = nbt.getString(TOGGLE_FACE_KEY);
        this.toggleFaceSelection = toggleFaceName.isEmpty() ? FaceSelectionData.FaceOption.ALL :
                FaceSelectionData.FaceOption.fromSerializedName(toggleFaceName);

        // Load inversion state (Phase 4B)
        this.inverted = nbt.getBoolean(INVERTED_KEY);

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

        System.out.println("Phase 4B Debug: Enhanced NBT loaded - Base: " + baseTexturePath +
                ", Toggle: " + toggleTexturePath + ", BaseFace: " + baseFaceSelection.getDisplayName() +
                ", ToggleFace: " + toggleFaceSelection.getDisplayName() + ", Inverted: " + inverted);
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

        System.out.println("Phase 4B Debug: Enhanced update tag created for client sync");
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
            System.out.println("Phase 4B Debug: Received enhanced data packet from server");
        }
    }
}