package com.tfmgtweaks.sound;

import com.drmangotea.tfmg.content.electricity.generators.large_generator.RotorBlockEntity;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

/** Looping rotor hum whose volume and pitch follow the rotor's speed. Client-only. */
public class RotorHumSoundInstance extends AbstractTickableSoundInstance {
    private final RotorBlockEntity rotor;
    private final BlockPos pos;
    private boolean activelyPlaying = true;

    public RotorHumSoundInstance(SoundEvent event, RotorBlockEntity rotor) {
        super(event, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
        this.rotor = rotor;
        this.pos = rotor.getBlockPos().immutable();
        this.looping = true;
        this.delay = 0;
        this.relative = false;
    }

    /** False once stopped, so a new instance is started if the rotor spins up again. */
    public boolean isActivelyPlaying() {
        return activelyPlaying;
    }

    @Override
    public void tick() {
        if (!activelyPlaying) {
            return;
        }
        if (rotor.isRemoved() || rotor.getLevel() == null || rotor.getSpeed() == 0) {
            activelyPlaying = false;
            stop();
        }
    }

    @Override
    public float getVolume() {
        return Mth.clamp(Math.abs(rotor.getSpeed()) / 128f, 0.1f, 0.7f);
    }

    @Override
    public float getPitch() {
        return Mth.clamp((Math.abs(rotor.getSpeed()) / 256f) + 0.5f, 0.6f, 1.4f);
    }

    @Override
    public double getX() {
        return pos.getX() + 0.5;
    }

    @Override
    public double getY() {
        return pos.getY() + 0.5;
    }

    @Override
    public double getZ() {
        return pos.getZ() + 0.5;
    }
}
