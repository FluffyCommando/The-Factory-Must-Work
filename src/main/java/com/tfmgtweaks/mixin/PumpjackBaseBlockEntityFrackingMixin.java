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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Reworks the pump jack base's fluid capability into player-configurable,
 * per-side assignment (oil/waste/Steam), replacing TFMG's own single
 * combined handler. Cancels TFMG's own registerCapabilities() entirely and
 * replaces it with a direction-aware version that looks up
 * PumpjackFrackingWrapper.roleOf(direction) and returns the matching
 * isolated view, or nothing for an unassigned side.
 *
 * All sides start unassigned; a player wrenches each side to a role (see
 * PumpjackWrenchInteractionHandler). Each face's assignment is fully
 * independent, and isolating oil/waste/Steam into separate views (instead
 * of one combined handler) avoids priority and cross-fluid-rejection bugs
 * a shared handler had.
 *
 * write()/read() persist waste/Steam amounts and each face's role, since
 * they live on the wrapper object, not the block entity itself.
 * tfmgtweaks$tickSteamProcessing and tfmgtweaks$invalidateCapabilitiesOnTick
 * both run at HEAD, not TAIL, since PumpjackBaseBlockEntity's own tick()
 * has multiple early returns that would silently skip a TAIL injection.
 */
@Mixin(PumpjackBaseBlockEntity.class)
public abstract class PumpjackBaseBlockEntityFrackingMixin implements ITFMGTweaksPumpjackFluidCapability {

    @Shadow
    protected IFluidHandler fluidCapability;

    @Shadow
    public FluidTank tank;

    private PumpjackFrackingWrapper tfmgtweaks$frackingCore;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void tfmgtweaks$installFrackingWrapper(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                                     CallbackInfo ci) {
        PumpjackBaseBlockEntity self = (PumpjackBaseBlockEntity) (Object) this;
        this.tfmgtweaks$frackingCore = new PumpjackFrackingWrapper(self, this.tank);
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
                    return switch (core.roleOf(context)) {
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

    /**
     * Fixes a separately-reported vanilla TFMG bug: a pump jack fills its
     * oil tank up to some small amount then appears to stop, since
     * process() never calls invalidateCapabilities() after filling the
     * tank, leaving a connected pipe stuck on a stale "nothing to drain"
     * cache. Runs unconditionally at HEAD rather than TAIL of process(),
     * since that method has multiple early returns.
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void tfmgtweaks$invalidateCapabilitiesOnTick(CallbackInfo ci) {
        PumpjackBaseBlockEntity self = (PumpjackBaseBlockEntity) (Object) this;
        if (self.getLevel() != null) {
            self.getLevel().invalidateCapabilities(self.getBlockPos());
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
            // The client never runs assignRole() itself, so this sync is
            // the only way it learns a face's role changed -- notify all
            // 6 neighbors so any connected pipe refreshes its rendering.
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

    /**
     * Comma-separated list of every face currently assigned to a role, for
     * the tooltip -- multiple faces can share the same role now (see
     * PumpjackFrackingWrapper's own doc for why), so this can no longer
     * just report a single side.
     */
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
