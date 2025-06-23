package net.justsomeswitches.blockentity;

import net.justsomeswitches.init.JustSomeSwitchesModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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
 * BlockEntity for Switches Lever with Simplified Texture System
 * ---
 * Phase 3C: Simplified ModelData Integration
 * Uses a simple approach inspired by Framed Blocks but tailored for our texture replacement needs
 */
public class SwitchesLeverBlockEntity extends BlockEntity {

    // ModelData property for our texture information
    public static final ModelProperty<SwitchTextureData> TEXTURE_PROPERTY = new ModelProperty<>();

    // NBT keys for texture storage
    private static final String TOGGLE_TEXTURE_KEY = "toggle_texture_path";
    private static final String BASE_TEXTURE_KEY = "base_texture_path";

    // Default textures
    private static final String DEFAULT_BASE_TEXTURE = "minecraft:block/cobblestone";
    private static final String DEFAULT_TOGGLE_TEXTURE = "minecraft:block/oak_planks";

    // Current texture paths
    private String baseTexturePath = DEFAULT_BASE_TEXTURE;
    private String toggleTexturePath = DEFAULT_TOGGLE_TEXTURE;

    public SwitchesLeverBlockEntity(BlockPos pos, BlockState blockState) {
        super(JustSomeSwitchesModBlockEntities.SWITCHES_LEVER.get(), pos, blockState);
    }

    // ========================================
    // TEXTURE MANAGEMENT
    // ========================================

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
        System.out.println("Phase 3C Debug: GUI slot items set - Toggle: " + toggleItem + ", Base: " + baseItem);
    }

    // ========================================
    // MODEL DATA INTEGRATION (SIMPLIFIED)
    // ========================================

    /**
     * Get ModelData for rendering system - simplified approach
     */
    @Override
    @Nonnull
    public ModelData getModelData() {
        SwitchTextureData textureData = new SwitchTextureData(baseTexturePath, toggleTexturePath);

        ModelData data = ModelData.builder()
                .with(TEXTURE_PROPERTY, textureData)
                .build();

        System.out.println("Phase 3C Debug: BlockEntity getModelData - Base: " + baseTexturePath +
                ", Toggle: " + toggleTexturePath + ", HasCustom: " + textureData.hasCustomTextures());

        return data;
    }

    // ========================================
    // HELPER METHODS FOR GUI INTEGRATION
    // ========================================

    /**
     * Extract texture path from ItemStack (for GUI integration)
     */
    @Nonnull
    public String getTextureFromItem(@Nonnull ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return "";
        }

        try {
            Block block = Block.byItem(itemStack.getItem());
            if (block != null && block != net.minecraft.world.level.block.Blocks.AIR) {
                // Get the registry name and convert to texture path
                net.minecraft.resources.ResourceLocation blockId = net.neoforged.neoforge.registries.ForgeRegistries.BLOCKS.getKey(block);
                if (blockId != null) {
                    String texturePath = blockId.getNamespace() + ":block/" + blockId.getPath();
                    System.out.println("Phase 3C Debug: Extracted texture path: " + texturePath + " from item: " + itemStack);
                    return texturePath;
                }
            }
        } catch (Exception e) {
            System.out.println("Phase 3C Debug: Error extracting texture from item: " + e.getMessage());
        }

        return "";
    }

    /**
     * Apply textures from ItemStacks (for GUI integration)
     */
    public void applyTextures(@Nonnull ItemStack toggleItem, @Nonnull ItemStack baseItem) {
        boolean changed = false;

        // Apply toggle texture
        if (!toggleItem.isEmpty()) {
            String toggleTexture = getTextureFromItem(toggleItem);
            if (!toggleTexture.isEmpty()) {
                changed |= setToggleTexture(toggleTexture);
            }
        } else {
            changed |= setToggleTexture(DEFAULT_TOGGLE_TEXTURE);
        }

        // Apply base texture
        if (!baseItem.isEmpty()) {
            String baseTexture = getTextureFromItem(baseItem);
            if (!baseTexture.isEmpty()) {
                changed |= setBaseTexture(baseTexture);
            }
        } else {
            changed |= setBaseTexture(DEFAULT_BASE_TEXTURE);
        }

        if (changed) {
            System.out.println("Phase 3C Debug: Textures applied - Base: " + baseTexturePath + ", Toggle: " + toggleTexturePath);
        }
    }

    /**
     * Drop stored texture items when block is broken
     */
    public void dropStoredTextures(@Nonnull Level level, @Nonnull BlockPos pos) {
        System.out.println("Phase 3C Debug: Switch broken at " + pos + " - textures were: Base: " + baseTexturePath + ", Toggle: " + toggleTexturePath);
    }

    // ========================================
    // NBT SERIALIZATION (NeoForge 1.20.4)
    // ========================================

    @Override
    public void saveAdditional(@Nonnull CompoundTag nbt) {
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

        return nbt;
    }

    /**
     * Handle update packet on client
     */
    @Override
    public void onDataPacket(@Nonnull Connection net, @Nonnull ClientboundBlockEntityDataPacket pkt) {
        CompoundTag nbt = pkt.getTag();
        if (nbt != null) {
            String newBaseTexture = nbt.getString(BASE_TEXTURE_KEY);
            String newToggleTexture = nbt.getString(TOGGLE_TEXTURE_KEY);

            boolean changed = false;
            if (!newBaseTexture.equals(this.baseTexturePath)) {
                this.baseTexturePath = newBaseTexture;
                changed = true;
            }
            if (!newToggleTexture.equals(this.toggleTexturePath)) {
                this.toggleTexturePath = newToggleTexture;
                changed = true;
            }

            // Sync GUI slot items
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

            if (changed) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
                System.out.println("Phase 3C Debug: Client received texture update - Base: " + baseTexturePath + ", Toggle: " + toggleTexturePath);
            }
        }
    }

    /**
     * Mark dirty and sync to clients
     */
    private void markDirtyAndSync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            // Force model data update by sending multiple update types
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(),
                    Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS | Block.UPDATE_KNOWN_SHAPE);
            System.out.println("Phase 3C Debug: Sent block update for model refresh at " + worldPosition);
        } else if (level != null && level.isClientSide) {
            // Client-side model data refresh
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
            System.out.println("Phase 3C Debug: Client-side block update sent at " + worldPosition);
        }
    }

    // ========================================
    // TICKERS (NeoForge 1.20.4 Compatible)
    // ========================================

    /**
     * Client-side tick (placeholder for future features)
     */
    public static void clientTick(@Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nonnull SwitchesLeverBlockEntity blockEntity) {
        // Placeholder for client-side logic if needed
    }

    /**
     * Server-side tick (placeholder for future features)
     */
    public static void serverTick(@Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nonnull SwitchesLeverBlockEntity blockEntity) {
        // Placeholder for server-side logic if needed
    }

    // ========================================
    // SIMPLE TEXTURE DATA CLASS
    // ========================================

    /**
     * Simple data class for switch texture information
     */
    public static class SwitchTextureData {
        private final String baseTexture;
        private final String toggleTexture;

        public SwitchTextureData(@Nonnull String baseTexture, @Nonnull String toggleTexture) {
            this.baseTexture = baseTexture;
            this.toggleTexture = toggleTexture;
        }

        @Nonnull
        public String getBaseTexture() {
            return baseTexture;
        }

        @Nonnull
        public String getToggleTexture() {
            return toggleTexture;
        }

        public boolean hasCustomTextures() {
            return !baseTexture.equals(DEFAULT_BASE_TEXTURE) ||
                    !toggleTexture.equals(DEFAULT_TOGGLE_TEXTURE);
        }
    }
}