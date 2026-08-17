package com.ishland.c2me.opts.accel.opencl.mixin.access;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({PerlinNoise.class})
public interface IOctavePerlinNoiseSampler {
   @Accessor("noiseLevels")
   ImprovedNoise[] getOctaveSamplers();

   @Accessor("amplitudes")
   DoubleList getAmplitudes();

   @Accessor("lowestFreqValueFactor")
   double getPersistence();

   @Accessor("lowestFreqInputFactor")
   double getLacunarity();
}
