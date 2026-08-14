package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.electricity.base.IElectric;
import com.drmangotea.tfmg.content.electricity.measurement.MultimeterItem;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import com.simibubi.create.content.equipment.goggles.GoggleOverlayRenderer;
import com.simibubi.create.foundation.gui.RemovedGuiUtils;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Wearing goggles blocks the multimeter overlay entirely, regardless of
 * whether a multimeter is even held.
 *
 * Root cause: TFMG's own GoggleOverlayRendererMixin injects its whole
 * multimeter tooltip block at HEAD of Create's renderOverlay, gated by
 * `if (isElectricBlock && !hasGoggles)` -- the inner check already
 * correctly gates on holdsMultimeter, so !hasGoggles on the outer
 * condition looks like it was meant to be !holdsMultimeter and got
 * swapped by mistake.
 *
 * An earlier version of this fix tried to @Redirect the isWearingGoggles()
 * call inside TFMG's own injected code to always return false. That
 * turned out fragile in a way that couldn't be confirmed without live
 * testing: it depends on our mixin being applied strictly after TFMG's
 * (so the call it's redirecting actually exists yet in the merged
 * bytecode), which is a real but unverifiable assumption about mixin
 * priority ordering between two different mods' configs -- and the fix
 * had no visible effect when tested.
 *
 * This replaces that with an independent, supplementary injection instead
 * of trying to modify TFMG's own code at all. It only activates
 * specifically to cover the goggles-worn gap (checks isWearingGoggles
 * itself, up front) -- when goggles aren't worn, this returns immediately
 * and TFMG's own already-working code handles it completely unchanged.
 * When goggles ARE worn and a multimeter is held, this builds and renders
 * the same tooltip TFMG's own code would have, then cancels the rest of
 * renderOverlay so neither TFMG's own gated-off code nor Create's
 * original body run afterward and potentially interfere.
 *
 * Position/drawing intentionally kept simpler than TFMG's own version
 * (skips the fade-in animation and ModernUI cursor-jiggle workaround) --
 * lower risk to get right without visual testing, and this is a
 * functional fix (can you see the info at all), not a cosmetic match.
 */
@Mixin(GoggleOverlayRenderer.class)
public abstract class GoggleOverlayRendererMultimeterFixMixin {

    @Inject(method = "renderOverlay", at = @At("HEAD"), cancellable = true, remap = false)
    private static void tfmgtweaks$renderMultimeterEvenWithGoggles(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (!GogglesItem.isWearingGoggles(mc.player)) {
            return;
        }
        if (!MultimeterItem.isHeldByPlayer(mc.player)) {
            return;
        }

        HitResult objectMouseOver = mc.hitResult;
        if (!(objectMouseOver instanceof BlockHitResult result)) {
            return;
        }

        ClientLevel world = mc.level;
        BlockPos pos = GoggleOverlayRenderer.proxiedOverlayPosition(world, result.getBlockPos());
        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof IElectric electric)) {
            return;
        }

        boolean isShifting = mc.player.isShiftKeyDown();
        List<Component> tooltip = new ArrayList<>();
        electric.makeMultimeterTooltip(tooltip, isShifting);
        if (tooltip.isEmpty()) {
            return;
        }

        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();
        int posX = width / 2;
        int posY = height / 2;

        // Standard vanilla tooltip colors -- same ones GuiGraphics itself
        // uses internally, avoids depending on Create's config-driven
        // color/fade logic.
        RemovedGuiUtils.drawHoveringText(guiGraphics, tooltip, posX, posY, width, height, -1,
                0xF0100010, 0x505000FF, 0x5028007F, mc.font);

        ci.cancel();
    }
}
