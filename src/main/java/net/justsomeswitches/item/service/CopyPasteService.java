package net.justsomeswitches.item.service;

import net.justsomeswitches.block.ISwitchBlock;
import net.justsomeswitches.blockentity.SwitchBlockEntity;
import net.justsomeswitches.util.InventoryHelper;
import net.justsomeswitches.util.NBTHelper;
import net.justsomeswitches.util.TextureRotation;
import net.justsomeswitches.util.TightSwitchShapes.SwitchModelType;
import net.justsomeswitches.util.BrushConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Service class for handling copy/paste operations. */
public class CopyPasteService {
    private CopyPasteService() {
        // Utility class
    }
    /** Checks if the brush has copied settings stored. */
    public static boolean hasCopiedSettings(@Nonnull ItemStack stack) {
        NBTHelper.NBTCache cache = new NBTHelper.NBTCache(stack);
        return cache.getBoolean(BrushConstants.HAS_COPIED_DATA_KEY);
    }
    /** Selective copying with performance optimizations. */
    public static void copySelectedSettings(@Nonnull ItemStack stack, @Nonnull SwitchBlockEntity blockEntity,
                                          boolean copyToggleBlock, boolean copyToggleFace, boolean copyToggleRotation,
                                          boolean copyIndicators, boolean copyBaseBlock, boolean copyBaseFace,
                                          boolean copyBaseRotation) {
        if (blockEntity.getLevel() == null) return;
        HolderLookup.Provider registries = blockEntity.getLevel().registryAccess();
        CompoundTag settingsTag = new CompoundTag();
        NBTHelper.batchNBTOperations(stack, tag -> {
            if (copyToggleFace) {
                settingsTag.putString(BrushConstants.TOGGLE_FACE_KEY, blockEntity.getToggleTextureVariable());
                settingsTag.putString(BrushConstants.TOGGLE_TEXTURE_PATH_KEY, blockEntity.getToggleTexturePath());
            }
            if (copyBaseFace) {
                settingsTag.putString(BrushConstants.BASE_FACE_KEY, blockEntity.getBaseTextureVariable());
                settingsTag.putString(BrushConstants.BASE_TEXTURE_PATH_KEY, blockEntity.getBaseTexturePath());
            }
            if (copyToggleRotation) {
                settingsTag.putString(BrushConstants.TOGGLE_ROTATION_KEY, blockEntity.getToggleTextureRotation().name());
            }
            if (copyBaseRotation) {
                settingsTag.putString(BrushConstants.BASE_ROTATION_KEY, blockEntity.getBaseTextureRotation().name());
            }
            if (copyIndicators) {
                settingsTag.putString(BrushConstants.POWER_MODE_KEY, blockEntity.getPowerMode().name());
            }
            if (copyToggleBlock && !blockEntity.getGuiToggleItem().isEmpty()) {
                settingsTag.put(BrushConstants.TOGGLE_BLOCK_KEY, blockEntity.getGuiToggleItem().saveOptional(registries));
            }
            if (copyBaseBlock && !blockEntity.getGuiBaseItem().isEmpty()) {
                settingsTag.put(BrushConstants.BASE_BLOCK_KEY, blockEntity.getGuiBaseItem().saveOptional(registries));
            }
            tag.put(BrushConstants.COPIED_SETTINGS_KEY, settingsTag);
            tag.putBoolean(BrushConstants.HAS_COPIED_DATA_KEY, true);
        });
    }
    /** Optimized settings comparison. */
    public static boolean hasIdenticalSettings(@Nonnull ItemStack stack, @Nonnull SwitchBlockEntity blockEntity) {
        NBTHelper.NBTCache cache = new NBTHelper.NBTCache(stack);
        CompoundTag settingsTag = cache.getCompound(BrushConstants.COPIED_SETTINGS_KEY);
        if (settingsTag == null || blockEntity.getLevel() == null) {
            return false;
        }
        HolderLookup.Provider registries = blockEntity.getLevel().registryAccess();
        return compareStringSetting(settingsTag, BrushConstants.TOGGLE_FACE_KEY, blockEntity.getToggleTextureVariable()) &&
               compareStringSetting(settingsTag, BrushConstants.BASE_FACE_KEY, blockEntity.getBaseTextureVariable()) &&
               compareStringSetting(settingsTag, BrushConstants.TOGGLE_ROTATION_KEY, blockEntity.getToggleTextureRotation().name()) &&
               compareStringSetting(settingsTag, BrushConstants.BASE_ROTATION_KEY, blockEntity.getBaseTextureRotation().name()) &&
               compareStringSetting(settingsTag, BrushConstants.POWER_MODE_KEY, blockEntity.getPowerMode().name()) &&
               compareItemSetting(settingsTag, BrushConstants.TOGGLE_BLOCK_KEY, blockEntity.getGuiToggleItem(), registries) &&
               compareItemSetting(settingsTag, BrushConstants.BASE_BLOCK_KEY, blockEntity.getGuiBaseItem(), registries);
    }
    private static boolean compareStringSetting(@Nonnull CompoundTag settingsTag, @Nonnull String key, @Nonnull String currentValue) {
        return settingsTag.getString(key).equals(currentValue);
    }
    private static boolean compareItemSetting(@Nonnull CompoundTag settingsTag, @Nonnull String key,
                                            @Nonnull ItemStack currentItem, @Nonnull HolderLookup.Provider registries) {
        if (settingsTag.contains(key)) {
            ItemStack storedItem = ItemStack.parseOptional(registries, settingsTag.getCompound(key));
            return ItemStack.isSameItem(storedItem, currentItem);
        } else {
            return currentItem.isEmpty();
        }
    }
    /** Optimized inventory validation with single-pass checking. */
    @Nonnull
    @SuppressWarnings("resource") // Level lifecycle managed by Minecraft
    public static List<String> validateRequiredBlocks(@Nonnull ItemStack stack, @Nonnull Player player) {
        NBTHelper.NBTCache cache = new NBTHelper.NBTCache(stack);
        CompoundTag settingsTag = cache.getCompound(BrushConstants.COPIED_SETTINGS_KEY);
        if (settingsTag == null) {
            return new ArrayList<>();
        }
        HolderLookup.Provider registries = player.level().registryAccess();
        List<ItemStack> requiredItems = new ArrayList<>();
        List<String> categories = new ArrayList<>();
        if (settingsTag.contains(BrushConstants.TOGGLE_BLOCK_KEY)) {
            requiredItems.add(ItemStack.parseOptional(registries, settingsTag.getCompound(BrushConstants.TOGGLE_BLOCK_KEY)));
            categories.add(BrushConstants.CATEGORY_TOGGLE);
        }
        if (settingsTag.contains(BrushConstants.BASE_BLOCK_KEY)) {
            requiredItems.add(ItemStack.parseOptional(registries, settingsTag.getCompound(BrushConstants.BASE_BLOCK_KEY)));
            categories.add(BrushConstants.CATEGORY_BASE);
        }
        if (requiredItems.isEmpty()) {
            return new ArrayList<>();
        }
        // Creative players are never charged, so nothing is ever missing.
        if (player.getAbilities().instabuild) {
            return new ArrayList<>();
        }
        // Tally inventory by item type, then charge each required category in order. This makes two
        // same-type requirements (e.g. the same block in toggle + base) correctly need TWO of that
        // block instead of one satisfying both categories - the fix for the paste duplication bug.
        Map<Item, Integer> available = new HashMap<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack slot = player.getInventory().getItem(i);
            if (!slot.isEmpty()) {
                available.merge(slot.getItem(), slot.getCount(), Integer::sum);
            }
        }
        List<String> missingBlocks = new ArrayList<>();
        for (int i = 0; i < requiredItems.size(); i++) {
            ItemStack required = requiredItems.get(i);
            if (required.isEmpty()) {
                continue;
            }
            int have = available.getOrDefault(required.getItem(), 0);
            if (have >= 1) {
                available.put(required.getItem(), have - 1);
            } else {
                String blockName = capitalizeFirst(required.getDisplayName().getString());
                missingBlocks.add("Missing " + blockName + " for " + categories.get(i));
            }
        }
        return missingBlocks;
    }
    @Nonnull
    private static String capitalizeFirst(@Nonnull String text) {
        return text.isEmpty() ? text : text.substring(0, 1).toUpperCase() + text.substring(1);
    }
    /** Clears all stored settings from the brush. */
    public static void clearAllSettings(@Nonnull ItemStack stack) {
        NBTHelper.NBTCache cache = new NBTHelper.NBTCache(stack);
        cache.remove(BrushConstants.COPIED_SETTINGS_KEY);
        cache.remove(BrushConstants.HAS_COPIED_DATA_KEY);
    }
    /** Result class for paste operations. */
    public static class PasteResult {
        public final boolean success;
        public final String message;
        public final List<String> missingBlocks;
        public PasteResult(boolean success, String message) {
            this.success = success;
            this.message = message;
            this.missingBlocks = new ArrayList<>();
        }
        public PasteResult(boolean success, String message, List<String> missingBlocks) {
            this.success = success;
            this.message = message;
            this.missingBlocks = missingBlocks != null ? missingBlocks : new ArrayList<>();
        }
    }
    /** Optimized paste operation with efficient inventory management. */
    @Nonnull
    @SuppressWarnings("resource") // Level lifecycle managed by Minecraft
    public static PasteResult applySettingsFromBrush(@Nonnull ItemStack stack, @Nonnull SwitchBlockEntity blockEntity, @Nonnull Player player) {
        NBTHelper.NBTCache cache = new NBTHelper.NBTCache(stack);
        CompoundTag settingsTag = cache.getCompound(BrushConstants.COPIED_SETTINGS_KEY);
        if (settingsTag == null) {
            return new PasteResult(false, "No settings to paste");
        }
        List<String> missingBlocks = validateRequiredBlocks(stack, player);
        if (!missingBlocks.isEmpty()) {
            return new PasteResult(false, BrushConstants.MSG_MISSING_BLOCKS_GUI, missingBlocks);
        }
        HolderLookup.Provider registries = player.level().registryAccess();
        applyAllSettings(settingsTag, blockEntity, player, registries);
        blockEntity.updateTextures();
        return new PasteResult(true, BrushConstants.MSG_SETTINGS_PASTED);
    }
    /** Partial paste operation - applies only settings for blocks that are available. */
    @Nonnull
    @SuppressWarnings("resource") // Level lifecycle managed by Minecraft
    public static PasteResult applyPartialSettingsFromBrush(@Nonnull ItemStack stack, @Nonnull SwitchBlockEntity blockEntity, @Nonnull Player player) {
        NBTHelper.NBTCache cache = new NBTHelper.NBTCache(stack);
        CompoundTag settingsTag = cache.getCompound(BrushConstants.COPIED_SETTINGS_KEY);
        if (settingsTag == null) {
            return new PasteResult(false, "No settings to paste");
        }
        HolderLookup.Provider registries = player.level().registryAccess();
        applyPowerMode(settingsTag, blockEntity);
        if (settingsTag.contains(BrushConstants.TOGGLE_BLOCK_KEY)) {
            ItemStack requiredToggleItem = ItemStack.parseOptional(registries, settingsTag.getCompound(BrushConstants.TOGGLE_BLOCK_KEY));
            if (InventoryHelper.hasAllItems(player, requiredToggleItem)) {
                InventoryHelper.removeItems(player, requiredToggleItem);
                blockEntity.setToggleSlotItem(requiredToggleItem);
                applyTextureAndRotation(settingsTag, blockEntity,
                                      BrushConstants.TOGGLE_FACE_KEY, BrushConstants.TOGGLE_ROTATION_KEY, true);
            }
        }
        if (settingsTag.contains(BrushConstants.BASE_BLOCK_KEY)) {
            ItemStack requiredBaseItem = ItemStack.parseOptional(registries, settingsTag.getCompound(BrushConstants.BASE_BLOCK_KEY));
            if (InventoryHelper.hasAllItems(player, requiredBaseItem)) {
                InventoryHelper.removeItems(player, requiredBaseItem);
                blockEntity.setBaseSlotItem(requiredBaseItem);
                applyTextureAndRotation(settingsTag, blockEntity,
                                      BrushConstants.BASE_FACE_KEY, BrushConstants.BASE_ROTATION_KEY, false);
            }
        }
        blockEntity.updateTextures();
        return new PasteResult(true, BrushConstants.MSG_SETTINGS_PARTIAL_APPLIED);
    }
    /** Apply all settings in an optimized manner. */
    private static void applyAllSettings(@Nonnull CompoundTag settingsTag, @Nonnull SwitchBlockEntity blockEntity,
                                        @Nonnull Player player, @Nonnull HolderLookup.Provider registries) {
        applyPowerMode(settingsTag, blockEntity);
        List<ItemStack> itemsToRemove = new ArrayList<>();
        if (settingsTag.contains(BrushConstants.TOGGLE_BLOCK_KEY)) {
            itemsToRemove.add(ItemStack.parseOptional(registries, settingsTag.getCompound(BrushConstants.TOGGLE_BLOCK_KEY)));
        }
        if (settingsTag.contains(BrushConstants.BASE_BLOCK_KEY)) {
            itemsToRemove.add(ItemStack.parseOptional(registries, settingsTag.getCompound(BrushConstants.BASE_BLOCK_KEY)));
        }
        InventoryHelper.removeItems(player, itemsToRemove.toArray(new ItemStack[0]));
        applyToggleSettings(settingsTag, blockEntity, registries);
        applyBaseSettings(settingsTag, blockEntity, registries);
    }
    private static void applyPowerMode(@Nonnull CompoundTag settingsTag, @Nonnull SwitchBlockEntity blockEntity) {
        if (settingsTag.contains(BrushConstants.POWER_MODE_KEY)) {
            try {
                SwitchBlockEntity.PowerMode powerMode =
                    SwitchBlockEntity.PowerMode.valueOf(settingsTag.getString(BrushConstants.POWER_MODE_KEY));
                powerMode = normalizeForTargetVariant(powerMode, blockEntity);
                blockEntity.setPowerMode(powerMode);
            } catch (IllegalArgumentException ignored) {
                // Invalid power mode - ignore
            }
        }
    }
    /** Normalizes a power mode for the target block's variant type. */
    private static SwitchBlockEntity.PowerMode normalizeForTargetVariant(
            @Nonnull SwitchBlockEntity.PowerMode mode, @Nonnull SwitchBlockEntity blockEntity) {
        Block block = blockEntity.getBlockState().getBlock();
        boolean isSlide = block instanceof ISwitchBlock switchBlock &&
                          switchBlock.getSwitchModelType() == SwitchModelType.SLIDE;
        if (isSlide) {
            if (mode == SwitchBlockEntity.PowerMode.NONE) {
                return SwitchBlockEntity.PowerMode.NONE_TOGGLE;
            }
        } else {
            if (mode == SwitchBlockEntity.PowerMode.NONE_TOGGLE ||
                mode == SwitchBlockEntity.PowerMode.NONE_BASE) {
                return SwitchBlockEntity.PowerMode.NONE;
            }
        }
        return mode;
    }
    private static void applyToggleSettings(@Nonnull CompoundTag settingsTag, @Nonnull SwitchBlockEntity blockEntity,
                                          @Nonnull HolderLookup.Provider registries) {
        if (settingsTag.contains(BrushConstants.TOGGLE_BLOCK_KEY)) {
            ItemStack toggleItem = ItemStack.parseOptional(registries, settingsTag.getCompound(BrushConstants.TOGGLE_BLOCK_KEY));
            blockEntity.setToggleSlotItem(toggleItem);
            applyTextureAndRotation(settingsTag, blockEntity,
                                  BrushConstants.TOGGLE_FACE_KEY, BrushConstants.TOGGLE_ROTATION_KEY, true);
        }
    }
    private static void applyBaseSettings(@Nonnull CompoundTag settingsTag, @Nonnull SwitchBlockEntity blockEntity,
                                        @Nonnull HolderLookup.Provider registries) {
        if (settingsTag.contains(BrushConstants.BASE_BLOCK_KEY)) {
            ItemStack baseItem = ItemStack.parseOptional(registries, settingsTag.getCompound(BrushConstants.BASE_BLOCK_KEY));
            blockEntity.setBaseSlotItem(baseItem);
            applyTextureAndRotation(settingsTag, blockEntity,
                                  BrushConstants.BASE_FACE_KEY, BrushConstants.BASE_ROTATION_KEY, false);
        }
    }
    private static void applyTextureAndRotation(@Nonnull CompoundTag settingsTag, @Nonnull SwitchBlockEntity blockEntity,
                                              @Nonnull String faceKey, @Nonnull String rotationKey, boolean isToggle) {
        if (settingsTag.contains(faceKey)) {
            String face = settingsTag.getString(faceKey);
            String texturePathKey = isToggle ? BrushConstants.TOGGLE_TEXTURE_PATH_KEY : BrushConstants.BASE_TEXTURE_PATH_KEY;
            String texturePath = settingsTag.contains(texturePathKey) ? settingsTag.getString(texturePathKey) : null;
            if (isToggle) {
                blockEntity.setToggleTextureVariable(face);
                if (texturePath != null && !texturePath.isEmpty()) {
                    blockEntity.setToggleTexture(texturePath);
                }
            } else {
                blockEntity.setBaseTextureVariable(face);
                if (texturePath != null && !texturePath.isEmpty()) {
                    blockEntity.setBaseTexture(texturePath);
                }
            }
        }
        if (settingsTag.contains(rotationKey)) {
            try {
                TextureRotation rotation = TextureRotation.valueOf(settingsTag.getString(rotationKey));
                if (isToggle) {
                    blockEntity.setToggleTextureRotation(rotation);
                } else {
                    blockEntity.setBaseTextureRotation(rotation);
                }
            } catch (IllegalArgumentException ignored) {
                // Invalid rotation - ignore
            }
        }
    }
}
