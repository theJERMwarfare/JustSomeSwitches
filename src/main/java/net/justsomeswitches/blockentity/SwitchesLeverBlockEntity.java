package net.justsomeswitches.blockentity;

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
 * Block Entity for Switches Lever - Phase 3B Enhanced
 * ---
 * This BlockEntity provides NBT-based texture storage for individual switch blocks,
 * enabling per-block texture customization with client-server synchronization.
 * ---
 * Phase 3B enhancements:
 * - Enhanced texture management with validation
 * - GUI slot management for texture application
 * - Professional error handling and debugging
 * - Complete NBT serialization with item storage
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

    // Current texture paths
    private String baseTexturePath = DEFAULT_BASE_TEXTURE;
    private String toggleTexturePath = DEFAULT_TOGGLE_TEXTURE;

    // ========================================
    // PHASE 3C: MODEL DATA INTEGRATION
    // ========================================

    /**
     * ModelProperty for passing texture data to custom models
     */
    public static final ModelProperty<SwitchTextureData> TEXTURE_PROPERTY = new ModelProperty<>();

    /**
     * Data class for passing texture information to custom models
     */
    public static class SwitchTextureData {
        private final String baseTexture;
        private final String toggleTexture;

        public SwitchTextureData(String baseTexture, String toggleTexture) {
            this.baseTexture = baseTexture;
            this.toggleTexture = toggleTexture;
        }

        public String getBaseTexture() {
            return baseTexture;
        }

        public String getToggleTexture() {
            return toggleTexture;
        }

        /**
         * Check if using custom textures (different from defaults)
         */
        public boolean hasCustomTextures() {
            return !baseTexture.equals(DEFAULT_BASE_TEXTURE) ||
                    !toggleTexture.equals(DEFAULT_TOGGLE_TEXTURE);
        }
    }

    /**
     * Get ModelData for custom model rendering
     */
    @Override
    @Nonnull
    public ModelData getModelData() {
        return ModelData.builder()
                .with(TEXTURE_PROPERTY, new SwitchTextureData(baseTexturePath, toggleTexturePath))
                .build();
    }

    // ========================================
    // CONSTRUCTOR AND BASIC SETUP
    // ========================================

    public SwitchesLeverBlockEntity(BlockPos pos, BlockState blockState) {
        super(JustSomeSwitchesModBlockEntities.SWITCHES_LEVER.get(), pos, blockState);
        System.out.println("Phase 3C Debug: SwitchesLeverBlockEntity created at " + pos);
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
    // TEXTURE MANAGEMENT METHODS
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
            // FIXED: Use BuiltInRegistries instead of ForgeRegistries for NeoForge 1.20.4
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
            if (blockId != null) {
                String texturePath = blockId.getNamespace() + ":block/" + blockId.getPath();
                System.out.println("Phase 3C Debug: Extracted texture path: " + texturePath + " from item: " + blockId);
                return texturePath;
            }
        } catch (Exception e) {
            System.err.println("Phase 3C Error: Failed to extract texture from item: " + e.getMessage());
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
            // Request model data update for custom models
            requestModelDataUpdate();
            System.out.println("Phase 3C Debug: BlockEntity marked dirty and synced at " + worldPosition);
        }
    }

    /**
     * Set the base texture for the switch (String version)
     */
    public boolean setBaseTexture(@Nonnull String texturePath) {
        if (!texturePath.equals(this.baseTexturePath)) {
            this.baseTexturePath = texturePath;
            markDirtyAndSync();

            System.out.println("Phase 3C Debug: BaseTexture updated to: " + texturePath);
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

            System.out.println("Phase 3C Debug: ToggleTexture updated to: " + texturePath);
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

    /**
     * Get the current base texture path
     */
    @Nonnull
    public String getBaseTexture() {
        return baseTexturePath;
    }

    /**
     * Get the current toggle texture path
     */
    @Nonnull
    public String getToggleTexture() {
        return toggleTexturePath;
    }

    /**
     * Reset textures to defaults
     */
    public void resetTextures() {
        if (!baseTexturePath.equals(DEFAULT_BASE_TEXTURE) || !toggleTexturePath.equals(DEFAULT_TOGGLE_TEXTURE)) {
            this.baseTexturePath = DEFAULT_BASE_TEXTURE;
            this.toggleTexturePath = DEFAULT_TOGGLE_TEXTURE;
            markDirtyAndSync();

            System.out.println("Phase 3C Debug: Textures reset to defaults");
        }
    }

    /**
     * Reset base texture to default (for GUI compatibility)
     */
    public void resetBaseTexture() {
        setBaseTexture(DEFAULT_BASE_TEXTURE);
    }

    /**
     * Reset toggle texture to default (for GUI compatibility)
     */
    public void resetToggleTexture() {
        setToggleTexture(DEFAULT_TOGGLE_TEXTURE);
    }

    /**
     * Check if using custom textures
     */
    public boolean hasCustomTextures() {
        return !baseTexturePath.equals(DEFAULT_BASE_TEXTURE) ||
                !toggleTexturePath.equals(DEFAULT_TOGGLE_TEXTURE);
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
    public ItemStack getGuiToggleItem() {
        return guiToggleItem;
    }

    /**
     * Get base item from GUI slot
     */
    @Nonnull
    public ItemStack getGuiBaseItem() {
        return guiBaseItem;
    }

    /**
     * Set GUI slot items (for GUI compatibility)
     */
    public void setGuiSlotItems(@Nonnull ItemStack toggleItem, @Nonnull ItemStack baseItem) {
        this.guiToggleItem = toggleItem.copy();
        this.guiBaseItem = baseItem.copy();

        System.out.println("Phase 3C Debug: GUI slot items updated - Toggle: " + toggleItem + ", Base: " + baseItem);
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
        }

        // Apply base texture
        if (!guiBaseItem.isEmpty()) {
            textureChanged |= setBaseTexture(guiBaseItem);
        } else {
            textureChanged |= setBaseTexture(DEFAULT_BASE_TEXTURE);
        }

        if (textureChanged) {
            System.out.println("Phase 3C Debug: Textures applied from GUI at " + worldPosition);
        }
    }

    /**
     * Drop stored texture blocks when switch is broken
     */
    public void dropStoredTextures(@Nonnull Level level, @Nonnull BlockPos pos) {
        if (!guiToggleItem.isEmpty()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), guiToggleItem);
            System.out.println("Phase 3C Debug: Dropped toggle texture item: " + guiToggleItem);
        }

        if (!guiBaseItem.isEmpty()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), guiBaseItem);
            System.out.println("Phase 3C Debug: Dropped base texture item: " + guiBaseItem);
        }
    }

    // ========================================
    // NBT SERIALIZATION
    // ========================================

    @Override
    protected void saveAdditional(@Nonnull CompoundTag nbt) {
        super.saveAdditional(nbt);

        // Save texture paths
        nbt.putString(BASE_TEXTURE_KEY, baseTexturePath);
        nbt.putString(TOGGLE_TEXTURE_KEY, toggleTexturePath);

        // Save GUI slot items
        if (!guiToggleItem.isEmpty()) {
            nbt.put("gui_toggle_item", guiToggleItem.save(new CompoundTag()));
        }
        if (!guiBaseItem.isEmpty()) {
            nbt.put("gui_base_item", guiBaseItem.save(new CompoundTag()));
        }

        System.out.println("Phase 3C Debug: Saved NBT - Base: " + baseTexturePath + ", Toggle: " + toggleTexturePath);
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

        System.out.println("Phase 3C Debug: Loaded NBT - Base: " + baseTexturePath + ", Toggle: " + toggleTexturePath);
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
     * Get update tag for client synchronization
     */
    @Override
    @Nonnull
    public CompoundTag getUpdateTag() {
        CompoundTag nbt = super.getUpdateTag();
        nbt.putString(BASE_TEXTURE_KEY, baseTexturePath);
        nbt.putString(TOGGLE_TEXTURE_KEY, toggleTexturePath);

        // Sync GUI slot items
        if (!guiToggleItem.isEmpty()) {
            nbt.put("gui_toggle_item", guiToggleItem.save(new CompoundTag()));
        }
        if (!guiBaseItem.isEmpty()) {
            nbt.put("gui_base_item", guiBaseItem.save(new CompoundTag()));
        }

        System.out.println("Phase 3C Debug: Created update tag for client sync");
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
            // Request model data update when receiving server data
            requestModelDataUpdate();
            System.out.println("Phase 3C Debug: Received data packet from server");
        }
    }
}