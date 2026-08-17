package com.ishland.c2me.opts.dfc.common.ast.misc;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;
import com.ishland.c2me.opts.dfc.common.ast.AstTransformer;
import com.ishland.c2me.opts.dfc.common.gen.meta.ValuesMethodDef;
import com.ishland.c2me.opts.dfc.common.gen.meta.ValuesMethodDefF32;

public class ConstantF32Node implements ConstantNodeLike {
   private final float value;

   public ConstantF32Node(float value) {
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

   public float getValue() {
      return this.value;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         ConstantF32Node that = (ConstantF32Node)o;
         return Float.compare(this.value, that.value) == 0;
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Float.hashCode(this.value);
   }

   @Override
   public AstNode.ReturnType getReturnType() {
      return AstNode.ReturnType.F32;
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
      return new ValuesMethodDefF32(this.value);
   }
}
