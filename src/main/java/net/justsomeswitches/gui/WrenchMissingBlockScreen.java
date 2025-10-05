package net.justsomeswitches.gui;

import net.justsomeswitches.network.NetworkHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import javax.annotation.Nonnull;
import java.util.List;

/** Client-side GUI for missing block notification. */
public class WrenchMissingBlockScreen extends AbstractContainerScreen<WrenchMissingBlockMenu> {
    
    private static final ResourceLocation BACKGROUND_TEXTURE = 
        new ResourceLocation("justsomeswitches", "textures/gui/switches_wrench_message_gui.png");
    private static final int GUI_WIDTH = 200;
    private static final int GUI_HEIGHT = 94;
    
    private final List<String> missingBlocks;
    
    public WrenchMissingBlockScreen(WrenchMissingBlockMenu menu, Inventory playerInventory, Component title) {
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
        
        addRenderableWidget(Button.builder(Component.literal("Paste"), this::onApplyClicked)
                .bounds(leftPos + 20, topPos + 61, 70, 20)
                .build());
        
        addRenderableWidget(Button.builder(Component.literal("Cancel"), this::onCancelClicked)
                .bounds(leftPos + 111, topPos + 61, 70, 20)
                .build());
    }
    
    @Override
    protected void renderBg(@Nonnull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND_TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
    }
    
    @Override
    protected void renderLabels(@Nonnull GuiGraphics graphics, int mouseX, int mouseY) {
        String titleText = missingBlocks.size() == 1 ? "Missing Required Block In Inventory" : "Missing Required Blocks In Inventory";
        Component title = Component.literal(titleText);
        
        int maxHeaderWidth = 178;
        int fullHeaderWidth = font.width("Missing Required Blocks In Inventory");
        float headerScale = Math.min(1.0f, (float)maxHeaderWidth / fullHeaderWidth);
        
        graphics.pose().pushPose();
        graphics.pose().scale(headerScale, headerScale, 1.0f);
        
        int scaledHeaderWidth = (int)(font.width(titleText) * headerScale);
        int headerScaledX = (int)((imageWidth - scaledHeaderWidth) / 2.0f / headerScale);
        int headerScaledY = (int)(12 / headerScale);
        
        graphics.drawString(font, title, headerScaledX, headerScaledY, 0x404040, false);
        
        graphics.pose().popPose();
        
        int buttonTextColor = 0xFFFFFF;
        int shadowColor = 0xFF555555;
        int maxTextWidth = 182;
        
        if (missingBlocks.size() == 1) {
            String missingText = missingBlocks.get(0);
            float baseScale = 0.7f;
            int fullTextWidth = font.width(missingText);
            float dynamicScale = Math.min(baseScale, (float)maxTextWidth / fullTextWidth);
            
            graphics.pose().pushPose();
            graphics.pose().scale(dynamicScale, dynamicScale, 1.0f);
            
            int scaledTextWidth = (int)(fullTextWidth * dynamicScale);
            int scaledX = (int)((imageWidth - scaledTextWidth) / 2.0f / dynamicScale);
            int scaledY = (int)(33 / dynamicScale);
            
            graphics.drawString(font, missingText, scaledX + 1, scaledY + 1, shadowColor, false);
            graphics.drawString(font, missingText, scaledX, scaledY, buttonTextColor, false);
            
            graphics.pose().popPose();
        } else if (missingBlocks.size() == 2) {
            int firstLineY = 29;
            int secondLineY = 38;
            
            for (int i = 0; i < 2; i++) {
                String missingText = missingBlocks.get(i);
                
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
        
        graphics.pose().pushPose();
        graphics.pose().scale(0.7f, 0.7f, 1.0f);
        
        String questionText = "Paste other possible texture settings?";
        int questionTextWidth = (int)(font.width(questionText) * 0.7f);
        int questionScaledX = (int)((imageWidth - questionTextWidth) / 2.0f / 0.7f);
        int questionScaledY = (int)(51 / 0.7f);
        
        graphics.drawString(font, questionText, questionScaledX + 1, questionScaledY + 1, shadowColor, false);
        graphics.drawString(font, questionText, questionScaledX, questionScaledY, 0xFFFFCC00, false);
        
        graphics.pose().popPose();
    }
    
    private void onApplyClicked(Button button) {
        BlockPos blockPos = menu.getBlockPos();
        NetworkHandler.sendWrenchMissingBlock(blockPos, true);
        onClose();
    }
    
    private void onCancelClicked(Button button) {
        BlockPos blockPos = menu.getBlockPos();
        NetworkHandler.sendWrenchMissingBlock(blockPos, false);
        onClose();
    }
    

}
