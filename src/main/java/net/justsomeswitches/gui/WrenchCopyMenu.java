package net.justsomeswitches.gui;

import net.justsomeswitches.blockentity.SwitchesLeverBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nonnull;

/**
 * Container menu for the Wrench Copy Settings GUI
 * Handles server-side logic for copy operation with selective settings
 */
public class WrenchCopyMenu extends AbstractContainerMenu {
    
    private final BlockPos blockPos;
    private final ContainerLevelAccess levelAccess;
    private SwitchesLeverBlockEntity blockEntity;
    
    // Copy selection state - tracks which settings are selected for copying
    private boolean copyToggleBlock = true;
    private boolean copyToggleFace = true;
    private boolean copyToggleRotation = true;
    private boolean copyIndicators = true;
    private boolean copyBaseBlock = true;
    private boolean copyBaseFace = true;
    private boolean copyBaseRotation = true;
    
    /**
     * Client-side constructor (required by NeoForge framework)
     */
    @SuppressWarnings("unused")
    public WrenchCopyMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, extraData.readBlockPos());
    }
    
    /**
     * Server-side constructor
     */
    public WrenchCopyMenu(int containerId, Inventory playerInventory, BlockPos blockPos) {
        super(JustSomeSwitchesMenuTypes.WRENCH_COPY.get(), containerId);
        
        this.blockPos = blockPos != null ? blockPos : BlockPos.ZERO;
        this.levelAccess = ContainerLevelAccess.create(playerInventory.player.level(), this.blockPos);
        
        // Initialize block entity reference with null safety
        this.levelAccess.execute((level, pos) -> {
            try {
                BlockEntity entity = level.getBlockEntity(pos);
                if (entity instanceof SwitchesLeverBlockEntity leverEntity) {
                    this.blockEntity = leverEntity;
                }
            } catch (Exception e) {
                // Graceful fallback - block entity will remain null
                this.blockEntity = null;
            }
        });
    }
    
    @Override
    @Nonnull
    public ItemStack quickMoveStack(@Nonnull Player player, int index) {
        // No slot interactions in copy GUI
        return ItemStack.EMPTY;
    }
    
    @Override
    public boolean stillValid(@Nonnull Player player) {
        if (blockEntity == null) {
            return false;
        }
        
        return levelAccess.evaluate((level, pos) -> {
            BlockEntity entity = level.getBlockEntity(pos);
            return entity instanceof SwitchesLeverBlockEntity && 
                   player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
        }, true);
    }
    
    // ========================================
    // COPY SELECTION STATE MANAGEMENT
    // ========================================
    
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
    
    /**
     * Sets all copy selections to the specified value
     */
    public void setAllCopySelections(boolean value) {
        copyToggleBlock = value;
        copyToggleFace = value;
        copyToggleRotation = value;
        copyIndicators = value;
        copyBaseBlock = value;
        copyBaseFace = value;
        copyBaseRotation = value;
    }
    
    // ========================================
    // BLOCK ENTITY DATA ACCESS
    // ========================================
    
    @SuppressWarnings("unused")
    public BlockPos getBlockPos() {
        return blockPos;
    }
    
    public SwitchesLeverBlockEntity getBlockEntity() {
        return blockEntity;
    }
    
    /**
     * Gets display text for toggle block (for preview)
     */
    public String getToggleBlockDisplay() {
        if (blockEntity == null) return "Default";
        ItemStack item = blockEntity.getGuiToggleItem();
        return item.isEmpty() ? "Default" : item.getDisplayName().getString();
    }
    
    /**
     * Gets display text for base block (for preview)
     */
    public String getBaseBlockDisplay() {
        if (blockEntity == null) return "Default";
        ItemStack item = blockEntity.getGuiBaseItem();
        return item.isEmpty() ? "Default" : item.getDisplayName().getString();
    }
    
    /**
     * Gets display text for toggle face (for preview)
     */
    public String getToggleFaceDisplay() {
        if (blockEntity == null) return "all";
        return blockEntity.getToggleTextureVariable();
    }
    
    /**
     * Gets display text for base face (for preview)
     */
    public String getBaseFaceDisplay() {
        if (blockEntity == null) return "all";
        return blockEntity.getBaseTextureVariable();
    }
    
    /**
     * Gets display text for toggle rotation (for preview)
     */
    public String getToggleRotationDisplay() {
        if (blockEntity == null) return "None";
        return blockEntity.getToggleTextureRotation().getDisplayName();
    }
    
    /**
     * Gets display text for base rotation (for preview)
     */
    public String getBaseRotationDisplay() {
        if (blockEntity == null) return "None";
        return blockEntity.getBaseTextureRotation().getDisplayName();
    }
    
    /**
     * Gets display text for indicators (for preview)
     */
    public String getIndicatorsDisplay() {
        if (blockEntity == null) return "Default";
        return blockEntity.getPowerMode().name().toLowerCase();
    }
    
    /**
     * Gets the toggle block ItemStack for preview
     */
    public ItemStack getToggleBlockItemStack() {
        if (blockEntity == null) return ItemStack.EMPTY;
        return blockEntity.getGuiToggleItem();
    }
    
    /**
     * Gets the base block ItemStack for preview
     */
    public ItemStack getBaseBlockItemStack() {
        if (blockEntity == null) return ItemStack.EMPTY;
        return blockEntity.getGuiBaseItem();
    }
    
    /**
     * Gets the toggle texture path for face preview
     */
    public String getToggleTexturePathForPreview() {
        if (blockEntity == null) return null;
        String texturePath = blockEntity.getToggleTexturePath();
        // Return null for default textures to show "Default" preview
        return texturePath.equals(net.justsomeswitches.blockentity.SwitchesLeverBlockEntity.DEFAULT_TOGGLE_TEXTURE) ? null : texturePath;
    }
    
    /**
     * Gets the base texture path for face preview
     */
    public String getBaseTexturePathForPreview() {
        if (blockEntity == null) return null;
        String texturePath = blockEntity.getBaseTexturePath();
        // Return null for default textures to show "Default" preview
        return texturePath.equals(net.justsomeswitches.blockentity.SwitchesLeverBlockEntity.DEFAULT_BASE_TEXTURE) ? null : texturePath;
    }
    
    /**
     * Gets toggle texture rotation
     */
    public net.justsomeswitches.util.TextureRotation getToggleTextureRotation() {
        if (blockEntity == null) return net.justsomeswitches.util.TextureRotation.NORMAL;
        return blockEntity.getToggleTextureRotation();
    }
    
    /**
     * Gets base texture rotation
     */
    public net.justsomeswitches.util.TextureRotation getBaseTextureRotation() {
        if (blockEntity == null) return net.justsomeswitches.util.TextureRotation.NORMAL;
        return blockEntity.getBaseTextureRotation();
    }
    
    /**
     * Gets power mode for indicator previews
     */
    @SuppressWarnings("unused")
    public net.justsomeswitches.blockentity.SwitchesLeverBlockEntity.PowerMode getPowerMode() {
        if (blockEntity == null) return net.justsomeswitches.blockentity.SwitchesLeverBlockEntity.PowerMode.DEFAULT;
        return blockEntity.getPowerMode();
    }
    
    /**
     * Gets unpowered texture for indicators
     */
    public String getUnpoweredTexture() {
        if (blockEntity == null) return "";
        return blockEntity.getUnpoweredTexture();
    }
    
    /**
     * Gets powered texture for indicators
     */
    public String getPoweredTexture() {
        if (blockEntity == null) return "";
        return blockEntity.getPoweredTexture();
    }
    
    /**
     * Checks if the source block has any custom textures
     */
    @SuppressWarnings("unused")
    public boolean hasCustomTextures() {
        if (blockEntity == null) return false;
        return blockEntity.hasCustomTextures();
    }
}
