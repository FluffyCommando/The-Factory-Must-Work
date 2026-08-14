package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.electricity.generators.large_generator.RotorBlockEntity;
import com.tfmgtweaks.TFMGTweaksSoundEvents;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * See GeneratorBlockEntityMixin for full context. Same
 * pattern applied to the large rotor/stator generator.
 */
@Mixin(RotorBlockEntity.class)
public abstract class RotorBlockEntityMixin {

    @Unique
    private int tfmgtweaks$soundTimer = 0;

    @Inject(method = "tick", at = @At("TAIL"))
    private void tfmgtweaks$playGeneratorHum(CallbackInfo ci) {
        RotorBlockEntity self = (RotorBlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level == null || !level.isClientSide) {
            return;
        }
        if (self.getSpeed() == 0) {
            tfmgtweaks$soundTimer = 0;
            return;
        }
        tfmgtweaks$soundTimer++;
        if (tfmgtweaks$soundTimer < 20) {
            return;
        }
        tfmgtweaks$soundTimer = 0;
        float randomPitch = (level.getRandom().nextFloat() - .5f) * 0.1f;
        TFMGTweaksSoundEvents.GENERATOR_HUM.playAt(level, self.getBlockPos(), 0.2f, 0.9f + randomPitch, false);
    }
}
