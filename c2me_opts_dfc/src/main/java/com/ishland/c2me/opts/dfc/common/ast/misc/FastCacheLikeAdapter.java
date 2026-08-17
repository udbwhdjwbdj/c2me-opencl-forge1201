package com.ishland.c2me.opts.dfc.common.ast.misc;

import com.ishland.c2me.opts.dfc.common.ast.EvalType;
import com.ishland.c2me.opts.dfc.common.ducks.IFastCacheLike;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions.MarkerOrMarked;

/**
 * Adapter exposing the 1.20.1 MarkerOrMarked through the dfc IFastCacheLike duck
 * while preserving the original marker type information for the OpenCL emitter.
 */
public class FastCacheLikeAdapter implements IFastCacheLike, MarkerOrMarked {

   private final MarkerOrMarked wrapped;

   public FastCacheLikeAdapter(MarkerOrMarked wrapped) {
      this.wrapped = wrapped;
   }

   @Override
   public net.minecraft.world.level.levelgen.DensityFunctions.Marker.Type type() {
      return this.wrapped.type();
   }

   @Override
   public DensityFunction wrapped() {
      return this.wrapped.wrapped();
   }

   @Override
   public double c2me$getCached(int var1, int var2, int var3, EvalType var4) {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean c2me$getCached(double[] var1, int[] var2, int[] var3, int[] var4, EvalType var5) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void c2me$cache(int var1, int var2, int var3, EvalType var4, double var5) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void c2me$cache(double[] var1, int[] var2, int[] var3, int[] var4, EvalType var5) {
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean c2me$isActualCache() {
      return true;
   }

   @Override
   public String c2me$describeCacheLike() {
      return "FastCacheLikeAdapter(" + this.wrapped.getClass().getName() + ")";
   }

   @Override
   public DensityFunction c2me$getDelegate() {
      return this.wrapped.wrapped();
   }

   @Override
   public DensityFunction c2me$withDelegate(DensityFunction var1) {
      return new FastCacheLikeAdapter(new DelegatingMarker(this.wrapped.type(), var1));
   }

   @Override
   public double compute(DensityFunction.FunctionContext var1) {
      return this.wrapped.compute(var1);
   }

   @Override
   public void fillArray(double[] var1, DensityFunction.ContextProvider var2) {
      this.wrapped.fillArray(var1, var2);
   }

   @Override
   public DensityFunction mapAll(DensityFunction.Visitor var1) {
      return this.wrapped.mapAll(var1);
   }

   @Override
   public double minValue() {
      return this.wrapped.minValue();
   }

   @Override
   public double maxValue() {
      return this.wrapped.maxValue();
   }

   @Override
   public KeyDispatchDataCodec<? extends DensityFunction> codec() {
      return this.wrapped.codec();
   }

   private static final class DelegatingMarker implements MarkerOrMarked {
      private final net.minecraft.world.level.levelgen.DensityFunctions.Marker.Type type;
      private final DensityFunction wrapped;

      private DelegatingMarker(net.minecraft.world.level.levelgen.DensityFunctions.Marker.Type type, DensityFunction wrapped) {
         this.type = type;
         this.wrapped = wrapped;
      }

      @Override
      public net.minecraft.world.level.levelgen.DensityFunctions.Marker.Type type() {
         return this.type;
      }

      @Override
      public DensityFunction wrapped() {
         return this.wrapped;
      }

      @Override
      public double compute(DensityFunction.FunctionContext var1) {
         return this.wrapped.compute(var1);
      }

      @Override
      public void fillArray(double[] var1, DensityFunction.ContextProvider var2) {
         this.wrapped.fillArray(var1, var2);
      }

      @Override
      public double minValue() {
         return this.wrapped.minValue();
      }

      @Override
      public double maxValue() {
         return this.wrapped.maxValue();
      }
   }
}
