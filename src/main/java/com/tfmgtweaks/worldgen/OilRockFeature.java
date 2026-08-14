package com.tfmgtweaks.worldgen;

import com.tfmgtweaks.TFMGTweaks;
import com.tfmgtweaks.config.TFMGTweaksConfig;
import com.tfmgtweaks.content.oilrock.OilRockBlockEntity;
import com.tfmgtweaks.registry.TFMGTweaksBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
 * Places a large, organically-shaped cluster of connected Oil Rock blocks
 * via a randomized flood-fill from the origin point. Every block in a
 * cluster shares one deposit (see OilRockBlockEntity's controller/member
 * pattern) -- the first-placed block becomes that cluster's controller.
 *
 * Replaces any block in minecraft:base_stone_overworld (stone and
 * deepslate), not just exact stone.
 *
 * After the primary cluster, up to OIL_ROCK_MAX_NEARBY_DEPOSITS
 * additional, fully independent "satellite" clusters attempt to spawn
 * nearby, each with its own controller/reserves.
 *
 * Also sprinkles a few visible crude oil source blocks into adjacent air
 * pockets around each cluster, purely so deposits are visible while
 * scouting -- cosmetic only.
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

    /**
     * Attempts to find a valid starting point for a satellite deposit near
     * `origin`. Returns null (giving up on this particular satellite,
     * not the whole feature) if no valid stone-type position is found
     * within a few tries.
     */
    @Nullable
    public static BlockPos findSatelliteStart(WorldGenLevel level, RandomSource random, BlockPos origin) {
        for (int attempt = 0; attempt < SATELLITE_FIND_ATTEMPTS; attempt++) {
            int distanceX = SATELLITE_MIN_DISTANCE + random.nextInt(SATELLITE_MAX_DISTANCE_BONUS);
            int distanceZ = SATELLITE_MIN_DISTANCE + random.nextInt(SATELLITE_MAX_DISTANCE_BONUS);
            int dx = random.nextBoolean() ? distanceX : -distanceX;
            int dz = random.nextBoolean() ? distanceZ : -distanceZ;
            int dy = random.nextInt(SATELLITE_VERTICAL_SPREAD * 2 + 1) - SATELLITE_VERTICAL_SPREAD;

            BlockPos candidate = origin.offset(dx, dy, dz);
            if (level.getBlockState(candidate).is(BlockTags.BASE_STONE_OVERWORLD)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Randomized flood-fill from `startingPos`. Returns null if the
     * starting position itself isn't valid (matching the original
     * behavior of failing the whole attempt rather than growing from a
     * bad seed).
     */
    @Nullable
    public static List<BlockPos> growCluster(WorldGenLevel level, RandomSource random, BlockPos startingPos) {
        if (!level.getBlockState(startingPos).is(BlockTags.BASE_STONE_OVERWORLD)) {
            return null;
        }

        int targetSize = MIN_CLUSTER_SIZE + random.nextInt(MAX_CLUSTER_SIZE_BONUS);

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
                    if (visited.add(neighbor)) {
                        frontier.add(neighbor);
                    }
                }
            }
        }

        return cluster.isEmpty() ? null : cluster;
    }

    /**
     * Places every block in `cluster`, wires up the controller/member
     * relationship (first block = controller), sprinkles visible oil, and
     * logs the result.
     */
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

        TFMGTweaks.LOGGER.info("[OilRockFeature] placed cluster of {} blocks at {}", cluster.size(), controllerPos);
    }

    private static void sprinkleVisibleOil(WorldGenLevel level, RandomSource random, List<BlockPos> cluster) {
        Fluid crudeOil = BuiltInRegistries.FLUID.get(ResourceLocation.fromNamespaceAndPath("tfmg", "crude_oil"));
        Fluid crudeOilSource = crudeOil instanceof FlowingFluid flowingFluid ? flowingFluid.getSource() : crudeOil;
        if (crudeOilSource == null) {
            return;
        }

        Set<BlockPos> clusterPositions = new HashSet<>(cluster);
        for (BlockPos memberPos : cluster) {
            if (random.nextFloat() >= OIL_SPRINKLE_CHANCE) {
                continue;
            }
            for (Direction direction : Direction.values()) {
                BlockPos adjacent = memberPos.relative(direction);
                if (clusterPositions.contains(adjacent)) {
                    continue;
                }
                if (level.getBlockState(adjacent).isAir()) {
                    level.setBlock(adjacent, crudeOilSource.defaultFluidState().createLegacyBlock(), 2);
                    break;
                }
            }
        }
    }
}
