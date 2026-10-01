package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.config.TFMGConfigs;
import com.drmangotea.tfmg.content.machinery.oil_processing.surface_scanner.SurfaceScannerBlockEntity;
import com.tfmgtweaks.compat.SableIntegration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Also finds Oil Rock with a full-height scan of each chunk, and scans from a Sable sub-level's world position. */
@Mixin(SurfaceScannerBlockEntity.class)
public abstract class SurfaceScannerBlockEntityMixin {
    private static final TagKey<Block> SURFACE_SCANNER_FINDABLE_TAG = TagKey.create(
            Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("tfmg", "surface_scanner_findable"));

    @Inject(method = "hasOil", at = @At("HEAD"), cancellable = true)
    private void tfmgtweaks$scanFromSableSubLevel(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!ModList.get().isLoaded("sable")) {
            return;
        }
        SurfaceScannerBlockEntity self = (SurfaceScannerBlockEntity) (Object) this;
        BlockPos worldPos = SableIntegration.resolveWorldPosition(self, pos);
        if (worldPos == null) {
            return;
        }
        Level level = self.getLevel();
        if (level == null) {
            return;
        }
        // getLevel() is the real world on a Sable sub-level, so scan it at the transformed position.
        cir.setReturnValue(tfmgtweaks$scanBothRanges(level, worldPos));
    }

    @Inject(method = "hasOil", at = @At("TAIL"), cancellable = true)
    private void tfmgtweaks$scanOilRockRange(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        SurfaceScannerBlockEntity self = (SurfaceScannerBlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level == null) {
            return;
        }
        if (tfmgtweaks$scanOilRockHeightRange(level, pos)) {
            cir.setReturnValue(true);
        }
    }

    /** TFMG's own single-level check plus the Oil Rock scan, at an arbitrary position. */
    private boolean tfmgtweaks$scanBothRanges(Level level, BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos);
        if (!level.hasChunk(chunkPos.x, chunkPos.z)) {
            // Never force-load a chunk to scan it.
            return false;
        }
        ChunkAccess chunk = level.getChunk(pos);
        int scanDepth = TFMGConfigs.common().machines.surfaceScannerScanDepth.get();
        AABB originalArea = new AABB(chunk.getPos().getMiddleBlockPosition(scanDepth).north().west())
                .inflate(7, 0, 7);
        for (BlockState state : chunk.getBlockStates(originalArea).toList()) {
            if (state.is(SURFACE_SCANNER_FINDABLE_TAG)) {
                return true;
            }
        }
        return tfmgtweaks$scanOilRockHeightRange(level, pos);
    }

    /** Full-height scan of the chunk, skipping sections whose palette can't contain a findable block. */
    private boolean tfmgtweaks$scanOilRockHeightRange(Level level, BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos);
        if (!level.hasChunk(chunkPos.x, chunkPos.z)) {
            return false;
        }
        LevelChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);
        for (LevelChunkSection section : chunk.getSections()) {
            if (section == null || section.hasOnlyAir()
                    || !section.getStates().maybeHas(state -> state.is(SURFACE_SCANNER_FINDABLE_TAG))) {
                continue;
            }
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        if (section.getBlockState(x, y, z).is(SURFACE_SCANNER_FINDABLE_TAG)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
