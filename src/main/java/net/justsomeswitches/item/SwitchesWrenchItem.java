package net.justsomeswitches.item;

import net.justsomeswitches.gui.SwitchesTextureMenu;
import net.justsomeswitches.block.ISwitchBlock;
import net.justsomeswitches.blockentity.SwitchBlockEntity;
import net.justsomeswitches.item.service.CopyPasteService;
import net.justsomeswitches.network.NetworkHandler;
import net.justsomeswitches.network.WrenchActionPayload;
import net.justsomeswitches.util.NBTHelper;
import net.justsomeswitches.util.WrenchConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.ChatFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/** Optimized switches wrench with copy/paste functionality. */
public class SwitchesWrenchItem extends Item {

    public SwitchesWrenchItem(@Nonnull Properties properties) {
        super(properties);
    }

    @Override
    @Nonnull
    public InteractionResult useOn(@Nonnull UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        Block block = context.getLevel().getBlockState(context.getClickedPos()).getBlock();
        if (!isSwitchBlock(block)) {
            return InteractionResult.FAIL;
        }

        KeyAction keyAction = detectKeyAction();
        return switch (keyAction) {
            case COPY -> handleCopyOperation(context);
            case PASTE -> handlePasteOperation(context);
            case NONE -> handleStandardGUI(context);
        };
    }
    
    /** Handle right-clicking air with shift to clear stored settings. */
    @Override
    @Nonnull
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(@Nonnull net.minecraft.world.level.Level level, @Nonnull Player player, @Nonnull net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (player.isShiftKeyDown() && CopyPasteService.hasCopiedSettings(stack)) {
            CopyPasteService.clearAllSettings(stack);
            showActionBarMessage(player, WrenchConstants.MSG_SETTINGS_CLEARED, ActionBarMessageType.SUCCESS);
            return net.minecraft.world.InteractionResultHolder.success(stack);
        }
        
        return net.minecraft.world.InteractionResultHolder.pass(stack);
    }

    private enum KeyAction {
        COPY, PASTE, NONE
    }
    
    private KeyAction detectKeyAction() {
        if (!net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
            return KeyAction.NONE;
        }
        
        return detectKeyActionClient();
    }
    
    @OnlyIn(Dist.CLIENT)
    private KeyAction detectKeyActionClient() {
        long windowHandle = Minecraft.getInstance().getWindow().getWindow();
        
        boolean altPressed = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS ||
                            GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS;
        boolean cPressed = GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_C) == GLFW.GLFW_PRESS;
        
        if (altPressed && cPressed) {
            return KeyAction.COPY;
        }

        if (altPressed) {
            return KeyAction.PASTE;
        }
        
