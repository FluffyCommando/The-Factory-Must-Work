package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.machinery.oil_processing.pumpjack.base.PumpjackBaseBlockEntity;
import com.drmangotea.tfmg.registry.TFMGFluids;
import com.tfmgtweaks.content.oilrock.OilRockBlockEntity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lets pump jacks extract from Oil Rock, which has no chunk FLUID_RESERVOIR for process() to check. */
@Mixin(PumpjackBaseBlockEntity.class)
public abstract class PumpjackBaseBlockEntityOilRockExtractionMixin {
    @Inject(method = "process", at = @At("HEAD"), cancellable = true)
    private void tfmgtweaks$extractFromOilRockDirectly(CallbackInfo ci) {
        PumpjackBaseBlockEntity self = (PumpjackBaseBlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level == null || self.deposit == null) {
            return;
        }
        if (!(level.getBlockEntity(self.deposit) instanceof OilRockBlockEntity)) {
            return;
        }

        ci.cancel();

        if (self.tank.getFluidAmount() + self.miningRate > self.tank.getCapacity()) {
            return;
        }
        int amountPumped = self.tank.forceFill(
                new FluidStack(TFMGFluids.CRUDE_OIL.get().getSource(), self.miningRate),
                IFluidHandler.FluidAction.EXECUTE);
        if (amountPumped > 0) {
            self.sendData();
        }
    }
}
