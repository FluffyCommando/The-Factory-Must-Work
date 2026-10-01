package com.tfmgtweaks.explosion;

import com.tfmgtweaks.TFMGTweaks;
import com.tfmgtweaks.compat.FlowingFluidsCompat;
import com.tfmgtweaks.compat.TFMGTagKeys;
import com.tfmgtweaks.config.TFMGTweaksConfig;
import com.tfmgtweaks.registry.TFMGTweaksFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Ignites and spreads fire through flammable fluid, replacing it with burning fuel a few blocks per tick. */
@EventBusSubscriber(modid = TFMGTweaks.MOD_ID)
public class FluidIgnition {

    private record PendingIgnition(ServerLevel level, BlockPos pos, int remainingBudget) {
    }

    private static final Deque<PendingIgnition> PENDING = new ArrayDeque<>();

    // Ignitions started by a player, fire, lava, an explosion or a burning entity; processed before spread.
    private static final Deque<PendingIgnition> PRIORITY_PENDING = new ArrayDeque<>();

    private record BurningFluid(ServerLevel level, BlockPos fluidPos) {
    }

    // Positions already queued, so they aren't queued twice.
    private static final Set<BurningFluid> PENDING_POSITIONS = new HashSet<>();

    private static final Set<BurningFluid> BURNING_FLUID_POSITIONS = new HashSet<>();

    private static final Map<BurningFluid, Long> IGNITION_TIME = new HashMap<>();

    private static final Deque<BurningFluid> PROACTIVE_CHECK_QUEUE = new ArrayDeque<>();

    // Chunk loads may happen off-thread, so discoveries are applied on the server tick.
    private static final Queue<BurningFluid> CHUNK_LOAD_DISCOVERIES = new ConcurrentLinkedQueue<>();

    private record PendingRemoval(ServerLevel level, BlockPos pos) {
    }

    private record CostedPos(BlockPos pos, int cost) {
    }

    private static final Deque<PendingRemoval> REMOVAL_FRONTIER = new ArrayDeque<>();

    // Positions being cleared, which fluid physics must not refill.
    private static final Set<BurningFluid> ACTIVELY_CLEARING = new HashSet<>();

    public static boolean isActivelyClearing(ServerLevel level, BlockPos pos) {
        return ACTIVELY_CLEARING.contains(new BurningFluid(level, pos));
    }

    private static final Direction[] SPREAD_DIRECTIONS = {
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN
    };

    // Replaces the fluid at fluidPos with burning fuel and starts tracking it.
    public static boolean markBurning(ServerLevel level, BlockPos fluidPos) {
        boolean isNew = BURNING_FLUID_POSITIONS.add(new BurningFluid(level, fluidPos.immutable()));
        if (isNew) {
            IGNITION_TIME.put(new BurningFluid(level, fluidPos.immutable()), level.getGameTime());
            FluidState originalFluidState = level.getFluidState(fluidPos);
            if (isBurningFuel(originalFluidState)) {
                return true;
            }
            if (originalFluidState.isSource()) {
                level.setBlockAndUpdate(fluidPos,
                        TFMGTweaksFluids.BURNING_FUEL_SOURCE.get().defaultFluidState().createLegacyBlock());
            } else {
                BlockState originalBlockState = level.getBlockState(fluidPos);
                BlockState burningState =
                        TFMGTweaksFluids.BURNING_FUEL_FLOWING.get().defaultFluidState().createLegacyBlock();
                if (originalBlockState.hasProperty(LiquidBlock.LEVEL) && burningState.hasProperty(LiquidBlock.LEVEL)) {
                    burningState = burningState.setValue(LiquidBlock.LEVEL, originalBlockState.getValue(LiquidBlock.LEVEL));
                }
                level.setBlockAndUpdate(fluidPos, burningState);
            }

            checkSourceSupport(level, fluidPos);
        }
        return isNew;
    }

    // Raytrace that includes fluid surfaces, which vanilla interaction raytracing skips.
    private static BlockHitResult fluidInclusiveRaytrace(ServerLevel level, Player player) {
        double reach = 5.0;
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 endPos = eyePos.add(look.x * reach, look.y * reach, look.z * reach);
        return level.clip(new ClipContext(eyePos, endPos, ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, player));
    }

    private static boolean isFlammable(FluidState fluidState) {
        return !fluidState.isEmpty() && fluidState.getType().is(TFMGTagKeys.FLAMMABLE_FLUID);
    }

