package net.justsomeswitches.item;

import net.justsomeswitches.gui.CustomizableTextureMenu;
import net.justsomeswitches.block.ISwitchBlock;
import net.justsomeswitches.blockentity.SwitchBlockEntity;
import net.justsomeswitches.item.service.CopyPasteService;
import net.justsomeswitches.network.NetworkHandler;
import net.justsomeswitches.network.BrushActionPayload;
import net.justsomeswitches.util.NBTHelper;
import net.justsomeswitches.util.BrushConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.ChatFormatting;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;

/** Switch Texture Brush with copy/paste functionality and dynamic active/inactive texture. */
public class SwitchTextureBrushItem extends Item {

    public SwitchTextureBrushItem(@Nonnull Properties properties) {
        super(properties);
    }

    @Override
    @Nonnull
    public InteractionResult useOn(@Nonnull UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        // Main hand only, matching other mods' tools. Declining openly beats appearing to work.
        if (context.getHand() != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        Block block = context.getLevel().getBlockState(context.getClickedPos()).getBlock();
        if (!isSwitchBlock(block)) {
            return InteractionResult.FAIL;
        }

        return switch (getMode(context.getItemInHand())) {
            case COPY -> handleCopyOperation(context);
            case PASTE -> handlePasteOperation(context);
            case CUSTOMIZE -> handleStandardGUI(context);
        };
    }
    
    /** Handle right-clicking air with shift to clear stored settings. */
    @Override
    @Nonnull
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(@Nonnull net.minecraft.world.level.Level level, @Nonnull Player player, @Nonnull net.minecraft.world.InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return net.minecraft.world.InteractionResultHolder.pass(player.getItemInHand(hand));
        }
        ItemStack stack = player.getItemInHand(hand);
        
        if (player.isShiftKeyDown() && CopyPasteService.hasCopiedSettings(stack)) {
            CopyPasteService.clearAllSettings(stack);
            showActionBarMessage(player, BrushConstants.MSG_SETTINGS_CLEARED, ActionBarMessageType.SUCCESS);
            return net.minecraft.world.InteractionResultHolder.success(stack);
        }
        
        return net.minecraft.world.InteractionResultHolder.pass(stack);
    }

    /**
     * Current mode, read from the stack's custom data. Stored there rather than in a dedicated
     * component so the server already has it when the interaction arrives, with no packet involved.
     * An unset or unrecognised value reads as CUSTOMIZE, so pre-existing brushes keep working.
     */
    @Nonnull
    public static BrushMode getMode(@Nonnull ItemStack stack) {
        String stored = new NBTHelper.NBTCache(stack).getString(BrushConstants.BRUSH_MODE_KEY, "");
        return BrushMode.fromName(stored);
    }
    /** Writes the mode to the stack. CUSTOMIZE removes the key so a default brush stays clean. */
    public static void setMode(@Nonnull ItemStack stack, @Nonnull BrushMode mode) {
        if (mode == BrushMode.CUSTOMIZE) {
            new NBTHelper.NBTCache(stack).remove(BrushConstants.BRUSH_MODE_KEY);
            return;
        }
        NBTHelper.batchNBTOperations(stack, tag -> tag.putString(BrushConstants.BRUSH_MODE_KEY, mode.name()));
    }
    
    private boolean isSwitchBlock(@Nonnull Block block) {
        return block instanceof ISwitchBlock;
    }

