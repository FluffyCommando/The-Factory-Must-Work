package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.electricity.utilities.traffic_light.TrafficLightBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Scales the light's transition widths to the timer length, so green is reachable at short timers. */
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
