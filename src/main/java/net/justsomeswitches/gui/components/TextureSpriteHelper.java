package net.justsomeswitches.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Shared, cached resolution of texture paths to block-atlas sprites for GUI previews.
 * Used by both PreviewSystem and TexturePreviewRenderer. GUI/render-thread only, so plain
 * HashMaps are safe. Call {@link #clear()} when the owning screen closes.
 */
public class TextureSpriteHelper {
    private final Map<String, ResourceLocation> resourceLocationCache = new HashMap<>();
    private final Map<TextureAtlasSprite, String> spriteNameCache = new HashMap<>();

    private ResourceLocation getCachedResourceLocation(@Nonnull String path) {
        return resourceLocationCache.computeIfAbsent(path, ResourceLocation::parse);
    }

    /** Sprite name via cache; returns "missingno" on error. Never closes sprite contents (Minecraft owns them). */
    @Nonnull
    @SuppressWarnings("resource")
    public String getSafeSpriteName(@Nonnull TextureAtlasSprite sprite) {
        return spriteNameCache.computeIfAbsent(sprite, s -> {
            try {
                return s.contents().name().toString();
            } catch (Exception e) {
                return "missingno";
            }
        });
    }

    /** Resolves a texture path to an atlas sprite (with _top/_side/_front fallback), or null if not found. */
    @Nullable
    public TextureAtlasSprite getTextureSprite(@Nonnull String texturePath) {
        try {
            TextureAtlasSprite sprite = Minecraft.getInstance()
                    .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                    .apply(getCachedResourceLocation(texturePath));
            if (sprite != null && !getSafeSpriteName(sprite).contains("missingno")) {
                return sprite;
            }
            if (texturePath.contains("_top") || texturePath.contains("_side") || texturePath.contains("_front")) {
                String basePath = texturePath.replaceAll("_(top|side|front)$", "");
                TextureAtlasSprite fallbackSprite = Minecraft.getInstance()
                        .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                        .apply(getCachedResourceLocation(basePath));
                if (fallbackSprite != null && !getSafeSpriteName(fallbackSprite).contains("missingno")) {
                    return fallbackSprite;
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /** Clears cached resolutions (call when the owning screen closes). */
    public void clear() {
        resourceLocationCache.clear();
        spriteNameCache.clear();
    }
}
