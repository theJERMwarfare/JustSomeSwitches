package net.justsomeswitches.gui;

import net.justsomeswitches.JustSomeSwitchesMod;
import net.minecraft.core.BlockPos;
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
    public static final DeferredHolder<MenuType<?>, MenuType<SwitchTextureMenu>> SWITCH_TEXTURE_MENU =
            MENU_TYPES.register("switch_texture_menu", () ->
                    IMenuTypeExtension.create((containerId, playerInventory, extraData) -> {
                        // Read block position from network data
                        if (extraData != null) {
                            var blockPos = extraData.readBlockPos();
                            return new SwitchTextureMenu(containerId, playerInventory, blockPos);
                        } else {
                            // Fallback for any edge cases - explicitly cast null to BlockPos to resolve ambiguity
                            return new SwitchTextureMenu(containerId, playerInventory, (BlockPos) null);
                        }
                    })
            );
}