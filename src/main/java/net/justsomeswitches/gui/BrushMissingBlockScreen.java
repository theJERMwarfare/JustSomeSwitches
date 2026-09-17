package net.justsomeswitches.gui;

import net.justsomeswitches.network.NetworkHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.justsomeswitches.gui.components.FittedButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import javax.annotation.Nonnull;
import java.util.List;

/** Client-side GUI for missing block notification. */
public class BrushMissingBlockScreen extends AbstractContainerScreen<BrushMissingBlockMenu> {
    
    private static final ResourceLocation BACKGROUND_TEXTURE = 
        ResourceLocation.fromNamespaceAndPath("justsomeswitches", "textures/gui/brush_message_gui.png");
    private static final int GUI_WIDTH = 200;
    private static final int GUI_HEIGHT = 94;
    
    private final List<net.justsomeswitches.util.MissingBlock> missingBlocks;
    
    public BrushMissingBlockScreen(BrushMissingBlockMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        this.missingBlocks = menu.getMissingBlocks();
    }
    
    @Override
    protected void init() {
        super.init();
        
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        
        addRenderableWidget(new FittedButton(leftPos + 12, topPos + 61, 84, 20,
                Component.translatable("gui.justsomeswitches.missing_block.paste_available"), this::onApplyClicked));
        
        addRenderableWidget(new FittedButton(leftPos + 104, topPos + 61, 84, 20,
                Component.translatable("gui.cancel"), this::onCancelClicked));
    }
    
    @Override
    protected void renderBg(@Nonnull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND_TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
    }
    
    @Override
    protected void renderLabels(@Nonnull GuiGraphics graphics, int mouseX, int mouseY) {
        Component title = Component.translatable(missingBlocks.size() == 1
                ? "gui.justsomeswitches.missing_block.title_one"
                : "gui.justsomeswitches.missing_block.title_many");
        
        int maxHeaderWidth = 178;
        // Measure the PLURAL form, not the drawn one, so singular and plural share a scale in any language.
        int fullHeaderWidth = font.width(Component.translatable("gui.justsomeswitches.missing_block.title_many"));
        float headerScale = Math.min(1.0f, (float)maxHeaderWidth / fullHeaderWidth);
        
        graphics.pose().pushPose();
        graphics.pose().scale(headerScale, headerScale, 1.0f);
        
        int scaledHeaderWidth = (int)(font.width(title) * headerScale);
        int headerScaledX = (int)((imageWidth - scaledHeaderWidth) / 2.0f / headerScale);
        int headerScaledY = (int)(12 / headerScale);
        
        graphics.drawString(font, title, headerScaledX, headerScaledY, 0x404040, false);
        
        graphics.pose().popPose();
        
        int buttonTextColor = 0xFFFFFF;
        int shadowColor = 0xFF555555;
        int maxTextWidth = 182;
        
        if (missingBlocks.size() == 1) {
            Component missingText = missingBlocks.get(0).toDisplay();
            float baseScale = 0.7f;
            int fullTextWidth = font.width(missingText);
            float dynamicScale = Math.min(baseScale, (float)maxTextWidth / fullTextWidth);
            
            graphics.pose().pushPose();
            graphics.pose().scale(dynamicScale, dynamicScale, 1.0f);
            
            int scaledTextWidth = (int)(fullTextWidth * dynamicScale);
            int scaledX = (int)((imageWidth - scaledTextWidth) / 2.0f / dynamicScale);
            int scaledY = (int)(32 / dynamicScale);
            
            graphics.drawString(font, missingText, scaledX + 1, scaledY + 1, shadowColor, false);
            graphics.drawString(font, missingText, scaledX, scaledY, buttonTextColor, false);
            
            graphics.pose().popPose();
        } else if (missingBlocks.size() == 2) {
            int firstLineY = 29;
            int secondLineY = 38;
            
            for (int i = 0; i < 2; i++) {
                Component missingText = missingBlocks.get(i).toDisplay();
                
                float baseScale = 0.7f;
                int fullTextWidth = font.width(missingText);
                float dynamicScale = Math.min(baseScale, (float)maxTextWidth / fullTextWidth);
                
                graphics.pose().pushPose();
                graphics.pose().scale(dynamicScale, dynamicScale, 1.0f);
                
                int scaledTextWidth = (int)(fullTextWidth * dynamicScale);
                int scaledX = (int)((imageWidth - scaledTextWidth) / 2.0f / dynamicScale);
                int yPos = i == 0 ? firstLineY : secondLineY;
                int scaledY = (int)(yPos / dynamicScale);
                
                graphics.drawString(font, missingText, scaledX + 1, scaledY + 1, shadowColor, false);
                graphics.drawString(font, missingText, scaledX, scaledY, buttonTextColor, false);
                
                graphics.pose().popPose();
            }
        }
        
        Component questionText = Component.translatable("gui.justsomeswitches.missing_block.question");
        float questionScale = Math.min(0.7f, (float)maxTextWidth / font.width(questionText));
        
        graphics.pose().pushPose();
        graphics.pose().scale(questionScale, questionScale, 1.0f);
        
        int questionTextWidth = (int)(font.width(questionText) * questionScale);
        int questionScaledX = (int)((imageWidth - questionTextWidth) / 2.0f / questionScale);
        int questionScaledY = (int)(48 / questionScale);
        
        graphics.drawString(font, questionText, questionScaledX + 1, questionScaledY + 1, shadowColor, false);
        graphics.drawString(font, questionText, questionScaledX, questionScaledY, 0xFFFFCC00, false);
        
        graphics.pose().popPose();
    }
    
    private void onApplyClicked(Button button) {
        BlockPos blockPos = menu.getBlockPos();
        NetworkHandler.sendBrushMissingBlock(blockPos, true);
        onClose();
    }
    
    private void onCancelClicked(Button button) {
        BlockPos blockPos = menu.getBlockPos();
        NetworkHandler.sendBrushMissingBlock(blockPos, false);
        onClose();
    }
    

}
