package net.justsomeswitches.blockentity;

import net.justsomeswitches.init.JustSomeSwitchesModBlockEntities;
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

/**
 * Block entity for lever with custom texture management.
 */
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


    private ItemStack guiToggleItem = ItemStack.EMPTY;
    private ItemStack guiBaseItem = ItemStack.EMPTY;

    private boolean suppressSync = false;

    /**
     * Power mode for controlling powered/unpowered texture behavior.
     */
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
    /**
     * Provides model data for custom rendering.
     */
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



    /**
     * Client-side tick method. Parameters required by NeoForge BlockEntity tick signature.
     */
    @SuppressWarnings("unused")
    public static void clientTick(@SuppressWarnings("unused") Level level, @SuppressWarnings("unused") BlockPos pos, @SuppressWarnings("unused") BlockState state, SwitchesLeverBlockEntity blockEntity) {
        if (blockEntity.isInBlockStateChange) {
            blockEntity.endNBTProtection();
            blockEntity.requestModelDataUpdate();
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SwitchesLeverBlockEntity blockEntity) {
        if (blockEntity.isInBlockStateChange) {
            blockEntity.endNBTProtection();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    /**
     * Updates textures and triggers model refresh.
     */
    public void updateTextures() {
        if (level != null) {
            setChanged();
            
            if (level.isClientSide) {
                requestModelDataUpdate();
            } else {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
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


    @SuppressWarnings("UnusedReturnValue")
    public boolean setBaseTexture(@Nonnull String texturePath) {
        if (!texturePath.equals(this.baseTexturePath)) {
            this.baseTexturePath = texturePath;
            markDirtyAndSync();
            return true;
        }
        return false;
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean setToggleTexture(@Nonnull String texturePath) {
        if (!texturePath.equals(this.toggleTexturePath)) {
            this.toggleTexturePath = texturePath;
            markDirtyAndSync();
            return true;
        }
        return false;
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean setBaseTextureVariable(@Nonnull String variable) {
        if (!variable.equals(this.baseTextureVariable)) {
            this.baseTextureVariable = variable;
            markDirtyAndSync();
            return true;
        }
        return false;
    }

    @SuppressWarnings("UnusedReturnValue")
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
    @Nonnull public PowerMode getPowerMode() { return powerMode; }

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


    @SuppressWarnings("UnusedReturnValue")
    public boolean setPowerMode(@Nonnull PowerMode mode) {
        if (mode != this.powerMode) {
            this.powerMode = mode;
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
    }


    public void setBaseSlotItem(@Nonnull ItemStack baseItem) {
        this.guiBaseItem = baseItem.copy();
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
            
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }


}