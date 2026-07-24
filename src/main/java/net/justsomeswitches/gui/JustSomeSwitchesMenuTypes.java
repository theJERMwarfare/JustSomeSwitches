package net.justsomeswitches.gui;

import net.justsomeswitches.JustSomeSwitchesMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Registration class for menu types. */
public class JustSomeSwitchesMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, JustSomeSwitchesMod.MODID);

    /** Switch texture menu type with block position support. */
    public static final DeferredHolder<MenuType<?>, MenuType<CustomizableTextureMenu>> SWITCH_TEXTURE_MENU =
            MENU_TYPES.register("switch_texture_menu", () ->
                    IMenuTypeExtension.create((containerId, playerInventory, extraData) -> {
                        var blockPos = extraData.readBlockPos();
                        return new CustomizableTextureMenu(containerId, playerInventory, blockPos);
                    })
            );
    
    /** Brush copy menu type for selective texture settings. */
    public static final DeferredHolder<MenuType<?>, MenuType<BrushCopyMenu>> BRUSH_COPY =
            MENU_TYPES.register("wrench_copy_menu", () ->
                    IMenuTypeExtension.create((containerId, playerInventory, extraData) -> {
                        var blockPos = extraData.readBlockPos();
                        return new BrushCopyMenu(containerId, playerInventory, blockPos);
                    })
            );
    
    /** Brush overwrite menu type for paste confirmation. */
    public static final DeferredHolder<MenuType<?>, MenuType<BrushOverwriteMenu>> BRUSH_OVERWRITE =
            MENU_TYPES.register("wrench_overwrite_menu", () ->
                    IMenuTypeExtension.create((containerId, playerInventory, extraData) -> {
                        var blockPos = extraData.readBlockPos();
                        return new BrushOverwriteMenu(containerId, playerInventory, blockPos);
                    })
            );
    
    /** Brush copy overwrite menu type for copy confirmation. */
    public static final DeferredHolder<MenuType<?>, MenuType<BrushCopyOverwriteMenu>> BRUSH_COPY_OVERWRITE =
            MENU_TYPES.register("wrench_copy_overwrite_menu", () ->
                    IMenuTypeExtension.create((containerId, playerInventory, extraData) -> {
                        var blockPos = extraData.readBlockPos();
                        return new BrushCopyOverwriteMenu(containerId, playerInventory, blockPos);
                    })
            );
    
    /** Brush missing block menu type for notification dialog. */
    public static final DeferredHolder<MenuType<?>, MenuType<BrushMissingBlockMenu>> BRUSH_MISSING_BLOCK =
            MENU_TYPES.register("wrench_missing_block_menu", () ->
                    IMenuTypeExtension.create(BrushMissingBlockMenu::new)
            );
}