package com.tfmgtweaks.content.polarizer;

import com.drmangotea.tfmg.base.TFMGUtils;
import com.tfmgtweaks.TFMGTweaks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent;

/** Converts lightning-struck magnetic alloy into 1 + random(count) magnets and cancels TFMG's own handler. */
@EventBusSubscriber(modid = TFMGTweaks.MOD_ID)
public class MagneticAlloyLightningFix {
    private static final ResourceLocation MAGNETIC_ALLOY_INGOT_ID =
            ResourceLocation.fromNamespaceAndPath("tfmg", "magnetic_alloy_ingot");
    private static final ResourceLocation MAGNET_ID =
            ResourceLocation.fromNamespaceAndPath("tfmg", "magnet");

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onStruckByLightning(EntityStruckByLightningEvent event) {
        if (!(event.getEntity() instanceof ItemEntity entity)) {
            return;
        }
        Item magneticAlloyIngot = BuiltInRegistries.ITEM.get(MAGNETIC_ALLOY_INGOT_ID);
        if (magneticAlloyIngot == Items.AIR || !entity.getItem().is(magneticAlloyIngot)) {
            return;
        }
        Item magnet = BuiltInRegistries.ITEM.get(MAGNET_ID);
        if (magnet == Items.AIR) {
            return;
        }
        int count = entity.getItem().getCount();
        int magnetsProduced = 1 + entity.level().random.nextInt(count);
        entity.setItem(new ItemStack(magnet, magnetsProduced));
        TFMGUtils.spawnElectricParticles(entity.level(), entity.blockPosition());
        event.setCanceled(true);
    }
}
