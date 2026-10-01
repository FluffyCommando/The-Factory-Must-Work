package com.tfmgtweaks.worldgen;

import com.tfmgtweaks.config.TFMGTweaksConfig;
import com.tfmgtweaks.content.oilrock.OilRockBlockEntity;
import com.tfmgtweaks.registry.TFMGTweaksBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FlowingFluid;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Places a cluster of Oil Rock confined to one chunk, plus up to maxNearbyDeposits satellite clusters,
 * with a little visible crude oil beside them.
 */
public class OilRockFeature extends Feature<NoneFeatureConfiguration> {
    private static final int MIN_CLUSTER_SIZE = 14;
    private static final int MAX_CLUSTER_SIZE_BONUS = 18;
    private static final float GROWTH_CHANCE = 0.55f;
    private static final float OIL_SPRINKLE_CHANCE = 0.35f;
    private static final int SATELLITE_MIN_DISTANCE = 10;
    private static final int SATELLITE_MAX_DISTANCE_BONUS = 15;
    private static final int SATELLITE_VERTICAL_SPREAD = 10;
    private static final int SATELLITE_FIND_ATTEMPTS = 10;

    public OilRockFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        BlockPos startingPos = context.origin();
        WorldGenLevel level = context.level();
        RandomSource random = context.random();

        List<BlockPos> primaryCluster = growCluster(level, random, startingPos);
        if (primaryCluster == null) {
            return false;
        }
        placeCluster(level, random, primaryCluster);

        int maxSatellites = TFMGTweaksConfig.OIL_ROCK_MAX_NEARBY_DEPOSITS.get();
        int satelliteCount = maxSatellites > 0 ? 1 + random.nextInt(maxSatellites) : 0;
        for (int i = 0; i < satelliteCount; i++) {
            BlockPos satelliteStart = findSatelliteStart(level, random, startingPos);
            if (satelliteStart == null) {
                continue;
            }
            List<BlockPos> satelliteCluster = growCluster(level, random, satelliteStart);
            if (satelliteCluster != null) {
                placeCluster(level, random, satelliteCluster);
            }
        }

        return true;
    }

    /** A stone position 10-24 blocks from origin within the neighboring chunks, or null. */
    @Nullable
    public static BlockPos findSatelliteStart(WorldGenLevel level, RandomSource random, BlockPos origin) {
        for (int attempt = 0; attempt < SATELLITE_FIND_ATTEMPTS; attempt++) {
            int distanceX = SATELLITE_MIN_DISTANCE + random.nextInt(SATELLITE_MAX_DISTANCE_BONUS);
            int distanceZ = SATELLITE_MIN_DISTANCE + random.nextInt(SATELLITE_MAX_DISTANCE_BONUS);
            int dx = random.nextBoolean() ? distanceX : -distanceX;
            int dz = random.nextBoolean() ? distanceZ : -distanceZ;
            int dy = random.nextInt(SATELLITE_VERTICAL_SPREAD * 2 + 1) - SATELLITE_VERTICAL_SPREAD;

            BlockPos candidate = origin.offset(dx, dy, dz);
            if (Math.abs(chunkX(candidate) - chunkX(origin)) > 1 || Math.abs(chunkZ(candidate) - chunkZ(origin)) > 1) {
                continue;
            }
            if (level.getBlockState(candidate).is(BlockTags.BASE_STONE_OVERWORLD)) {
                return candidate;
            }
        }
        return null;
    }

    /** Randomized flood fill that stays in the starting chunk; null if the start isn't stone. */
    @Nullable
    public static List<BlockPos> growCluster(WorldGenLevel level, RandomSource random, BlockPos startingPos) {
        if (!level.getBlockState(startingPos).is(BlockTags.BASE_STONE_OVERWORLD)) {
            return null;
        }

        int targetSize = MIN_CLUSTER_SIZE + random.nextInt(MAX_CLUSTER_SIZE_BONUS);
        int startChunkX = chunkX(startingPos);
        int startChunkZ = chunkZ(startingPos);

        List<BlockPos> cluster = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> frontier = new ArrayDeque<>();
        frontier.add(startingPos);
        visited.add(startingPos);

        while (!frontier.isEmpty() && cluster.size() < targetSize) {
            BlockPos current = frontier.poll();
            if (!level.getBlockState(current).is(BlockTags.BASE_STONE_OVERWORLD)) {
                continue;
            }
            cluster.add(current);

            for (Direction direction : Direction.values()) {
                if (random.nextFloat() < GROWTH_CHANCE) {
                    BlockPos neighbor = current.relative(direction);
                    if (chunkX(neighbor) != startChunkX || chunkZ(neighbor) != startChunkZ) {
                        continue;
                    }
                    if (visited.add(neighbor)) {
                        frontier.add(neighbor);
                    }
                }
            }
        }

        return cluster.isEmpty() ? null : cluster;
    }

    /** Places the cluster, sets up its controller and sprinkles visible oil. */
    public static void placeCluster(WorldGenLevel level, RandomSource random, List<BlockPos> cluster) {
        for (BlockPos pos : cluster) {
            level.setBlock(pos, TFMGTweaksBlocks.OIL_ROCK.get().defaultBlockState(), 2);
        }

        sprinkleVisibleOil(level, random, cluster);

        BlockPos controllerPos = cluster.get(0);
        if (level.getBlockEntity(controllerPos) instanceof OilRockBlockEntity controllerBE) {
            controllerBE.initializeAsController(cluster);
        }
        for (int i = 1; i < cluster.size(); i++) {
            if (level.getBlockEntity(cluster.get(i)) instanceof OilRockBlockEntity memberBE) {
                memberBE.initializeAsMember(controllerPos);
            }
        }

    }

    private static void sprinkleVisibleOil(WorldGenLevel level, RandomSource random, List<BlockPos> cluster) {
        Fluid crudeOil = BuiltInRegistries.FLUID.get(ResourceLocation.fromNamespaceAndPath("tfmg", "crude_oil"));
        Fluid crudeOilSource = crudeOil instanceof FlowingFluid flowingFluid ? flowingFluid.getSource() : crudeOil;
        if (crudeOilSource == null) {
            return;
        }

        Set<BlockPos> clusterPositions = new HashSet<>(cluster);
        int clusterChunkX = chunkX(cluster.get(0));
        int clusterChunkZ = chunkZ(cluster.get(0));
        for (BlockPos memberPos : cluster) {
            if (random.nextFloat() >= OIL_SPRINKLE_CHANCE) {
                continue;
            }
            for (Direction direction : Direction.values()) {
                BlockPos adjacent = memberPos.relative(direction);
                if (clusterPositions.contains(adjacent)
                        || chunkX(adjacent) != clusterChunkX || chunkZ(adjacent) != clusterChunkZ) {
                    continue;
                }
                if (level.getBlockState(adjacent).isAir()) {
                    level.setBlock(adjacent, crudeOilSource.defaultFluidState().createLegacyBlock(), 2);
                    break;
                }
            }
        }
    }

    private static int chunkX(BlockPos pos) {
        return SectionPos.blockToSectionCoord(pos.getX());
    }

    private static int chunkZ(BlockPos pos) {
        return SectionPos.blockToSectionCoord(pos.getZ());
    }
}
