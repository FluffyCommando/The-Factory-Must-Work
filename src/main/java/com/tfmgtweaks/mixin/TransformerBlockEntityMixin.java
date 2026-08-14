package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.electricity.network.transformer.small.TransformerBlockEntity;
import com.tfmgtweaks.TFMGTweaksSoundEvents;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * See GeneratorBlockEntityMixin for full context. Plays
 * while the transformer is actively passing power (getOutputPower() > 0)
 * rather than kinetic speed, since this transformer isn't kinetic-driven --
 * it converts voltage between two electric networks.
 */
@Mixin(TransformerBlockEntity.class)
public abstract class TransformerBlockEntityMixin {

    @Unique
    private int tfmgtweaks$soundTimer = 0;

    @Inject(method = "tick", at = @At("TAIL"))
    private void tfmgtweaks$playElectricHum(CallbackInfo ci) {
        TransformerBlockEntity self = (TransformerBlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level == null || !level.isClientSide) {
            return;
        }
        if (self.getOutputPower() <= 0) {
            tfmgtweaks$soundTimer = 0;
            return;
        }
        tfmgtweaks$soundTimer++;
        if (tfmgtweaks$soundTimer < 20) {
            return;
        }
        tfmgtweaks$soundTimer = 0;
        float randomPitch = (level.getRandom().nextFloat() - .5f) * 0.1f;
        TFMGTweaksSoundEvents.ELECTRIC_HUM.playAt(level, self.getBlockPos(), 0.15f, 1.0f + randomPitch, false);
    }
}
