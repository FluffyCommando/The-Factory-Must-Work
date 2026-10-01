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

/** Shows the multimeter overlay while wearing goggles, which TFMG's own overlay check blocks. */
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

        // Vanilla tooltip colors.
        RemovedGuiUtils.drawHoveringText(guiGraphics, tooltip, posX, posY, width, height, -1,
                0xF0100010, 0x505000FF, 0x5028007F, mc.font);

        ci.cancel();
    }
}
