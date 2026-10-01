package com.tfmgtweaks.pumpjack;

import com.drmangotea.tfmg.content.machinery.oil_processing.pumpjack.base.PumpjackBaseBlockEntity;
import com.tfmgtweaks.api.ITFMGTweaksPumpjackFluidCapability;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Wrenching a pump jack base face cycles its role (NONE -> OIL -> WASTE -> STEAM).
 * The change happens server-side and reaches the client through block entity sync.
 */
@EventBusSubscriber
public class PumpjackWrenchInteractionHandler {
    @SubscribeEvent
    public static void onRightClickPumpjack(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockEntity be = level.getBlockEntity(event.getPos());
        if (!(be instanceof PumpjackBaseBlockEntity pumpjack)) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (!stack.is(ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "tools/wrench")))) {
            return;
        }

        event.setCanceled(true);

        if (level.isClientSide) {
            return;
        }

        PumpjackFrackingWrapper core = ((ITFMGTweaksPumpjackFluidCapability) pumpjack).tfmgtweaks$getFrackingCore();
        if (core == null) {
            return;
        }

        PumpjackFrackingWrapper.FaceRole newRole = core.cycleRole(event.getFace());

        Player player = event.getEntity();
        if (player != null) {
            String message = newRole == PumpjackFrackingWrapper.FaceRole.NONE
                    ? "Side unassigned"
                    : "Side set to " + newRole.name().toLowerCase();
            player.displayClientMessage(Component.literal(message).withStyle(ChatFormatting.GRAY), true);
        }
    }
}
