package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.machinery.misc.air_intake.AirIntakeBlockEntity;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.tfmgtweaks.airintake.AirIntakeGasMode;
import com.tfmgtweaks.api.ITFMGTweaksAirIntakeGasMode;
import com.tfmgtweaks.config.TFMGTweaksConfig;
import com.tfmgtweaks.integration.pollution.PollutionCompat;
import com.tfmgtweaks.integration.pollution.PollutionIntegration;
import com.tfmgtweaks.mixin.accessor.AirIntakeBlockEntityAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.simibubi.create.content.kinetics.base.DirectionalKineticBlock.FACING;

/**
 * Shows shaft speed, air production and required RPM in the goggle tooltip.
 * Rewrites 3x3 formation so a premature 2x2 can't block a valid 3x3.
 */
@Mixin(AirIntakeBlockEntity.class)
public abstract class AirIntakeBlockEntityMixin {
    @Shadow
    int diameter;

    @Shadow
    public BlockPos controller;

    @Shadow
    public List<AirIntakeBlockEntity> blockEntities;

    /** Persists controller, which TFMG never saves, so formed groups survive a reload. */
    @Inject(method = "read", at = @At("TAIL"), require = 0)
    private void tfmgtweaks$readController(CompoundTag compound, HolderLookup.Provider registries,
                                            boolean clientPacket, CallbackInfo ci) {
        if (compound.contains("TfmgtweaksControllerX")) {
            controller = new BlockPos(
                    compound.getInt("TfmgtweaksControllerX"),
                    compound.getInt("TfmgtweaksControllerY"),
                    compound.getInt("TfmgtweaksControllerZ"));
        }
    }

    @Inject(method = "write", at = @At("TAIL"), require = 0)
    private void tfmgtweaks$writeController(CompoundTag compound, HolderLookup.Provider registries,
                                             boolean clientPacket, CallbackInfo ci) {
        if (controller != null) {
            compound.putInt("TfmgtweaksControllerX", controller.getX());
            compound.putInt("TfmgtweaksControllerY", controller.getY());
            compound.putInt("TfmgtweaksControllerZ", controller.getZ());
        }
    }

    /** Fractional pollution reduction carried between ticks, since rates are per minute. */
    @Unique
    private float tfmgtweaks$pollutionAccumulator = 0f;

    @Inject(method = "addToGoggleTooltip", at = @At("RETURN"), cancellable = true)
    private void tfmgtweaks$addProductionInfo(List<Component> tooltip, boolean isPlayerSneaking,
                                               CallbackInfoReturnable<Boolean> cir) {
        AirIntakeBlockEntity self = (AirIntakeBlockEntity) (Object) this;
        float shaftSpeed = self.maxShaftSpeed;
        int production = ((int) shaftSpeed * (diameter * diameter)) / 40;

        tooltip.add(Component.literal("Shaft Speed: " + (int) shaftSpeed + " RPM")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Air Production: " + production + " mB/t")
                .withStyle(production > 0 ? ChatFormatting.AQUA : ChatFormatting.RED));

        if (production == 0 && shaftSpeed > 0) {
            int neededRpm = (int) Math.ceil(40.0 / (diameter * diameter));
            tooltip.add(Component.literal("Needs at least " + neededRpm + " RPM to produce air")
                    .withStyle(ChatFormatting.RED));
        }

        cir.setReturnValue(true);
    }

