package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.electricity.network.large_switch.LargeSwitchBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Same crash shape as the one fixed for LargeTransformerBlockEntity.
 * resistance() guards with a broad IElectric check, then calls
 * getControlledBlock().getData() -- but getControlledBlock() does its own
 * stricter check and can return null. Since resistance() runs every
 * tick, this would crash-loop the same way the transformer did.
 *
 * Fix: bail out to 0 when getControlledBlock() is null.
 */
@Mixin(LargeSwitchBlockEntity.class)
public abstract class LargeSwitchBlockEntityMixin {

    @Inject(method = "resistance", at = @At("HEAD"), cancellable = true)
    private void tfmgtweaks$guardNullControlledBlock(CallbackInfoReturnable<Float> cir) {
        LargeSwitchBlockEntity self = (LargeSwitchBlockEntity) (Object) this;
        if (self.getControlledBlock() == null) {
            cir.setReturnValue(0f);
        }
    }
}
