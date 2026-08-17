package com.ishland.c2me.opts.dfc.common.ast.misc;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.AstTransformer;
import com.ishland.c2me.opts.dfc.common.gen.meta.ValuesMethodDef;
import com.ishland.c2me.opts.dfc.common.gen.meta.ValuesMethodDefF64;

public class ConstantNode implements ConstantNodeLike {
   private final double value;

   public ConstantNode(double value) {
      this.value = value;
   }

   @Override
   public AstNode[] getChildren() {
      return new AstNode[0];
   }

   @Override
   public AstNode transform(AstTransformer transformer) {
      return transformer.transform(this);
   }

   public double getValue() {
      return this.value;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         ConstantNode that = (ConstantNode)o;
         return Double.compare(this.value, that.value) == 0;
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Double.hashCode(this.value);
   }

   @Override
   public boolean relaxedEquals(AstNode o) {
      return this.equals(o);
   }

   @Override
   public int relaxedHashCode() {
      return this.hashCode();
   }

   @Override
   public ValuesMethodDef getDef() {
      return new ValuesMethodDefF64(this.value);
   }
}
