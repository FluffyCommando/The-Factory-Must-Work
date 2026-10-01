package com.tfmgtweaks.airintake;

import com.drmangotea.tfmg.content.machinery.misc.air_intake.AirIntakeBlockEntity;
import com.tfmgtweaks.api.ITFMGTweaksAirIntakeGasMode;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Wrenching an Air Intake cycles its gas mode for the whole group.
 * The back face is skipped so TFMG's own shaft toggle still works.
 */
@EventBusSubscriber
public class AirIntakeWrenchInteractionHandler {
    @SubscribeEvent
    public static void onRightClickAirIntake(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (!(level.getBlockEntity(event.getPos()) instanceof AirIntakeBlockEntity clicked)) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (!stack.is(ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "tools/wrench")))) {
            return;
        }

        Direction clickedFace = event.getFace();
        BlockState state = level.getBlockState(event.getPos());
        if (clickedFace != null && state.hasProperty(DirectionalBlock.FACING)
                && clickedFace == state.getValue(DirectionalBlock.FACING).getOpposite()) {
            return;
        }

        event.setCanceled(true);

        if (level.isClientSide) {
            return;
        }

        AirIntakeBlockEntity controller = clicked.controller != null
                && level.getBlockEntity(clicked.controller) instanceof AirIntakeBlockEntity controllerBE
                ? controllerBE
                : clicked;

        ITFMGTweaksAirIntakeGasMode controllerGas = (ITFMGTweaksAirIntakeGasMode) controller;
        AirIntakeGasMode newMode = controllerGas.tfmgtweaks$getGasMode().next();

        controllerGas.tfmgtweaks$setGasMode(newMode);
        for (AirIntakeBlockEntity member : controller.blockEntities) {
            ((ITFMGTweaksAirIntakeGasMode) member).tfmgtweaks$setGasMode(newMode);
        }

        Player player = event.getEntity();
        if (player != null) {
            player.displayClientMessage(
                    Component.translatable("tfmgtweaks.air_intake.mode_set", newMode.displayName())
                            .withStyle(ChatFormatting.GRAY),
                    true);
        }
    }
}

