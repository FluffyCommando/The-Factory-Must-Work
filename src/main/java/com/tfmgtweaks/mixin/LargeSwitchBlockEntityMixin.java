package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.electricity.network.large_switch.LargeSwitchBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Returns 0 resistance when there's no controlled block, fixing a crash loop. */
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
