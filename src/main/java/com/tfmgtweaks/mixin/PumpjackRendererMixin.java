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

/** Draws thicker, twisted-looking pump jack ropes. */
@Mixin(PumpjackRenderer.class)
public abstract class PumpjackRendererMixin {
    // 6x vanilla lead width per ribbon.
    private static final float TFMGTWEAKS$ROPE_THICKNESS_MULTIPLIER = 6.0F;

    // Sub-segments drawn per original segment.
    private static final int TFMGTWEAKS$RESOLUTION = 4;

    private static final int TFMGTWEAKS$STRIPE_COUNT = 12;
    private static final float TFMGTWEAKS$STRIPE_DARK_FACTOR = 0.7F;
    private static final float TFMGTWEAKS$STRIPE_PHASE_OFFSET = 0.5F;
    private static final float TFMGTWEAKS$ROPE_BASE_COLOR = 0.1F;

    // Rope attach-point offsets relative to the pump jack's facing; the second crank rope mirrors X.
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

    // Passes the block entity to the static addVertexPair redirect.
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

    // Crank-to-hammer rope, drawn once per side.
    @Inject(
        method = "renderPumpjackLink",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderType;leash()Lnet/minecraft/client/renderer/RenderType;"))
    private void tfmgtweaks$prepareCrankLinkOffsets(boolean second, PoseStack pMatrixStack, MultiBufferSource pBuffer,
                                                      PumpjackBlockEntity be, CallbackInfo ci) {
        boolean large = tfmgtweaks$isLargeVariant(be, be.connectorPosition);
        // The second crank rope mirrors the first, so X is flipped.
        float mirrorX = second ? -1.0F : 1.0F;
        tfmgtweaks$activeStartOffsetX = mirrorX * (large ? TFMGTWEAKS$CRANK_LINK_START_OFFSET_X_LARGE : TFMGTWEAKS$CRANK_LINK_START_OFFSET_X);
        tfmgtweaks$activeStartOffsetY = large ? TFMGTWEAKS$CRANK_LINK_START_OFFSET_Y_LARGE : TFMGTWEAKS$CRANK_LINK_START_OFFSET_Y;
        tfmgtweaks$activeStartOffsetZ = large ? TFMGTWEAKS$CRANK_LINK_START_OFFSET_Z_LARGE : TFMGTWEAKS$CRANK_LINK_START_OFFSET_Z;
        tfmgtweaks$activeEndOffsetX = mirrorX * (large ? TFMGTWEAKS$CRANK_LINK_END_OFFSET_X_LARGE : TFMGTWEAKS$CRANK_LINK_END_OFFSET_X);
        tfmgtweaks$activeEndOffsetY = large ? TFMGTWEAKS$CRANK_LINK_END_OFFSET_Y_LARGE : TFMGTWEAKS$CRANK_LINK_END_OFFSET_Y;
        tfmgtweaks$activeEndOffsetZ = large ? TFMGTWEAKS$CRANK_LINK_END_OFFSET_Z_LARGE : TFMGTWEAKS$CRANK_LINK_END_OFFSET_Z;
    }

    // Hammer-to-head rope.
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
        // Second ribbon is offset 90 degrees from the first.
        float crossNudgeX = scaledNudgeZ;
        float crossNudgeZ = -scaledNudgeX;

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
            // Attach-point offset, blended from start (t=0) to end (t=1).
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
