package net.justsomeswitches.gui;

import net.justsomeswitches.block.ISwitchBlock;
import net.justsomeswitches.blockentity.SwitchBlockEntity;
import net.justsomeswitches.gui.components.DropdownManager;
import net.justsomeswitches.gui.components.FaceSelectionHandler;
import net.justsomeswitches.gui.components.TexturePreviewRenderer;
import net.justsomeswitches.network.NetworkHandler;
import net.justsomeswitches.util.TightSwitchShapes.SwitchModelType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;

import javax.annotation.Nonnull;

/**
 * Main screen for switch texture customization GUI.
 * Coordinates TexturePreviewRenderer, DropdownManager, and FaceSelectionHandler components
 * to provide real-time 2D/3D previews and interactive texture selection.
 */
public class SwitchesTextureScreen extends AbstractContainerScreen<SwitchesTextureMenu> {

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 176;

    private static final ResourceLocation GUI_BACKGROUND = new ResourceLocation("justsomeswitches", "textures/gui/switch_texture_gui.png");

    // 3D Preview
    private static final int PREVIEW_CENTER_X = 81;
    private static final int PREVIEW_CENTER_Y = 39;

    // Face selection dropdowns
    private static final int LEFT_FACE_X = 11;
    private static final int LEFT_FACE_Y = 50;
    private static final int RIGHT_FACE_X = 119;
    private static final int RIGHT_FACE_Y = 50;
    private static final int FACE_DROPDOWN_HEIGHT = 12;

    // Rotation dropdowns
    private static final int LEFT_ROTATION_X = 26;
    private static final int LEFT_ROTATION_Y = 65;
    private static final int RIGHT_ROTATION_X = 119;
    private static final int RIGHT_ROTATION_Y = 65;
    private static final int ROTATION_DROPDOWN_HEIGHT = 12;

    // Texture previews
    private static final int LEFT_PREVIEW_X = 36;
    private static final int LEFT_PREVIEW_Y = 28;
    private static final int RIGHT_PREVIEW_X = 122;
    private static final int RIGHT_PREVIEW_Y = 28;

    // Power dropdown
    private static final int POWER_DROPDOWN_X = 65;
    private static final int POWER_DROPDOWN_Y = 50;
    private static final int POWER_DROPDOWN_HEIGHT = 12;

    // Power previews
    private static final int UNPOWERED_PREVIEW_X = 64;
    private static final int UNPOWERED_PREVIEW_Y = 64;
    private static final int POWERED_PREVIEW_X = 69;
    private static final int POWERED_PREVIEW_Y = 73;

    // Power labels
    private static final int UNPOWERED_LABEL_X = 73;
    private static final int UNPOWERED_LABEL_Y = 64;
    private static final int POWERED_LABEL_X = 78;
    private static final int POWERED_LABEL_Y = 73;




    // Component for handling all dropdown UI and interactions
    private DropdownManager dropdownManager;

    // Component for handling face selection state management
    private FaceSelectionHandler faceSelectionHandler;

    // Component for handling all preview rendering
    private final TexturePreviewRenderer previewRenderer;

    // Cached power modes for the current block variant
    private SwitchBlockEntity.PowerMode[] powerModes;

    /** Creates a new switch texture customization screen. */
    public SwitchesTextureScreen(@Nonnull SwitchesTextureMenu menu, @Nonnull Inventory playerInventory, @Nonnull Component title) {
        super(menu, playerInventory, title);

        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;

        this.titleLabelX = 8;
        this.titleLabelY = 10;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 86;

        // Initialize preview renderer component
        this.previewRenderer = new TexturePreviewRenderer(menu, this.font);
    }
    
    // Note: Preview rendering methods moved to TexturePreviewRenderer component

    /** Initializes screen components. Order is critical: DropdownManager requires Font from super.init(), FaceSelectionHandler depends on DropdownManager. */
    @Override
    protected void init() {
        super.init();

        // Update preview renderer font (null during constructor, available after super.init)
        this.previewRenderer.setFont(this.font);

        // Initialize dropdown manager here (font must be available)
        this.dropdownManager = new DropdownManager(menu, this.font);

        // Cache power modes for this block variant
        this.powerModes = DropdownManager.getModesForVariant(getBlockModelType());

        // Initialize face selection handler (must be after dropdown manager)
        this.faceSelectionHandler = new FaceSelectionHandler(menu, dropdownManager);

        menu.completeInitialization();

        faceSelectionHandler.updateUIState();
        
        // Start batch mode - queue updates instead of executing immediately
        BlockPos blockPos = menu.getBlockPos();
        if (blockPos != null) {
            NetworkHandler.sendBatchUpdateControl(blockPos, true);
        }
    }
    
