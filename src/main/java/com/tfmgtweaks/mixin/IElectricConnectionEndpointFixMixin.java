package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.electricity.connection.cables.CableConnection;
import com.drmangotea.tfmg.content.electricity.base.IElectric;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The same blockPos1-vs-blockPos2 confusion fixed in
 * CableConnectorBlockEntity also exists in IElectric
 * itself, the shared interface every electric block uses:
 *
 *  - onConnected(): builds network connectivity when any electric block
 *    is placed. Compares blockPos1 to getBlockPos() with `==`, but
 *    getBlockPos() constructs a new object every call, so the comparison
 *    is always false and always falls through to blockPos1 regardless of
 *    which endpoint that actually is.
 *  - updateUnpowered(): doesn't even attempt a self-check, always uses
 *    blockPos1 directly. Notifies connected segments of insufficient
 *    power, so a broken version leaves stale power-availability state.
 *
 * Both corrected to read whichever endpoint isn't this block's own
 * position.
 */
@Mixin(IElectric.class)
public interface IElectricConnectionEndpointFixMixin {

    @Redirect(
        method = "onConnected",
        at = @At(value = "FIELD",
            target = "Lcom/drmangotea/tfmg/content/electricity/connection/cables/CableConnection;blockPos1:Lnet/minecraft/core/BlockPos;",
            ordinal = 1))
    default BlockPos tfmgtweaks$correctOtherEndpointOnConnected(CableConnection connection) {
        BlockPos selfPos = ((IElectric) this).getBlockPos();
        return connection.blockPos1.equals(selfPos) ? connection.blockPos2 : connection.blockPos1;
    }

    @Redirect(
        method = "updateUnpowered",
        at = @At(value = "FIELD",
            target = "Lcom/drmangotea/tfmg/content/electricity/connection/cables/CableConnection;blockPos1:Lnet/minecraft/core/BlockPos;"))
    default BlockPos tfmgtweaks$correctOtherEndpointOnUpdateUnpowered(CableConnection connection) {
        BlockPos selfPos = ((IElectric) this).getBlockPos();
        return connection.blockPos1.equals(selfPos) ? connection.blockPos2 : connection.blockPos1;
    }
}