        return KeyAction.NONE;
    }
    
    private boolean isSwitchBlock(@Nonnull Block block) {
        return block instanceof ISwitchBlock;
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
            showActionBarMessage(player, WrenchConstants.MSG_NO_SETTINGS_TO_COPY, ActionBarMessageType.ERROR);
            return InteractionResult.SUCCESS;
        }
        
        if (CopyPasteService.hasCopiedSettings(stack)) {
            if (CopyPasteService.hasIdenticalSettings(stack, blockEntity)) {
                showActionBarMessage(player, WrenchConstants.MSG_SETTINGS_ALREADY_COPIED, ActionBarMessageType.INFO);
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
            showActionBarMessage(player, WrenchConstants.MSG_SETTINGS_NOT_COPIED, ActionBarMessageType.INFO);
            return InteractionResult.SUCCESS;
        }

        Level level = context.getLevel();
        BlockPos blockPos = context.getClickedPos();
        if (!(level.getBlockEntity(blockPos) instanceof SwitchBlockEntity)) {
            return InteractionResult.FAIL;
        }

        if (level.isClientSide) {
            NetworkHandler.sendWrenchAction(blockPos,
                WrenchActionPayload.WrenchAction.PASTE,
                player.getUsedItemHand());
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
                return new SwitchesTextureMenu(containerId, playerInventory, blockPos);
            }
        };

        net.minecraftforge.network.NetworkHooks.openScreen(player, menuProvider, buf -> buf.writeBlockPos(blockPos));
    }
    
    private void openCopyTextureGUI(@Nonnull ServerPlayer player, @Nonnull BlockPos blockPos) {
        MenuProvider menuProvider = new MenuProvider() {
            @Override
            @Nonnull
            public Component getDisplayName() {
                return Component.literal(WrenchConstants.GUI_COPY_TEXTURE_TITLE);
            }

            @Override
            @Nonnull
            public AbstractContainerMenu createMenu(int containerId, @Nonnull Inventory playerInventory, @Nonnull Player player) {
                return new net.justsomeswitches.gui.WrenchCopyMenu(containerId, playerInventory, blockPos);
            }
        };

        net.minecraftforge.network.NetworkHooks.openScreen(player, menuProvider, buf -> buf.writeBlockPos(blockPos));
    }
    
    private void openCopyOverwriteGUI(@Nonnull ServerPlayer player, @Nonnull BlockPos blockPos) {
        MenuProvider menuProvider = new MenuProvider() {
            @Override
            @Nonnull
            public Component getDisplayName() {
                return Component.literal(WrenchConstants.GUI_DIFFERENT_SETTINGS_FOUND);
            }

            @Override
            @Nonnull
            public AbstractContainerMenu createMenu(int containerId, @Nonnull Inventory playerInventory, @Nonnull Player player) {
                return new net.justsomeswitches.gui.WrenchCopyOverwriteMenu(containerId, playerInventory, blockPos);
            }
        };

        net.minecraftforge.network.NetworkHooks.openScreen(player, menuProvider, buf -> buf.writeBlockPos(blockPos));
    }
    
    /** Server-side paste operation - delegated to service. */
    @SuppressWarnings("unused") // Called from network handlers
    public CopyPasteService.PasteResult applySettingsFromWrenchServer(ItemStack stack, SwitchBlockEntity blockEntity, Player player) {
        return CopyPasteService.applySettingsFromWrench(stack, blockEntity, player);
    }
    
    /** Server-side partial paste operation - delegated to service. */
    @SuppressWarnings("unused") // Called from network handlers
    public CopyPasteService.PasteResult applyPartialSettingsFromWrenchServer(ItemStack stack, SwitchBlockEntity blockEntity, Player player) {
        return CopyPasteService.applyPartialSettingsFromWrench(stack, blockEntity, player);
    }
    
    /** Server-side copy operation - delegated to service. */
    @SuppressWarnings("unused") // Called from network handlers
    public void copySelectedSettingsToWrench(ItemStack stack, SwitchBlockEntity blockEntity,
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
    public CopyPasteService.PasteResult checkInventoryForPasteServer(ItemStack stack, Player player) {
        List<String> missingBlocks = CopyPasteService.validateRequiredBlocks(stack, player);
        if (!missingBlocks.isEmpty()) {
            return new CopyPasteService.PasteResult(false, WrenchConstants.MSG_MISSING_BLOCKS_GUI, missingBlocks);
        }
        return new CopyPasteService.PasteResult(true, "All blocks available");
    }
    
    @SuppressWarnings("unused") // Called from network handlers
    public void clearAllSettingsServer(ItemStack stack) {
        CopyPasteService.clearAllSettings(stack);
    }
    
    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level level, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        
        if (CopyPasteService.hasCopiedSettings(stack)) {
            addStoredSettingsTooltip(stack, tooltip);
        }
        
        addControlsTooltip(tooltip);
    }
    
    private void addStoredSettingsTooltip(@Nonnull ItemStack stack, @Nonnull List<Component> tooltip) {
        tooltip.add(Component.literal("⚙ Settings Stored").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.empty());
        
        NBTHelper.NBTCache cache = new NBTHelper.NBTCache(stack);
        CompoundTag settingsTag = cache.getCompound(WrenchConstants.COPIED_SETTINGS_KEY);
        if (settingsTag != null) {
            addSettingIfPresent(tooltip, settingsTag, WrenchConstants.TOGGLE_BLOCK_KEY, "Toggle Block: ", true);
            addSettingIfPresent(tooltip, settingsTag, WrenchConstants.TOGGLE_FACE_KEY, "Toggle Face: ", false);
            addSettingIfPresent(tooltip, settingsTag, WrenchConstants.BASE_BLOCK_KEY, "Base Block: ", true);
            addSettingIfPresent(tooltip, settingsTag, WrenchConstants.BASE_FACE_KEY, "Base Face: ", false);
            addSettingIfPresent(tooltip, settingsTag, WrenchConstants.TOGGLE_ROTATION_KEY, "Toggle Rotation: ", false);
            addSettingIfPresent(tooltip, settingsTag, WrenchConstants.BASE_ROTATION_KEY, "Base Rotation: ", false);
            addSettingIfPresent(tooltip, settingsTag, WrenchConstants.POWER_MODE_KEY, "Indicators: ", false);
            
            if (tooltip.size() > WrenchConstants.TOOLTIP_MAX_LINES) {
                tooltip.add(Component.literal("...").withStyle(ChatFormatting.GRAY));
            }
        }
        
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Note: Only applies to placed Switches blocks")
                   .withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY));
    }
    
    private void addSettingIfPresent(@Nonnull List<Component> tooltip, @Nonnull CompoundTag settingsTag, 
                                   @Nonnull String key, @Nonnull String prefix, boolean isItem) {
        if (settingsTag.contains(key)) {
            String value;
            if (isItem) {
                // Remove brackets around item names for cleaner display
                value = ItemStack.of(settingsTag.getCompound(key)).getDisplayName().getString();
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
        if (key.equals(WrenchConstants.TOGGLE_ROTATION_KEY) || key.equals(WrenchConstants.BASE_ROTATION_KEY)) {
            try {
                net.justsomeswitches.util.TextureRotation rotation = 
                    net.justsomeswitches.util.TextureRotation.valueOf(rawValue);
                return rotation.getDegrees() + "°";
            } catch (IllegalArgumentException e) {
                return rawValue;
            }
        }
        
        // Format power mode values with proper capitalization
        if (key.equals(WrenchConstants.POWER_MODE_KEY)) {
            return switch (rawValue.toUpperCase()) {
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
    
    private void addControlsTooltip(@Nonnull List<Component> tooltip) {
        tooltip.add(Component.literal("Shift + Right-Click: Open Texture Customization GUI").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Shift + ALT + C + Right-Click: Copy Texture Settings").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Shift + ALT + Right-Click: Paste Texture Settings").withStyle(ChatFormatting.DARK_GRAY));
    }
}
