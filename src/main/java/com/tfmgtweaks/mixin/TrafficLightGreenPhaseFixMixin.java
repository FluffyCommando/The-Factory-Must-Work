package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.electricity.utilities.traffic_light.TrafficLightBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * TFMG's own TrafficLightBlockEntity.tick() picks the light color with:
 *
 *   int halfTimer = timerLength.getValue() / 2;
 *   if (timer < halfTimer - 30 && timer > 60) light = 0;      // green
 *   else if (timer > halfTimer + 30) light = 2;                // red
 *   else light = 1;                                            // yellow
 *
 * Both transition widths (30 for the yellow-before-green window, 60 for
 * the yellow-before-reset window) are fixed tick counts, never scaled to
 * the actual configured timer length. At TFMG's own default/minimum
 * timer setting (180 ticks, from timerLength's own .between(180, ...)),
 * halfTimer is 90, so halfTimer-30 is exactly 60 -- the green condition
 * becomes `timer < 60 && timer > 60`, which no integer can ever satisfy.
 * Green is mathematically unreachable at the default setting, and only
 * gets a vanishingly thin window even one or two timer steps above it,
 * confirmed directly by bytecode comparison against a compiled Create
 * Edition jar showing the identical branch structure.
 *
 * Fix: instead of touching the buggy calculation itself (which would
 * mean retranscribing the whole method, since it's woven through timer
 * countdown, the glow animation, and the reset check), this recomputes
 * `light` correctly after the original tick() has already run, using
 * transition widths clamped to a safe fraction of the half-cycle rather
 * than the fixed 30/60. For any timer length large enough that the
 * clamp never engages (the normal, non-buggy case this session has
 * generally seen with larger timers), this produces the exact same
 * result the original logic already gave -- it only changes behavior
 * for the short timer lengths where the original math was broken.
 */
@Mixin(TrafficLightBlockEntity.class)
public abstract class TrafficLightGreenPhaseFixMixin {

    @Shadow
    protected ScrollValueBehaviour timerLength;

    @Shadow
    int light;

    @Inject(method = "tick", at = @At("TAIL"))
    private void tfmgtweaks$fixGreenPhase(CallbackInfo ci) {
        TrafficLightBlockEntity self = (TrafficLightBlockEntity) (Object) this;
        if (self.getLevel() == null || !self.getLevel().isClientSide) {
            return;
        }

        int halfTimer = timerLength.getValue() / 2;
        int transitionWindow = Math.min(30, halfTimer / 4);
        int finalTransitionWindow = Math.min(60, halfTimer / 4);

        if (self.timer < halfTimer - transitionWindow && self.timer > finalTransitionWindow) {
            light = 0;
        } else if (self.timer > halfTimer + transitionWindow) {
            light = 2;
        } else {
            light = 1;
        }
    }
}
