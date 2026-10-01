package com.tfmgtweaks.worldgen;

import com.tfmgtweaks.TFMGTweaks;
import com.tfmgtweaks.config.TFMGTweaksConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Replaces TFMG's old oil nodes with Oil Rock clusters as chunks load, when enabled in config.
 * Migrations are queued and processed one per tick.
 */
@EventBusSubscriber(modid = TFMGTweaks.MOD_ID)
public class OldOilNodeMigration {
    private static final int MAX_MIGRATIONS_PER_TICK = 1;

    private record PendingMigration(ServerLevel level, BlockPos oldMarkerPos) {
    }

    private static final Deque<PendingMigration> PENDING = new ArrayDeque<>();

    private static Block oilDepositBlock() {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("tfmg", "oil_deposit"));
    }

    private static Fluid crudeOilFluid() {
        return BuiltInRegistries.FLUID.get(ResourceLocation.fromNamespaceAndPath("tfmg", "crude_oil"));
    }

    private static Block fossilstoneBlock() {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("tfmg", "fossilstone"));
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!TFMGTweaksConfig.OIL_ROCK_REPLACES_OLD_OIL_NODES.get()
                || !TFMGTweaksConfig.OIL_ROCK_MIGRATE_OLD_DEPOSITS.get()) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        ChunkAccess chunk = event.getChunk();
        ChunkPos chunkPos = chunk.getPos();
        Block oilDeposit = oilDepositBlock();

        boolean fullHeight = TFMGTweaksConfig.OIL_ROCK_MIGRATE_SCAN_FULL_HEIGHT.get();
        int minY = fullHeight ? serverLevel.getMinBuildHeight() : -64;
        int maxY = fullHeight ? serverLevel.getMaxBuildHeight() - 1 : -64;

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = chunkPos.getMinBlockX(); x <= chunkPos.getMaxBlockX(); x++) {
            for (int z = chunkPos.getMinBlockZ(); z <= chunkPos.getMaxBlockZ(); z++) {
                for (int y = minY; y <= maxY; y++) {
                    cursor.set(x, y, z);
                    if (chunk.getBlockState(cursor).is(oilDeposit)) {
                        PENDING.add(new PendingMigration(serverLevel, cursor.immutable()));
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        int processed = 0;
        while (processed < MAX_MIGRATIONS_PER_TICK && !PENDING.isEmpty()) {
            PendingMigration next = PENDING.poll();
            migrate(next.level(), next.oldMarkerPos());
            processed++;
        }
    }

    /** Clears the oil shaft, fossilstone and bedrock around the old node so a new cluster can grow there. */
    private static final int SHAFT_CLEAR_HEIGHT = 24;

    private static void clearOldOilShaft(ServerLevel level, BlockPos markerPos) {
        Fluid crudeOil = crudeOilFluid();
        Block fossilstone = fossilstoneBlock();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = 1; y <= SHAFT_CLEAR_HEIGHT; y++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    cursor.set(markerPos.getX() + dx, markerPos.getY() + y, markerPos.getZ() + dz);
                    BlockState state = level.getBlockState(cursor);
                    boolean isCrudeOil = crudeOil != null && state.getFluidState().getType().isSame(crudeOil);
                    boolean isFossilstone = fossilstone != null && state.is(fossilstone);
                    boolean isVanillaBedrock = state.is(Blocks.BEDROCK);
                    if (isCrudeOil || isFossilstone || isVanillaBedrock) {
                        level.setBlock(cursor, Blocks.DEEPSLATE.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    /** Height above the world bottom still treated as the bottom when choosing bedrock over stone. */
    private static final int NEAR_WORLD_BOTTOM_MARGIN = 8;

    private static void migrate(ServerLevel level, BlockPos oldMarkerPos) {
        // Skip if the old node is already gone.
        if (!level.getBlockState(oldMarkerPos).is(oilDepositBlock())) {
            return;
        }

        clearOldOilShaft(level, oldMarkerPos);

        RandomSource random = RandomSource.create(level.getSeed() ^ oldMarkerPos.asLong());

        BlockPos newAttemptPos = oldMarkerPos.above();

        List<BlockPos> cluster = OilRockFeature.growCluster(level, random, newAttemptPos);
        if (cluster != null) {
            OilRockFeature.placeCluster(level, random, cluster);
        }

        // Replace the old node so it isn't found again: bedrock near the world bottom, stone elsewhere.
        boolean nearWorldBottom = oldMarkerPos.getY() <= level.getMinBuildHeight() + NEAR_WORLD_BOTTOM_MARGIN;
        BlockState deactivatedState = nearWorldBottom ? Blocks.BEDROCK.defaultBlockState() : Blocks.STONE.defaultBlockState();
        level.setBlock(oldMarkerPos, deactivatedState, 3);

    }
}
