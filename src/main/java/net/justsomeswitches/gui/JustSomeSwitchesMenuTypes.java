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
    public static final DeferredHolder<MenuType<?>, MenuType<SwitchesTextureMenu>> SWITCH_TEXTURE_MENU =
            MENU_TYPES.register("switch_texture_menu", () ->
                    IMenuTypeExtension.create((containerId, playerInventory, extraData) -> {
                        var blockPos = extraData.readBlockPos();
                        return new SwitchesTextureMenu(containerId, playerInventory, blockPos);
                    })
            );
    
    /** Wrench copy menu type for selective texture settings. */
    public static final DeferredHolder<MenuType<?>, MenuType<WrenchCopyMenu>> WRENCH_COPY =
            MENU_TYPES.register("wrench_copy_menu", () ->
                    IMenuTypeExtension.create((containerId, playerInventory, extraData) -> {
                        var blockPos = extraData.readBlockPos();
                        return new WrenchCopyMenu(containerId, playerInventory, blockPos);
                    })
            );
    
    /** Wrench overwrite menu type for paste confirmation. */
    public static final DeferredHolder<MenuType<?>, MenuType<WrenchOverwriteMenu>> WRENCH_OVERWRITE =
            MENU_TYPES.register("wrench_overwrite_menu", () ->
                    IMenuTypeExtension.create((containerId, playerInventory, extraData) -> {
                        var blockPos = extraData.readBlockPos();
                        return new WrenchOverwriteMenu(containerId, playerInventory, blockPos);
                    })
            );
    
    /** Wrench copy overwrite menu type for copy confirmation. */
    public static final DeferredHolder<MenuType<?>, MenuType<WrenchCopyOverwriteMenu>> WRENCH_COPY_OVERWRITE =
            MENU_TYPES.register("wrench_copy_overwrite_menu", () ->
                    IMenuTypeExtension.create((containerId, playerInventory, extraData) -> {
                        var blockPos = extraData.readBlockPos();
                        return new WrenchCopyOverwriteMenu(containerId, playerInventory, blockPos);
                    })
            );
    
    /** Wrench missing block menu type for notification dialog. */
    public static final DeferredHolder<MenuType<?>, MenuType<WrenchMissingBlockMenu>> WRENCH_MISSING_BLOCK =
            MENU_TYPES.register("wrench_missing_block_menu", () ->
                    IMenuTypeExtension.create(WrenchMissingBlockMenu::new)
            );
}