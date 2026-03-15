package net.justsomeswitches.item.service;

import net.justsomeswitches.block.ISwitchBlock;
import net.justsomeswitches.blockentity.SwitchBlockEntity;
import net.justsomeswitches.gui.FaceSelectionData;
import net.justsomeswitches.util.InventoryHelper;
import net.justsomeswitches.util.NBTHelper;
import net.justsomeswitches.util.TextureRotation;
import net.justsomeswitches.util.TightSwitchShapes.SwitchModelType;
import net.justsomeswitches.util.WrenchConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/** Service class for handling copy/paste operations. */
public class CopyPasteService {
    private CopyPasteService() {
        // Utility class
    }
    /** Checks if the wrench has copied settings stored. */
    public static boolean hasCopiedSettings(@Nonnull ItemStack stack) {
        NBTHelper.NBTCache cache = new NBTHelper.NBTCache(stack);
        return cache.getBoolean(WrenchConstants.HAS_COPIED_DATA_KEY);
    }
    /** Copies settings from block entity to wrench NBT. */
    @SuppressWarnings("unused") // Available for future use
    public static void copySettingsToWrench(@Nonnull ItemStack stack, @Nonnull SwitchBlockEntity blockEntity) {
        if (blockEntity.getLevel() == null) return;
        HolderLookup.Provider registries = blockEntity.getLevel().registryAccess();
        CompoundTag settingsTag = new CompoundTag();
        NBTHelper.batchNBTOperations(stack,
            tag -> {
                settingsTag.putString(WrenchConstants.TOGGLE_FACE_KEY, blockEntity.getToggleTextureVariable());
                settingsTag.putString(WrenchConstants.BASE_FACE_KEY, blockEntity.getBaseTextureVariable());
                settingsTag.putString(WrenchConstants.TOGGLE_ROTATION_KEY, blockEntity.getToggleTextureRotation().name());
                settingsTag.putString(WrenchConstants.BASE_ROTATION_KEY, blockEntity.getBaseTextureRotation().name());
                settingsTag.putString(WrenchConstants.POWER_MODE_KEY, blockEntity.getPowerMode().name());
                if (!blockEntity.getGuiToggleItem().isEmpty()) {
                    settingsTag.put(WrenchConstants.TOGGLE_BLOCK_KEY, blockEntity.getGuiToggleItem().saveOptional(registries));
                }
                if (!blockEntity.getGuiBaseItem().isEmpty()) {
                    settingsTag.put(WrenchConstants.BASE_BLOCK_KEY, blockEntity.getGuiBaseItem().saveOptional(registries));
                }
                tag.put(WrenchConstants.COPIED_SETTINGS_KEY, settingsTag);
                tag.putBoolean(WrenchConstants.HAS_COPIED_DATA_KEY, true);
            }
        );
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
                settingsTag.putString(WrenchConstants.TOGGLE_FACE_KEY, blockEntity.getToggleTextureVariable());
            }
            if (copyBaseFace) {
                settingsTag.putString(WrenchConstants.BASE_FACE_KEY, blockEntity.getBaseTextureVariable());
            }
            if (copyToggleRotation) {
                settingsTag.putString(WrenchConstants.TOGGLE_ROTATION_KEY, blockEntity.getToggleTextureRotation().name());
            }
            if (copyBaseRotation) {
                settingsTag.putString(WrenchConstants.BASE_ROTATION_KEY, blockEntity.getBaseTextureRotation().name());
            }
            if (copyIndicators) {
                settingsTag.putString(WrenchConstants.POWER_MODE_KEY, blockEntity.getPowerMode().name());
            }
            if (copyToggleBlock && !blockEntity.getGuiToggleItem().isEmpty()) {
                settingsTag.put(WrenchConstants.TOGGLE_BLOCK_KEY, blockEntity.getGuiToggleItem().saveOptional(registries));
            }
            if (copyBaseBlock && !blockEntity.getGuiBaseItem().isEmpty()) {
                settingsTag.put(WrenchConstants.BASE_BLOCK_KEY, blockEntity.getGuiBaseItem().saveOptional(registries));
            }
            tag.put(WrenchConstants.COPIED_SETTINGS_KEY, settingsTag);
            tag.putBoolean(WrenchConstants.HAS_COPIED_DATA_KEY, true);
        });
    }
    /** Optimized settings comparison. */
    public static boolean hasIdenticalSettings(@Nonnull ItemStack stack, @Nonnull SwitchBlockEntity blockEntity) {
        NBTHelper.NBTCache cache = new NBTHelper.NBTCache(stack);
        CompoundTag settingsTag = cache.getCompound(WrenchConstants.COPIED_SETTINGS_KEY);
        if (settingsTag == null || blockEntity.getLevel() == null) {
            return false;
        }
        HolderLookup.Provider registries = blockEntity.getLevel().registryAccess();
        return compareStringSetting(settingsTag, WrenchConstants.TOGGLE_FACE_KEY, blockEntity.getToggleTextureVariable()) &&
               compareStringSetting(settingsTag, WrenchConstants.BASE_FACE_KEY, blockEntity.getBaseTextureVariable()) &&
               compareStringSetting(settingsTag, WrenchConstants.TOGGLE_ROTATION_KEY, blockEntity.getToggleTextureRotation().name()) &&
               compareStringSetting(settingsTag, WrenchConstants.BASE_ROTATION_KEY, blockEntity.getBaseTextureRotation().name()) &&
               compareStringSetting(settingsTag, WrenchConstants.POWER_MODE_KEY, blockEntity.getPowerMode().name()) &&
               compareItemSetting(settingsTag, WrenchConstants.TOGGLE_BLOCK_KEY, blockEntity.getGuiToggleItem(), registries) &&
               compareItemSetting(settingsTag, WrenchConstants.BASE_BLOCK_KEY, blockEntity.getGuiBaseItem(), registries);
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
        CompoundTag settingsTag = cache.getCompound(WrenchConstants.COPIED_SETTINGS_KEY);
        if (settingsTag == null) {
            return new ArrayList<>();
        }
        HolderLookup.Provider registries = player.level().registryAccess();
        List<ItemStack> requiredItems = new ArrayList<>();
        List<String> categories = new ArrayList<>();
        if (settingsTag.contains(WrenchConstants.TOGGLE_BLOCK_KEY)) {
            requiredItems.add(ItemStack.parseOptional(registries, settingsTag.getCompound(WrenchConstants.TOGGLE_BLOCK_KEY)));
            categories.add(WrenchConstants.CATEGORY_TOGGLE);
        }
        if (settingsTag.contains(WrenchConstants.BASE_BLOCK_KEY)) {
            requiredItems.add(ItemStack.parseOptional(registries, settingsTag.getCompound(WrenchConstants.BASE_BLOCK_KEY)));
            categories.add(WrenchConstants.CATEGORY_BASE);
        }
        if (requiredItems.isEmpty()) {
            return new ArrayList<>();
        }
        ItemStack[] itemsArray = requiredItems.toArray(new ItemStack[0]);
        if (InventoryHelper.hasAllItems(player, itemsArray)) {
            return new ArrayList<>();
        }
        List<String> missingBlocks = new ArrayList<>();
        for (int i = 0; i < requiredItems.size(); i++) {
            if (!InventoryHelper.hasAllItems(player, requiredItems.get(i))) {
                String blockName = capitalizeFirst(requiredItems.get(i).getDisplayName().getString());
                missingBlocks.add("Missing " + blockName + " for " + categories.get(i));
            }
        }
        return missingBlocks;
    }
    @Nonnull
    private static String capitalizeFirst(@Nonnull String text) {
        return text.isEmpty() ? text : text.substring(0, 1).toUpperCase() + text.substring(1);
    }
    /** Clears all stored settings from the wrench. */
    public static void clearAllSettings(@Nonnull ItemStack stack) {
        NBTHelper.NBTCache cache = new NBTHelper.NBTCache(stack);
        cache.remove(WrenchConstants.COPIED_SETTINGS_KEY);
        cache.remove(WrenchConstants.HAS_COPIED_DATA_KEY);
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
    public static PasteResult applySettingsFromWrench(@Nonnull ItemStack stack, @Nonnull SwitchBlockEntity blockEntity, @Nonnull Player player) {
        NBTHelper.NBTCache cache = new NBTHelper.NBTCache(stack);
        CompoundTag settingsTag = cache.getCompound(WrenchConstants.COPIED_SETTINGS_KEY);
        if (settingsTag == null) {
            return new PasteResult(false, "No settings to paste");
        }
        List<String> missingBlocks = validateRequiredBlocks(stack, player);
        if (!missingBlocks.isEmpty()) {
            return new PasteResult(false, WrenchConstants.MSG_MISSING_BLOCKS_GUI, missingBlocks);
        }
        HolderLookup.Provider registries = player.level().registryAccess();
        applyAllSettings(settingsTag, blockEntity, player, registries);
        blockEntity.updateTextures();
        return new PasteResult(true, WrenchConstants.MSG_SETTINGS_PASTED);
    }
    /** Partial paste operation - applies only settings for blocks that are available. */
    @Nonnull
    @SuppressWarnings("resource") // Level lifecycle managed by Minecraft
    public static PasteResult applyPartialSettingsFromWrench(@Nonnull ItemStack stack, @Nonnull SwitchBlockEntity blockEntity, @Nonnull Player player) {
        NBTHelper.NBTCache cache = new NBTHelper.NBTCache(stack);
        CompoundTag settingsTag = cache.getCompound(WrenchConstants.COPIED_SETTINGS_KEY);
        if (settingsTag == null) {
            return new PasteResult(false, "No settings to paste");
        }
        HolderLookup.Provider registries = player.level().registryAccess();
        applyPowerMode(settingsTag, blockEntity);
        if (settingsTag.contains(WrenchConstants.TOGGLE_BLOCK_KEY)) {
            ItemStack requiredToggleItem = ItemStack.parseOptional(registries, settingsTag.getCompound(WrenchConstants.TOGGLE_BLOCK_KEY));
            if (InventoryHelper.hasAllItems(player, requiredToggleItem)) {
                InventoryHelper.removeItems(player, requiredToggleItem);
                blockEntity.setToggleSlotItem(requiredToggleItem);
                applyTextureAndRotation(settingsTag, blockEntity, requiredToggleItem,
                                      WrenchConstants.TOGGLE_FACE_KEY, WrenchConstants.TOGGLE_ROTATION_KEY, true);
            }
        }
        if (settingsTag.contains(WrenchConstants.BASE_BLOCK_KEY)) {
            ItemStack requiredBaseItem = ItemStack.parseOptional(registries, settingsTag.getCompound(WrenchConstants.BASE_BLOCK_KEY));
            if (InventoryHelper.hasAllItems(player, requiredBaseItem)) {
                InventoryHelper.removeItems(player, requiredBaseItem);
                blockEntity.setBaseSlotItem(requiredBaseItem);
                applyTextureAndRotation(settingsTag, blockEntity, requiredBaseItem,
                                      WrenchConstants.BASE_FACE_KEY, WrenchConstants.BASE_ROTATION_KEY, false);
            }
        }
        blockEntity.updateTextures();
        return new PasteResult(true, WrenchConstants.MSG_SETTINGS_PARTIAL_APPLIED);
    }
    /** Apply all settings in an optimized manner. */
    private static void applyAllSettings(@Nonnull CompoundTag settingsTag, @Nonnull SwitchBlockEntity blockEntity,
                                        @Nonnull Player player, @Nonnull HolderLookup.Provider registries) {
        applyPowerMode(settingsTag, blockEntity);
        List<ItemStack> itemsToRemove = new ArrayList<>();
        if (settingsTag.contains(WrenchConstants.TOGGLE_BLOCK_KEY)) {
            itemsToRemove.add(ItemStack.parseOptional(registries, settingsTag.getCompound(WrenchConstants.TOGGLE_BLOCK_KEY)));
        }
        if (settingsTag.contains(WrenchConstants.BASE_BLOCK_KEY)) {
            itemsToRemove.add(ItemStack.parseOptional(registries, settingsTag.getCompound(WrenchConstants.BASE_BLOCK_KEY)));
        }
        InventoryHelper.removeItems(player, itemsToRemove.toArray(new ItemStack[0]));
        applyToggleSettings(settingsTag, blockEntity, registries);
        applyBaseSettings(settingsTag, blockEntity, registries);
    }
    private static void applyPowerMode(@Nonnull CompoundTag settingsTag, @Nonnull SwitchBlockEntity blockEntity) {
        if (settingsTag.contains(WrenchConstants.POWER_MODE_KEY)) {
            try {
                SwitchBlockEntity.PowerMode powerMode =
                    SwitchBlockEntity.PowerMode.valueOf(settingsTag.getString(WrenchConstants.POWER_MODE_KEY));
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
        if (settingsTag.contains(WrenchConstants.TOGGLE_BLOCK_KEY)) {
            ItemStack toggleItem = ItemStack.parseOptional(registries, settingsTag.getCompound(WrenchConstants.TOGGLE_BLOCK_KEY));
            blockEntity.setToggleSlotItem(toggleItem);
            applyTextureAndRotation(settingsTag, blockEntity, toggleItem,
                                  WrenchConstants.TOGGLE_FACE_KEY, WrenchConstants.TOGGLE_ROTATION_KEY, true);
        }
    }
    private static void applyBaseSettings(@Nonnull CompoundTag settingsTag, @Nonnull SwitchBlockEntity blockEntity,
                                        @Nonnull HolderLookup.Provider registries) {
        if (settingsTag.contains(WrenchConstants.BASE_BLOCK_KEY)) {
            ItemStack baseItem = ItemStack.parseOptional(registries, settingsTag.getCompound(WrenchConstants.BASE_BLOCK_KEY));
            blockEntity.setBaseSlotItem(baseItem);
            applyTextureAndRotation(settingsTag, blockEntity, baseItem,
                                  WrenchConstants.BASE_FACE_KEY, WrenchConstants.BASE_ROTATION_KEY, false);
        }
    }
    private static void applyTextureAndRotation(@Nonnull CompoundTag settingsTag, @Nonnull SwitchBlockEntity blockEntity,
                                              @Nonnull ItemStack item, @Nonnull String faceKey, @Nonnull String rotationKey, boolean isToggle) {
        if (settingsTag.contains(faceKey)) {
            String face = settingsTag.getString(faceKey);
            String texturePath = FaceSelectionData.getTextureForVariable(item, face);
            if (isToggle) {
                blockEntity.setToggleTextureVariable(face);
                if (texturePath != null) {
                    blockEntity.setToggleTexture(texturePath);
                }
            } else {
                blockEntity.setBaseTextureVariable(face);
                if (texturePath != null) {
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
