package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.electricity.generators.GeneratorBlockEntity;
import com.tfmgtweaks.TFMGTweaksSoundEvents;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Rotor/stator generators, transformers, and electric
 * switches lost their ambient sound during TFMG's 1.0 -> 1.2 rewrite --
 * both the sound events and every playSound() call site were removed,
 * while engines kept theirs intact. This restores the generator hum for
 * the regular generator, using the recovered sound event (see
 * TFMGTweaksSoundEvents) and a periodic-trigger pattern matching how
 * RegularEngineBlockEntity still plays its own sound today.
 */
@Mixin(GeneratorBlockEntity.class)
public abstract class GeneratorBlockEntityMixin {

    @Unique
    private int tfmgtweaks$soundTimer = 0;

    @Inject(method = "tick", at = @At("TAIL"))
    private void tfmgtweaks$playGeneratorHum(CallbackInfo ci) {
        GeneratorBlockEntity self = (GeneratorBlockEntity) (Object) this;
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
        TFMGTweaksSoundEvents.GENERATOR_HUM.playAt(level, self.getBlockPos(), 0.15f, 1.0f + randomPitch, false);
    }
}
