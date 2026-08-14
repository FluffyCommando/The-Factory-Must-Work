package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.electricity.connection.cables.CableConnection;
import com.drmangotea.tfmg.content.electricity.connection.cables.CableConnectorBlockEntity;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * After removing a cable insulator, the other end
 * of the connection keeps showing stale voltage/network data forever.
 *
 * Root cause: three places in this class read connection.blockPos1
 * assuming it's always "the other end," when it's actually whichever
 * endpoint wasn't stored as blockPos2 -- roughly half the time, THIS
 * connector is blockPos1, so the real partner (blockPos2) never gets
 * notified/pruned/traversed correctly (in notifyRemoval(),
 * removeConnection()'s cleanup lambda, and getConnectedWires()).
 *
 * Fix: read whichever endpoint isn't this connector's own position,
 * instead of always assuming blockPos1.
 */
@Mixin(CableConnectorBlockEntity.class)
public abstract class CableConnectorBlockEntityMixin {

    @Redirect(
        method = {
            "notifyRemoval",
            "lambda$removeConnection$1",
            "getConnectedWires(Ljava/util/List;)Ljava/util/List;"
        },
        at = @At(value = "FIELD",
            target = "Lcom/drmangotea/tfmg/content/electricity/connection/cables/CableConnection;blockPos1:Lnet/minecraft/core/BlockPos;"))
    private BlockPos tfmgtweaks$getActualPartnerPos(CableConnection connection) {
        CableConnectorBlockEntity self = (CableConnectorBlockEntity) (Object) this;
        return connection.blockPos1.equals(self.getBlockPos()) ? connection.blockPos2 : connection.blockPos1;
    }
}
