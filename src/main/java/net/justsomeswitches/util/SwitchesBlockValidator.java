package net.justsomeswitches.util;

import net.justsomeswitches.SwitchesModTags;
import net.justsomeswitches.config.SwitchesServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * Validates blocks for switch texture customization using hybrid tag + property system.
 * Priority: blocked tag → allowed tag → standard building tags → property checks.
 * Emissive blocks (Sea Lantern, Glowstone) allowed — texture applied at switch brightness only.
 */
public class SwitchesBlockValidator {
    private static final Map<Block, Boolean> VALIDATION_CACHE = new HashMap<>();
    private static final BlockPos TEST_POS = BlockPos.ZERO;
    /** Validates ItemStack for texture customization. */
    public static boolean isValidTextureItem(@Nonnull ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }
        Block block = blockItem.getBlock();
        Boolean cachedResult = VALIDATION_CACHE.get(block);
        if (cachedResult != null) {
            return cachedResult;
        }
        boolean result = validateBlock(block);
        VALIDATION_CACHE.put(block, result);
        return result;
    }
    /** Validates block using hybrid tag + property system. */
    private static boolean validateBlock(@Nonnull Block block) {
        BlockState state = block.defaultBlockState();
        if (state.is(SwitchesModTags.Blocks.SWITCHES_BLOCKED)) {
            return false;
        }
        if (state.is(SwitchesModTags.Blocks.SWITCHES_ALLOWED)) {
            return true;
        }
        if (isInStandardBuildingTags(state)) {
            return true;
        }
        return validateBlockProperties(state);
    }
    /** Checks if block is in standard building material tags for automatic mod compatibility. */
    private static boolean isInStandardBuildingTags(@Nonnull BlockState state) {
        return state.is(Tags.Blocks.STORAGE_BLOCKS) ||
               state.is(BlockTags.PLANKS) ||
               state.is(BlockTags.TERRACOTTA);
    }
    /** Validates block properties: full cube, solid, hardness 0-50, BlockEntity config check. */
    private static boolean validateBlockProperties(@Nonnull BlockState state) {
        if (!Block.isShapeFullBlock(state.getShape(EmptyBlockGetter.INSTANCE, TEST_POS))) {
            return false;
        }
        if (!state.canOcclude()) {
            return false;
        }
        float hardness = state.getDestroySpeed(EmptyBlockGetter.INSTANCE, TEST_POS);
        if (hardness < 0 || hardness > 50.0f) {
            return false;
        }
        try {
            if (!SwitchesServerConfig.ALLOW_BLOCK_ENTITIES.get() && state.getBlock() instanceof EntityBlock) {
                return false;
            }
        } catch (Exception e) {
            if (state.getBlock() instanceof EntityBlock) {
                return false;
            }
        }
        return true;
    }
}
