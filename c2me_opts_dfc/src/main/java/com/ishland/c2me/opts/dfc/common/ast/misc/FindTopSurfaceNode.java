package com.ishland.c2me.opts.dfc.common.ast.misc;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.AstTransformer;
import java.util.Objects;

public class FindTopSurfaceNode implements AstNode {
   public final AstNode density;
   public final AstNode upperBound;
   public final AstNode lowerBound;
   public final int cellHeight;

   public FindTopSurfaceNode(AstNode density, AstNode upperBound, AstNode lowerBound, int cellHeight) {
      this.density = Objects.requireNonNull(density);
      this.upperBound = Objects.requireNonNull(upperBound);
      this.lowerBound = Objects.requireNonNull(lowerBound);
      this.cellHeight = cellHeight;
   }

   @Override
   public AstNode[] getChildren() {
      return new AstNode[]{this.density, this.upperBound, this.lowerBound};
   }

   @Override
   public AstNode transform(AstTransformer transformer) {
      AstNode density = this.density.transform(transformer);
      AstNode upperBound = this.upperBound.transform(transformer);
      AstNode lowerBound = this.lowerBound.transform(transformer);
      return density == this.density && upperBound == this.upperBound && lowerBound == this.lowerBound
         ? transformer.transform(this)
         : transformer.transform(new FindTopSurfaceNode(density, upperBound, lowerBound, this.cellHeight));
   }

   @Override
   public boolean equals(Object o) {
      if (o != null && this.getClass() == o.getClass()) {
         FindTopSurfaceNode that = (FindTopSurfaceNode)o;
         return this.cellHeight == that.cellHeight
            && Objects.equals(this.density, that.density)
            && Objects.equals(this.upperBound, that.upperBound)
            && Objects.equals(this.lowerBound, that.lowerBound);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      int result = 1;
      result = 31 * result + this.getClass().hashCode();
      result = 31 * result + this.density.hashCode();
      result = 31 * result + this.upperBound.hashCode();
      result = 31 * result + this.lowerBound.hashCode();
      return 31 * result + Integer.hashCode(this.cellHeight);
   }

   @Override
   public boolean relaxedEquals(AstNode o) {
      if (o != null && this.getClass() == o.getClass()) {
         FindTopSurfaceNode that = (FindTopSurfaceNode)o;
         return this.cellHeight == that.cellHeight
            && this.density.relaxedEquals(that.density)
            && this.upperBound.relaxedEquals(that.upperBound)
            && this.lowerBound.relaxedEquals(that.lowerBound);
      } else {
         return false;
      }
   }

   @Override
   public int relaxedHashCode() {
      int result = 1;
      result = 31 * result + this.getClass().hashCode();
      result = 31 * result + this.density.relaxedHashCode();
      result = 31 * result + this.upperBound.relaxedHashCode();
      result = 31 * result + this.lowerBound.relaxedHashCode();
      return 31 * result + Integer.hashCode(this.cellHeight);
   }
}
