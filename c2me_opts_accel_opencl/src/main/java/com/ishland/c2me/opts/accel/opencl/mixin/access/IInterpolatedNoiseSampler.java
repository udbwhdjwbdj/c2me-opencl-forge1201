package com.ishland.c2me.opts.accel.opencl.mixin.access;

import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({BlendedNoise.class})
public interface IInterpolatedNoiseSampler {
   @Accessor("minLimitNoise")
   PerlinNoise getLowerInterpolatedNoise();

   @Accessor("maxLimitNoise")
   PerlinNoise getUpperInterpolatedNoise();

   @Accessor("mainNoise")
   PerlinNoise getInterpolationNoise();

   @Accessor("xzMultiplier")
   double getScaledXzScale();

   @Accessor("yMultiplier")
   double getScaledYScale();

   @Accessor("xzFactor")
   double getXzFactor();

   @Accessor("yFactor")
   double getYFactor();

   @Accessor("smearScaleMultiplier")
   double getSmearScaleMultiplier();

   @Accessor("maxValue")
   double getMaxValue();

   @Accessor("xzScale")
   double getXzScale();

   @Accessor("yScale")
   double getYScale();
}
