package net.justsomeswitches.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Optional;

/**
 * Handles detection and processing of Connected Textures Mod (CTM) and Fusion blocks.
 * Extracts standalone base texture (tile 0) from 47-texture CTM systems.
 * Zero overhead when CTM/Fusion not installed (cached mod detection).
 */
public class ConnectedTextureHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectedTextureHandler.class);
    private static Boolean ctmLoaded = null;
    /** Checks if CTM or Fusion mod is loaded (cached after first call). */
    private static boolean isConnectedTextureModLoaded() {
        if (ctmLoaded == null) {
            ctmLoaded = ModList.get().isLoaded("ctm") || ModList.get().isLoaded("fusion");
        }
        return ctmLoaded;
    }
    /** Attempts to extract base texture (tile 0) from CTM block using .mcmeta or naming conventions. */
    @Nonnull
    public static Optional<String> getBaseTexture(@Nonnull ResourceLocation texturePath) {
        if (!isConnectedTextureModLoaded()) {
            return Optional.empty();
        }
        try {
            ResourceLocation metaLocation = ResourceLocation.fromNamespaceAndPath(
                texturePath.getNamespace(),
                "textures/" + texturePath.getPath() + ".png.mcmeta"
            );
            Resource resource = Minecraft.getInstance()
                .getResourceManager()
                .getResourceOrThrow(metaLocation);
            try (Reader reader = new InputStreamReader(resource.open())) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                if (json.has("ctm")) {
                    JsonObject ctm = json.getAsJsonObject("ctm");
                    if (ctm.has("tiles")) {
                        JsonArray tiles = ctm.getAsJsonArray("tiles");
                        if (!tiles.isEmpty()) {
                            String baseTile = tiles.get(0).getAsString();
                            if (baseTile.contains("-")) {
                                baseTile = baseTile.split("-")[0];
                            }
                            String baseTexturePath = texturePath.getNamespace() + ":"
                                    + texturePath.getPath() + "_" + baseTile;
                            return Optional.of(baseTexturePath);
                        }
                    }
                    return tryCommonBaseTextureNames(texturePath);
                }
            }
        } catch (Exception e) {
            LOGGER.debug("No CTM metadata for {}", texturePath);
        }
        return tryCommonBaseTextureNames(texturePath);
    }
    /** Attempts to find base texture using common naming conventions (_base, _0, _standalone). */
    @Nonnull
    private static Optional<String> tryCommonBaseTextureNames(@Nonnull ResourceLocation texturePath) {
        String[] commonSuffixes = {"_base", "_0", "_standalone"};
        for (String suffix : commonSuffixes) {
            ResourceLocation candidatePath = ResourceLocation.fromNamespaceAndPath(
                texturePath.getNamespace(),
                "textures/" + texturePath.getPath() + suffix + ".png"
            );
            try {
                Minecraft.getInstance()
                    .getResourceManager()
                    .getResourceOrThrow(candidatePath);
                return Optional.of(texturePath.getNamespace() + ":" + texturePath.getPath() + suffix);
            } catch (Exception ignored) {
                // Candidate not found, try next suffix
            }
        }
        return Optional.empty();
    }
}
