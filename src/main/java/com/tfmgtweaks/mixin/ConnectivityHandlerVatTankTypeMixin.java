package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.decoration.tanks.TFMGFluidTankBlockEntity;
import com.drmangotea.tfmg.content.machinery.vat.base.VatBlock;
import com.drmangotea.tfmg.content.machinery.vat.base.VatBlockEntity;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

/** Stops vats or tanks of different materials merging into one multiblock. */
@Mixin(ConnectivityHandler.class)
public abstract class ConnectivityHandlerVatTankTypeMixin {
    private static String tfmgtweaks$currentVatType = null;
    private static Class<?> tfmgtweaks$currentTankClass = null;

    /** Records which vat type or tank class started this formation search. */
    @Inject(method = "formMulti(Lnet/minecraft/world/level/block/entity/BlockEntity;)V", at = @At("HEAD"))
    private static void tfmgtweaks$trackFormationOrigin(BlockEntity be, CallbackInfo ci) {
        tfmgtweaks$currentVatType = null;
        tfmgtweaks$currentTankClass = null;
        if (be instanceof VatBlockEntity && be.getBlockState().getBlock() instanceof VatBlock originBlock) {
            tfmgtweaks$currentVatType = originBlock.vatType;
        } else if (be instanceof TFMGFluidTankBlockEntity) {
            tfmgtweaks$currentTankClass = be.getBlockState().getBlock().getClass();
        }
    }

    /** Rejects candidates whose vat type or tank class doesn't match. */
    @Inject(method = "partAt", at = @At("RETURN"), cancellable = true)
    private static void tfmgtweaks$rejectMismatchedVatTankType(BlockEntityType<?> type, BlockGetter level, BlockPos pos,
                                                                 CallbackInfoReturnable<BlockEntity> cir) {
        BlockEntity result = cir.getReturnValue();
        if (result == null) {
            return;
        }
        if (tfmgtweaks$currentVatType != null && result instanceof VatBlockEntity) {
            VatBlock resultBlock = result.getBlockState().getBlock() instanceof VatBlock vb ? vb : null;
            if (resultBlock == null || !Objects.equals(resultBlock.vatType, tfmgtweaks$currentVatType)) {
                cir.setReturnValue(null);
            }
        } else if (tfmgtweaks$currentTankClass != null && result instanceof TFMGFluidTankBlockEntity) {
            if (result.getBlockState().getBlock().getClass() != tfmgtweaks$currentTankClass) {
                cir.setReturnValue(null);
            }
        }
    }
}
