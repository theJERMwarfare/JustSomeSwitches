package net.justsomeswitches.client.color;

import net.justsomeswitches.blockentity.SwitchBlockEntity;
import net.justsomeswitches.client.model.SwitchDynamicModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

/** Universal block color handler using BlockColors delegation with category-aware source state selection. */
public class SwitchBlockColorHandler implements BlockColor {
    @Override
    public int getColor(@Nonnull BlockState state, @Nullable BlockAndTintGetter level,
                       @Nullable BlockPos pos, int tintIndex) {
        if (level == null || pos == null) {
            return 0xFFFFFF;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof SwitchBlockEntity switchBE)) {
            return 0xFFFFFF;
        }
        // Decode tintIndex offset to determine category and actual tint index
        // Toggle quads: tintIndex 0-99 → delegate to toggleSourceBlockState
        // Base quads: tintIndex 100+ → delegate to baseSourceBlockState with (tintIndex - offset)
        BlockState sourceState;
        int actualTintIndex;
        if (tintIndex >= SwitchDynamicModel.BASE_TINT_OFFSET) {
            sourceState = switchBE.getBaseSourceBlockState();
            actualTintIndex = tintIndex - SwitchDynamicModel.BASE_TINT_OFFSET;
        } else {
            sourceState = switchBE.getToggleSourceBlockState();
            actualTintIndex = tintIndex;
        }
        if (sourceState == null) {
            return 0xFFFFFF;
        }
        BlockColors blockColors = Minecraft.getInstance().getBlockColors();
        return blockColors.getColor(sourceState, level, pos, actualTintIndex);
    }
}
