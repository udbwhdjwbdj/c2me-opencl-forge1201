package com.ishland.c2me.opts.accel.opencl.mixin.access;

import net.minecraft.world.level.levelgen.DensityFunctions.EndIslandDensityFunction;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({EndIslandDensityFunction.class})
public interface IDensityFunctionTypesEndIslands {
   @Accessor("islandNoise")
   SimplexNoise getSampler();
}
