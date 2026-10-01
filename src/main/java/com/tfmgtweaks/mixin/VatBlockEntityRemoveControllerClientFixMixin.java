package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.machinery.vat.base.VatBlock;
import com.drmangotea.tfmg.content.machinery.vat.base.VatBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

/** Runs the client-safe part of removeController() on the client too, so split vats render correctly. */
@Mixin(VatBlockEntity.class)
public abstract class VatBlockEntityRemoveControllerClientFixMixin {
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
    boolean evaluateNextTick;

    @Shadow
    public abstract void applyVatSize(int blocks);

    @Shadow
    protected abstract void onInventoryChanged();

    @Shadow
    private native void refreshCapability();

    @Overwrite
    public void removeController(boolean keepFluids) {
        VatBlockEntity self = (VatBlockEntity) (Object) this;
        boolean isClientSide = self.getLevel().isClientSide;

        controller = null;
        width = 1;
        height = 1;

        BlockState state = self.getBlockState();
        if (VatBlock.isVat(state)) {
            state = state.setValue(VatBlock.BOTTOM, true);
            state = state.setValue(VatBlock.TOP, true);
            state = state.setValue(VatBlock.SHAPE, window ? VatBlock.Shape.WINDOW : VatBlock.Shape.PLAIN);
            self.getLevel().setBlock(self.getBlockPos(), state, 22);
        }

        if (isClientSide) {
            return;
        }

        updateConnectivity = true;
        if (!keepFluids) {
            applyVatSize(1);
        }
        onInventoryChanged();

        evaluateNextTick = true;

        refreshCapability();
        self.setChanged();
        self.sendData();
    }
}
