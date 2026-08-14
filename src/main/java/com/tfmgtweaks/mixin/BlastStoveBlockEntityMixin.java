package com.tfmgtweaks.mixin;

import com.drmangotea.tfmg.content.machinery.metallurgy.blast_stove.BlastStoveBlockEntity;
import org.spongepowered.asm.mixin.Mixin;

/**
 * A Blast Stove multiblock built at 3x3 renders with only its 4 corner
 * blocks visible -- the rest are genuinely invisible. Traced to Create's
 * own "window" system for FluidTankBlockEntity (BlastStoveBlockEntity's
 * parent), which makes center/edge segments of a 3-wide tank see-through
 * so players can view fluid level -- the exact symptom shape, though the
 * precise mechanism wasn't conclusively pinned down since it's several
 * layers deep in inherited Create code.
 *
 * Rather than chase that further, this caps the structure at 2x2 --
 * Blast Stove clearly wasn't designed with 3x3 in mind (TFMG's own class
 * even has an unused, dead MAX_SIZE=2 constant never actually wired up).
 * Create's own multiblock-forming algorithm always searches up to
 * getMaxWidth(), so this also self-heals any existing broken 3x3
 * structure the next time it re-forms.
 */
@Mixin(BlastStoveBlockEntity.class)
public abstract class BlastStoveBlockEntityMixin {

    public int getMaxWidth() {
        return 2;
    }
}
