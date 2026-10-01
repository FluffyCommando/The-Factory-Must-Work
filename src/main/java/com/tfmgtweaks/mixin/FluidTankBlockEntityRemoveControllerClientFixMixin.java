package com.tfmgtweaks.mixin;

import com.simibubi.create.content.fluids.tank.BoilerData;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

/** Runs the client-safe part of removeController() on the client too, so split tanks render correctly. */
@Mixin(FluidTankBlockEntity.class)
public abstract class FluidTankBlockEntityRemoveControllerClientFixMixin {
    @Shadow
    protected BlockPos controller;

    @Shadow
    protected int width;

    @Shadow
    protected int height;

    @Shadow
    protected boolean window;

    @Shadow
    protected boolean updateConnectivity;

    @Shadow
    public BoilerData boiler;

    @Shadow
    protected FluidTank tankInventory;

    @Shadow
    public abstract void applyFluidTankSize(int blocks);

    @Shadow
    protected abstract void onFluidStackChanged(FluidStack newFluidStack);

    @Shadow
    abstract void refreshCapability();

    @Overwrite
    public void removeController(boolean keepFluids) {
        FluidTankBlockEntity self = (FluidTankBlockEntity) (Object) this;
        boolean isClientSide = self.getLevel().isClientSide;

        controller = null;
        width = 1;
        height = 1;
        boiler.clear();

        BlockState state = self.getBlockState();
        if (FluidTankBlock.isTank(state)) {
            state = state.setValue(FluidTankBlock.BOTTOM, true);
            state = state.setValue(FluidTankBlock.TOP, true);
            state = state.setValue(FluidTankBlock.SHAPE, window ? FluidTankBlock.Shape.WINDOW : FluidTankBlock.Shape.PLAIN);
            self.getLevel().setBlock(self.getBlockPos(), state,
                    Block.UPDATE_CLIENTS | Block.UPDATE_INVISIBLE | Block.UPDATE_KNOWN_SHAPE);
        }

        if (isClientSide) {
            return;
        }

        updateConnectivity = true;
        if (!keepFluids) {
            applyFluidTankSize(1);
        }
        onFluidStackChanged(tankInventory.getFluid());

        refreshCapability();
        self.setChanged();
        self.sendData();
    }
}
