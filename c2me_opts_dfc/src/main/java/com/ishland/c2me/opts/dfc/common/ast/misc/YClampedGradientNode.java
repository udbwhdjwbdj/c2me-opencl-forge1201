package com.ishland.c2me.opts.dfc.common.ast.misc;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.AstTransformer;

public class YClampedGradientNode implements AstNode {
   public final double fromY;
   public final double toY;
   public final double fromValue;
   public final double toValue;

   public YClampedGradientNode(double fromY, double toY, double fromValue, double toValue) {
      this.fromY = fromY;
      this.toY = toY;
      this.fromValue = fromValue;
      this.toValue = toValue;
   }

   @Override
   public AstNode[] getChildren() {
      return new AstNode[0];
   }

   @Override
   public AstNode transform(AstTransformer transformer) {
      return transformer.transform(this);
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         YClampedGradientNode that = (YClampedGradientNode)o;
         return Double.compare(this.fromY, that.fromY) == 0
            && Double.compare(this.toY, that.toY) == 0
            && Double.compare(this.fromValue, that.fromValue) == 0
            && Double.compare(this.toValue, that.toValue) == 0;
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      int result = 1;
      result = 31 * result + this.getClass().hashCode();
      result = 31 * result + Double.hashCode(this.fromY);
      result = 31 * result + Double.hashCode(this.toY);
      result = 31 * result + Double.hashCode(this.fromValue);
      return 31 * result + Double.hashCode(this.toValue);
   }

   @Override
   public boolean relaxedEquals(AstNode o) {
      return this.equals(o);
   }

   @Override
   public int relaxedHashCode() {
      return this.hashCode();
   }
}
