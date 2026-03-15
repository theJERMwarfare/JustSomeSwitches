package net.justsomeswitches.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.justsomeswitches.gui.WrenchCopyMenu;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Handles all preview rendering for the Wrench Copy GUI.
 */
public class PreviewSystem {
    private static final int PREVIEW_SIZE = 20;
    private static final int SMALL_PREVIEW_SIZE = 6;
    /** Draws preview for the setting (matching texture customization GUI). */
    @SuppressWarnings("SameParameterValue") // Index used for different preview types
    public void drawPreview(@Nonnull GuiGraphics graphics, int x, int y, int index, 
                           @Nonnull WrenchCopyMenu menu, @Nonnull Font font) {
        switch (index) {
            case 0: // Toggle Block
                drawBlockItemPreview(graphics, x, y, menu.getToggleBlockItemStack(), font);
                break;
                
            case 1:
                drawFaceTexturePreview(graphics, x + 1, y + 1, true, menu, font);
                break;
                
            case 2: // Toggle Rotation
                drawRotationPreview(graphics, x, y, index, menu, font);
                break;
                
            case 3: // Indicators
                drawIndicatorsPreview(graphics, x, y, menu);
                break;
                
            case 4: // Base Block
                drawBlockItemPreview(graphics, x, y, menu.getBaseBlockItemStack(), font);
                break;
                
            case 5:
                drawFaceTexturePreview(graphics, x + 1, y + 1, false, menu, font);
                break;
                
            case 6: // Base Rotation
                drawRotationPreview(graphics, x, y, index, menu, font);
                break;
        }
    }
    /** Draws block item preview. */
    private void drawBlockItemPreview(@Nonnull GuiGraphics graphics, int x, int y, 
                                     @Nonnull ItemStack itemStack, @Nonnull Font font) {
        if (itemStack.isEmpty()) {
            drawCenteredTextInBox(graphics, "Default", x, y, PREVIEW_SIZE, PREVIEW_SIZE, font);
        } else {
            graphics.pose().pushPose();
            graphics.pose().translate(x + 10f, y + 10f, 0);
            graphics.pose().scale(1.125f, 1.125f, 1.0f);
            graphics.pose().translate(-8f, -8f, 0);
            graphics.renderItem(itemStack, 0, 0);
            graphics.pose().popPose();
        }
    }
    /** Draws face texture preview (matching SwitchTextureScreen style). */
    private void drawFaceTexturePreview(@Nonnull GuiGraphics graphics, int x, int y, boolean isToggle, 
                                       @Nonnull WrenchCopyMenu menu, @Nonnull Font font) {
        final int size = 18;
        try {
            String texturePath = isToggle ? menu.getToggleTexturePathForPreview() : menu.getBaseTexturePathForPreview();
            if (texturePath != null && !texturePath.isEmpty() && !"Default".equals(texturePath)) {
                net.justsomeswitches.util.TextureRotation rotation = null;
                if ((isToggle && menu.getCopyToggleRotation()) || (!isToggle && menu.getCopyBaseRotation())) {
                    rotation = isToggle ? menu.getToggleTextureRotation() : menu.getBaseTextureRotation();
                }
                drawTexturePreviewBox(graphics, x, y, size, texturePath, rotation);
            } else {
                String faceVariable = getPreviewText(isToggle ? 1 : 5, menu);
                String displayText = (!faceVariable.trim().isEmpty()) ? faceVariable : "Default";
                if ("all".equals(displayText)) {
                    displayText = "Default";
                }
                drawCenteredTextInBox(graphics, displayText, x, y, size, size, font);
            }
        } catch (Exception e) {
            graphics.fill(x, y, x + size, y + size, 0xFFFF0000);
        }
    }
    /** Draws texture preview box. */
    @SuppressWarnings("SameParameterValue")
    private void drawTexturePreviewBox(@Nonnull GuiGraphics graphics, int x, int y, int size,
                                      @Nonnull String texturePath,
                                      @Nullable net.justsomeswitches.util.TextureRotation rotation) {
        try {
            TextureAtlasSprite sprite = getTextureSprite(texturePath);
            if (sprite != null && !getSafeSpriteName(sprite).contains("missingno")) {
                graphics.fill(x, y, x + size, y + size, 0xFFFFFFFF);
                if (rotation != null && rotation != net.justsomeswitches.util.TextureRotation.NORMAL) {
                    drawRotatedTexturePreview(graphics, x, y, size, sprite, rotation);
                } else {
                    graphics.blit(x, y, 0, size, size, sprite);
                }
            } else {
                graphics.fill(x, y, x + size, y + size, 0xFFFF00FF);
            }
        } catch (Exception e) {
            graphics.fill(x, y, x + size, y + size, 0xFFFF0000);
        }
    }
    /** Draws rotated texture preview. */
    private void drawRotatedTexturePreview(@Nonnull GuiGraphics graphics, int x, int y, int size, 
                                          @Nonnull TextureAtlasSprite sprite, 
                                          @Nonnull net.justsomeswitches.util.TextureRotation rotation) {
        graphics.pose().pushPose();
        float halfSize = size / 2f;
        graphics.pose().translate(x + halfSize, y + halfSize, 0);
        graphics.pose().mulPose(new org.joml.Quaternionf().fromAxisAngleDeg(0, 0, 1, rotation.getDegrees()));
        graphics.pose().translate(-halfSize, -halfSize, 0);
        graphics.blit(0, 0, 0, size, size, sprite);
        graphics.pose().popPose();
    }
    /** Draws rotation preview (text-based). */
    private void drawRotationPreview(@Nonnull GuiGraphics graphics, int x, int y, int index, 
                                    @Nonnull WrenchCopyMenu menu, @Nonnull Font font) {
        String previewText = getPreviewText(index, menu);
        drawCenteredTextInBox(graphics, previewText, x, y, 20, 20, font);
    }
    /** Draws indicators preview (same as SwitchTextureScreen power previews). */
    private void drawIndicatorsPreview(@Nonnull GuiGraphics graphics, int x, int y, @Nonnull WrenchCopyMenu menu) {
        String unpoweredTexture = getUnpoweredTexturePreview(menu);
        String poweredTexture = getPoweredTexturePreview(menu);
        if (!unpoweredTexture.isEmpty()) {
            drawSmallTexturePreviewBox(graphics, x, y, unpoweredTexture);
        } else {
            graphics.fill(x, y, x + SMALL_PREVIEW_SIZE, y + SMALL_PREVIEW_SIZE, 0xFFCCCCCC);
        }
        if (!poweredTexture.isEmpty()) {
            drawSmallTexturePreviewBox(graphics, x, y + 8, poweredTexture);
        } else {
            graphics.fill(x, y + 8, x + SMALL_PREVIEW_SIZE, y + 8 + SMALL_PREVIEW_SIZE, 0xFFFFAAAA);
        }
    }
    /** Draws small texture preview box (6x6, same as SwitchTextureScreen). */
    private void drawSmallTexturePreviewBox(@Nonnull GuiGraphics graphics, int x, int y, @Nonnull String texturePath) {
        try {
            TextureAtlasSprite sprite = getTextureSprite(texturePath);
            if (sprite != null) {
                String spriteName = getSafeSpriteName(sprite);
                if (!spriteName.contains("missingno")) {
                    graphics.fill(x, y, x + SMALL_PREVIEW_SIZE, y + SMALL_PREVIEW_SIZE, 0xFFFFFFFF);
                    drawUVSpecificPreview(graphics, x, y, sprite);
                    return;
                }
            }
            graphics.fill(x, y, x + SMALL_PREVIEW_SIZE, y + SMALL_PREVIEW_SIZE, 0xFFCCCCCC);
        } catch (Exception e) {
            graphics.fill(x, y, x + SMALL_PREVIEW_SIZE, y + SMALL_PREVIEW_SIZE, 0xFFCCCCCC);
        }
    }
    /** UV-specific preview for power indicators (copied from SwitchTextureScreen). */
    private void drawUVSpecificPreview(@Nonnull GuiGraphics graphics, int x, int y, @Nonnull TextureAtlasSprite sprite) {
        try {
            int offsetX = 6;
            int offsetY = 18;
            int textureX = x - offsetX;
            int textureY = y - offsetY;
            graphics.enableScissor(x, y, x + SMALL_PREVIEW_SIZE, y + SMALL_PREVIEW_SIZE);
            graphics.blit(textureX, textureY, 0, 48, 48, sprite);
            graphics.disableScissor();
            
        } catch (Exception e) {
            graphics.fill(x, y, x + SMALL_PREVIEW_SIZE, y + SMALL_PREVIEW_SIZE, 0xFFCCCCCC);
        }
    }
    /** Gets unpowered texture preview path. */
    private String getUnpoweredTexturePreview(@Nonnull WrenchCopyMenu menu) {
        try {
            return menu.getUnpoweredTexture();
        } catch (Exception e) {
            return "";
        }
    }
    /** Gets powered texture preview path. */
    private String getPoweredTexturePreview(@Nonnull WrenchCopyMenu menu) {
        try {
            return menu.getPoweredTexture();
        } catch (Exception e) {
            return "";
        }
    }
    /** Draws text centered both horizontally and vertically in a specified box. */
    private void drawCenteredTextInBox(@Nonnull GuiGraphics graphics, @Nonnull String text, 
                                      int boxX, int boxY, int boxWidth, int boxHeight, @Nonnull Font font) {
        int textWidth = font.width(text);
        int textHeight = font.lineHeight;
        float scaleX = (boxWidth - 2) / (float)textWidth;
        float scaleY = (boxHeight - 2) / (float)textHeight;
        float scale = Math.min(Math.min(scaleX, scaleY), 1.0f);
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0f);
        int scaledTextWidth = (int)(textWidth * scale);
        int scaledTextHeight = (int)(textHeight * scale);
        int centeredX = (int)((boxX + (boxWidth - scaledTextWidth) / 2.0f) / scale);
        int centeredY = (int)((boxY + (boxHeight - scaledTextHeight) / 2.0f) / scale);
        if (text.endsWith("°")) {
            centeredY -= (int)(0.5f / scale);
        }
        graphics.drawString(font, text, centeredX, centeredY, 0xFFFFFF, true);
        graphics.pose().popPose();
    }
    /** Gets texture sprite (same as SwitchTextureScreen). */
    private TextureAtlasSprite getTextureSprite(@Nonnull String texturePath) {
        try {
            ResourceLocation textureLocation = ResourceLocation.parse(texturePath);
            TextureAtlasSprite sprite = Minecraft.getInstance()
                    .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                    .apply(textureLocation);

            if (sprite != null) {
                String spriteName = getSafeSpriteName(sprite);
                if (!spriteName.contains("missingno")) {
                    return sprite;
                }
            }
            if (texturePath.contains("_top") || texturePath.contains("_side") || texturePath.contains("_front")) {
                String basePath = texturePath.replaceAll("_(top|side|front)$", "");
                ResourceLocation fallbackLocation = ResourceLocation.parse(basePath);
                TextureAtlasSprite fallbackSprite = Minecraft.getInstance()
                        .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                        .apply(fallbackLocation);

                if (fallbackSprite != null) {
                    String fallbackSpriteName = getSafeSpriteName(fallbackSprite);
                    if (!fallbackSpriteName.contains("missingno")) {
                        return fallbackSprite;
                    }
                }
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }
    /** Safely retrieves sprite name without closing the sprite contents. */
    @Nonnull
    @SuppressWarnings("resource") // Sprite contents managed by Minecraft, must NOT be closed
    private String getSafeSpriteName(@Nonnull TextureAtlasSprite sprite) {
        try {
            return sprite.contents().name().toString();
        } catch (Exception e) {
            return "missingno";
        }
    }
    /** Gets preview text for given index. */
    @Nonnull
    private String getPreviewText(int index, @Nonnull WrenchCopyMenu menu) {
        try {
            String result = switch (index) {
                case 0 -> menu.getToggleBlockDisplay();
                case 1 -> menu.getToggleFaceDisplay();
                case 2 -> menu.getToggleRotationDisplay();
                case 3 -> menu.getIndicatorsDisplay();
                case 4 -> menu.getBaseBlockDisplay();
                case 5 -> menu.getBaseFaceDisplay();
                case 6 -> menu.getBaseRotationDisplay();
                default -> "Default";
            };
            if (result == null || result.trim().isEmpty()) {
                result = "Default";
            }
            return result;
        } catch (Exception e) {
            return "Default";
        }
    }
}