package com.tfmgtweaks.sound;

import com.drmangotea.tfmg.content.electricity.generators.large_generator.RotorBlockEntity;
import com.tfmgtweaks.TFMGTweaksSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

/** Holds the client-only sound code, so RotorBlockEntityMixin never loads client classes on a server. */
public class RotorSoundClientHelper {
    private static final Map<BlockPos, RotorHumSoundInstance> ACTIVE = new HashMap<>();

    public static void tick(RotorBlockEntity rotor) {
        if (rotor.getSpeed() == 0) {
            return;
        }
        BlockPos pos = rotor.getBlockPos();
        RotorHumSoundInstance existing = ACTIVE.get(pos);
        if (existing != null && existing.isActivelyPlaying()) {
            // Already playing; volume and pitch update on their own.
            return;
        }
        RotorHumSoundInstance instance =
                new RotorHumSoundInstance(TFMGTweaksSoundEvents.GENERATOR_HUM.getMainEvent(), rotor);
        ACTIVE.put(pos, instance);
        Minecraft.getInstance().getSoundManager().play(instance);
    }
}
