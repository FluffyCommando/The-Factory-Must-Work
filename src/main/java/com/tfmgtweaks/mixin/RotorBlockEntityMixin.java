package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.electricity.generators.large_generator.RotorBlockEntity;
import com.tfmgtweaks.sound.RotorSoundClientHelper;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Plays the restored rotor hum as a looping sound that follows rotor speed. */
@Mixin(RotorBlockEntity.class)
public abstract class RotorBlockEntityMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void tfmgtweaks$playGeneratorHum(CallbackInfo ci) {
        RotorBlockEntity self = (RotorBlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level == null || !level.isClientSide) {
            return;
        }
        RotorSoundClientHelper.tick(self);
    }
}
