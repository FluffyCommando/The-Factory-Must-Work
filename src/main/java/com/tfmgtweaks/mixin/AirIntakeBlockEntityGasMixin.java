package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.base.fluid.ForceableFluidTank;
import com.drmangotea.tfmg.content.machinery.misc.air_intake.AirIntakeBlockEntity;
import com.tfmgtweaks.airintake.AirIntakeGasMode;
import com.tfmgtweaks.api.ITFMGTweaksAirIntakeGasMode;
import com.tfmgtweaks.config.TFMGTweaksConfig;
import com.tfmgtweaks.integration.pollution.PollutionCompat;
import com.tfmgtweaks.integration.pollution.PollutionIntegration;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Adds a selectable gas mode (Air / Carbon Dioxide / Sulfur Dioxide), consuming nearby pollution for CO2 and SO2. */
@Mixin(AirIntakeBlockEntity.class)
public abstract class AirIntakeBlockEntityGasMixin implements ITFMGTweaksAirIntakeGasMode {
    @Shadow
    protected ForceableFluidTank tankInventory;

    @Shadow
    int diameter;

    @Shadow
    public BlockPos controller;

    @Unique
    private AirIntakeGasMode tfmgtweaks$mode = AirIntakeGasMode.AIR;

    @Unique
    private static final int TFMGTWEAKS$MB_PER_BUCKET = 1000;

    @Override
    public AirIntakeGasMode tfmgtweaks$getGasMode() {
        return tfmgtweaks$mode;
    }

    @Override
    public void tfmgtweaks$setGasMode(AirIntakeGasMode mode) {
        if (mode == tfmgtweaks$mode) {
            return;
        }
        tfmgtweaks$mode = mode;
        tankInventory.setFluid(FluidStack.EMPTY);
        AirIntakeBlockEntity self = (AirIntakeBlockEntity) (Object) this;
        self.setChanged();
        self.sendData();
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE",
            target = "Lcom/drmangotea/tfmg/base/fluid/ForceableFluidTank;forceFill("
                    + "Lnet/neoforged/neoforge/fluids/FluidStack;"
                    + "Lnet/neoforged/neoforge/fluids/capability/IFluidHandler$FluidAction;)I"))
    private int tfmgtweaks$redirectProducedGas(ForceableFluidTank tank, FluidStack originalStack, IFluidHandler.FluidAction action) {
        if (tfmgtweaks$mode == AirIntakeGasMode.AIR) {
            return tank.forceFill(originalStack, action);
        }
        if (!action.execute()) {
            return 0;
        }

        AirIntakeBlockEntity self = (AirIntakeBlockEntity) (Object) this;
        if (!controller.equals(self.getBlockPos())) {
            return 0;
        }
        if (!PollutionCompat.isLoaded() || !TFMGTweaksConfig.AIR_INTAKE_POLLUTION_CLEANING_ENABLED.get()) {
            return 0;
        }
        if (!(self.getLevel() instanceof ServerLevel serverLevel)) {
            return 0;
        }

        int desiredPollution = tfmgtweaks$desiredProduction(self);
        if (desiredPollution <= 0) {
            return 0;
        }

        int spaceRemaining = tank.getCapacity() - tank.getFluidAmount();
        int maxUsefulPollution = spaceRemaining / TFMGTWEAKS$MB_PER_BUCKET;
        int cappedDesired = Math.min(desiredPollution, maxUsefulPollution);
        if (cappedDesired <= 0) {
            return 0;
        }

        BlockPos pos = self.getBlockPos();
        AirIntakeGasMode mode = tfmgtweaks$mode;
        int fallbackHeight = TFMGTweaksConfig.AIR_INTAKE_GAS_PRODUCTION_HEIGHT_Y.get();
        int settleY = PollutionCompat.getGasSettleHeightIfInstalled(() -> () ->
                PollutionIntegration.getGasSettleHeight(serverLevel, pos, mode), fallbackHeight);

        int consumedPollution = PollutionCompat.consumePollutionIfInstalled(() -> () ->
                tfmgtweaks$consumePollutionNearby(serverLevel, pos, mode, cappedDesired, settleY));
        if (consumedPollution <= 0) {
            return 0;
        }

        int producedMb = consumedPollution * TFMGTWEAKS$MB_PER_BUCKET;
        int newAmount = Math.min(tank.getFluidAmount() + producedMb, tank.getCapacity());
        tank.setFluid(new FluidStack(tfmgtweaks$mode.fluid(), newAmount));
        return producedMb;
    }

    /** Spends pollution in nearby chunks, radius scaling with intake size. */
    @Unique
    private int tfmgtweaks$consumePollutionNearby(ServerLevel serverLevel, BlockPos pos, AirIntakeGasMode mode,
                                                   int desired, int settleY) {
        int radius = Math.max(0, diameter - 1);
        ChunkPos center = new ChunkPos(pos);
        int remaining = desired;
        int consumed = 0;
        for (int dx = -radius; dx <= radius && remaining > 0; dx++) {
            for (int dz = -radius; dz <= radius && remaining > 0; dz++) {
                ChunkPos chunk = new ChunkPos(center.x + dx, center.z + dz);
                int fromThisChunk = PollutionIntegration.spendPollutionInChunk(serverLevel, chunk, settleY, mode, remaining);
                consumed += fromThisChunk;
                remaining -= fromThisChunk;
            }
        }
        return consumed;
    }

    /** Same throughput formula as TFMG's own air production. */
    @Unique
    private int tfmgtweaks$desiredProduction(AirIntakeBlockEntity self) {
        float shaftSpeed = self.maxShaftSpeed;
        return ((int) shaftSpeed * (diameter * diameter)) / 40;
    }

    @Inject(method = "write", at = @At("TAIL"))
    private void tfmgtweaks$writeGasMode(CompoundTag compound, HolderLookup.Provider registries,
                                          boolean clientPacket, CallbackInfo ci) {
        compound.putString("TfmgtweaksGasMode", tfmgtweaks$mode.name());
    }

    @Inject(method = "read", at = @At("TAIL"))
    private void tfmgtweaks$readGasMode(CompoundTag compound, HolderLookup.Provider registries,
                                         boolean clientPacket, CallbackInfo ci) {
        if (compound.contains("TfmgtweaksGasMode")) {
            try {
                tfmgtweaks$mode = AirIntakeGasMode.valueOf(compound.getString("TfmgtweaksGasMode"));
            } catch (IllegalArgumentException ignored) {
                tfmgtweaks$mode = AirIntakeGasMode.AIR;
            }
        }
    }

    @Inject(method = "addToGoggleTooltip", at = @At("RETURN"))
    private void tfmgtweaks$addGasModeTooltip(List<Component> tooltip, boolean isPlayerSneaking,
                                               CallbackInfoReturnable<Boolean> cir) {
        tooltip.add(Component.translatable("tfmgtweaks.air_intake.mode_tooltip", tfmgtweaks$mode.displayName())
                .withStyle(ChatFormatting.DARK_GRAY));
        if (tfmgtweaks$mode != AirIntakeGasMode.AIR && !PollutionCompat.isLoaded()) {
            tooltip.add(Component.translatable("tfmgtweaks.air_intake.no_pollution_mod")
                    .withStyle(ChatFormatting.RED));
        }
    }
}

