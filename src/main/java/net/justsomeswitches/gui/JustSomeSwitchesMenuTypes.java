package net.justsomeswitches.gui;

import net.justsomeswitches.JustSomeSwitchesMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Registration class for menu types in Just Some Switches mod
 * ---
 * Phase 3C Fix: Resolved ambiguous constructor reference by explicitly casting null to BlockPos
 */
public class JustSomeSwitchesMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, JustSomeSwitchesMod.MODID);

    /**
     * Switch Texture Menu Type - Enhanced for Phase 3B with block position support
     * ---
     * Now supports passing block position data through extraData for BlockEntity integration
     */
    public static final DeferredHolder<MenuType<?>, MenuType<SwitchesTextureMenu>> SWITCH_TEXTURE_MENU =
            MENU_TYPES.register("switch_texture_menu", () ->
                    IMenuTypeExtension.create((containerId, playerInventory, extraData) -> {
                        // Read block position from network data
                        var blockPos = extraData.readBlockPos();
                        return new SwitchesTextureMenu(containerId, playerInventory, blockPos);
                    })
            );
    
    /**
     * Wrench Copy Menu Type - For copy texture settings GUI with selective checkboxes
     * ---
     * Supports block position data for accessing source block entity settings
     */
    public static final DeferredHolder<MenuType<?>, MenuType<WrenchCopyMenu>> WRENCH_COPY =
            MENU_TYPES.register("wrench_copy_menu", () ->
                    IMenuTypeExtension.create((containerId, playerInventory, extraData) -> {
                        // Read block position from network data
                        var blockPos = extraData.readBlockPos();
                        return new WrenchCopyMenu(containerId, playerInventory, blockPos);
                    })
            );
    
    /**
     * Wrench Overwrite Menu Type - For paste overwrite confirmation dialog
     * ---
     * Simple confirmation dialog with block position for context
     */
    public static final DeferredHolder<MenuType<?>, MenuType<WrenchOverwriteMenu>> WRENCH_OVERWRITE =
            MENU_TYPES.register("wrench_overwrite_menu", () ->
                    IMenuTypeExtension.create((containerId, playerInventory, extraData) -> {
                        // Read block position from network data
                        var blockPos = extraData.readBlockPos();
                        return new WrenchOverwriteMenu(containerId, playerInventory, blockPos);
                    })
            );
    
    /**
     * Wrench Copy Overwrite Menu Type - For copy overwrite confirmation dialog
     * ---
     * Simple confirmation dialog with block position for context when overwriting stored copy settings
     */
    public static final DeferredHolder<MenuType<?>, MenuType<WrenchCopyOverwriteMenu>> WRENCH_COPY_OVERWRITE =
            MENU_TYPES.register("wrench_copy_overwrite_menu", () ->
                    IMenuTypeExtension.create((containerId, playerInventory, extraData) -> {
                        // Read block position from network data
                        var blockPos = extraData.readBlockPos();
                        return new WrenchCopyOverwriteMenu(containerId, playerInventory, blockPos);
                    })
            );
    
    /**
     * Wrench Missing Block Menu Type - For missing block notification dialog
     * ---
     * Shows when player is missing blocks needed for paste operation
     */
    public static final DeferredHolder<MenuType<?>, MenuType<WrenchMissingBlockMenu>> WRENCH_MISSING_BLOCK =
            MENU_TYPES.register("wrench_missing_block_menu", () ->
                    IMenuTypeExtension.create(WrenchMissingBlockMenu::new)
            );
}