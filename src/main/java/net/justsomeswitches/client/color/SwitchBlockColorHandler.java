package net.justsomeswitches.client.color;

import net.justsomeswitches.blockentity.SwitchesLeverBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

/** Universal block color handler using BlockColors delegation for ANY tinted block. */
public class SwitchBlockColorHandler implements BlockColor {
    
    @Override
    public int getColor(@Nonnull BlockState state, @Nullable BlockAndTintGetter level, 
                       @Nullable BlockPos pos, int tintIndex) {
        
        // GUI context - no level/pos for biome colors
        if (level == null || pos == null) {
            return 0xFFFFFF; // White fallback
        }
        
        // Get block entity
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof SwitchesLeverBlockEntity switchBE)) {
            return 0xFFFFFF;
        }
        
        // Get source block state
        BlockState sourceState = switchBE.getSourceBlockState();
        if (sourceState == null) {
            return 0xFFFFFF;
        }
        
        // UNIVERSAL DELEGATION - works for ANY block!
        // Delegates to Minecraft's BlockColors registry which handles:
        // - Grass-type tinting (biome-aware)
        // - Foliage-type tinting (biome-aware)
        // - Water-type tinting (biome-aware)
        // - Custom tinting from modded blocks
        // - All automatically, with zero block-specific code
        BlockColors blockColors = Minecraft.getInstance().getBlockColors();
        return blockColors.getColor(sourceState, level, pos, tintIndex);
    }
}
