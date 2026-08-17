package com.ishland.c2me.opts.dfc.common.gen.backports;

import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunction.ContextProvider;
import net.minecraft.world.level.levelgen.DensityFunction.FunctionContext;
import net.minecraft.world.level.levelgen.DensityFunction.SinglePointContext;
import net.minecraft.world.level.levelgen.DensityFunction.Visitor;

public record FindTopSurface(DensityFunction delegate, DensityFunction upperBound, int lowerBound, int cellHeight) implements DensityFunction {
   public double compute(FunctionContext pos) {
      int topY = Mth.floor(this.upperBound.compute(pos) / (double)this.cellHeight) * this.cellHeight;
      if (topY <= this.lowerBound) {
         return (double)this.lowerBound;
      } else {
         for (int blockY = topY; blockY >= this.lowerBound; blockY -= this.cellHeight) {
            if (this.delegate.compute(new SinglePointContext(pos.blockX(), blockY, pos.blockZ())) > 0.0) {
               return (double)blockY;
            }
         }

         return (double)this.lowerBound;
      }
   }

   public void fillArray(double[] densities, ContextProvider applier) {
      applier.fillAllDirectly(densities, this);
   }

   public DensityFunction mapAll(Visitor visitor) {
      return visitor.apply(new FindTopSurface(this.delegate.mapAll(visitor), this.upperBound.mapAll(visitor), this.lowerBound, this.cellHeight));
   }

   public double minValue() {
      return (double)this.lowerBound;
   }

   public double maxValue() {
      return Math.max((double)this.lowerBound, this.upperBound.maxValue());
   }

   public KeyDispatchDataCodec<? extends DensityFunction> codec() {
      throw new UnsupportedOperationException();
   }
}
