package net.justsomeswitches.blockentity;

import net.justsomeswitches.init.JustSomeSwitchesModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Block Entity for Switches Lever that handles texture customization storage
 * ---
 * This BlockEntity stores custom texture information in NBT data and provides
 * methods for getting/setting textures for the base and toggle components.
 * ---
 * Phase 3A Implementation: NeoForge 1.20.4 compatible NBT storage
 */
public class SwitchesLeverBlockEntity extends BlockEntity {

    // NBT tag names for data persistence
    private static final String TAG_BASE_TEXTURE = "BaseTexture";
    private static final String TAG_TOGGLE_TEXTURE = "ToggleTexture";
    private static final String TAG_BASE_ITEM = "BaseItem";
    private static final String TAG_TOGGLE_ITEM = "ToggleItem";

    // Default textures (same as model defaults)
    private static final String DEFAULT_BASE_TEXTURE = "minecraft:block/cobblestone";
    private static final String DEFAULT_TOGGLE_TEXTURE = "minecraft:block/oak_planks";

    // Current texture settings stored in this block entity
    private String baseTexture = DEFAULT_BASE_TEXTURE;
    private String toggleTexture = DEFAULT_TOGGLE_TEXTURE;

    // Stored items for dropping when block is broken
    private ItemStack baseItem = ItemStack.EMPTY;
    private ItemStack toggleItem = ItemStack.EMPTY;

    // Update tracking for client-server sync
    private boolean needsUpdate = false;

    /**
     * Constructor for the SwitchesLeverBlockEntity
     * @param pos The position of this block entity
     * @param blockState The block state associated with this block entity
     */
    public SwitchesLeverBlockEntity(BlockPos pos, BlockState blockState) {
        super(JustSomeSwitchesModBlockEntities.SWITCHES_LEVER.get(), pos, blockState);
    }

    // ========================================
    // TEXTURE MANAGEMENT METHODS
    // ========================================

    /**
     * Sets the base texture from a block item
     * @param blockItem The block item to extract texture from
     * @return true if texture was successfully set
     */
    public boolean setBaseTexture(@Nonnull ItemStack blockItem) {
        if (blockItem.isEmpty() || !(blockItem.getItem() instanceof BlockItem item)) {
            return false;
        }

        Block block = item.getBlock();
        String texturePath = getTextureFromBlock(block);

        if (texturePath != null) {
            this.baseTexture = texturePath;
            this.baseItem = blockItem.copy();
            markUpdated();
            return true;
        }
        return false;
    }

    /**
     * Sets the toggle texture from a block item
     * @param blockItem The block item to extract texture from
     * @return true if texture was successfully set
     */
    public boolean setToggleTexture(@Nonnull ItemStack blockItem) {
        if (blockItem.isEmpty() || !(blockItem.getItem() instanceof BlockItem item)) {
            return false;
        }

        Block block = item.getBlock();
        String texturePath = getTextureFromBlock(block);

        if (texturePath != null) {
            this.toggleTexture = texturePath;
            this.toggleItem = blockItem.copy();
            markUpdated();
            return true;
        }
        return false;
    }

    /**
     * Extracts texture path from a block for use in models
     * @param block The block to get texture from
     * @return The texture path or null if invalid
     */
    @Nullable
    private String getTextureFromBlock(@Nonnull Block block) {
        try {
            ResourceLocation blockId = block.builtInRegistryHolder().key().location();
            return blockId.getNamespace() + ":block/" + blockId.getPath();
        } catch (Exception e) {
            return null;
        }
    }

    // ========================================
    // GETTER METHODS
    // ========================================

    /**
     * Gets the current base texture path
     * @return The base texture resource location string
     */
    @Nonnull
    public String getBaseTexture() {
        return baseTexture;
    }

    /**
     * Gets the current toggle texture path
     * @return The toggle texture resource location string
     */
    @Nonnull
    public String getToggleTexture() {
        return toggleTexture;
    }

    /**
     * Gets the stored base item (for GUI population)
     * @return A copy of the ItemStack stored for the base texture
     */
    @Nonnull
    public ItemStack getBaseItem() {
        return baseItem.copy();
    }

    /**
     * Gets the stored toggle item (for GUI population)
     * @return A copy of the ItemStack stored for the toggle texture
     */
    @Nonnull
    public ItemStack getToggleItem() {
        return toggleItem.copy();
    }

