package com.tfmgtweaks;

import com.drmangotea.tfmg.registry.TFMGSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

/** Restores sound events removed in TFMG's 1.0 -> 1.2 rewrite, using TFMG's own sound registry. */
public class TFMGTweaksSoundEvents {
    public static TFMGSoundEvents.SoundEntry ELECTRIC_HUM;
    public static TFMGSoundEvents.SoundEntry GENERATOR_HUM;
    public static TFMGSoundEvents.SoundEntry SWITCH_ON;
    public static TFMGSoundEvents.SoundEntry SWITCH_OFF;

    public static void init() {
        ELECTRIC_HUM = register("electric_hum", "Electric hum");
        GENERATOR_HUM = register("generator_hum", "Generator hum");
        SWITCH_ON = register("switch_on", "Switch closing");
        SWITCH_OFF = register("switch_off", "Switch opening");
    }

    private static TFMGSoundEvents.SoundEntry register(String path, String subtitle) {
        TFMGSoundEvents.SoundEntry entry = TFMGSoundEvents.create(ResourceLocation.fromNamespaceAndPath("tfmg", path))
                .subtitle(subtitle)
                .category(SoundSource.BLOCKS)
                .attenuationDistance(16)
                .build();
        entry.prepare();
        return entry;
    }
}
