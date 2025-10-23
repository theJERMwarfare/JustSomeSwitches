package net.justsomeswitches.client.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/** Custom geometry loader for switches with JSON parsing. */
public class SwitchesGeometryLoader implements IGeometryLoader<SwitchesGeometry> {

    public static final SwitchesGeometryLoader INSTANCE = new SwitchesGeometryLoader();
    public static final ResourceLocation ID = new ResourceLocation(
            "justsomeswitches", "switches_loader");

    @Override
    @Nonnull
    public SwitchesGeometry read(@Nonnull JsonObject jsonObject,
                              @Nonnull JsonDeserializationContext context) {
        Map<String, String> baseTextures = new HashMap<>();
        Map<String, String> toggleTextures = new HashMap<>();
        Map<String, String> powerTextures = new HashMap<>();
        if (jsonObject.has("texture_categories")) {
            JsonObject textureCategories = jsonObject.getAsJsonObject("texture_categories");
            baseTextures = parseTextureCategory(textureCategories, "base");
            toggleTextures = parseTextureCategory(textureCategories, "toggle");
            powerTextures = parseTextureCategory(textureCategories, "power");
        }
        String baseModelLocation = "minecraft:block/lever";
        if (jsonObject.has("base_model")) {
            baseModelLocation = jsonObject.get("base_model").getAsString();
        }
        PowerModeConfig powerModeConfig = parsePowerModeConfig(jsonObject);
        return new SwitchesGeometry(
                baseTextures,
                toggleTextures,
                powerTextures,
                powerModeConfig,
                baseModelLocation
        );
    }

    /** Parses texture category from JSON. */
    @Nonnull
    private Map<String, String> parseTextureCategory(@Nonnull JsonObject textureCategories,
                                                    @Nonnull String categoryName) {
        Map<String, String> textures = new HashMap<>();
        if (textureCategories.has(categoryName)) {
            JsonObject category = textureCategories.getAsJsonObject(categoryName);
            for (Map.Entry<String, JsonElement> entry : category.entrySet()) {
                String key = entry.getKey();
                String texturePath = entry.getValue().getAsString();
                textures.put(key, texturePath);
            }
        }
        if (textures.isEmpty()) {
            switch (categoryName) {
                case "base":
                    textures.put("default", "minecraft:block/stone");
                    break;
                case "toggle":
                    textures.put("default", "minecraft:block/oak_planks");
                    break;
                case "power":
                    textures.put("unpowered", "minecraft:block/gray_concrete_powder");
                    textures.put("powered", "minecraft:block/redstone_block");
                    break;
            }
        }
        return textures;
    }

    /** Parses power mode configuration from JSON. */
    @Nonnull
    private SwitchesGeometryLoader.PowerModeConfig parsePowerModeConfig(@Nonnull JsonObject jsonObject) {
        if (jsonObject.has("power_modes")) {
            JsonObject powerModes = jsonObject.getAsJsonObject("power_modes");
            String altUnpowered = "minecraft:block/redstone_block";
            String altPowered = "minecraft:block/lime_concrete_powder";
            if (powerModes.has("alt")) {
                JsonObject altMode = powerModes.getAsJsonObject("alt");
                altUnpowered = altMode.has("unpowered") ?
                        altMode.get("unpowered").getAsString() : altUnpowered;
                altPowered = altMode.has("powered") ?
                        altMode.get("powered").getAsString() : altPowered;
            }
            return new SwitchesGeometryLoader.PowerModeConfig(altUnpowered, altPowered);
        }
        return new SwitchesGeometryLoader.PowerModeConfig(
                "minecraft:block/redstone_block",
                "minecraft:block/lime_concrete_powder"
        );
    }

    /** Data class for power mode configuration. */
    public static class PowerModeConfig {
        public final String altUnpoweredTexture;
        public final String altPoweredTexture;

        public PowerModeConfig(@Nonnull String altUnpowered, @Nonnull String altPowered) {
            this.altUnpoweredTexture = altUnpowered;
            this.altPoweredTexture = altPowered;
        }
    }
}
