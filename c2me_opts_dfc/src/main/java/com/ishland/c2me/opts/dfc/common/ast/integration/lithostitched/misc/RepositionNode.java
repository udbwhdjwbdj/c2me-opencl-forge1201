package com.ishland.c2me.opts.dfc.common.ast.integration.lithostitched.misc;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.AstTransformer;
import java.util.Objects;

public class RepositionNode implements AstNode {
   public final AstNode input;
   public final AstNode inputX;
   public final AstNode inputY;
   public final AstNode inputZ;

   public RepositionNode(AstNode input, AstNode inputX, AstNode inputY, AstNode inputZ) {
      this.input = Objects.requireNonNull(input);
      this.inputX = Objects.requireNonNull(inputX);
      this.inputY = Objects.requireNonNull(inputY);
      this.inputZ = Objects.requireNonNull(inputZ);
   }

   @Override
   public AstNode[] getChildren() {
      return new AstNode[]{this.input, this.inputX, this.inputY, this.inputZ};
   }

   @Override
   public AstNode transform(AstTransformer transformer) {
      AstNode input = this.input.transform(transformer);
      AstNode inputX = this.inputX.transform(transformer);
      AstNode inputY = this.inputY.transform(transformer);
      AstNode inputZ = this.inputZ.transform(transformer);
      return input == this.input && inputX == this.inputX && inputY == this.inputY && inputZ == this.inputZ
         ? transformer.transform(this)
         : transformer.transform(new RepositionNode(input, inputX, inputY, inputZ));
   }

   @Override
   public boolean equals(Object o) {
      if (o != null && this.getClass() == o.getClass()) {
         RepositionNode that = (RepositionNode)o;
         return this.input.equals(that.input) && this.inputX.equals(that.inputX) && this.inputY.equals(that.inputY) && this.inputZ.equals(that.inputZ);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      int result = 1;
      result = 31 * result + this.input.hashCode();
      result = 31 * result + this.inputX.hashCode();
      result = 31 * result + this.inputY.hashCode();
      return 31 * result + this.inputZ.hashCode();
   }

   @Override
   public boolean relaxedEquals(AstNode o) {
      if (o != null && this.getClass() == o.getClass()) {
         RepositionNode that = (RepositionNode)o;
         return this.input.relaxedEquals(that.input)
            && this.inputX.relaxedEquals(that.inputX)
            && this.inputY.relaxedEquals(that.inputY)
            && this.inputZ.relaxedEquals(that.inputZ);
      } else {
         return false;
      }
   }

   @Override
   public int relaxedHashCode() {
      int result = 1;
      result = 31 * result + this.input.relaxedHashCode();
      result = 31 * result + this.inputX.relaxedHashCode();
      result = 31 * result + this.inputY.relaxedHashCode();
      return 31 * result + this.inputZ.relaxedHashCode();
   }
}
