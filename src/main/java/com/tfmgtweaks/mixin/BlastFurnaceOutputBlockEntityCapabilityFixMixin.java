package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.machinery.metallurgy.blast_furnace.BlastFurnaceOutputBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Invalidates capabilities once, when an output tank first fills, so connected pipes can drain it. */
@Mixin(BlastFurnaceOutputBlockEntity.class)
public abstract class BlastFurnaceOutputBlockEntityCapabilityFixMixin {
    @Unique
    private boolean tfmgtweaks$hasEverHadOutput = false;

    @Inject(method = "tick", at = @At("HEAD"))
    private void tfmgtweaks$invalidateOnFirstOutput(CallbackInfo ci) {
        if (tfmgtweaks$hasEverHadOutput) {
            return;
        }
        BlastFurnaceOutputBlockEntity self = (BlastFurnaceOutputBlockEntity) (Object) this;
        if (self.getLevel() == null || self.getLevel().isClientSide) {
            return;
        }
        if (!self.primaryTank.getFluid().isEmpty() || !self.secondaryTank.getFluid().isEmpty()) {
            tfmgtweaks$hasEverHadOutput = true;
            self.getLevel().invalidateCapabilities(self.getBlockPos());
        }
    }
}
