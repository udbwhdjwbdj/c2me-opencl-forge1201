package com.ishland.c2me.opts.dfc.common.ast.noise;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.AstTransformer;
import java.util.Objects;
import net.minecraft.world.level.levelgen.DensityFunction.NoiseHolder;

public class GenericShiftedNoiseNode implements AstNode {
   public final AstNode inputX;
   public final AstNode inputY;
   public final AstNode inputZ;
   public final NoiseHolder noise;

   public GenericShiftedNoiseNode(AstNode inputX, AstNode inputY, AstNode inputZ, NoiseHolder noise) {
      this.inputX = Objects.requireNonNull(inputX);
      this.inputY = Objects.requireNonNull(inputY);
      this.inputZ = Objects.requireNonNull(inputZ);
      this.noise = Objects.requireNonNull(noise);
   }

   @Override
   public AstNode[] getChildren() {
      return new AstNode[]{this.inputX, this.inputY, this.inputZ};
   }

   @Override
   public AstNode transform(AstTransformer transformer) {
      AstNode inputX = this.inputX.transform(transformer);
      AstNode inputY = this.inputY.transform(transformer);
      AstNode inputZ = this.inputZ.transform(transformer);
      return inputX == this.inputX && inputY == this.inputY && inputZ == this.inputZ
         ? transformer.transform(this)
         : transformer.transform(new GenericShiftedNoiseNode(inputX, inputY, inputZ, this.noise));
   }

   @Override
   public boolean equals(Object o) {
      if (o != null && this.getClass() == o.getClass()) {
         GenericShiftedNoiseNode that = (GenericShiftedNoiseNode)o;
         return this.inputX.equals(that.inputX) && this.inputY.equals(that.inputY) && this.inputZ.equals(that.inputZ) && this.noise.equals(that.noise);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      int result = 1;
      result = 31 * result + this.inputX.hashCode();
      result = 31 * result + this.inputY.hashCode();
      result = 31 * result + this.inputZ.hashCode();
      return 31 * result + this.noise.hashCode();
   }

   @Override
   public boolean relaxedEquals(AstNode o) {
      if (o != null && this.getClass() == o.getClass()) {
         GenericShiftedNoiseNode that = (GenericShiftedNoiseNode)o;
         return this.inputX.relaxedEquals(that.inputX)
            && this.inputY.relaxedEquals(that.inputY)
            && this.inputZ.relaxedEquals(that.inputZ)
            && this.noise.equals(that.noise);
      } else {
         return false;
      }
   }

   @Override
   public int relaxedHashCode() {
      int result = 1;
      result = 31 * result + this.inputX.relaxedHashCode();
      result = 31 * result + this.inputY.relaxedHashCode();
      result = 31 * result + this.inputZ.relaxedHashCode();
      return 31 * result + this.noise.hashCode();
   }
}
