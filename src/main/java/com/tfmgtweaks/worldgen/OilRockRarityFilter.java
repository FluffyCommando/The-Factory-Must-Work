package com.tfmgtweaks.worldgen;

import com.mojang.serialization.MapCodec;
import com.tfmgtweaks.config.TFMGTweaksConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

/** Rarity filter with its chance read from config at runtime. */
public class OilRockRarityFilter extends PlacementFilter {
    public static final OilRockRarityFilter INSTANCE = new OilRockRarityFilter();
    public static final MapCodec<OilRockRarityFilter> CODEC = MapCodec.unit(() -> INSTANCE);

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        int chance = TFMGTweaksConfig.OIL_ROCK_SPAWN_CHANCE.get();
        return chance <= 1 || random.nextInt(chance) == 0;
    }

    @Override
    public PlacementModifierType<?> type() {
        return TFMGTweaksPlacementModifiers.OIL_ROCK_RARITY_FILTER.get();
    }
}
