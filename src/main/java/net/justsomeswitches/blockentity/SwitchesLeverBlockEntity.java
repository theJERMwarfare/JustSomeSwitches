package net.justsomeswitches.blockentity;

import net.justsomeswitches.gui.FaceSelectionData;
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
 * Switches Lever BlockEntity with advanced texture management and NBT persistence.
 * Features raw JSON variable system for face-specific texture customization.
 */
public class SwitchesLeverBlockEntity extends BlockEntity {


    // ========================================
    // WALL ORIENTATION SYSTEM
    // ========================================
    
    // Wall orientation for advanced placement
    private String wallOrientation = "center";
    private static final String WALL_ORIENTATION_KEY = "wall_orientation";
    
    /**
     * Set wall orientation for advanced placement
     */
    public void setWallOrientation(@Nonnull String orientation) {
        if (!orientation.equals(this.wallOrientation)) {
            this.wallOrientation = orientation;
            markDirtyAndSync();
        }
    }
    
    /**
     * Get current wall orientation
     */
    @Nonnull
    public String getWallOrientation() {
        return wallOrientation;
    }

    // ========================================
    // TEXTURE CONFIGURATION
    // ========================================
    // Default texture paths
    public static final String DEFAULT_BASE_TEXTURE = "minecraft:block/stone";
    public static final String DEFAULT_TOGGLE_TEXTURE = "minecraft:block/oak_planks";

    // NBT keys for texture storage
    private static final String BASE_TEXTURE_KEY = "base_texture_path";
    private static final String TOGGLE_TEXTURE_KEY = "toggle_texture_path";

    // NBT keys for face selection storage (now strings)
    private static final String BASE_VARIABLE_KEY = "base_texture_variable";
    private static final String TOGGLE_VARIABLE_KEY = "toggle_texture_variable";

    // Current texture configuration
    private String baseTexturePath = DEFAULT_BASE_TEXTURE;
    private String toggleTexturePath = DEFAULT_TOGGLE_TEXTURE;

    // Raw face selections (JSON variable names)
    private String baseTextureVariable = "all";
    private String toggleTextureVariable = "all";

    // GUI slot storage
    private ItemStack guiToggleItem = ItemStack.EMPTY;
    private ItemStack guiBaseItem = ItemStack.EMPTY;
    
    // Control sync behavior during analysis vs user selection
    private boolean suppressSync = false;

    // ========================================
    // POWER CATEGORY SYSTEM
    // ========================================

    /**
     * Power mode enum for controlling how powered/unpowered UV faces are textured
     */
    public enum PowerMode {
        DEFAULT,  // Use block's original JSON texture definitions
        ALT,      // unpowered = redstone block, powered = green concrete powder  
        NONE      // both use current toggle face texture selection
    }

    // Power category state
    private PowerMode powerMode = PowerMode.DEFAULT;
    private static final String POWER_MODE_KEY = "power_mode";

    // Hardcoded texture paths for ALT mode
    private static final String ALT_UNPOWERED_TEXTURE = "minecraft:block/redstone_block";
    private static final String ALT_POWERED_TEXTURE = "minecraft:block/lime_concrete_powder";

    // ========================================
    // BLOCKSTATE PROTECTION SYSTEM
    // ========================================

    // Track if we're in a blockstate change to prevent NBT corruption
    private boolean isInBlockStateChange = false;
    private boolean skipNextNBTLoad = false;

    /**
     * Mark start of blockstate change to protect NBT data
     */
    public void protectNBTDuringStateChange() {
        this.isInBlockStateChange = true;
        this.skipNextNBTLoad = true;
    }

    /**
     * Mark end of blockstate change and restore normal NBT processing
     */
    public void endNBTProtection() {
        this.isInBlockStateChange = false;
        this.skipNextNBTLoad = false;
    }

    // ========================================
    // MODEL DATA INTEGRATION
    // ========================================