    @Inject(method = "getPossibleDiameter", at = @At("HEAD"), cancellable = true, require = 0)
    private void tfmgtweaks$fixLargeMultiblockOverlap(CallbackInfoReturnable<Integer> cir) {
        AirIntakeBlockEntity self = (AirIntakeBlockEntity) (Object) this;
        AirIntakeBlockEntityAccessor selfAccessor = (AirIntakeBlockEntityAccessor) self;
        Level level = self.getLevel();
        if (level == null) {
            return;
        }

        BlockPos selfPos = self.getBlockPos();
        if (!controller.equals(selfPos)) {
            cir.setReturnValue(1);
            return;
        }

        Direction direction = self.getBlockState().getValue(FACING);

        for (int size = 3; size >= 2; size--) {
            for (BlockPos origin : tfmgtweaks$candidateOrigins(selfPos, size, direction)) {
                List<BlockPos> shape = tfmgtweaks$computeShape(origin, size, direction);
                if (!tfmgtweaks$isValidShape(level, shape, direction)) {
                    continue;
                }
                if (!tfmgtweaks$canAbsorb(level, shape, origin)) {
                    continue;
                }

                if (origin.equals(selfPos)) {
                    tfmgtweaks$commit(level, selfAccessor, shape, origin);
                    cir.setReturnValue(size);
                    return;
                }

                // The best shape isn't rooted here, so hand control to its origin, which forms it on its own tick.
                if (level.getBlockEntity(origin) instanceof AirIntakeBlockEntity originBE) {
                    originBE.controller = origin;
                    originBE.blockEntities.clear();
                    ((AirIntakeBlockEntityAccessor) originBE).tfmgtweaks$setIsController(true);
                }
                controller = origin;
                selfAccessor.tfmgtweaks$setIsController(false);
                cir.setReturnValue(1);
                return;
            }
        }

        blockEntities.clear();
        controller = selfPos;
        selfAccessor.tfmgtweaks$setIsController(false);
        cir.setReturnValue(1);
    }

