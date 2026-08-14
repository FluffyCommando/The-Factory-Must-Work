package com.tfmgtweaks;

import com.drmangotea.tfmg.base.TFMGCreativeTabs;
import com.mojang.logging.LogUtils;
import com.tfmgtweaks.config.TFMGTweaksConfig;
import com.tfmgtweaks.content.boiler.BoilerBlockEntity;
import com.tfmgtweaks.registry.TFMGTweaksBlockEntities;
import com.tfmgtweaks.registry.TFMGTweaksBlocks;
import com.tfmgtweaks.registry.TFMGTweaksFluids;
import com.tfmgtweaks.registry.TFMGTweaksItems;
import com.tfmgtweaks.worldgen.TFMGTweaksFeatures;
import com.tfmgtweaks.worldgen.TFMGTweaksPlacementModifiers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.slf4j.Logger;

@Mod(TFMGTweaks.MOD_ID)
public class TFMGTweaks {

    public static final String MOD_ID = "tfmgtweaks";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TFMGTweaks(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Create: The Factory Must WORK initializing");

        TFMGTweaksSoundEvents.init();

        TFMGTweaksBlocks.BLOCKS.register(modEventBus);
        TFMGTweaksItems.ITEMS.register(modEventBus);
        TFMGTweaksBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        TFMGTweaksFeatures.FEATURES.register(modEventBus);
        TFMGTweaksPlacementModifiers.PLACEMENT_MODIFIERS.register(modEventBus);
        TFMGTweaksFluids.FLUID_TYPES.register(modEventBus);
        TFMGTweaksFluids.FLUIDS.register(modEventBus);

        modEventBus.addListener(this::registerCapabilities);
        modEventBus.addListener(this::buildCreativeModeTabContents);

        modContainer.registerConfig(ModConfig.Type.COMMON, TFMGTweaksConfig.SPEC);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        BoilerBlockEntity.registerCapabilities(event);
    }

    /**
     * TFMG's own creative tab population (TFMGCreativeTabs.addCreative())
     * only iterates Registrate-managed items (REGISTRATE.getAll(...)),
     * since that's how TFMG registers its own content -- our own items,
     * registered via plain NeoForge DeferredRegister rather than
     * Registrate, would never be picked up by that loop automatically.
     * Subscribing to the same event ourselves and adding our items when
     * TFMG's own main tab is being built is the correct way to appear
     * there instead. TFMGCreativeTabs.TFMG_MAIN itself is a plain
     * DeferredHolder<CreativeModeTab, CreativeModeTab> (not a
     * Registrate-typed field), so referencing it directly is safe.
     */
    private void buildCreativeModeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTab() != TFMGCreativeTabs.TFMG_MAIN.get()) {
            return;
        }
        event.accept(TFMGTweaksItems.OIL_ROCK.get());
        event.accept(TFMGTweaksItems.BOILER.get());
        event.accept(TFMGTweaksItems.STEAM_BUCKET.get());
    }
}
