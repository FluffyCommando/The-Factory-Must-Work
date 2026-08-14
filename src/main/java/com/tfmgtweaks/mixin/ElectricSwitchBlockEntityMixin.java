package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.electricity.network.electric_switch.ElectricSwitchBlockEntity;
import com.tfmgtweaks.TFMGTweaksSoundEvents;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * See GeneratorBlockEntityMixin for full context.
 *
 * Unlike the hum sounds, this one is a discrete click on state change, not
 * a periodic loop -- analogSignalChanged(int) is called whenever the
 * switch's redstone signal changes, and "signal" (the previous value) is
 * still the OLD value at HEAD, since the method's first statement is what
 * overwrites it. Comparing old vs new here detects the actual on/off
 * transition. "signal" is package-private in the target class, so it's
 * shadowed here rather than accessed directly.
 */
@Mixin(ElectricSwitchBlockEntity.class)
public abstract class ElectricSwitchBlockEntityMixin {

    @Shadow
    private int signal;

    @Inject(method = "analogSignalChanged", at = @At("HEAD"))
    private void tfmgtweaks$playSwitchSound(int newSignal, CallbackInfo ci) {
        ElectricSwitchBlockEntity self = (ElectricSwitchBlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level == null || !level.isClientSide) {
            return;
        }
        boolean wasOn = signal > 0;
        boolean nowOn = newSignal > 0;
        if (wasOn == nowOn) {
            return;
        }
        if (nowOn) {
            TFMGTweaksSoundEvents.SWITCH_ON.playAt(level, self.getBlockPos(), 1.0f, 1.0f, false);
        } else {
            TFMGTweaksSoundEvents.SWITCH_OFF.playAt(level, self.getBlockPos(), 1.0f, 1.0f, false);
        }
    }
}