    private static boolean isBurningFuel(FluidState fluidState) {
        return fluidState.getType() == TFMGTweaksFluids.BURNING_FUEL_SOURCE.get()
                || fluidState.getType() == TFMGTweaksFluids.BURNING_FUEL_FLOWING.get();
    }

    // Flammable fluid that isn't burning yet; burning fuel is itself tagged flammable.
    private static boolean isUnlitFlammable(FluidState fluidState) {
        return isFlammable(fluidState) && !isBurningFuel(fluidState);
    }

    // Explosions don't list fluid blocks as affected, so the blast area is scanned for flammable fluid.
    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (!TFMGTweaksConfig.FLUID_IGNITION_ENABLED.get()) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        int maxSpread = TFMGTweaksConfig.FLUID_IGNITION_MAX_SPREAD.get();

        List<BlockPos> affected = event.getAffectedBlocks();
        if (affected.isEmpty()) {
            return;
        }

        int[] xs = new int[affected.size()];
        int[] ys = new int[affected.size()];
        int[] zs = new int[affected.size()];
        for (int i = 0; i < affected.size(); i++) {
            BlockPos pos = affected.get(i);
            xs[i] = pos.getX();
            ys[i] = pos.getY();
            zs[i] = pos.getZ();
        }
        Arrays.sort(xs);
        Arrays.sort(ys);
        Arrays.sort(zs);
        int medianX = xs[xs.length / 2];
        int medianY = ys[ys.length / 2];
        int medianZ = zs[zs.length / 2];

