package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.machinery.oil_processing.pumpjack.base.PumpjackBaseBlockEntity;
import com.tfmgtweaks.config.TFMGTweaksConfig;
import com.tfmgtweaks.content.oilrock.OilRockBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets pump jacks recognize Oil Rock deposits, not just TFMG's own oil
 * deposit. findDeposit()'s single-block check is redirected to check our
 * own tfmgtweaks:oil_deposit_blocks tag (containing both tfmg:oil_deposit
 * and tfmgtweaks:oil_rock) instead.
 *
 * Two additional injections around the existing miningRate/process()
 * sequence in tick():
 *  - Before process(): applies OIL_ROCK_BASE_EXTRACTION_MULTIPLIER (and
 *    OIL_ROCK_CRACKED_EXTRACTION_MULTIPLIER once cracked) when connected
 *    to an Oil Rock. Zeroes miningRate entirely if
 *    OIL_ROCK_REQUIRE_CRACKED_TO_EXTRACT is on and it isn't cracked yet.
 *  - After process(): if finite reserves are enabled, depletes the
 *    deposit by miningRate and resets it once empty.
 */
@Mixin(PumpjackBaseBlockEntity.class)
public abstract class PumpjackBaseBlockEntityMixin {

    private static final TagKey<Block> OIL_DEPOSIT_BLOCKS = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("tfmgtweaks", "oil_deposit_blocks"));

    @Redirect(
        method = "findDeposit",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"))
    private boolean tfmgtweaks$checkOilDepositOrRock(BlockState state, Block block) {
        return state.is(OIL_DEPOSIT_BLOCKS);
    }

    @Inject(
        method = "tick",
        at = @At(value = "INVOKE",
            target = "Lcom/drmangotea/tfmg/content/machinery/oil_processing/pumpjack/base/PumpjackBaseBlockEntity;process()V"))
    private void tfmgtweaks$applyExtractionMultipliers(CallbackInfo ci) {
        PumpjackBaseBlockEntity self = (PumpjackBaseBlockEntity) (Object) this;
        OilRockBlockEntity oilRock = tfmgtweaks$getOilRock(self);
        if (oilRock == null) {
            return;
        }
        boolean cracked = oilRock.isCracked();
        if (!cracked && TFMGTweaksConfig.OIL_ROCK_REQUIRE_CRACKED_TO_EXTRACT.get()) {
            self.miningRate = 0;
            return;
        }
        double multiplier = TFMGTweaksConfig.OIL_ROCK_BASE_EXTRACTION_MULTIPLIER.get();
        if (cracked) {
            multiplier *= TFMGTweaksConfig.OIL_ROCK_CRACKED_EXTRACTION_MULTIPLIER.get();
        }
        self.miningRate = (int) (self.miningRate * multiplier);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tfmgtweaks$depleteOilRock(CallbackInfo ci) {
        if (!TFMGTweaksConfig.OIL_ROCK_FINITE_RESERVES.get()) {
            return;
        }
        PumpjackBaseBlockEntity self = (PumpjackBaseBlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        OilRockBlockEntity oilRock = tfmgtweaks$getOilRock(self);
        if (oilRock == null) {
            return;
        }
        // Mirror process()'s own guard: it only ever attempts a fill when
        // there's room for the full miningRate amount, so miningRate is an
        // accurate stand-in for what actually got pumped this tick.
        if (self.tank.getFluidAmount() + self.miningRate > self.tank.getCapacity()) {
            return;
        }
        if (self.miningRate <= 0) {
            return;
        }
        if (oilRock.depleteReserves(self.miningRate)) {
            self.deposit = null;
        }
    }

    private OilRockBlockEntity tfmgtweaks$getOilRock(PumpjackBaseBlockEntity self) {
        if (self.deposit == null) {
            return null;
        }
        Level level = self.getLevel();
        if (level == null) {
            return null;
        }
        BlockEntity be = level.getBlockEntity(self.deposit);
        return be instanceof OilRockBlockEntity oilRock ? oilRock : null;
    }
}
