package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.electricity.connection.cables.CableConnection;
import com.drmangotea.tfmg.content.electricity.base.IElectric;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Reads whichever cable endpoint isn't this block in onConnected() and updateUnpowered(). */
@Mixin(IElectric.class)
public interface IElectricConnectionEndpointFixMixin {
    @Redirect(
        require = 0,
        method = "onConnected",
        at = @At(value = "FIELD",
            target = "Lcom/drmangotea/tfmg/content/electricity/connection/cables/CableConnection;blockPos1:Lnet/minecraft/core/BlockPos;",
            ordinal = 1))
    default BlockPos tfmgtweaks$correctOtherEndpointOnConnected(CableConnection connection) {
        BlockPos selfPos = ((IElectric) this).getBlockPos();
        return connection.blockPos1.equals(selfPos) ? connection.blockPos2 : connection.blockPos1;
    }

    @Redirect(
        require = 0,
        method = "updateUnpowered",
        at = @At(value = "FIELD",
            target = "Lcom/drmangotea/tfmg/content/electricity/connection/cables/CableConnection;blockPos1:Lnet/minecraft/core/BlockPos;"))
    default BlockPos tfmgtweaks$correctOtherEndpointOnUpdateUnpowered(CableConnection connection) {
        BlockPos selfPos = ((IElectric) this).getBlockPos();
        return connection.blockPos1.equals(selfPos) ? connection.blockPos2 : connection.blockPos1;
    }
}
