package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.machinery.metallurgy.blast_stove.BlastStoveBlockEntity;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Reported bug: a Blast Stove (and the machines piping into it, e.g. Air
 * Intake) stops functioning after a server restart or a period of
 * disuse, needing the upper block broken and replaced to work again.
 *
 * Same self-healing pattern already fixed for the distillation tower,
 * blast furnace, and vat this session: BlastStoveBlockEntity.tick()
 * checks `if (updateConnectivity) updateConnectivity();` every tick, but
 * the updateConnectivity flag is only ever set true from block-place /
 * neighbor-changed handlers -- never by a plain world load. If that flag
 * is stale on load, the multiblock's connectivity (and therefore its
 * exposed capability) never gets re-evaluated until something like
 * breaking and replacing a block triggers a fresh event.
 *
 * BlastStoveBlockEntity has no lazyTick() of its own -- it's purely
 * inherited from FluidTankBlockEntity (confirmed via source; compare
 * SteelTankBlockEntity, which does override it), so this mixes into
 * FluidTankBlockEntity directly (the actual declaring class) rather than
 * risking the same inherited-but-unoverridden-method failure that broke
 * an earlier mixin this session, scoped to Blast Stove specifically via
 * an instanceof check.
 */
@Mixin(FluidTankBlockEntity.class)
public abstract class BlastStoveConnectivityRefreshMixin {

    @Inject(method = "lazyTick", at = @At("HEAD"))
    private void tfmgtweaks$refreshBlastStoveConnectivity(CallbackInfo ci) {
        if ((Object) this instanceof BlastStoveBlockEntity blastStove) {
            blastStove.updateConnectivity = true;
        }
    }
}
