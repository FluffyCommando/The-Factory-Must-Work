package com.tfmgtweaks.registry;

import com.tfmgtweaks.TFMGTweaks;
import com.tfmgtweaks.content.boiler.BoilerBlockEntity;
import com.tfmgtweaks.content.oilrock.OilRockBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class TFMGTweaksBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TFMGTweaks.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OilRockBlockEntity>> OIL_ROCK =
            BLOCK_ENTITIES.register("oil_rock", () -> BlockEntityType.Builder.of(
                    OilRockBlockEntity::new, TFMGTweaksBlocks.OIL_ROCK.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BoilerBlockEntity>> BOILER =
            BLOCK_ENTITIES.register("boiler", () -> BlockEntityType.Builder.of(
                    BoilerBlockEntity::new, TFMGTweaksBlocks.BOILER.get()).build(null));
}
