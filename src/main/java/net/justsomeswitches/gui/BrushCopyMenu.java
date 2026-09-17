package net.justsomeswitches.gui;

import net.justsomeswitches.blockentity.SwitchBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nonnull;

/** Container menu for brush copy settings GUI with selective copy logic. */
public class BrushCopyMenu extends AbstractContainerMenu {
    
    private final BlockPos blockPos;
    private final ContainerLevelAccess levelAccess;
    private SwitchBlockEntity blockEntity;
    
    private boolean copyToggleBlock = true;
    private boolean copyToggleFace = true;
    private boolean copyToggleRotation = true;
    private boolean copyIndicators = true;
    private boolean copyBaseBlock = true;
    private boolean copyBaseFace = true;
    private boolean copyBaseRotation = true;
    
    /** Client-side constructor required by NeoForge framework. */
    @SuppressWarnings("unused")
    public BrushCopyMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, extraData.readBlockPos());
    }
    
    /** Server-side constructor. */
    public BrushCopyMenu(int containerId, Inventory playerInventory, BlockPos blockPos) {
        super(JustSomeSwitchesMenuTypes.BRUSH_COPY.get(), containerId);
        
        this.blockPos = blockPos != null ? blockPos : BlockPos.ZERO;
        this.levelAccess = ContainerLevelAccess.create(playerInventory.player.level(), this.blockPos);
        
        this.levelAccess.execute((level, pos) -> {
            try {
                BlockEntity entity = level.getBlockEntity(pos);
                if (entity instanceof SwitchBlockEntity leverEntity) {
                    this.blockEntity = leverEntity;
                }
            } catch (Exception e) {
                this.blockEntity = null;
            }
        });
    }
    
    @Override
    @Nonnull
    public ItemStack quickMoveStack(@Nonnull Player player, int index) {
        return ItemStack.EMPTY;
    }
    
    @Override
    public boolean stillValid(@Nonnull Player player) {
        if (blockEntity == null) {
            return false;
        }
        
        return levelAccess.evaluate((level, pos) -> {
            BlockEntity entity = level.getBlockEntity(pos);
            return entity instanceof SwitchBlockEntity && 
                   player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
        }, true);
    }
    
    public boolean getCopyToggleBlock() { return copyToggleBlock; }
    public boolean getCopyToggleFace() { return copyToggleFace; }
    public boolean getCopyToggleRotation() { return copyToggleRotation; }
    public boolean getCopyIndicators() { return copyIndicators; }
    public boolean getCopyBaseBlock() { return copyBaseBlock; }
    public boolean getCopyBaseFace() { return copyBaseFace; }
    public boolean getCopyBaseRotation() { return copyBaseRotation; }
    
    public void setCopyToggleBlock(boolean value) { this.copyToggleBlock = value; }
    public void setCopyToggleFace(boolean value) { this.copyToggleFace = value; }
    public void setCopyToggleRotation(boolean value) { this.copyToggleRotation = value; }
    public void setCopyIndicators(boolean value) { this.copyIndicators = value; }
    public void setCopyBaseBlock(boolean value) { this.copyBaseBlock = value; }
    public void setCopyBaseFace(boolean value) { this.copyBaseFace = value; }
    public void setCopyBaseRotation(boolean value) { this.copyBaseRotation = value; }
    
    /** Sets all copy selections to the specified value. */
    public void setAllCopySelections(boolean value) {
        copyToggleBlock = value;
        copyToggleFace = value;
        copyToggleRotation = value;
        copyIndicators = value;
        copyBaseBlock = value;
        copyBaseFace = value;
        copyBaseRotation = value;
    }
    
    @SuppressWarnings("unused")
    public BlockPos getBlockPos() {
        return blockPos;
    }
    
    public SwitchBlockEntity getBlockEntity() {
        return blockEntity;
    }
    
    /** Returns display text for toggle block preview. */
    public Component getToggleBlockDisplay() {
        if (blockEntity == null) return defaultLabel();
        ItemStack item = blockEntity.getGuiToggleItem();
        // getHoverName, not getDisplayName: the latter wraps the name in square brackets.
        return item.isEmpty() ? defaultLabel() : item.getHoverName();
    }
    
    /** Returns display text for base block preview. */
    public Component getBaseBlockDisplay() {
        if (blockEntity == null) return defaultLabel();
        ItemStack item = blockEntity.getGuiBaseItem();
        // getHoverName, not getDisplayName: the latter wraps the name in square brackets.
        return item.isEmpty() ? defaultLabel() : item.getHoverName();
    }
    
    /**
     * Toggle face variable. Deliberately a String and deliberately NOT translated: this is the block's
     * own JSON texture variable name, which face selection matches exactly.
     */
    public String getToggleFaceDisplay() {
        if (blockEntity == null) return "all";
        return blockEntity.getToggleTextureVariable();
    }
    
    /** Base face variable. Data, not UI text. See getToggleFaceDisplay. */
    public String getBaseFaceDisplay() {
        if (blockEntity == null) return "all";
        return blockEntity.getBaseTextureVariable();
    }
    
    /** Toggle rotation, as degrees. Language neutral, so it stays a String. */
    public String getToggleRotationDisplay() {
        if (blockEntity == null) return net.justsomeswitches.util.TextureRotation.NORMAL.getDisplayName();
        return blockEntity.getToggleTextureRotation().getDisplayName();
    }
    
    /** Base rotation, as degrees. Language neutral, so it stays a String. */
    public String getBaseRotationDisplay() {
        if (blockEntity == null) return net.justsomeswitches.util.TextureRotation.NORMAL.getDisplayName();
        return blockEntity.getBaseTextureRotation().getDisplayName();
    }
    
    /**
     * Indicator mode. Shares PowerMode's translatable name with the dropdown and the brush tooltip.
     * The old code lowercased the enum name, so this screen showed "none_toggle".
     */
    public Component getIndicatorsDisplay() {
        if (blockEntity == null) return defaultLabel();
        return blockEntity.getPowerMode().getDisplayName();
    }

    /** Shared "no custom setting" label. */
    private static Component defaultLabel() {
        return Component.translatable("gui.justsomeswitches.copy.default");
    }
    
    /** Returns toggle block ItemStack for preview. */
    public ItemStack getToggleBlockItemStack() {
        if (blockEntity == null) return ItemStack.EMPTY;
        return blockEntity.getGuiToggleItem();
    }
    
    /** Returns base block ItemStack for preview. */
    public ItemStack getBaseBlockItemStack() {
        if (blockEntity == null) return ItemStack.EMPTY;
        return blockEntity.getGuiBaseItem();
    }
    
    /** Returns toggle texture path for face preview. */
    public String getToggleTexturePathForPreview() {
        if (blockEntity == null) return null;
        String texturePath = blockEntity.getToggleTexturePath();
        return texturePath.equals(net.justsomeswitches.blockentity.SwitchBlockEntity.DEFAULT_TOGGLE_TEXTURE) ? null : texturePath;
    }
    
    /** Returns base texture path for face preview. */
    public String getBaseTexturePathForPreview() {
        if (blockEntity == null) return null;
        String texturePath = blockEntity.getBaseTexturePath();
        return texturePath.equals(net.justsomeswitches.blockentity.SwitchBlockEntity.DEFAULT_BASE_TEXTURE) ? null : texturePath;
    }
    
    /** Returns toggle texture rotation. */
    public net.justsomeswitches.util.TextureRotation getToggleTextureRotation() {
        if (blockEntity == null) return net.justsomeswitches.util.TextureRotation.NORMAL;
        return blockEntity.getToggleTextureRotation();
    }
    
    /** Returns base texture rotation. */
    public net.justsomeswitches.util.TextureRotation getBaseTextureRotation() {
        if (blockEntity == null) return net.justsomeswitches.util.TextureRotation.NORMAL;
        return blockEntity.getBaseTextureRotation();
    }
    
    /** Returns power mode for indicator previews. */
    @SuppressWarnings("unused")
    public net.justsomeswitches.blockentity.SwitchBlockEntity.PowerMode getPowerMode() {
        if (blockEntity == null) return net.justsomeswitches.blockentity.SwitchBlockEntity.PowerMode.DEFAULT;
        return blockEntity.getPowerMode();
    }
    
    /** Returns unpowered texture for indicators. */
    public String getUnpoweredTexture() {
        if (blockEntity == null) return "";
        return blockEntity.getUnpoweredTexture();
    }
    
    /** Returns powered texture for indicators. */
    public String getPoweredTexture() {
        if (blockEntity == null) return "";
        return blockEntity.getPoweredTexture();
    }
    
    /** Checks if source block has custom textures. */
    @SuppressWarnings("unused")
    public boolean hasCustomTextures() {
        if (blockEntity == null) return false;
        return blockEntity.hasCustomTextures();
    }
}
