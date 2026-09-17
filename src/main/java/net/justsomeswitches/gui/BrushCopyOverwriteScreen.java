package net.justsomeswitches.gui;

import net.justsomeswitches.gui.components.TextFit;
import net.justsomeswitches.network.NetworkHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import javax.annotation.Nonnull;

/** Client-side GUI for brush copy overwrite confirmation. */
public class BrushCopyOverwriteScreen extends AbstractContainerScreen<BrushCopyOverwriteMenu> {
    
    private static final ResourceLocation BACKGROUND_TEXTURE = 
        ResourceLocation.fromNamespaceAndPath("justsomeswitches", "textures/gui/brush_message_gui.png");
    private static final int GUI_WIDTH = 200;
    private static final int GUI_HEIGHT = 94;
    // Widths a label may occupy before it is shrunk, matching BrushMissingBlockScreen.
    private static final int MAX_TITLE_WIDTH = 178;
    private static final int MAX_TEXT_WIDTH = 182;
    private static final float BODY_SCALE = 0.7f;
    
    public BrushCopyOverwriteScreen(BrushCopyOverwriteMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }
    
    @Override
    protected void init() {
        super.init();
        
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        
        addRenderableWidget(Button.builder(Component.translatable("gui.justsomeswitches.copy_overwrite.copy_new"), this::onOverwriteClicked)
                .bounds(leftPos + 20, topPos + 61, 70, 20)
                .build());
        
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), this::onCancelClicked)
                .bounds(leftPos + 111, topPos + 61, 70, 20)
                .build());
    }
    
    @Override
    protected void renderBg(@Nonnull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND_TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
    }
    
    @Override
    protected void renderLabels(@Nonnull GuiGraphics graphics, int mouseX, int mouseY) {
        // Each block keeps the arithmetic it had and only computes the scale it used to hardcode,
        // so English lands on the original constant and renders exactly as before.
        Component titleText = Component.translatable("gui.justsomeswitches.copy_overwrite.title");
        int titleWidth = font.width(titleText);
        float titleScale = TextFit.scale(1.0f, MAX_TITLE_WIDTH, titleWidth);
        graphics.pose().pushPose();
        graphics.pose().scale(titleScale, titleScale, 1.0f);
        graphics.drawString(font, titleText, (int)((imageWidth - titleWidth * titleScale) / 2 / titleScale),
                (int)(12 / titleScale), 0x404040, false);
        graphics.pose().popPose();

        Component questionText = Component.translatable("gui.justsomeswitches.copy_overwrite.question");
        int questionWidth = font.width(questionText);
        float questionScale = TextFit.scale(BODY_SCALE, MAX_TEXT_WIDTH, questionWidth);
        graphics.pose().pushPose();
        graphics.pose().scale(questionScale, questionScale, 1.0f);
        int questionX = (int)((imageWidth - questionWidth * questionScale) / 2 / questionScale);
        graphics.drawString(font, questionText, questionX + 1, (int)((32 + 1) / questionScale), 0xFF555555, false);
        graphics.drawString(font, questionText, questionX, (int)(32 / questionScale), 0xFFFFFF, false);
        graphics.pose().popPose();

        Component warningText = Component.translatable("gui.justsomeswitches.copy_overwrite.warning");
        int warningWidth = font.width(warningText);
        float warningScale = TextFit.scale(BODY_SCALE, MAX_TEXT_WIDTH, warningWidth);
        graphics.pose().pushPose();
        graphics.pose().scale(warningScale, warningScale, 1.0f);
        int warningX = (int)((imageWidth - warningWidth * warningScale) / 2 / warningScale);
        graphics.drawString(font, warningText, warningX + 1, (int)((48 + 1) / warningScale), 0xFF555555, false);
        graphics.drawString(font, warningText, warningX, (int)(48 / warningScale), 0xFFFFCC00, false);
        graphics.pose().popPose();
    }
    
    private void onOverwriteClicked(Button button) {
        BlockPos blockPos = menu.getBlockPos();
        NetworkHandler.sendBrushCopyOverwrite(blockPos, true);
        onClose();
    }
    
    private void onCancelClicked(Button button) {
        BlockPos blockPos = menu.getBlockPos();
        NetworkHandler.sendBrushCopyOverwrite(blockPos, false);
        onClose();
    }
    

}
