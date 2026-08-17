package com.ishland.c2me.opts.accel.opencl.common.ducks;

import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.Aquifer.FluidPicker;

public interface ChunkNoiseSamplerExtension {
   FluidPicker c2me$getFluidLevelSampler();

   NoiseGeneratorSettings c2me$getChunkGeneratorSettings();

   RandomState c2me$getNoiseConfig();
}
