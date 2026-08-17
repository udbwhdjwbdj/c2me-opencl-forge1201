package com.ishland.c2me.opts.accel.opencl.mixin.access;

import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({NormalNoise.class})
public interface IDoublePerlinNoiseSampler {
   @Accessor("first")
   PerlinNoise getFirstSampler();

   @Accessor("second")
   PerlinNoise getSecondSampler();

   @Accessor("valueFactor")
   double getAmplitude();
}
