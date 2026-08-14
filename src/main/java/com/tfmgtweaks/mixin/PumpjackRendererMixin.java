package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.machinery.oil_processing.pumpjack.hammer.PumpjackBlockEntity;
import com.drmangotea.tfmg.content.machinery.oil_processing.pumpjack.hammer.PumpjackRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The pump jack's crank-to-hammer and hammer-to-head "rope" is drawn
 * procedurally by PumpjackRenderer, the same technique vanilla uses for
 * animal leads (RenderType.leash() + addVertexPair()): a strip of quads
 * along a parabolic sag curve, sampled at 25 fixed points from a "start"
 * anchor (wherever the pose stack is translated to) to an "end" anchor
 * (TFMG's computed direction vector).
 *
 * This mixin widens the ribbon, adds a second ribbon crossed 90 degrees
 * for visual thickness, subdivides each original sample into finer ones,
 * blends configurable start/end offsets across the curve so the two ends
 * can be moved independently, and redraws the alternating light/dark
 * stripe pattern vanilla uses to fake a twisted-rope look.
 */
@Mixin(PumpjackRenderer.class)
public abstract class PumpjackRendererMixin {

    // Vanilla lead width is 0.025F; 6x (~0.15 units) per ribbon.
    private static final float TFMGTWEAKS$ROPE_THICKNESS_MULTIPLIER = 6.0F;

    // How many sub-segments to draw per original segment. 24 original
    // segments x 4 = 96 total.
    private static final int TFMGTWEAKS$RESOLUTION = 4;

    // Stripe settings (see class doc, point 3).
    private static final int TFMGTWEAKS$STRIPE_COUNT = 12;
    private static final float TFMGTWEAKS$STRIPE_DARK_FACTOR = 0.7F;
    private static final float TFMGTWEAKS$STRIPE_PHASE_OFFSET = 0.5F;
    private static final float TFMGTWEAKS$ROPE_BASE_COLOR = 0.1F;

    // Attach-point offsets, X/Y/Z per (start, end) per rope per variant.
    // t=0/"start" is inferred to be the lower/mechanism-side anchor and
    // t=1/"end" the higher one -- not visually confirmed, so verify
    // against what you see and swap if start/end read backwards. X/Z are
    // relative to the pump jack's own facing. All default to 0 (stock
    // placement). CRANK_LINK_*_X is for one of the two crank ropes; the
    // other gets X negated automatically (mirror images of each other).
    private static final float TFMGTWEAKS$CRANK_LINK_START_OFFSET_X = 0.0F;
    private static final float TFMGTWEAKS$CRANK_LINK_START_OFFSET_Y = 0.0F;
    private static final float TFMGTWEAKS$CRANK_LINK_START_OFFSET_Z = 0.0F;
    private static final float TFMGTWEAKS$CRANK_LINK_END_OFFSET_X = 0.0F;
    private static final float TFMGTWEAKS$CRANK_LINK_END_OFFSET_Y = 0.0F;
    private static final float TFMGTWEAKS$CRANK_LINK_END_OFFSET_Z = 0.0F;
    private static final float TFMGTWEAKS$CRANK_LINK_START_OFFSET_X_LARGE = 0.0F;
    private static final float TFMGTWEAKS$CRANK_LINK_START_OFFSET_Y_LARGE = 0.0F;
    private static final float TFMGTWEAKS$CRANK_LINK_START_OFFSET_Z_LARGE = 0.0F;
    private static final float TFMGTWEAKS$CRANK_LINK_END_OFFSET_X_LARGE = 0.0F;
    private static final float TFMGTWEAKS$CRANK_LINK_END_OFFSET_Y_LARGE = 0.0F;
    private static final float TFMGTWEAKS$CRANK_LINK_END_OFFSET_Z_LARGE = 0.0F;
    private static final float TFMGTWEAKS$HEAD_LINK_START_OFFSET_X = 0.0F;
    private static final float TFMGTWEAKS$HEAD_LINK_START_OFFSET_Y = 0.0F;
    private static final float TFMGTWEAKS$HEAD_LINK_START_OFFSET_Z = 0.0F;
    private static final float TFMGTWEAKS$HEAD_LINK_END_OFFSET_X = 0.0F;
    private static final float TFMGTWEAKS$HEAD_LINK_END_OFFSET_Y = 0.0F;
    private static final float TFMGTWEAKS$HEAD_LINK_END_OFFSET_Z = 0.0F;
    private static final float TFMGTWEAKS$HEAD_LINK_START_OFFSET_X_LARGE = 0.0F;
    private static final float TFMGTWEAKS$HEAD_LINK_START_OFFSET_Y_LARGE = 0.0F;
    private static final float TFMGTWEAKS$HEAD_LINK_START_OFFSET_Z_LARGE = 0.0F;
    private static final float TFMGTWEAKS$HEAD_LINK_END_OFFSET_X_LARGE = 0.0F;
    private static final float TFMGTWEAKS$HEAD_LINK_END_OFFSET_Y_LARGE = 0.0F;
    private static final float TFMGTWEAKS$HEAD_LINK_END_OFFSET_Z_LARGE = 0.0F;

    // Handoff from the @Inject handlers below to the static addVertexPair
    // redirect, which has no block entity parameter of its own. Safe as
    // static: render calls never interleave.
    @Unique
    private static float tfmgtweaks$activeStartOffsetX = 0.0F;
    @Unique
    private static float tfmgtweaks$activeStartOffsetY = 0.0F;
    @Unique
    private static float tfmgtweaks$activeStartOffsetZ = 0.0F;
    @Unique
    private static float tfmgtweaks$activeEndOffsetX = 0.0F;
    @Unique
    private static float tfmgtweaks$activeEndOffsetY = 0.0F;
    @Unique
    private static float tfmgtweaks$activeEndOffsetZ = 0.0F;

    @Unique
    private static boolean tfmgtweaks$isLargeVariant(PumpjackBlockEntity be, BlockPos pos) {
        if (pos == null || be.getLevel() == null) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(be.getLevel().getBlockState(pos).getBlock());
        return id.getPath().startsWith("large_pumpjack");
    }

    // renderPumpjackLink draws the crank-to-hammer rope (called twice per
    // frame, once per side).
    @Inject(
        method = "renderPumpjackLink",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderType;leash()Lnet/minecraft/client/renderer/RenderType;"))
    private void tfmgtweaks$prepareCrankLinkOffsets(boolean second, PoseStack pMatrixStack, MultiBufferSource pBuffer,
                                                      PumpjackBlockEntity be, CallbackInfo ci) {
        boolean large = tfmgtweaks$isLargeVariant(be, be.connectorPosition);
        // the crank rope is drawn twice, once per side (second=false/true) --
        // they're mirror images of each other, so X gets flipped for the
        // second one instead of applying the same value to both.
        float mirrorX = second ? -1.0F : 1.0F;
        tfmgtweaks$activeStartOffsetX = mirrorX * (large ? TFMGTWEAKS$CRANK_LINK_START_OFFSET_X_LARGE : TFMGTWEAKS$CRANK_LINK_START_OFFSET_X);
        tfmgtweaks$activeStartOffsetY = large ? TFMGTWEAKS$CRANK_LINK_START_OFFSET_Y_LARGE : TFMGTWEAKS$CRANK_LINK_START_OFFSET_Y;
        tfmgtweaks$activeStartOffsetZ = large ? TFMGTWEAKS$CRANK_LINK_START_OFFSET_Z_LARGE : TFMGTWEAKS$CRANK_LINK_START_OFFSET_Z;
        tfmgtweaks$activeEndOffsetX = mirrorX * (large ? TFMGTWEAKS$CRANK_LINK_END_OFFSET_X_LARGE : TFMGTWEAKS$CRANK_LINK_END_OFFSET_X);
        tfmgtweaks$activeEndOffsetY = large ? TFMGTWEAKS$CRANK_LINK_END_OFFSET_Y_LARGE : TFMGTWEAKS$CRANK_LINK_END_OFFSET_Y;
        tfmgtweaks$activeEndOffsetZ = large ? TFMGTWEAKS$CRANK_LINK_END_OFFSET_Z_LARGE : TFMGTWEAKS$CRANK_LINK_END_OFFSET_Z;
    }

    // renderFrontPumpjackLink draws the hammer-to-head rope.
    @Inject(
        method = "renderFrontPumpjackLink",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderType;leash()Lnet/minecraft/client/renderer/RenderType;"))
    private void tfmgtweaks$prepareHeadLinkOffsets(PoseStack pMatrixStack, MultiBufferSource pBuffer,
                                                     PumpjackBlockEntity be, CallbackInfo ci) {
        boolean large = tfmgtweaks$isLargeVariant(be, be.headPosition);
        tfmgtweaks$activeStartOffsetX = large ? TFMGTWEAKS$HEAD_LINK_START_OFFSET_X_LARGE : TFMGTWEAKS$HEAD_LINK_START_OFFSET_X;
        tfmgtweaks$activeStartOffsetY = large ? TFMGTWEAKS$HEAD_LINK_START_OFFSET_Y_LARGE : TFMGTWEAKS$HEAD_LINK_START_OFFSET_Y;
        tfmgtweaks$activeStartOffsetZ = large ? TFMGTWEAKS$HEAD_LINK_START_OFFSET_Z_LARGE : TFMGTWEAKS$HEAD_LINK_START_OFFSET_Z;
        tfmgtweaks$activeEndOffsetX = large ? TFMGTWEAKS$HEAD_LINK_END_OFFSET_X_LARGE : TFMGTWEAKS$HEAD_LINK_END_OFFSET_X;
        tfmgtweaks$activeEndOffsetY = large ? TFMGTWEAKS$HEAD_LINK_END_OFFSET_Y_LARGE : TFMGTWEAKS$HEAD_LINK_END_OFFSET_Y;
        tfmgtweaks$activeEndOffsetZ = large ? TFMGTWEAKS$HEAD_LINK_END_OFFSET_Z_LARGE : TFMGTWEAKS$HEAD_LINK_END_OFFSET_Z;
    }

    @Redirect(
        method = {"renderPumpjackLink", "renderFrontPumpjackLink"},
        at = @At(value = "INVOKE",
            target = "Lcom/drmangotea/tfmg/content/machinery/oil_processing/pumpjack/hammer/PumpjackRenderer;"
                + "addVertexPair(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lorg/joml/Matrix4f;FFFIIIIFFFFIZ)V"))
    private static void tfmgtweaks$smoothThickenAndCrossRope(VertexConsumer vertexConsumer, Matrix4f matrix,
                                                               float x, float y, float z,
                                                               int light1, int light2, int light3, int light4,
                                                               float widthA, float widthB,
                                                               float nudgeX, float nudgeZ,
                                                               int index, boolean reverse) {
        float scaledWidthA = widthA * TFMGTWEAKS$ROPE_THICKNESS_MULTIPLIER;
        float scaledWidthB = widthB * TFMGTWEAKS$ROPE_THICKNESS_MULTIPLIER;
        float scaledNudgeX = nudgeX * TFMGTWEAKS$ROPE_THICKNESS_MULTIPLIER;
        float scaledNudgeZ = nudgeZ * TFMGTWEAKS$ROPE_THICKNESS_MULTIPLIER;
        // ribbon 2's nudge is ribbon 1's nudge rotated 90 degrees
        float crossNudgeX = scaledNudgeZ;
        float crossNudgeZ = -scaledNudgeX;

        // t-range this call is responsible for (see the class doc for why).
        float tEnd = index / 24.0F;
        boolean isEndpoint = reverse ? (index == 24) : (index == 0);
        int steps = isEndpoint ? 1 : TFMGTWEAKS$RESOLUTION;
        float tStart = reverse ? (index + 1) / 24.0F : (index - 1) / 24.0F;

        for (int s = 1; s <= steps; s++) {
            float t = isEndpoint ? tEnd : Mth.lerp((float) s / steps, tStart, tEnd);

            int packedI = (int) Mth.lerp(t, (float) light1, (float) light2);
            int packedJ = (int) Mth.lerp(t, (float) light3, (float) light4);
            int packedLight = LightTexture.pack(packedI, packedJ);

            float curveX = x * t;
            float curveY = y > 0.0F ? y * t * t : y - y * (1.0F - t) * (1.0F - t);
            float curveZ = z * t;
            // start (t=0) / end (t=1) attach-point offset, blended linearly
            curveX += Mth.lerp(t, tfmgtweaks$activeStartOffsetX, tfmgtweaks$activeEndOffsetX);
            curveY += Mth.lerp(t, tfmgtweaks$activeStartOffsetY, tfmgtweaks$activeEndOffsetY);
            curveZ += Mth.lerp(t, tfmgtweaks$activeStartOffsetZ, tfmgtweaks$activeEndOffsetZ);

            float colorA = tfmgtweaks$stripeColor(t, 0.0F);
            float colorB = tfmgtweaks$stripeColor(t, TFMGTWEAKS$STRIPE_PHASE_OFFSET);

            tfmgtweaks$emitPair(vertexConsumer, matrix, curveX, curveY, curveZ,
                    scaledWidthA, scaledWidthB, scaledNudgeX, scaledNudgeZ, packedLight, colorA);
            tfmgtweaks$emitPair(vertexConsumer, matrix, curveX, curveY, curveZ,
                    scaledWidthA, scaledWidthB, crossNudgeX, crossNudgeZ, packedLight, colorB);
        }
    }

    private static float tfmgtweaks$stripeColor(float t, float phaseOffset) {
        int stripeIndex = (int) Math.floor(t * TFMGTWEAKS$STRIPE_COUNT + phaseOffset);
        boolean dark = Math.floorMod(stripeIndex, 2) == 1;
        return TFMGTWEAKS$ROPE_BASE_COLOR * (dark ? TFMGTWEAKS$STRIPE_DARK_FACTOR : 1.0F);
    }

    private static void tfmgtweaks$emitPair(VertexConsumer vertexConsumer, Matrix4f matrix,
                                             float curveX, float curveY, float curveZ,
                                             float widthA, float widthB, float nudgeX, float nudgeZ,
                                             int packedLight, float color) {
        vertexConsumer.addVertex(matrix, curveX - nudgeX, curveY + widthB, curveZ + nudgeZ)
                .setColor(color, color, color, 1.0F)
                .setLight(packedLight);
        vertexConsumer.addVertex(matrix, curveX + nudgeX, curveY + widthA - widthB, curveZ - nudgeZ)
                .setColor(color, color, color, 1.0F)
                .setLight(packedLight);
    }
}
