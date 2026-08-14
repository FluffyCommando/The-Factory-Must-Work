package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.machinery.misc.firebox.FireboxBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A firebox crashes on every login attempt near it,
 * making the affected chunk effectively unplayable.
 *
 * Root cause: lazyTick() gets `controller` (can legitimately be null
 * right after a chunk loads, same multiblock-timing issue found
 * elsewhere this session), then calls canBurn(controller) unconditionally
 * -- which dereferences controller.exhuastTank with no null check on
 * controller itself. Recurs indefinitely once triggered from lazyTick().
 *
 * Fix: return false (the existing "can't burn" fallback) if controller
 * is null.
 */
@Mixin(FireboxBlockEntity.class)
public abstract class FireboxBlockEntityMixin {

    @Inject(method = "canBurn", at = @At("HEAD"), cancellable = true)
    private void tfmgtweaks$guardNullController(FireboxBlockEntity controller, CallbackInfoReturnable<Boolean> cir) {
        if (controller == null) {
            cir.setReturnValue(false);
        }
    }
}