    /** Returns the brush ItemStack held in the player's main or off hand, or null if neither holds one. */
    @Nullable
    public static ItemStack findBrushInHands(@Nonnull Player player) {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof SwitchTextureBrushItem) {
            return mainHand;
        }
        ItemStack offHand = player.getOffhandItem();
        if (offHand.getItem() instanceof SwitchTextureBrushItem) {
            return offHand;
        }
        return null;
    }
    
    private InteractionResult handleStandardGUI(@Nonnull UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.FAIL;
        return openGUIOnServer(context.getLevel(), player, context.getClickedPos(),
                              this::openTextureCustomizationGUI);
    }
    
    private InteractionResult handleCopyOperation(@Nonnull UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.FAIL;
        Level level = context.getLevel();
        BlockPos blockPos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        
        if (!(level.getBlockEntity(blockPos) instanceof SwitchBlockEntity blockEntity)) {
            return InteractionResult.FAIL;
        }
        
        if (!blockEntity.hasCustomTextures()) {
            showActionBarMessage(player, BrushConstants.MSG_NO_SETTINGS_TO_COPY, ActionBarMessageType.ERROR);
            return InteractionResult.SUCCESS;
        }
        
        if (CopyPasteService.hasCopiedSettings(stack)) {
            if (CopyPasteService.hasIdenticalSettings(stack, blockEntity)) {
                showActionBarMessage(player, BrushConstants.MSG_SETTINGS_ALREADY_COPIED, ActionBarMessageType.INFO);
                return InteractionResult.SUCCESS;
            }
            
            return openGUIOnServer(level, player, blockPos, this::openCopyOverwriteGUI);
        }
        
        return openGUIOnServer(level, player, blockPos, this::openCopyTextureGUI);
    }
    
    private InteractionResult handlePasteOperation(@Nonnull UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.FAIL;
        ItemStack stack = context.getItemInHand();
        if (!CopyPasteService.hasCopiedSettings(stack)) {
            showActionBarMessage(player, BrushConstants.MSG_SETTINGS_NOT_COPIED, ActionBarMessageType.INFO);
            return InteractionResult.SUCCESS;
        }

        Level level = context.getLevel();
        BlockPos blockPos = context.getClickedPos();
        if (!(level.getBlockEntity(blockPos) instanceof SwitchBlockEntity)) {
            return InteractionResult.FAIL;
        }

        if (level.isClientSide) {
            NetworkHandler.sendBrushAction(blockPos,
                BrushActionPayload.BrushAction.PASTE,
                InteractionHand.MAIN_HAND);
        }
        
        return InteractionResult.SUCCESS;
    }

    private InteractionResult openGUIOnServer(@Nonnull Level level, @Nonnull Player player, 
                                             @Nonnull BlockPos blockPos, @Nonnull GUIOpener guiOpener) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            guiOpener.openGUI(serverPlayer, blockPos);
            return InteractionResult.SUCCESS;
        }
        return level.isClientSide ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }
    
    @FunctionalInterface
    private interface GUIOpener {
        void openGUI(@Nonnull ServerPlayer player, @Nonnull BlockPos blockPos);
    }

    @SuppressWarnings("resource") // Level lifecycle managed by Minecraft, not by us
    private void showActionBarMessage(@Nonnull Player player, @Nonnull String message, @Nonnull ActionBarMessageType type) {
        if (player.level().isClientSide) {
            net.minecraft.network.chat.Component styledMessage = formatActionBarMessage(message, type);
            player.displayClientMessage(styledMessage, true);
        }
    }
    
    private net.minecraft.network.chat.Component formatActionBarMessage(String message, ActionBarMessageType type) {
        return switch (type) {
            case SUCCESS -> Component.literal(message).withStyle(net.minecraft.ChatFormatting.GREEN);
            case ERROR -> Component.literal(message).withStyle(net.minecraft.ChatFormatting.RED);
            case INFO -> Component.literal(message).withStyle(net.minecraft.ChatFormatting.BLUE);
        };
    }
    
    private enum ActionBarMessageType {
        SUCCESS, ERROR, INFO
    }

    private void openTextureCustomizationGUI(@Nonnull ServerPlayer player, @Nonnull BlockPos blockPos) {
        MenuProvider menuProvider = new MenuProvider() {
            @Override
            @Nonnull
            public Component getDisplayName() {
                return Component.translatable("gui.justsomeswitches.switch_texture.title");
            }

            @Override
            @Nonnull
            public AbstractContainerMenu createMenu(int containerId, @Nonnull Inventory playerInventory, @Nonnull Player player) {
                return new CustomizableTextureMenu(containerId, playerInventory, blockPos);
            }
        };

        player.openMenu(menuProvider, buf -> buf.writeBlockPos(blockPos));
    }
    
    private void openCopyTextureGUI(@Nonnull ServerPlayer player, @Nonnull BlockPos blockPos) {
        MenuProvider menuProvider = new MenuProvider() {
            @Override
            @Nonnull
            public Component getDisplayName() {
                return Component.literal(BrushConstants.GUI_COPY_TEXTURE_TITLE);
            }

            @Override
            @Nonnull
            public AbstractContainerMenu createMenu(int containerId, @Nonnull Inventory playerInventory, @Nonnull Player player) {
                return new net.justsomeswitches.gui.BrushCopyMenu(containerId, playerInventory, blockPos);
            }
        };

        player.openMenu(menuProvider, buf -> buf.writeBlockPos(blockPos));
    }
    
    private void openCopyOverwriteGUI(@Nonnull ServerPlayer player, @Nonnull BlockPos blockPos) {
        MenuProvider menuProvider = new MenuProvider() {
            @Override
            @Nonnull
            public Component getDisplayName() {
                return Component.literal(BrushConstants.GUI_DIFFERENT_SETTINGS_FOUND);
            }

            @Override
            @Nonnull
            public AbstractContainerMenu createMenu(int containerId, @Nonnull Inventory playerInventory, @Nonnull Player player) {
                return new net.justsomeswitches.gui.BrushCopyOverwriteMenu(containerId, playerInventory, blockPos);
            }
        };

        player.openMenu(menuProvider, buf -> buf.writeBlockPos(blockPos));
    }
    
    /** Server-side paste operation - delegated to service. */
    @SuppressWarnings("unused") // Called from network handlers
    public CopyPasteService.PasteResult applySettingsFromBrushServer(ItemStack stack, SwitchBlockEntity blockEntity, Player player) {
        return CopyPasteService.applySettingsFromBrush(stack, blockEntity, player);
    }
    
    /** Server-side partial paste operation - delegated to service. */
    @SuppressWarnings("unused") // Called from network handlers
    public CopyPasteService.PasteResult applyPartialSettingsFromBrushServer(ItemStack stack, SwitchBlockEntity blockEntity, Player player) {
        return CopyPasteService.applyPartialSettingsFromBrush(stack, blockEntity, player);
    }
    
    /** Server-side copy operation - delegated to service. */
    @SuppressWarnings("unused") // Called from network handlers
    public void copySelectedSettingsToBrush(ItemStack stack, SwitchBlockEntity blockEntity,
                                            boolean copyToggleBlock, boolean copyToggleFace, boolean copyToggleRotation,
                                            boolean copyIndicators, boolean copyBaseBlock, boolean copyBaseFace,
                                            boolean copyBaseRotation) {
        CopyPasteService.copySelectedSettings(stack, blockEntity, copyToggleBlock, copyToggleFace, 
                                            copyToggleRotation, copyIndicators, copyBaseBlock, 
                                            copyBaseFace, copyBaseRotation);
    }
    
    @SuppressWarnings("unused") // Called from network handlers
    public boolean hasCopiedSettingsServer(ItemStack stack) {
        return CopyPasteService.hasCopiedSettings(stack);
    }
    
    @SuppressWarnings("unused") // Called from network handlers
    public boolean hasIdenticalSettingsServer(ItemStack stack, SwitchBlockEntity blockEntity) {
        return CopyPasteService.hasIdenticalSettings(stack, blockEntity);
    }
    
    @SuppressWarnings("unused") // Called from network handlers
    public CopyPasteService.PasteResult checkInventoryForPasteServer(ItemStack stack, Player player,
                                                                     List<ItemStack> alsoAvailable) {
        List<String> missingBlocks = CopyPasteService.validateRequiredBlocks(stack, player, alsoAvailable);
        if (!missingBlocks.isEmpty()) {
            return new CopyPasteService.PasteResult(false, BrushConstants.MSG_MISSING_BLOCKS_GUI, missingBlocks);
        }
        return new CopyPasteService.PasteResult(true, "All blocks available");
    }
    
    @SuppressWarnings("unused") // Called from network handlers
    public void clearAllSettingsServer(ItemStack stack) {
        CopyPasteService.clearAllSettings(stack);
    }
    
    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nonnull Item.TooltipContext context, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        BrushMode mode = getMode(stack);
        appendModeToName(tooltip, mode);
        boolean hasSettings = CopyPasteService.hasCopiedSettings(stack);
        if (hasSettings) {
            tooltip.add(Component.empty());
            addStoredSettingsTooltip(stack, tooltip, context);
        }
        tooltip.add(Component.empty());
        addControlsTooltip(tooltip, mode, hasSettings);
    }
    
    /**
     * Puts the mode on the item name line rather than its own, to keep the tooltip short. Vanilla adds
     * the name at index 0 immediately before calling this (ItemStack:787), so index 0 is the name here.
     */
    private void appendModeToName(@Nonnull List<Component> tooltip, @Nonnull BrushMode mode) {
        Component modeName = mode.getDisplayName().withStyle(mode.getColor());
        if (tooltip.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.justsomeswitches.brush.mode", modeName));
            return;
        }
        tooltip.set(0, Component.translatable("tooltip.justsomeswitches.brush.name_with_mode",
                   tooltip.get(0), modeName).withStyle(ChatFormatting.GRAY));
    }

    /**
     * Face and rotation share a line so each category's settings stay together and the tooltip stays
     * short. Either can be stored without the other, since the copy screen selects them separately.
     */
    private void addFaceAndRotation(@Nonnull List<Component> tooltip, @Nonnull CompoundTag settingsTag,
                                    @Nonnull String faceKey, @Nonnull String faceTranslationKey,
                                    @Nonnull String rotationKey, @Nonnull String rotationTranslationKey) {
        MutableComponent line = null;
        if (settingsTag.contains(faceKey)) {
            line = Component.translatable(faceTranslationKey,
                   formatSettingValue(faceKey, settingsTag.getString(faceKey)));
        }
        if (settingsTag.contains(rotationKey)) {
            MutableComponent rotation = Component.translatable(rotationTranslationKey,
                   formatSettingValue(rotationKey, settingsTag.getString(rotationKey)));
            line = line == null ? rotation : line.append(", ").append(rotation);
        }
        if (line != null) {
            tooltip.add(line.withStyle(ChatFormatting.GRAY));
        }
    }

    private void addStoredSettingsTooltip(@Nonnull ItemStack stack, @Nonnull List<Component> tooltip, @Nonnull Item.TooltipContext context) {
        tooltip.add(Component.literal("⚙ Settings Stored").withStyle(ChatFormatting.YELLOW));
        NBTHelper.NBTCache cache = new NBTHelper.NBTCache(stack);
        CompoundTag settingsTag = cache.getCompound(BrushConstants.COPIED_SETTINGS_KEY);
        if (settingsTag != null) {
            int beforeSettings = tooltip.size();
            addSettingIfPresent(tooltip, settingsTag, BrushConstants.TOGGLE_BLOCK_KEY, "Toggle Block: ", true, context);
            addFaceAndRotation(tooltip, settingsTag,
                BrushConstants.TOGGLE_FACE_KEY, "tooltip.justsomeswitches.brush.setting.toggle_face",
                BrushConstants.TOGGLE_ROTATION_KEY, "tooltip.justsomeswitches.brush.setting.toggle_rotation");
            addSettingIfPresent(tooltip, settingsTag, BrushConstants.BASE_BLOCK_KEY, "Base Block: ", true, context);
            addFaceAndRotation(tooltip, settingsTag,
                BrushConstants.BASE_FACE_KEY, "tooltip.justsomeswitches.brush.setting.base_face",
                BrushConstants.BASE_ROTATION_KEY, "tooltip.justsomeswitches.brush.setting.base_rotation");
            addSettingIfPresent(tooltip, settingsTag, BrushConstants.POWER_MODE_KEY, "Indicators: ", false, context);
            
            // Count only the settings, not the whole tooltip, or this claims a truncation that never happened.
            if (tooltip.size() - beforeSettings > BrushConstants.TOOLTIP_MAX_LINES) {
                tooltip.add(Component.literal("...").withStyle(ChatFormatting.GRAY));
            }
        }
        
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Note: Only applies to placed Customizable Switch blocks")
                   .withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY));
    }
    
    private void addSettingIfPresent(@Nonnull List<Component> tooltip, @Nonnull CompoundTag settingsTag,
                                   @Nonnull String key, @Nonnull String prefix, boolean isItem, @Nonnull Item.TooltipContext context) {
        if (settingsTag.contains(key)) {
            String value;
            HolderLookup.Provider registries = context.registries();
            if (isItem && registries != null) {
                // getHoverName, not getDisplayName: the latter wraps the name in square brackets.
                value = ItemStack.parseOptional(registries, settingsTag.getCompound(key)).getHoverName().getString();
            } else {
                String rawValue = settingsTag.getString(key);
                value = formatSettingValue(key, rawValue);
            }
            tooltip.add(Component.literal(prefix + value).withStyle(ChatFormatting.GRAY));
        }
    }
    
    /** Formats setting values for better tooltip display. */
    @Nonnull
    private String formatSettingValue(@Nonnull String key, @Nonnull String rawValue) {
        // Format rotation values to show degrees
        if (key.equals(BrushConstants.TOGGLE_ROTATION_KEY) || key.equals(BrushConstants.BASE_ROTATION_KEY)) {
            try {
                net.justsomeswitches.util.TextureRotation rotation = 
                    net.justsomeswitches.util.TextureRotation.valueOf(rawValue);
                return rotation.getDegrees() + "°";
            } catch (IllegalArgumentException e) {
                return rawValue;
            }
        }
        
        // Format power mode values with proper capitalization
        if (key.equals(BrushConstants.POWER_MODE_KEY)) {
            return switch (rawValue.toUpperCase(Locale.ROOT)) {
                case "DEFAULT" -> "Default";
                case "ALT" -> "Alt";
                case "NONE" -> "None";
                default -> capitalizeFirst(rawValue.toLowerCase());
            };
        }
        
        return rawValue;
    }
    
    @Nonnull
    private String capitalizeFirst(@Nonnull String text) {
        return text.isEmpty() ? text : text.substring(0, 1).toUpperCase() + text.substring(1);
    }
    
    /**
     * Controls, described for the mode the brush is actually in. Component.keybind resolves to the
     * player's own sneak key at render time, so a rebound key shows correctly. It is a common-side
     * API, unlike KeyMapping, so this class stays safe to load on a dedicated server.
     */
    private void addControlsTooltip(@Nonnull List<Component> tooltip, @Nonnull BrushMode mode, boolean hasSettings) {
        Component sneakKey = Component.keybind("key.sneak");
        tooltip.add(mode.getActionDescription(sneakKey).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.justsomeswitches.brush.change_mode", sneakKey)
                   .withStyle(ChatFormatting.DARK_GRAY));
        if (hasSettings) {
            tooltip.add(Component.translatable("tooltip.justsomeswitches.brush.clear", sneakKey)
                       .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
