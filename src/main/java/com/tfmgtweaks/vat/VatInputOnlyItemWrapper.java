package com.tfmgtweaks.vat;

import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import net.minecraft.world.item.ItemStack;

/** Vat item handler that rejects insertion into output slots. */
public class VatInputOnlyItemWrapper implements IItemHandlerModifiable {
    private final IItemHandlerModifiable input;
    private final CombinedInvWrapper combined;
    private final int inputSlots;

    public VatInputOnlyItemWrapper(IItemHandlerModifiable input, IItemHandlerModifiable output) {
        this.input = input;
        this.combined = new CombinedInvWrapper(input, output);
        this.inputSlots = input.getSlots();
    }

    @Override
    public int getSlots() {
        return combined.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return combined.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (slot < inputSlots) {
            return combined.insertItem(slot, stack, simulate);
        }
        return stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return combined.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return combined.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (slot < inputSlots) {
            return combined.isItemValid(slot, stack);
        }
        return false;
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        combined.setStackInSlot(slot, stack);
    }
}
