package net.justsomeswitches.gui;

import net.justsomeswitches.JustSomeSwitchesMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** Registration class for menu types. */
public class JustSomeSwitchesMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, JustSomeSwitchesMod.MODID);

    /** Switch texture menu type with block position support. */
    public static final RegistryObject<MenuType<CustomizableTextureMenu>> SWITCH_TEXTURE_MENU =
            MENU_TYPES.register("switch_texture_menu", () ->
                    IForgeMenuType.create((containerId, playerInventory, extraData) -> {
                        var blockPos = extraData.readBlockPos();
                        return new CustomizableTextureMenu(containerId, playerInventory, blockPos);
                    })
            );
    
    /** Brush copy menu type for selective texture settings. */
    public static final RegistryObject<MenuType<BrushCopyMenu>> BRUSH_COPY =
            MENU_TYPES.register("wrench_copy_menu", () ->
                    IForgeMenuType.create((containerId, playerInventory, extraData) -> {
                        var blockPos = extraData.readBlockPos();
                        return new BrushCopyMenu(containerId, playerInventory, blockPos);
                    })
            );
    
    /** Brush overwrite menu type for paste confirmation. */
    public static final RegistryObject<MenuType<BrushOverwriteMenu>> BRUSH_OVERWRITE =
            MENU_TYPES.register("wrench_overwrite_menu", () ->
                    IForgeMenuType.create((containerId, playerInventory, extraData) -> {
                        var blockPos = extraData.readBlockPos();
                        return new BrushOverwriteMenu(containerId, playerInventory, blockPos);
                    })
            );
    
    /** Brush copy overwrite menu type for copy confirmation. */
    public static final RegistryObject<MenuType<BrushCopyOverwriteMenu>> BRUSH_COPY_OVERWRITE =
            MENU_TYPES.register("wrench_copy_overwrite_menu", () ->
                    IForgeMenuType.create((containerId, playerInventory, extraData) -> {
                        var blockPos = extraData.readBlockPos();
                        return new BrushCopyOverwriteMenu(containerId, playerInventory, blockPos);
                    })
            );
    
    /** Brush missing block menu type for notification dialog. */
    public static final RegistryObject<MenuType<BrushMissingBlockMenu>> BRUSH_MISSING_BLOCK =
            MENU_TYPES.register("wrench_missing_block_menu", () ->
                    IForgeMenuType.create(BrushMissingBlockMenu::new)
            );
}