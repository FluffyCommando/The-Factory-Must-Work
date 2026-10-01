package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.machinery.vat.base.VatBlockEntity;
import com.drmangotea.tfmg.mixin.accessor.TankSegmentAccessor;
import com.drmangotea.tfmg.recipes.VatMachineRecipe;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.List;

/** Fixes vat recipes with 2+ item outputs dropping outputs after one stacks (break -> continue). */
@Mixin(VatBlockEntity.class)
public abstract class VatBlockEntityHandleRecipeFixMixin {
    @Shadow
    protected IFluidHandler fluidCapability;

    @Shadow
    protected IItemHandlerModifiable itemCapability;

    @Shadow
    int timer;

    @Shadow
    int heatLevel;

    @Shadow
    HeatCondition heatCondition;

    @Shadow
    public VatMachineRecipe recipe;

    @Overwrite
    public void handleRecipe() {
        VatBlockEntity self = (VatBlockEntity) (Object) this;

        if (recipe == null)
            return;
        if (!self.isController())
            return;
        if (heatLevel < recipe.heatLevel)
            return;
        if (recipe.getRequiredHeat() == HeatCondition.HEATED && heatCondition == HeatCondition.NONE)
            return;
        if (recipe.getRequiredHeat() == HeatCondition.SUPERHEATED && heatCondition != HeatCondition.SUPERHEATED)
            return;

        if (timer >= recipe.getProcessingDuration()) {
            SmartFluidTank outputFluidHandler = self.outputTank.getPrimaryHandler();
            IFluidHandler fluidHandler = fluidCapability;
            IItemHandler itemHandler = itemCapability;

            // fluid input
            for (SizedFluidIngredient ingredient : recipe.getFluidIngredients()) {
                for (int i = 0; i < fluidHandler.getTanks(); i++) {
                    FluidStack fluidInTank = fluidHandler.getFluidInTank(i);
                    if (ingredient.test(new FluidStack(fluidInTank.getFluidHolder(), 4000))) {
                        fluidHandler.getFluidInTank(i).setAmount(fluidInTank.getAmount() - ingredient.amount());
                        break;
                    }
                }
            }

            // item output; the fix is the continue below
            for (ProcessingOutput output : recipe.getRollableResults()) {
                ItemStack itemStack = output.rollOutput(self.getLevel().random);

                boolean handled = false;
                for (int i = 0; i < self.outputInventory.getSlots(); i++) {
                    ItemStack stackInSlot = self.outputInventory.getStackInSlot(i);
                    if (stackInSlot.isEmpty())
                        continue;

                    if (stackInSlot.is(itemStack.getItem())) {
                        self.outputInventory.getStackInSlot(i).setCount(stackInSlot.getCount() + (itemStack.getCount()));
                        handled = true;
                        break;
                    }
                }
                if (handled)
                    continue;
                for (int i = 0; i < self.outputInventory.getSlots(); i++) {
                    ItemStack itemInSlot = self.outputInventory.getStackInSlot(i);
                    if (itemInSlot.isEmpty()) {
                        self.outputInventory.setStackInSlot(i, itemStack);
                        break;
                    }
                }
            }

            // item input
            if (recipe != null)
                for (Ingredient ingredient : recipe.getIngredients()) {
                    for (int i = 0; i < fluidHandler.getTanks(); i++) {
                        ItemStack stackInInv = itemHandler.getStackInSlot(i);
                        if (ingredient.test(new ItemStack(stackInInv.getItem(), 64))) {
                            stackInInv.setCount(stackInInv.getCount() - ingredient.getItems()[0].getCount());
                            break;
                        }
                    }
                }

            // fluid output
            List<FluidStack> handledFluidStacks = new ArrayList<>();
            List<SmartFluidTankBehaviour.TankSegment> tankSegments = List.of(self.outputTank.getTanks());
            if (recipe != null)
                for (FluidStack fluidStack : recipe.getFluidResults()) {
                    for (SmartFluidTankBehaviour.TankSegment tankSegment : tankSegments) {
                        SmartFluidTank tank = ((TankSegmentAccessor) tankSegment).tfmg$tank();
                        FluidStack fluidInTank = tank.getFluid();
                        if (handledFluidStacks.contains(fluidStack)) break;

                        if (fluidInTank.getFluid().isSame(fluidStack.getFluid())) {
                            tank.fill(new FluidStack(fluidStack.getFluid(), fluidStack.getAmount()), IFluidHandler.FluidAction.EXECUTE);
                            handledFluidStacks.add(fluidStack);
                            break;
                        }
                        if (!handledFluidStacks.contains(fluidStack) && fluidInTank.isEmpty()) {
                            tank.fill(new FluidStack(fluidStack.getFluid(), fluidStack.getAmount()), IFluidHandler.FluidAction.EXECUTE);
                            break;
                        }
                    }
                }
            recipe = null;
            timer = 0;
        } else {
            timer++;
        }
    }
}
