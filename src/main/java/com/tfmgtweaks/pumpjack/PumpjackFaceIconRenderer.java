package com.tfmgtweaks.pumpjack;

import com.drmangotea.tfmg.content.machinery.oil_processing.pumpjack.base.PumpjackBaseBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tfmgtweaks.api.ITFMGTweaksPumpjackFluidCapability;
import com.tfmgtweaks.registry.TFMGTweaksItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Draws a floating bucket icon outside each pump jack face assigned a role. */
public class PumpjackFaceIconRenderer implements BlockEntityRenderer<PumpjackBaseBlockEntity> {
    public PumpjackFaceIconRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(PumpjackBaseBlockEntity be, float partialTick, PoseStack poseStack,
                        MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (!(be instanceof ITFMGTweaksPumpjackFluidCapability accessor)) {
            return;
        }
        PumpjackFrackingWrapper core = accessor.tfmgtweaks$getFrackingCore();
        if (core == null) {
            return;
        }

        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        for (Direction direction : Direction.values()) {
            PumpjackFrackingWrapper.FaceRole role = core.roleOf(direction);
            if (role == PumpjackFrackingWrapper.FaceRole.NONE) {
                continue;
            }
            ItemStack icon = iconFor(role);
            if (icon.isEmpty()) {
                continue;
            }

            poseStack.pushPose();
            double horizontalOffset = 0.36; // main column's actual half-width (0.3125) + a small gap
            double verticalOffset = 0.55;   // cap/base already reach the full block boundary + a small gap
            poseStack.translate(
                    0.5 + direction.getStepX() * horizontalOffset,
                    0.5 + direction.getStepY() * verticalOffset,
                    0.5 + direction.getStepZ() * horizontalOffset
            );
            applyFacingRotation(poseStack, direction);
            poseStack.scale(0.4f, 0.4f, 0.4f);
            itemRenderer.renderStatic(icon, ItemDisplayContext.FIXED, packedLight, packedOverlay,
                    poseStack, bufferSource, be.getLevel(), 0);
            poseStack.popPose();
        }
    }

    /** Rotates the icon to face outward from face. */
    private void applyFacingRotation(PoseStack poseStack, Direction direction) {
        switch (direction) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(270));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
            case UP -> poseStack.mulPose(Axis.XP.rotationDegrees(270));
            case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(90));
            case SOUTH -> {
            }
        }
    }

    private ItemStack iconFor(PumpjackFrackingWrapper.FaceRole role) {
        return switch (role) {
            case OIL -> new ItemStack(itemOrAir("tfmg", "crude_oil_bucket"));
            case WASTE -> new ItemStack(wasteIcon());
            case STEAM -> new ItemStack(TFMGTweaksItems.STEAM_BUCKET.get());
            case NONE -> ItemStack.EMPTY;
        };
    }

    private Item wasteIcon() {
        Item pollutedWaterBucket = itemOrAir("adpother", "polluted_water_bucket");
        return pollutedWaterBucket != Items.AIR ? pollutedWaterBucket : Items.WATER_BUCKET;
    }

    private Item itemOrAir(String namespace, String path) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(namespace, path));
    }
}
