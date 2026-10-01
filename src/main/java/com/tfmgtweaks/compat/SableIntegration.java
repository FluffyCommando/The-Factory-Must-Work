package com.tfmgtweaks.compat;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

/** Optional Sable support, built against the Sable Companion API. */
public class SableIntegration {
    // Cached per context and tick, since a single scan queries the same block entity many times.
    @Nullable
    private static BlockEntity cachedContext;
    private static long cachedTick = Long.MIN_VALUE;
    @Nullable
    private static SubLevelAccess cachedSubLevel;

    /** Transforms localPos into world space if context is on a Sable sub-level; null otherwise. */
    @Nullable
    public static BlockPos resolveWorldPosition(BlockEntity context, BlockPos localPos) {
        SubLevelAccess subLevel = getContainingCached(context);
        if (subLevel == null) {
            return null;
        }
        Pose3dc pose = subLevel.logicalPose();
        Vec3 localVec = new Vec3(localPos.getX() + 0.5, localPos.getY() + 0.5, localPos.getZ() + 0.5);
        Vec3 world = pose.transformPosition(localVec);
        return BlockPos.containing(world.x, world.y, world.z);
    }

    @Nullable
    private static SubLevelAccess getContainingCached(BlockEntity context) {
        Level level = context.getLevel();
        long tick = level != null ? level.getGameTime() : -1;
        if (context != cachedContext || tick != cachedTick) {
            cachedContext = context;
            cachedTick = tick;
            cachedSubLevel = SableCompanion.INSTANCE.getContaining(context);
        }
        return cachedSubLevel;
    }
}