    /** Returns the SwitchModelType for the block this GUI is editing. */
    private SwitchModelType getBlockModelType() {
        if (menu.getBlockPos() != null && menu.getLevel() != null) {
            Block block = menu.getLevel().getBlockState(menu.getBlockPos()).getBlock();
            if (block instanceof ISwitchBlock switchBlock) {
                return switchBlock.getSwitchModelType();
            }
        }
        return SwitchModelType.LEVER;
    }

    /** Flushes pending batch updates and clears texture caches when screen closes. */
    @Override
    public void onClose() {
        // End batch mode and flush pending updates
        BlockPos blockPos = menu.getBlockPos();
        if (blockPos != null) {
            NetworkHandler.sendBatchUpdateControl(blockPos, false);
        }
        
        // Clear texture caches to free memory
        previewRenderer.clearCaches();
        
        // Clear component references for faster GC
        dropdownManager = null;
        faceSelectionHandler = null;
        
        super.onClose();
    }

    /** Updates face selection state to detect item changes in slots. */
    @Override
    public void containerTick() {
        super.containerTick();
        faceSelectionHandler.updateUIState();
    }



    // Note: drawLive3DPreview moved to TexturePreviewRenderer component




    /**
     * Handles mouse hover detection - allows normal hover even with dropdowns open.
     */
    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        // Allow normal hover detection
        return super.isMouseOver(mouseX, mouseY);
    }
    
    /**
     * Prevents slot clicking when dropdowns are open.
     */
    @Override 
    protected void slotClicked(@Nonnull net.minecraft.world.inventory.Slot slot, int slotId, int mouseButton, @Nonnull net.minecraft.world.inventory.ClickType type) {
        // Block slot clicks when any dropdown is open
        if (dropdownManager.isAnyDropdownOpen()) {
            return;  // Don't process slot click
        }
        super.slotClicked(slot, slotId, mouseButton, type);
    }
    
    /** Handles mouse click events for dropdown interactions. CRITICAL: Intercepts clicks before parent class processing. */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Only process left-click (button 0) for dropdown interactions
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        
        int guiLeft = (this.width - this.imageWidth) / 2;
        int guiTop = (this.height - this.imageHeight) / 2;

        // PRIORITY 1: Handle dropdown popup selection clicks FIRST (when dropdowns are open)
        // Only process selections if the corresponding dropdown is actually open
        if (dropdownManager.isLeftDropdownOpen()) {
            if (dropdownManager.handleFaceDropdownSelection(mouseX, mouseY, guiLeft + LEFT_FACE_X, 
                    guiTop + LEFT_FACE_Y + FACE_DROPDOWN_HEIGHT, faceSelectionHandler.getLeftTextureSelection(), true)) {
                return true;
            }
        }
        
        if (dropdownManager.isRightDropdownOpen()) {
            if (dropdownManager.handleFaceDropdownSelection(mouseX, mouseY, guiLeft + RIGHT_FACE_X, 
                    guiTop + RIGHT_FACE_Y + FACE_DROPDOWN_HEIGHT, faceSelectionHandler.getRightTextureSelection(), false)) {
                return true;
            }
        }
        
        if (dropdownManager.isPowerDropdownOpen()) {
            if (dropdownManager.handlePowerDropdownSelection(mouseX, mouseY, guiLeft + POWER_DROPDOWN_X,
                    guiTop + POWER_DROPDOWN_Y + POWER_DROPDOWN_HEIGHT, powerModes)) {
                return true;
            }
        }
        
        if (dropdownManager.isLeftRotationDropdownOpen()) {
            if (dropdownManager.handleToggleRotationDropdownSelection(mouseX, mouseY, guiLeft + LEFT_ROTATION_X, 
                    guiTop + LEFT_ROTATION_Y + ROTATION_DROPDOWN_HEIGHT)) {
                return true;
            }
        }
        
        if (dropdownManager.isRightRotationDropdownOpen()) {
            if (dropdownManager.handleBaseRotationDropdownSelection(mouseX, mouseY, guiLeft + RIGHT_ROTATION_X, 
                    guiTop + RIGHT_ROTATION_Y + ROTATION_DROPDOWN_HEIGHT)) {
                return true;
            }
        }

        // PRIORITY 2: If any dropdown is open and click wasn't in dropdown area, close all dropdowns
        if (dropdownManager.isAnyDropdownOpen()) {
            dropdownManager.closeAllDropdowns();
            return true; // Event consumed - closed dropdowns, don't pass to parent
        }

        // PRIORITY 3: Handle dropdown button clicks (to open dropdowns)
        if (dropdownManager.isWithinFaceDropdownBounds(mouseX, mouseY, guiLeft + LEFT_FACE_X, guiTop + LEFT_FACE_Y)) {
            if (faceSelectionHandler.getLeftTextureSelection().enabled()) {
                dropdownManager.toggleLeftDropdown();
                return true;
            }
        }
        
        if (dropdownManager.isWithinFaceDropdownBounds(mouseX, mouseY, guiLeft + RIGHT_FACE_X, guiTop + RIGHT_FACE_Y)) {
            if (faceSelectionHandler.getRightTextureSelection().enabled()) {
                dropdownManager.toggleRightDropdown();
                return true;
            }
        }
        
        if (dropdownManager.isWithinPowerDropdownBounds(mouseX, mouseY, guiLeft + POWER_DROPDOWN_X, guiTop + POWER_DROPDOWN_Y)) {
            dropdownManager.togglePowerDropdown();
            return true;
        }
        
        if (dropdownManager.isWithinRotationDropdownBounds(mouseX, mouseY, guiLeft + LEFT_ROTATION_X, guiTop + LEFT_ROTATION_Y)) {
            if (faceSelectionHandler.hasToggleTextureBlock()) {
                dropdownManager.toggleToggleRotationDropdown();
                return true;
            }
        }
        
        if (dropdownManager.isWithinRotationDropdownBounds(mouseX, mouseY, guiLeft + RIGHT_ROTATION_X, guiTop + RIGHT_ROTATION_Y)) {
            if (faceSelectionHandler.hasBaseTextureBlock()) {
                dropdownManager.toggleBaseRotationDropdown();
                return true;
            }
        }

        // PRIORITY 4: Let parent class handle normal GUI interactions (slots, etc.)
        return super.mouseClicked(mouseX, mouseY, button);
    }



    /**
     * Renders background layer: GUI texture, 3D preview, dropdowns, power elements, and 2D previews.
     * Order matters for proper z-level layering.
     */
    @Override
    protected void renderBg(@Nonnull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int guiLeft = (this.width - this.imageWidth) / 2;
        int guiTop = (this.height - this.imageHeight) / 2;

        // Draw GUI background
        graphics.blit(GUI_BACKGROUND, guiLeft, guiTop + 4, 0, 0, this.imageWidth, this.imageHeight, 256, 256);

        // Draw live 3D switch preview with current textures
        previewRenderer.renderLive3DPreview(graphics, guiLeft + PREVIEW_CENTER_X, guiTop + PREVIEW_CENTER_Y,
                faceSelectionHandler.getLeftTextureSelection(), faceSelectionHandler.getRightTextureSelection());

        // Draw face selection dropdowns
        drawCleanArchitectureDropdowns(graphics, guiLeft, guiTop);

        // Draw power category dropdown and previews
        drawPowerCategoryElements(graphics, guiLeft, guiTop);

        // Draw working 2D texture previews with null safety
        previewRenderer.render2DTexturePreviews(graphics,
                guiLeft + LEFT_PREVIEW_X, guiTop + LEFT_PREVIEW_Y,
                guiLeft + RIGHT_PREVIEW_X, guiTop + RIGHT_PREVIEW_Y,
                faceSelectionHandler.getLeftTextureSelection(), faceSelectionHandler.getRightTextureSelection());
    }

    // Note: Basic 3D preview and fallback text methods moved to TexturePreviewRenderer

    /** Draws face selection and rotation dropdown buttons. */
    private void drawCleanArchitectureDropdowns(@Nonnull GuiGraphics graphics, int guiLeft, int guiTop) {
        // Left (toggle) face selection dropdown
        dropdownManager.renderFaceDropdownButton(graphics, guiLeft + LEFT_FACE_X, guiTop + LEFT_FACE_Y,
                faceSelectionHandler.getLeftTextureSelection(), dropdownManager.isLeftDropdownOpen());

        // Right (base) face selection dropdown
        dropdownManager.renderFaceDropdownButton(graphics, guiLeft + RIGHT_FACE_X, guiTop + RIGHT_FACE_Y,
                faceSelectionHandler.getRightTextureSelection(), dropdownManager.isRightDropdownOpen());

        // Left (toggle) rotation dropdown
        dropdownManager.renderRotationDropdownButton(graphics, guiLeft + LEFT_ROTATION_X, guiTop + LEFT_ROTATION_Y,
                menu.getToggleTextureRotation(), dropdownManager.isLeftRotationDropdownOpen(),
                faceSelectionHandler.hasToggleTextureBlock());

        // Right (base) rotation dropdown
        dropdownManager.renderRotationDropdownButton(graphics, guiLeft + RIGHT_ROTATION_X, guiTop + RIGHT_ROTATION_Y,
                menu.getBaseTextureRotation(), dropdownManager.isRightRotationDropdownOpen(),
                faceSelectionHandler.hasBaseTextureBlock());
    }





    // Note: 2D texture preview methods moved to TexturePreviewRenderer

    /** Renders entire screen with dropdown popups overlaid for proper z-ordering. Tooltips are suppressed when dropdowns are open. */
    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // ALWAYS render the base screen with container slots
        super.render(graphics, mouseX, mouseY, partialTick);
        
        // Only render tooltips when dropdowns are NOT open
        if (!dropdownManager.isAnyDropdownOpen()) {
            renderTooltip(graphics, mouseX, mouseY);
        }

        // Render dropdown popups for proper z-order with hover support
        int guiLeft = (this.width - this.imageWidth) / 2;
        int guiTop = (this.height - this.imageHeight) / 2;

        if (dropdownManager.isLeftDropdownOpen()) {
            dropdownManager.renderFaceDropdownPopup(graphics, guiLeft + LEFT_FACE_X, 
                    guiTop + LEFT_FACE_Y + FACE_DROPDOWN_HEIGHT, faceSelectionHandler.getLeftTextureSelection(), mouseX, mouseY);
        }

        if (dropdownManager.isRightDropdownOpen()) {
            dropdownManager.renderFaceDropdownPopup(graphics, guiLeft + RIGHT_FACE_X, 
                    guiTop + RIGHT_FACE_Y + FACE_DROPDOWN_HEIGHT, faceSelectionHandler.getRightTextureSelection(), mouseX, mouseY);
        }

        if (dropdownManager.isPowerDropdownOpen()) {
            dropdownManager.renderPowerDropdownPopup(graphics, guiLeft + POWER_DROPDOWN_X,
                    guiTop + POWER_DROPDOWN_Y + POWER_DROPDOWN_HEIGHT, menu.getPowerMode(), mouseX, mouseY, powerModes);
        }

        if (dropdownManager.isLeftRotationDropdownOpen()) {
            dropdownManager.renderRotationDropdownPopup(graphics, guiLeft + LEFT_ROTATION_X, 
                    guiTop + LEFT_ROTATION_Y + ROTATION_DROPDOWN_HEIGHT, menu.getToggleTextureRotation(), mouseX, mouseY);
        }

        if (dropdownManager.isRightRotationDropdownOpen()) {
            dropdownManager.renderRotationDropdownPopup(graphics, guiLeft + RIGHT_ROTATION_X, 
                    guiTop + RIGHT_ROTATION_Y + ROTATION_DROPDOWN_HEIGHT, menu.getBaseTextureRotation(), mouseX, mouseY);
        }
    }


    /** Draws power mode dropdown, unpowered/powered texture previews, and labels. */
    private void drawPowerCategoryElements(@Nonnull GuiGraphics graphics, int guiLeft, int guiTop) {
        // Draw power mode dropdown
        dropdownManager.renderPowerDropdownButton(graphics, guiLeft + POWER_DROPDOWN_X, guiTop + POWER_DROPDOWN_Y,
                menu.getPowerMode(), dropdownManager.isPowerDropdownOpen());
        
        // Draw power texture previews
        previewRenderer.renderPowerTexturePreviews(graphics,
                guiLeft + UNPOWERED_PREVIEW_X, guiTop + UNPOWERED_PREVIEW_Y,
                guiLeft + POWERED_PREVIEW_X, guiTop + POWERED_PREVIEW_Y);
        
        // Draw power labels
        drawPowerLabels(graphics, guiLeft, guiTop);
    }



    // Note: Power texture preview methods moved to TexturePreviewRenderer component

    /** Draws "Unpowered" and "Powered" labels at 75% scale. */
    private void drawPowerLabels(@Nonnull GuiGraphics graphics, int guiLeft, int guiTop) {
        // Draw "Unpowered" label with 75% scale
        graphics.pose().pushPose();
        graphics.pose().scale(0.75f, 0.75f, 1.0f);
        graphics.drawString(this.font, "Unpowered", (int)((guiLeft + UNPOWERED_LABEL_X) / 0.75f), (int)((guiTop + UNPOWERED_LABEL_Y) / 0.75f), 0xFF404040, false);
        graphics.pose().popPose();
        
        // Draw "Powered" label with 75% scale
        graphics.pose().pushPose();
        graphics.pose().scale(0.75f, 0.75f, 1.0f);
        graphics.drawString(this.font, "Powered", (int)((guiLeft + POWERED_LABEL_X) / 0.75f), (int)((guiTop + POWERED_LABEL_Y) / 0.75f), 0xFF404040, false);
        graphics.pose().popPose();
    }

    /** Renders title and player inventory labels. */
    @Override
    protected void renderLabels(@Nonnull GuiGraphics graphics, int mouseX, int mouseY) {
        // Draw title
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);

        // Draw player inventory label
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }
}