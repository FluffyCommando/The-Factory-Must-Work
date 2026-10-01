package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.machinery.misc.firebox.FireboxBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Fixes a crash on chunk load: canBurn() is called with a controller that can be null. */
@Mixin(FireboxBlockEntity.class)
public abstract class FireboxBlockEntityMixin {
    @Inject(method = "canBurn", at = @At("HEAD"), cancellable = true)
    private void tfmgtweaks$guardNullController(FireboxBlockEntity controller, CallbackInfoReturnable<Boolean> cir) {
        if (controller == null) {
            cir.setReturnValue(false);
        }
    }
}
