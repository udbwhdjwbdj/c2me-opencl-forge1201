package com.ishland.c2me.opts.dfc.common.ast;

public interface AstNode {
   AstNode[] getChildren();

   AstNode transform(AstTransformer var1);

   boolean relaxedEquals(AstNode var1);

   int relaxedHashCode();

   default AstNode.ReturnType getReturnType() {
      return AstNode.ReturnType.F64;
   }

   public static enum ReturnType {
      F64,
      F32;
   }
}