    /**
     * ModelProperty for passing texture data to custom models
     */
    public static final ModelProperty<SwitchTextureData> TEXTURE_PROPERTY = new ModelProperty<>();

    /**
     * Enhanced data class for passing texture information to custom models
     */
    public static class SwitchTextureData {
        private final String baseTexture;
        private final String toggleTexture;
        private final String baseVariable;
        private final String toggleVariable;
        private final PowerMode powerMode;
        private final String unpoweredTexture;
        private final String poweredTexture;
        public SwitchTextureData(String baseTexture, String toggleTexture,
                                 String baseVariable, String toggleVariable, PowerMode powerMode,
                                 SwitchesLeverBlockEntity blockEntity) {
            this.baseTexture = baseTexture;
            this.toggleTexture = toggleTexture;
            this.baseVariable = baseVariable;
            this.toggleVariable = toggleVariable;
            this.powerMode = powerMode;
            
            // Get model-specific power textures (empty for DEFAULT mode)
            this.unpoweredTexture = blockEntity != null ? blockEntity.getUnpoweredTextureForModel() : "";
            this.poweredTexture = blockEntity != null ? blockEntity.getPoweredTextureForModel() : "";
        }

        public String getBaseTexture() { return baseTexture; }
        public String getToggleTexture() { return toggleTexture; }
        public String getBaseVariable() { return baseVariable; }
        public String getToggleVariable() { return toggleVariable; }
        public PowerMode getPowerMode() { return powerMode; }
        public String getUnpoweredTexture() { return unpoweredTexture; }
        public String getPoweredTexture() { return poweredTexture; }

        /**
         * Check if power mode affects model rendering
         */
        public boolean hasPowerTextureOverride() {
            return powerMode != PowerMode.DEFAULT;
        }

        /**
         * Check if using custom textures (different from defaults)
         */
        public boolean hasCustomTextures() {
            return !baseTexture.equals(DEFAULT_BASE_TEXTURE) ||
                    !toggleTexture.equals(DEFAULT_TOGGLE_TEXTURE) ||
                    !baseVariable.equals("all") ||
                    !toggleVariable.equals("all") ||
                    powerMode != PowerMode.DEFAULT;
        }
    }

    /**
     * Get ModelData for custom model rendering
     */
    @Override
    @Nonnull
    public ModelData getModelData() {
        return ModelData.builder()
                .with(TEXTURE_PROPERTY, new SwitchTextureData(
                        baseTexturePath, toggleTexturePath,
                        baseTextureVariable, toggleTextureVariable, powerMode, this))
                .build();
    }

    // ========================================
    // CONSTRUCTOR AND BASIC SETUP
    // ========================================

    public SwitchesLeverBlockEntity(BlockPos pos, BlockState blockState) {
        super(JustSomeSwitchesModBlockEntities.SWITCHES_LEVER.get(), pos, blockState);
    }

    // ========================================
    // BLOCKSTATE OVERRIDE PROTECTION
    // ========================================

