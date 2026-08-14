package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.machinery.misc.air_intake.AirIntakeBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * The Air Intake's production formula silently
 * truncates to 0 below a real RPM threshold (integer division), and
 * diesel engines fed from it just never start -- with no in-game
 * indication of shaft speed, production rate, or the threshold anywhere.
 *
 * Adds the missing info to the goggle tooltip: current shaft speed,
 * current production rate, and -- when production is 0 despite some
 * shaft speed -- the RPM actually needed.
 */
@Mixin(AirIntakeBlockEntity.class)
public abstract class AirIntakeBlockEntityMixin {

    @Shadow
    int diameter;

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
}
