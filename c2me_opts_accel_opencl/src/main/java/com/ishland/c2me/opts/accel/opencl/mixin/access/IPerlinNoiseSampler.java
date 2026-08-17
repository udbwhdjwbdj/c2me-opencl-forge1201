package com.ishland.c2me.opts.accel.opencl.mixin.access;

import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({ImprovedNoise.class})
public interface IPerlinNoiseSampler {
   @Accessor("p")
   byte[] getPermutation();
}
