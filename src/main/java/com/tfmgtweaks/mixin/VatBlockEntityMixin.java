package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.machinery.vat.base.VatBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Reported: "Chemical Vat refuses recipes intermittently
 * -- sometimes a valid recipe just won't process... looks like a stale
 * recipe cache or input queue not invalidating."
 *
 * Root cause: evaluate() (scans for attached machines, builds
 * machineMap) is gated by a one-shot flag consumed on the first tick --
 * if that first call runs before every neighboring machine's chunk is
 * loaded (a real server timing difference), machineMap permanently
 * misses whatever wasn't loaded yet, which can make an otherwise-valid
 * recipe appear unrecognized. Same pattern already found and fixed for
 * the distillation tower, blast furnace, and pump jack.
 *
 * Fix: call evaluate() periodically from lazyTick() instead of relying
 * solely on the one-shot flag.
 */
@Mixin(VatBlockEntity.class)
public abstract class VatBlockEntityMixin {

    @Inject(method = "lazyTick", at = @At("HEAD"))
    private void tfmgtweaks$reevaluateOnLazyTick(CallbackInfo ci) {
        VatBlockEntity self = (VatBlockEntity) (Object) this;
        self.evaluate();
    }
}
