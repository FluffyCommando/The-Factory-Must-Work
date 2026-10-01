package com.tfmgtweaks.mixin;

import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Falls back to the unshifted UV when Create's connected-texture target is missing, instead of throwing. */
@Mixin(CTSpriteShiftEntry.class)
public abstract class CTSpriteShiftEntryNullTargetFixMixin {
    @Inject(method = "getTargetU", at = @At("HEAD"), cancellable = true)
    private void tfmgtweaks$fallBackWhenTargetMissingU(float localU, int index, CallbackInfoReturnable<Float> cir) {
        CTSpriteShiftEntry self = (CTSpriteShiftEntry) (Object) this;
        if (self.getTarget() == null) {
            cir.setReturnValue(localU);
        }
    }

    @Inject(method = "getTargetV", at = @At("HEAD"), cancellable = true)
    private void tfmgtweaks$fallBackWhenTargetMissingV(float localV, int index, CallbackInfoReturnable<Float> cir) {
        CTSpriteShiftEntry self = (CTSpriteShiftEntry) (Object) this;
        if (self.getTarget() == null) {
            cir.setReturnValue(localV);
        }
    }
}
