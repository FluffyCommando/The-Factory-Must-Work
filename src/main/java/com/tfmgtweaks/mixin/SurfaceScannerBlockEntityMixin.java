package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.base.TFMGUtils;
import com.drmangotea.tfmg.content.machinery.oil_processing.surface_scanner.SurfaceScannerBlockEntity;
import com.drmangotea.tfmg.integration.sable.SurfaceScannerSable;
import com.tfmgtweaks.content.oilrock.OilRockBlock;
import com.tfmgtweaks.registry.TFMGTweaksBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds Oil Rock to TFMG:CE's scanner, which only detects chunks with a FLUID_RESERVOIR. */
@Mixin(SurfaceScannerBlockEntity.class)
public abstract class SurfaceScannerBlockEntityMixin {
    @Unique
    private static final int TFMGTWEAKS$GRID_SIZE = 7;

    @Unique
    private static final int TFMGTWEAKS$GRID_OFFSET = TFMGTWEAKS$GRID_SIZE / 2;

    @Shadow
    public boolean[][] grid;

    @Shadow
    private BlockPos nearestDeposit;

    @Shadow
    private long lastScanTick;

    @Unique
    private long tfmgtweaks$seenOilRockChanges = -1;

    @Inject(method = "findDeposits", at = @At("TAIL"))
    private void tfmgtweaks$scanForOilRock(CallbackInfo ci) {
        SurfaceScannerBlockEntity self = (SurfaceScannerBlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level == null || grid == null || grid.length < TFMGTWEAKS$GRID_SIZE) {
            return;
        }
        BlockPos scannerPos = SurfaceScannerSable.getActualPosition(self);
        ChunkPos centerChunk = new ChunkPos(scannerPos);

        for (int x = 0; x < TFMGTWEAKS$GRID_SIZE; x++) {
            for (int z = 0; z < TFMGTWEAKS$GRID_SIZE; z++) {
                if (grid[x][z]) {
                    continue;
                }
                int chunkX = centerChunk.x + x - TFMGTWEAKS$GRID_OFFSET;
                int chunkZ = centerChunk.z + z - TFMGTWEAKS$GRID_OFFSET;
                if (!level.hasChunk(chunkX, chunkZ)) {
                    continue;
                }
                BlockPos midpoint = tfmgtweaks$scanChunkForOilRock(level, chunkX, chunkZ);
                if (midpoint == null) {
                    continue;
                }
                grid[x][z] = true;
                if (nearestDeposit == null) {
                    nearestDeposit = midpoint;
                } else if (!nearestDeposit.equals(midpoint)) {
                    float currentDistance = TFMGUtils.getDistance(scannerPos, nearestDeposit, true);
                    float newDistance = TFMGUtils.getDistance(scannerPos, midpoint, true);
                    if (newDistance < currentDistance) {
                        nearestDeposit = midpoint;
                    }
                }
            }
        }
    }

    /** Full-height scan of the chunk, skipping sections whose palette can't contain Oil Rock; null on a miss. */
    @Unique
    private static BlockPos tfmgtweaks$scanChunkForOilRock(Level level, int chunkX, int chunkZ) {
        Block oilRock = TFMGTweaksBlocks.OIL_ROCK.get();
        LevelChunk chunk = level.getChunk(chunkX, chunkZ);
        for (LevelChunkSection section : chunk.getSections()) {
            if (section == null || section.hasOnlyAir() || !section.getStates().maybeHas(state -> state.is(oilRock))) {
                continue;
            }
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        if (section.getBlockState(x, y, z).is(oilRock)) {
                            return new ChunkPos(chunkX, chunkZ).getMiddleBlockPosition(0).north().west();
                        }
                    }
                }
            }
        }
        return null;
    }

    /** Forces a rescan when Oil Rock has been placed or removed since the last scan. */
    @Inject(method = "lazyTick", at = @At("HEAD"))
    private void tfmgtweaks$rescanAfterOilRockChange(CallbackInfo ci) {
        long changes = OilRockBlock.getChangeCount();
        if (changes != tfmgtweaks$seenOilRockChanges) {
            tfmgtweaks$seenOilRockChanges = changes;
            lastScanTick = Long.MIN_VALUE;
        }
    }
}
