package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.electricity.network.transformer.large.LargeTransformerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Returns 0 resistance when there's no controlled block, fixing a crash loop. */
@Mixin(LargeTransformerBlockEntity.class)
public abstract class LargeTransformerBlockEntityMixin {
    @Inject(method = "resistance", at = @At("HEAD"), cancellable = true)
    private void tfmgtweaks$guardNullControlledBlock(CallbackInfoReturnable<Float> cir) {
        LargeTransformerBlockEntity self = (LargeTransformerBlockEntity) (Object) this;
        if (self.getControlledBlock() == null) {
            cir.setReturnValue(0f);
        }
    }
}
