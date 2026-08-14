package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.machinery.metallurgy.casting_basin.CastingBasinBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Same confirmed bug pattern found across a deep search of TFMG block
 * entities that both expose a fluid capability and mutate their own tank
 * internally without ever calling invalidateCapabilities().
 *
 * CastingBasinBlockEntity.tick() calls tank.setFluid(FluidStack.EMPTY)
 * when a recipe finishes, but never invalidates -- a pipe that cached
 * "tank full" never learns it emptied again, matching "casting basin
 * stops accepting input after the first recipe."
 *
 * Runs unconditionally at HEAD of tick() rather than TAIL, since tick()
 * has multiple early returns.
 */
@Mixin(CastingBasinBlockEntity.class)
public abstract class CastingBasinBlockEntityCapabilityFixMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void tfmgtweaks$invalidateCapabilitiesOnTick(CallbackInfo ci) {
        CastingBasinBlockEntity self = (CastingBasinBlockEntity) (Object) this;
        if (self.getLevel() != null) {
            self.getLevel().invalidateCapabilities(self.getBlockPos());
        }
    }
}