    /**
     * Checks if this switch has custom textures applied
     * @return true if either texture is customized from defaults
     */
    public boolean hasCustomTextures() {
        return !baseTexture.equals(DEFAULT_BASE_TEXTURE) ||
                !toggleTexture.equals(DEFAULT_TOGGLE_TEXTURE);
    }

    /**
     * Resets textures to defaults and clears stored items
     */
    public void resetTextures() {
        this.baseTexture = DEFAULT_BASE_TEXTURE;
        this.toggleTexture = DEFAULT_TOGGLE_TEXTURE;
        this.baseItem = ItemStack.EMPTY;
        this.toggleItem = ItemStack.EMPTY;
        markUpdated();
    }

    // ========================================
    // ITEM DROPPING SUPPORT
    // ========================================

    /**
     * Drops stored texture items when the block is broken
     * @param level The world level
     * @param pos The block position
     */
    public void dropStoredTextures(@Nonnull Level level, @Nonnull BlockPos pos) {
        if (!baseItem.isEmpty()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), baseItem);
        }
        if (!toggleItem.isEmpty()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), toggleItem);
        }
    }

    // ========================================
    // NBT SERIALIZATION (NeoForge 1.20.4 Compatible)
    // ========================================

    @Override
    protected void saveAdditional(@Nonnull CompoundTag tag) {
        super.saveAdditional(tag);

        // Save texture paths
        tag.putString(TAG_BASE_TEXTURE, baseTexture);
        tag.putString(TAG_TOGGLE_TEXTURE, toggleTexture);

        // Save stored items if they exist
        if (!baseItem.isEmpty()) {
            CompoundTag baseItemTag = new CompoundTag();
            baseItem.save(baseItemTag);
            tag.put(TAG_BASE_ITEM, baseItemTag);
        }

        if (!toggleItem.isEmpty()) {
            CompoundTag toggleItemTag = new CompoundTag();
            toggleItem.save(toggleItemTag);
            tag.put(TAG_TOGGLE_ITEM, toggleItemTag);
        }
    }

    @Override
    public void load(@Nonnull CompoundTag tag) {
        super.load(tag);

        // Load texture paths (with fallbacks to defaults)
        baseTexture = tag.getString(TAG_BASE_TEXTURE);
        if (baseTexture.isEmpty()) {
            baseTexture = DEFAULT_BASE_TEXTURE;
        }

        toggleTexture = tag.getString(TAG_TOGGLE_TEXTURE);
        if (toggleTexture.isEmpty()) {
            toggleTexture = DEFAULT_TOGGLE_TEXTURE;
        }

        // Load stored items
        if (tag.contains(TAG_BASE_ITEM)) {
            baseItem = ItemStack.of(tag.getCompound(TAG_BASE_ITEM));
        } else {
            baseItem = ItemStack.EMPTY;
        }

        if (tag.contains(TAG_TOGGLE_ITEM)) {
            toggleItem = ItemStack.of(tag.getCompound(TAG_TOGGLE_ITEM));
        } else {
            toggleItem = ItemStack.EMPTY;
        }
    }

    // ========================================
    // CLIENT-SERVER SYNCHRONIZATION
    // ========================================

    @Override
    @Nonnull
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    @Nullable
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /**
     * Marks the block entity as needing client update and saves data
     */
    private void markUpdated() {
        this.needsUpdate = true;
        setChanged(); // Mark for saving to disk

        if (level != null && !level.isClientSide) {
            // Sync changes to clients
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ========================================
    // TICKER METHODS (for future use)
    // ========================================

    /**
     * Server-side tick method
     * @param level The world level
     * @param pos The block position
     * @param state The block state
     * @param blockEntity The block entity instance
     */
    public static void serverTick(@Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState state,
                                  @Nonnull SwitchesLeverBlockEntity blockEntity) {
        // Server-side tick logic if needed in future phases
        if (blockEntity.needsUpdate) {
            blockEntity.needsUpdate = false;
            // Any additional server-side update logic here
        }
    }

    /**
     * Client-side tick method
     * @param level The world level
     * @param pos The block position
     * @param state The block state
     * @param blockEntity The block entity instance
     */
    public static void clientTick(@Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState state,
                                  @Nonnull SwitchesLeverBlockEntity blockEntity) {
        // Client-side tick logic if needed in future phases
        if (blockEntity.needsUpdate) {
            blockEntity.needsUpdate = false;
            // Any client-side update logic here (like renderer refresh)
        }
    }
}