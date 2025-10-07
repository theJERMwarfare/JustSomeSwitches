package net.justsomeswitches.blockentity;

import net.justsomeswitches.init.JustSomeSwitchesModBlockEntities;
import net.justsomeswitches.util.TextureRotation;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Block entity for lever with custom texture management, NBT storage, and client-server synchronization. */
public class SwitchesLeverBlockEntity extends BlockEntity {

    private String wallOrientation = "center";
    private static final String WALL_ORIENTATION_KEY = "wall_orientation";

    public void setWallOrientation(@Nonnull String orientation) {
        if (!orientation.equals(this.wallOrientation)) {
            this.wallOrientation = orientation;
            markDirtyAndSync();
        }
    }

    @Nonnull
    public String getWallOrientation() {
        return wallOrientation;
    }

    public static final String DEFAULT_BASE_TEXTURE = "minecraft:block/stone";
    public static final String DEFAULT_TOGGLE_TEXTURE = "minecraft:block/oak_planks";

    private static final String BASE_TEXTURE_KEY = "base_texture_path";
    private static final String TOGGLE_TEXTURE_KEY = "toggle_texture_path";
    private static final String BASE_VARIABLE_KEY = "base_texture_variable";
    private static final String TOGGLE_VARIABLE_KEY = "toggle_texture_variable";

    private String baseTexturePath = DEFAULT_BASE_TEXTURE;
    private String toggleTexturePath = DEFAULT_TOGGLE_TEXTURE;
    private String baseTextureVariable = "all";
    private String toggleTextureVariable = "all";

    private TextureRotation baseTextureRotation = TextureRotation.NORMAL;
    private TextureRotation toggleTextureRotation = TextureRotation.NORMAL;
    private static final String BASE_ROTATION_KEY = "base_texture_rotation";
    private static final String TOGGLE_ROTATION_KEY = "toggle_texture_rotation";

    private ItemStack guiToggleItem = ItemStack.EMPTY;
    private ItemStack guiBaseItem = ItemStack.EMPTY;

    private boolean suppressSync = false;
    private boolean batchMode = false;
    private boolean pendingBatchUpdate = false;
    private long batchStartTime = 0;
    private static final long BATCH_TIMEOUT_MS = 1000;

    /** Power mode for controlling powered/unpowered texture behavior. */
    public enum PowerMode {
        DEFAULT,
        ALT,
        NONE
    }

    private PowerMode powerMode = PowerMode.DEFAULT;
    private static final String POWER_MODE_KEY = "power_mode";
    private static final String ALT_UNPOWERED_TEXTURE = "minecraft:block/redstone_block";
    private static final String ALT_POWERED_TEXTURE = "minecraft:block/lime_concrete_powder";

    private boolean isInBlockStateChange = false;
    private boolean skipNextNBTLoad = false;

    private String lastSyncedBaseTexture = DEFAULT_BASE_TEXTURE;
    private String lastSyncedToggleTexture = DEFAULT_TOGGLE_TEXTURE;
    private String lastSyncedBaseVariable = "all";
    private String lastSyncedToggleVariable = "all";
    private PowerMode lastSyncedPowerMode = PowerMode.DEFAULT;
    private String lastSyncedWallOrientation = "center";
    private TextureRotation lastSyncedBaseRotation = TextureRotation.NORMAL;
    private TextureRotation lastSyncedToggleRotation = TextureRotation.NORMAL;

    public void protectNBTDuringStateChange() {
        this.isInBlockStateChange = true;
        this.skipNextNBTLoad = true;
    }

    public void endNBTProtection() {
        this.isInBlockStateChange = false;
        this.skipNextNBTLoad = false;
    }

    public static final ModelProperty<String> TOGGLE_TEXTURE = new ModelProperty<>();
    public static final ModelProperty<String> BASE_TEXTURE = new ModelProperty<>();
    public static final ModelProperty<String> FACE_SELECTION = new ModelProperty<>();
    public static final ModelProperty<Boolean> INVERTED = new ModelProperty<>();
    public static final ModelProperty<String> POWER_MODE = new ModelProperty<>();
    public static final ModelProperty<String> WALL_ORIENTATION = new ModelProperty<>();
    public static final ModelProperty<String> BASE_ROTATION = new ModelProperty<>();
    public static final ModelProperty<String> TOGGLE_ROTATION = new ModelProperty<>();
    public static final ModelProperty<Boolean> HAS_TOGGLE_BLOCK = new ModelProperty<>();
    public static final ModelProperty<Boolean> GHOST_MODE = new ModelProperty<>();
    public static final ModelProperty<Float> GHOST_ALPHA = new ModelProperty<>();
    public static final ModelProperty<BlockState> GHOST_STATE = new ModelProperty<>();

