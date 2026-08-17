package com.ishland.c2me.opts.dfc.common.gen.meta;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;

public record ValuesMethodDefF64(boolean isConst, String generatedMethod, double constValue) implements ValuesMethodDef {
   public ValuesMethodDefF64(String generatedMethod) {
      this(false, generatedMethod, Double.NaN);
   }

   public ValuesMethodDefF64(double constValue) {
      this(true, null, constValue);
   }

   @Override
   public AstNode.ReturnType returnType() {
      return AstNode.ReturnType.F64;
   }
}
