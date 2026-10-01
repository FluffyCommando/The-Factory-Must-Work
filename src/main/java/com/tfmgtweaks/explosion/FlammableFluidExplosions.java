package com.tfmgtweaks.explosion;

import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.tfmgtweaks.TFMGTweaks;
import com.tfmgtweaks.advancement.TFMGTweaksTriggers;
import com.tfmgtweaks.compat.TFMGTagKeys;
import com.tfmgtweaks.config.TFMGTweaksConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/** Makes tanks holding flammable fluid explode when blown up, spilling fluid, fire debris and follow-up blasts. */
@EventBusSubscriber(modid = TFMGTweaks.MOD_ID)
public class FlammableFluidExplosions {
    private record PendingFuelExplosion(ServerLevel level, BlockPos pos, int flammableAmountMb, Fluid fluid,
                                         List<BlockPos> tankBlocks, IFluidHandler handler) {
    }

    private static final Deque<PendingFuelExplosion> PENDING = new ArrayDeque<>();

    /** Every fluid handler currently queued, so connected tanks aren't queued repeatedly. */
    private static final Set<IFluidHandler> PENDING_HANDLERS =
            Collections.newSetFromMap(new IdentityHashMap<>());

    /** Hard cap on queue size. */
    private static final int MAX_PENDING_EXPLOSIONS = 500;

    /** Secondary explosions at random nearby spots after the main blast. */
    private record PendingSecondaryExplosion(ServerLevel level, BlockPos pos, double power, long triggerAtGameTime) {
    }

    private static final List<PendingSecondaryExplosion> PENDING_SECONDARY = new ArrayList<>();

    /** Caps on tracked falling entities. */
    private static final int MAX_TRACKED_DEBRIS_AGE_TICKS = 200;

    private static final Set<FallingBlockEntity> TRACKED_FIRE_ENTITIES =
            Collections.newSetFromMap(new IdentityHashMap<>());

    private record TrackedFluid(FallingBlockEntity entity, BlockState blockState) {
    }

    private static final Set<TrackedFluid> TRACKED_FLUID_ENTITIES =
            Collections.newSetFromMap(new IdentityHashMap<>());

    /** Total flammable fluid a handler holds and which fluid it is. */
    private record FlammableContents(int amountMb, Fluid fluid) {
    }

    private static FlammableContents scanFlammableContents(IFluidHandler handler) {
        int total = 0;
        Fluid fluid = null;
        for (int i = 0; i < handler.getTanks(); i++) {
            FluidStack stack = handler.getFluidInTank(i);
            if (!stack.isEmpty() && stack.getFluid().is(TFMGTagKeys.FLAMMABLE_FLUID)) {
                total += stack.getAmount();
                if (fluid == null) {
                    fluid = stack.getFluid();
                }
            }
        }
        return new FlammableContents(total, fluid);
    }

    /** Queues an explosion for a handler at pos holding enough flammable fluid, skipping ones already queued. */
    private static void queueIfFlammableTank(ServerLevel level, BlockPos pos,
            Set<IFluidHandler> alreadyQueuedHandlers, int minAmount) {
        if (PENDING.size() >= MAX_PENDING_EXPLOSIONS) {
            return;
        }
        IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
        if (handler == null || !alreadyQueuedHandlers.add(handler)) {
            return;
        }
        if (!PENDING_HANDLERS.add(handler)) {
            return;
        }
        FlammableContents contents = scanFlammableContents(handler);
        if (contents.amountMb() >= minAmount && contents.fluid() != null) {
            List<BlockPos> tankBlocks = collectFluidTankBlocks(level, pos);
            PENDING.add(new PendingFuelExplosion(level, pos.immutable(), contents.amountMb(), contents.fluid(),
                    tankBlocks, handler));
            awardTankExplodedToNearbyPlayers(level, pos);
        } else {
            // Releases the handler if it holds too little flammable fluid to explode.
            PENDING_HANDLERS.remove(handler);
        }
    }

