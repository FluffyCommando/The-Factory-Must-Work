package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.machinery.vat.base.VatBlock;
import com.drmangotea.tfmg.content.machinery.vat.base.VatBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Follow-up to VatMixedTypeConnectivityFixMixin: that fix calls
 * ConnectivityHandler.splitMulti() on detecting mismatched vat types,
 * which resolves to VatBlockEntity's own removeController() for the
 * actual split. Confirmed by reading it directly: removeController()'s
 * very first line is `if (level.isClientSide) return;` -- meaning it is
 * a complete no-op on the client, not just skipping some server-only
 * bookkeeping. Not even controller/width/height get reset there.
 *
 * Since TFMG's own multiblock formation (VatBlock.onPlace() ->
 * updateConnectivity()) runs independently on both sides with no
 * client-side guard of its own, the client computes its own copy of the
 * (wrong, merged) structure and renders it -- and our earlier fix's
 * splitMulti() call, while fully correct server-side, has zero effect
 * on that client-side copy. Reported symptom ("still try to connect")
 * is exactly this: the server's logical state is fine, but the client's
 * own visual state is never corrected.
 *
 * Fix: @Overwrite the full method (matching TFMG's own source exactly),
 * restructured so the client-safe parts -- resetting
 * controller/width/height and recomputing the visual blockstate, both
 * pure local state with no cross-client authority concerns -- run
 * unconditionally on both sides, while the genuinely server-authoritative
 * parts (resizing tanks/inventory, recipe re-evaluation, capability
 * refresh, saving, network sync) stay gated behind the same client check
 * as before. This also benefits any other caller of removeController(),
 * not just our own split -- e.g. normal vat disassembly likely has the
 * same latent client-visual-lag gap.
 */
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
