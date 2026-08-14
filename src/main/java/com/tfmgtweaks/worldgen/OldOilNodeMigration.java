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
 * TFMG's own oil_well/oil_deposit are both hardcoded to spawn only at
 * Y=-64, so by default, detecting an unmigrated deposit is just "is
 * tfmg:oil_deposit present at Y=-64 in this chunk" -- a single block
 * check per column. If OIL_ROCK_MIGRATE_SCAN_FULL_HEIGHT is also
 * enabled (for a world where a different mod's own worldgen override
 * let old deposits spawn at other heights too), every Y level in every
 * loaded column is checked instead. Migrating removes the marker found
 * (converting it to bedrock or stone -- see migrate()), so no separate
 * tracking is needed either way.
 *
 * Only fires when both OIL_ROCK_REPLACES_OLD_OIL_NODES and
 * OIL_ROCK_MIGRATE_OLD_DEPOSITS are true, extending the same "Oil Rock
 * is the only way to find oil" intent to pre-existing chunks.
 *
 * Detection happens on chunk load; actual world modification is
 * deferred to a queue drained a few entries per server tick, rather than
 * mutating blocks inside the load event itself.
 */
@EventBusSubscriber(modid = TFMGTweaks.MOD_ID)
public class OldOilNodeMigration {

    private static final int MAX_MIGRATIONS_PER_TICK = 1;

    private record PendingMigration(ServerLevel level, BlockPos oldMarkerPos) {
    }

    private static final Deque<PendingMigration> PENDING = new ArrayDeque<>();

    /** Looked up by ID since Registrate isn't on our compile classpath. */
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

    /**
     * TFMG's own oil deposit feature also carves a shaft of crude oil
     * fluid up to 24 blocks above the marker, plus scattered fossilstone
     * -- both fail growCluster()'s BASE_STONE_OVERWORLD check, so a new
     * cluster can't grow directly above a migrated marker without this.
     *
     * Also clears vanilla bedrock within the same range. A marker at the
     * normal Y=-64 has its attempted starting position (marker.above(),
     * Y=-63) land directly inside vanilla's own randomized bottom-of-
     * world bedrock transition zone -- the bottom several Y levels each
     * have a per-column random chance of generating as bedrock instead
     * of stone, decreasing with height. growCluster() hard-fails
     * (returns null, placing nothing) if the exact starting position
     * isn't stone-type, so without this a large fraction of migration
     * attempts at the normal height would fail purely because vanilla
     * happened to generate bedrock at that one specific column -- not
     * from anything TFMG's oil deposit itself left behind. Harmless
     * no-op for a marker found well above the world's actual bottom
     * (the full-height-scan case), since real vanilla bedrock never
     * generates up there in the first place.
     *
     * Clears a 3x3 column (the shaft can drift horizontally) by
     * SHAFT_CLEAR_HEIGHT, replacing only crude oil fluid, fossilstone,
     * and bedrock with deepslate -- not a blanket clear, so any real
     * cave the shaft passed through is left alone. Starts at y=1 (one
     * above the marker), so this never touches the marker's own
     * position, which migrate() itself deliberately overwrites.
     */
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

    /**
     * How close to the world's actual minimum build height a marker
     * needs to be for bedrock to still be the right deactivation choice
     * -- a small margin above the exact minimum, since vanilla's own
     * randomized bedrock transition zone (see clearOldOilShaft's own
     * doc) extends a few blocks above the true floor too, so a marker
     * just above the literal minimum is still unambiguously "at the
     * bottom of the world" in the way the bedrock choice was designed
     * around.
     */
    private static final int NEAR_WORLD_BOTTOM_MARGIN = 8;

    private static void migrate(ServerLevel level, BlockPos oldMarkerPos) {
        // The marker might already be gone by the time this is actually
        // processed (chunk unloaded and something else changed it,
        // another mod touched it, etc.) -- if so, just skip it rather
        // than forcing a change.
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

        // Deactivate the old marker either way, so this chunk is never
        // re-queued on a later load regardless of whether a new cluster
        // happened to form successfully above it.
        //
        // Bedrock only for a marker actually near the world's bottom --
        // that's where the normal Y=-64 case always lands, and bedrock
        // blends in naturally there as part of the world's own bottom
        // layer instead of reading as an obvious leftover block, with
        // the added benefit that being permanently unbreakable means it
        // can never later be mistaken for something minable. A marker
        // found well above the bottom (only possible via the full-
        // height-scan option, for old deposits a different mod's
        // worldgen let spawn elsewhere) uses stone instead -- an
        // unbreakable bedrock block floating in the middle of, say, a
        // sky island would be far more out of place than a single
        // ordinary stone block sitting where the deposit used to be.
        boolean nearWorldBottom = oldMarkerPos.getY() <= level.getMinBuildHeight() + NEAR_WORLD_BOTTOM_MARGIN;
        BlockState deactivatedState = nearWorldBottom ? Blocks.BEDROCK.defaultBlockState() : Blocks.STONE.defaultBlockState();
        level.setBlock(oldMarkerPos, deactivatedState, 3);

        TFMGTweaks.LOGGER.info("[OldOilNodeMigration] migrated old oil node at {} (new attempt above at {})",
                oldMarkerPos, newAttemptPos);
    }
}
