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
        Map<String, WallOrientationData> orientationTransforms = new HashMap<>();
        if (jsonObject.has("orientations")) {
            JsonObject orientations = jsonObject.getAsJsonObject("orientations");
            orientationTransforms = parseOrientations(orientations);
        }
        Map<String, String> variableMap = new HashMap<>();
        if (jsonObject.has("variables")) {
            JsonObject variables = jsonObject.getAsJsonObject("variables");
            variableMap = parseJsonVariables(variables);
        }
        String baseModelLocation = "minecraft:block/lever";
        if (jsonObject.has("base_model")) {
            baseModelLocation = jsonObject.get("base_model").getAsString();
        }
        PowerModeConfig powerModeConfig = parsePowerModeConfig(jsonObject);
        int toggleRotationCompensation = 0;
        if (jsonObject.has("toggle_rotation_compensation")) {
            toggleRotationCompensation = jsonObject.get("toggle_rotation_compensation").getAsInt();
        }
        boolean isSlideModel = jsonObject.has("is_slide_model") &&
                jsonObject.get("is_slide_model").getAsBoolean();
        return new SwitchesGeometry(
                baseTextures,
                toggleTextures,
                powerTextures,
                orientationTransforms,
                variableMap,
                powerModeConfig,
                baseModelLocation,
                toggleRotationCompensation,
                isSlideModel
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

    /** Parses wall orientations from JSON. */
    @Nonnull
    private Map<String, SwitchesGeometryLoader.WallOrientationData> parseOrientations(@Nonnull JsonObject orientations) {
        Map<String, SwitchesGeometryLoader.WallOrientationData> orientationData = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : orientations.entrySet()) {
            String orientationName = entry.getKey();
            JsonObject orientationConfig = entry.getValue().getAsJsonObject();
            float rotX = 0.0f, rotY = 0.0f, rotZ = 0.0f;
            if (orientationConfig.has("rotation")) {
                JsonObject rotation = orientationConfig.getAsJsonObject("rotation");
                rotX = rotation.has("x") ? rotation.get("x").getAsFloat() : 0.0f;
                rotY = rotation.has("y") ? rotation.get("y").getAsFloat() : 0.0f;
                rotZ = rotation.has("z") ? rotation.get("z").getAsFloat() : 0.0f;
            }
            float transX = 0.0f, transY = 0.5f, transZ = 0.0f;
            if (orientationConfig.has("translation")) {
                JsonObject translation = orientationConfig.getAsJsonObject("translation");
                transX = translation.has("x") ? translation.get("x").getAsFloat() : 0.0f;
                transY = translation.has("y") ? translation.get("y").getAsFloat() : 0.5f;
                transZ = translation.has("z") ? translation.get("z").getAsFloat() : 0.0f;
            }
            orientationData.put(orientationName, new SwitchesGeometryLoader.WallOrientationData(
                    rotX, rotY, rotZ, transX, transY, transZ));
        }
        if (orientationData.isEmpty()) {
            orientationData.put("center", new SwitchesGeometryLoader.WallOrientationData(0, 0, 0, 0, 0.5f, 0));
            orientationData.put("left", new SwitchesGeometryLoader.WallOrientationData(0, 270, 0, -0.4f, 0.5f, 0));
            orientationData.put("right", new SwitchesGeometryLoader.WallOrientationData(0, 90, 0, 0.4f, 0.5f, 0));
            orientationData.put("top", new SwitchesGeometryLoader.WallOrientationData(-90, 0, 0, 0, 0.9f, 0));
            orientationData.put("bottom", new SwitchesGeometryLoader.WallOrientationData(90, 0, 0, 0, 0.1f, 0));
        }
        return orientationData;
    }

    /** Parses JSON variables for face selection. */
    @Nonnull
    private Map<String, String> parseJsonVariables(@Nonnull JsonObject variables) {
        Map<String, String> variableMap = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : variables.entrySet()) {
            String variableName = entry.getKey();
            String variableValue = entry.getValue().getAsString();
            variableMap.put(variableName, variableValue);
        }
        if (variableMap.isEmpty()) {
            variableMap.put("all", "all");
            variableMap.put("north", "north");
            variableMap.put("south", "south");
            variableMap.put("east", "east");
            variableMap.put("west", "west");
            variableMap.put("up", "up");
            variableMap.put("down", "down");
        }
        return variableMap;
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

    /** Data class for wall orientation configuration. */
    public static class WallOrientationData {
        public final float rotationX, rotationY, rotationZ;
        public final float translationX, translationY, translationZ;

        public WallOrientationData(float rotX, float rotY, float rotZ,
                                  float transX, float transY, float transZ) {
            this.rotationX = rotX;
            this.rotationY = rotY;
            this.rotationZ = rotZ;
            this.translationX = transX;
            this.translationY = transY;
            this.translationZ = transZ;
        }
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
