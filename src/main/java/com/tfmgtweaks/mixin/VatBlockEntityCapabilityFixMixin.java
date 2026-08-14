package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.machinery.vat.base.VatBlockEntity;
import com.tfmgtweaks.vat.VatInputOnlyFluidWrapper;
import com.tfmgtweaks.vat.VatInputOnlyItemWrapper;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Chemical vats: a full input side effectively blocks output.
 *
 * Root cause: VatBlockEntity exposes its item/fluid capability as a plain
 * combined wrapper over input+output with no concept of which side
 * external insertion should go into. Once input fills up, an external
 * hopper/pump can spill directly into the output side, so recipe
 * completion (which needs an empty output slot) finds none and
 * production silently stalls.
 *
 * Fix: two wrapper classes (VatInputOnlyItemWrapper,
 * VatInputOnlyFluidWrapper) that delegate everything to the real combined
 * wrapper except insert/fill, which they route to the input side only.
 * Only applied on the controller branch.
 *
 * tfmgtweaks$invalidateCapabilitiesOnTick fixes a separate, related bug:
 * VatBlockEntity only calls invalidateCapabilities() from
 * refreshCapability() (structural changes only) -- handleRecipe(), which
 * actually writes new output, never calls it. A pipe that cached "nothing
 * here" right after the vat formed never learns output became available.
 * Runs unconditionally at HEAD of tick() rather than injecting into
 * handleRecipe() (which has multiple early returns that would risk a
 * silently-skipped TAIL injection).
 */
@Mixin(VatBlockEntity.class)
public abstract class VatBlockEntityCapabilityFixMixin {

    @Inject(method = "getNewItemCapability", at = @At("RETURN"), cancellable = true)
    private void tfmgtweaks$restrictItemInsertToInput(CallbackInfoReturnable<IItemHandlerModifiable> cir) {
        VatBlockEntity self = (VatBlockEntity) (Object) this;
        if (self.isController()) {
            cir.setReturnValue(new VatInputOnlyItemWrapper(self.inputInventory, self.outputInventory));
        }
    }

    @Inject(method = "getNewFluidCapability", at = @At("RETURN"), cancellable = true)
    private void tfmgtweaks$restrictFluidFillToInput(CallbackInfoReturnable<IFluidHandler> cir) {
        VatBlockEntity self = (VatBlockEntity) (Object) this;
        if (!self.isController()) {
            return;
        }
        IFluidHandler inputHandler = self.inputTank.getCapability();
        IFluidHandler outputHandler = self.outputTank.getCapability();
        if (inputHandler == null || outputHandler == null) {
            return;
        }
        cir.setReturnValue(new VatInputOnlyFluidWrapper(inputHandler, outputHandler));
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tfmgtweaks$invalidateCapabilitiesOnTick(CallbackInfo ci) {
        VatBlockEntity self = (VatBlockEntity) (Object) this;
        if (self.isController() && self.getLevel() != null) {
            self.getLevel().invalidateCapabilities(self.getBlockPos());
        }
    }
}
