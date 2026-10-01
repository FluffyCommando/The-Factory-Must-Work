package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.electricity.generators.GeneratorBlockEntity;
import com.tfmgtweaks.TFMGTweaksSoundEvents;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Plays the restored generator hum while the generator turns. */
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