    /** Every origin whose size x size footprint contains self, self first. */
    @Unique
    private static List<BlockPos> tfmgtweaks$candidateOrigins(BlockPos self, int size, Direction direction) {
        List<BlockPos> origins = new ArrayList<>(size * size);
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                BlockPos origin = direction.getAxis().isHorizontal()
                        ? self.below(i).relative(direction.getCounterClockWise(), j)
                        : self.west(i).north(j);
                origins.add(origin);
            }
        }
        return origins;
    }

    /** The size x size footprint rooted at origin. */
    @Unique
    private static List<BlockPos> tfmgtweaks$computeShape(BlockPos origin, int size, Direction direction) {
        List<BlockPos> shape = new ArrayList<>(size * size);
        BlockPos checkedPos = origin;
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                shape.add(checkedPos);
                checkedPos = direction.getAxis().isHorizontal() ? checkedPos.above() : checkedPos.east();
            }
            if (direction.getAxis().isHorizontal()) {
                checkedPos = checkedPos.below(size);
                checkedPos = checkedPos.relative(direction.getClockWise());
            } else {
                checkedPos = checkedPos.west(size);
                checkedPos = checkedPos.south();
            }
        }
        return shape;
    }

    /** Whether every position holds an intake with the same facing. */
    @Unique
    private static boolean tfmgtweaks$isValidShape(Level level, List<BlockPos> shape, Direction direction) {
        for (BlockPos pos : shape) {
            if (!(level.getBlockEntity(pos) instanceof AirIntakeBlockEntity checkedBE)) {
                return false;
            }
            if (checkedBE.getBlockState().getValue(FACING) != direction) {
                return false;
            }
        }
        return true;
    }

    /** A shape can only be claimed if every existing group it touches lies entirely inside it. */
    @Unique
    private static boolean tfmgtweaks$canAbsorb(Level level, List<BlockPos> shape, BlockPos origin) {
        Set<BlockPos> shapeSet = new HashSet<>(shape);
        for (BlockPos pos : shape) {
            AirIntakeBlockEntity checkedBE = (AirIntakeBlockEntity) level.getBlockEntity(pos);
            BlockPos existingController = checkedBE.controller;
            if (existingController == null || existingController.equals(origin)) {
                continue;
            }
            if (!(level.getBlockEntity(existingController) instanceof AirIntakeBlockEntity controllerBE)) {
                continue;
            }
            if (controllerBE.blockEntities.isEmpty()) {
                if (((AirIntakeBlockEntityAccessor) controllerBE).tfmgtweaks$isController()) {
                    return false;
                }
                continue;
            }
            for (AirIntakeBlockEntity member : controllerBE.blockEntities) {
                if (!shapeSet.contains(member.getBlockPos())) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Claims shape with self as its origin. */
    @Unique
    private void tfmgtweaks$commit(Level level, AirIntakeBlockEntityAccessor selfAccessor,
                                    List<BlockPos> shape, BlockPos origin) {
        blockEntities.clear();
        for (BlockPos pos : shape) {
            AirIntakeBlockEntity checkedBE = (AirIntakeBlockEntity) level.getBlockEntity(pos);
            AirIntakeBlockEntityAccessor checkedAccessor = (AirIntakeBlockEntityAccessor) checkedBE;
            boolean isOrigin = pos.equals(origin);
            checkedAccessor.tfmgtweaks$setIsController(isOrigin);
            checkedAccessor.tfmgtweaks$setIsUsedByController(!isOrigin);
            checkedBE.controller = origin;
            checkedBE.setController(origin);
            blockEntities.add(checkedBE);
        }
        controller = origin;
        selfAccessor.tfmgtweaks$setIsController(true);
    }

    /**
     * Gradually reduces nearby pollution while assembled, in Air mode only (other modes convert it instead).
     * Runs once per group, on the origin.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void tfmgtweaks$cleanPollution(CallbackInfo ci) {
        if (!PollutionCompat.isLoaded() || !TFMGTweaksConfig.AIR_INTAKE_POLLUTION_CLEANING_ENABLED.get()) {
            return;
        }
        AirIntakeBlockEntity self = (AirIntakeBlockEntity) (Object) this;
        if (((ITFMGTweaksAirIntakeGasMode) self).tfmgtweaks$getGasMode() != AirIntakeGasMode.AIR) {
            return;
        }
        Level level = self.getLevel();
        if (!(level instanceof ServerLevel serverLevel) || !controller.equals(self.getBlockPos())) {
            return;
        }

        double ratePerMinute;
        if (diameter >= 3) {
            ratePerMinute = TFMGTweaksConfig.AIR_INTAKE_POLLUTION_RATE_3X3.get();
        } else if (diameter == 2) {
            ratePerMinute = TFMGTweaksConfig.AIR_INTAKE_POLLUTION_RATE_2X2.get();
        } else {
            ratePerMinute = TFMGTweaksConfig.AIR_INTAKE_POLLUTION_RATE_1X1.get();
        }

        // Speed bonus scales from 1x at the baseline RPM to 2x at Create's max rotation speed, clamped to [0, 2].
        float currentSpeed = Math.abs(self.getSpeed());
        float baseline = TFMGTweaksConfig.AIR_INTAKE_POLLUTION_SPEED_BONUS_BASELINE_RPM.get();
        float maxSpeed = AllConfigs.server().kinetics.maxRotationSpeed.get();
        float speedBonus;
        if (maxSpeed <= baseline) {
            speedBonus = 1.0f;
        } else {
            speedBonus = 1.0f + (currentSpeed - baseline) / (maxSpeed - baseline);
            speedBonus = Mth.clamp(speedBonus, 0f, 2f);
        }

        double perTick = ratePerMinute * speedBonus / 1200.0;
        tfmgtweaks$pollutionAccumulator += (float) perTick;
        if (tfmgtweaks$pollutionAccumulator < 1f) {
            return;
        }
        int wholeAmount = (int) tfmgtweaks$pollutionAccumulator;
        tfmgtweaks$pollutionAccumulator -= wholeAmount;

        BlockPos pos = self.getBlockPos();
        PollutionCompat.executeIfInstalled(() -> () ->
                PollutionIntegration.reducePollutionNear(serverLevel, pos, wholeAmount));
    }
}