    /** Provides model data for custom rendering with textures, rotations, and ghost preview properties. */
    @Override
    @Nonnull
    public ModelData getModelData() {
        String faceSelection = baseTextureVariable + "," + toggleTextureVariable;
        return ModelData.builder()
                .with(TOGGLE_TEXTURE, toggleTexturePath)
                .with(BASE_TEXTURE, baseTexturePath)
                .with(FACE_SELECTION, faceSelection)
                .with(INVERTED, false)
                .with(POWER_MODE, powerMode.name())
                .with(WALL_ORIENTATION, wallOrientation)
                .with(BASE_ROTATION, baseTextureRotation.name())
                .with(TOGGLE_ROTATION, toggleTextureRotation.name())
                .with(HAS_TOGGLE_BLOCK, !guiToggleItem.isEmpty())
                .build();
    }

    public SwitchesLeverBlockEntity(BlockPos pos, BlockState blockState) {
        super(JustSomeSwitchesModBlockEntities.SWITCHES_LEVER.get(), pos, blockState);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void setBlockState(@Nonnull BlockState blockState) {
        protectNBTDuringStateChange();
        super.setBlockState(blockState);
        if (level != null && !level.isClientSide) {
            level.scheduleTick(worldPosition, blockState.getBlock(), 1);
        }
        if (level != null) {
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, blockState, blockState, Block.UPDATE_CLIENTS);
        }
    }

    /** Client-side tick method for NBT protection handling. */
    public static void clientTick(@SuppressWarnings("unused") Level level, @SuppressWarnings("unused") BlockPos pos, @SuppressWarnings("unused") BlockState state, SwitchesLeverBlockEntity blockEntity) {
        if (blockEntity.isInBlockStateChange) {
            blockEntity.endNBTProtection();
            blockEntity.requestModelDataUpdate();
        }
    }

    /** Server-side tick method for NBT protection and batch timeout handling. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, SwitchesLeverBlockEntity blockEntity) {
        if (blockEntity.isInBlockStateChange) {
            blockEntity.endNBTProtection();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
        if (blockEntity.batchMode && blockEntity.pendingBatchUpdate) {
            long elapsed = System.currentTimeMillis() - blockEntity.batchStartTime;
            if (elapsed > BATCH_TIMEOUT_MS) {
                blockEntity.flushBatchUpdates();
            }
        }
    }

    /** Updates textures with change detection to prevent redundant sync. In batch mode, queues updates instead. */
    public void updateTextures() {
        if (level != null) {
            boolean hasChanges = hasChangesFromLastSync();
            if (hasChanges) {
                if (batchMode) {
                    pendingBatchUpdate = true;
                    return;
                }
                executeUpdate();
            }
        }
    }

