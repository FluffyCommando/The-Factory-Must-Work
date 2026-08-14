package com.tfmgtweaks.compat;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

/**
 * Deliberately isolated in its own class. SableCompanion.INSTANCE throws
 * on class-init if no Sable implementation is registered, so this class
 * must never be referenced unless Sable is confirmed loaded first --
 * SurfaceScannerBlockEntityMixin only calls in after checking
 * ModList.get().isLoaded("sable").
 *
 * Built against "Sable Companion," a small, stable API meant for optional
 * compatibility (as opposed to Sable's own main mod, which is explicitly
 * documented as mixin-heavy and intrusive).
 */
public class SableIntegration {

    // Cached per (context, tick) since the surface scanner calls this up
    // to 25 times per scan for the same block entity in the same tick.
    @Nullable
    private static BlockEntity cachedContext;
    private static long cachedTick = Long.MIN_VALUE;
    @Nullable
    private static SubLevelAccess cachedSubLevel;

    /**
     * If `context` is currently part of an active Sable sub-level
     * (physics object), transforms `localPos` into its true world-space
     * position via the sub-level's current pose. Returns null if
     * `context` isn't on a physics object.
     */
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
