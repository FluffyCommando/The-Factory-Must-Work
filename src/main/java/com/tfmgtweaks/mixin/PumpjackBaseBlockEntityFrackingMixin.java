package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.machinery.oil_processing.pumpjack.base.PumpjackBaseBlockEntity;
import com.tfmgtweaks.api.ITFMGTweaksPumpjackFluidCapability;
import com.tfmgtweaks.pumpjack.PumpjackFrackingWrapper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Replaces the pump jack base's fluid capability with per-face roles (oil, waste, Steam). */
@Mixin(PumpjackBaseBlockEntity.class)
public abstract class PumpjackBaseBlockEntityFrackingMixin implements ITFMGTweaksPumpjackFluidCapability {
    @Shadow
    protected IFluidHandler fluidCapability;

    private PumpjackFrackingWrapper tfmgtweaks$frackingCore;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void tfmgtweaks$installFrackingWrapper(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                                     CallbackInfo ci) {
        PumpjackBaseBlockEntity self = (PumpjackBaseBlockEntity) (Object) this;
        this.tfmgtweaks$frackingCore = new PumpjackFrackingWrapper(self, self.tank);
        this.fluidCapability = this.tfmgtweaks$frackingCore.forDisplay();
    }

    @Override
    public PumpjackFrackingWrapper tfmgtweaks$getFrackingCore() {
        return this.tfmgtweaks$frackingCore;
    }

    @Inject(method = "registerCapabilities", at = @At("HEAD"), cancellable = true)
    private static void tfmgtweaks$registerDirectionalCapability(RegisterCapabilitiesEvent event, CallbackInfo ci) {
        tfmgtweaks$doRegisterDirectionalCapability(event);
        ci.cancel();
    }

    @SuppressWarnings("unchecked")
    private static void tfmgtweaks$doRegisterDirectionalCapability(RegisterCapabilitiesEvent event) {
        BlockEntityType<PumpjackBaseBlockEntity> pumpjackType = (BlockEntityType<PumpjackBaseBlockEntity>)
                BuiltInRegistries.BLOCK_ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath("tfmg", "pumpjack_base"));
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                pumpjackType,
                (be, context) -> {
                    PumpjackFrackingWrapper core = ((ITFMGTweaksPumpjackFluidCapability) be).tfmgtweaks$getFrackingCore();
                    if (core == null) {
                        return null;
                    }
                    if (context == null) {
                        return core.forDisplay();
                    }
                    PumpjackFrackingWrapper.FaceRole role = core.roleOf(context);
                    return switch (role) {
                        case OIL -> core.forOilOnly();
                        case WASTE -> core.forWasteOnly();
                        case STEAM -> core.forSteamOnly();
                        case NONE -> null;
                    };
                }
        );
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tfmgtweaks$tickSteamProcessing(CallbackInfo ci) {
        if (tfmgtweaks$frackingCore != null) {
            tfmgtweaks$frackingCore.tickProcessing();
        }
    }

    /** Whether the oil tank has filled since this block entity was created. */
    @Unique
    private boolean tfmgtweaks$hasEverHadOil = false;

    /** Invalidates capabilities once, when the oil tank first fills, so connected pipes can drain it. */
    @Inject(method = "onFluidStackChanged", at = @At("HEAD"))
    private void tfmgtweaks$invalidateCapabilitiesOnFluidChange(FluidStack newFluidStack, CallbackInfo ci) {
        if (tfmgtweaks$hasEverHadOil || newFluidStack.getAmount() <= 0) {
            return;
        }
        tfmgtweaks$hasEverHadOil = true;
        PumpjackBaseBlockEntity self = (PumpjackBaseBlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level != null && !level.isClientSide) {
            level.invalidateCapabilities(self.getBlockPos());
        }
    }

    @Inject(method = "write", at = @At("HEAD"))
    private void tfmgtweaks$writeFrackingState(CompoundTag compound, HolderLookup.Provider registries,
                                                boolean clientPacket, CallbackInfo ci) {
        if (tfmgtweaks$frackingCore != null) {
            compound.putInt("TFMGTweaksWasteWater", tfmgtweaks$frackingCore.wasteAmount);
            compound.putInt("TFMGTweaksSteam", tfmgtweaks$frackingCore.steamAmount);
            for (Direction direction : Direction.values()) {
                PumpjackFrackingWrapper.FaceRole role = tfmgtweaks$frackingCore.roleOf(direction);
                if (role != PumpjackFrackingWrapper.FaceRole.NONE) {
                    compound.putInt("TFMGTweaksFace" + direction.ordinal(), role.ordinal());
                }
            }
        }
    }

    @Inject(method = "read", at = @At("TAIL"))
    private void tfmgtweaks$readFrackingState(CompoundTag compound, HolderLookup.Provider registries,
                                               boolean clientPacket, CallbackInfo ci) {
        if (tfmgtweaks$frackingCore != null) {
            tfmgtweaks$frackingCore.wasteAmount = compound.getInt("TFMGTweaksWasteWater");
            tfmgtweaks$frackingCore.steamAmount = compound.getInt("TFMGTweaksSteam");
            for (Direction direction : Direction.values()) {
                String key = "TFMGTweaksFace" + direction.ordinal();
                PumpjackFrackingWrapper.FaceRole role = compound.contains(key)
                        ? PumpjackFrackingWrapper.FaceRole.values()[compound.getInt(key)]
                        : PumpjackFrackingWrapper.FaceRole.NONE;
                tfmgtweaks$frackingCore.loadRole(direction, role);
            }
            // The client only learns about role changes here, so refresh neighboring pipes.
            if (clientPacket) {
                tfmgtweaks$frackingCore.notifyAllNeighbors();
            }
        }
    }

    @Inject(method = "addToGoggleTooltip", at = @At("RETURN"), cancellable = true)
    private void tfmgtweaks$addFrackingTooltip(List<Component> tooltip, boolean isPlayerSneaking,
                                                CallbackInfoReturnable<Boolean> cir) {
        if (tfmgtweaks$frackingCore == null) {
            return;
        }
        tooltip.add(Component.literal("Oil: " + tfmgtweaks$facesFor(PumpjackFrackingWrapper.FaceRole.OIL)
                + " | Waste: " + tfmgtweaks$facesFor(PumpjackFrackingWrapper.FaceRole.WASTE)
                + " | Steam: " + tfmgtweaks$facesFor(PumpjackFrackingWrapper.FaceRole.STEAM))
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Wrench a side to assign it")
                .withStyle(ChatFormatting.DARK_GRAY));
        if (tfmgtweaks$frackingCore.steamAmount > 0) {
            tooltip.add(Component.literal("Steam: " + tfmgtweaks$frackingCore.steamAmount + " mB")
                    .withStyle(ChatFormatting.GRAY));
        }
        if (tfmgtweaks$frackingCore.wasteAmount > 0) {
            tooltip.add(Component.literal("Waste Byproduct: " + tfmgtweaks$frackingCore.wasteAmount + " mB")
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    /** Comma-separated faces assigned to role. */
    private String tfmgtweaks$facesFor(PumpjackFrackingWrapper.FaceRole role) {
        StringBuilder result = new StringBuilder();
        for (Direction direction : Direction.values()) {
            if (tfmgtweaks$frackingCore.roleOf(direction) == role) {
                if (!result.isEmpty()) {
                    result.append(", ");
                }
                result.append(direction.toString().toLowerCase());
            }
        }
        return result.isEmpty() ? "unassigned" : result.toString();
    }
}
