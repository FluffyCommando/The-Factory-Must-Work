package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.electricity.network.transformer.large.LargeTransformerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Connecting cables near a large transformer can crash
 * and permanently crash-loop a dedicated server.
 *
 * Root cause: resistance() guards with a broad IElectric check, then
 * calls getControlledBlock().getData() -- but getControlledBlock() does
 * its own stricter check and returns null if it doesn't match. Any other
 * electric block placed nearby satisfies the outer check but fails the
 * inner one, so getData() runs on null. Since resistance() runs every
 * tick, this crash-loops indefinitely once triggered.
 *
 * Fix: bail out to 0 (the existing fallback for other non-matching
 * paths) when getControlledBlock() is null.
 */
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
