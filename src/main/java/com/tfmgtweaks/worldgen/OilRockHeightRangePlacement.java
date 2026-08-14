package com.tfmgtweaks.worldgen;

import com.mojang.serialization.MapCodec;
import com.tfmgtweaks.config.TFMGTweaksConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

import java.util.stream.Stream;

/**
 * Same idea as vanilla's minecraft:height_range, but reads min/max height
 * from TFMGTweaksConfig at runtime instead of a fixed JSON value, so the
 * user can adjust where Oil Rock clusters start spawning via config
 * without needing a resource pack.
 */
public class OilRockHeightRangePlacement extends PlacementModifier {

    public static final OilRockHeightRangePlacement INSTANCE = new OilRockHeightRangePlacement();
    public static final MapCodec<OilRockHeightRangePlacement> CODEC = MapCodec.unit(() -> INSTANCE);

    @Override
    public Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos pos) {
        int min = TFMGTweaksConfig.OIL_ROCK_MIN_HEIGHT.get();
        int max = TFMGTweaksConfig.OIL_ROCK_MAX_HEIGHT.get();
        if (max < min) {
            int tmp = min;
            min = max;
            max = tmp;
        }
        int y = min + random.nextInt(Math.max(1, max - min + 1));
        return Stream.of(new BlockPos(pos.getX(), y, pos.getZ()));
    }

    @Override
    public PlacementModifierType<?> type() {
        return TFMGTweaksPlacementModifiers.OIL_ROCK_HEIGHT_RANGE.get();
    }
}
