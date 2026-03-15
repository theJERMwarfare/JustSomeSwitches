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

/** Client-side GUI for wrench copy overwrite confirmation. */
public class WrenchCopyOverwriteScreen extends AbstractContainerScreen<WrenchCopyOverwriteMenu> {
    
    private static final ResourceLocation BACKGROUND_TEXTURE = 
        ResourceLocation.fromNamespaceAndPath("justsomeswitches", "textures/gui/switches_wrench_message_gui.png");
    private static final int GUI_WIDTH = 200;
    private static final int GUI_HEIGHT = 94;
    
    public WrenchCopyOverwriteScreen(WrenchCopyOverwriteMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }
    
    @Override
    protected void init() {
        super.init();
        
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        
        addRenderableWidget(Button.builder(Component.literal("Copy New"), this::onOverwriteClicked)
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
        Component titleText = Component.literal("Texture Settings Already Copied");
        int titleWidth = font.width(titleText);
        graphics.drawString(font, titleText, (imageWidth - titleWidth) / 2, 12, 0x404040, false);
        
        graphics.pose().pushPose();
        graphics.pose().scale(0.7f, 0.7f, 1.0f);
        Component questionText = Component.literal("Copy new texture settings to wrench?");
        int questionWidth = font.width(questionText);
        int questionX = (int)((imageWidth - questionWidth * 0.7f) / 2 / 0.7f);
        graphics.drawString(font, questionText, questionX + 1, (int)((32 + 1) / 0.7f), 0xFF555555, false);
        graphics.drawString(font, questionText, questionX, (int)(32 / 0.7f), 0xFFFFFF, false);
        graphics.pose().popPose();
        
        graphics.pose().pushPose();
        graphics.pose().scale(0.7f, 0.7f, 1.0f);
        Component warningText = Component.literal("(Previous texture settings will be cleared)");
        int warningWidth = font.width(warningText);
        int warningX = (int)((imageWidth - warningWidth * 0.7f) / 2 / 0.7f);
        graphics.drawString(font, warningText, warningX + 1, (int)((48 + 1) / 0.7f), 0xFF555555, false);
        graphics.drawString(font, warningText, warningX, (int)(48 / 0.7f), 0xFFFFCC00, false);
        graphics.pose().popPose();
    }
    
    private void onOverwriteClicked(Button button) {
        BlockPos blockPos = menu.getBlockPos();
        NetworkHandler.sendWrenchCopyOverwrite(blockPos, true);
        onClose();
    }
    
    private void onCancelClicked(Button button) {
        BlockPos blockPos = menu.getBlockPos();
        NetworkHandler.sendWrenchCopyOverwrite(blockPos, false);
        onClose();
    }
    

}
