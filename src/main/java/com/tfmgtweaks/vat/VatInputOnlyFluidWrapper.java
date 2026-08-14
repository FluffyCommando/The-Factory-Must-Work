package com.tfmgtweaks.vat;

import com.simibubi.create.foundation.fluid.CombinedTankWrapper;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * See VatBlockEntityCapabilityFixMixin for the full explanation. Same idea
 * as VatInputOnlyItemWrapper but for fluids: reports both tanks (so
 * goggles/JEI-style queries can still see input contents), but fill()
 * only ever writes to input and drain() only ever reads from output --
 * fully symmetric isolation, not just fill() restricted.
 *
 * Without this, an external pump can fill the vat's output tank directly
 * once input is full (the original bug this class already fixed), and
 * separately, a naive combined-handler drain() search across BOTH tanks
 * together is exactly the same root-cause shape that caused Steam to be
 * rejected on the pump jack whenever oil/waste was also present in one
 * shared handler there -- reported as the vat's output fluid not being
 * extractable even with a filter configured for it, no matter how many
 * times the multiblock was rebuilt (ruling out a multiblock-state
 * staleness cause, since rebuilding didn't help). Isolating drain() to
 * output only, the same way fill() is already isolated to input only,
 * closes that off entirely.
 */
public class VatInputOnlyFluidWrapper implements IFluidHandler {

    private final IFluidHandler input;
    private final IFluidHandler output;
    private final CombinedTankWrapper combined;

    public VatInputOnlyFluidWrapper(IFluidHandler input, IFluidHandler output) {
        this.input = input;
        this.output = output;
        this.combined = new CombinedTankWrapper(input, output);
    }

    @Override
    public int getTanks() {
        return combined.getTanks();
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return combined.getFluidInTank(tank);
    }

    @Override
    public int getTankCapacity(int tank) {
        return combined.getTankCapacity(tank);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return combined.isFluidValid(tank, stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return input.fill(resource, action);
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return output.drain(resource, action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return output.drain(maxDrain, action);
    }
}
