package com.tfmgtweaks;

import com.drmangotea.tfmg.content.machinery.oil_processing.pumpjack.base.PumpjackBaseBlockEntity;
import com.tfmgtweaks.pumpjack.PumpjackFaceIconRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Dist.CLIENT-scoped so this class (and the client-only rendering types it
 * imports, like BlockEntityRenderer) is never loaded at all on a dedicated
 * server -- registering a block entity renderer is inherently a
 * client-only concern.
 */
@EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class TFMGTweaksClient {

    @SuppressWarnings("unchecked")
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        BlockEntityType<PumpjackBaseBlockEntity> pumpjackType = (BlockEntityType<PumpjackBaseBlockEntity>)
                BuiltInRegistries.BLOCK_ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath("tfmg", "pumpjack_base"));
        event.registerBlockEntityRenderer(pumpjackType, PumpjackFaceIconRenderer::new);
    }
}
