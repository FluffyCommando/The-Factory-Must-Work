package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.machinery.vat.base.VatBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Re-evaluates attached machines every lazyTick, so machines in late-loading chunks are found. */
@Mixin(VatBlockEntity.class)
public abstract class VatBlockEntityMixin {
    @Inject(method = "lazyTick", at = @At("HEAD"))
    private void tfmgtweaks$reevaluateOnLazyTick(CallbackInfo ci) {
        VatBlockEntity self = (VatBlockEntity) (Object) this;
        self.evaluate();
    }
}
