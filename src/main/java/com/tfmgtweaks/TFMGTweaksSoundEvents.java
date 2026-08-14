package com.tfmgtweaks;

import com.drmangotea.tfmg.registry.TFMGSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

/**
 * The electric_hum, generator_hum, switch_on, and
 * switch_off sound events were removed during TFMG's 1.0 -> 1.2 rewrite
 * (only affecting generators, rotors, transformers, switches -- engines
 * kept their sound code).
 *
 * Reuses TFMG's own TFMGSoundEvents registry API rather than a separate
 * sound system. Audio and sounds.json/lang entries recovered from TFMG
 * 1.0.2f, shipped as resource overrides under assets/tfmg/.
 *
 * Timing: TFMG's own constructor calls TFMGSoundEvents.prepare()
 * synchronously before ours runs, so each entry here calls prepare() on
 * itself individually rather than relying on that bulk pass. TFMG's
 * RegisterEvent listener then picks these up automatically, since ALL is
 * a shared, mutable map.
 */
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
