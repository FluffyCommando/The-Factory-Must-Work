package com.tfmgtweaks.vat;

import com.simibubi.create.foundation.fluid.CombinedTankWrapper;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Vat fluid handler that fills only the input tanks and drains only the output tanks. */
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
