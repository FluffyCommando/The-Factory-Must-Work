package com.tfmgtweaks.worldgen;

import com.mojang.serialization.MapCodec;
import com.tfmgtweaks.config.TFMGTweaksConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

/**
 * Same idea as Create's own ConfigPlacementFilter, which TFMG's own oil
 * placed features already reference for a global "disable worldgen"
 * switch -- this is our own equivalent, gating TFMG's bedrock-locked oil
 * nodes behind our own oilRockReplacesOldOilNodes config option.
 *
 * Applied via a resource override of TFMG's own placed features, not a
 * mixin -- placement modifier lists are just data.
 */
public class OilNodeConfigFilter extends PlacementFilter {

    public static final OilNodeConfigFilter INSTANCE = new OilNodeConfigFilter();
    public static final MapCodec<OilNodeConfigFilter> CODEC = MapCodec.unit(() -> INSTANCE);

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        return !TFMGTweaksConfig.OIL_ROCK_REPLACES_OLD_OIL_NODES.get();
    }

    @Override
    public PlacementModifierType<?> type() {
        return TFMGTweaksPlacementModifiers.OIL_NODE_CONFIG_FILTER.get();
    }
}
