package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.decoration.tanks.steel.SteelTankBlockEntity;
import com.drmangotea.tfmg.content.machinery.oil_processing.distillation_tower.controller.DistillationControllerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.drmangotea.tfmg.content.machinery.oil_processing.distillation_tower.controller.DistillationControllerBlock.getFacing;

/** Refreshes the steel tank's tower state and heat every tick, so heat isn't stuck at 0 after a reload. */
@Mixin(DistillationControllerBlockEntity.class)
public abstract class DistillationControllerHeatSourceRefreshMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void tfmgtweaks$refreshHeatSource(CallbackInfo ci) {
        DistillationControllerBlockEntity self = (DistillationControllerBlockEntity) (Object) this;
        if (self.getLevel() == null || self.getLevel().isClientSide) {
            return;
        }
        BlockEntity beBehind = self.getLevel().getBlockEntity(
                self.getBlockPos().relative(getFacing(self.getBlockState()).getOpposite()));
        if (beBehind instanceof SteelTankBlockEntity steelTank) {
            steelTank.updateBoilerState();
            steelTank.updateTemperature();
        }
    }
}
