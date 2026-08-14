package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.config.TFMGConfigs;
import com.drmangotea.tfmg.content.machinery.oil_processing.surface_scanner.SurfaceScannerBlockEntity;
import com.tfmgtweaks.api.ITFMGTweaksSurfaceScannerSignal;
import com.tfmgtweaks.compat.SableIntegration;
import com.tfmgtweaks.config.TFMGTweaksConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * TFMG's own SurfaceScannerBlockEntity re-scans every lazy tick (once a
 * second) while powered, unconditionally -- a visible stutter since the
 * scan itself only runs client-side.
 *
 * Redirects that call to only actually scan if
 * SURFACE_SCANNER_RESCAN_INTERVAL_TICKS has elapsed, or the scanner's
 * position has changed (accounting for Sable physics objects, whose
 * real-world position can move while getBlockPos() stays fixed).
 *
 * Also implements ITFMGTweaksSurfaceScannerSignal (read by
 * SurfaceScannerBlockMixin for redstone output), maintaining a separate
 * server-side scan grid since TFMG's own `grid` field is only populated
 * client-side.
 */
@Mixin(SurfaceScannerBlockEntity.class)
public abstract class SurfaceScannerBlockEntityRescanThrottleMixin implements ITFMGTweaksSurfaceScannerSignal {

    @Unique
    private long tfmgtweaks$lastScanTick = Long.MIN_VALUE;

    @Unique
    private BlockPos tfmgtweaks$lastScanPos = null;

    @Unique
    private final boolean[][] tfmgtweaks$serverGrid = new boolean[5][5];

    @Redirect(method = "lazyTick", at = @At(value = "INVOKE",
            target = "Lcom/drmangotea/tfmg/content/machinery/oil_processing/surface_scanner/SurfaceScannerBlockEntity;findDeposits()V"))
    private void tfmgtweaks$throttledFindDeposits(SurfaceScannerBlockEntity self) {
        Level level = self.getLevel();
        BlockPos effectivePos = tfmgtweaks$getEffectivePosition(self);

        boolean moved = tfmgtweaks$lastScanPos == null || !tfmgtweaks$lastScanPos.equals(effectivePos);

        int intervalTicks = TFMGTweaksConfig.SURFACE_SCANNER_RESCAN_INTERVAL_TICKS.get();
        long currentTick = level != null ? level.getGameTime() : Long.MIN_VALUE;
        boolean intervalElapsed = tfmgtweaks$lastScanTick == Long.MIN_VALUE
                || (currentTick - tfmgtweaks$lastScanTick) >= intervalTicks;

        if (!moved && !intervalElapsed) {
            return;
        }

        self.findDeposits();
        if (level != null && !level.isClientSide) {
            tfmgtweaks$updateServerGrid(self, level);
        }

        tfmgtweaks$lastScanPos = effectivePos;
        tfmgtweaks$lastScanTick = currentTick;
    }

    @Unique
    private void tfmgtweaks$updateServerGrid(SurfaceScannerBlockEntity self, Level level) {
        BlockPos basePos = self.getBlockPos();
        int scanDepth = TFMGConfigs.common().machines.surfaceScannerScanDepth.get();
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                BlockPos checkPos = new BlockPos(
                        basePos.getX() + (x - 2) * 16, scanDepth, basePos.getZ() + (z - 2) * 16);
                tfmgtweaks$serverGrid[x][z] = self.hasOil(checkPos);
            }
        }
        // The signal output may have just changed -- let adjacent redstone
        // components (comparators, dust, etc.) know rather than waiting
        // for some unrelated neighbor update to notice.
        level.updateNeighborsAt(basePos, self.getBlockState().getBlock());
    }

    @Unique
    private BlockPos tfmgtweaks$getEffectivePosition(SurfaceScannerBlockEntity self) {
        if (ModList.get().isLoaded("sable")) {
            BlockPos worldPos = SableIntegration.resolveWorldPosition(self, self.getBlockPos());
            if (worldPos != null) {
                return worldPos;
            }
        }
        return self.getBlockPos();
    }

    /** Nearest detected deposit in this direction, mapped to signal strength -- closer is stronger. */
    @Override
    public int tfmgtweaks$getSignalForDirection(Direction direction) {
        int bestDistance = Integer.MAX_VALUE;
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                if (!tfmgtweaks$serverGrid[x][z]) {
                    continue;
                }
                int dx = x - 2;
                int dz = z - 2;
                Direction cellDirection = tfmgtweaks$dominantDirection(dx, dz);
                if (cellDirection != null && cellDirection != direction) {
                    continue;
                }
                int distance = Math.max(Math.abs(dx), Math.abs(dz));
                bestDistance = Math.min(bestDistance, distance);
            }
        }
        if (bestDistance == Integer.MAX_VALUE) {
            return 0;
        }
        return Math.max(1, 15 - bestDistance * 5);
    }

    @Unique
    private Direction tfmgtweaks$dominantDirection(int dx, int dz) {
        if (dx == 0 && dz == 0) {
            return null;
        }
        if (Math.abs(dx) >= Math.abs(dz)) {
            return dx > 0 ? Direction.EAST : Direction.WEST;
        }
        return dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }
}
