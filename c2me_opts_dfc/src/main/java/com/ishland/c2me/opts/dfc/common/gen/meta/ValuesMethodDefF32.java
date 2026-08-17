package com.ishland.c2me.opts.dfc.common.gen.meta;

import com.ishland.c2me.opts.dfc.common.ast.AstNode;

public record ValuesMethodDefF32(boolean isConst, String generatedMethod, float constValue) implements ValuesMethodDef {
   public ValuesMethodDefF32(String generatedMethod) {
      this(false, generatedMethod, Float.NaN);
   }

   public ValuesMethodDefF32(float constValue) {
      this(true, null, constValue);
   }

   @Override
   public AstNode.ReturnType returnType() {
      return AstNode.ReturnType.F32;
   }
}
