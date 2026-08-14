package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.decoration.tanks.steel.SteelTankBlockEntity;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Breaking part of a
 * multi-block steel fluid tank while Create goggles are active can leave a
 * segment's `controller` pointer stale, so {@code getControllerBE()}
 * returns null. The original addToGoggleTooltip then calls
 * {@code .getBlockPos()} on that null result without checking, crashing
 * the client.
 *
 * This just adds the missing null check: if the controller segment is gone,
 * there's nothing sensible to show in the goggle overlay, so skip the
 * tooltip instead of crashing.
 */
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
