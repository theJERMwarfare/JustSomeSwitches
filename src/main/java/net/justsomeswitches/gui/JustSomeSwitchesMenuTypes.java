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
    public static final RegistryObject<MenuType<SwitchesTextureMenu>> SWITCH_TEXTURE_MENU =
            MENU_TYPES.register("switch_texture_menu", () ->
                    IForgeMenuType.create((containerId, playerInventory, extraData) -> {
                        var blockPos = extraData.readBlockPos();
                        return new SwitchesTextureMenu(containerId, playerInventory, blockPos);
                    })
            );
    
    /** Wrench copy menu type for selective texture settings. */
    public static final RegistryObject<MenuType<WrenchCopyMenu>> WRENCH_COPY =
            MENU_TYPES.register("wrench_copy_menu", () ->
                    IForgeMenuType.create((containerId, playerInventory, extraData) -> {
                        var blockPos = extraData.readBlockPos();
                        return new WrenchCopyMenu(containerId, playerInventory, blockPos);
                    })
            );
    
    /** Wrench overwrite menu type for paste confirmation. */
    public static final RegistryObject<MenuType<WrenchOverwriteMenu>> WRENCH_OVERWRITE =
            MENU_TYPES.register("wrench_overwrite_menu", () ->
                    IForgeMenuType.create((containerId, playerInventory, extraData) -> {
                        var blockPos = extraData.readBlockPos();
                        return new WrenchOverwriteMenu(containerId, playerInventory, blockPos);
                    })
            );
    
    /** Wrench copy overwrite menu type for copy confirmation. */
    public static final RegistryObject<MenuType<WrenchCopyOverwriteMenu>> WRENCH_COPY_OVERWRITE =
            MENU_TYPES.register("wrench_copy_overwrite_menu", () ->
                    IForgeMenuType.create((containerId, playerInventory, extraData) -> {
                        var blockPos = extraData.readBlockPos();
                        return new WrenchCopyOverwriteMenu(containerId, playerInventory, blockPos);
                    })
            );
    
    /** Wrench missing block menu type for notification dialog. */
    public static final RegistryObject<MenuType<WrenchMissingBlockMenu>> WRENCH_MISSING_BLOCK =
            MENU_TYPES.register("wrench_missing_block_menu", () ->
                    IForgeMenuType.create(WrenchMissingBlockMenu::new)
            );
}