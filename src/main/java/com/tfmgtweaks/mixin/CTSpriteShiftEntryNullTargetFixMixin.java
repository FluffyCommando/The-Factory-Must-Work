package com.tfmgtweaks.mixin;

import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Reported symptom: hundreds of "CCL has caught an exception whilst
 * rendering a block" messages spamming chat, for a create:fluid_tank
 * being used as Create's own native Boiler. Not related to TFMG at all
 * -- CodeChickenLib (a rendering compatibility mod) is catching and
 * printing these instead of crashing, which is why the game keeps
 * running.
 *
 * Root cause: CTSpriteShiftEntry.getTargetU()/getTargetV() call
 * getTarget().getU(...)/getV(...) with no null check --
 * getTarget() (inherited from catnip's SpriteShiftEntry, which we don't
 * have source for, so the exact reason it's sometimes unpopulated isn't
 * pinned down here) can return null, throwing a NullPointerException
 * during chunk section rebuilds. This is Create's own connected-texture
 * sprite-shifting system, used by (among other things) its native
 * FluidTankModel -- nothing TFMG-specific, and nothing this mod's own
 * content touches.
 *
 * Fix: guard both methods and fall back to the original, unshifted UV
 * coordinate when the target sprite isn't available, rather than
 * crashing that block's render. The block loses its connected-texture
 * blending in that case (renders with its normal, unshifted texture
 * instead), but that's a minor visual gap compared to a render
 * exception on every affected chunk rebuild.
 */
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