    /**
     * Override setBlockState to protect NBT data
     */
    @Override
    public void setBlockState(@Nonnull BlockState blockState) {
        // Mark that we're in a blockstate change
        protectNBTDuringStateChange();

        super.setBlockState(blockState);

        // Schedule NBT protection end after blockstate change completes
        if (level != null && !level.isClientSide) {
            level.scheduleTick(worldPosition, blockState.getBlock(), 1);
        }

        // Force model data update
        if (level != null) {
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, blockState, blockState, Block.UPDATE_CLIENTS);
        }
    }

    // ========================================
    // TICK METHODS FOR CLEANUP
    // ========================================

    public static void clientTick(Level level, BlockPos pos, BlockState state, SwitchesLeverBlockEntity blockEntity) {
        // Clean up NBT protection if active
        if (blockEntity.isInBlockStateChange) {
            blockEntity.endNBTProtection();
            blockEntity.requestModelDataUpdate();
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SwitchesLeverBlockEntity blockEntity) {
        // Clean up NBT protection if active
        if (blockEntity.isInBlockStateChange) {
            blockEntity.endNBTProtection();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    // ========================================
    // TEXTURE MANAGEMENT
    // ========================================



    /**
     * Mark BlockEntity as dirty and trigger synchronization
     * Fixed to handle both client-to-server and server-to-client updates
     */
    private void markDirtyAndSync() {
        if (level != null && !suppressSync) {
            setChanged();
            requestModelDataUpdate();
            
            if (level.isClientSide) {
                // Client side: Mark for server sync (server will handle the update)
                } else {
                // Server side: Send update to clients immediately
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
                }
        }
    }
    
    /**
     * Temporarily suppress sync during analysis operations
     */
    public void setSyncSuppressed(boolean suppressed) {
        this.suppressSync = suppressed;
    }

    /**
     * Set the base texture for the switch
     */
    public boolean setBaseTexture(@Nonnull String texturePath) {
        if (!texturePath.equals(this.baseTexturePath)) {
            this.baseTexturePath = texturePath;
            markDirtyAndSync();
            return true;
        }
        return false;
    }

    /**
     * Set the toggle texture for the switch
     */
    public boolean setToggleTexture(@Nonnull String texturePath) {
        if (!texturePath.equals(this.toggleTexturePath)) {
            this.toggleTexturePath = texturePath;
            markDirtyAndSync();
            return true;
        }
        return false;
    }

    // ========================================
    // RAW TEXTURE VARIABLE MANAGEMENT
    // ========================================

    /**
     * Set base texture variable (raw JSON variable name)
     */
    public boolean setBaseTextureVariable(@Nonnull String variable) {
        if (!variable.equals(this.baseTextureVariable)) {
            this.baseTextureVariable = variable;
            markDirtyAndSync();
            return true;
        }
        return false;
    }

    /**
     * Set toggle texture variable (raw JSON variable name)
     */
    public boolean setToggleTextureVariable(@Nonnull String variable) {
        if (!variable.equals(this.toggleTextureVariable)) {
            this.toggleTextureVariable = variable;
            markDirtyAndSync();
            return true;
        }
        return false;
    }

    // ========================================
    // RAW TEXTURE SELECTION INTEGRATION
    // ========================================

    /**
     * Get raw texture selection for base texture slot
     */
    @Nonnull
    public FaceSelectionData.RawTextureSelection getBaseTextureSelection() {
        return FaceSelectionData.createRawTextureSelection(guiBaseItem, baseTextureVariable);
    }

    /**
     * Get raw texture selection for toggle texture slot
     */
    @Nonnull
    public FaceSelectionData.RawTextureSelection getToggleTextureSelection() {
        return FaceSelectionData.createRawTextureSelection(guiToggleItem, toggleTextureVariable);
    }

    // ========================================
    // GETTERS FOR GUI AND MODEL INTEGRATION
    // ========================================

    @Nonnull public String getBaseTexture() { return baseTexturePath; }
    @Nonnull public String getToggleTexture() { return toggleTexturePath; }
    @Nonnull public String getBaseTextureVariable() { return baseTextureVariable; }
    @Nonnull public String getToggleTextureVariable() { return toggleTextureVariable; }
    @Nonnull public PowerMode getPowerMode() { return powerMode; }

    /**
     * Get all custom textures as a map for renderer integration
     * Returns empty map if no custom textures are configured
     */
    @Nonnull
    public java.util.Map<String, ResourceLocation> getAllCustomTextures() {
        java.util.Map<String, ResourceLocation> customTextures = new java.util.HashMap<>();
        
        // Only include textures that are different from defaults
        if (!baseTexturePath.equals(DEFAULT_BASE_TEXTURE)) {
            try {
                customTextures.put("base_" + baseTextureVariable, new ResourceLocation(baseTexturePath));
            } catch (Exception e) {
                // Invalid texture path, skip
            }
        }
        
        if (!toggleTexturePath.equals(DEFAULT_TOGGLE_TEXTURE)) {
            try {
                customTextures.put("toggle_" + toggleTextureVariable, new ResourceLocation(toggleTexturePath));
            } catch (Exception e) {
                // Invalid texture path, skip
            }
        }
        
        return customTextures;
    }



    /**
     * Reset base texture to default
     */
    public void resetBaseTexture() {
        setBaseTexture(DEFAULT_BASE_TEXTURE);
        setBaseTextureVariable("all");
    }

    /**
     * Reset toggle texture to default
     */
    public void resetToggleTexture() {
        setToggleTexture(DEFAULT_TOGGLE_TEXTURE);
        setToggleTextureVariable("all");
    }

    /**
     * Check if using custom textures
     */
    public boolean hasCustomTextures() {
        return !baseTexturePath.equals(DEFAULT_BASE_TEXTURE) ||
                !toggleTexturePath.equals(DEFAULT_TOGGLE_TEXTURE) ||
                !baseTextureVariable.equals("all") ||
                !toggleTextureVariable.equals("all") ||
                powerMode != PowerMode.DEFAULT ||
                hasPowerTextureOverrides();
    }
    
    /**
     * Check if power mode requires texture overrides for model rendering
     */
    public boolean hasPowerTextureOverrides() {
        return switch (powerMode) {
            case ALT, NONE -> true; // These modes override power textures
            case DEFAULT -> false; // Uses original JSON textures
        };
    }

    // ========================================
    // POWER CATEGORY MANAGEMENT
    // ========================================

    /**
     * Set the power mode for controlling powered/unpowered UV textures
     */
    public boolean setPowerMode(@Nonnull PowerMode mode) {
        if (mode != this.powerMode) {
            this.powerMode = mode;
            markDirtyAndSync();
            return true;
        }
        return false;
    }

    /**
     * Get texture path for unpowered state based on current power mode
     */
    @Nonnull
    public String getUnpoweredTexture() {
        return switch (powerMode) {
            case ALT -> ALT_UNPOWERED_TEXTURE;
            case NONE -> toggleTexturePath; // Use toggle texture for both states
            case DEFAULT -> "minecraft:block/gray_concrete_powder"; // Use the actual unpowered texture from switches_lever.json
        };
    }

    /**
     * Get texture path for powered state based on current power mode
     */
    @Nonnull
    public String getPoweredTexture() {
        return switch (powerMode) {
            case ALT -> ALT_POWERED_TEXTURE;
            case NONE -> toggleTexturePath; // Use toggle texture for both states
            case DEFAULT -> "minecraft:block/redstone_block"; // Use the actual powered texture from switches_lever.json
        };
    }

    /**
     * Get texture path for unpowered state for MODEL RENDERING
     * (different from preview - this returns empty for DEFAULT to use original JSON)
     */
    @Nonnull
    public String getUnpoweredTextureForModel() {
        return switch (powerMode) {
            case ALT -> ALT_UNPOWERED_TEXTURE;
            case NONE -> toggleTexturePath;
            case DEFAULT -> ""; // Use original block JSON definitions
        };
    }

    /**
     * Get texture path for powered state for MODEL RENDERING
     * (different from preview - this returns empty for DEFAULT to use original JSON)
     */
    @Nonnull
    public String getPoweredTextureForModel() {
        return switch (powerMode) {
            case ALT -> ALT_POWERED_TEXTURE;
            case NONE -> toggleTexturePath;
            case DEFAULT -> ""; // Use original block JSON definitions
        };
    }

    // ========================================
    // GUI SLOT MANAGEMENT
    // ========================================

    @Nonnull public ItemStack getGuiToggleItem() { return guiToggleItem; }
    @Nonnull public ItemStack getGuiBaseItem() { return guiBaseItem; }



    /**
     * Set only the toggle slot item - for independent category updates
     */
    public void setToggleSlotItem(@Nonnull ItemStack toggleItem) {
        this.guiToggleItem = toggleItem.copy();
    }

    /**
     * Set only the base slot item - for independent category updates
     */
    public void setBaseSlotItem(@Nonnull ItemStack baseItem) {
        this.guiBaseItem = baseItem.copy();
    }

    /**
     * Drop stored texture blocks when switch is broken
     */
    public void dropStoredTextures(@Nonnull Level level, @Nonnull BlockPos pos) {
        if (!guiToggleItem.isEmpty()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), guiToggleItem);
        }

        if (!guiBaseItem.isEmpty()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), guiBaseItem);
        }
    }

    // ========================================
    // CORRECTED NBT SERIALIZATION - NO MORE DATA LOSS
    // ========================================

    @Override
    protected void saveAdditional(@Nonnull CompoundTag nbt) {
        super.saveAdditional(nbt);

        // Save texture paths
        nbt.putString(BASE_TEXTURE_KEY, baseTexturePath);
        nbt.putString(TOGGLE_TEXTURE_KEY, toggleTexturePath);

        // Save raw texture variables
        nbt.putString(BASE_VARIABLE_KEY, baseTextureVariable);
        nbt.putString(TOGGLE_VARIABLE_KEY, toggleTextureVariable);

        // Save power mode
        nbt.putString(POWER_MODE_KEY, powerMode.name());
        
        // Save wall orientation
        nbt.putString(WALL_ORIENTATION_KEY, wallOrientation);

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

        // Skip NBT loading during blockstate changes to prevent corruption
        if (skipNextNBTLoad) {
            skipNextNBTLoad = false;
            return;
        }

        // Load texture paths with defaults
        String loadedBasePath = nbt.getString(BASE_TEXTURE_KEY);
        String loadedTogglePath = nbt.getString(TOGGLE_TEXTURE_KEY);
        
        this.baseTexturePath = loadedBasePath;
        this.toggleTexturePath = loadedTogglePath;

        // Validate loaded paths
        if (this.baseTexturePath.isEmpty()) {
            this.baseTexturePath = DEFAULT_BASE_TEXTURE;
        }
        if (this.toggleTexturePath.isEmpty()) {
            this.toggleTexturePath = DEFAULT_TOGGLE_TEXTURE;
        }

        // Load raw texture variables
        String loadedBaseVar = nbt.getString(BASE_VARIABLE_KEY);
        String loadedToggleVar = nbt.getString(TOGGLE_VARIABLE_KEY);
        
        this.baseTextureVariable = loadedBaseVar;
        this.toggleTextureVariable = loadedToggleVar;

        // Validate loaded variables
        if (this.baseTextureVariable.isEmpty()) {
            this.baseTextureVariable = "all";
        }
        if (this.toggleTextureVariable.isEmpty()) {
            this.toggleTextureVariable = "all";
        }

        // Load power mode
        String loadedPowerMode = nbt.getString(POWER_MODE_KEY);
        if (!loadedPowerMode.isEmpty()) {
            try {
                this.powerMode = PowerMode.valueOf(loadedPowerMode);
            } catch (IllegalArgumentException e) {
                this.powerMode = PowerMode.DEFAULT;

            }
        } else {
            this.powerMode = PowerMode.DEFAULT;
        }
        
        // Load wall orientation
        String loadedWallOrientation = nbt.getString(WALL_ORIENTATION_KEY);
        this.wallOrientation = loadedWallOrientation.isEmpty() ? "center" : loadedWallOrientation;

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

        // Include all data for client sync
        nbt.putString(BASE_TEXTURE_KEY, baseTexturePath);
        nbt.putString(TOGGLE_TEXTURE_KEY, toggleTexturePath);
        nbt.putString(BASE_VARIABLE_KEY, baseTextureVariable);
        nbt.putString(TOGGLE_VARIABLE_KEY, toggleTextureVariable);
        nbt.putString(POWER_MODE_KEY, powerMode.name());
        nbt.putString(WALL_ORIENTATION_KEY, wallOrientation);

        // Sync GUI slot items
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


}