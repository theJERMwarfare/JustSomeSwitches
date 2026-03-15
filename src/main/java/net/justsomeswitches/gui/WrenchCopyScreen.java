package net.justsomeswitches.gui;

import net.justsomeswitches.gui.components.CheckboxRenderer;
import net.justsomeswitches.gui.components.PreviewSystem;
import net.justsomeswitches.gui.components.CopyActionHandler;
import net.justsomeswitches.network.NetworkHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import javax.annotation.Nonnull;

/** Copy texture settings GUI with selective copying interface. */
public class WrenchCopyScreen extends AbstractContainerScreen<WrenchCopyMenu> {
    
    // UI constants
    private static final ResourceLocation GUI_TEXTURE = 
        ResourceLocation.fromNamespaceAndPath("justsomeswitches", "textures/gui/wrench_copy_gui.png");
    private static final int GUI_WIDTH = 187;
    private static final int GUI_HEIGHT = 240;
    
    // Components for rendering and interaction
    private final CheckboxRenderer checkboxRenderer = new CheckboxRenderer();
    private final PreviewSystem previewSystem = new PreviewSystem();
    private final CopyActionHandler copyActionHandler = new CopyActionHandler();
    
    // Button positions from user specifications
    private static final int SELECT_ALL_X = 12;
    private static final int SELECT_ALL_Y = 179;  // Moved up 6 pixels (185 - 6)
    private static final int CLEAR_ALL_X = 95;
    private static final int CLEAR_ALL_Y = 179;   // Moved up 6 pixels (185 - 6)
    private static final int COPY_SELECTED_X = 12;
    private static final int COPY_SELECTED_Y = 208;
    private static final int CANCEL_X = 95;
    private static final int CANCEL_Y = 208;
    private static final int BUTTON_WIDTH = 80;
    private static final int BUTTON_HEIGHT = 20;
    

    
    public WrenchCopyScreen(@Nonnull WrenchCopyMenu menu, @Nonnull Inventory playerInventory, @SuppressWarnings("unused") @Nonnull Component title) {
        super(menu, playerInventory, Component.literal("Copy Texture Settings"));
        
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        
        // Hide labels as they are rendered in renderLabels() method
        this.titleLabelX = -1000;
        this.titleLabelY = -1000;
        this.inventoryLabelX = -1000;
        this.inventoryLabelY = -1000;
    }
    
    @Override
    protected void init() {
        super.init();
        
        int guiLeft = (this.width - this.imageWidth) / 2;
        int guiTop = (this.height - this.imageHeight) / 2;
        
        // Add buttons using exact coordinates from user specifications
        addRenderableWidget(Button.builder(
            Component.literal("Select All"),
            button -> copyActionHandler.handleSelectAll(menu)
        ).bounds(guiLeft + SELECT_ALL_X, guiTop + SELECT_ALL_Y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        
        addRenderableWidget(Button.builder(
            Component.literal("Clear All"),
            button -> copyActionHandler.handleClearAll(menu)
        ).bounds(guiLeft + CLEAR_ALL_X, guiTop + CLEAR_ALL_Y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        
        addRenderableWidget(Button.builder(
            Component.literal("Copy Selected"),
            button -> copyActionHandler.handleCopySelected(menu, this::onClose)
        ).bounds(guiLeft + COPY_SELECTED_X, guiTop + COPY_SELECTED_Y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        
        addRenderableWidget(Button.builder(
            Component.literal("Cancel"),
            button -> copyActionHandler.handleCancel(this::onClose)
        ).bounds(guiLeft + CANCEL_X, guiTop + CANCEL_Y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        
        // Start batch mode - queue updates instead of executing immediately
        NetworkHandler.sendBatchUpdateControl(menu.getBlockPos(), true);
    }
    
    /** Called when screen is closed to flush pending batch updates. */
    @Override
    public void onClose() {
        // End batch mode and flush pending updates
        NetworkHandler.sendBatchUpdateControl(menu.getBlockPos(), false);
        super.onClose();
    }
    
    @Override
    protected void renderBg(@Nonnull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int guiLeft = (this.width - this.imageWidth) / 2;
        int guiTop = (this.height - this.imageHeight) / 2;
        
        // Draw GUI background image
        graphics.blit(GUI_TEXTURE, guiLeft, guiTop, 0, 0, this.imageWidth, this.imageHeight);
        
        // Draw checkboxes and previews using exact positions
        CheckboxRenderer.CheckboxPosition[] checkboxPositions = checkboxRenderer.getCheckboxPositions();
        int index = 0;
        for (CheckboxRenderer.CheckboxPosition pos : checkboxPositions) {
            checkboxRenderer.drawVanillaCheckbox(graphics, guiLeft + pos.checkboxX, guiTop + pos.checkboxY, index, mouseX, mouseY, menu);
            previewSystem.drawPreview(graphics, guiLeft + pos.previewX, guiTop + pos.previewY, index, menu, this.font);
            index++;
        }
    }
    
    @Override
    protected void renderLabels(@Nonnull GuiGraphics graphics, int mouseX, int mouseY) {
        // Draw title like default GUI headers
        graphics.drawString(this.font, this.title, 8, 6, 0x404040, false);
        
        // Draw checkbox labels with button text formatting and drop shadow
        CheckboxRenderer.CheckboxPosition[] checkboxPositions = checkboxRenderer.getCheckboxPositions();
        //noinspection ForLoopReplaceableByForEach - Index is required for positioning
        for (int i = 0; i < checkboxPositions.length; i++) {
            CheckboxRenderer.CheckboxPosition pos = checkboxPositions[i];
            int labelX = pos.checkboxX + CheckboxRenderer.getCheckboxSize() + 5; // 5px spacing after checkbox
            int labelY = (int)(pos.checkboxY + 4.5f); // Center text vertically with checkbox (moved up 1.5 pixels total)
            
            // Use same color as button text with drop shadow
            graphics.drawString(this.font, pos.label, labelX, labelY, 0xFFFFFF, true);
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Left click
            int guiLeft = (this.width - this.imageWidth) / 2;
            int guiTop = (this.height - this.imageHeight) / 2;
            
            // Use CheckboxRenderer to handle checkbox clicks
            if (checkboxRenderer.handleCheckboxClick(mouseX, mouseY, guiLeft, guiTop, menu)) {
                return true;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    

    

    

    

    

    

    

}
