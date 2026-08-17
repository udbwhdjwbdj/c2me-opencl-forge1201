package com.ishland.c2me.opts.dfc.common.ast;

import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.DensityFunction.ContextProvider;
import net.minecraft.world.level.levelgen.DensityFunction.FunctionContext;

public enum EvalType {
   NORMAL,
   INTERPOLATION;

   public static EvalType from(FunctionContext pos) {
      return pos instanceof NoiseChunk ? INTERPOLATION : NORMAL;
   }

   public static EvalType from(ContextProvider applier) {
      return applier instanceof NoiseChunk ? INTERPOLATION : NORMAL;
   }
}