    /** Executes BlockEntity update operations for both immediate and batched updates. */
    private void executeUpdate() {
        if (level != null) {
            setChanged();
            if (level.isClientSide) {
                requestModelDataUpdate();
            } else {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
            updateLastSyncedValues();
        }
    }

    /** Checks if current values differ from last synced state. */
    private boolean hasChangesFromLastSync() {
        return !baseTexturePath.equals(lastSyncedBaseTexture) ||
               !toggleTexturePath.equals(lastSyncedToggleTexture) ||
               !baseTextureVariable.equals(lastSyncedBaseVariable) ||
               !toggleTextureVariable.equals(lastSyncedToggleVariable) ||
               powerMode != lastSyncedPowerMode ||
               !wallOrientation.equals(lastSyncedWallOrientation) ||
               baseTextureRotation != lastSyncedBaseRotation ||
               toggleTextureRotation != lastSyncedToggleRotation;
    }

    /** Updates tracking fields to current values after successful sync. */
    private void updateLastSyncedValues() {
        lastSyncedBaseTexture = baseTexturePath;
        lastSyncedToggleTexture = toggleTexturePath;
        lastSyncedBaseVariable = baseTextureVariable;
        lastSyncedToggleVariable = toggleTextureVariable;
        lastSyncedPowerMode = powerMode;
        lastSyncedWallOrientation = wallOrientation;
        lastSyncedBaseRotation = baseTextureRotation;
        lastSyncedToggleRotation = toggleTextureRotation;
    }

    /** Starts batch mode - updates queued instead of executing immediately. Called when GUI opens. */
    public void startBatchMode() {
        this.batchMode = true;
        this.pendingBatchUpdate = false;
        this.batchStartTime = System.currentTimeMillis();
    }

    /** Ends batch mode and flushes any pending updates. Called when GUI closes. */
    public void endBatchModeAndFlush() {
        if (batchMode) {
            flushBatchUpdates();
            this.batchMode = false;
            this.pendingBatchUpdate = false;
        }
    }

    /** Flushes pending batch updates by executing the update operation. */
    private void flushBatchUpdates() {
        if (pendingBatchUpdate && hasChangesFromLastSync()) {
            executeUpdate();
            this.pendingBatchUpdate = false;
        }
    }

    private void markDirtyAndSync() {
        if (level != null && !suppressSync) {
            updateTextures();
        }
    }

    public void setSyncSuppressed(boolean suppressed) {
        this.suppressSync = suppressed;
    }

    @SuppressWarnings("UnusedReturnValue") // Return value part of API design
    public boolean setBaseTexture(@Nonnull String texturePath) {
        if (!texturePath.equals(this.baseTexturePath)) {
            this.baseTexturePath = texturePath;
            markDirtyAndSync();
            return true;
        }
        return false;
    }

    @SuppressWarnings("UnusedReturnValue") // Return value part of API design
    public boolean setToggleTexture(@Nonnull String texturePath) {
        if (!texturePath.equals(this.toggleTexturePath)) {
            this.toggleTexturePath = texturePath;
            markDirtyAndSync();
            return true;
        }
        return false;
    }

    @SuppressWarnings("UnusedReturnValue") // Return value part of API design
    public boolean setBaseTextureVariable(@Nonnull String variable) {
        if (!variable.equals(this.baseTextureVariable)) {
            this.baseTextureVariable = variable;
            markDirtyAndSync();
            return true;
        }
        return false;
    }

    @SuppressWarnings("UnusedReturnValue") // Return value part of API design
    public boolean setToggleTextureVariable(@Nonnull String variable) {
        if (!variable.equals(this.toggleTextureVariable)) {
            this.toggleTextureVariable = variable;
            markDirtyAndSync();
            return true;
        }
        return false;
    }

    @Nonnull public String getBaseTextureVariable() { return baseTextureVariable; }
    @Nonnull public String getToggleTextureVariable() { return toggleTextureVariable; }
    @Nonnull public String getBaseTexturePath() { return baseTexturePath; }
    @Nonnull public String getToggleTexturePath() { return toggleTexturePath; }
    @Nonnull public PowerMode getPowerMode() { return powerMode; }
    @Nonnull public TextureRotation getBaseTextureRotation() { return baseTextureRotation; }
    @Nonnull public TextureRotation getToggleTextureRotation() { return toggleTextureRotation; }

    public void resetBaseTexture() {
        setBaseTexture(DEFAULT_BASE_TEXTURE);
        setBaseTextureVariable("all");
    }

    public void resetToggleTexture() {
        setToggleTexture(DEFAULT_TOGGLE_TEXTURE);
        setToggleTextureVariable("all");
    }

    public boolean hasCustomTextures() {
        return !baseTexturePath.equals(DEFAULT_BASE_TEXTURE) ||
                !toggleTexturePath.equals(DEFAULT_TOGGLE_TEXTURE) ||
                !baseTextureVariable.equals("all") ||
                !toggleTextureVariable.equals("all") ||
                powerMode != PowerMode.DEFAULT ||
                hasPowerTextureOverrides();
    }

    public boolean hasPowerTextureOverrides() {
        return switch (powerMode) {
            case ALT, NONE -> true;
            case DEFAULT -> false;
        };
    }

    @SuppressWarnings("UnusedReturnValue") // Return value part of API design
    public boolean setPowerMode(@Nonnull PowerMode mode) {
        if (mode != this.powerMode) {
            this.powerMode = mode;
            markDirtyAndSync();
            return true;
        }
        return false;
    }

    @SuppressWarnings("UnusedReturnValue") // Return value part of API design
    public boolean setBaseTextureRotation(@Nonnull TextureRotation rotation) {
        if (rotation != this.baseTextureRotation) {
            this.baseTextureRotation = rotation;
            markDirtyAndSync();
            return true;
        }
        return false;
    }

    @SuppressWarnings("UnusedReturnValue") // Return value part of API design
    public boolean setToggleTextureRotation(@Nonnull TextureRotation rotation) {
        if (rotation != this.toggleTextureRotation) {
            this.toggleTextureRotation = rotation;
            markDirtyAndSync();
            return true;
        }
        return false;
    }

    @Nonnull
    public String getUnpoweredTexture() {
        return switch (powerMode) {
            case ALT -> ALT_UNPOWERED_TEXTURE;
            case NONE -> toggleTexturePath;
            case DEFAULT -> "minecraft:block/gray_concrete_powder";
        };
    }

    @Nonnull
    public String getPoweredTexture() {
        return switch (powerMode) {
            case ALT -> ALT_POWERED_TEXTURE;
            case NONE -> toggleTexturePath;
            case DEFAULT -> "minecraft:block/redstone_block";
        };
    }

    @Nonnull public ItemStack getGuiToggleItem() { return guiToggleItem; }
    @Nonnull public ItemStack getGuiBaseItem() { return guiBaseItem; }

    public void setToggleSlotItem(@Nonnull ItemStack toggleItem) {
        this.guiToggleItem = toggleItem.copy();
        // Force update to sync HAS_TOGGLE_BLOCK state to client
        // This is critical because the slot state affects model rendering
        if (level != null && !level.isClientSide) {
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        } else if (level != null) {
            requestModelDataUpdate();
        }
    }

    public void setBaseSlotItem(@Nonnull ItemStack baseItem) {
        this.guiBaseItem = baseItem.copy();
        // Force update to ensure NBT sync
        if (level != null && !level.isClientSide) {
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        } else if (level != null) {
            requestModelDataUpdate();
        }
    }

    public void dropStoredTextures(@Nonnull Level level, @Nonnull BlockPos pos) {
        if (!guiToggleItem.isEmpty()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), guiToggleItem);
        }
        if (!guiBaseItem.isEmpty()) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), guiBaseItem);
        }
    }

    @Override
    protected void saveAdditional(@Nonnull CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.putString(BASE_TEXTURE_KEY, baseTexturePath);
        nbt.putString(TOGGLE_TEXTURE_KEY, toggleTexturePath);
        nbt.putString(BASE_VARIABLE_KEY, baseTextureVariable);
        nbt.putString(TOGGLE_VARIABLE_KEY, toggleTextureVariable);
        nbt.putString(POWER_MODE_KEY, powerMode.name());
        nbt.putString(WALL_ORIENTATION_KEY, wallOrientation);
        nbt.putString(BASE_ROTATION_KEY, baseTextureRotation.name());
        nbt.putString(TOGGLE_ROTATION_KEY, toggleTextureRotation.name());
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
        if (skipNextNBTLoad) {
            skipNextNBTLoad = false;
            return;
        }
        String loadedBasePath = nbt.getString(BASE_TEXTURE_KEY);
        String loadedTogglePath = nbt.getString(TOGGLE_TEXTURE_KEY);
        this.baseTexturePath = loadedBasePath;
        this.toggleTexturePath = loadedTogglePath;
        if (this.baseTexturePath.isEmpty()) {
            this.baseTexturePath = DEFAULT_BASE_TEXTURE;
        }
        if (this.toggleTexturePath.isEmpty()) {
            this.toggleTexturePath = DEFAULT_TOGGLE_TEXTURE;
        }
        String loadedBaseVar = nbt.getString(BASE_VARIABLE_KEY);
        String loadedToggleVar = nbt.getString(TOGGLE_VARIABLE_KEY);
        this.baseTextureVariable = loadedBaseVar;
        this.toggleTextureVariable = loadedToggleVar;
        if (this.baseTextureVariable.isEmpty()) {
            this.baseTextureVariable = "all";
        }
        if (this.toggleTextureVariable.isEmpty()) {
            this.toggleTextureVariable = "all";
        }
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
        String loadedWallOrientation = nbt.getString(WALL_ORIENTATION_KEY);
        this.wallOrientation = loadedWallOrientation.isEmpty() ? "center" : loadedWallOrientation;
        this.baseTextureRotation = parseTextureRotation(nbt.getString(BASE_ROTATION_KEY));
        this.toggleTextureRotation = parseTextureRotation(nbt.getString(TOGGLE_ROTATION_KEY));
        this.guiToggleItem = nbt.contains("gui_toggle_item") ?
                ItemStack.of(nbt.getCompound("gui_toggle_item")) : ItemStack.EMPTY;
        this.guiBaseItem = nbt.contains("gui_base_item") ?
                ItemStack.of(nbt.getCompound("gui_base_item")) : ItemStack.EMPTY;
        updateLastSyncedValues();
    }

    /** Safely parses TextureRotation from NBT string. */
    @Nonnull
    private TextureRotation parseTextureRotation(@Nonnull String rotationString) {
        if (rotationString.isEmpty()) {
            return TextureRotation.NORMAL;
        }
        try {
            return TextureRotation.valueOf(rotationString);
        } catch (IllegalArgumentException e) {
            return TextureRotation.NORMAL;
        }
    }

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
        nbt.putString(BASE_VARIABLE_KEY, baseTextureVariable);
        nbt.putString(TOGGLE_VARIABLE_KEY, toggleTextureVariable);
        nbt.putString(POWER_MODE_KEY, powerMode.name());
        nbt.putString(WALL_ORIENTATION_KEY, wallOrientation);
        nbt.putString(BASE_ROTATION_KEY, baseTextureRotation.name());
        nbt.putString(TOGGLE_ROTATION_KEY, toggleTextureRotation.name());
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
            // Clear NBT protection flag - network data is authoritative and must be loaded
            // This prevents block state changes from blocking network NBT updates
            skipNextNBTLoad = false;
            load(nbt);
            requestModelDataUpdate();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }
}
