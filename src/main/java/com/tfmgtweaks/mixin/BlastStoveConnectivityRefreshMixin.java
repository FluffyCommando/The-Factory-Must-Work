package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.machinery.metallurgy.blast_stove.BlastStoveBlockEntity;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Refreshes Blast Stove connectivity every lazyTick, so it works again after a server restart. */
@Mixin(FluidTankBlockEntity.class)
public abstract class BlastStoveConnectivityRefreshMixin {
    @Inject(method = "lazyTick", at = @At("HEAD"))
    private void tfmgtweaks$refreshBlastStoveConnectivity(CallbackInfo ci) {
        if ((Object) this instanceof BlastStoveBlockEntity blastStove) {
            blastStove.updateConnectivity = true;
        }
    }
}