        // Ignores outlier positions that would produce an enormous scan area.
        final int outlierThreshold = 128;

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        int keptCount = 0;
        for (BlockPos pos : affected) {
            if (Math.abs(pos.getX() - medianX) > outlierThreshold
                    || Math.abs(pos.getY() - medianY) > outlierThreshold
                    || Math.abs(pos.getZ() - medianZ) > outlierThreshold) {
                continue;
            }
            keptCount++;
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        if (keptCount == 0) {
            minX = maxX = medianX;
            minY = maxY = medianY;
            minZ = maxZ = medianZ;
        }

        int centerX = (minX + maxX) / 2;
        int centerY = (minY + maxY) / 2;
        int centerZ = (minZ + maxZ) / 2;
        int radius = Math.max(2, Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ)) / 2 + 2);
        if (radius > 64) {
            radius = 64;
        }
        int radiusSq = radius * radius;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz > radiusSq) {
                        continue;
                    }
                    cursor.set(centerX + dx, centerY + dy, centerZ + dz);
                    if (isUnlitFlammable(level.getFluidState(cursor))) {
                        queuePriorityIgnition(level, cursor, maxSpread);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!TFMGTweaksConfig.FLUID_IGNITION_ENABLED.get()) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        ItemStack stack = event.getItemStack();
        boolean isFlintAndSteel = stack.getItem() instanceof FlintAndSteelItem;
        boolean isFireCharge = stack.getItem() == Items.FIRE_CHARGE;
        if (!isFlintAndSteel && !isFireCharge) {
            return;
        }
        Player player = event.getEntity();
        BlockHitResult hit = fluidInclusiveRaytrace(level, player);
        if (hit.getType() != BlockHitResult.Type.BLOCK) {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        FluidState fluidState = level.getFluidState(pos);
        if (!isUnlitFlammable(fluidState)) {
            return;
        }

        level.playSound(player, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS,
                1.0F, level.getRandom().nextFloat() * 0.4F + 0.8F);

        if (isFlintAndSteel) {
            InteractionHand hand = event.getHand();
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        } else if (!player.isCreative()) {
            stack.shrink(1);
        }

        queuePriorityIgnition(level, pos, TFMGTweaksConfig.FLUID_IGNITION_MAX_SPREAD.get());
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    // Resumes tracking burning fuel already present in a loaded chunk.
    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!TFMGTweaksConfig.FLUID_IGNITION_ENABLED.get()) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }

        LevelChunkSection[] sections = chunk.getSections();
        int minSectionIndex = level.getMinSection();
        ChunkPos chunkPos = chunk.getPos();

        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection section = sections[i];
            if (section == null || section.hasOnlyAir()) {
                continue;
            }
            if (!section.getStates().maybeHas(state -> isBurningFuel(state.getFluidState()))) {
                continue;
            }

            int sectionMinY = (minSectionIndex + i) << 4;
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        BlockState state = section.getBlockState(x, y, z);
                        if (!isBurningFuel(state.getFluidState())) {
                            continue;
                        }
                        BlockPos pos = new BlockPos(
                                chunkPos.getMinBlockX() + x, sectionMinY + y, chunkPos.getMinBlockZ() + z);
                        rediscoverTracking(level, pos);
                    }
                }
            }
        }
    }

    private static void rediscoverTracking(ServerLevel level, BlockPos pos) {
        CHUNK_LOAD_DISCOVERIES.add(new BurningFluid(level, pos.immutable()));
    }

    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (!TFMGTweaksConfig.FLUID_IGNITION_ENABLED.get()) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockState changedState = event.getState();
        BlockPos changedPos = event.getPos();
        int maxSpread = TFMGTweaksConfig.FLUID_IGNITION_MAX_SPREAD.get();

        for (Direction side : event.getNotifiedSides()) {
            checkSourceSupport(level, changedPos.relative(side));
        }

        boolean isIgnitionSource = changedState.is(Blocks.LAVA) || changedState.is(Blocks.FIRE)
                || changedState.is(Blocks.SOUL_FIRE);
        if (isIgnitionSource) {
            for (Direction side : event.getNotifiedSides()) {
                BlockPos neighborPos = changedPos.relative(side);
                FluidState fluidState = level.getFluidState(neighborPos);
                if (isUnlitFlammable(fluidState)) {
                    queuePriorityIgnition(level, neighborPos, maxSpread);
                }
            }
            return;
        }

        if (isUnlitFlammable(changedState.getFluidState())) {
            for (Direction side : SPREAD_DIRECTIONS) {
                if (isBurningFuel(level.getFluidState(changedPos.relative(side)))) {
                    queueIgnition(level, changedPos, maxSpread);
                    return;
                }
            }
        }
    }

    // Removes non-source burning fuel with no connected source after its grace period; off with Flowing Fluids.
    private static void checkSourceSupport(ServerLevel level, BlockPos pos) {
        if (FlowingFluidsCompat.isLoaded()) {
            return;
        }
        if (!BURNING_FLUID_POSITIONS.contains(new BurningFluid(level, pos))) {
            return;
        }
        FluidState fluidState = level.getFluidState(pos);
        if (!isBurningFuel(fluidState) || fluidState.isSource()) {
            return;
        }
        int radius = TFMGTweaksConfig.FLUID_IGNITION_SOURCE_SUPPORT_RADIUS.get();
        int cost = connectedSourceCost(level, pos, radius);
        if (cost < 0) {
            if (isWithinSupportGracePeriod(level, pos)) {
                return;
            }
            REMOVAL_FRONTIER.add(new PendingRemoval(level, pos));
        }
    }

    private static boolean isWithinSupportGracePeriod(ServerLevel level, BlockPos pos) {
        Long ignitedAt = IGNITION_TIME.get(new BurningFluid(level, pos));
        if (ignitedAt == null) {
            return false;
        }
        int gracePeriod = TFMGTweaksConfig.FLUID_IGNITION_SUPPORT_GRACE_PERIOD.get();
        return level.getGameTime() - ignitedAt < gracePeriod;
    }

    // Dijkstra search, since an upward step resets the spread cost.
    private static int connectedSourceCost(ServerLevel level, BlockPos pos, int maxDistance) {
        if (level.getFluidState(pos).isSource()) {
            return 0;
        }

        Map<BlockPos, Integer> bestCost = new HashMap<>();
        bestCost.put(pos, 0);
        PriorityQueue<CostedPos> queue = new PriorityQueue<>(Comparator.comparingInt(CostedPos::cost));
        queue.add(new CostedPos(pos, 0));

        while (!queue.isEmpty()) {
            CostedPos current = queue.poll();
            if (current.cost() > bestCost.getOrDefault(current.pos(), Integer.MAX_VALUE)) {
                continue;
            }
            for (Direction side : SPREAD_DIRECTIONS) {
                BlockPos neighbor = current.pos().relative(side);
                FluidState neighborState = level.getFluidState(neighbor);
                if (!isBurningFuel(neighborState)) {
                    continue;
                }
                if (neighborState.isSource()) {
                    if (neighbor.getY() >= pos.getY()) {
                        return current.cost() + 1;
                    }
                    continue;
                }
                boolean isUpwardStep = neighbor.getY() > current.pos().getY();
                int newCost = isUpwardStep ? 0 : current.cost() + 1;
                if (newCost > maxDistance) {
                    continue;
                }
                Integer existingCost = bestCost.get(neighbor);
                if (existingCost == null || newCost < existingCost) {
                    bestCost.put(neighbor, newCost);
                    queue.add(new CostedPos(neighbor, newCost));
                }
            }
        }
        return -1;
    }

    private static boolean isConnectedToSource(ServerLevel level, BlockPos pos, int maxDistance) {
        return connectedSourceCost(level, pos, maxDistance) >= 0;
    }

    // Burning entities ignite flammable fluid at or directly below their feet.
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        if (!TFMGTweaksConfig.FLUID_IGNITION_ENABLED.get()) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity entity) || !entity.isOnFire()) {
            return;
        }
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        int maxSpread = TFMGTweaksConfig.FLUID_IGNITION_MAX_SPREAD.get();
        BlockPos feetPos = entity.blockPosition();
        if (isUnlitFlammable(level.getFluidState(feetPos))) {
            queuePriorityIgnition(level, feetPos, maxSpread);
            return;
        }
        BlockPos belowFeet = feetPos.below();
        if (isUnlitFlammable(level.getFluidState(belowFeet))) {
            queuePriorityIgnition(level, belowFeet, maxSpread);
        }
    }

    private static void queueIgnition(ServerLevel level, BlockPos pos, int remainingBudget) {
        BlockPos immutablePos = pos.immutable();
        if (!PENDING_POSITIONS.add(new BurningFluid(level, immutablePos))) {
            return;
        }
        PENDING.add(new PendingIgnition(level, immutablePos, remainingBudget));
    }

    // Not deduplicated against the spread queue, so a direct ignition never waits behind it.
    private static void queuePriorityIgnition(ServerLevel level, BlockPos pos, int remainingBudget) {
        BlockPos immutablePos = pos.immutable();
        PENDING_POSITIONS.add(new BurningFluid(level, immutablePos));
        PRIORITY_PENDING.add(new PendingIgnition(level, immutablePos, remainingBudget));
    }

    // Tracks burning fuel that appeared on its own, e.g. by flowing, and lights its unlit neighbors.
    private static void track(ServerLevel level, BlockPos pos) {
        BurningFluid burning = new BurningFluid(level, pos.immutable());
        if (!BURNING_FLUID_POSITIONS.add(burning)) {
            return;
        }
        IGNITION_TIME.put(burning, level.getGameTime());
        int maxSpread = TFMGTweaksConfig.FLUID_IGNITION_MAX_SPREAD.get();
        for (Direction side : SPREAD_DIRECTIONS) {
            BlockPos neighbor = pos.relative(side);
            if (isUnlitFlammable(level.getFluidState(neighbor))) {
                queueIgnition(level, neighbor, maxSpread);
            }
        }
    }

    // Clears static state when the server stops.
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        BURNING_FLUID_POSITIONS.clear();
        IGNITION_TIME.clear();
        PENDING.clear();
        PRIORITY_PENDING.clear();
        PENDING_POSITIONS.clear();
        REMOVAL_FRONTIER.clear();
        ACTIVELY_CLEARING.clear();
        PROACTIVE_CHECK_QUEUE.clear();
        CHUNK_LOAD_DISCOVERIES.clear();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        BurningFluid discovered;
        while ((discovered = CHUNK_LOAD_DISCOVERIES.poll()) != null) {
            if (BURNING_FLUID_POSITIONS.add(discovered)) {
                IGNITION_TIME.put(discovered, discovered.level().getGameTime());
            }
        }

        int maxPerTick = TFMGTweaksConfig.FLUID_IGNITION_SPREAD_PER_TICK.get();
        processIgnitionQueue(PRIORITY_PENDING, maxPerTick);
        processIgnitionQueue(PENDING, maxPerTick);

        int maxSpread = TFMGTweaksConfig.FLUID_IGNITION_MAX_SPREAD.get();

        List<BurningFluid> newlyFound = new ArrayList<>();
        Iterator<BurningFluid> it = BURNING_FLUID_POSITIONS.iterator();
        while (it.hasNext()) {
            BurningFluid burning = it.next();
            ServerLevel level = burning.level();
            BlockPos fluidPos = burning.fluidPos();
            FluidState here = level.getFluidState(fluidPos);
            if (!isBurningFuel(here)) {
                it.remove();
                IGNITION_TIME.remove(burning);
                if (isUnlitFlammable(here)) {
                    queueIgnition(level, fluidPos, maxSpread);
                }
                for (Direction side : SPREAD_DIRECTIONS) {
                    BlockPos neighbor = fluidPos.relative(side);
                    if (isUnlitFlammable(level.getFluidState(neighbor))) {
                        queueIgnition(level, neighbor, maxSpread);
                    }
                }
                continue;
            }

            for (Direction side : SPREAD_DIRECTIONS) {
                BlockPos neighbor = fluidPos.relative(side);
                BurningFluid neighborKey = new BurningFluid(level, neighbor);
                if (BURNING_FLUID_POSITIONS.contains(neighborKey)) {
                    continue;
                }
                if (isBurningFuel(level.getFluidState(neighbor))) {
                    newlyFound.add(neighborKey);
                }
            }
        }
        for (BurningFluid found : newlyFound) {
            track(found.level(), found.fluidPos());
        }

        int removalPerTick = TFMGTweaksConfig.FLUID_IGNITION_REMOVAL_PER_TICK.get();
        int removed = 0;
        while (removed < removalPerTick && !REMOVAL_FRONTIER.isEmpty()) {
            PendingRemoval next = REMOVAL_FRONTIER.poll();
            FluidState currentState = next.level().getFluidState(next.pos());
            if (!isBurningFuel(currentState)) {
                continue;
            }
            if (currentState.isSource()) {
                continue;
            }
            int radius = TFMGTweaksConfig.FLUID_IGNITION_SOURCE_SUPPORT_RADIUS.get();
            if (isConnectedToSource(next.level(), next.pos(), radius)) {
                continue;
            }
            next.level().setBlockAndUpdate(next.pos(), Blocks.AIR.defaultBlockState());
            BURNING_FLUID_POSITIONS.remove(new BurningFluid(next.level(), next.pos()));
            IGNITION_TIME.remove(new BurningFluid(next.level(), next.pos()));
            ACTIVELY_CLEARING.add(new BurningFluid(next.level(), next.pos()));
            for (Direction side : SPREAD_DIRECTIONS) {
                REMOVAL_FRONTIER.add(new PendingRemoval(next.level(), next.pos().relative(side)));
            }
            removed++;
        }
        if (REMOVAL_FRONTIER.isEmpty()) {
            ACTIVELY_CLEARING.clear();
        }

        int proactiveBudget = TFMGTweaksConfig.FLUID_IGNITION_PROACTIVE_CHECK_PER_TICK.get();
        int proactiveChecked = 0;
        while (proactiveChecked < proactiveBudget) {
            if (PROACTIVE_CHECK_QUEUE.isEmpty()) {
                if (BURNING_FLUID_POSITIONS.isEmpty()) {
                    break;
                }
                PROACTIVE_CHECK_QUEUE.addAll(BURNING_FLUID_POSITIONS);
            }
            BurningFluid next = PROACTIVE_CHECK_QUEUE.poll();
            if (next == null) {
                break;
            }
            checkSourceSupport(next.level(), next.fluidPos());
            proactiveChecked++;
        }
    }

    // Stale entries are discarded without using up the per-tick budget.
    private static void processIgnitionQueue(Deque<PendingIgnition> queue, int maxPerTick) {
        int processed = 0;
        int examined = 0;
        int maxExamined = maxPerTick * 64;
        while (processed < maxPerTick && examined < maxExamined && !queue.isEmpty()) {
            PendingIgnition next = queue.poll();
            PENDING_POSITIONS.remove(new BurningFluid(next.level(), next.pos()));
            examined++;
            if (!isUnlitFlammable(next.level().getFluidState(next.pos()))) {
                if (isBurningFuel(next.level().getFluidState(next.pos()))) {
                    track(next.level(), next.pos());
                }
                continue;
            }
            ignite(next.level(), next.pos(), next.remainingBudget());
            processed++;
        }
    }

    private static void ignite(ServerLevel level, BlockPos pos, int remainingBudget) {
        if (remainingBudget <= 0) {
            return;
        }
        if (isActivelyClearing(level, pos)) {
            return;
        }
        FluidState fluidState = level.getFluidState(pos);
        if (isBurningFuel(fluidState)) {
            track(level, pos);
            return;
        }
        if (!isFlammable(fluidState)) {
            return;
        }
        if (!markBurning(level, pos)) {
            return;
        }

        int nextBudget = remainingBudget - 1;
        if (nextBudget <= 0) {
            return;
        }
        for (Direction side : SPREAD_DIRECTIONS) {
            BlockPos neighborPos = pos.relative(side);
            FluidState neighborFluid = level.getFluidState(neighborPos);
            if (isUnlitFlammable(neighborFluid)) {
                queueIgnition(level, neighborPos, nextBudget);
            }
        }
    }
}
