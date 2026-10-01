package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.decoration.tanks.steel.SteelTankBlockEntity;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Skips the goggle tooltip when the tank's controller is gone, instead of crashing. */
@Mixin(SteelTankBlockEntity.class)
public abstract class SteelTankBlockEntityMixin {
    @Inject(method = "addToGoggleTooltip", at = @At("HEAD"), cancellable = true)
    private void tfmgtweaks$fixNullControllerTooltipCrash(List<Component> tooltip, boolean isPlayerSneaking,
                                                            CallbackInfoReturnable<Boolean> cir) {
        SteelTankBlockEntity self = (SteelTankBlockEntity) (Object) this;
        if (self.getControllerBE() == null) {
            cir.setReturnValue(false);
        }
    }
}