    /** Awards the hidden "tank exploded" advancement to every player within 32 blocks. */
    private static void awardTankExplodedToNearbyPlayers(ServerLevel level, BlockPos pos) {
        AABB range = new AABB(pos).inflate(32);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, range)) {
            TFMGTweaksTriggers.TANK_EXPLODED.trigger(player);
        }
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (!TFMGTweaksConfig.FUEL_EXPLOSIONS_ENABLED.get()) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Difficulty difficulty = level.getDifficulty();
        if (difficulty == Difficulty.PEACEFUL) {
            return;
        }
        if (TFMGTweaksConfig.FUEL_EXPLOSIONS_REQUIRE_HARD_DIFFICULTY.get() && difficulty != Difficulty.HARD) {
            return;
        }

        int minAmount = TFMGTweaksConfig.FUEL_EXPLOSIONS_MIN_AMOUNT_MB.get();
        Set<IFluidHandler> alreadyQueuedHandlers = Collections.newSetFromMap(new IdentityHashMap<>());

        for (BlockPos pos : event.getAffectedBlocks()) {
            queueIfFlammableTank(level, pos, alreadyQueuedHandlers, minAmount);
        }
    }

    /** Every block position of a multiblock fluid tank, or an empty list for anything else. */
    private static List<BlockPos> collectFluidTankBlocks(ServerLevel level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof FluidTankBlockEntity tankBE)) {
            return List.of();
        }
        FluidTankBlockEntity controllerBE = tankBE.getControllerBE();
        if (controllerBE == null) {
            return List.of();
        }
        BlockPos controllerPos = controllerBE.getController();
        int width = controllerBE.getWidth();
        int height = controllerBE.getHeight();
        // Guards against corrupted tank dimensions.
        if (width <= 0 || height <= 0 || width > 32 || height > 32) {
            return List.of();
        }
        List<BlockPos> blocks = new ArrayList<>(width * height * width);
        for (int yOffset = 0; yOffset < height; yOffset++) {
            for (int xOffset = 0; xOffset < width; xOffset++) {
                for (int zOffset = 0; zOffset < width; zOffset++) {
                    blocks.add(controllerPos.offset(xOffset, yOffset, zOffset));
                }
            }
        }
        return blocks;
    }

    /** Clears static state when the server stops. */

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        PENDING.clear();
        PENDING_HANDLERS.clear();
        PENDING_SECONDARY.clear();
        TRACKED_FIRE_ENTITIES.clear();
        TRACKED_FLUID_ENTITIES.clear();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        while (!PENDING.isEmpty()) {
            PendingFuelExplosion next = PENDING.poll();
            PENDING_HANDLERS.remove(next.handler());
            trigger(next.level(), next.pos(), next.flammableAmountMb(), next.fluid(), next.tankBlocks());
        }

        Iterator<PendingSecondaryExplosion> secondaryIt = PENDING_SECONDARY.iterator();
        while (secondaryIt.hasNext()) {
            PendingSecondaryExplosion secondary = secondaryIt.next();
            if (secondary.level().getGameTime() >= secondary.triggerAtGameTime()) {
                secondaryIt.remove();
                spawnFireDebris(secondary.level(), secondary.pos(),
                        TFMGTweaksConfig.FUEL_EXPLOSIONS_SECONDARY_FALLING_FIRE_COUNT.get());
                Level.ExplosionInteraction secondaryInteraction =
                        TFMGTweaksConfig.FUEL_EXPLOSIONS_SURROUNDING_BLOCKS_DAMAGED.get()
                                ? Level.ExplosionInteraction.BLOCK
                                : Level.ExplosionInteraction.NONE;
                secondary.level().explode(null,
                        secondary.pos().getX() + 0.5, secondary.pos().getY() + 0.5, secondary.pos().getZ() + 0.5,
                        (float) secondary.power(), secondaryInteraction);
            }
        }

        Iterator<FallingBlockEntity> fireIt = TRACKED_FIRE_ENTITIES.iterator();
        while (fireIt.hasNext()) {
            FallingBlockEntity fireEntity = fireIt.next();
            if (!fireEntity.isAlive() || fireEntity.tickCount > MAX_TRACKED_DEBRIS_AGE_TICKS) {
                // Stops tracking fire debris once it lands or has fallen too long.
                fireIt.remove();
                continue;
            }
            if (!(fireEntity.level() instanceof ServerLevel level)) {
                fireIt.remove();
                continue;
            }
            applyDrag(fireEntity, TFMGTweaksConfig.FUEL_EXPLOSIONS_FALLING_DRAG.get());
            BlockPos currentPos = fireEntity.blockPosition();
            if (!isFlammable(level.getFluidState(currentPos))) {
                continue;
            }
            // Fire debris landing in flammable fluid ignites it.
            fireIt.remove();
            fireEntity.discard();
            FluidIgnition.markBurning(level, currentPos);
        }

        Iterator<TrackedFluid> fluidIt = TRACKED_FLUID_ENTITIES.iterator();
        while (fluidIt.hasNext()) {
            TrackedFluid tracked = fluidIt.next();
            FallingBlockEntity fluidEntity = tracked.entity();
            if (!fluidEntity.isAlive()) {
                // Ignites the landing position.
                if (fluidEntity.level() instanceof ServerLevel level) {
                    FluidIgnition.markBurning(level, fluidEntity.blockPosition());
                }
                fluidIt.remove();
                continue;
            }
            if (fluidEntity.tickCount > MAX_TRACKED_DEBRIS_AGE_TICKS) {
                // Stops tracking falling fluid once it lands or has fallen too long.
                fluidIt.remove();
                continue;
            }
            if (!(fluidEntity.level() instanceof ServerLevel level)) {
                fluidIt.remove();
                continue;
            }
            applyDrag(fluidEntity, TFMGTweaksConfig.FUEL_EXPLOSIONS_FALLING_FLUID_DRAG.get());
            spawnFluidTrailParticles(level, fluidEntity, tracked.blockState());
        }
    }

    /** Falling fluid blocks render invisibly, so particles are spawned to show them. */
    private static void spawnFluidTrailParticles(ServerLevel level, FallingBlockEntity fluidEntity, BlockState blockState) {
        double x = fluidEntity.getX();
        double y = fluidEntity.getY() + 0.5;
        double z = fluidEntity.getZ();
        level.sendParticles(ParticleTypes.SPLASH, x, y, z, 3, 0.2, 0.2, 0.2, 0.01);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, blockState),
                x, y, z, 3, 0.2, 0.2, 0.2, 0.01);
    }

    /** Extra horizontal drag on top of normal falling-block physics. */
    private static void applyDrag(FallingBlockEntity entity, double drag) {
        if (drag >= 1.0) {
            return;
        }
        Vec3 motion = entity.getDeltaMovement();
        entity.setDeltaMovement(motion.x * drag, motion.y, motion.z * drag);
    }

    private static boolean isFlammable(FluidState fluidState) {
        return !fluidState.isEmpty() && fluidState.getType().is(TFMGTagKeys.FLAMMABLE_FLUID);
    }

    private static void trigger(ServerLevel level, BlockPos pos, int flammableAmountMb, Fluid fluid,
                                 List<BlockPos> tankBlocks) {
        // Spawns one falling fluid entity per 1000 mB in the tank, up to the configured maximum.
        BlockState fluidBlockState = fluid.defaultFluidState().createLegacyBlock();
        if (!fluidBlockState.isAir()) {
            int maxFluidEntities = TFMGTweaksConfig.FUEL_EXPLOSIONS_MAX_FLUID_SPILL_COUNT.get();
            int fluidEntityCount = Math.min(maxFluidEntities, Math.max(1, flammableAmountMb / 1000));
            RandomSource fluidRandom = level.getRandom();
            for (int i = 0; i < fluidEntityCount; i++) {
                
                BlockPos fluidSpawnPos = pos.offset(fluidRandom.nextInt(3) - 1, 0, fluidRandom.nextInt(3) - 1);
                if (!level.getBlockState(fluidSpawnPos).isAir()) {
                    continue;
                }
                FallingBlockEntity fluidEntity = FallingBlockEntity.fall(level, fluidSpawnPos, fluidBlockState);
                if (fluidEntity != null) {
                    TRACKED_FLUID_ENTITIES.add(new TrackedFluid(fluidEntity, fluidBlockState));
                }
            }
        }

        int fallingFireCount = TFMGTweaksConfig.FUEL_EXPLOSIONS_FALLING_FIRE_COUNT.get();
        spawnFireDebris(level, pos, fallingFireCount);

        double power = Math.min(
                (flammableAmountMb / 1000.0) * TFMGTweaksConfig.FUEL_EXPLOSIONS_POWER_PER_BUCKET.get(),
                TFMGTweaksConfig.FUEL_EXPLOSIONS_MAX_POWER.get());

        
        // Destroys the whole tank multiblock regardless of the surroundingBlocksDamaged setting.
        for (BlockPos tankPos : tankBlocks) {
            if (tankPos.equals(pos) || level.getBlockState(tankPos).isAir()) {
                continue;
            }
            level.destroyBlock(tankPos, false);
        }
        // Chain-reacts to nearby tanks.
        chainToNearbyTanks(level, pos, tankBlocks);

        Level.ExplosionInteraction interaction = TFMGTweaksConfig.FUEL_EXPLOSIONS_SURROUNDING_BLOCKS_DAMAGED.get()
                ? Level.ExplosionInteraction.BLOCK
                : Level.ExplosionInteraction.NONE;
        level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                (float) power, interaction);

        int secondaryCount = TFMGTweaksConfig.FUEL_EXPLOSIONS_SECONDARY_COUNT.get();
        if (secondaryCount > 0) {
            int radius = TFMGTweaksConfig.FUEL_EXPLOSIONS_SECONDARY_RADIUS.get();
            double secondaryPower = TFMGTweaksConfig.FUEL_EXPLOSIONS_SECONDARY_POWER.get();
            int maxDelay = TFMGTweaksConfig.FUEL_EXPLOSIONS_SECONDARY_MAX_DELAY_TICKS.get();
            RandomSource secondaryRandom = level.getRandom();
            for (int i = 0; i < secondaryCount; i++) {
                int dx = secondaryRandom.nextInt(radius * 2 + 1) - radius;
                int dy = secondaryRandom.nextInt(radius * 2 + 1) - radius;
                int dz = secondaryRandom.nextInt(radius * 2 + 1) - radius;
                BlockPos secondaryPos = pos.offset(dx, dy, dz);
                long delay = secondaryRandom.nextInt(maxDelay + 1);
                PENDING_SECONDARY.add(new PendingSecondaryExplosion(
                        level, secondaryPos, secondaryPower, level.getGameTime() + delay));
            }
        }

        igniteAround(level, pos);
    }

    /** Spawns falling fire debris for primary and secondary tank explosions. */
    private static void spawnFireDebris(ServerLevel level, BlockPos pos, int count) {
        RandomSource random = level.getRandom();
        for (int i = 0; i < count; i++) {
            BlockPos firePos = pos.offset(random.nextInt(3) - 1, 0, random.nextInt(3) - 1);
            if (level.getBlockState(firePos).isAir()) {
                FallingBlockEntity fireEntity = FallingBlockEntity.fall(level, firePos, BaseFireBlock.getState(level, firePos));
                TRACKED_FIRE_ENTITIES.add(fireEntity);
            }
        }
    }

    /** Queues chain-reaction explosions for nearby tanks holding enough flammable fluid. */
    private static void chainToNearbyTanks(ServerLevel level, BlockPos originPos, List<BlockPos> tankBlocks) {
        int radius = TFMGTweaksConfig.FUEL_EXPLOSIONS_CHAIN_RADIUS.get();
        if (radius <= 0) {
            return;
        }

        int minX;
        int minY;
        int minZ;
        int maxX;
        int maxY;
        int maxZ;
        if (tankBlocks.isEmpty()) {
            minX = maxX = originPos.getX();
            minY = maxY = originPos.getY();
            minZ = maxZ = originPos.getZ();
        } else {
            minX = Integer.MAX_VALUE;
            minY = Integer.MAX_VALUE;
            minZ = Integer.MAX_VALUE;
            maxX = Integer.MIN_VALUE;
            maxY = Integer.MIN_VALUE;
            maxZ = Integer.MIN_VALUE;
            for (BlockPos tankPos : tankBlocks) {
                minX = Math.min(minX, tankPos.getX());
                minY = Math.min(minY, tankPos.getY());
                minZ = Math.min(minZ, tankPos.getZ());
                maxX = Math.max(maxX, tankPos.getX());
                maxY = Math.max(maxY, tankPos.getY());
                maxZ = Math.max(maxZ, tankPos.getZ());
            }
        }
        minX -= radius;
        minY -= radius;
        minZ -= radius;
        maxX += radius;
        maxY += radius;
        maxZ += radius;

        int minAmount = TFMGTweaksConfig.FUEL_EXPLOSIONS_MIN_AMOUNT_MB.get();
        Set<IFluidHandler> alreadyQueuedHandlers = Collections.newSetFromMap(new IdentityHashMap<>());
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    cursor.set(x, y, z);
                    queueIfFlammableTank(level, cursor, alreadyQueuedHandlers, minAmount);
                }
            }
        }
    }

    private static void igniteAround(ServerLevel level, BlockPos center) {
        int radius = TFMGTweaksConfig.FUEL_EXPLOSIONS_FIRE_RADIUS.get();
        double chance = TFMGTweaksConfig.FUEL_EXPLOSIONS_FIRE_CHANCE.get();
        if (radius <= 0 || chance <= 0) {
            return;
        }

        RandomSource random = level.getRandom();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int radiusSq = radius * radius;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz > radiusSq) {
                        continue;
                    }
                    if (random.nextDouble() > chance) {
                        continue;
                    }
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    tryIgnite(level, cursor);
                }
            }
        }
    }

    private static void tryIgnite(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.isAir()) {
            return;
        }
        FluidState fluidState = level.getFluidState(pos);
        if (!fluidState.isEmpty()) {
            return;
        }
        if (!BaseFireBlock.canBePlacedAt(level, pos, Direction.UP)) {
            return;
        }
        BlockState fireState = BaseFireBlock.getState(level, pos);
        level.setBlock(pos, fireState, Block.UPDATE_CLIENTS);
    }
}
