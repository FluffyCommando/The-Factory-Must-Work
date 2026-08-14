package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.machinery.metallurgy.blast_furnace.BlastFurnaceOutputBlockEntity;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Same bug pattern as CastingBasinBlockEntityCapabilityFixMixin. This one
 * mutates tanks across two block entities, not just its own:
 *  1. primaryTank/secondaryTank.fill() -- this block's own tanks.
 *  2. tuyereBE.tank -- a separate BlastFurnaceHatchBlockEntity (tracked
 *     in the public tuyerePos field), mutated directly.
 *
 * Both are invalidated here. A third mutation point (a second hatch used
 * for gas byproduct, found via an on-the-fly position calculation rather
 * than a stored field) is NOT covered -- reproducing that formula safely
 * wasn't worth the risk for a narrower case.
 */
@Mixin(BlastFurnaceOutputBlockEntity.class)
public abstract class BlastFurnaceOutputBlockEntityCapabilityFixMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void tfmgtweaks$invalidateCapabilitiesOnTick(CallbackInfo ci) {
        BlastFurnaceOutputBlockEntity self = (BlastFurnaceOutputBlockEntity) (Object) this;
        if (self.getLevel() == null) {
            return;
        }
        self.getLevel().invalidateCapabilities(self.getBlockPos());
        BlockPos tuyerePos = self.tuyerePos;
        if (tuyerePos != null) {
            self.getLevel().invalidateCapabilities(tuyerePos);
        }
    }
}
