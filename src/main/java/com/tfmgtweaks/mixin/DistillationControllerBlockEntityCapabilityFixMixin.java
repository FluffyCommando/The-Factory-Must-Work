package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.machinery.oil_processing.distillation_tower.controller.DistillationControllerBlockEntity;
import com.drmangotea.tfmg.content.machinery.oil_processing.distillation_tower.output.DistillationOutputBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

/**
 * Same bug pattern as CastingBasinBlockEntityCapabilityFixMixin. Directly
 * matches a reported bug: "Distillation Tower output stops --
 * distillation outputs occasionally stop pulling/pushing fluid until the
 * controller is broken+replaced or the multiblock reformed."
 *
 * manageRecipe() has two mutation points, neither invalidated:
 *  1. tank.drain() -- this controller's own input tank.
 *  2. output.tank.fill() -- for each DistillationOutputBlockEntity in
 *     getOutputs(), a separate block entity per output.
 *
 * Both are invalidated here, individually per output, since a pipe could
 * be connected to any one of them.
 */
@Mixin(DistillationControllerBlockEntity.class)
public abstract class DistillationControllerBlockEntityCapabilityFixMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void tfmgtweaks$invalidateCapabilitiesOnTick(CallbackInfo ci) {
        DistillationControllerBlockEntity self = (DistillationControllerBlockEntity) (Object) this;
        if (self.getLevel() == null) {
            return;
        }
        self.getLevel().invalidateCapabilities(self.getBlockPos());
        ArrayList<DistillationOutputBlockEntity> outputs = self.getOutputs();
        if (outputs != null) {
            for (DistillationOutputBlockEntity output : outputs) {
                self.getLevel().invalidateCapabilities(output.getBlockPos());
            }
        }
    }
}
