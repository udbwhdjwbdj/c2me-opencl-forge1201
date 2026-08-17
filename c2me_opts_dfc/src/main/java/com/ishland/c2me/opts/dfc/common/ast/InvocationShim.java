package com.ishland.c2me.opts.dfc.common.ast;

import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunction.ContextProvider;
import net.minecraft.world.level.levelgen.DensityFunction.FunctionContext;
import net.minecraft.world.level.levelgen.DensityFunction.NoiseHolder;

public class InvocationShim {
   public static double invokeDensityFunctionSample(DensityFunction densityFunction, FunctionContext pos) {
      return densityFunction.compute(pos);
   }

   public static void invokeDensityFunctionFill(DensityFunction densityFunction, double[] densities, ContextProvider applier) {
      densityFunction.fillArray(densities, applier);
   }

   public static double invokeMathHelperClampedMap(double value, double oldStart, double oldEnd, double newStart, double newEnd) {
      return Mth.clampedMap(value, oldStart, oldEnd, newStart, newEnd);
   }

   public static double invokeDensityFunctionNoiseSample(NoiseHolder noise, double x, double y, double z) {
      return noise.getValue(x, y, z);
   }

   public static float invokeMathHelperLerp(float delta, float start, float end) {
      return Mth.lerp(delta, start, end);
   }

   public static int invokeFloor(double value) {
      return Mth.floor(value);
   }
}
